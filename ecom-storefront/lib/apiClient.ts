/**
 * Shared API client aligned with backend contracts.
 * - X-API-Version: v1
 * - X-Correlation-Id (UUID per request)
 * - Authorization: Bearer when logged in
 * - Standard error parsing
 * - Retry GET requests only
 * - Log correlationId in dev
 */

import type { ApiErrorPayload, ApiRequestConfig } from "./types/api";
import { generateCorrelationId } from "./utils/uuid";

const BASE_URL =
  typeof window !== "undefined"
    ? "" // Use relative URLs; Next.js rewrites /api to backend
    : process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

const DEFAULT_HEADERS: Record<string, string> = {
  "Content-Type": "application/json",
  "X-API-Version": "v1",
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly payload: ApiErrorPayload | null,
    public readonly status: number,
    public readonly correlationId?: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export type AccessTokenProvider = () => string | null;
export type On401Handler = () => Promise<string | null>;

let accessTokenProvider: AccessTokenProvider | null = null;
let on401Handler: On401Handler | null = null;

/**
 * Set the access token provider (from AuthContext).
 */
export function setAccessTokenProvider(provider: AccessTokenProvider | null): void {
  accessTokenProvider = provider;
}

/**
 * Get current access token for custom requests (e.g. multipart upload).
 */
export function getAccessToken(): string | null {
  return accessTokenProvider?.() ?? null;
}

/**
 * Set the 401 handler. When a request returns 401, this is called to attempt refresh.
 * If it returns a new token, the request is retried once.
 */
export function setOn401Handler(handler: On401Handler | null): void {
  on401Handler = handler;
}

function getHeaders(skipAuth = false): Record<string, string> {
  const correlationId = generateCorrelationId();
  const headers = { ...DEFAULT_HEADERS, "X-Correlation-Id": correlationId };

  if (!skipAuth && accessTokenProvider) {
    const token = accessTokenProvider();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  return headers;
}

async function parseErrorResponse(res: Response): Promise<{ message: string; payload: ApiErrorPayload | null }> {
  const text = await res.text();
  try {
    const body = JSON.parse(text);
    if (body && typeof body === "object") {
      const message =
        (typeof body.message === "string" && body.message) ||
        (typeof body.error === "string" && body.error) ||
        (Array.isArray(body.errors) && body.errors[0]?.defaultMessage) ||
        text ||
        res.statusText;
      return {
        message,
        payload: typeof body.message === "string" ? {
          timestamp: body.timestamp ?? "",
          path: body.path ?? "",
          errorCode: body.errorCode,
          message: body.message,
          correlationId: body.correlationId,
        } : null,
      };
    }
  } catch {
    // Plain text (e.g. auth "Email already registered")
  }
  return { message: text || res.statusText, payload: null };
}

const GET_RETRY_COUNT = 2;
const GET_RETRY_DELAY_MS = 500;

async function fetchWithRetry(
  url: string,
  init: RequestInit,
  isGet: boolean,
  skipRetry: boolean
): Promise<Response> {
  let lastRes: Response | null = null;
  const maxAttempts = isGet && !skipRetry ? GET_RETRY_COUNT + 1 : 1;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const res = await fetch(url, init);
    lastRes = res;

    if (res.ok) return res;
    if (!isGet || skipRetry || attempt === maxAttempts) return res;

    await new Promise((r) => setTimeout(r, GET_RETRY_DELAY_MS));
  }

  return lastRes!;
}

export async function apiClient<T>(
  path: string,
  config: ApiRequestConfig = {}
): Promise<T> {
  const { skipAuth = false, skipRetry = false, ...init } = config;
  const url = path.startsWith("http") ? path : `${BASE_URL}${path}`;
  const isGet = (init.method ?? "GET").toUpperCase() === "GET";
  const headers = new Headers(init.headers);
  const builtHeaders = getHeaders(skipAuth);
  for (const [k, v] of Object.entries(builtHeaders)) {
    headers.set(k, v);
  }

  const res = await fetchWithRetry(
    url,
    { ...init, headers },
    isGet,
    skipRetry
  );

  const correlationId = headers.get("X-Correlation-Id") ?? undefined;
  if (process.env.NODE_ENV === "development" && correlationId) {
    console.debug(`[apiClient] ${init.method ?? "GET"} ${path} correlationId=${correlationId} status=${res.status}`);
  }

  if (res.status === 401 && on401Handler) {
    const newToken = await on401Handler();
    if (newToken) {
      headers.set("Authorization", `Bearer ${newToken}`);
      const retryRes = await fetch(url, { ...init, headers });
      const retryCorrelationId = headers.get("X-Correlation-Id");
      if (process.env.NODE_ENV === "development" && retryCorrelationId) {
        console.debug(`[apiClient] retry after refresh ${path} status=${retryRes.status}`);
      }
      if (!retryRes.ok) {
        const { message, payload } = await parseErrorResponse(retryRes);
        throw new ApiError(message, payload, retryRes.status, retryCorrelationId ?? undefined);
      }
      const ct = retryRes.headers.get("Content-Type") ?? "";
      if (ct.includes("application/json")) {
        return retryRes.json() as Promise<T>;
      }
      return retryRes.text() as unknown as T;
    }
  }

  if (!res.ok) {
    const { message, payload } = await parseErrorResponse(res);
    throw new ApiError(message, payload, res.status, correlationId);
  }

  const contentType = res.headers.get("Content-Type") ?? "";
  if (contentType.includes("application/json")) {
    return res.json() as Promise<T>;
  }
  return res.text() as unknown as T;
}
