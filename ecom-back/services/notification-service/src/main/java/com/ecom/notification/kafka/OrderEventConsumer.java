package com.ecom.notification.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.notification.service.ConsumerDedupService;
import com.ecom.notification.service.NotificationUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private final NotificationUseCases notificationService;
    private final ConsumerDedupService dedupService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(NotificationUseCases notificationService, ConsumerDedupService dedupService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-created:order.created.v1}", groupId = "notification-service")
    public void onOrderCreated(String rawEvent) {
        parse(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            notificationService.handleDomainEvent(event, "order.created.v1");
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-authorized:payment.authorized.v1}", groupId = "notification-service")
    public void onPaymentAuthorized(String rawEvent) {
        parse(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            notificationService.handleDomainEvent(event, "payment.authorized.v1");
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-failed:payment.failed.v1}", groupId = "notification-service")
    public void onPaymentFailed(String rawEvent) {
        parse(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            notificationService.handleDomainEvent(event, "payment.failed.v1");
        });
    }

    private java.util.Optional<DomainEvent<Map<String, Object>>> parse(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            var typeRef = new TypeReference<DomainEvent<Map<String, Object>>>() {};
            return java.util.Optional.of(objectMapper.readValue(rawEvent, typeRef));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }
}
