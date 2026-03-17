"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
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
  const authVersionRef = useRef(0);
  const commitAccessToken = useCallback((token: string | null) => {
    setAccessToken(token);
    setAccessTokenProvider(() => token);
  }, []);

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
      commitAccessToken(res.accessToken);
      return true;
    } catch {
      commitAccessToken(null);
      return false;
    }
  }, [commitAccessToken]);

  /** Returns new access token for apiClient 401 retry. */
  const handle401 = useCallback(async (): Promise<string | null> => {
    try {
      const res = await authApi.refresh();
      commitAccessToken(res.accessToken);
      return res.accessToken;
    } catch {
      commitAccessToken(null);
      return null;
    }
  }, [commitAccessToken]);

  const login = useCallback(async (email: string, password: string): Promise<string[]> => {
    const res = await authApi.login({ email, password });
    authVersionRef.current += 1;
    commitAccessToken(res.accessToken);
    setIsLoading(false);
    return getRolesFromToken(res.accessToken);
  }, [commitAccessToken]);

  const signup = useCallback(async (email: string, password: string, role?: string) => {
    const res = await authApi.signup({ email, password, role });
    authVersionRef.current += 1;
    commitAccessToken(res.accessToken);
    setIsLoading(false);
  }, [commitAccessToken]);

  const logout = useCallback(async () => {
    if (accessToken) {
      try {
        await authApi.logout(accessToken);
      } catch {
        // ignore - clear client auth state regardless
      }
    }
    authVersionRef.current += 1;
    commitAccessToken(null);
    setIsLoading(false);
  }, [accessToken, commitAccessToken]);

  useEffect(() => {
    const bootstrapVersion = authVersionRef.current;
    authApi
      .refresh()
      .then((res) => {
        if (authVersionRef.current !== bootstrapVersion) return;
        commitAccessToken(res.accessToken);
      })
      .catch(() => {
        if (authVersionRef.current !== bootstrapVersion) return;
        commitAccessToken(null);
      })
      .finally(() => {
        if (authVersionRef.current !== bootstrapVersion) return;
        setIsLoading(false);
      });
  }, [commitAccessToken]);

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
