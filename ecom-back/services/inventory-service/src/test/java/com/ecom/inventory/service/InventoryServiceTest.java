package com.ecom.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.entity.InventoryStock;
import com.ecom.inventory.repository.InventoryReservationRepository;
import com.ecom.inventory.repository.InventoryStockRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryStockRepository stockRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryLockService lockService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void reserveFailsWhenStockIsInsufficient() {
        InventoryStock stock = new InventoryStock();
        stock.setSku("SKU-1");
        stock.setAvailableQuantity(1);
        stock.setReservedQuantity(0);

        when(lockService.acquire("SKU-1")).thenReturn(true);
        when(reservationRepository.findById("res-1")).thenReturn(Optional.empty());
        when(stockRepository.findBySkuForUpdate("SKU-1")).thenReturn(Optional.of(stock));

        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.reserve(new ReservationRequest("res-1", "SKU-1", 2, 10)));
    }

    @Test
    void reserveUpdatesStockCounters() {
        InventoryStock stock = new InventoryStock();
        stock.setSku("SKU-1");
        stock.setAvailableQuantity(10);
        stock.setReservedQuantity(1);

        when(lockService.acquire("SKU-1")).thenReturn(true);
        when(reservationRepository.findById("res-2")).thenReturn(Optional.empty());
        when(stockRepository.findBySkuForUpdate("SKU-1")).thenReturn(Optional.of(stock));

        inventoryService.reserve(new ReservationRequest("res-2", "SKU-1", 3, 10));

        assertEquals(7, stock.getAvailableQuantity());
        assertEquals(4, stock.getReservedQuantity());
    }
}
