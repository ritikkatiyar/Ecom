/**
 * Product API - list, get, create, update, image upload.
 */
import { apiClient, getAccessToken } from "../apiClient";
import type { Product, ProductPage, ProductRequest } from "../types/product";

export interface GetProductsParams {
  page?: number;
  size?: number;
  category?: string;
  brand?: string;
  q?: string;
  sortBy?: string;
  direction?: string;
}

export async function getProducts(
  params: GetProductsParams = {}
): Promise<ProductPage> {
  const search = new URLSearchParams();
  if (params.page != null) search.set("page", String(params.page));
  if (params.size != null) search.set("size", String(params.size));
  if (params.category) search.set("category", params.category);
  if (params.brand) search.set("brand", params.brand);
  if (params.q) search.set("q", params.q);
  if (params.sortBy) search.set("sortBy", params.sortBy);
  if (params.direction) search.set("direction", params.direction);
  return apiClient<ProductPage>(`/api/products?${search.toString()}`);
}

export async function getProduct(id: string): Promise<Product> {
  return apiClient<Product>(`/api/products/${id}`);
}

export async function createProduct(data: ProductRequest): Promise<Product> {
  return apiClient<Product>("/api/products", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function updateProduct(
  id: string,
  data: ProductRequest
): Promise<Product> {
  return apiClient<Product>(`/api/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export async function uploadProductImages(files: File[]): Promise<string[]> {
  const formData = new FormData();
  files.forEach((f) => formData.append("files", f));
  const BASE_URL =
    typeof window !== "undefined"
      ? ""
      : process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
  const url = `${BASE_URL}/api/products/images`;
  const headers: Record<string, string> = {
    "X-API-Version": "v1",
  };
  const token = getAccessToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(url, {
    method: "POST",
    headers,
    body: formData,
  });
  if (!res.ok) {
    const text = await res.text();
    let msg = text;
    try {
      const j = JSON.parse(text);
      msg = j.message ?? j.error ?? text;
    } catch {
      // ignore
    }
    throw new Error(msg || "Upload failed");
  }
  return res.json();
}
