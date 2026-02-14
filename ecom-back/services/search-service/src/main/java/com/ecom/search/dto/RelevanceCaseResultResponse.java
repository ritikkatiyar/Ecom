package com.ecom.search.dto;

import java.util.List;

public record RelevanceCaseResultResponse(
        String query,
        String expectedTopProductId,
        String actualTopProductId,
        boolean topMatch,
        boolean expectedPresentInTopN,
        List<String> topProductIds) {
}
