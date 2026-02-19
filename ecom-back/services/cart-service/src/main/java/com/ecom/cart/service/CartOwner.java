package com.ecom.cart.service;

public record CartOwner(Long userId, String guestId) {

    public static CartOwner forUser(Long userId) {
        return new CartOwner(userId, null);
    }

    public static CartOwner forGuest(String guestId) {
        return new CartOwner(null, guestId);
    }

    public boolean isUser() {
        return userId != null;
    }
}
