package com.ecom.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String status,
        String providerPaymentId,
        String idempotencyKey,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
