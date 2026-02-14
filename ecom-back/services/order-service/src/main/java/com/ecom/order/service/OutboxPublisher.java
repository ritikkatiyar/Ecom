package com.ecom.order.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.common.reliability.OutboxPublishSupport;
import com.ecom.order.entity.OutboxEventRecord;
import com.ecom.order.entity.OutboxStatus;
import com.ecom.order.repository.OutboxEventRepository;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int maxRetry;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.max-retry:5}") int maxRetry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "PT3S")
    @Transactional
    public void publishPending() {
        List<OutboxEventRecord> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        OutboxPublishSupport.publishPending(
                pending,
                kafkaTemplate,
                maxRetry,
                outboxEventRepository::save);
    }
}
