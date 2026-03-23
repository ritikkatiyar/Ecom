const DEV_BACKEND_FALLBACK = "http://localhost:8080";

function normalizeBaseUrl(value: string): string {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

export function getBackendBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_BACKEND_URL;
  if (configured && configured.trim().length > 0) {
    return normalizeBaseUrl(configured.trim());
  }

  if (process.env.NODE_ENV !== "production") {
    return DEV_BACKEND_FALLBACK;
  }

  throw new Error(
    "NEXT_PUBLIC_BACKEND_URL is required in production. Point it to the API gateway."
  );
}
