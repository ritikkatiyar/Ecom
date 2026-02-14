package com.ecom.search.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductIndexRequest(
        @NotBlank String productId,
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotBlank String brand,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        List<String> colors,
        List<String> sizes,
        Boolean active) {
}
