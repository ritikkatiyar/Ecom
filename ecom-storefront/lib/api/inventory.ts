import { apiClient } from "../apiClient";
import type { StockResponse, StockUpsertRequest } from "../types/inventory";

export async function getStock(sku: string): Promise<StockResponse> {
  return apiClient<StockResponse>(`/api/inventory/stock/${encodeURIComponent(sku)}`);
}

export async function upsertStock(request: StockUpsertRequest): Promise<StockResponse> {
  return apiClient<StockResponse>("/api/inventory/stock", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
