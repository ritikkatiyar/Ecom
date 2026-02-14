package com.ecom.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.service.OrderUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderUseCases orderService;

    public OrderController(OrderUseCases orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam Long userId) {
        return orderService.listOrders(userId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }

    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirm(@PathVariable String orderId) {
        return orderService.confirmOrder(orderId);
    }

    @PostMapping("/admin/saga/timeouts/run")
    public ResponseEntity<String> runTimeoutSweep() {
        int timedOut = orderService.markTimedOutOrders();
        return ResponseEntity.ok("Timed-out orders marked cancelled: " + timedOut);
    }

    @PostMapping("/admin/outbox/replay-failed")
    public ResponseEntity<String> replayFailedOutbox() {
        int replayed = orderService.replayFailedOutboxEvents();
        return ResponseEntity.ok("Outbox failed events reset to pending: " + replayed);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleStateIssue(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
