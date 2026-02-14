package com.ecom.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MergeCartRequest(
        @NotNull Long userId,
        @NotBlank String guestId
) {
}
