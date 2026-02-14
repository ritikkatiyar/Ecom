package com.ecom.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record ReservationActionRequest(
        @NotBlank String reservationId
) {
}
