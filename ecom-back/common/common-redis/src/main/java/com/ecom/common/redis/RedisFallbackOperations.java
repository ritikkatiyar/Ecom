package com.ecom.common.redis;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Generic Redis operations with fallback when Redis is unavailable.
 * FAANG-style: optional dependency with graceful degradation.
 *
 * <ul>
 *   <li>When Redis is null (not configured): all ops no-op or return safe defaults.</li>
 *   <li>When Redis throws: catch, log, return fallback value / no-op.</li>
 * </ul>
 *
 * Use for: token blacklist, session store, rate limits, cache—any Redis-dependent feature.
 */
public final class RedisFallbackOperations {

    private static final Logger log = LoggerFactory.getLogger(RedisFallbackOperations.class);

    private final StringRedisTemplate redis;
    private final boolean redisAvailable;

    public RedisFallbackOperations(StringRedisTemplate redis) {
        this.redis = redis;
        this.redisAvailable = redis != null;
    }

    /**
     * Set key=value with TTL. On Redis unavailability: no-op (graceful degradation).
     */
    public void set(String key, String value, Duration ttl) {
        if (!redisAvailable) {
            return;
        }
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, skipping set key={}: {}", key, e.getMessage());
        }
    }

    /**
     * Check if key exists. On Redis unavailability: returns false (assume absent).
     * Use when "absent" is the safe default for your feature.
     */
    public boolean hasKey(String key) {
        if (!redisAvailable) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(key));
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, assuming key absent: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Delete key. On Redis unavailability: no-op.
     */
    public void delete(String key) {
        if (!redisAvailable) {
            return;
        }
        try {
            redis.delete(key);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, skipping delete key={}: {}", key, e.getMessage());
        }
    }

    /**
     * Execute a Redis read operation with fallback on error.
     * @param op Redis operation
     * @param onError value to return when Redis throws
     * @return result of op, or onError on exception
     */
    public <T> T getOrDefault(RedisOp<T> op, T onError) {
        if (!redisAvailable) {
            return onError;
        }
        try {
            return op.execute(redis);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, using fallback: {}", e.getMessage());
            return onError;
        }
    }

    /**
     * Execute a Redis write operation; no-op on error.
     */
    public void runOrNoOp(RedisVoidOp op) {
        if (!redisAvailable) {
            return;
        }
        try {
            op.execute(redis);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, skipping write: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    public interface RedisOp<T> {
        T execute(StringRedisTemplate redis);
    }

    @FunctionalInterface
    public interface RedisVoidOp {
        void execute(StringRedisTemplate redis);
    }
}
