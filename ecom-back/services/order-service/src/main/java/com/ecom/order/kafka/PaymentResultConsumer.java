package com.ecom.order.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.order.service.OrderUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentResultConsumer {

    private final OrderUseCases orderService;
    private final ObjectMapper objectMapper;

    public PaymentResultConsumer(OrderUseCases orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-authorized:payment.authorized.v1}", groupId = "order-service")
    public void onPaymentAuthorized(String rawEvent) {
        readOrderId(rawEvent).ifPresent(orderService::markPaymentAuthorized);
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-failed:payment.failed.v1}", groupId = "order-service")
    public void onPaymentFailed(String rawEvent) {
        readOrderId(rawEvent).ifPresent(orderService::markPaymentFailed);
    }

    private java.util.Optional<String> readOrderId(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            var typeRef = new TypeReference<DomainEvent<Map<String, Object>>>() {};
            DomainEvent<Map<String, Object>> event = objectMapper.readValue(rawEvent, typeRef);
            if (event.payload() == null) {
                return java.util.Optional.empty();
            }
            Object orderIdObj = event.payload().get("orderId");
            if (orderIdObj == null) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(orderIdObj.toString());
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }
}
