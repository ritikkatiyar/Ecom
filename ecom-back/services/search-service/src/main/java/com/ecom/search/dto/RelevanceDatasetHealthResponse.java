package com.ecom.search.dto;

import java.time.Instant;

public record RelevanceDatasetHealthResponse(
        String datasetVersion,
        int datasetSize,
        Instant lastRefreshedAt,
        int refreshCadenceDays,
        long daysSinceRefresh,
        boolean refreshRequired,
        Instant evaluatedAt) {
}
