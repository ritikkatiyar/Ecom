package com.ecom.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.dto.ProviderDeadLetterResponse;
import com.ecom.payment.entity.PaymentRecord;
import com.ecom.payment.entity.PaymentStatus;
import com.ecom.payment.entity.ProviderDeadLetterRecord;
import com.ecom.payment.entity.WebhookEventRecord;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.repository.ProviderDeadLetterRepository;
import com.ecom.payment.repository.WebhookEventRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class PaymentService implements PaymentUseCases {

    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ProviderDeadLetterRepository deadLetterRepository;
    private final OutboxService outboxService;
    private final PaymentProviderGateway providerGateway;
    private final MeterRegistry meterRegistry;
    private final String paymentAuthorizedTopic;
    private final String paymentFailedTopic;
    private final int providerMaxAttempts;

    public PaymentService(
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            ProviderDeadLetterRepository deadLetterRepository,
            OutboxService outboxService,
            PaymentProviderGateway providerGateway,
            MeterRegistry meterRegistry,
            @Value("${app.kafka.topics.payment-authorized:payment.authorized.v1}") String paymentAuthorizedTopic,
            @Value("${app.kafka.topics.payment-failed:payment.failed.v1}") String paymentFailedTopic,
            @Value("${app.payment.provider.max-attempts:3}") int providerMaxAttempts) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.outboxService = outboxService;
        this.providerGateway = providerGateway;
        this.meterRegistry = meterRegistry;
        this.paymentAuthorizedTopic = paymentAuthorizedTopic;
        this.paymentFailedTopic = paymentFailedTopic;
        this.providerMaxAttempts = Math.max(1, providerMaxAttempts);
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
        record.setProviderPaymentId(createProviderPaymentIdWithRetry(request));

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

    @Override
    @Transactional(readOnly = true)
    public List<ProviderDeadLetterResponse> listProviderDeadLetters() {
        return deadLetterRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse requeueProviderDeadLetter(Long deadLetterId) {
        ProviderDeadLetterRecord deadLetter = deadLetterRepository.findById(deadLetterId)
                .orElseThrow(() -> new IllegalArgumentException("Provider dead-letter record not found"));
        if (!"PENDING".equals(deadLetter.getStatus())) {
            throw new IllegalArgumentException("Provider dead-letter record is not requeueable");
        }

        var existing = paymentRepository.findByIdempotencyKey(deadLetter.getIdempotencyKey());
        if (existing.isPresent()) {
            deadLetter.setStatus("REQUEUED");
            deadLetter.setResolvedAt(Instant.now());
            deadLetter.setRequeuedPaymentId(existing.get().getId());
            deadLetterRepository.save(deadLetter);
            meterRegistry.counter("payment.provider.requeue.total", "result", "already_exists").increment();
            return toResponse(existing.get());
        }

        try {
            String providerPaymentId = providerGateway.createPaymentId(
                    deadLetter.getOrderId(),
                    deadLetter.getAmount(),
                    deadLetter.getCurrency());

            PaymentRecord record = new PaymentRecord();
            record.setOrderId(deadLetter.getOrderId());
            record.setUserId(deadLetter.getUserId());
            record.setAmount(deadLetter.getAmount().setScale(2, RoundingMode.HALF_UP));
            record.setCurrency(deadLetter.getCurrency().toUpperCase());
            record.setStatus(PaymentStatus.PENDING);
            record.setIdempotencyKey(deadLetter.getIdempotencyKey());
            record.setProviderPaymentId(providerPaymentId);
            PaymentRecord saved = paymentRepository.save(record);

            deadLetter.setStatus("REQUEUED");
            deadLetter.setResolvedAt(Instant.now());
            deadLetter.setRequeuedPaymentId(saved.getId());
            deadLetterRepository.save(deadLetter);
            meterRegistry.counter("payment.provider.requeue.total", "result", "success").increment();
            return toResponse(saved);
        } catch (Exception ex) {
            deadLetter.setAttempts(deadLetter.getAttempts() + 1);
            deadLetter.setFailureReason(limitFailureReason(ex.getMessage()));
            deadLetterRepository.save(deadLetter);
            meterRegistry.counter("payment.provider.requeue.total", "result", "failed").increment();
            throw new IllegalStateException("Provider requeue failed");
        }
    }

    @Override
    public boolean setProviderOutageMode(boolean enabled) {
        providerGateway.setOutageMode(enabled);
        meterRegistry.counter("payment.provider.outage.toggle.total", "enabled", String.valueOf(enabled)).increment();
        return providerGateway.isOutageMode();
    }

    @Override
    public boolean getProviderOutageMode() {
        return providerGateway.isOutageMode();
    }

    private String createProviderPaymentIdWithRetry(CreatePaymentIntentRequest request) {
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
        moveToProviderDeadLetter(request, lastError);
        meterRegistry.counter("payment.provider.dlq.total").increment();
        throw new IllegalStateException("Payment provider unavailable; request moved to DLQ");
    }

    private void moveToProviderDeadLetter(CreatePaymentIntentRequest request, Exception lastError) {
        ProviderDeadLetterRecord deadLetter = new ProviderDeadLetterRecord();
        deadLetter.setIdempotencyKey(request.idempotencyKey());
        deadLetter.setOrderId(request.orderId());
        deadLetter.setUserId(request.userId());
        deadLetter.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        deadLetter.setCurrency(request.currency().toUpperCase());
        deadLetter.setAttempts(providerMaxAttempts);
        deadLetter.setStatus("PENDING");
        deadLetter.setFailureReason(limitFailureReason(lastError == null ? "Unknown provider failure" : lastError.getMessage()));
        deadLetterRepository.save(deadLetter);
    }

    private String limitFailureReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 250 ? reason.substring(0, 250) : reason;
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

    private ProviderDeadLetterResponse toResponse(ProviderDeadLetterRecord deadLetter) {
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
