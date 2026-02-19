package com.ecom.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.MergeCartRequest;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private UserCartStore userCartStore;

    @Mock
    private GuestCartStore guestCartStore;

    @Mock
    private CartOwnerResolver cartOwnerResolver;

    @InjectMocks
    private CartService cartService;

    @Test
    void addUserItemAccumulatesQuantity() {
        CartItemRequest request = new CartItemRequest(1L, null, "p1", 3);
        CartResponse expected = new CartResponse("USER", "1", 5, java.util.List.of());

        when(cartOwnerResolver.resolve(1L, null)).thenReturn(CartOwner.forUser(1L));
        when(userCartStore.getCart(1L)).thenReturn(expected);

        var response = cartService.addItem(request);

        verify(userCartStore).addItem(1L, "p1", 3);
        assertEquals(5, response.totalItems());
    }

    @Test
    void mergeGuestIntoUserMovesItems() {
        when(guestCartStore.entries("g1")).thenReturn(Map.of("p1", 2));
        when(userCartStore.getCart(1L)).thenReturn(new CartResponse("USER", "1", 2, java.util.List.of()));

        var response = cartService.merge(new MergeCartRequest(1L, "g1"));

        verify(userCartStore).addItem(1L, "p1", 2);
        verify(guestCartStore).clear("g1");
        assertEquals("USER", response.ownerType());
        assertEquals(2, response.totalItems());
    }

    @Test
    void getCartRequiresExactlyOneOwnerType() {
        when(cartOwnerResolver.resolve(null, null))
                .thenThrow(new IllegalArgumentException("Exactly one of userId or guestId is required"));
        when(cartOwnerResolver.resolve(1L, "g1"))
                .thenThrow(new IllegalArgumentException("Exactly one of userId or guestId is required"));

        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(null, null));
        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(1L, "g1"));
    }
}
