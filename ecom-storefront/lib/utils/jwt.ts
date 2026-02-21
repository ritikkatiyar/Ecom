/**
 * Decode JWT payload without verification (backend validates).
 * Extract userId and role for client-side guards.
 */

export interface JwtPayload {
  sub?: string;
  role?: string;
  exp?: number;
  iat?: number;
  jti?: string;
}

/**
 * Parse and decode JWT payload. Does NOT verify signature.
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = parts[1];
    const decoded = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(decoded) as JwtPayload;
  } catch {
    return null;
  }
}

/**
 * Extract roles from JWT. Backend uses "role" claim (single string).
 */
export function getRolesFromToken(token: string): string[] {
  const payload = decodeJwtPayload(token);
  if (!payload?.role) return [];
  const role = String(payload.role).toUpperCase();
  return role ? [role] : [];
}

/**
 * Check if token is expired (with 60s buffer).
 */
export function isTokenExpired(token: string): boolean {
  const payload = decodeJwtPayload(token);
  if (!payload?.exp) return true;
  return payload.exp * 1000 < Date.now() + 60_000;
}
