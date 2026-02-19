package com.ecom.review.dto;

import java.time.Instant;

import com.ecom.review.entity.ReviewStatus;

public record ReviewResponse(
        Long id,
        Long userId,
        String productId,
        Integer rating,
        String title,
        String comment,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
