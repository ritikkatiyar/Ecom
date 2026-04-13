package com.ecom.search.entity;

import java.time.Instant;

import com.ecom.common.reliability.EventConsumptionRecord;

import org.apache.solr.client.solrj.beans.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConsumedEventRecord implements EventConsumptionRecord {

    @Field("id")
    private String eventId;

    @Field
    private Instant consumedAt;
}
