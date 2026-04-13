package com.ecom.search.service;

import com.ecom.common.reliability.ConsumerDedupSupport;
import org.springframework.stereotype.Service;

import com.ecom.search.entity.ConsumedEventRecord;
import com.ecom.search.repository.ConsumedEventRepository;

@Service
public class ConsumerDedupService {

    private final ConsumedEventRepository consumedEventRepository;

    public ConsumerDedupService(ConsumedEventRepository consumedEventRepository) {
        this.consumedEventRepository = consumedEventRepository;
    }

    public boolean markIfNew(String eventId) {
        return ConsumerDedupSupport.markIfNew(
                eventId,
                consumedEventRepository::existsById,
                ConsumedEventRecord::new,
                consumedEventRepository::save);
    }
}
