package com.ecom.inventory.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReservationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryService.class);

    private final InventoryUseCases inventoryService;
    private final int batchSize;

    public ReservationExpiryService(
            InventoryUseCases inventoryService,
            @Value("${app.inventory.reservation-expiry-batch-size:100}") int batchSize) {
        this.inventoryService = inventoryService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.inventory.reservation-expiry-scan-delay:PT60S}")
    public void releaseExpiredReservations() {
        int released = inventoryService.releaseExpiredReservations(Instant.now(), batchSize);
        if (released > 0) {
            log.info("Released {} expired inventory reservations", released);
        }
    }
}
