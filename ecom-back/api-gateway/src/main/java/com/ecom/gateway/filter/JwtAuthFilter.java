package com.ecom.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final AuthValidationClient authValidationClient;
    private final GatewayAuthRoutePolicy gatewayAuthRoutePolicy;
    private final GatewayErrorWriter gatewayErrorWriter;

    public JwtAuthFilter(
            AuthValidationClient authValidationClient,
            GatewayAuthRoutePolicy gatewayAuthRoutePolicy,
            GatewayErrorWriter gatewayErrorWriter) {
        this.authValidationClient = authValidationClient;
        this.gatewayAuthRoutePolicy = gatewayAuthRoutePolicy;
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == HttpMethod.OPTIONS || !gatewayAuthRoutePolicy.isProtected(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.info("auth 401 AUTH_TOKEN_MISSING path={} method={}", path, method);
            return gatewayErrorWriter.write(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_MISSING",
                    "Bearer token is required for this endpoint.");
        }

        return authValidationClient.isActive(authorization)
                .flatMap(active -> {
                    if (!active) {
                        log.info("auth 401 AUTH_TOKEN_INVALID path={} method={} validateReturnedActive=false", path, method);
                        return gatewayErrorWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "AUTH_TOKEN_INVALID",
                                "Token is invalid, expired, or blacklisted.");
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.warn("auth 503 AUTH_VALIDATION_UNAVAILABLE path={} method={} error={}", path, method, ex.getMessage());
                    return gatewayErrorWriter.write(
                        exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AUTH_VALIDATION_UNAVAILABLE",
                        "Auth validation service is unavailable.");
                });
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
