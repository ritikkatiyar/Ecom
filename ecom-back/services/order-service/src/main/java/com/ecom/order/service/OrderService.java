package com.ecom.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.entity.OrderRecord;
import com.ecom.order.entity.OrderStatus;
import com.ecom.order.entity.OutboxStatus;
import com.ecom.order.repository.OutboxEventRepository;
import com.ecom.order.repository.OrderRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class OrderService implements OrderUseCases {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderItemCodec orderItemCodec;
    private final OrderResponseMapper orderResponseMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final InventoryReservationClient inventoryReservationClient;
    private final int paymentTimeoutMinutes;
    private final Counter timeoutCounter;
    private final Counter outboxReplayCounter;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            OrderItemCodec orderItemCodec,
            OrderResponseMapper orderResponseMapper,
            OrderEventPublisher orderEventPublisher,
            InventoryReservationClient inventoryReservationClient,
            MeterRegistry meterRegistry,
            @Value("${app.saga.payment-timeout-minutes:15}") int paymentTimeoutMinutes) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderItemCodec = orderItemCodec;
        this.orderResponseMapper = orderResponseMapper;
        this.orderEventPublisher = orderEventPublisher;
        this.inventoryReservationClient = inventoryReservationClient;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        this.timeoutCounter = meterRegistry.counter("order.saga.timeout.total");
        this.outboxReplayCounter = meterRegistry.counter("order.outbox.replay.total");
        Gauge.builder("order.outbox.failed.records", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.FAILED))
                .register(meterRegistry);
    }

    /**
     * Reserves inventory synchronously, persists the order in PAYMENT_PENDING, and emits order.created.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        validateCurrency(request.currency());

        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String orderId = "ord_" + UUID.randomUUID();
        inventoryReservationClient.reserve(orderId, request.items());

        OrderRecord order = new OrderRecord();
        order.setId(orderId);
        order.setUserId(request.userId());
        order.setCurrency(request.currency().toUpperCase());
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setTotalAmount(total);
        order.setItemsJson(orderItemCodec.writeItems(request.items()));

        order = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(order, request.items());

        return toResponse(order);
    }

    /**
     * Loads one order by id for polling or detail views.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return toResponse(fetch(orderId));
    }

    /**
     * Returns all orders for a user sorted by newest first.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    /**
     * Cancels an order that has not reached a final successful state and emits timeout-style release flow.
     */
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order cannot be cancelled in state " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        orderEventPublisher.publishOrderTimedOut(order);
        return toResponse(order);
    }

    /**
     * Confirms an order manually while it is still awaiting completion.
     */
    @Transactional
    public OrderResponse confirmOrder(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING && order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Order cannot be confirmed in state " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return toResponse(orderRepository.save(order));
    }

    /**
     * Finalizes the order after payment authorization succeeds.
     */
    @Transactional
    public void markPaymentAuthorized(String orderId) {
        OrderRecord order = fetch(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    /**
     * Cancels the order when payment fails, unless it already reached a final state.
     */
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

    @Override
    /**
     * Sweeps expired payment-pending orders and emits release events for reserved inventory.
     */
    @Transactional
    public int markTimedOutOrders() {
        Instant deadline = Instant.now().minus(paymentTimeoutMinutes, ChronoUnit.MINUTES);
        List<OrderRecord> staleOrders = orderRepository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                OrderStatus.PAYMENT_PENDING, deadline);

        int updated = 0;
        for (OrderRecord order : staleOrders) {
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
                continue;
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            orderEventPublisher.publishOrderTimedOut(order);
            timeoutCounter.increment();
            updated++;
        }
        return updated;
    }

    @Override
    /**
     * Requeues failed outbox rows for scheduled republishing.
     */
    @Transactional
    public int replayFailedOutboxEvents() {
        List<com.ecom.order.entity.OutboxEventRecord> failed =
                outboxEventRepository.findTop100ByStatusOrderByUpdatedAtAsc(OutboxStatus.FAILED);
        int replayed = 0;
        for (com.ecom.order.entity.OutboxEventRecord event : failed) {
            event.setStatus(OutboxStatus.PENDING);
            event.setLastError(null);
            outboxEventRepository.save(event);
            outboxReplayCounter.increment();
            replayed++;
        }
        return replayed;
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void scheduledTimeoutSweep() {
        markTimedOutOrders();
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

    private OrderResponse toResponse(OrderRecord record) {
        return orderResponseMapper.toResponse(record);
    }
}
