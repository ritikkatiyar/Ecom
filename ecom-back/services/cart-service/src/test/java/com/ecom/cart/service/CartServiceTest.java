package com.ecom.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.MergeCartRequest;
import com.ecom.cart.entity.CartItem;
import com.ecom.cart.repository.CartItemRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private CartService cartService;

    @Test
    void addUserItemAccumulatesQuantity() {
        CartItem existing = new CartItem();
        existing.setUserId(1L);
        existing.setProductId("p1");
        existing.setQuantity(2);

        when(cartItemRepository.findByUserIdAndProductId(1L, "p1")).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(existing));

        var response = cartService.addItem(new CartItemRequest(1L, null, "p1", 3));

        assertEquals(5, existing.getQuantity());
        assertEquals(5, response.totalItems());
    }

    @Test
    void mergeGuestIntoUserMovesItems() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("cart:guest:g1")).thenReturn(Map.of("p1", "2"));
        when(cartItemRepository.findByUserIdAndProductId(1L, "p1")).thenReturn(Optional.empty());

        CartItem saved = new CartItem();
        saved.setUserId(1L);
        saved.setProductId("p1");
        saved.setQuantity(2);
        when(cartItemRepository.save(org.mockito.ArgumentMatchers.any(CartItem.class))).thenReturn(saved);
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(saved));

        var response = cartService.merge(new MergeCartRequest(1L, "g1"));

        assertEquals("USER", response.ownerType());
        assertEquals(2, response.totalItems());
    }

    @Test
    void getCartRequiresExactlyOneOwnerType() {
        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(null, null));
        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(1L, "g1"));
    }
}
