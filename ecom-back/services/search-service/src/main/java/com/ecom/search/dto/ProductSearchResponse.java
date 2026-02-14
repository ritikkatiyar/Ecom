package com.ecom.search.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductSearchResponse(
        String productId,
        String name,
        String description,
        String category,
        String brand,
        BigDecimal price,
        List<String> colors,
        List<String> sizes,
        Boolean active,
        Instant updatedAt) {
}
