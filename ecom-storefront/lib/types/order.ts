export interface OrderItemRequest {
  productId: string;
  sku: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  userId: number;
  currency: string;
  items: OrderItemRequest[];
}

export interface OrderResponse {
  id: string;
  userId: number;
  status: string;
  totalAmount: number;
  currency: string;
  items: OrderItemRequest[];
  createdAt: string;
  updatedAt: string;
}
