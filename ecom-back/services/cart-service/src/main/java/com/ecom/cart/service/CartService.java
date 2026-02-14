package com.ecom.cart.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.CartItemResponse;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.MergeCartRequest;
import com.ecom.cart.entity.CartItem;
import com.ecom.cart.repository.CartItemRepository;

@Service
public class CartService {

    private static final Duration GUEST_CART_TTL = Duration.ofDays(7);

    private final CartItemRepository cartItemRepository;
    private final StringRedisTemplate redisTemplate;

    public CartService(CartItemRepository cartItemRepository, StringRedisTemplate redisTemplate) {
        this.cartItemRepository = cartItemRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Owner owner = resolveOwner(request.userId(), request.guestId());
        if (owner.user()) {
            addUserItem(owner.userId(), request.productId(), request.quantity());
            return getUserCart(owner.userId());
        }
        addGuestItem(owner.guestId(), request.productId(), request.quantity());
        return getGuestCart(owner.guestId());
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId, String guestId) {
        Owner owner = resolveOwner(userId, guestId);
        return owner.user() ? getUserCart(owner.userId()) : getGuestCart(owner.guestId());
    }

    @Transactional
    public CartResponse removeItem(Long userId, String guestId, String productId) {
        Owner owner = resolveOwner(userId, guestId);
        if (owner.user()) {
            cartItemRepository.deleteByUserIdAndProductId(owner.userId(), productId);
            return getUserCart(owner.userId());
        }
        redisTemplate.opsForHash().delete(guestKey(owner.guestId()), productId);
        refreshGuestTtl(owner.guestId());
        return getGuestCart(owner.guestId());
    }

    @Transactional
    public void clearCart(Long userId, String guestId) {
        Owner owner = resolveOwner(userId, guestId);
        if (owner.user()) {
            cartItemRepository.deleteByUserId(owner.userId());
        } else {
            redisTemplate.delete(guestKey(owner.guestId()));
        }
    }

    @Transactional
    public CartResponse merge(MergeCartRequest request) {
        Map<Object, Object> guestItems = redisTemplate.opsForHash().entries(guestKey(request.guestId()));
        for (Map.Entry<Object, Object> entry : guestItems.entrySet()) {
            addUserItem(request.userId(), entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
        }
        redisTemplate.delete(guestKey(request.guestId()));
        return getUserCart(request.userId());
    }

    private void addUserItem(Long userId, String productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setUserId(userId);
            newItem.setProductId(productId);
            newItem.setQuantity(0);
            return newItem;
        });
        item.setQuantity(item.getQuantity() + quantity);
        cartItemRepository.save(item);
    }

    private void addGuestItem(String guestId, String productId, int quantity) {
        String key = guestKey(guestId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        int existing = entries.containsKey(productId) ? Integer.parseInt(entries.get(productId).toString()) : 0;
        redisTemplate.opsForHash().put(key, productId, String.valueOf(existing + quantity));
        refreshGuestTtl(guestId);
    }

    private CartResponse getUserCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> payload = new ArrayList<>();
        int total = 0;
        for (CartItem item : items) {
            payload.add(new CartItemResponse(item.getProductId(), item.getQuantity()));
            total += item.getQuantity();
        }
        return new CartResponse("USER", String.valueOf(userId), total, payload);
    }

    private CartResponse getGuestCart(String guestId) {
        Map<Object, Object> entries = new HashMap<>(redisTemplate.opsForHash().entries(guestKey(guestId)));
        List<CartItemResponse> payload = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            int quantity = Integer.parseInt(entry.getValue().toString());
            payload.add(new CartItemResponse(entry.getKey().toString(), quantity));
            total += quantity;
        }
        return new CartResponse("GUEST", guestId, total, payload);
    }

    private void refreshGuestTtl(String guestId) {
        redisTemplate.expire(guestKey(guestId), GUEST_CART_TTL);
    }

    private String guestKey(String guestId) {
        return "cart:guest:" + guestId;
    }

    private Owner resolveOwner(Long userId, String guestId) {
        boolean hasUser = userId != null;
        boolean hasGuest = guestId != null && !guestId.isBlank();

        if (hasUser == hasGuest) {
            throw new IllegalArgumentException("Exactly one of userId or guestId is required");
        }
        return hasUser ? Owner.forUser(userId) : Owner.forGuest(guestId);
    }

    private record Owner(Long userId, String guestId) {
        static Owner forUser(Long userId) { return new Owner(userId, null); }
        static Owner forGuest(String guestId) { return new Owner(null, guestId); }
        boolean user() { return userId != null; }
    }
}
