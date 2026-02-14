package com.ecom.search.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecom.common.DomainEvent;
import com.ecom.search.service.ConsumerDedupService;
import com.ecom.search.dto.ProductIndexRequest;
import com.ecom.search.service.SearchUseCases;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProductIndexConsumer {

    private final SearchUseCases searchService;
    private final ConsumerDedupService dedupService;
    private final ObjectMapper objectMapper;

    public ProductIndexConsumer(SearchUseCases searchService, ConsumerDedupService dedupService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.product-upserted:product.upserted.v1}", groupId = "search-service")
    public void onProductUpserted(String rawEvent) {
        parse(rawEvent).ifPresent(event -> {
            if (!dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString())) {
                return;
            }
            if (event.payload() == null) {
                return;
            }
            searchService.upsertProduct(toRequest(event.payload()));
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.product-deleted:product.deleted.v1}", groupId = "search-service")
    public void onProductDeleted(String rawEvent) {
        parse(rawEvent)
                .filter(event -> dedupService.markIfNew(event.eventId() == null ? null : event.eventId().toString()))
                .map(DomainEvent::payload)
                .map(payload -> payload == null ? null : payload.get("productId"))
                .map(String::valueOf)
                .ifPresent(searchService::deleteProduct);
    }

    private java.util.Optional<DomainEvent<Map<String, Object>>> parse(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            var typeRef = new TypeReference<DomainEvent<Map<String, Object>>>() {};
            return java.util.Optional.of(objectMapper.readValue(rawEvent, typeRef));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private ProductIndexRequest toRequest(Map<String, Object> payload) {
        String productId = value(payload.get("productId"));
        String name = value(payload.get("name"));
        String description = value(payload.get("description"));
        String category = value(payload.get("category"));
        String brand = value(payload.get("brand"));
        BigDecimal price = new BigDecimal(value(payload.get("price"), "0"));
        List<String> colors = toStringList(payload.get("colors"));
        List<String> sizes = toStringList(payload.get("sizes"));
        Boolean active = payload.get("active") == null ? true : Boolean.valueOf(payload.get("active").toString());

        return new ProductIndexRequest(productId, name, description, category, brand, price, colors, sizes, active);
    }

    private String value(Object value) {
        return value(value, "");
    }

    private String value(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> raw) {
            return raw.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
