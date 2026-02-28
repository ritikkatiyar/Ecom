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
import { getRolesFromToken } from "@/lib/utils/jwt";

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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
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
    try {
      const res = await authApi.refresh();
      setAccessToken(res.accessToken);
      return true;
    } catch {
      setAccessToken(null);
      return false;
    }
  }, []);

  /** Returns new access token for apiClient 401 retry. */
  const handle401 = useCallback(async (): Promise<string | null> => {
    try {
      const res = await authApi.refresh();
      setAccessToken(res.accessToken);
      return res.accessToken;
    } catch {
      setAccessToken(null);
      return null;
    }
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<string[]> => {
    const res = await authApi.login({ email, password });
    setAccessToken(res.accessToken);
    return getRolesFromToken(res.accessToken);
  }, []);

  const signup = useCallback(async (email: string, password: string, role?: string) => {
    const res = await authApi.signup({ email, password, role });
    setAccessToken(res.accessToken);
  }, []);

  const logout = useCallback(async () => {
    if (accessToken) {
      try {
        await authApi.logout(accessToken);
      } catch {
        // ignore - clear client auth state regardless
      }
    }
    setAccessToken(null);
  }, [accessToken]);

  useEffect(() => {
    authApi
      .refresh()
      .then((res) => setAccessToken(res.accessToken))
      .catch(() => setAccessToken(null))
      .finally(() => setIsLoading(false));
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
