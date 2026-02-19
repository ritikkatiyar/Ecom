package com.ecom.gateway.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ecom.gateway.config.SecurityConfig;
import com.ecom.gateway.service.ReleaseGateMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(ReleaseGateCallbackController.class)
@Import(SecurityConfig.class)
class ReleaseGateCallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReleaseGateMetricsService releaseGateMetricsService;

    @Test
    void shouldRecordCallbackAndReturnAccepted() {
        String payload = """
                {
                  "event":"rollback_verification",
                  "environment":"staging",
                  "status":"triggered",
                  "ref":"refs/heads/main",
                  "runId":"1234",
                  "details":"smoke_failure_rollback_triggered"
                }
                """;

        webTestClient.post()
                .uri("/internal/release-gate/callbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.status").isEqualTo("accepted")
                .jsonPath("$.event").isEqualTo("rollback_verification")
                .jsonPath("$.environment").isEqualTo("staging")
                .jsonPath("$.callbackStatus").isEqualTo("triggered");

        verify(releaseGateMetricsService)
                .recordCallback(eq("rollback_verification"), eq("staging"), eq("triggered"));
    }

    @Test
    void shouldRejectCallbackWhenRequiredFieldsMissing() {
        String payload = """
                {
                  "event":"",
                  "environment":"production",
                  "status":""
                }
                """;

        webTestClient.post()
                .uri("/internal/release-gate/callbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("event, environment, and status are required");

        verifyNoInteractions(releaseGateMetricsService);
    }
}
