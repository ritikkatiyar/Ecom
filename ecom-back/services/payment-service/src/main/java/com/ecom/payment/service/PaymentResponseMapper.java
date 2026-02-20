package com.ecom.payment.service;

import org.springframework.stereotype.Component;

import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.ProviderDeadLetterResponse;
import com.ecom.payment.entity.PaymentRecord;
import com.ecom.payment.entity.ProviderDeadLetterRecord;

@Component
public class PaymentResponseMapper {

    public PaymentResponse toResponse(PaymentRecord payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getProviderPaymentId(),
                payment.getIdempotencyKey(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    public ProviderDeadLetterResponse toResponse(ProviderDeadLetterRecord deadLetter) {
        return new ProviderDeadLetterResponse(
                deadLetter.getId(),
                deadLetter.getIdempotencyKey(),
                deadLetter.getOrderId(),
                deadLetter.getUserId(),
                deadLetter.getAmount(),
                deadLetter.getCurrency(),
                deadLetter.getAttempts(),
                deadLetter.getStatus(),
                deadLetter.getFailureReason(),
                deadLetter.getRequeuedPaymentId(),
                deadLetter.getCreatedAt(),
                deadLetter.getUpdatedAt(),
                deadLetter.getResolvedAt());
    }
}
