package com.ecom.payment.service;

import com.ecom.common.reliability.ConsumerDedupSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.payment.repository.ConsumedEventRepository;
import com.ecom.payment.entity.ConsumedEventRecord;

@Service
public class ConsumerDedupService {

    private final ConsumedEventRepository consumedEventRepository;

    public ConsumerDedupService(ConsumedEventRepository consumedEventRepository) {
        this.consumedEventRepository = consumedEventRepository;
    }

    @Transactional
    public boolean markIfNew(String eventId) {
        return ConsumerDedupSupport.markIfNew(
                eventId,
                consumedEventRepository::existsById,
                ConsumedEventRecord::new,
                consumedEventRepository::save);
    }
}
