package com.ecom.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.ecom.common.DomainEvent;
import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderItemRequest;
import com.ecom.order.entity.OrderRecord;
import com.ecom.order.entity.OrderStatus;
import com.ecom.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OrderService orderService = new OrderService(
            orderRepository,
            new ObjectMapper(),
            kafkaTemplate,
            "order.created.v1");

    @Test
    void createOrderCalculatesTotalAndMovesToPaymentPending() {
        var request = new CreateOrderRequest(
                1L,
                "INR",
                List.of(
                        new OrderItemRequest("p1", "sku1", 2, new BigDecimal("100.00")),
                        new OrderItemRequest("p2", "sku2", 1, new BigDecimal("50.00"))));

        when(orderRepository.save(any(OrderRecord.class))).thenAnswer(invocation -> {
            OrderRecord o = invocation.getArgument(0);
            if (o.getId() == null || o.getId().isBlank()) {
                o.setId("ord_1");
                o.setCreatedAt(Instant.now());
                o.setUpdatedAt(Instant.now());
            }
            return o;
        });

        var response = orderService.createOrder(request);

        assertEquals("PAYMENT_PENDING", response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());
    }

    @Test
    void cancelFailsForConfirmedOrder() {
        OrderRecord record = new OrderRecord();
        record.setId("ord_2");
        record.setUserId(1L);
        record.setStatus(OrderStatus.CONFIRMED);
        record.setTotalAmount(new BigDecimal("10.00"));
        record.setCurrency("INR");
        record.setItemsJson("[]");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());

        when(orderRepository.findById("ord_2")).thenReturn(Optional.of(record));

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder("ord_2"));
    }

    @Test
    void paymentAuthorizedMarksOrderConfirmed() {
        OrderRecord record = new OrderRecord();
        record.setId("ord_3");
        record.setUserId(1L);
        record.setStatus(OrderStatus.PAYMENT_PENDING);
        record.setTotalAmount(new BigDecimal("10.00"));
        record.setCurrency("INR");
        record.setItemsJson("[]");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());

        when(orderRepository.findById("ord_3")).thenReturn(Optional.of(record));
        when(orderRepository.save(any(OrderRecord.class))).thenAnswer(i -> i.getArgument(0));

        orderService.markPaymentAuthorized("ord_3");

        assertEquals(OrderStatus.CONFIRMED, record.getStatus());
        verify(orderRepository).save(record);
    }

    @Test
    void paymentFailedDoesNotChangeConfirmedOrder() {
        OrderRecord record = new OrderRecord();
        record.setId("ord_4");
        record.setUserId(1L);
        record.setStatus(OrderStatus.CONFIRMED);
        record.setTotalAmount(new BigDecimal("10.00"));
        record.setCurrency("INR");
        record.setItemsJson("[]");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());

        when(orderRepository.findById("ord_4")).thenReturn(Optional.of(record));

        orderService.markPaymentFailed("ord_4");

        assertEquals(OrderStatus.CONFIRMED, record.getStatus());
        verify(orderRepository, never()).save(any(OrderRecord.class));
    }
}

