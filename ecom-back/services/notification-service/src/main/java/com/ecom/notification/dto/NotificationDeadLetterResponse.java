package com.ecom.notification.dto;

import java.time.Instant;

public record NotificationDeadLetterResponse(
        Long id,
        String eventId,
        String eventType,
        Long userId,
        String recipientEmail,
        String subject,
        String body,
        String status,
        Integer retries,
        String reason,
        String sourceTopic,
        Instant createdAt,
        Instant updatedAt) {
}
