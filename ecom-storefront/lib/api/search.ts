import { apiClient } from "../apiClient";
import type { SearchProductPage } from "../types/search";

export interface SearchProductsParams {
  q?: string;
  category?: string;
  brand?: string;
  page?: number;
  size?: number;
  activeOnly?: boolean;
  sortBy?: string;
  direction?: string;
}

export async function searchProducts(
  params: SearchProductsParams = {}
): Promise<SearchProductPage> {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  if (params.category) search.set("category", params.category);
  if (params.brand) search.set("brand", params.brand);
  if (params.page != null) search.set("page", String(params.page));
  if (params.size != null) search.set("size", String(params.size));
  if (params.activeOnly != null) search.set("activeOnly", String(params.activeOnly));
  if (params.sortBy) search.set("sortBy", params.sortBy);
  if (params.direction) search.set("direction", params.direction);

  return apiClient<SearchProductPage>(`/api/search/products?${search.toString()}`);
}
