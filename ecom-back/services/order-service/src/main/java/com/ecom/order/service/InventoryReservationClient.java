package com.ecom.order.service;

import com.ecom.order.dto.OrderItemRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryReservationClient {

    private final RestClient restClient;
    private final int reservationTtlMinutes;

    public InventoryReservationClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.inventory.base-url:http://localhost:8084}") String inventoryBaseUrl,
            @Value("${app.inventory.reservation-ttl-minutes:30}") int reservationTtlMinutes) {
        this.restClient = restClientBuilder.baseUrl(inventoryBaseUrl).build();
        this.reservationTtlMinutes = reservationTtlMinutes;
    }

    public void reserve(String orderId, List<OrderItemRequest> items) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order items are required");
        }

        Map<String, Integer> quantityBySku = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            if (item == null || item.sku() == null || item.sku().isBlank() || item.quantity() <= 0) {
                throw new IllegalArgumentException("Invalid order item payload");
            }
            quantityBySku.merge(item.sku(), item.quantity(), Integer::sum);
        }

        try {
            for (Map.Entry<String, Integer> entry : quantityBySku.entrySet()) {
                restClient.post()
                        .uri("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InventoryReservationRequest(
                                reservationId(orderId, entry.getKey()),
                                entry.getKey(),
                                entry.getValue(),
                                reservationTtlMinutes))
                        .retrieve()
                        .toBodilessEntity();
            }
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(readableMessage(ex));
        }
    }

    private String reservationId(String orderId, String sku) {
        return orderId + ":" + sku;
    }

    private String readableMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body;
        }
        return ex.getStatusText();
    }

    private record InventoryReservationRequest(
            String reservationId,
            String sku,
            int quantity,
            int ttlMinutes) {
    }
}
