package com.ecom.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.review.entity.ReviewRecord;
import com.ecom.review.entity.ReviewStatus;

public interface ReviewRepository extends JpaRepository<ReviewRecord, Long> {

    Optional<ReviewRecord> findByIdAndUserId(Long id, Long userId);

    List<ReviewRecord> findByProductIdAndStatusOrderByCreatedAtDesc(String productId, ReviewStatus status);

    List<ReviewRecord> findByProductIdOrderByCreatedAtDesc(String productId);

    List<ReviewRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
