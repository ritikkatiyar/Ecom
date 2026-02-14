package com.ecom.order.service;

import java.time.Instant;
import java.util.List;

public record OrderCreatedPayload(
        String orderId,
        Long userId,
        String currency,
        String status,
        List<OrderItemPayload> items,
        Instant createdAt
) {
    public record OrderItemPayload(String productId, String sku, int quantity, String unitPrice) {}
}
