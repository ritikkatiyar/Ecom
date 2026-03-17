package com.ecom.inventory.kafka;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.inventory.service.ConsumerDedupService;
import com.ecom.inventory.service.InventoryUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InventorySagaConsumer {

    private final InventoryUseCases inventoryService;
    private final ConsumerDedupService dedupService;
    private final ObjectMapper objectMapper;

    public InventorySagaConsumer(
            InventoryUseCases inventoryService,
            ConsumerDedupService dedupService,
            ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-authorized:payment.authorized.v1}", groupId = "inventory-service")
    public void onPaymentAuthorized(String rawEvent) {
        parseEvent(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            String orderId = readString(event.payload(), "orderId");
            if (orderId != null) {
                inventoryService.confirmForOrder(orderId);
            }
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-failed:payment.failed.v1}", groupId = "inventory-service")
    public void onPaymentFailed(String rawEvent) {
        parseEvent(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            String orderId = readString(event.payload(), "orderId");
            if (orderId != null) {
                inventoryService.releaseForOrder(orderId);
            }
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.order-timed-out:order.timed-out.v1}", groupId = "inventory-service")
    public void onOrderTimedOut(String rawEvent) {
        parseEvent(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            String orderId = readString(event.payload(), "orderId");
            if (orderId != null) {
                inventoryService.releaseForOrder(orderId);
            }
        });
    }

    private java.util.Optional<DomainEvent<Map<String, Object>>> parseEvent(String rawEvent) {
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

    private String readString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

}
