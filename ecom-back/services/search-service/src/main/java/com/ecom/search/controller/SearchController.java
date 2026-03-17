package com.ecom.search.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.search.dto.ProductIndexRequest;
import com.ecom.search.dto.ProductSearchPageResponse;
import com.ecom.search.dto.ProductSearchResponse;
import com.ecom.search.dto.RelevanceDatasetHealthResponse;
import com.ecom.search.dto.RelevanceEvaluationResponse;
import com.ecom.search.dto.ReindexResponse;
import com.ecom.search.service.SearchUseCases;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final SearchUseCases searchService;

    public SearchController(SearchUseCases searchService) {
        this.searchService = searchService;
    }

    /**
     * Indexes or updates a single product document in the search backend.
     */
    @PostMapping("/index/products")
    public ResponseEntity<ProductSearchResponse> upsertProduct(@Valid @RequestBody ProductIndexRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(searchService.upsertProduct(request));
    }

    /**
     * Indexes or updates multiple product documents in one request.
     */
    @PostMapping("/index/products/bulk")
    public ResponseEntity<List<ProductSearchResponse>> bulkUpsert(@Valid @RequestBody List<ProductIndexRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(searchService.bulkUpsert(requests));
    }

    /**
     * Removes a product document from the search index.
     */
    @DeleteMapping("/index/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String productId) {
        searchService.deleteProduct(productId);
    }

    /**
     * Searches indexed products with filters, pagination, and sorting.
     */
    @GetMapping("/products")
    public ProductSearchPageResponse search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "score") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction) {
        return searchService.search(q, category, brand, activeOnly, page, size, sortBy, direction);
    }

    /**
     * Returns autocomplete suggestions for a partial product query.
     */
    @GetMapping("/autocomplete")
    public List<String> autocomplete(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return searchService.autocomplete(q, size);
    }

    /**
     * Rebuilds the product search index from product-service data.
     */
    @PostMapping("/reindex/products")
    public ReindexResponse reindexProducts(
            @RequestParam(name = "purgeFirst", defaultValue = "true") boolean purgeFirst,
            @RequestParam(name = "pageSize", defaultValue = "200") int pageSize) {
        return searchService.reindexFromProductService(purgeFirst, pageSize);
    }

    /**
     * Runs the offline relevance evaluation dataset and returns quality metrics.
     */
    @GetMapping("/admin/relevance/evaluate")
    public RelevanceEvaluationResponse evaluateRelevance(
            @RequestParam(name = "topN", defaultValue = "5") int topN) {
        return searchService.evaluateRelevanceDataset(topN);
    }

    /**
     * Reports coverage and integrity checks for the relevance evaluation dataset.
     */
    @GetMapping("/admin/relevance/dataset/health")
    public RelevanceDatasetHealthResponse evaluateRelevanceDatasetHealth() {
        return searchService.evaluateRelevanceDatasetHealth();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
