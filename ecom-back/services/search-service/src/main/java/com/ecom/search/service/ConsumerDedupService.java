package com.ecom.search.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.search.entity.ConsumedEventRecord;
import com.ecom.search.repository.ConsumedEventRepository;

@Service
public class ConsumerDedupService {

    private final ConsumedEventRepository consumedEventRepository;

    public ConsumerDedupService(ConsumedEventRepository consumedEventRepository) {
        this.consumedEventRepository = consumedEventRepository;
    }

    @Transactional
    public boolean markIfNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        if (consumedEventRepository.existsById(eventId)) {
            return false;
        }
        ConsumedEventRecord record = new ConsumedEventRecord();
        record.setEventId(eventId);
        record.setConsumedAt(Instant.now());
        consumedEventRepository.save(record);
        return true;
    }
}
