package com.ecom.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.ecom.search.model.SearchProductDocument;

public interface SearchProductRepository extends ElasticsearchRepository<SearchProductDocument, String> {
}
