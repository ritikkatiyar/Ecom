package com.ecom.payment.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.payment.entity.OutboxEventRecord;
import com.ecom.payment.entity.OutboxStatus;
import com.ecom.payment.repository.OutboxEventRepository;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int maxRetry;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.max-retry:5}") int maxRetry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "PT3S")
    @Transactional
    public void publishPending() {
        List<OutboxEventRecord> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEventRecord record : pending) {
            try {
                kafkaTemplate.send(record.getTopic(), record.getMessageKey(), record.getPayload()).get(5, TimeUnit.SECONDS);
                record.setStatus(OutboxStatus.SENT);
                record.setLastError(null);
            } catch (Exception ex) {
                int attempts = record.getAttempts() + 1;
                record.setAttempts(attempts);
                record.setStatus(attempts >= maxRetry ? OutboxStatus.FAILED : OutboxStatus.PENDING);
                record.setLastError(trim(ex.getMessage(), 500));
            }
            outboxEventRepository.save(record);
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
