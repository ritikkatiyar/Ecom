"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { setAccessTokenProvider, setOn401Handler } from "@/lib/apiClient";
import * as authApi from "@/lib/api/auth";
import { getRolesFromToken, isTokenExpired } from "@/lib/utils/jwt";

export interface AuthState {
  isAuthenticated: boolean;
  roles: string[];
  userId: string | null;
  isLoading: boolean;
}

export interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<string[]>;
  signup: (email: string, password: string, role?: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const STORAGE_ACCESS = "ecom_access_token";
const STORAGE_REFRESH = "ecom_refresh_token";

function loadStoredTokens(): {
  accessToken: string | null;
  refreshToken: string | null;
} {
  if (typeof window === "undefined") return { accessToken: null, refreshToken: null };
  const access = sessionStorage.getItem(STORAGE_ACCESS);
  const refresh = sessionStorage.getItem(STORAGE_REFRESH);
  return {
    accessToken: access,
    refreshToken: refresh,
  };
}

function saveTokens(accessToken: string, refreshToken: string): void {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(STORAGE_ACCESS, accessToken);
  sessionStorage.setItem(STORAGE_REFRESH, refreshToken);
}

function clearTokens(): void {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(STORAGE_ACCESS);
  sessionStorage.removeItem(STORAGE_REFRESH);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const roles = useMemo(() => {
    if (!accessToken) return [];
    return getRolesFromToken(accessToken);
  }, [accessToken]);

  const userId = useMemo(() => {
    if (!accessToken) return null;
    try {
      const parts = accessToken.split(".");
      if (parts.length !== 3) return null;
      const payload = JSON.parse(
        atob(parts[1].replace(/-/g, "+").replace(/_/g, "/"))
      );
      return payload.sub ?? null;
    } catch {
      return null;
    }
  }, [accessToken]);

  const refreshSession = useCallback(async (): Promise<boolean> => {
    const stored = loadStoredTokens();
    const rt = refreshToken ?? stored.refreshToken;
    if (!rt) return false;

    try {
      const res = await authApi.refresh(rt);
      setAccessToken(res.accessToken);
      setRefreshToken(res.refreshToken);
      saveTokens(res.accessToken, res.refreshToken);
      return true;
    } catch {
      setAccessToken(null);
      setRefreshToken(null);
      clearTokens();
      return false;
    }
  }, [refreshToken]);

  /** Returns new access token for apiClient 401 retry. */
  const handle401 = useCallback(async (): Promise<string | null> => {
    const stored = loadStoredTokens();
    const rt = stored.refreshToken;
    if (!rt) return null;
    try {
      const res = await authApi.refresh(rt);
      setAccessToken(res.accessToken);
      setRefreshToken(res.refreshToken);
      saveTokens(res.accessToken, res.refreshToken);
      return res.accessToken;
    } catch {
      setAccessToken(null);
      setRefreshToken(null);
      clearTokens();
      return null;
    }
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<string[]> => {
    const res = await authApi.login({ email, password });
    setAccessToken(res.accessToken);
    setRefreshToken(res.refreshToken);
    saveTokens(res.accessToken, res.refreshToken);
    return getRolesFromToken(res.accessToken);
  }, []);

  const signup = useCallback(async (email: string, password: string, role?: string) => {
    const res = await authApi.signup({ email, password, role });
    setAccessToken(res.accessToken);
    setRefreshToken(res.refreshToken);
    saveTokens(res.accessToken, res.refreshToken);
  }, []);

  const logout = useCallback(async () => {
    const access = accessToken ?? loadStoredTokens().accessToken;
    const refresh = refreshToken ?? loadStoredTokens().refreshToken;
    if (access) {
      try {
        await authApi.logout(access, refresh);
      } catch {
        // ignore - clear state regardless
      }
    }
    setAccessToken(null);
    setRefreshToken(null);
    clearTokens();
  }, [accessToken, refreshToken]);

  useEffect(() => {
    const stored = loadStoredTokens();
    if (stored.accessToken && stored.refreshToken) {
      if (isTokenExpired(stored.accessToken)) {
        authApi
          .refresh(stored.refreshToken)
          .then((res) => {
            setAccessToken(res.accessToken);
            setRefreshToken(res.refreshToken);
            saveTokens(res.accessToken, res.refreshToken);
          })
          .catch(() => {
            clearTokens();
          })
          .finally(() => setIsLoading(false));
      } else {
        setAccessToken(stored.accessToken);
        setRefreshToken(stored.refreshToken);
        setIsLoading(false);
      }
    } else {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    setAccessTokenProvider(() => accessToken);
  }, [accessToken]);

  useEffect(() => {
    setOn401Handler(handle401);
    return () => setOn401Handler(null);
  }, [handle401]);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: !!accessToken,
      roles,
      userId,
      isLoading,
      login,
      signup,
      logout,
      refreshSession,
    }),
    [accessToken, roles, userId, isLoading, login, signup, logout, refreshSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
