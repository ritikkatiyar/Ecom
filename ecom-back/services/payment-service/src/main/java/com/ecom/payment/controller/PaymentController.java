package com.ecom.payment.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.payment.dto.CreatePaymentIntentRequest;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.PaymentWebhookRequest;
import com.ecom.payment.dto.ProviderDeadLetterResponse;
import com.ecom.payment.dto.WebhookAckResponse;
import com.ecom.payment.service.PaymentUseCases;
import com.ecom.payment.service.WebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentUseCases paymentService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PaymentController(PaymentUseCases paymentService,
                             WebhookSignatureVerifier signatureVerifier,
                             ObjectMapper objectMapper,
                             Validator validator) {
        this.paymentService = paymentService;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.validator = validator;
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
            @RequestBody String rawPayload) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing webhook signature");
        }
        if (!signatureVerifier.isValid(rawPayload, signature)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
        PaymentWebhookRequest request = parsePayload(rawPayload);
        String status = paymentService.handleWebhook(request);
        return new WebhookAckResponse(status);
    }

    @GetMapping("/provider/dead-letters")
    public List<ProviderDeadLetterResponse> listProviderDeadLetters() {
        return paymentService.listProviderDeadLetters();
    }

    @PostMapping("/provider/dead-letters/{deadLetterId}/requeue")
    public PaymentResponse requeueProviderDeadLetter(@PathVariable Long deadLetterId) {
        return paymentService.requeueProviderDeadLetter(deadLetterId);
    }

    @PostMapping("/provider/outage-mode")
    public Map<String, Object> setProviderOutageMode(@RequestParam boolean enabled) {
        boolean current = paymentService.setProviderOutageMode(enabled);
        return Map.of("outageMode", current);
    }

    @GetMapping("/provider/outage-mode")
    public Map<String, Object> getProviderOutageMode() {
        return Map.of("outageMode", paymentService.getProviderOutageMode());
    }

    private PaymentWebhookRequest parsePayload(String rawPayload) {
        try {
            PaymentWebhookRequest request = objectMapper.readValue(rawPayload, PaymentWebhookRequest.class);
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException("Invalid webhook payload");
            }
            return request;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid webhook payload");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
