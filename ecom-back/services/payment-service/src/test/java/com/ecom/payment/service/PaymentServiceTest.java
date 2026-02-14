package com.ecom.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.entity.PaymentRecord;
import com.ecom.payment.entity.PaymentStatus;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.repository.WebhookEventRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentService paymentService = new PaymentService(
            paymentRepository,
            webhookEventRepository,
            outboxService,
            "payment.authorized.v1",
            "payment.failed.v1");

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
}
