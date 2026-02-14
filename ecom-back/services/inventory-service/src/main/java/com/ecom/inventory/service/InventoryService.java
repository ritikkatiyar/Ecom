package com.ecom.inventory.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.inventory.dto.ReservationActionRequest;
import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.dto.StockResponse;
import com.ecom.inventory.dto.StockUpsertRequest;
import com.ecom.inventory.entity.InventoryReservation;
import com.ecom.inventory.entity.InventoryStock;
import com.ecom.inventory.repository.InventoryReservationRepository;
import com.ecom.inventory.repository.InventoryStockRepository;

@Service
public class InventoryService implements InventoryUseCases {

    private final InventoryStockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryLockService lockService;

    public InventoryService(
            InventoryStockRepository stockRepository,
            InventoryReservationRepository reservationRepository,
            InventoryLockService lockService) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.lockService = lockService;
    }

    @Transactional
    public StockResponse upsertStock(StockUpsertRequest request) {
        InventoryStock stock = stockRepository.findBySku(request.sku()).orElseGet(InventoryStock::new);
        if (stock.getId() == null) {
            stock.setSku(request.sku());
            stock.setReservedQuantity(0);
        }
        stock.setAvailableQuantity(request.availableQuantity());
        InventoryStock saved = stockRepository.save(stock);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(String sku) {
        InventoryStock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found"));
        return toResponse(stock);
    }

    @Transactional
    public StockResponse reserve(ReservationRequest request) {
        if (!lockService.acquire(request.sku())) {
            throw new IllegalStateException("Could not acquire inventory lock for SKU");
        }
        try {
            if (reservationRepository.findById(request.reservationId()).isPresent()) {
                throw new IllegalArgumentException("Reservation already exists");
            }

            InventoryStock stock = stockRepository.findBySkuForUpdate(request.sku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found"));

            if (stock.getAvailableQuantity() < request.quantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }

            stock.setAvailableQuantity(stock.getAvailableQuantity() - request.quantity());
            stock.setReservedQuantity(stock.getReservedQuantity() + request.quantity());
            stockRepository.save(stock);

            Instant now = Instant.now();
            InventoryReservation reservation = new InventoryReservation();
            reservation.setReservationId(request.reservationId());
            reservation.setSku(request.sku());
            reservation.setQuantity(request.quantity());
            reservation.setStatus("RESERVED");
            reservation.setCreatedAt(now);
            reservation.setUpdatedAt(now);
            reservation.setExpiresAt(now.plusSeconds(request.ttlMinutes() * 60L));
            reservationRepository.save(reservation);

            return toResponse(stock);
        } finally {
            lockService.release(request.sku());
        }
    }

    @Transactional
    public StockResponse release(ReservationActionRequest request) {
        InventoryReservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Reservation is not releasable");
        }

        if (!lockService.acquire(reservation.getSku())) {
            throw new IllegalStateException("Could not acquire inventory lock for SKU");
        }
        try {
            InventoryStock stock = stockRepository.findBySkuForUpdate(reservation.getSku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found"));

            stock.setAvailableQuantity(stock.getAvailableQuantity() + reservation.getQuantity());
            stock.setReservedQuantity(Math.max(0, stock.getReservedQuantity() - reservation.getQuantity()));
            stockRepository.save(stock);

            reservation.setStatus("RELEASED");
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);

            return toResponse(stock);
        } finally {
            lockService.release(reservation.getSku());
        }
    }

    @Transactional
    public StockResponse confirm(ReservationActionRequest request) {
        InventoryReservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Reservation is not confirmable");
        }

        if (!lockService.acquire(reservation.getSku())) {
            throw new IllegalStateException("Could not acquire inventory lock for SKU");
        }
        try {
            InventoryStock stock = stockRepository.findBySkuForUpdate(reservation.getSku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found"));

            stock.setReservedQuantity(Math.max(0, stock.getReservedQuantity() - reservation.getQuantity()));
            stockRepository.save(stock);

            reservation.setStatus("CONFIRMED");
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);

            return toResponse(stock);
        } finally {
            lockService.release(reservation.getSku());
        }
    }

    @Override
    @Transactional
    public void reserveForOrder(String orderId, List<OrderItemReservation> items, int ttlMinutes) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order items are required");
        }

        Map<String, Integer> bySku = new java.util.LinkedHashMap<>();
        for (OrderItemReservation item : items) {
            if (item == null || item.sku() == null || item.sku().isBlank() || item.quantity() <= 0) {
                throw new IllegalArgumentException("Invalid order item payload");
            }
            bySku.merge(item.sku(), item.quantity(), Integer::sum);
        }

        List<String> reservedIds = new ArrayList<>();
        try {
            for (var entry : bySku.entrySet()) {
                String reservationId = reservationId(orderId, entry.getKey());
                ReservationRequest request = new ReservationRequest(reservationId, entry.getKey(), entry.getValue(), ttlMinutes);
                try {
                    reserve(request);
                } catch (IllegalArgumentException ex) {
                    if (!"Reservation already exists".equals(ex.getMessage())) {
                        throw ex;
                    }
                }
                reservedIds.add(reservationId);
            }
        } catch (Exception ex) {
            for (String reservationId : reservedIds) {
                safeRelease(reservationId);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void releaseForOrder(String orderId) {
        for (InventoryReservation reservation : findOrderReservations(orderId)) {
            safeRelease(reservation.getReservationId());
        }
    }

    @Override
    @Transactional
    public void confirmForOrder(String orderId) {
        for (InventoryReservation reservation : findOrderReservations(orderId)) {
            safeConfirm(reservation.getReservationId());
        }
    }

    private List<InventoryReservation> findOrderReservations(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return List.of();
        }
        return reservationRepository.findByReservationIdStartingWith(orderId + ":");
    }

    private void safeRelease(String reservationId) {
        var found = reservationRepository.findById(reservationId);
        if (found.isEmpty()) {
            return;
        }
        String status = found.get().getStatus();
        if ("RELEASED".equals(status) || "CONFIRMED".equals(status)) {
            return;
        }
        release(new ReservationActionRequest(reservationId));
    }

    private void safeConfirm(String reservationId) {
        var found = reservationRepository.findById(reservationId);
        if (found.isEmpty()) {
            return;
        }
        String status = found.get().getStatus();
        if ("CONFIRMED".equals(status) || "RELEASED".equals(status)) {
            return;
        }
        confirm(new ReservationActionRequest(reservationId));
    }

    private String reservationId(String orderId, String sku) {
        return orderId + ":" + sku;
    }

    private StockResponse toResponse(InventoryStock stock) {
        return new StockResponse(stock.getSku(), stock.getAvailableQuantity(), stock.getReservedQuantity());
    }
}
