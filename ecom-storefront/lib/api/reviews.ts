import { apiClient } from "../apiClient";
import type { ReviewResponse } from "../types/review";

export async function listProductReviewsForModeration(
  productId: string
): Promise<ReviewResponse[]> {
  return apiClient<ReviewResponse[]>(
    `/api/reviews?productId=${encodeURIComponent(productId)}&includePending=true`
  );
}

export async function moderateReview(
  reviewId: number,
  status: "APPROVED" | "REJECTED" | "PENDING"
): Promise<ReviewResponse> {
  return apiClient<ReviewResponse>(`/api/reviews/${reviewId}/moderate`, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}
