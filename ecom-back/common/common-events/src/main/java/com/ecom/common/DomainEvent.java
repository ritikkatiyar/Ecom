package com.ecom.common;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        String schemaVersion,
        String traceId,
        T payload
) {
}
