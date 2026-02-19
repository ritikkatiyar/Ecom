package com.ecom.review.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.review.dto.CreateReviewRequest;
import com.ecom.review.dto.ModerateReviewRequest;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.dto.UpdateReviewRequest;
import com.ecom.review.service.ReviewUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private final ReviewUseCases reviewUseCases;

    public ReviewController(ReviewUseCases reviewUseCases) {
        this.reviewUseCases = reviewUseCases;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestParam Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewUseCases.createReview(userId, request));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateReviewRequest request) {
        return reviewUseCases.updateReview(userId, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        reviewUseCases.deleteReview(userId, reviewId);
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse getReview(@PathVariable Long reviewId) {
        return reviewUseCases.getReview(reviewId);
    }

    @GetMapping
    public List<ReviewResponse> listProductReviews(
            @RequestParam String productId,
            @RequestParam(defaultValue = "false") boolean includePending) {
        return reviewUseCases.listProductReviews(productId, includePending);
    }

    @GetMapping("/by-user")
    public List<ReviewResponse> listUserReviews(@RequestParam Long userId) {
        return reviewUseCases.listUserReviews(userId);
    }

    @PostMapping("/{reviewId}/moderate")
    public ReviewResponse moderateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ModerateReviewRequest request) {
        return reviewUseCases.moderateReview(reviewId, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
