/**
 * Proxy product get (GET), update (PUT), delete (DELETE) to backend.
 */
import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

async function proxy(
  request: NextRequest,
  id: string,
  method: string
) {
  const url = `${BACKEND}/api/products/${id}`;
  const headers = new Headers(request.headers);
  headers.delete("host");
  try {
    const body = method === "PUT" ? await request.text() : undefined;
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

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  return proxy(request, id, "GET");
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  return proxy(request, id, "PUT");
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  return proxy(request, id, "DELETE");
}
