package com.ecom.cart.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.MergeCartRequest;

@Service
public class CartService implements CartUseCases {

    private final UserCartStore userCartStore;
    private final GuestCartStore guestCartStore;
    private final CartOwnerResolver cartOwnerResolver;

    public CartService(
            UserCartStore userCartStore,
            GuestCartStore guestCartStore,
            CartOwnerResolver cartOwnerResolver) {
        this.userCartStore = userCartStore;
        this.guestCartStore = guestCartStore;
        this.cartOwnerResolver = cartOwnerResolver;
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        CartOwner owner = cartOwnerResolver.resolve(request.userId(), request.guestId());
        if (owner.isUser()) {
            userCartStore.addItem(owner.userId(), request.productId(), request.quantity());
            return userCartStore.getCart(owner.userId());
        }
        guestCartStore.addItem(owner.guestId(), request.productId(), request.quantity());
        return guestCartStore.getCart(owner.guestId());
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId, String guestId) {
        CartOwner owner = cartOwnerResolver.resolve(userId, guestId);
        return owner.isUser() ? userCartStore.getCart(owner.userId()) : guestCartStore.getCart(owner.guestId());
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, String guestId, String productId) {
        CartOwner owner = cartOwnerResolver.resolve(userId, guestId);
        if (owner.isUser()) {
            userCartStore.removeItem(owner.userId(), productId);
            return userCartStore.getCart(owner.userId());
        }
        guestCartStore.removeItem(owner.guestId(), productId);
        return guestCartStore.getCart(owner.guestId());
    }

    @Override
    @Transactional
    public void clearCart(Long userId, String guestId) {
        CartOwner owner = cartOwnerResolver.resolve(userId, guestId);
        if (owner.isUser()) {
            userCartStore.clear(owner.userId());
            return;
        }
        guestCartStore.clear(owner.guestId());
    }

    @Override
    @Transactional
    public CartResponse merge(MergeCartRequest request) {
        Map<String, Integer> guestItems = guestCartStore.entries(request.guestId());
        for (Map.Entry<String, Integer> entry : guestItems.entrySet()) {
            userCartStore.addItem(request.userId(), entry.getKey(), entry.getValue());
        }
        guestCartStore.clear(request.guestId());
        return userCartStore.getCart(request.userId());
    }
}
