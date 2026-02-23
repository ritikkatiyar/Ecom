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
  headers.delete("content-length");
  try {
    const arrayBuffer = await request.arrayBuffer();
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: arrayBuffer,
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
  }
}
