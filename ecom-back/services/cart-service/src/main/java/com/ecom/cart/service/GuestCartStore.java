package com.ecom.cart.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ecom.cart.dto.CartItemResponse;
import com.ecom.cart.dto.CartResponse;

@Component
public class GuestCartStore {

    private static final Logger log = LoggerFactory.getLogger(GuestCartStore.class);
    private static final Duration GUEST_CART_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, InMemoryGuestCart> fallbackCarts = new ConcurrentHashMap<>();

    public GuestCartStore(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addItem(String guestId, String productId, int quantity) {
        if (redisTemplate == null) {
            addItemFallback(guestId, productId, quantity);
            return;
        }
        try {
            String key = guestKey(guestId);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            int existing = entries.containsKey(productId) ? Integer.parseInt(entries.get(productId).toString()) : 0;
            redisTemplate.opsForHash().put(key, productId, String.valueOf(existing + quantity));
            refreshTtl(guestId);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for guest cart addItem; using in-memory fallback: {}", ex.getMessage());
            addItemFallback(guestId, productId, quantity);
        }
    }

    public Map<String, Integer> entries(String guestId) {
        if (redisTemplate == null) {
            return entriesFallback(guestId);
        }
        try {
            Map<Object, Object> raw = new HashMap<>(redisTemplate.opsForHash().entries(guestKey(guestId)));
            Map<String, Integer> parsed = new HashMap<>();
            for (Map.Entry<Object, Object> entry : raw.entrySet()) {
                parsed.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
            }
            return parsed;
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for guest cart entries; using in-memory fallback: {}", ex.getMessage());
            return entriesFallback(guestId);
        }
    }

    public void removeItem(String guestId, String productId) {
        if (redisTemplate == null) {
            removeItemFallback(guestId, productId);
            return;
        }
        try {
            redisTemplate.opsForHash().delete(guestKey(guestId), productId);
            refreshTtl(guestId);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for guest cart removeItem; using in-memory fallback: {}", ex.getMessage());
            removeItemFallback(guestId, productId);
        }
    }

    public void clear(String guestId) {
        if (redisTemplate == null) {
            fallbackCarts.remove(guestKey(guestId));
            return;
        }
        try {
            redisTemplate.delete(guestKey(guestId));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable for guest cart clear; using in-memory fallback: {}", ex.getMessage());
            fallbackCarts.remove(guestKey(guestId));
        }
    }

    public CartResponse getCart(String guestId) {
        Map<String, Integer> entries = entries(guestId);
        List<CartItemResponse> payload = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            payload.add(new CartItemResponse(entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }
        return new CartResponse("GUEST", guestId, total, payload);
    }

    private void refreshTtl(String guestId) {
        if (redisTemplate == null) {
            touchFallbackCart(guestKey(guestId));
            return;
        }
        redisTemplate.expire(guestKey(guestId), GUEST_CART_TTL);
    }

    private String guestKey(String guestId) {
        return "cart:guest:" + guestId;
    }

    private void addItemFallback(String guestId, String productId, int quantity) {
        InMemoryGuestCart cart = touchFallbackCart(guestKey(guestId));
        cart.items.merge(productId, quantity, Integer::sum);
    }

    private Map<String, Integer> entriesFallback(String guestId) {
        InMemoryGuestCart cart = touchFallbackCart(guestKey(guestId));
        return new HashMap<>(cart.items);
    }

    private void removeItemFallback(String guestId, String productId) {
        InMemoryGuestCart cart = touchFallbackCart(guestKey(guestId));
        cart.items.remove(productId);
    }

    private InMemoryGuestCart touchFallbackCart(String key) {
        Instant now = Instant.now();
        return fallbackCarts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                existing = new InMemoryGuestCart();
            }
            existing.expiresAt = now.plus(GUEST_CART_TTL);
            return existing;
        });
    }

    private static final class InMemoryGuestCart {
        private final Map<String, Integer> items = new ConcurrentHashMap<>();
        private Instant expiresAt = Instant.now().plus(GUEST_CART_TTL);
    }
}
