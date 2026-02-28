import { apiClient } from "../apiClient";
import type {
  ProviderDeadLetterResponse,
  ProviderOutageModeResponse,
  SearchDatasetHealthResponse,
} from "../types/admin";

export async function getSearchDatasetHealth(): Promise<SearchDatasetHealthResponse> {
  return apiClient<SearchDatasetHealthResponse>(
    "/api/search/admin/relevance/dataset/health"
  );
}

export async function getProviderOutageMode(): Promise<ProviderOutageModeResponse> {
  return apiClient<ProviderOutageModeResponse>("/api/payments/provider/outage-mode");
}

export async function listProviderDeadLetters(): Promise<ProviderDeadLetterResponse[]> {
  return apiClient<ProviderDeadLetterResponse[]>("/api/payments/provider/dead-letters");
}

export async function setProviderOutageMode(enabled: boolean): Promise<ProviderOutageModeResponse> {
  return apiClient<ProviderOutageModeResponse>(`/api/payments/provider/outage-mode?enabled=${enabled}`, {
    method: "POST",
  });
}

export async function requeueProviderDeadLetter(deadLetterId: number): Promise<void> {
  await apiClient(`/api/payments/provider/dead-letters/${deadLetterId}/requeue`, {
    method: "POST",
  });
}
