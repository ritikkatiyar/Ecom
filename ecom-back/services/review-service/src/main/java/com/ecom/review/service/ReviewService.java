package com.ecom.review.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.review.dto.CreateReviewRequest;
import com.ecom.review.dto.ModerateReviewRequest;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.dto.UpdateReviewRequest;
import com.ecom.review.entity.ReviewRecord;
import com.ecom.review.entity.ReviewStatus;
import com.ecom.review.repository.ReviewRepository;

@Service
public class ReviewService implements ReviewUseCases {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        validateUserId(userId);
        ReviewRecord record = new ReviewRecord();
        record.setUserId(userId);
        record.setProductId(request.productId().trim());
        record.setRating(request.rating());
        record.setTitle(request.title().trim());
        record.setComment(request.comment().trim());
        record.setStatus(ReviewStatus.PENDING);
        return toResponse(reviewRepository.save(record));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        validateUserId(userId);
        ReviewRecord record = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for userId/reviewId"));
        record.setRating(request.rating());
        record.setTitle(request.title().trim());
        record.setComment(request.comment().trim());
        record.setStatus(ReviewStatus.PENDING);
        return toResponse(reviewRepository.save(record));
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        validateUserId(userId);
        ReviewRecord record = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for userId/reviewId"));
        reviewRepository.delete(record);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {
        ReviewRecord record = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> listProductReviews(String productId, boolean includePending) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId is required");
        }
        List<ReviewRecord> records = includePending
                ? reviewRepository.findByProductIdOrderByCreatedAtDesc(productId.trim())
                : reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId.trim(), ReviewStatus.APPROVED);
        return records.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> listUserReviews(Long userId) {
        validateUserId(userId);
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, ModerateReviewRequest request) {
        ReviewRecord record = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        ReviewStatus status = ReviewStatus.valueOf(request.status().trim().toUpperCase());
        record.setStatus(status);
        return toResponse(reviewRepository.save(record));
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be a positive number");
        }
    }

    private ReviewResponse toResponse(ReviewRecord record) {
        return new ReviewResponse(
                record.getId(),
                record.getUserId(),
                record.getProductId(),
                record.getRating(),
                record.getTitle(),
                record.getComment(),
                record.getStatus(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
