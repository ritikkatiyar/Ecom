package com.ecom.payment.service;

import java.time.Instant;

public record PaymentResultPayload(
        String orderId,
        String paymentId,
        String providerPaymentId,
        String status,
        String reason,
        Instant occurredAt
) {
}
