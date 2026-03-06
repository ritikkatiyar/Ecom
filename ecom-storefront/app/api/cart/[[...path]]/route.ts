import { NextRequest, NextResponse } from "next/server";

const BACKEND =
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

const METHODS_WITH_BODY = new Set(["POST", "PUT", "PATCH", "DELETE"]);

async function proxy(request: NextRequest, path: string[] = []) {
  const suffix = path.length ? `/${path.join("/")}` : "";
  const url = `${BACKEND}/api/cart${suffix}${request.nextUrl.search}`;
  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.delete("connection");
  headers.delete("transfer-encoding");
  headers.delete("accept-encoding");
  headers.delete("content-length");
  headers.set("X-API-Version", "v1");

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30000);
  try {
    const method = request.method.toUpperCase();
    const body = METHODS_WITH_BODY.has(method)
      ? await request.text()
      : undefined;
    const upstream = await fetch(url, {
      method,
      headers,
      body: body || undefined,
      signal: controller.signal,
    });
    const text = await upstream.text();
    const contentType =
      upstream.headers.get("Content-Type") ?? "application/json";
    const correlationId =
      upstream.headers.get("X-Correlation-Id") ??
      upstream.headers.get("x-correlation-id");
    return new NextResponse(text, {
      status: upstream.status,
      statusText: upstream.statusText,
      headers: {
        "Content-Type": contentType,
        ...(correlationId ? { "X-Correlation-Id": correlationId } : {}),
      },
    });
  } catch (err) {
    if (err instanceof Error && err.name === "AbortError") {
      return NextResponse.json(
        { message: "Cart request timed out." },
        { status: 504 }
      );
    }
    const msg = err instanceof Error ? err.message : String(err);
    if (process.env.NODE_ENV === "development") {
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

type RouteContext = {
  params: Promise<{ path?: string[] }>;
};

export async function GET(request: NextRequest, context: RouteContext) {
  const { path = [] } = await context.params;
  return proxy(request, path);
}

export async function POST(request: NextRequest, context: RouteContext) {
  const { path = [] } = await context.params;
  return proxy(request, path);
}

export async function PUT(request: NextRequest, context: RouteContext) {
  const { path = [] } = await context.params;
  return proxy(request, path);
}

export async function PATCH(request: NextRequest, context: RouteContext) {
  const { path = [] } = await context.params;
  return proxy(request, path);
}

export async function DELETE(request: NextRequest, context: RouteContext) {
  const { path = [] } = await context.params;
  return proxy(request, path);
}
