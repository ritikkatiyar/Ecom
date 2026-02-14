package com.ecom.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentIntentRequest(
        @NotBlank String orderId,
        @NotNull Long userId,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String idempotencyKey
) {
}
