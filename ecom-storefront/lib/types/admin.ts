export interface SearchDatasetHealthResponse {
  datasetVersion: string;
  datasetSize: number;
  lastRefreshedAt?: string;
  refreshCadenceDays: number;
  daysSinceRefresh: number;
  refreshRequired: boolean;
  evaluatedAt: string;
}

export interface ProviderDeadLetterResponse {
  id: number;
  idempotencyKey: string;
  orderId: string;
  userId: number;
  amount: number;
  currency: string;
  attempts: number;
  status: string;
  failureReason?: string;
  requeuedPaymentId?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface ProviderOutageModeResponse {
  outageMode: boolean;
}
