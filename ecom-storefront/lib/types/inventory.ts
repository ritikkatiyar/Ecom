export interface StockResponse {
  sku: string;
  availableQuantity: number;
  reservedQuantity: number;
}

export interface StockUpsertRequest {
  sku: string;
  availableQuantity: number;
}
