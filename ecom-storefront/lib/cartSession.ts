import { generateCorrelationId } from "./utils/uuid";

const GUEST_ID_KEY = "ecom_guest_id";

export function getOrCreateGuestId(): string {
  if (typeof window === "undefined") {
    return "server-guest";
  }
  const existing = window.localStorage.getItem(GUEST_ID_KEY);
  if (existing) return existing;
  const next = `guest-${generateCorrelationId()}`;
  window.localStorage.setItem(GUEST_ID_KEY, next);
  return next;
}
