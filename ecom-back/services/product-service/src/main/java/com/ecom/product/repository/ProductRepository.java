package com.ecom.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecom.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
}
