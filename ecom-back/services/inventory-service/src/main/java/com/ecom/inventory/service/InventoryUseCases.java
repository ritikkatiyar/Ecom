package com.ecom.inventory.service;

import java.time.Instant;
import java.util.List;

import com.ecom.inventory.dto.ReservationActionRequest;
import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.dto.StockResponse;
import com.ecom.inventory.dto.StockUpsertRequest;

public interface InventoryUseCases {

    /**
     * Creates or updates the stock record for a SKU.
     */
    StockResponse upsertStock(StockUpsertRequest request);

    /**
     * Returns the latest available and reserved quantities for a SKU.
     */
    StockResponse getStock(String sku);

    /**
     * Reserves quantity for one reservation id and SKU.
     */
    StockResponse reserve(ReservationRequest request);

    /**
     * Releases a previously reserved quantity back into available stock.
     */
    StockResponse release(ReservationActionRequest request);

    /**
     * Confirms a reservation after payment succeeds.
     */
    StockResponse confirm(ReservationActionRequest request);

    /**
     * Reserves all SKUs for an order as a single logical operation.
     */
    void reserveForOrder(String orderId, List<OrderItemReservation> items, int ttlMinutes);

    /**
     * Releases every reservation currently associated with an order.
     */
    void releaseForOrder(String orderId);

    /**
     * Confirms every reservation currently associated with an order.
     */
    void confirmForOrder(String orderId);

    /**
     * Releases expired reservations in bounded batches during background cleanup.
     */
    int releaseExpiredReservations(Instant cutoff, int batchSize);
}
