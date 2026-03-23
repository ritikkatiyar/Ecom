/**
 * Frontend flags from gateway.
 * Fetched from GET /api/internal/frontend-flags (proxied to gateway /internal/frontend-flags).
 */

import { apiClient } from "../apiClient";

export interface FrontendFlags {
  betaBannerEnabled: boolean;
}

const DEFAULT_FLAGS: FrontendFlags = {
  betaBannerEnabled: true,
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
