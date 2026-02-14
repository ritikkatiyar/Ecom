package com.ecom.order.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.OrderStatus;
import com.ecom.order.entity.OrderRecord;

public interface OrderRepository extends JpaRepository<OrderRecord, String> {
    List<OrderRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OrderRecord> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(OrderStatus status, Instant updatedAtBefore);
}
