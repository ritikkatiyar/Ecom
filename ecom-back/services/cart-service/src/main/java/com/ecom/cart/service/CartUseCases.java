package com.ecom.cart.service;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.MergeCartRequest;

public interface CartUseCases {

    CartResponse addItem(CartItemRequest request);

    CartResponse getCart(Long userId, String guestId);

    CartResponse removeItem(Long userId, String guestId, String productId);

    void clearCart(Long userId, String guestId);

    CartResponse merge(MergeCartRequest request);
}
