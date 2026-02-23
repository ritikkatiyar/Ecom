package com.ecom.gateway.filter;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class AuthValidationClient {

    private static final Logger log = LoggerFactory.getLogger(AuthValidationClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public AuthValidationClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.gateway.auth.validate-url:http://localhost:8081/api/auth/validate}") String validateUrl,
            @Value("${app.gateway.auth.validate-timeout-seconds:15}") int timeoutSeconds) {
        this.webClient = webClientBuilder
                .baseUrl(validateUrl)
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public Mono<Boolean> isActive(String authorization) {
        return webClient.get()
                .uri("")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(timeout)
                .map(body -> {
                    boolean active = body.path("active").asBoolean(false);
                    if (!active) {
                        log.info("auth validate returned active=false (token invalid/expired/blacklisted)");
                    }
                    return active;
                });
    }
}
