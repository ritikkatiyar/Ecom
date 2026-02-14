package com.ecom.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderItemRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.entity.OrderRecord;
import com.ecom.order.entity.OrderStatus;
import com.ecom.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderService implements OrderUseCases {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    private final String orderCreatedTopic;

    public OrderService(
            OrderRepository orderRepository,
            ObjectMapper objectMapper,
            OutboxService outboxService,
            @Value("${app.kafka.topics.order-created:order.created.v1}") String orderCreatedTopic) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
        this.orderCreatedTopic = orderCreatedTopic;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        validateCurrency(request.currency());

        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        OrderRecord order = new OrderRecord();
        order.setUserId(request.userId());
        order.setCurrency(request.currency().toUpperCase());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(total);
        order.setItemsJson(writeItems(request.items()));

        order = orderRepository.save(order);
        publishOrderCreated(order, request.items());

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order = orderRepository.save(order);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return toResponse(fetch(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order cannot be cancelled in state " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmOrder(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING && order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Order cannot be confirmed in state " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void markPaymentAuthorized(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    @Transactional
    public void markPaymentFailed(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private OrderRecord fetch(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank() || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter code");
        }
    }

    private String writeItems(List<OrderItemRequest> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize order items", ex);
        }
    }

    private List<OrderItemRequest> readItems(String itemsJson) {
        try {
            return objectMapper.readerForListOf(OrderItemRequest.class).readValue(itemsJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not deserialize order items", ex);
        }
    }

    private OrderResponse toResponse(OrderRecord record) {
        return new OrderResponse(
                record.getId(),
                record.getUserId(),
                record.getStatus().name(),
                record.getTotalAmount(),
                record.getCurrency(),
                readItems(record.getItemsJson()),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private void publishOrderCreated(OrderRecord order, List<OrderItemRequest> items) {
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
}
