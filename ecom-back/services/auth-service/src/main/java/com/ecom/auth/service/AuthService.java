package com.ecom.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.auth.dto.LoginRequest;
import com.ecom.auth.dto.RefreshRequest;
import com.ecom.auth.dto.SignupRequest;
import com.ecom.auth.dto.TokenResponse;
import com.ecom.auth.entity.RefreshToken;
import com.ecom.auth.entity.UserAccount;
import com.ecom.auth.repository.RefreshTokenRepository;
import com.ecom.auth.repository.UserAccountRepository;

import io.jsonwebtoken.Claims;

@Service
public class AuthService implements AuthUseCases {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public AuthService(
            UserAccountRepository userRepo,
            RefreshTokenRepository refreshRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            AuthTokenIssuer authTokenIssuer,
            RefreshTokenGenerator refreshTokenGenerator) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authTokenIssuer = authTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    @Override
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepo.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null || request.role().isBlank() ? "CUSTOMER" : request.role());
        UserAccount saved = userRepo.save(user);

        return authTokenIssuer.issueTokens(saved);
    }

    @Override
    @Transactional
    public TokenResponse oauthLogin(String email) {
        UserAccount user = userRepo.findByEmail(email).orElseGet(() -> {
            UserAccount newUser = new UserAccount();
            newUser.setEmail(email);
            newUser.setPasswordHash(passwordEncoder.encode(refreshTokenGenerator.nextToken()));
            newUser.setRole("CUSTOMER");
            return userRepo.save(newUser);
        });
        return authTokenIssuer.issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("Login attempt received for email={}", request.email());
        UserAccount user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for email={}", request.email());
                    return new IllegalArgumentException("User not found");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for email={}", request.email());
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("Login success for email={} userId={}", user.getEmail(), user.getId());
        return authTokenIssuer.issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshRepo.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        UserAccount user = userRepo.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        token.setRevoked(true);
        refreshRepo.save(token);

        return authTokenIssuer.issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        Claims claims = jwtService.parse(accessToken);
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Access token does not contain token identifier");
        }
        tokenBlacklistService.blacklist(jti, claims.getExpiration().toInstant());

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshRepo.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshRepo.save(token);
            });
        }
    }

    @Override
    public boolean isAccessTokenActive(String accessToken) {
        try {
            Claims claims = jwtService.parse(accessToken);
            String jti = claims.getId();
            return jti != null && !jti.isBlank() && !tokenBlacklistService.isBlacklisted(jti);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
