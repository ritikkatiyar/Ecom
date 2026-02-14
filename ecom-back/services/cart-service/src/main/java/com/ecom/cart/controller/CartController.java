package com.ecom.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.cart.dto.CartItemRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.MergeCartRequest;
import com.ecom.cart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(request));
    }

    @GetMapping
    public CartResponse getCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestId) {
        return cartService.getCart(userId, guestId);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable String productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestId) {
        return cartService.removeItem(userId, guestId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String guestId) {
        cartService.clearCart(userId, guestId);
    }

    @PostMapping("/merge")
    public CartResponse merge(@Valid @RequestBody MergeCartRequest request) {
        return cartService.merge(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
