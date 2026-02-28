package com.ecom.order.entity;

import java.time.Instant;

import com.ecom.common.reliability.RetryableOutboxRecord;

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
@Table(name = "order_outbox_events", indexes = {
        @Index(name = "idx_order_outbox_status", columnList = "status,created_at")
})
public class OutboxEventRecord implements RetryableOutboxRecord {

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
    private int attempts;

    @Column(length = 500)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public void markSent() {
        this.status = OutboxStatus.SENT;
    }

    @Override
    public void setAttempts(Integer attempts) {
        this.attempts = attempts == null ? 0 : attempts;
    }

    @Override
    public void markPending() {
        this.status = OutboxStatus.PENDING;
    }

    @Override
    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
