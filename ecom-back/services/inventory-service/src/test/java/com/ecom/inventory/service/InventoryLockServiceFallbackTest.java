package com.ecom.inventory.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryLockServiceFallbackTest {

    @Test
    void inventoryLockUsesInMemoryFallbackWhenRedisIsUnavailable() {
        InventoryLockService service = new InventoryLockService(null);

        assertTrue(service.acquire("SKU-1"));
        assertFalse(service.acquire("SKU-1"));

        service.release("SKU-1");

        assertTrue(service.acquire("SKU-1"));
    }
}
