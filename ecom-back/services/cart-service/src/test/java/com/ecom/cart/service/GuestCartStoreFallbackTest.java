package com.ecom.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GuestCartStoreFallbackTest {

    @Test
    void guestCartUsesInMemoryFallbackWhenRedisIsUnavailable() {
        GuestCartStore store = new GuestCartStore(null);

        store.addItem("guest-1", "product-1", 2);
        store.addItem("guest-1", "product-1", 1);
        store.removeItem("guest-1", "missing-product");

        assertEquals(3, store.getCart("guest-1").totalItems());
        assertEquals(3, store.entries("guest-1").get("product-1"));
    }
}
