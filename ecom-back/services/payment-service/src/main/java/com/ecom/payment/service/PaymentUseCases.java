package com.ecom.payment.service;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.dto.ProviderDeadLetterResponse;

import java.util.List;

public interface PaymentUseCases {

    PaymentResponse createIntent(CreatePaymentIntentRequest request);

    PaymentResponse getById(String paymentId);

    String handleWebhook(PaymentWebhookRequest request);

    void createPendingForOrder(String orderId, Long userId, String currency);

    List<ProviderDeadLetterResponse> listProviderDeadLetters();

    PaymentResponse requeueProviderDeadLetter(Long deadLetterId);

    boolean setProviderOutageMode(boolean enabled);

    boolean getProviderOutageMode();
}
