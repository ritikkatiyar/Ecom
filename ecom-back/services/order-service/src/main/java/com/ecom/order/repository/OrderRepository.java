package com.ecom.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.OrderRecord;

public interface OrderRepository extends JpaRepository<OrderRecord, String> {
    List<OrderRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
