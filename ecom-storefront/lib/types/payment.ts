export interface CreatePaymentIntentRequest {
  orderId: string;
  userId: number;
  amount: number;
  currency: string;
  idempotencyKey: string;
}

export interface PaymentResponse {
  paymentId: string;
  orderId: string;
  userId: number;
  amount: number;
  currency: string;
  status: string;
  providerPaymentId?: string;
  idempotencyKey?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}
