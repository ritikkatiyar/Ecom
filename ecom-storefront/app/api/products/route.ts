/**
 * Proxy product list (GET) and create (POST) to backend.
 */
import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

async function proxy(request: NextRequest, method: string) {
  const url = `${BACKEND}/api/products${request.nextUrl.search}`;
  const headers = new Headers(request.headers);
  headers.delete("host");
  try {
    const body = method === "POST" ? await request.text() : undefined;
    const res = await fetch(url, {
      method,
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

export async function GET(request: NextRequest) {
  return proxy(request, "GET");
}

export async function POST(request: NextRequest) {
  return proxy(request, "POST");
}
