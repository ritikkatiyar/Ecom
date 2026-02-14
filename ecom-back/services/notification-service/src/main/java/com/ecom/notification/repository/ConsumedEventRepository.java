package com.ecom.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.notification.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventRecord, String> {
}
