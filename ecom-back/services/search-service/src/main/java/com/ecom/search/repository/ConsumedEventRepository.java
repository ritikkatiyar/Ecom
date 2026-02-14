package com.ecom.search.repository;

import java.time.Instant;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.ecom.search.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends ElasticsearchRepository<ConsumedEventRecord, String> {

    long deleteByConsumedAtBefore(Instant cutoff);
}
