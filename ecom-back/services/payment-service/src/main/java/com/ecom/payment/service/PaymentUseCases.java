package com.ecom.payment.service;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;

public interface PaymentUseCases {

    PaymentResponse createIntent(CreatePaymentIntentRequest request);

    PaymentResponse getById(String paymentId);

    String handleWebhook(PaymentWebhookRequest request);

    void createPendingForOrder(String orderId, Long userId, String currency);
}
