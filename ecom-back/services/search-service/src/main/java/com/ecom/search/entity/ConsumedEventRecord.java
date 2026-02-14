package com.ecom.search.entity;

import java.time.Instant;

import com.ecom.common.reliability.EventConsumptionRecord;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "search_consumed_events")
public class ConsumedEventRecord implements EventConsumptionRecord {

    @Id
    private String eventId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant consumedAt;
}
