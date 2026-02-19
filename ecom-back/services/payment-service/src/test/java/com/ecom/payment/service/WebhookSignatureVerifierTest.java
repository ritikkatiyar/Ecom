package com.ecom.payment.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("test-secret");

    @Test
    void validSignatureShouldPass() throws Exception {
        String payload = "{\"providerEventId\":\"ev-1\"}";
        String signature = hmacSha256Hex(payload, "test-secret");
        assertTrue(verifier.isValid(payload, signature));
    }

    @Test
    void invalidSignatureShouldFail() {
        assertFalse(verifier.isValid("{\"x\":1}", "not-valid"));
    }

    @Test
    void missingInputsShouldFail() {
        assertFalse(verifier.isValid(null, "abc"));
        assertFalse(verifier.isValid("{}", null));
        assertFalse(verifier.isValid("{}", " "));
    }

    private String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
