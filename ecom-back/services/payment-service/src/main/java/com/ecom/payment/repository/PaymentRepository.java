package com.ecom.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.payment.entity.PaymentRecord;

public interface PaymentRepository extends JpaRepository<PaymentRecord, String> {
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentRecord> findByProviderPaymentId(String providerPaymentId);
    Optional<PaymentRecord> findByOrderId(String orderId);
}
