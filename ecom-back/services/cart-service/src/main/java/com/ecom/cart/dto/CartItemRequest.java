package com.ecom.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CartItemRequest(
        Long userId,
        String guestId,
        @NotBlank String productId,
        @Min(1) int quantity
) {
}
