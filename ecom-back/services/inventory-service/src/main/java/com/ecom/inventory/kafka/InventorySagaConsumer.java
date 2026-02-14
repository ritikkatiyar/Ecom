package com.ecom.inventory.kafka;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.inventory.service.ConsumerDedupService;
import com.ecom.inventory.service.InventoryUseCases;
import com.ecom.inventory.service.OutboxService;
import com.ecom.inventory.service.OrderItemReservation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InventorySagaConsumer {

    private final InventoryUseCases inventoryService;
    private final ConsumerDedupService dedupService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final int reservationTtlMinutes;
    private final String inventoryReservedTopic;
    private final String inventoryReservationFailedTopic;

    public InventorySagaConsumer(
            InventoryUseCases inventoryService,
            ConsumerDedupService dedupService,
            OutboxService outboxService,
            ObjectMapper objectMapper,
            @Value("${app.inventory.reservation-ttl-minutes:30}") int reservationTtlMinutes,
            @Value("${app.kafka.topics.inventory-reserved:inventory.reserved.v1}") String inventoryReservedTopic,
            @Value("${app.kafka.topics.inventory-reservation-failed:inventory.reservation.failed.v1}") String inventoryReservationFailedTopic) {
        this.inventoryService = inventoryService;
        this.dedupService = dedupService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.reservationTtlMinutes = reservationTtlMinutes;
        this.inventoryReservedTopic = inventoryReservedTopic;
        this.inventoryReservationFailedTopic = inventoryReservationFailedTopic;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-created:order.created.v1}", groupId = "inventory-service")
    public void onOrderCreated(String rawEvent) {
        parseEvent(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            String orderId = readString(event.payload(), "orderId");
            if (orderId == null) {
                return;
            }
            List<OrderItemReservation> items = readItems(event.payload());
            if (items.isEmpty()) {
                publishReservationFailed(orderId, "No order items in order.created event");
                return;
            }

            try {
                inventoryService.reserveForOrder(orderId, items, reservationTtlMinutes);
                publishReserved(orderId);
            } catch (Exception ex) {
                publishReservationFailed(orderId, ex.getMessage());
            }
        });
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

    private List<OrderItemReservation> readItems(Map<String, Object> payload) {
        Object rawItems = payload.get("items");
        if (!(rawItems instanceof List<?> rawList)) {
            return List.of();
        }
        return rawList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> new OrderItemReservation(readString(item, "sku"), readInt(item, "quantity")))
                .filter(item -> item.sku() != null && !item.sku().isBlank() && item.quantity() > 0)
                .toList();
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

    private int readInt(Map<String, Object> payload, String key) {
        if (payload == null || payload.get(key) == null) {
            return 0;
        }
        try {
            return Integer.parseInt(payload.get(key).toString());
        } catch (Exception ex) {
            return 0;
        }
    }

    private void publishReserved(String orderId) {
        publish(inventoryReservedTopic, "inventory.reserved.v1", orderId, null);
    }

    private void publishReservationFailed(String orderId, String reason) {
        publish(inventoryReservationFailedTopic, "inventory.reservation.failed.v1", orderId, reason);
    }

    private void publish(String topic, String eventType, String orderId, String reason) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("orderId", orderId);
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        payload.put("emittedAt", Instant.now().toString());
        outboxService.enqueue(topic, orderId, eventType, payload, "inventory-service");
    }
}
