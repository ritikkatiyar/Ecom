package com.ecom.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String providerPaymentId,
        @NotBlank String eventType,
        String failureReason
) {
}
