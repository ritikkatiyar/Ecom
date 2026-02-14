package com.ecom.order.service;

import java.util.List;

import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderResponse;

public interface OrderUseCases {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(String orderId);

    List<OrderResponse> listOrders(Long userId);

    OrderResponse cancelOrder(String orderId);

    OrderResponse confirmOrder(String orderId);

    void markPaymentAuthorized(String orderId);

    void markPaymentFailed(String orderId);
}
