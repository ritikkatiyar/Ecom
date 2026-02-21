package com.ecom.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ecom.product.dto.ProductRequest;
import com.ecom.product.model.Product;
import com.ecom.product.repository.ProductRepository;

@Service
public class ProductService implements ProductUseCases {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public ProductService(ProductRepository productRepository, MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Product create(ProductRequest request) {
        Product p = map(request, new Product());
        return productRepository.save(p);
    }

    public Product update(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return productRepository.save(map(request, existing));
    }

    public Product get(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }

    public Page<Product> search(String category, String brand, String q, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            criteriaList.add(Criteria.where("category").is(category));
        }
        if (brand != null && !brand.isBlank()) {
            criteriaList.add(Criteria.where("brand").is(brand));
        }
        if (q != null && !q.isBlank()) {
            String regex = ".*" + Pattern.quote(q.trim()) + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(Criteria[]::new)));
        }

        long total = mongoTemplate.count(query, Product.class);
        query.with(pageable);

        List<Product> content = mongoTemplate.find(query, Product.class);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    private Product map(ProductRequest request, Product p) {
        p.setName(request.name());
        p.setDescription(request.description());
        p.setCategory(request.category());
        p.setBrand(request.brand());
        p.setPrice(request.price());
        p.setColors(request.colors());
        p.setSizes(request.sizes());
        p.setActive(request.active() == null ? true : request.active());
        p.setImageUrls(request.imageUrls());
        return p;
    }
}
