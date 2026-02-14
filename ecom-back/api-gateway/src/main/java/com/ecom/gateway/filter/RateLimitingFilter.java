package com.ecom.gateway.filter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final class CounterWindow {
        private volatile long windowStartMillis;
        private final AtomicInteger requests = new AtomicInteger(0);
    }

    private final Map<String, CounterWindow> clientWindows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimitingFilter(
            @Value("${app.gateway.rate-limit.max-requests:120}") int maxRequests,
            @Value("${app.gateway.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
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
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
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
