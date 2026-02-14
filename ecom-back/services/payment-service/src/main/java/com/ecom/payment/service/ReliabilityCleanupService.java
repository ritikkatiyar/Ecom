package com.ecom.payment.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.payment.entity.OutboxStatus;
import com.ecom.payment.repository.ConsumedEventRepository;
import com.ecom.payment.repository.OutboxEventRepository;

@Service
public class ReliabilityCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ReliabilityCleanupService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ConsumedEventRepository consumedEventRepository;
    private final Duration sentRetention;
    private final Duration failedRetention;
    private final Duration dedupRetention;

    public ReliabilityCleanupService(
            OutboxEventRepository outboxEventRepository,
            ConsumedEventRepository consumedEventRepository,
            @Value("${app.cleanup.outbox-sent-retention:P7D}") Duration sentRetention,
            @Value("${app.cleanup.outbox-failed-retention:P30D}") Duration failedRetention,
            @Value("${app.cleanup.dedup-retention:P14D}") Duration dedupRetention) {
        this.outboxEventRepository = outboxEventRepository;
        this.consumedEventRepository = consumedEventRepository;
        this.sentRetention = sentRetention;
        this.failedRetention = failedRetention;
        this.dedupRetention = dedupRetention;
    }

    @Scheduled(fixedDelayString = "${app.cleanup.fixed-delay:PT6H}")
    @Transactional
    public void cleanupReliabilityData() {
        Instant now = Instant.now();
        long sentDeleted = outboxEventRepository.deleteByStatusAndUpdatedAtBefore(
                OutboxStatus.SENT, now.minus(sentRetention));
        long failedDeleted = outboxEventRepository.deleteByStatusAndUpdatedAtBefore(
                OutboxStatus.FAILED, now.minus(failedRetention));
        long dedupDeleted = consumedEventRepository.deleteByConsumedAtBefore(now.minus(dedupRetention));

        if (sentDeleted + failedDeleted + dedupDeleted > 0) {
            log.info("Cleanup removed payment reliability records: sentOutbox={}, failedOutbox={}, dedup={}",
                    sentDeleted, failedDeleted, dedupDeleted);
        }
    }
}
