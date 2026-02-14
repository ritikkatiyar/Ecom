package com.ecom.payment.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.payment.entity.OutboxEventRecord;
import com.ecom.payment.entity.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventRecord, String> {

    List<OutboxEventRecord> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    long deleteByStatusAndUpdatedAtBefore(OutboxStatus status, Instant cutoff);
}
