package com.ecom.notification.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.notification.repository.ConsumedEventRepository;

@Service
public class ReliabilityCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ReliabilityCleanupService.class);

    private final ConsumedEventRepository consumedEventRepository;
    private final Duration dedupRetention;

    public ReliabilityCleanupService(
            ConsumedEventRepository consumedEventRepository,
            @Value("${app.cleanup.dedup-retention:P14D}") Duration dedupRetention) {
        this.consumedEventRepository = consumedEventRepository;
        this.dedupRetention = dedupRetention;
    }

    @Scheduled(fixedDelayString = "${app.cleanup.fixed-delay:PT6H}")
    @Transactional
    public void cleanupReliabilityData() {
        long dedupDeleted = consumedEventRepository.deleteByConsumedAtBefore(Instant.now().minus(dedupRetention));
        if (dedupDeleted > 0) {
            log.info("Cleanup removed notification dedup records: {}", dedupDeleted);
        }
    }
}
