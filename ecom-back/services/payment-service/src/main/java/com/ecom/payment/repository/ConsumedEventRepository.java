package com.ecom.payment.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.payment.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventRecord, String> {

    long deleteByConsumedAtBefore(Instant cutoff);
}
