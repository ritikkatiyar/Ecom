/**
 * Proxy product image upload (POST multipart) to backend.
 */
import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const url = `${BACKEND}/api/products/images`;
  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.set("X-API-Version", "v1");
  headers.delete("content-length");
  headers.delete("connection");
  headers.delete("transfer-encoding");
  headers.delete("accept-encoding");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 45000);
  try {
    const arrayBuffer = await request.arrayBuffer();
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: arrayBuffer,
      signal: controller.signal,
    });
    const text = await res.text();
    const correlationId =
      res.headers.get("X-Correlation-Id") ??
      res.headers.get("x-correlation-id") ??
      request.headers.get("X-Correlation-Id") ??
      request.headers.get("x-correlation-id");
    return new NextResponse(text, {
      status: res.status,
      statusText: res.statusText,
      headers: {
        "Content-Type": res.headers.get("Content-Type") ?? "application/json",
        ...(correlationId ? { "X-Correlation-Id": correlationId } : {}),
      },
    });
  } catch (err) {
    if (err instanceof Error && err.name === "AbortError") {
      return NextResponse.json(
        { message: "Upload request timed out." },
        { status: 504 }
      );
    }
    const msg = err instanceof Error ? err.message : String(err);
    if (process.env.NODE_ENV === "development") {
      console.error("[api/products/images] proxy error:", err);
      return NextResponse.json(
        { message: "Backend unavailable", detail: msg },
        { status: 502 }
      );
    }
    return NextResponse.json(
      { message: "Backend unavailable" },
      { status: 502 }
    );
  } finally {
    clearTimeout(timeout);
  }
}
