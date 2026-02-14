package com.ecom.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.OutboxEventRecord;
import com.ecom.order.entity.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventRecord, String> {

    List<OutboxEventRecord> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
