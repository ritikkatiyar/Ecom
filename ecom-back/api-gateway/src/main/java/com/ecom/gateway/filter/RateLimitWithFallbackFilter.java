package com.ecom.gateway.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Rate limit filter with Redis + in-memory fallback.
 * When mode=redis: tries Redis first; on Redis error, falls back to in-memory.
 * When mode=in-memory: uses in-memory only.
 * Gateway starts even when Redis is unavailable.
 */
@Component
public class RateLimitWithFallbackFilter implements GlobalFilter, Ordered {

    private static final String REDIS_KEY_PREFIX = "ratelimit:";
    private static final int ORDER = -200;

    private static final class CounterWindow {
        private volatile long windowStartMillis;
        private final AtomicInteger requests = new AtomicInteger(0);
    }

    private final ReactiveStringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final long windowSeconds;
    private final String mode;
    private final GatewayErrorWriter gatewayErrorWriter;
    private final Map<String, CounterWindow> inMemoryWindows = new ConcurrentHashMap<>();

    public RateLimitWithFallbackFilter(
            @Autowired(required = false) ReactiveStringRedisTemplate redisTemplate,
            @Value("${app.gateway.rate-limit.max-requests:120}") int maxRequests,
            @Value("${app.gateway.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${app.gateway.rate-limit.mode:redis}") String mode,
            GatewayErrorWriter gatewayErrorWriter) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.mode = mode != null ? mode : "redis";
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/internal/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientKey(exchange);

        if ("redis".equalsIgnoreCase(mode) && redisTemplate != null) {
            return tryRedisThenFallback(exchange, chain, clientKey);
        }
        return applyInMemory(exchange, chain, clientKey);
    }

    private Mono<Void> tryRedisThenFallback(
            ServerWebExchange exchange, GatewayFilterChain chain, String clientKey) {
        String redisKey = REDIS_KEY_PREFIX + clientKey;
        return redisIncrement(redisKey)
                .flatMap(count -> {
                    if (count > maxRequests) {
                        return gatewayErrorWriter.write(
                                exchange,
                                HttpStatus.TOO_MANY_REQUESTS,
                                "RATE_LIMIT_EXCEEDED",
                                "Rate limit exceeded. Try again later.");
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> applyInMemory(exchange, chain, clientKey));
    }

    private Mono<Long> redisIncrement(String key) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                });
    }

    private Mono<Void> applyInMemory(
            ServerWebExchange exchange, GatewayFilterChain chain, String clientKey) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = windowSeconds * 1000L;

        CounterWindow window = inMemoryWindows.computeIfAbsent(clientKey, k -> {
            CounterWindow w = new CounterWindow();
            w.windowStartMillis = now;
            return w;
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

    private String resolveClientKey(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return comma > 0 ? forwardedFor.substring(0, comma).trim() : forwardedFor.trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
