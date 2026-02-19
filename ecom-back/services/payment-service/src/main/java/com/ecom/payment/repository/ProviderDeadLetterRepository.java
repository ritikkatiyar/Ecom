package com.ecom.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.payment.entity.ProviderDeadLetterRecord;

public interface ProviderDeadLetterRepository extends JpaRepository<ProviderDeadLetterRecord, Long> {

    List<ProviderDeadLetterRecord> findAllByOrderByCreatedAtDesc();
}
