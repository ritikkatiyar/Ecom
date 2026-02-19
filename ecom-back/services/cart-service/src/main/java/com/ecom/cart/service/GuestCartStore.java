package com.ecom.cart.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ecom.cart.dto.CartItemResponse;
import com.ecom.cart.dto.CartResponse;

@Component
public class GuestCartStore {

    private static final Duration GUEST_CART_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public GuestCartStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addItem(String guestId, String productId, int quantity) {
        String key = guestKey(guestId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        int existing = entries.containsKey(productId) ? Integer.parseInt(entries.get(productId).toString()) : 0;
        redisTemplate.opsForHash().put(key, productId, String.valueOf(existing + quantity));
        refreshTtl(guestId);
    }

    public Map<String, Integer> entries(String guestId) {
        Map<Object, Object> raw = new HashMap<>(redisTemplate.opsForHash().entries(guestKey(guestId)));
        Map<String, Integer> parsed = new HashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            parsed.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
        }
        return parsed;
    }

    public void removeItem(String guestId, String productId) {
        redisTemplate.opsForHash().delete(guestKey(guestId), productId);
        refreshTtl(guestId);
    }

    public void clear(String guestId) {
        redisTemplate.delete(guestKey(guestId));
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
        redisTemplate.expire(guestKey(guestId), GUEST_CART_TTL);
    }

    private String guestKey(String guestId) {
        return "cart:guest:" + guestId;
    }
}
