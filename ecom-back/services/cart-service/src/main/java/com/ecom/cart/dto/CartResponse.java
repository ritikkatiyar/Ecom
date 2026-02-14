package com.ecom.cart.dto;

import java.util.List;

public record CartResponse(String ownerType, String ownerId, int totalItems, List<CartItemResponse> items) {
}
