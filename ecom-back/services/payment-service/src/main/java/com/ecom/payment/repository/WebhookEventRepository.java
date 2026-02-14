package com.ecom.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.payment.entity.WebhookEventRecord;

public interface WebhookEventRepository extends JpaRepository<WebhookEventRecord, String> {
}
