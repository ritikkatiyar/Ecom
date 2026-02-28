export interface CartItem {
  productId: string;
  quantity: number;
}

export interface CartResponse {
  ownerType: "USER" | "GUEST";
  ownerId: string;
  totalItems: number;
  items: CartItem[];
}
