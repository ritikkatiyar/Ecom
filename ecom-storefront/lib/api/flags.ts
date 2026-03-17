/**
 * Frontend flags from gateway (beta banner, admin console).
 * Fetched from GET /api/internal/frontend-flags (proxied to gateway /internal/frontend-flags).
 */

import { apiClient } from "../apiClient";

export interface FrontendFlags {
  betaBannerEnabled: boolean;
  adminConsoleEnabled: boolean;
}

const DEFAULT_FLAGS: FrontendFlags = {
  betaBannerEnabled: true,
  adminConsoleEnabled: true, // Allow admin in dev when backend may be unreachable
};

export async function getFrontendFlags(): Promise<FrontendFlags> {
  try {
    const res = await apiClient<FrontendFlags>("/api/internal/frontend-flags", {
      skipAuth: true,
    });
    return res;
  } catch {
    return DEFAULT_FLAGS;
  }
}
