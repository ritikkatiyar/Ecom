package com.ecom.review.service;

import java.util.List;

import com.ecom.review.dto.CreateReviewRequest;
import com.ecom.review.dto.ModerateReviewRequest;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.dto.UpdateReviewRequest;

public interface ReviewUseCases {

    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long userId, Long reviewId);

    ReviewResponse getReview(Long reviewId);

    List<ReviewResponse> listProductReviews(String productId, boolean includePending);

    List<ReviewResponse> listUserReviews(Long userId);

    ReviewResponse moderateReview(Long reviewId, ModerateReviewRequest request);
}
