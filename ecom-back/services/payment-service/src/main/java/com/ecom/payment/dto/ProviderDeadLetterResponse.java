package com.ecom.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderDeadLetterResponse(
        Long id,
        String idempotencyKey,
        String orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        int attempts,
        String status,
        String failureReason,
        String requeuedPaymentId,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
}
