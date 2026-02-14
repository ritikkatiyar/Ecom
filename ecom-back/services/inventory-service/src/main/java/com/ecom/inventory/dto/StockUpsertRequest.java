package com.ecom.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StockUpsertRequest(
        @NotBlank String sku,
        @Min(0) int availableQuantity
) {
}
