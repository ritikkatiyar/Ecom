package com.ecom.search.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.ecom.search.model.SearchProductDocument;

@Repository
public class SearchProductRepository {

    private final SolrClient solrClient;
    private final String productsCore;

    public SearchProductRepository(
            SolrClient solrClient,
            @Value("${app.search.solr.products-core:products}") String productsCore) {
        this.solrClient = solrClient;
        this.productsCore = productsCore;
    }

    public SearchProductDocument save(SearchProductDocument document) {
        updateDocuments(List.of(document));
        return document;
    }

    public Iterable<SearchProductDocument> saveAll(List<SearchProductDocument> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        updateDocuments(documents);
        return documents;
    }

    public void deleteById(String productId) {
        try {
            solrClient.deleteById(productsCore, productId);
            solrClient.commit(productsCore);
        } catch (Exception ex) {
            throw new SearchRepositoryException("Could not delete product from Solr index", ex);
        }
    }

    public void deleteAll() {
        try {
            solrClient.deleteByQuery(productsCore, "*:*");
            solrClient.commit(productsCore);
        } catch (Exception ex) {
            throw new SearchRepositoryException("Could not clear Solr product core", ex);
        }
    }

    public SearchPage search(
            String q,
            String category,
            String brand,
            boolean activeOnly,
            int page,
            int size,
            String sortBy,
            String direction) {
        SolrQuery query = new SolrQuery();
        boolean hasSearchTerm = q != null && !q.isBlank();
        query.setStart(page * size);
        query.setRows(size);
        query.setFields("id", "name", "description", "category", "brand", "price", "colors", "sizes", "active", "updatedAt", "score");

        if (hasSearchTerm) {
            String escaped = ClientUtils.escapeQueryChars(q.trim());
            query.setQuery(escaped);
            query.set("defType", "edismax");
            query.set("qf", "name^4 description^2 brand_text^2 category_text");
            query.set("pf", "name^8");
            query.set("pf2", "name^5 brand_text^2");
            query.set("mm", "100%");
        } else {
            query.setQuery("*:*");
        }

        if (activeOnly) {
            query.addFilterQuery("active:true");
        }
        if (category != null && !category.isBlank()) {
            query.addFilterQuery("category:" + quote(category));
        }
        if (brand != null && !brand.isBlank()) {
            query.addFilterQuery("brand:" + quote(brand));
        }

        applySorting(query, sortBy, direction, hasSearchTerm);

        try {
            QueryResponse response = solrClient.query(productsCore, query);
            List<SearchProductDocument> documents = response.getBeans(SearchProductDocument.class);
            long totalHits = response.getResults() == null ? 0 : response.getResults().getNumFound();
            return new SearchPage(documents, totalHits);
        } catch (Exception ex) {
            throw new SearchRepositoryException("Could not execute Solr product search", ex);
        }
    }

    public List<String> autocomplete(String q, int size) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }

        SolrQuery query = new SolrQuery();
        query.setRows(size * 3);
        query.setFields("name", "brand");
        String escapedPrefix = ClientUtils.escapeQueryChars(trimmed.toLowerCase());
        query.setQuery("name:" + escapedPrefix + "* OR brand_text:" + escapedPrefix + "*");
        query.addSort(SolrQuery.SortClause.desc("score"));

        try {
            QueryResponse response = solrClient.query(productsCore, query);
            List<SearchProductDocument> documents = response.getBeans(SearchProductDocument.class);
            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            for (SearchProductDocument document : documents) {
                addSuggestion(suggestions, document.getName(), trimmed, size);
                addSuggestion(suggestions, document.getBrand(), trimmed, size);
                if (suggestions.size() >= size) {
                    break;
                }
            }
            return new ArrayList<>(suggestions);
        } catch (Exception ex) {
            throw new SearchRepositoryException("Could not execute Solr autocomplete query", ex);
        }
    }

    private void updateDocuments(List<SearchProductDocument> documents) {
        try {
            solrClient.addBeans(productsCore, documents);
            solrClient.commit(productsCore);
        } catch (Exception ex) {
            throw new SearchRepositoryException("Could not write product documents to Solr", ex);
        }
    }

    private void applySorting(SolrQuery query, String sortBy, String direction, boolean hasSearchTerm) {
        SolrQuery.ORDER order = "asc".equalsIgnoreCase(direction) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
        if ("price".equalsIgnoreCase(sortBy)) {
            query.addSort("price", order);
            query.addSort("score", SolrQuery.ORDER.desc);
            return;
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            query.addSort("name_sort", order);
            query.addSort("score", SolrQuery.ORDER.desc);
            return;
        }
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            query.addSort("updatedAt", order);
            query.addSort("score", SolrQuery.ORDER.desc);
            return;
        }
        if (!hasSearchTerm) {
            query.addSort("updatedAt", SolrQuery.ORDER.desc);
            return;
        }
        query.addSort("score", SolrQuery.ORDER.desc);
    }

    private void addSuggestion(LinkedHashSet<String> suggestions, String candidate, String prefix, int size) {
        if (candidate == null || suggestions.size() >= size || !candidate.toLowerCase().startsWith(prefix.toLowerCase())) {
            return;
        }
        suggestions.add(candidate);
    }

    private String quote(String value) {
        return "\"" + ClientUtils.escapeQueryChars(value.trim()) + "\"";
    }

    public record SearchPage(
            List<SearchProductDocument> content,
            long totalHits) {
    }

    public static class SearchRepositoryException extends RuntimeException {
        public SearchRepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
