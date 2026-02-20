package com.ecom.order.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecom.order.dto.OrderItemRequest;
import com.ecom.order.entity.OrderRecord;

@Component
public class OrderEventPublisher {

    private final OutboxService outboxService;
    private final String orderCreatedTopic;
    private final String orderTimedOutTopic;

    public OrderEventPublisher(
            OutboxService outboxService,
            @Value("${app.kafka.topics.order-created:order.created.v1}") String orderCreatedTopic,
            @Value("${app.kafka.topics.order-timed-out:order.timed-out.v1}") String orderTimedOutTopic) {
        this.outboxService = outboxService;
        this.orderCreatedTopic = orderCreatedTopic;
        this.orderTimedOutTopic = orderTimedOutTopic;
    }

    public void publishOrderCreated(OrderRecord order, List<OrderItemRequest> items) {
        List<OrderCreatedPayload.OrderItemPayload> payloadItems = items.stream()
                .map(i -> new OrderCreatedPayload.OrderItemPayload(
                        i.productId(),
                        i.sku(),
                        i.quantity(),
                        i.unitPrice().toPlainString()))
                .toList();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(),
                order.getUserId(),
                order.getCurrency(),
                order.getStatus().name(),
                payloadItems,
                Instant.now());

        outboxService.enqueue(orderCreatedTopic, order.getId(), "order.created.v1", payload, "order-service");
    }

    public void publishOrderTimedOut(OrderRecord order) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("status", order.getStatus().name());
        payload.put("timedOutAt", Instant.now().toString());
        outboxService.enqueue(orderTimedOutTopic, order.getId(), "order.timed-out.v1", payload, "order-service");
    }
}
