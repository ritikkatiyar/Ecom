package com.ecom.gateway.filter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "app.gateway.rate-limit.mode", havingValue = "in-memory")
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final class CounterWindow {
        private volatile long windowStartMillis;
        private final AtomicInteger requests = new AtomicInteger(0);
    }

    private final Map<String, CounterWindow> clientWindows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;
    private final GatewayErrorWriter gatewayErrorWriter;

    public RateLimitingFilter(
            @Value("${app.gateway.rate-limit.max-requests:120}") int maxRequests,
            @Value("${app.gateway.rate-limit.window-seconds:60}") int windowSeconds,
            GatewayErrorWriter gatewayErrorWriter) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/internal/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        String client = clientKey(exchange);
        long now = Instant.now().toEpochMilli();

        CounterWindow window = clientWindows.computeIfAbsent(client, key -> {
            CounterWindow created = new CounterWindow();
            created.windowStartMillis = now;
            return created;
        });

        synchronized (window) {
            if (now - window.windowStartMillis >= windowMillis) {
                window.windowStartMillis = now;
                window.requests.set(0);
            }

            int current = window.requests.incrementAndGet();
            if (current > maxRequests) {
                return gatewayErrorWriter.write(
                        exchange,
                        HttpStatus.TOO_MANY_REQUESTS,
                        "RATE_LIMIT_EXCEEDED",
                        "Rate limit exceeded. Try again later.");
            }
        }

        return chain.filter(exchange);
    }

    private String clientKey(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return comma > 0 ? forwardedFor.substring(0, comma).trim() : forwardedFor.trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return String.valueOf(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
