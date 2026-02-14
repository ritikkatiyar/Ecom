package com.ecom.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.client.RestClient;

import com.ecom.search.dto.RelevanceDatasetHealthResponse;
import com.ecom.search.repository.SearchProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class SearchServiceDatasetHealthTest {

    @Test
    void shouldMarkDatasetAsStaleWhenRefreshCadenceExceeded() {
        String datasetJson = """
                [
                  {"query":"iphone 15","category":"electronics","brand":"Apple","expectedTopProductId":"prod-iphone-15","expectedInTopN":["prod-iphone-15"]},
                  {"query":"nike running shoes","category":"fashion","brand":"Nike","expectedTopProductId":"prod-nike-runner-1","expectedInTopN":["prod-nike-runner-1"]}
                ]
                """;
        String metadataJson = """
                {
                  "version":"2025.01.01",
                  "lastRefreshedAt":"2025-01-01T00:00:00Z",
                  "refreshCadenceDays":14
                }
                """;

        SearchService service = new SearchService(
                mock(SearchProductRepository.class),
                mock(ElasticsearchOperations.class),
                new ObjectMapper(),
                RestClient.builder(),
                "http://localhost:8083",
                new ByteArrayResource(datasetJson.getBytes()),
                new ByteArrayResource(metadataJson.getBytes()),
                85.0);

        RelevanceDatasetHealthResponse health = service.evaluateRelevanceDatasetHealth();

        assertEquals("2025.01.01", health.datasetVersion());
        assertEquals(2, health.datasetSize());
        assertEquals(14, health.refreshCadenceDays());
        assertTrue(health.refreshRequired());
        assertTrue(health.daysSinceRefresh() > 14);
    }

    @Test
    void shouldNotMarkDatasetAsStaleWithinCadence() {
        String datasetJson = """
                [{"query":"wireless earbuds","category":"electronics","brand":"","expectedTopProductId":"prod-earbuds-pro","expectedInTopN":["prod-earbuds-pro"]}]
                """;
        String metadataJson = """
                {
                  "version":"2026.02.14",
                  "lastRefreshedAt":"2099-01-01T00:00:00Z",
                  "refreshCadenceDays":14
                }
                """;

        SearchService service = new SearchService(
                mock(SearchProductRepository.class),
                mock(ElasticsearchOperations.class),
                new ObjectMapper(),
                RestClient.builder(),
                "http://localhost:8083",
                new ByteArrayResource(datasetJson.getBytes()),
                new ByteArrayResource(metadataJson.getBytes()),
                85.0);

        RelevanceDatasetHealthResponse health = service.evaluateRelevanceDatasetHealth();

        assertEquals("2026.02.14", health.datasetVersion());
        assertEquals(1, health.datasetSize());
        assertFalse(health.refreshRequired());
        assertEquals(0, health.daysSinceRefresh());
    }
}
