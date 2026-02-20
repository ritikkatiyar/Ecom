package com.ecom.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecom.auth.dto.TokenResponse;
import com.ecom.auth.entity.RefreshToken;
import com.ecom.auth.entity.UserAccount;
import com.ecom.auth.repository.RefreshTokenRepository;

@Component
public class AuthTokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final long refreshTokenTtlDays;

    public AuthTokenIssuer(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            RefreshTokenGenerator refreshTokenGenerator,
            @Value("${security.jwt.refresh-token-ttl-days:14}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public TokenResponse issueTokens(UserAccount user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getRole());

        RefreshToken refresh = new RefreshToken();
        refresh.setUserId(user.getId());
        refresh.setToken(refreshTokenGenerator.nextToken());
        refresh.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refresh);

        return new TokenResponse(access, refresh.getToken(), "Bearer", jwtService.accessTokenTtlSeconds());
    }
}
