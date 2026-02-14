package com.ecom.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.entity.PaymentRecord;
import com.ecom.payment.entity.PaymentStatus;
import com.ecom.payment.entity.WebhookEventRecord;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.repository.WebhookEventRepository;

@Service
public class PaymentService implements PaymentUseCases {

    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final OutboxService outboxService;
    private final String paymentAuthorizedTopic;
    private final String paymentFailedTopic;

    public PaymentService(
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            OutboxService outboxService,
            @Value("${app.kafka.topics.payment-authorized:payment.authorized.v1}") String paymentAuthorizedTopic,
            @Value("${app.kafka.topics.payment-failed:payment.failed.v1}") String paymentFailedTopic) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.outboxService = outboxService;
        this.paymentAuthorizedTopic = paymentAuthorizedTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    @Transactional
    public PaymentResponse createIntent(CreatePaymentIntentRequest request) {
        if (request.currency().length() != 3) {
            throw new IllegalArgumentException("Currency must be 3 letters");
        }

        var existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(request.orderId());
        record.setUserId(request.userId());
        record.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        record.setCurrency(request.currency().toUpperCase());
        record.setStatus(PaymentStatus.PENDING);
        record.setIdempotencyKey(request.idempotencyKey());
        record.setProviderPaymentId("rzp_" + UUID.randomUUID().toString().replace("-", ""));

        return toResponse(paymentRepository.save(record));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(String paymentId) {
        return toResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found")));
    }

    @Transactional
    public String handleWebhook(PaymentWebhookRequest request) {
        if (webhookEventRepository.existsById(request.providerEventId())) {
            return "already_processed";
        }

        PaymentRecord record = paymentRepository.findByProviderPaymentId(request.providerPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for providerPaymentId"));

        WebhookEventRecord webhook = new WebhookEventRecord();
        webhook.setProviderEventId(request.providerEventId());
        webhookEventRepository.save(webhook);

        if ("payment.authorized".equalsIgnoreCase(request.eventType())) {
            record.setStatus(PaymentStatus.AUTHORIZED);
            record.setFailureReason(null);
            record.setProviderEventId(request.providerEventId());
            paymentRepository.save(record);
            publishResult(record, "AUTHORIZED", null);
            return "processed";
        }

        if ("payment.failed".equalsIgnoreCase(request.eventType())) {
            record.setStatus(PaymentStatus.FAILED);
            record.setFailureReason(request.failureReason());
            record.setProviderEventId(request.providerEventId());
            paymentRepository.save(record);
            publishResult(record, "FAILED", request.failureReason());
            return "processed";
        }

        return "ignored";
    }

    @Transactional
    public void createPendingForOrder(String orderId, Long userId, String currency) {
        paymentRepository.findByOrderId(orderId).ifPresentOrElse(p -> {
        }, () -> {
            PaymentRecord record = new PaymentRecord();
            record.setOrderId(orderId);
            record.setUserId(userId);
            record.setAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            record.setCurrency(currency == null || currency.isBlank() ? "INR" : currency.toUpperCase());
            record.setStatus(PaymentStatus.PENDING);
            record.setIdempotencyKey("order:" + orderId);
            record.setProviderPaymentId("rzp_pending_" + UUID.randomUUID().toString().replace("-", ""));
            paymentRepository.save(record);
        });
    }

    private void publishResult(PaymentRecord record, String status, String reason) {
        PaymentResultPayload payload = new PaymentResultPayload(
                record.getOrderId(),
                record.getId(),
                record.getProviderPaymentId(),
                status,
                reason,
                Instant.now());

        String eventType = "AUTHORIZED".equals(status) ? "payment.authorized.v1" : "payment.failed.v1";
        String topic = "AUTHORIZED".equals(status) ? paymentAuthorizedTopic : paymentFailedTopic;
        outboxService.enqueue(topic, record.getOrderId(), eventType, payload, "payment-service");
    }

    private PaymentResponse toResponse(PaymentRecord p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getUserId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus().name(),
                p.getProviderPaymentId(),
                p.getIdempotencyKey(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
