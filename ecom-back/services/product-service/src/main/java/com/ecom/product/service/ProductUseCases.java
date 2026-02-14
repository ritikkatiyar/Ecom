package com.ecom.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecom.product.dto.ProductRequest;
import com.ecom.product.model.Product;

public interface ProductUseCases {

    Product create(ProductRequest request);

    Product update(String id, ProductRequest request);

    Product get(String id);

    void delete(String id);

    Page<Product> search(String category, String brand, String q, Pageable pageable);
}
