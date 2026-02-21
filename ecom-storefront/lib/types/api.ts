/**
 * Standard backend error payload.
 * @see API Gateway JSON error model
 */
export interface ApiErrorPayload {
  timestamp: string;
  path: string;
  errorCode?: string;
  message: string;
  correlationId?: string;
}

export interface ApiRequestConfig extends RequestInit {
  /** Skip attaching Authorization header */
  skipAuth?: boolean;
  /** Skip retry on failure */
  skipRetry?: boolean;
}
