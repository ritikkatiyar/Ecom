package com.ecom.notification.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.common.DomainEvent;
import com.ecom.notification.dto.NotificationDeadLetterResponse;
import com.ecom.notification.dto.NotificationResponse;
import com.ecom.notification.entity.NotificationDeadLetterRecord;
import com.ecom.notification.entity.NotificationRecord;
import com.ecom.notification.entity.NotificationStatus;
import com.ecom.notification.repository.NotificationDeadLetterRepository;
import com.ecom.notification.repository.NotificationRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class NotificationService implements NotificationUseCases {

    private final NotificationRecordRepository repository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final EmailSender emailSender;
    private final NotificationTemplateService templateService;
    private final NotificationAlertPublisher alertPublisher;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int maxRetry;
    private final String notificationDlqTopic;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter deadLetterCounter;
    private final Counter requeueCounter;

    public NotificationService(
            NotificationRecordRepository repository,
            NotificationDeadLetterRepository deadLetterRepository,
            EmailSender emailSender,
            NotificationTemplateService templateService,
            NotificationAlertPublisher alertPublisher,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.notification.max-retry:3}") int maxRetry,
            @Value("${app.kafka.topics.notification-dlq:notification.dlq.v1}") String notificationDlqTopic) {
        this.repository = repository;
        this.deadLetterRepository = deadLetterRepository;
        this.emailSender = emailSender;
        this.templateService = templateService;
        this.alertPublisher = alertPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.maxRetry = maxRetry;
        this.notificationDlqTopic = notificationDlqTopic;
        this.sentCounter = meterRegistry.counter("notification.sent.total");
        this.failedCounter = meterRegistry.counter("notification.failed.total");
        this.deadLetterCounter = meterRegistry.counter("notification.dead_letter.total");
        this.requeueCounter = meterRegistry.counter("notification.requeue.total");
    }

    @Override
    @Transactional
    public void handleDomainEvent(DomainEvent<Map<String, Object>> event, String fallbackEventType) {
        if (event == null || event.payload() == null) {
            return;
        }

        String eventId = event.eventId() == null ? UUID.randomUUID().toString() : event.eventId().toString();
        if (repository.findByEventId(eventId).isPresent()) {
            return;
        }

        NotificationRecord record = new NotificationRecord();
        record.setEventId(eventId);
        record.setEventType(event.eventType() == null || event.eventType().isBlank() ? fallbackEventType : event.eventType());
        record.setUserId(resolveUserId(event.payload()));
        record.setRecipientEmail(resolveRecipientEmail(event.payload()));
        record.setSubject(templateService.renderSubject(record.getEventType(), event.payload()));
        record.setBody(templateService.renderBody(record.getEventType(), event.payload()));
        record.setRetryCount(0);
        record.setStatus(NotificationStatus.PENDING);

        repository.save(record);
        deliver(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listFailed() {
        return repository.findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.FAILED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int retryFailed() {
        List<NotificationRecord> failed = repository.findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.FAILED);
        int retried = 0;
        for (NotificationRecord record : failed) {
            if (record.getRetryCount() >= maxRetry) {
                moveToDeadLetter(record);
                continue;
            }
            deliver(record);
            retried++;
        }
        return retried;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeadLetterResponse> listDeadLetters() {
        return deadLetterRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toDeadLetterResponse)
                .toList();
    }

    @Override
    @Transactional
    public boolean requeueDeadLetter(Long deadLetterId) {
        var deadLetterOpt = deadLetterRepository.findById(deadLetterId);
        if (deadLetterOpt.isEmpty()) {
            return false;
        }
        NotificationDeadLetterRecord deadLetter = deadLetterOpt.get();
        if (!"DEAD_LETTER".equalsIgnoreCase(deadLetter.getStatus())) {
            return false;
        }

        NotificationRecord record = repository.findByEventId(deadLetter.getEventId()).orElseGet(NotificationRecord::new);
        record.setEventId(deadLetter.getEventId());
        record.setEventType(deadLetter.getEventType());
        record.setUserId(deadLetter.getUserId());
        record.setRecipientEmail(deadLetter.getRecipientEmail());
        record.setSubject(deadLetter.getSubject());
        record.setBody(deadLetter.getBody());
        record.setStatus(NotificationStatus.PENDING);
        record.setRetryCount(Math.max(0, deadLetter.getRetries()));
        record.setLastError(null);
        repository.save(record);

        deadLetter.setStatus("REQUEUED");
        deadLetterRepository.save(deadLetter);
        requeueCounter.increment();
        return true;
    }

    private void deliver(NotificationRecord record) {
        try {
            emailSender.send(record.getRecipientEmail(), record.getSubject(), record.getBody());
            record.setStatus(NotificationStatus.SENT);
            record.setLastError(null);
            sentCounter.increment();
        } catch (Exception ex) {
            int nextRetry = record.getRetryCount() + 1;
            record.setRetryCount(nextRetry);
            record.setLastError(trim(ex.getMessage(), 500));
            if (nextRetry >= maxRetry) {
                record.setStatus(NotificationStatus.DEAD_LETTER);
                repository.save(record);
                moveToDeadLetter(record);
                return;
            }
            record.setStatus(NotificationStatus.FAILED);
            failedCounter.increment();
        }
        repository.save(record);
    }

    private NotificationResponse toResponse(NotificationRecord record) {
        return new NotificationResponse(
                record.getId(),
                record.getEventId(),
                record.getEventType(),
                record.getUserId(),
                record.getRecipientEmail(),
                record.getSubject(),
                record.getBody(),
                record.getStatus().name(),
                record.getRetryCount(),
                record.getLastError(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private NotificationDeadLetterResponse toDeadLetterResponse(NotificationDeadLetterRecord deadLetter) {
        return new NotificationDeadLetterResponse(
                deadLetter.getId(),
                deadLetter.getEventId(),
                deadLetter.getEventType(),
                deadLetter.getUserId(),
                deadLetter.getRecipientEmail(),
                deadLetter.getSubject(),
                deadLetter.getBody(),
                deadLetter.getStatus(),
                deadLetter.getRetries(),
                deadLetter.getReason(),
                deadLetter.getSourceTopic(),
                deadLetter.getCreatedAt(),
                deadLetter.getUpdatedAt());
    }

    private void moveToDeadLetter(NotificationRecord record) {
        if (deadLetterRepository.findByEventId(record.getEventId()).isPresent()) {
            return;
        }
        NotificationDeadLetterRecord dead = new NotificationDeadLetterRecord();
        dead.setEventId(record.getEventId());
        dead.setEventType(record.getEventType());
        dead.setUserId(record.getUserId());
        dead.setRecipientEmail(record.getRecipientEmail());
        dead.setSubject(record.getSubject());
        dead.setBody(record.getBody());
        dead.setStatus("DEAD_LETTER");
        dead.setRetries(record.getRetryCount());
        dead.setReason(record.getLastError());
        dead.setSourceTopic(record.getEventType());
        deadLetterRepository.save(dead);
        publishDlqEvent(dead);
        alertPublisher.publishDeadLetterAlert(dead);
        deadLetterCounter.increment();
    }

    private void publishDlqEvent(NotificationDeadLetterRecord dead) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", dead.getEventId());
        payload.put("eventType", dead.getEventType());
        payload.put("userId", dead.getUserId());
        payload.put("recipientEmail", dead.getRecipientEmail());
        payload.put("subject", dead.getSubject());
        payload.put("body", dead.getBody());
        payload.put("reason", dead.getReason());
        payload.put("retries", dead.getRetries());
        payload.put("sourceTopic", dead.getSourceTopic());

        DomainEvent<Map<String, Object>> event = new DomainEvent<>(
                UUID.randomUUID(),
                "notification.dead-letter.v1",
                java.time.Instant.now(),
                "notification-service",
                "v1",
                UUID.randomUUID().toString(),
                payload);
        try {
            kafkaTemplate.send(notificationDlqTopic, dead.getEventId(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }

    private Long resolveUserId(Map<String, Object> payload) {
        Object userId = payload.get("userId");
        if (userId == null) {
            return 0L;
        }
        return Long.valueOf(userId.toString());
    }

    private String resolveRecipientEmail(Map<String, Object> payload) {
        Object email = payload.get("email");
        if (email != null && !email.toString().isBlank()) {
            return email.toString();
        }

        Long userId = resolveUserId(payload);
        return "user" + userId + "@example.local";
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    @Scheduled(fixedDelayString = "PT60S")
    public void scheduledRetryFailed() {
        retryFailed();
    }
}
