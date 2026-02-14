package com.ecom.notification.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.notification.entity.NotificationDeadLetterRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationAlertPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String alertTopic;

    public NotificationAlertPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.notification-alert:notification.alert.v1}") String alertTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.alertTopic = alertTopic;
    }

    public void publishDeadLetterAlert(NotificationDeadLetterRecord deadLetter) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("severity", "HIGH");
        payload.put("alertType", "NOTIFICATION_DEAD_LETTER");
        payload.put("eventId", deadLetter.getEventId());
        payload.put("eventType", deadLetter.getEventType());
        payload.put("reason", deadLetter.getReason());
        payload.put("retries", deadLetter.getRetries());
        payload.put("createdAt", deadLetter.getCreatedAt() == null ? Instant.now().toString() : deadLetter.getCreatedAt().toString());

        DomainEvent<Map<String, Object>> event = new DomainEvent<>(
                UUID.randomUUID(),
                "notification.alert.v1",
                Instant.now(),
                "notification-service",
                "v1",
                UUID.randomUUID().toString(),
                payload);

        try {
            kafkaTemplate.send(alertTopic, deadLetter.getEventId(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }
}
