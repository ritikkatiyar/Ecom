package com.ecom.inventory.service;

import java.util.List;

import com.ecom.inventory.dto.ReservationActionRequest;
import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.dto.StockResponse;
import com.ecom.inventory.dto.StockUpsertRequest;

public interface InventoryUseCases {

    StockResponse upsertStock(StockUpsertRequest request);

    StockResponse getStock(String sku);

    StockResponse reserve(ReservationRequest request);

    StockResponse release(ReservationActionRequest request);

    StockResponse confirm(ReservationActionRequest request);

    void reserveForOrder(String orderId, List<OrderItemReservation> items, int ttlMinutes);

    void releaseForOrder(String orderId);

    void confirmForOrder(String orderId);
}
