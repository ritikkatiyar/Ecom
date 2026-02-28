export interface ReviewResponse {
  id: number;
  userId: number;
  productId: string;
  rating: number;
  title?: string;
  comment?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
  updatedAt: string;
}
