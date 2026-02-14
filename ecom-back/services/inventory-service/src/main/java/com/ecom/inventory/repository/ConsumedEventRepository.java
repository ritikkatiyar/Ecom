package com.ecom.inventory.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.inventory.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventRecord, String> {

    long deleteByConsumedAtBefore(Instant cutoff);
}
