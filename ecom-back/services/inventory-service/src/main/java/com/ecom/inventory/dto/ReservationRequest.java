package com.ecom.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(
        @NotBlank String reservationId,
        @NotBlank String sku,
        @Min(1) int quantity,
        @Min(1) @Max(120) int ttlMinutes
) {
}
