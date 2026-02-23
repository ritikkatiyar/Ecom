package com.ecom.common.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configures {@link RedisFallbackOperations} with optional Redis.
 * When Redis is unavailable, operations gracefully degrade (no-op / safe defaults).
 * Registered via META-INF/spring AutoConfiguration.imports for services that depend on common-redis.
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisFallbackConfig {

    @Bean
    public RedisFallbackOperations redisFallbackOperations(
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        return new RedisFallbackOperations(redisTemplate);
    }
}
