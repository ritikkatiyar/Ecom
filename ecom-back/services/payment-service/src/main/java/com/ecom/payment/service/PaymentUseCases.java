package com.ecom.payment.service;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.dto.ProviderDeadLetterResponse;

import java.util.List;

public interface PaymentUseCases {

    /**
     * Creates or reuses a payment intent for an order checkout attempt.
     */
    PaymentResponse createIntent(CreatePaymentIntentRequest request);

    /**
     * Returns the persisted state of one payment record.
     */
    PaymentResponse getById(String paymentId);

    /**
     * Applies an incoming provider webhook and emits the resulting payment event when relevant.
     */
    String handleWebhook(PaymentWebhookRequest request);

    /**
     * Creates a placeholder pending payment record for an order when needed.
     */
    void createPendingForOrder(String orderId, Long userId, String currency);

    /**
     * Lists provider dead-letter records in reverse chronological order.
     */
    List<ProviderDeadLetterResponse> listProviderDeadLetters();

    /**
     * Requeues one provider dead-letter record and recreates a live payment when possible.
     */
    PaymentResponse requeueProviderDeadLetter(Long deadLetterId);

    /**
     * Toggles simulated provider outage mode for drills and local testing.
     */
    boolean setProviderOutageMode(boolean enabled);

    /**
     * Returns whether simulated provider outage mode is currently enabled.
     */
    boolean getProviderOutageMode();
}
