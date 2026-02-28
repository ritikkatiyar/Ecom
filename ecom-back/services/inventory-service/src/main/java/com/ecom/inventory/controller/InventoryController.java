package com.ecom.inventory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.inventory.dto.ReservationActionRequest;
import com.ecom.inventory.dto.ReservationRequest;
import com.ecom.inventory.dto.StockResponse;
import com.ecom.inventory.dto.StockUpsertRequest;
import com.ecom.inventory.exception.InventoryNotFoundException;
import com.ecom.inventory.service.InventoryUseCases;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryUseCases inventoryService;

    public InventoryController(InventoryUseCases inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stock")
    public ResponseEntity<StockResponse> upsertStock(@Valid @RequestBody StockUpsertRequest request) {
        log.info("Inventory upsert requested sku={} availableQuantity={}", request.sku(), request.availableQuantity());
        StockResponse response = inventoryService.upsertStock(request);
        log.info("Inventory upsert completed sku={} availableQuantity={} reservedQuantity={}",
                response.sku(), response.availableQuantity(), response.reservedQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stock/{sku}")
    public StockResponse getStock(@PathVariable String sku) {
        log.info("Inventory lookup requested sku={}", sku);
        StockResponse response = inventoryService.getStock(sku);
        log.info("Inventory lookup completed sku={} availableQuantity={} reservedQuantity={}",
                response.sku(), response.availableQuantity(), response.reservedQuantity());
        return response;
    }

    @PostMapping("/reserve")
    public StockResponse reserve(@Valid @RequestBody ReservationRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    public StockResponse release(@Valid @RequestBody ReservationActionRequest request) {
        return inventoryService.release(request);
    }

    @PostMapping("/confirm")
    public StockResponse confirm(@Valid @RequestBody ReservationActionRequest request) {
        return inventoryService.confirm(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Inventory request validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<String> handleNotFound(InventoryNotFoundException ex) {
        log.warn("Inventory lookup not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        log.warn("Inventory request conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
