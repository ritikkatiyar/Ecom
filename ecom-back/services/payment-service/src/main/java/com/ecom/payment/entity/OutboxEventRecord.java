package com.ecom.payment.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_outbox_events", indexes = {
        @Index(name = "idx_payment_outbox_status", columnList = "status,createdAt")
})
public class OutboxEventRecord {

    @Id
    @Column(nullable = false, length = 120)
    private String id;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(nullable = false, length = 120)
    private String messageKey;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 20000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(length = 500)
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
