/**
 * Product API - list, get, create, update, image upload.
 */
import { ApiError, apiClient, fetchWithAuthRetry, getAccessToken } from "../apiClient";
import { generateCorrelationId } from "../utils/uuid";
import type { Product, ProductPage, ProductRequest } from "../types/product";

const MAX_IMAGE_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const MAX_IMAGE_REQUEST_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif",
]);

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
  params: GetProductsParams = {},
  config: { revalidateSeconds?: number } = {}
): Promise<ProductPage> {
  const search = new URLSearchParams();
  if (params.page != null) search.set("page", String(params.page));
  if (params.size != null) search.set("size", String(params.size));
  if (params.category) search.set("category", params.category);
  if (params.brand) search.set("brand", params.brand);
  if (params.q) search.set("q", params.q);
  if (params.sortBy) search.set("sortBy", params.sortBy);
  if (params.direction) search.set("direction", params.direction);
  return apiClient<ProductPage>(`/api/products?${search.toString()}`, {
    next:
      config.revalidateSeconds != null
        ? { revalidate: config.revalidateSeconds, tags: ["products"] }
        : undefined,
  });
}

export async function getProduct(
  id: string,
  config: { revalidateSeconds?: number } = {}
): Promise<Product> {
  return apiClient<Product>(`/api/products/${id}`, {
    next:
      config.revalidateSeconds != null
        ? { revalidate: config.revalidateSeconds, tags: [`product-${id}`, "products"] }
        : undefined,
  });
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
  if (!files.length) {
    throw new Error("No files selected for upload");
  }
  const invalidType = files.find((f) => !ALLOWED_IMAGE_TYPES.has(f.type));
  if (invalidType) {
    throw new Error(`Unsupported file type for "${invalidType.name}".`);
  }
  const oversize = files.find((f) => f.size > MAX_IMAGE_FILE_SIZE_BYTES);
  if (oversize) {
    throw new Error(`"${oversize.name}" exceeds 10MB max file size.`);
  }
  const totalBytes = files.reduce((sum, file) => sum + file.size, 0);
  if (totalBytes > MAX_IMAGE_REQUEST_SIZE_BYTES) {
    throw new Error("Total selected files exceed 10MB request limit.");
  }
  const formData = new FormData();
  files.forEach((f) => formData.append("files", f));
  const BASE_URL =
    typeof window !== "undefined"
      ? ""
      : process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
  const url = `${BASE_URL}/api/products/images`;
  const headers: Record<string, string> = {
    "X-API-Version": "v1",
    "X-Correlation-Id": generateCorrelationId(),
  };
  const correlationId = headers["X-Correlation-Id"];
  const token = getAccessToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 45000);
  let res: Response;
  try {
    res = await fetchWithAuthRetry(url, {
      method: "POST",
      headers,
      body: formData,
      signal: controller.signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new ApiError(
        "Image upload timed out. Please retry with fewer or smaller files.",
        null,
        0,
        correlationId
      );
    }
    throw err;
  } finally {
    clearTimeout(timeout);
  }

  if (!res.ok) {
    const text = await res.text();
    let msg = text || "Upload failed";
    let responseCorrelationId: string | undefined =
      res.headers.get("X-Correlation-Id") ??
      res.headers.get("x-correlation-id") ??
      undefined;
    try {
      const j = JSON.parse(text);
      msg = j.message ?? j.error ?? text;
      responseCorrelationId = responseCorrelationId ?? j.correlationId ?? undefined;
    } catch {
      // ignore
    }
    if (res.status === 413) {
      msg = "Upload too large. Max 10MB per file and 10MB per request.";
    }
    throw new ApiError(msg, null, res.status, responseCorrelationId ?? correlationId);
  }

  const payload = await res.json();
  if (Array.isArray(payload) && payload.every((u) => typeof u === "string")) {
    return payload;
  }
  throw new ApiError("Upload succeeded but returned an unexpected response.", null, 502, correlationId);
}
