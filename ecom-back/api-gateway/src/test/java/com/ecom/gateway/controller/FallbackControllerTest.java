package com.ecom.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ecom.gateway.config.SecurityConfig;

@WebFluxTest(FallbackController.class)
@Import(SecurityConfig.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnStandardizedServiceUnavailablePayload() {
        webTestClient.get()
                .uri("/fallback/payment")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("payment");
    }
}
