package com.ecom.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ecom.common.redis.RedisFallbackOperations;

/**
 * Token blacklist for logout. Uses Redis with generic fallback when Redis unavailable.
 * FAANG-style: when Redis down, blacklist check assumes "not blacklisted" (validation proceeds).
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:access:";

    private final RedisFallbackOperations redis;

    public TokenBlacklistService(RedisFallbackOperations redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.set(key(jti), "1", ttl);
    }

    /**
     * Returns true if token is blacklisted. When Redis unavailable, returns false
     * (graceful degradation: allow validation to proceed).
     */
    public boolean isBlacklisted(String jti) {
        return redis.hasKey(key(jti));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
