package com.ecom.payment.entity;

import java.time.Instant;

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
@Table(name = "payment_consumed_events")
public class ConsumedEventRecord {

    @Id
    @Column(nullable = false, length = 120)
    private String eventId;

    @Column(nullable = false)
    private Instant consumedAt;
}
