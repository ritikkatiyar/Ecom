package com.ecom.inventory.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;

    public InventoryLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String sku) {
        String key = key(sku);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key,
                String.valueOf(Instant.now().toEpochMilli()),
                LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    public void release(String sku) {
        redisTemplate.delete(key(sku));
    }

    private String key(String sku) {
        return "inventory:lock:" + sku;
    }
}
