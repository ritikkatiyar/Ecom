package com.ecom.gateway.filter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private final GatewayErrorWriter gatewayErrorWriter;

    public JwtAuthFilter(
            WebClient.Builder webClientBuilder,
            @Value("${app.gateway.auth.validate-url:http://localhost:8081/api/auth/validate}") String validateUrl,
            GatewayErrorWriter gatewayErrorWriter) {
        this.webClient = webClientBuilder
                .baseUrl(validateUrl)
                .build();
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == HttpMethod.OPTIONS || !isProtected(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return gatewayErrorWriter.write(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_MISSING",
                    "Bearer token is required for this endpoint.");
        }

        return webClient.get()
                .uri("")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .flatMap(body -> {
                    boolean active = body.path("active").asBoolean(false);
                    if (!active) {
                        return gatewayErrorWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "AUTH_TOKEN_INVALID",
                                "Token is invalid, expired, or blacklisted.");
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    return gatewayErrorWriter.write(
                            exchange,
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "AUTH_VALIDATION_UNAVAILABLE",
                            "Auth validation service is unavailable.");
                });
    }

    private boolean isProtected(String path, HttpMethod method) {
        if (path.startsWith("/api/cart")
                || path.startsWith("/api/orders")
                || path.startsWith("/api/payments")
                || path.startsWith("/api/inventory")
                || path.startsWith("/api/users")
                || path.startsWith("/api/reviews")) {
            return true;
        }

        if (path.startsWith("/api/products")) {
            return method != HttpMethod.GET;
        }

        if (path.startsWith("/api/search")) {
            return method != HttpMethod.GET;
        }

        return false;
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
