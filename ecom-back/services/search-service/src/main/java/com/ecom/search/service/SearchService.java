package com.ecom.search.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.client.RestClient;

import com.ecom.search.dto.ProductIndexRequest;
import com.ecom.search.dto.ProductSearchPageResponse;
import com.ecom.search.dto.ProductSearchResponse;
import com.ecom.search.dto.ReindexResponse;
import com.ecom.search.model.SearchProductDocument;
import com.ecom.search.repository.SearchProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SearchService implements SearchUseCases {

    private final SearchProductRepository repository;
    private final ElasticsearchOperations operations;
    private final ObjectMapper objectMapper;
    private final RestClient productClient;

    public SearchService(
            SearchProductRepository repository,
            ElasticsearchOperations operations,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${app.search.product-service-base-url:http://localhost:8083}") String productServiceBaseUrl) {
        this.repository = repository;
        this.operations = operations;
        this.objectMapper = objectMapper;
        this.productClient = restClientBuilder.baseUrl(productServiceBaseUrl).build();
    }

    @Override
    public ProductSearchResponse upsertProduct(ProductIndexRequest request) {
        SearchProductDocument saved = repository.save(map(request));
        return toResponse(saved);
    }

    @Override
    public List<ProductSearchResponse> bulkUpsert(List<ProductIndexRequest> requests) {
        List<SearchProductDocument> docs = requests.stream().map(this::map).toList();
        Iterable<SearchProductDocument> saved = repository.saveAll(docs);
        List<ProductSearchResponse> responses = new ArrayList<>();
        saved.forEach(doc -> responses.add(toResponse(doc)));
        return responses;
    }

    @Override
    public void deleteProduct(String productId) {
        repository.deleteById(productId);
    }

    @Override
    public ProductSearchPageResponse search(
            String q,
            String category,
            String brand,
            boolean activeOnly,
            int page,
            int size,
            String sortBy,
            String direction) {
        validatePaging(page, size);
        String queryDsl = buildSearchDsl(q, category, brand, activeOnly, page, size, sortBy, direction);
        SearchHits<SearchProductDocument> hits = operations.search(new StringQuery(queryDsl), SearchProductDocument.class);

        List<ProductSearchResponse> content = hits.stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();

        return new ProductSearchPageResponse(content, hits.getTotalHits(), page, size);
    }

    @Override
    public List<String> autocomplete(String q, int size) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int safeSize = Math.max(1, Math.min(size, 20));
        String queryDsl = buildAutocompleteDsl(q, safeSize);
        SearchHits<SearchProductDocument> hits = operations.search(new StringQuery(queryDsl), SearchProductDocument.class);

        return hits.stream()
                .map(SearchHit::getContent)
                .flatMap(doc -> java.util.stream.Stream.of(doc.getName(), doc.getBrand()))
                .filter(v -> v != null && v.toLowerCase().startsWith(q.trim().toLowerCase()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    @Override
    public ReindexResponse reindexFromProductService(boolean purgeFirst, int pageSize) {
        int safePageSize = Math.max(1, Math.min(pageSize, 500));
        if (purgeFirst) {
            repository.deleteAll();
        }

        int page = 0;
        int pagesProcessed = 0;
        int fetched = 0;
        int indexed = 0;

        while (true) {
            ProductPageResponse productPage = fetchProductPage(page, safePageSize);
            if (productPage == null || productPage.content() == null || productPage.content().isEmpty()) {
                break;
            }

            fetched += productPage.content().size();
            List<ProductIndexRequest> requests = productPage.content().stream()
                    .map(this::toIndexRequest)
                    .toList();
            indexed += bulkUpsert(requests).size();

            pagesProcessed++;
            if (productPage.last()) {
                break;
            }
            page++;
        }

        return new ReindexResponse(purgeFirst, pagesProcessed, fetched, indexed, Instant.now());
    }

    private SearchProductDocument map(ProductIndexRequest request) {
        SearchProductDocument doc = new SearchProductDocument();
        doc.setProductId(request.productId());
        doc.setName(request.name());
        doc.setDescription(request.description());
        doc.setCategory(request.category());
        doc.setBrand(request.brand());
        doc.setPrice(request.price());
        doc.setColors(request.colors());
        doc.setSizes(request.sizes());
        doc.setActive(request.active() == null ? true : request.active());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }

    private ProductSearchResponse toResponse(SearchProductDocument doc) {
        return new ProductSearchResponse(
                doc.getProductId(),
                doc.getName(),
                doc.getDescription(),
                doc.getCategory(),
                doc.getBrand(),
                doc.getPrice(),
                doc.getColors(),
                doc.getSizes(),
                doc.getActive(),
                doc.getUpdatedAt());
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private String buildSearchDsl(
            String q,
            String category,
            String brand,
            boolean activeOnly,
            int page,
            int size,
            String sortBy,
            String direction) {
        String mustClause = (q == null || q.isBlank()) ? "{\"match_all\":{}}" : textQueryClause(q.trim());

        List<String> shouldClauses = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            shouldClauses.add("{\"match_phrase\":{\"name\":{\"query\":" + json(q.trim()) + ",\"boost\":8}}}");
            shouldClauses.add("{\"match_phrase_prefix\":{\"name\":{\"query\":" + json(q.trim()) + ",\"boost\":5}}}");
            shouldClauses.add("{\"match_phrase_prefix\":{\"brand\":{\"query\":" + json(q.trim()) + ",\"boost\":2}}}");
        }

        List<String> filters = new ArrayList<>();
        if (activeOnly) {
            filters.add("{\"term\":{\"active\":true}}");
        }
        if (category != null && !category.isBlank()) {
            filters.add("{\"term\":{\"category\":" + json(category.trim()) + "}}");
        }
        if (brand != null && !brand.isBlank()) {
            filters.add("{\"term\":{\"brand\":" + json(brand.trim()) + "}}");
        }

        String filterSection = filters.isEmpty() ? "" : ",\"filter\":[" + String.join(",", filters) + "]";
        String shouldSection = shouldClauses.isEmpty() ? "" : ",\"should\":[" + String.join(",", shouldClauses) + "],\"minimum_should_match\":0";
        String order = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
        String sortClause = resolveSortClause(sortBy, order, q);

        return "{"
                + "\"from\":" + (page * size) + ","
                + "\"size\":" + size + ","
                + "\"query\":{\"bool\":{\"must\":[" + mustClause + "]" + filterSection + shouldSection + "}},"
                + "\"sort\":[" + sortClause + "]"
                + "}";
    }

    private String buildAutocompleteDsl(String q, int size) {
        return "{"
                + "\"size\":" + size + ","
                + "\"query\":{\"bool\":{\"should\":["
                + "{\"match_phrase_prefix\":{\"name\":{\"query\":" + json(q.trim()) + "}}},"
                + "{\"match_phrase_prefix\":{\"brand\":{\"query\":" + json(q.trim()) + "}}}"
                + "],\"minimum_should_match\":1}},"
                + "\"sort\":[{\"_score\":{\"order\":\"desc\"}}]"
                + "}";
    }

    private String resolveSortClause(String sortBy, String order, String q) {
        if ("price".equalsIgnoreCase(sortBy)) {
            return "{\"price\":{\"order\":\"" + order + "\"}},{\"_score\":{\"order\":\"desc\"}}";
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            return "{\"name.keyword\":{\"order\":\"" + order + "\"}},{\"_score\":{\"order\":\"desc\"}}";
        }
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            return "{\"updatedAt\":{\"order\":\"" + order + "\"}},{\"_score\":{\"order\":\"desc\"}}";
        }
        if (q == null || q.isBlank()) {
            return "{\"updatedAt\":{\"order\":\"desc\"}}";
        }
        return "{\"_score\":{\"order\":\"desc\"}}";
    }

    private String textQueryClause(String q) {
        return "{\"multi_match\":{\"query\":" + json(q)
                + ",\"fields\":[\"name^4\",\"description^2\",\"brand^2\",\"category\"],\"fuzziness\":\"AUTO\",\"operator\":\"and\"}}";
    }

    private ProductPageResponse fetchProductPage(int page, int size) {
        return productClient.get()
                .uri(uriBuilder -> buildProductPageUri(uriBuilder, page, size))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProductPageResponse.class);
    }

    private java.net.URI buildProductPageUri(UriBuilder uriBuilder, int page, int size) {
        return uriBuilder
                .path("/api/products")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sortBy", "updatedAt")
                .queryParam("direction", "desc")
                .build();
    }

    private ProductIndexRequest toIndexRequest(ProductFromProductService product) {
        return new ProductIndexRequest(
                product.id(),
                product.name(),
                product.description(),
                product.category(),
                product.brand(),
                product.price(),
                product.colors(),
                product.sizes(),
                product.active());
    }

    private record ProductPageResponse(
            List<ProductFromProductService> content,
            boolean last) {
    }

    private record ProductFromProductService(
            String id,
            String name,
            String description,
            String category,
            String brand,
            java.math.BigDecimal price,
            List<String> colors,
            List<String> sizes,
            Boolean active,
            Map<String, Object> ignored) {
    }

    private String json(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not encode query value", ex);
        }
    }
}
