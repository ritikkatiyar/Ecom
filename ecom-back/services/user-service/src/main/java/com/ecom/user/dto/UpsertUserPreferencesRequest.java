package com.ecom.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertUserPreferencesRequest(
        boolean marketingEmailsEnabled,
        boolean smsEnabled,
        @NotBlank @Size(max = 10) String preferredLanguage,
        @NotBlank @Size(max = 10) String preferredCurrency) {
}
