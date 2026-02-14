package com.ecom.search.dto;

import java.time.Instant;

public record ReindexResponse(
        boolean purged,
        int pagesProcessed,
        int productsFetched,
        int productsIndexed,
        Instant completedAt) {
}
