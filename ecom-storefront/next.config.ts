import type { NextConfig } from "next";
import { getBackendBaseUrl } from "./lib/backendUrl";

const nextConfig: NextConfig = {
  eslint: {
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    const backend = getBackendBaseUrl();
    // Auth excluded: app/api/auth/[...path]/route.ts proxies POST correctly.
    // Gateway serves /internal/* directly (not under /api).
    const rewrites = [
      {
        source: "/api/internal/frontend-flags",
        destination: `${backend}/internal/frontend-flags`,
      },
    ];
    const apiPaths = [
      "products",
      "orders",
      "inventory",
      "search",
      "payments",
      "reviews",
      "users",
      "notifications",
      "internal",
    ];
    const apiRewrites = apiPaths.map((p) => ({
      source: `/api/${p}/:path*`,
      destination: `${backend}/api/${p}/:path*`,
    }));
    return [...rewrites, ...apiRewrites];
  },
};

export default nextConfig;
