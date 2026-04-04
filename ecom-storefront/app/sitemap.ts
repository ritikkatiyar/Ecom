import type { MetadataRoute } from "next";
import { getProducts } from "@/lib/api/products";
import { siteUrl } from "@/lib/seo";

const staticRoutes = [
  "",
  "/shop",
  "/collections",
  "/search",
  "/login",
  "/signup",
  "/cart",
  "/checkout",
  "/feedback",
];

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date();
  let productRoutes: MetadataRoute.Sitemap = [];

  try {
    const page = await getProducts(
      { page: 0, size: 200, sortBy: "name", direction: "asc" },
      { revalidateSeconds: 3600 }
    );
    productRoutes = page.content
      .filter((product) => product.active)
      .map((product) => ({
        url: `${siteUrl}/products/${product.id}`,
        lastModified: now,
        changeFrequency: "weekly" as const,
        priority: 0.8,
      }));
  } catch {
    productRoutes = [];
  }

  return [
    ...staticRoutes.map((route) => ({
      url: `${siteUrl}${route}`,
      lastModified: now,
      changeFrequency: route === "" ? "daily" as const : "weekly" as const,
      priority: route === "" ? 1 : 0.7,
    })),
    ...productRoutes,
  ];
}
