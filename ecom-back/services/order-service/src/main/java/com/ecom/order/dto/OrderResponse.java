package com.ecom.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        Long userId,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemRequest> items,
        Instant createdAt,
        Instant updatedAt
) {
}
