package com.ecom.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.dto.WebhookAckResponse;
import com.ecom.payment.service.PaymentUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentUseCases paymentService;
    private final String webhookSecret;

    public PaymentController(PaymentUseCases paymentService,
                             @Value("${app.payment.webhook-secret:dev-webhook-secret}") String webhookSecret) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/intents")
    public ResponseEntity<PaymentResponse> createIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createIntent(request));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable String paymentId) {
        return paymentService.getById(paymentId);
    }

    @PostMapping("/webhook")
    public WebhookAckResponse webhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @Valid @RequestBody PaymentWebhookRequest request) {
        if (signature != null && !signature.isBlank() && !signature.equals(webhookSecret)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
        String status = paymentService.handleWebhook(request);
        return new WebhookAckResponse(status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
