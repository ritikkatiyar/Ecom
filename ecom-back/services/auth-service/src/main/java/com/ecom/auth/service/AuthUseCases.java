package com.ecom.auth.service;

import com.ecom.auth.dto.LoginRequest;
import com.ecom.auth.dto.RefreshRequest;
import com.ecom.auth.dto.SignupRequest;
import com.ecom.auth.dto.TokenResponse;

public interface AuthUseCases {

    TokenResponse signup(SignupRequest request);

    TokenResponse oauthLogin(String email);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);

    void logout(String accessToken, String refreshToken);

    boolean isAccessTokenActive(String accessToken);
}
