package com.ecom.order.kafka;

import org.springframework.stereotype.Component;

import com.ecom.order.service.OrderUseCases;

@Component
public class InventoryReservationConsumer {

    public InventoryReservationConsumer(OrderUseCases orderService) {
    }
}
