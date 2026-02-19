package com.ecom.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Long defaultAddressId,
        Instant createdAt,
        Instant updatedAt) {
}
