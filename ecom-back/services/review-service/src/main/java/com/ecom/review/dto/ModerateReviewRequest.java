package com.ecom.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ModerateReviewRequest(
        @NotBlank
        @Pattern(regexp = "^(?i)(APPROVED|REJECTED|PENDING)$")
        String status) {
}
