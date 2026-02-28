/**
 * Proxy auth API to backend and enforce refresh-token cookie handling.
 * - Refresh token is stored in HttpOnly cookie on login/signup/refresh.
 * - Refresh/logout read refresh token from cookie and forward to backend body.
 */
import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
const REFRESH_COOKIE = "ecom_refresh_token";

function toJsonSafe(text: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === "object" ? (parsed as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

function attachRefreshCookie(
  res: NextResponse,
  refreshToken: string,
  expiresInSeconds?: number
) {
  const maxAge = Number.isFinite(expiresInSeconds) && (expiresInSeconds ?? 0) > 0
    ? Math.max(60, Number(expiresInSeconds))
    : 60 * 60 * 24 * 30;
  res.cookies.set({
    name: REFRESH_COOKIE,
    value: refreshToken,
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge,
  });
}

function clearRefreshCookie(res: NextResponse) {
  res.cookies.set({
    name: REFRESH_COOKIE,
    value: "",
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    expires: new Date(0),
  });
}

// Handle CORS preflight (OPTIONS) - custom headers trigger preflight
export async function OPTIONS() {
  return new NextResponse(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-API-Version, X-Correlation-Id",
    },
  });
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  const pathStr = path.join("/");
  const url = `${BACKEND}/api/auth/${pathStr}`;
  const headers = new Headers(request.headers);
  headers.delete("host");

  let requestBody: string | undefined;
  try {
    const rawBody = await request.text();
    const bodyJson = rawBody ? toJsonSafe(rawBody) : {};

    // For refresh/logout, inject refresh token from HttpOnly cookie.
    if (pathStr === "refresh" || pathStr === "logout") {
      const refreshFromCookie = request.cookies.get(REFRESH_COOKIE)?.value;
      if (refreshFromCookie) {
        const merged = { ...(bodyJson ?? {}), refreshToken: refreshFromCookie };
        requestBody = JSON.stringify(merged);
      } else {
        requestBody = rawBody || JSON.stringify({});
      }
    } else {
      requestBody = rawBody || undefined;
    }
  } catch {
    requestBody = undefined;
  }

  try {
    const upstream = await fetch(url, {
      method: "POST",
      headers,
      body: requestBody,
    });
    const text = await upstream.text();
    const parsed = toJsonSafe(text);
    const responsePayload: Record<string, unknown> = parsed ? { ...parsed } : {};

    let refreshToken: string | undefined;
    if (parsed && typeof parsed.refreshToken === "string") {
      refreshToken = parsed.refreshToken;
      delete responsePayload.refreshToken;
    }

    const response = new NextResponse(parsed ? JSON.stringify(responsePayload) : text, {
      status: upstream.status,
      statusText: upstream.statusText,
      headers: {
        "Content-Type": upstream.headers.get("Content-Type") ?? "application/json",
      },
    });

    if (upstream.ok && refreshToken && (pathStr === "login" || pathStr === "signup" || pathStr === "refresh")) {
      const exp = parsed && typeof parsed.expiresInSeconds === "number"
        ? parsed.expiresInSeconds
        : undefined;
      attachRefreshCookie(response, refreshToken, exp);
    }

    if (pathStr === "logout") {
      clearRefreshCookie(response);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: "Backend unavailable" },
      { status: 502 }
    );
  }
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  const pathStr = path.join("/");
  const url = `${BACKEND}/api/auth/${pathStr}${request.nextUrl.search}`;
  const headers = new Headers(request.headers);
  headers.delete("host");
  try {
    const res = await fetch(url, { method: "GET", headers });
    const text = await res.text();
    return new NextResponse(text, {
      status: res.status,
      statusText: res.statusText,
      headers: {
        "Content-Type": res.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch {
    return NextResponse.json(
      { message: "Backend unavailable" },
      { status: 502 }
    );
  }
}
