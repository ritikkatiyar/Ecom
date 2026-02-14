package com.ecom.inventory.dto;

public record StockResponse(
        String sku,
        int availableQuantity,
        int reservedQuantity
) {
}
