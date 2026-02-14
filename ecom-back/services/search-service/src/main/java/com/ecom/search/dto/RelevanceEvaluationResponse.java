package com.ecom.search.dto;

import java.time.Instant;
import java.util.List;

public record RelevanceEvaluationResponse(
        int datasetSize,
        int passed,
        int failed,
        double passRate,
        double targetPassRate,
        boolean meetsTarget,
        Instant evaluatedAt,
        List<RelevanceCaseResultResponse> results) {
}
