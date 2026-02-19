package com.ecom.user.dto;

import java.time.Instant;

public record AddressResponse(
        Long id,
        Long userId,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt) {
}
