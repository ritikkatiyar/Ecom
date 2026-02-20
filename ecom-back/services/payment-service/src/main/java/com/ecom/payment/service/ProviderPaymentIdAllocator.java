package com.ecom.payment.service;

import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.entity.ProviderDeadLetterRecord;
import com.ecom.payment.repository.ProviderDeadLetterRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ProviderPaymentIdAllocator {

    private final ProviderDeadLetterRepository deadLetterRepository;
    private final PaymentProviderGateway providerGateway;
    private final MeterRegistry meterRegistry;
    private final int providerMaxAttempts;

    public ProviderPaymentIdAllocator(
            ProviderDeadLetterRepository deadLetterRepository,
            PaymentProviderGateway providerGateway,
            MeterRegistry meterRegistry,
            @Value("${app.payment.provider.max-attempts:3}") int providerMaxAttempts) {
        this.deadLetterRepository = deadLetterRepository;
        this.providerGateway = providerGateway;
        this.meterRegistry = meterRegistry;
        this.providerMaxAttempts = Math.max(1, providerMaxAttempts);
    }

    public String allocate(CreatePaymentIntentRequest request) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= providerMaxAttempts; attempt++) {
            try {
                return providerGateway.createPaymentId(
                        request.orderId(),
                        request.amount().setScale(2, RoundingMode.HALF_UP),
                        request.currency().toUpperCase());
            } catch (Exception ex) {
                lastError = ex;
                meterRegistry.counter("payment.provider.retry.total").increment();
            }
        }

        ProviderDeadLetterRecord deadLetter = new ProviderDeadLetterRecord();
        deadLetter.setIdempotencyKey(request.idempotencyKey());
        deadLetter.setOrderId(request.orderId());
        deadLetter.setUserId(request.userId());
        deadLetter.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        deadLetter.setCurrency(request.currency().toUpperCase());
        deadLetter.setAttempts(providerMaxAttempts);
        deadLetter.setStatus("PENDING");
        deadLetter.setFailureReason(sanitizeFailureReason(lastError == null ? "Unknown provider failure" : lastError.getMessage()));
        deadLetterRepository.save(deadLetter);

        meterRegistry.counter("payment.provider.dlq.total").increment();
        throw new IllegalStateException("Payment provider unavailable; request moved to DLQ");
    }

    public String sanitizeFailureReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 250 ? reason.substring(0, 250) : reason;
    }
}
