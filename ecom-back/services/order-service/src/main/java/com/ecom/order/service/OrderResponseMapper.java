package com.ecom.order.service;

import org.springframework.stereotype.Component;

import com.ecom.order.dto.OrderResponse;
import com.ecom.order.entity.OrderRecord;

@Component
public class OrderResponseMapper {

    private final OrderItemCodec orderItemCodec;

    public OrderResponseMapper(OrderItemCodec orderItemCodec) {
        this.orderItemCodec = orderItemCodec;
    }

    public OrderResponse toResponse(OrderRecord record) {
        return new OrderResponse(
                record.getId(),
                record.getUserId(),
                record.getStatus().name(),
                record.getTotalAmount(),
                record.getCurrency(),
                orderItemCodec.readItems(record.getItemsJson()),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
