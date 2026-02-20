package com.ecom.order.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ecom.order.dto.OrderItemRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderItemCodec {

    private final ObjectMapper objectMapper;

    public OrderItemCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeItems(List<OrderItemRequest> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize order items", ex);
        }
    }

    public List<OrderItemRequest> readItems(String itemsJson) {
        try {
            return objectMapper.readerForListOf(OrderItemRequest.class).readValue(itemsJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not deserialize order items", ex);
        }
    }
}
