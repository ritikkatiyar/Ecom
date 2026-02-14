package com.ecom.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.ecom.search.entity.ConsumedEventRecord;

public interface ConsumedEventRepository extends ElasticsearchRepository<ConsumedEventRecord, String> {
}
