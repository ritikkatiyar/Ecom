package com.ecom.order.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.order.service.ConsumerDedupService;
import com.ecom.order.service.OrderUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InventoryReservationConsumer {

    private final OrderUseCases orderService;
    private final ConsumerDedupService dedupService;
    private final ObjectMapper objectMapper;

    public InventoryReservationConsumer(OrderUseCases orderService, ConsumerDedupService dedupService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.inventory-reservation-failed:inventory.reservation.failed.v1}", groupId = "order-service")
    public void onReservationFailed(String rawEvent) {
        readEvent(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            readOrderId(event).ifPresent(orderService::markPaymentFailed);
        });
    }

    private java.util.Optional<DomainEvent<Map<String, Object>>> readEvent(String rawEvent) {
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

    private java.util.Optional<String> readOrderId(DomainEvent<Map<String, Object>> event) {
        if (event == null || event.payload() == null) {
            return java.util.Optional.empty();
        }
        Object orderIdObj = event.payload().get("orderId");
        if (orderIdObj == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(orderIdObj.toString());
    }
}
