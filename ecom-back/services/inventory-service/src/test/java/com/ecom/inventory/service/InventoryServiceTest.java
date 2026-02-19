package com.ecom.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.entity.InventoryReservation;
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

    @Test
    void reserveForOrderIsIdempotentForSameOrderSku() {
        InventoryStock stock = new InventoryStock();
        stock.setSku("SKU-1");
        stock.setAvailableQuantity(10);
        stock.setReservedQuantity(0);

        InventoryReservation existing = new InventoryReservation();
        existing.setReservationId("order-1:SKU-1");
        existing.setSku("SKU-1");
        existing.setQuantity(1);
        existing.setStatus("RESERVED");
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        existing.setExpiresAt(Instant.now().plusSeconds(600));

        when(lockService.acquire("SKU-1")).thenReturn(true);
        when(reservationRepository.findById("order-1:SKU-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(stockRepository.findBySkuForUpdate("SKU-1")).thenReturn(Optional.of(stock));

        inventoryService.reserveForOrder("order-1", List.of(new OrderItemReservation("SKU-1", 1)), 10);
        inventoryService.reserveForOrder("order-1", List.of(new OrderItemReservation("SKU-1", 1)), 10);

        assertEquals(9, stock.getAvailableQuantity());
        assertEquals(1, stock.getReservedQuantity());
        verify(stockRepository, times(1)).findBySkuForUpdate("SKU-1");
    }

    @Test
    void releaseExpiredReservationsReleasesReservedRecords() {
        Instant now = Instant.now();

        InventoryStock stock = new InventoryStock();
        stock.setSku("SKU-1");
        stock.setAvailableQuantity(4);
        stock.setReservedQuantity(2);

        InventoryReservation expired = new InventoryReservation();
        expired.setReservationId("res-expired");
        expired.setSku("SKU-1");
        expired.setQuantity(2);
        expired.setStatus("RESERVED");
        expired.setCreatedAt(now.minusSeconds(600));
        expired.setUpdatedAt(now.minusSeconds(600));
        expired.setExpiresAt(now.minusSeconds(10));

        when(reservationRepository.findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc("RESERVED", now))
                .thenReturn(List.of(expired));
        when(reservationRepository.findById("res-expired"))
                .thenReturn(Optional.of(expired), Optional.of(expired));
        when(lockService.acquire("SKU-1")).thenReturn(true);
        when(stockRepository.findBySkuForUpdate("SKU-1")).thenReturn(Optional.of(stock));

        int released = inventoryService.releaseExpiredReservations(now, 100);

        assertEquals(1, released);
        assertEquals("RELEASED", expired.getStatus());
        assertEquals(6, stock.getAvailableQuantity());
        assertEquals(0, stock.getReservedQuantity());
    }
}
