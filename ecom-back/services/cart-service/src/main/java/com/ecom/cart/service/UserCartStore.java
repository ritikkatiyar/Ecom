package com.ecom.cart.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ecom.cart.dto.CartItemResponse;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.entity.CartItem;
import com.ecom.cart.repository.CartItemRepository;

@Component
public class UserCartStore {

    private final CartItemRepository cartItemRepository;

    public UserCartStore(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    public void addItem(Long userId, String productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> newCartItem(userId, productId));
        item.setQuantity(item.getQuantity() + quantity);
        cartItemRepository.save(item);
    }

    public void removeItem(Long userId, String productId) {
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    public void clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> payload = new ArrayList<>();
        int total = 0;
        for (CartItem item : items) {
            payload.add(new CartItemResponse(item.getProductId(), item.getQuantity()));
            total += item.getQuantity();
        }
        return new CartResponse("USER", String.valueOf(userId), total, payload);
    }

    public Optional<CartItem> findItem(Long userId, String productId) {
        return cartItemRepository.findByUserIdAndProductId(userId, productId);
    }

    private CartItem newCartItem(Long userId, String productId) {
        CartItem newItem = new CartItem();
        newItem.setUserId(userId);
        newItem.setProductId(productId);
        newItem.setQuantity(0);
        return newItem;
    }
}
