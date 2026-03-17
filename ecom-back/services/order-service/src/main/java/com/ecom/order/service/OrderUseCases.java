package com.ecom.order.service;

import java.util.List;

import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderResponse;

public interface OrderUseCases {

    /**
     * Creates a new order after synchronously reserving inventory for every requested item.
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Returns the latest persisted state for one order.
     */
    OrderResponse getOrder(String orderId);

    /**
     * Lists orders for one user in reverse chronological order.
     */
    List<OrderResponse> listOrders(Long userId);

    /**
     * Cancels a non-finalized order and triggers inventory release.
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * Manually confirms an order that is still awaiting final completion.
     */
    OrderResponse confirmOrder(String orderId);

    /**
     * Marks an order as confirmed after payment authorization succeeds.
     */
    void markPaymentAuthorized(String orderId);

    /**
     * Marks an order as cancelled after payment failure or equivalent compensation.
     */
    void markPaymentFailed(String orderId);

    /**
     * Cancels stale payment-pending orders whose checkout deadline has expired.
     */
    int markTimedOutOrders();

    /**
     * Resets failed outbox events so the scheduled publisher can retry them.
     */
    int replayFailedOutboxEvents();
}
