package com.ecom.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ecom.payment.service.PaymentUseCases;
import com.ecom.payment.service.WebhookSignatureVerifier;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentUseCases paymentUseCases;

    @MockBean
    private WebhookSignatureVerifier signatureVerifier;

    @Test
    void webhookShouldRejectMissingSignature() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing webhook signature"));
    }

    @Test
    void webhookShouldRejectInvalidSignature() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid webhook signature"));
    }

    @Test
    void webhookShouldRejectMalformedPayload() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "signed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerEventId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid webhook payload"));
    }

    @Test
    void webhookShouldProcessWithValidSignature() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(true);
        when(paymentUseCases.handleWebhook(any())).thenReturn("processed");

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "signed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"processed\"}"));
    }

    private String validPayload() {
        return """
                {
                  "providerEventId": "ev-123",
                  "providerPaymentId": "rzp_123",
                  "eventType": "payment.authorized",
                  "failureReason": null
                }
                """;
    }
}
