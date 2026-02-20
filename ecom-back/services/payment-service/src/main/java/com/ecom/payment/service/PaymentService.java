package com.ecom.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    private final PaymentProviderGateway providerGateway;
    private final ProviderPaymentIdAllocator providerPaymentIdAllocator;
    private final PaymentResultPublisher paymentResultPublisher;
    private final PaymentResponseMapper paymentResponseMapper;
    private final MeterRegistry meterRegistry;

    public PaymentService(
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            ProviderDeadLetterRepository deadLetterRepository,
            PaymentProviderGateway providerGateway,
            ProviderPaymentIdAllocator providerPaymentIdAllocator,
            PaymentResultPublisher paymentResultPublisher,
            PaymentResponseMapper paymentResponseMapper,
            MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.providerGateway = providerGateway;
        this.providerPaymentIdAllocator = providerPaymentIdAllocator;
        this.paymentResultPublisher = paymentResultPublisher;
        this.paymentResponseMapper = paymentResponseMapper;
        this.meterRegistry = meterRegistry;
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
        record.setProviderPaymentId(providerPaymentIdAllocator.allocate(request));

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
            paymentResultPublisher.publish(record, "AUTHORIZED", null);
            return "processed";
        }

        if ("payment.failed".equalsIgnoreCase(request.eventType())) {
            record.setStatus(PaymentStatus.FAILED);
            record.setFailureReason(request.failureReason());
            record.setProviderEventId(request.providerEventId());
            paymentRepository.save(record);
            paymentResultPublisher.publish(record, "FAILED", request.failureReason());
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
            deadLetter.setFailureReason(providerPaymentIdAllocator.sanitizeFailureReason(ex.getMessage()));
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

    private PaymentResponse toResponse(PaymentRecord p) {
        return paymentResponseMapper.toResponse(p);
    }

    private ProviderDeadLetterResponse toResponse(ProviderDeadLetterRecord deadLetter) {
        return paymentResponseMapper.toResponse(deadLetter);
    }
}
