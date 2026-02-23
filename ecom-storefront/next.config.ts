import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const backend =
      process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
    // Auth excluded: app/api/auth/[...path]/route.ts proxies POST correctly.
    // External rewrites can return 405 for POST.
    const apiPaths = [
      "products",
      "cart",
      "orders",
      "inventory",
      "search",
      "payments",
      "reviews",
      "users",
      "notifications",
      "internal",
    ];
    return apiPaths.map((p) => ({
      source: `/api/${p}/:path*`,
      destination: `${backend}/api/${p}/:path*`,
    }));
  },
};

export default nextConfig;
