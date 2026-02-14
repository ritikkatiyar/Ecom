package com.ecom.notification.entity;

import java.time.Instant;

import com.ecom.common.reliability.EventConsumptionRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_consumed_events")
public class ConsumedEventRecord implements EventConsumptionRecord {

    @Id
    @Column(nullable = false, length = 120)
    private String eventId;

    @Column(nullable = false)
    private Instant consumedAt;
}
