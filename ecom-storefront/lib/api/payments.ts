import { apiClient } from "../apiClient";
import type { CreatePaymentIntentRequest, PaymentResponse } from "../types/payment";

export async function createPaymentIntent(
  request: CreatePaymentIntentRequest
): Promise<PaymentResponse> {
  return apiClient<PaymentResponse>("/api/payments/intents", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
