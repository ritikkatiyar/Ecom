package com.ecom.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotBlank String productId,
        @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 5000) String comment) {
}
