package com.ecom.payment.kafka;

import com.ecom.payment.service.PaymentUseCases;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    public OrderCreatedConsumer(PaymentUseCases paymentService) {
    }
}
