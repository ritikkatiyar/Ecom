package com.ecom.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class ApiVersionFilter implements GlobalFilter, Ordered {

    private final String requiredVersion;
    private final GatewayErrorWriter gatewayErrorWriter;

    public ApiVersionFilter(
            @Value("${app.gateway.api-version.required:v1}") String requiredVersion,
            GatewayErrorWriter gatewayErrorWriter) {
        this.requiredVersion = requiredVersion;
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        String version = exchange.getRequest().getHeaders().getFirst("X-API-Version");
        if (requiredVersion.equalsIgnoreCase(version)) {
            return chain.filter(exchange);
        }

        return gatewayErrorWriter.write(
                exchange,
                HttpStatus.BAD_REQUEST,
                "API_VERSION_MISMATCH",
                "X-API-Version must be '" + requiredVersion + "'");
    }

    @Override
    public int getOrder() {
        return -250;
    }
}
