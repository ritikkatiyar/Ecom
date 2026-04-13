package com.ecom.search.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchProductDocument {

    @Field("id")
    private String productId;

    @Field
    private String name;

    @Field
    private String description;

    @Field
    private String category;

    @Field
    private String brand;

    @Field
    private BigDecimal price;

    @Field
    private List<String> colors;

    @Field
    private List<String> sizes;

    @Field
    private Boolean active;

    @Field
    private Instant updatedAt;
}
