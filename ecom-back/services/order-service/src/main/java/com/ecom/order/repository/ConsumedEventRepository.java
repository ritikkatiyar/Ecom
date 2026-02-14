package com.ecom.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventRecord, String> {
}
