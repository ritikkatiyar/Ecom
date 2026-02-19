package com.ecom.cart.service;

import org.springframework.stereotype.Component;

@Component
public class CartOwnerResolver {

    public CartOwner resolve(Long userId, String guestId) {
        boolean hasUser = userId != null;
        boolean hasGuest = guestId != null && !guestId.isBlank();

        if (hasUser == hasGuest) {
            throw new IllegalArgumentException("Exactly one of userId or guestId is required");
        }
        return hasUser ? CartOwner.forUser(userId) : CartOwner.forGuest(guestId);
    }
}
