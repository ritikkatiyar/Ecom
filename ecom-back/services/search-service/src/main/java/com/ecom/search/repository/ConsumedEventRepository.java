package com.ecom.search.repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.ecom.search.entity.ConsumedEventRecord;

@Repository
public class ConsumedEventRepository {

    private final SolrClient solrClient;
    private final String dedupCore;

    public ConsumedEventRepository(
            SolrClient solrClient,
            @Value("${app.search.solr.dedup-core:search_consumed_events}") String dedupCore) {
        this.solrClient = solrClient;
        this.dedupCore = dedupCore;
    }

    public boolean existsById(String eventId) {
        SolrQuery query = new SolrQuery("id:" + quote(eventId));
        query.setRows(0);
        try {
            return solrClient.query(dedupCore, query).getResults().getNumFound() > 0;
        } catch (Exception ex) {
            throw new SearchProductRepository.SearchRepositoryException("Could not query Solr dedup core", ex);
        }
    }

    public ConsumedEventRecord save(ConsumedEventRecord record) {
        try {
            solrClient.addBean(dedupCore, record);
            solrClient.commit(dedupCore);
            return record;
        } catch (Exception ex) {
            throw new SearchProductRepository.SearchRepositoryException("Could not write Solr dedup record", ex);
        }
    }

    public long deleteByConsumedAtBefore(Instant cutoff) {
        String rangeQuery = "consumedAt:[* TO " + DateTimeFormatter.ISO_INSTANT.format(cutoff) + "]";
        SolrQuery countQuery = new SolrQuery(rangeQuery);
        countQuery.setRows(0);
        try {
            long count = solrClient.query(dedupCore, countQuery).getResults().getNumFound();
            if (count > 0) {
                solrClient.deleteByQuery(dedupCore, rangeQuery);
                solrClient.commit(dedupCore);
            }
            return count;
        } catch (Exception ex) {
            throw new SearchProductRepository.SearchRepositoryException("Could not delete expired Solr dedup records", ex);
        }
    }

    private String quote(String value) {
        return "\"" + ClientUtils.escapeQueryChars(value) + "\"";
    }
}
