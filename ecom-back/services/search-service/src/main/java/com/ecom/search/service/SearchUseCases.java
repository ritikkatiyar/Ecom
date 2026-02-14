package com.ecom.search.service;

import java.util.List;

import com.ecom.search.dto.ProductIndexRequest;
import com.ecom.search.dto.ProductSearchPageResponse;
import com.ecom.search.dto.ProductSearchResponse;
import com.ecom.search.dto.ReindexResponse;

public interface SearchUseCases {

    ProductSearchResponse upsertProduct(ProductIndexRequest request);

    List<ProductSearchResponse> bulkUpsert(List<ProductIndexRequest> requests);

    void deleteProduct(String productId);

    ProductSearchPageResponse search(
            String q,
            String category,
            String brand,
            boolean activeOnly,
            int page,
            int size,
            String sortBy,
            String direction);

    List<String> autocomplete(String q, int size);

    ReindexResponse reindexFromProductService(boolean purgeFirst, int pageSize);
}
