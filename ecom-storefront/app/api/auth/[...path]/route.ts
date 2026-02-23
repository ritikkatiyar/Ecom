/**
 * Proxy auth API to backend. Next.js rewrites can return 405 for POST.
 */
import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

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
  try {
    const body = await request.text();
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: body || undefined,
    });
    const text = await res.text();
    return new NextResponse(text, {
      status: res.status,
      statusText: res.statusText,
      headers: {
        "Content-Type": res.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch (err) {
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
  } catch (err) {
    return NextResponse.json(
      { message: "Backend unavailable" },
      { status: 502 }
    );
  }
}
