package com.ecom.payment.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecom.payment.entity.PaymentRecord;

@Component
public class PaymentResultPublisher {

    private final OutboxService outboxService;
    private final String paymentAuthorizedTopic;
    private final String paymentFailedTopic;

    public PaymentResultPublisher(
            OutboxService outboxService,
            @Value("${app.kafka.topics.payment-authorized:payment.authorized.v1}") String paymentAuthorizedTopic,
            @Value("${app.kafka.topics.payment-failed:payment.failed.v1}") String paymentFailedTopic) {
        this.outboxService = outboxService;
        this.paymentAuthorizedTopic = paymentAuthorizedTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    public void publish(PaymentRecord payment, String status, String reason) {
        PaymentResultPayload payload = new PaymentResultPayload(
                payment.getOrderId(),
                payment.getId(),
                payment.getProviderPaymentId(),
                status,
                reason,
                Instant.now());

        String eventType = "AUTHORIZED".equals(status) ? "payment.authorized.v1" : "payment.failed.v1";
        String topic = "AUTHORIZED".equals(status) ? paymentAuthorizedTopic : paymentFailedTopic;
        outboxService.enqueue(topic, payment.getOrderId(), eventType, payload, "payment-service");
    }
}
