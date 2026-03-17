package com.ecom.inventory.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryLockService {

    private static final Logger log = LoggerFactory.getLogger(InventoryLockService.class);
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Instant> fallbackLocks = new ConcurrentHashMap<>();

    public InventoryLockService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String sku) {
        if (redisTemplate == null) {
            return acquireFallback(sku);
        }
        String key = key(sku);
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    String.valueOf(Instant.now().toEpochMilli()),
                    LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for inventory lock; using in-memory fallback: {}", ex.getMessage());
            return acquireFallback(sku);
        }
    }

    public void release(String sku) {
        if (redisTemplate == null) {
            fallbackLocks.remove(key(sku));
            return;
        }
        try {
            redisTemplate.delete(key(sku));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for inventory unlock; using in-memory fallback: {}", ex.getMessage());
            fallbackLocks.remove(key(sku));
        }
    }

    private String key(String sku) {
        return "inventory:lock:" + sku;
    }

    private boolean acquireFallback(String sku) {
        String key = key(sku);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(LOCK_TTL);
        Instant result = fallbackLocks.compute(key, (ignored, currentExpiry) -> {
            if (currentExpiry == null || !currentExpiry.isAfter(now)) {
                return expiresAt;
            }
            return currentExpiry;
        });
        return expiresAt.equals(result);
    }
}
