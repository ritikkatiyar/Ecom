package com.ecom.search.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfig {

    @Bean(destroyMethod = "close")
    public SolrClient solrClient(@Value("${app.search.solr.base-url:http://127.0.0.1:8983/solr}") String baseUrl) {
        return new Http2SolrClient.Builder(baseUrl).build();
    }

    @Bean
    public ApplicationRunner solrInitializer(
            SolrClient solrClient,
            @Value("${app.search.solr.products-core:products}") String productsCore,
            @Value("${app.search.solr.dedup-core:search_consumed_events}") String dedupCore) {
        return args -> {
            ensureCore(solrClient, productsCore);
            ensureCore(solrClient, dedupCore);

            ensureFields(solrClient, productsCore, productFields());
            ensureFields(solrClient, dedupCore, dedupFields());
            ensureCopyFields(solrClient, productsCore, productCopyFields());
        };
    }

    private void ensureCore(SolrClient solrClient, String coreName) throws SolrServerException, IOException {
        CoreAdminResponse status = CoreAdminRequest.getStatus(coreName, solrClient);
        if (status.getCoreStatus(coreName) != null) {
            return;
        }

        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName(coreName);
        createRequest.setConfigSet("_default");
        createRequest.process(solrClient);
    }

    private void ensureFields(SolrClient solrClient, String coreName, List<Map<String, Object>> requiredFields)
            throws SolrServerException, IOException {
        SchemaResponse.FieldsResponse fieldsResponse = new SchemaRequest.Fields().process(solrClient, coreName);
        Map<String, Map<String, Object>> existingFields = new LinkedHashMap<>();
        for (Map<String, Object> field : fieldsResponse.getFields()) {
            existingFields.put(String.valueOf(field.get("name")), field);
        }

        List<Map<String, Object>> missingFields = new ArrayList<>();
        for (Map<String, Object> field : requiredFields) {
            String fieldName = String.valueOf(field.get("name"));
            if (!existingFields.containsKey(fieldName)) {
                missingFields.add(field);
            }
        }

        if (!missingFields.isEmpty()) {
            for (Map<String, Object> field : missingFields) {
                new SchemaRequest.AddField(field).process(solrClient, coreName);
            }
        }
    }

    private void ensureCopyFields(SolrClient solrClient, String coreName, List<CopyFieldDefinition> copyFields)
            throws SolrServerException, IOException {
        SchemaResponse.CopyFieldsResponse copyFieldsResponse = new SchemaRequest.CopyFields().process(solrClient, coreName);
        List<Map<String, Object>> existingCopyFields = copyFieldsResponse.getCopyFields();
        for (CopyFieldDefinition definition : copyFields) {
            boolean exists = existingCopyFields.stream().anyMatch(copyField ->
                    definition.source().equals(copyField.get("source"))
                            && definition.destination().equals(copyField.get("dest")));
            if (!exists) {
                new SchemaRequest.AddCopyField(definition.source(), List.of(definition.destination())).process(solrClient, coreName);
            }
        }
    }

    private List<Map<String, Object>> productFields() {
        return List.of(
                field("name", "text_general", true, true, false),
                field("description", "text_general", true, true, false),
                field("category", "string", true, true, false),
                field("category_text", "text_general", false, true, false),
                field("brand", "string", true, true, false),
                field("brand_text", "text_general", false, true, false),
                field("price", "pdouble", true, true, false),
                field("colors", "strings", true, true, true),
                field("sizes", "strings", true, true, true),
                field("active", "boolean", true, true, false),
                field("updatedAt", "pdate", true, true, false),
                field("name_sort", "string", false, true, false));
    }

    private List<Map<String, Object>> dedupFields() {
        return List.of(field("consumedAt", "pdate", true, true, false));
    }

    private List<CopyFieldDefinition> productCopyFields() {
        return List.of(
                new CopyFieldDefinition("name", "name_sort"),
                new CopyFieldDefinition("brand", "brand_text"),
                new CopyFieldDefinition("category", "category_text"));
    }

    private Map<String, Object> field(String name, String type, boolean stored, boolean indexed, boolean multiValued) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type", type);
        field.put("stored", stored);
        field.put("indexed", indexed);
        field.put("multiValued", multiValued);
        return field;
    }

    private record CopyFieldDefinition(
            String source,
            String destination) {
    }
}
