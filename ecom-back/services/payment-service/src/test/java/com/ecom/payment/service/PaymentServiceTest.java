package com.ecom.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.entity.PaymentRecord;
import com.ecom.payment.entity.PaymentStatus;
import com.ecom.payment.entity.ProviderDeadLetterRecord;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.repository.ProviderDeadLetterRepository;
import com.ecom.payment.repository.WebhookEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ProviderDeadLetterRepository deadLetterRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PaymentProviderGateway providerGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        var meterRegistry = new SimpleMeterRegistry();
        ProviderPaymentIdAllocator providerPaymentIdAllocator =
                new ProviderPaymentIdAllocator(deadLetterRepository, providerGateway, meterRegistry, 3);
        PaymentResultPublisher paymentResultPublisher =
                new PaymentResultPublisher(outboxService, "payment.authorized.v1", "payment.failed.v1");
        PaymentResponseMapper paymentResponseMapper = new PaymentResponseMapper();

        paymentService = new PaymentService(
                paymentRepository,
                webhookEventRepository,
                deadLetterRepository,
                providerGateway,
                providerPaymentIdAllocator,
                paymentResultPublisher,
                paymentResponseMapper,
                meterRegistry);
    }

    @Test
    void createIntentIsIdempotent() {
        PaymentRecord existing = new PaymentRecord();
        existing.setId("pay_1");
        existing.setOrderId("ord_1");
        existing.setUserId(1L);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setCurrency("INR");
        existing.setStatus(PaymentStatus.PENDING);
        existing.setIdempotencyKey("idem-1");
        existing.setProviderPaymentId("rzp_1");
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        var res = paymentService.createIntent(new CreatePaymentIntentRequest("ord_1", 1L, new BigDecimal("100.00"), "INR", "idem-1"));
        assertEquals("pay_1", res.paymentId());
    }

    @Test
    void webhookAuthorizedUpdatesStatus() {
        PaymentRecord record = new PaymentRecord();
        record.setId("pay_2");
        record.setOrderId("ord_2");
        record.setUserId(2L);
        record.setAmount(new BigDecimal("10.00"));
        record.setCurrency("INR");
        record.setStatus(PaymentStatus.PENDING);
        record.setIdempotencyKey("idem-2");
        record.setProviderPaymentId("rzp_2");

        when(webhookEventRepository.existsById("ev-1")).thenReturn(false);
        when(paymentRepository.findByProviderPaymentId("rzp_2")).thenReturn(Optional.of(record));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(i -> i.getArgument(0));

        String status = paymentService.handleWebhook(new PaymentWebhookRequest("ev-1", "rzp_2", "payment.authorized", null));

        assertEquals("processed", status);
        assertEquals(PaymentStatus.AUTHORIZED, record.getStatus());
    }

    @Test
    void createIntentMovesToDeadLetterWhenProviderIsUnavailable() {
        CreatePaymentIntentRequest request =
                new CreatePaymentIntentRequest("ord_dlq", 3L, new BigDecimal("55.00"), "INR", "idem-dlq");

        when(paymentRepository.findByIdempotencyKey("idem-dlq")).thenReturn(Optional.empty());
        when(providerGateway.createPaymentId(any(), any(), any()))
                .thenThrow(new IllegalStateException("provider outage"));

        assertThrows(IllegalStateException.class, () -> paymentService.createIntent(request));
        verify(providerGateway, times(3)).createPaymentId(any(), any(), any());
        verify(deadLetterRepository, times(1)).save(any(ProviderDeadLetterRecord.class));
    }

    @Test
    void requeueProviderDeadLetterCreatesPendingPayment() {
        ProviderDeadLetterRecord deadLetter = new ProviderDeadLetterRecord();
        deadLetter.setId(11L);
        deadLetter.setIdempotencyKey("idem-requeue");
        deadLetter.setOrderId("ord_requeue");
        deadLetter.setUserId(10L);
        deadLetter.setAmount(new BigDecimal("30.00"));
        deadLetter.setCurrency("INR");
        deadLetter.setAttempts(3);
        deadLetter.setStatus("PENDING");

        PaymentRecord saved = new PaymentRecord();
        saved.setId("pay_requeue");
        saved.setOrderId("ord_requeue");
        saved.setUserId(10L);
        saved.setAmount(new BigDecimal("30.00"));
        saved.setCurrency("INR");
        saved.setStatus(PaymentStatus.PENDING);
        saved.setIdempotencyKey("idem-requeue");
        saved.setProviderPaymentId("rzp_new");
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());

        when(deadLetterRepository.findById(11L)).thenReturn(Optional.of(deadLetter));
        when(paymentRepository.findByIdempotencyKey("idem-requeue")).thenReturn(Optional.empty());
        when(providerGateway.createPaymentId(any(), any(), any())).thenReturn("rzp_new");
        when(paymentRepository.save(any(PaymentRecord.class))).thenReturn(saved);

        var response = paymentService.requeueProviderDeadLetter(11L);

        assertEquals("pay_requeue", response.paymentId());
        assertEquals("PENDING", response.status());
        verify(deadLetterRepository, times(1)).save(any(ProviderDeadLetterRecord.class));
    }
}
