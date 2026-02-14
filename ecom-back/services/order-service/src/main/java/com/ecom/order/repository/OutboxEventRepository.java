package com.ecom.order.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.OutboxEventRecord;
import com.ecom.order.entity.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventRecord, String> {

    List<OutboxEventRecord> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEventRecord> findTop100ByStatusOrderByUpdatedAtAsc(OutboxStatus status);

    long countByStatus(OutboxStatus status);

    long deleteByStatusAndUpdatedAtBefore(OutboxStatus status, Instant cutoff);
}
