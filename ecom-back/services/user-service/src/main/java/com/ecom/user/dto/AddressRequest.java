package com.ecom.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 40) String label,
        @NotBlank @Size(max = 180) String line1,
        @Size(max = 180) String line2,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 60) String country,
        boolean defaultAddress) {
}
