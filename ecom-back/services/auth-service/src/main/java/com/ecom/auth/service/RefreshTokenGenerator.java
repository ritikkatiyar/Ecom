package com.ecom.auth.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 48;

    private final SecureRandom secureRandom = new SecureRandom();

    public String nextToken() {
        byte[] buffer = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
