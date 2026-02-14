package com.ecom.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String currency,
        @Valid @Size(min = 1) List<OrderItemRequest> items
) {
}
