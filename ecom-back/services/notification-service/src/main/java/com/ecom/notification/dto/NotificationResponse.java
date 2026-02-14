package com.ecom.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String eventId,
        String eventType,
        Long userId,
        String recipientEmail,
        String subject,
        String body,
        String status,
        Integer retryCount,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
}
