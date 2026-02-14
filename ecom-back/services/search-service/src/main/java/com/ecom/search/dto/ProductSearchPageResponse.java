package com.ecom.search.dto;

import java.util.List;

public record ProductSearchPageResponse(
        List<ProductSearchResponse> content,
        long totalElements,
        int page,
        int size) {
}
