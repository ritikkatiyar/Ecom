package com.ecom.gateway.filter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class AuthValidationClient {

    private final WebClient webClient;

    public AuthValidationClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.gateway.auth.validate-url:http://localhost:8081/api/auth/validate}") String validateUrl) {
        this.webClient = webClientBuilder
                .baseUrl(validateUrl)
                .build();
    }

    public Mono<Boolean> isActive(String authorization) {
        return webClient.get()
                .uri("")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .map(body -> body.path("active").asBoolean(false));
    }
}
