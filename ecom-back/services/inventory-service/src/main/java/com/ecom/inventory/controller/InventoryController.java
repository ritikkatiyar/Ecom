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
import com.ecom.inventory.service.InventoryUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final InventoryUseCases inventoryService;

    public InventoryController(InventoryUseCases inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stock")
    public ResponseEntity<StockResponse> upsertStock(@Valid @RequestBody StockUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.upsertStock(request));
    }

    @GetMapping("/stock/{sku}")
    public StockResponse getStock(@PathVariable String sku) {
        return inventoryService.getStock(sku);
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
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
