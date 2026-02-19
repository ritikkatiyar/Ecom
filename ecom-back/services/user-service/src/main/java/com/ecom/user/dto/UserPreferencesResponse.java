package com.ecom.user.dto;

import java.time.Instant;

public record UserPreferencesResponse(
        Long userId,
        boolean marketingEmailsEnabled,
        boolean smsEnabled,
        String preferredLanguage,
        String preferredCurrency,
        Instant createdAt,
        Instant updatedAt) {
}
