/**
 * Auth API types and functions.
 */

import { apiClient } from "../apiClient";
import type { ApiRequestConfig } from "../types/api";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  role?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresInSeconds: number;
}

export async function login(
  data: LoginRequest,
  config?: ApiRequestConfig
): Promise<TokenResponse> {
  return apiClient<TokenResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(data),
    skipAuth: true,
    skipRetry: true,
    ...config,
  });
}

export async function signup(
  data: SignupRequest,
  config?: ApiRequestConfig
): Promise<TokenResponse> {
  return apiClient<TokenResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(data),
    skipAuth: true,
    skipRetry: true,
    ...config,
  });
}

export async function refresh(
  config?: ApiRequestConfig
): Promise<TokenResponse> {
  return apiClient<TokenResponse>("/api/auth/refresh", {
    method: "POST",
    body: JSON.stringify({}),
    skipAuth: true,
    skipRetry: true,
    ...config,
  });
}

export async function logout(
  accessToken: string
): Promise<void> {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
    "Content-Type": "application/json",
    "X-API-Version": "v1",
  };
  await apiClient<void>("/api/auth/logout", {
    method: "POST",
    headers,
    body: JSON.stringify({}),
    skipAuth: true,
    skipRetry: true,
  });
}
