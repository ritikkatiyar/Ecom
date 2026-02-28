import { apiClient } from "../apiClient";
import type { CartResponse } from "../types/cart";

interface OwnerParams {
  userId?: number;
  guestId?: string;
}

interface AddItemRequest extends OwnerParams {
  productId: string;
  quantity: number;
}

function ownerSearch(params: OwnerParams): string {
  const search = new URLSearchParams();
  if (params.userId != null) search.set("userId", String(params.userId));
  if (params.guestId) search.set("guestId", params.guestId);
  return search.toString();
}

export async function getCart(params: OwnerParams): Promise<CartResponse> {
  const qs = ownerSearch(params);
  return apiClient<CartResponse>(`/api/cart${qs ? `?${qs}` : ""}`);
}

export async function addCartItem(request: AddItemRequest): Promise<CartResponse> {
  return apiClient<CartResponse>("/api/cart/items", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function removeCartItem(
  productId: string,
  params: OwnerParams
): Promise<CartResponse> {
  const qs = ownerSearch(params);
  return apiClient<CartResponse>(`/api/cart/items/${productId}${qs ? `?${qs}` : ""}`, {
    method: "DELETE",
  });
}

export async function clearCart(params: OwnerParams): Promise<void> {
  const qs = ownerSearch(params);
  await apiClient<void>(`/api/cart${qs ? `?${qs}` : ""}`, {
    method: "DELETE",
  });
}
