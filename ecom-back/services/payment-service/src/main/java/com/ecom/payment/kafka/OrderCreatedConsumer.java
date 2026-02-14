package com.ecom.payment.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.payment.service.ConsumerDedupService;
import com.ecom.payment.service.PaymentUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedConsumer {

    private final PaymentUseCases paymentService;
    private final ConsumerDedupService dedupService;
    private final ObjectMapper objectMapper;

    public OrderCreatedConsumer(PaymentUseCases paymentService, ConsumerDedupService dedupService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-created:order.created.v1}", groupId = "payment-service")
    public void onOrderCreated(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            return;
        }

        try {
            var typeRef = new TypeReference<DomainEvent<Map<String, Object>>>() {};
            DomainEvent<Map<String, Object>> event = objectMapper.readValue(rawEvent, typeRef);
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }

            if (event.payload() == null) {
                return;
            }

            Object orderIdObj = event.payload().get("orderId");
            Object userIdObj = event.payload().get("userId");
            Object currencyObj = event.payload().get("currency");

            if (orderIdObj == null || userIdObj == null) {
                return;
            }

            String orderId = orderIdObj.toString();
            Long userId = Long.valueOf(userIdObj.toString());
            String currency = currencyObj == null ? "INR" : currencyObj.toString();

            paymentService.createPendingForOrder(orderId, userId, currency);
        } catch (Exception ignored) {
            // Keep consumer resilient; malformed payloads can be routed to DLQ in a later phase.
        }
    }
}
