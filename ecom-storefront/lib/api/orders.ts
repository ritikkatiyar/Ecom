import { apiClient } from "../apiClient";
import type { CreateOrderRequest, OrderResponse } from "../types/order";

export async function createOrder(request: CreateOrderRequest): Promise<OrderResponse> {
  return apiClient<OrderResponse>("/api/orders", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function getOrder(orderId: string): Promise<OrderResponse> {
  return apiClient<OrderResponse>(`/api/orders/${orderId}`);
}

export async function listUserOrders(userId: number): Promise<OrderResponse[]> {
  return apiClient<OrderResponse[]>(`/api/orders?userId=${userId}`);
}
