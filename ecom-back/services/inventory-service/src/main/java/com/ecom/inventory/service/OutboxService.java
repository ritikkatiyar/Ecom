package com.ecom.inventory.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.common.DomainEvent;
import com.ecom.inventory.entity.OutboxEventRecord;
import com.ecom.inventory.entity.OutboxStatus;
import com.ecom.inventory.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String topic, String messageKey, String eventType, Object payload, String producer) {
        DomainEvent<Object> event = new DomainEvent<>(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                producer,
                "v1",
                UUID.randomUUID().toString(),
                payload);

        OutboxEventRecord record = new OutboxEventRecord();
        record.setId(event.eventId().toString());
        record.setTopic(topic);
        record.setMessageKey(messageKey);
        record.setEventType(eventType);
        record.setPayload(write(event));
        record.setStatus(OutboxStatus.PENDING);
        record.setAttempts(0);
        repository.save(record);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox event", ex);
        }
    }
}
