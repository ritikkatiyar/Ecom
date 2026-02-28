"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listProductReviewsForModeration, moderateReview } from "@/lib/api/reviews";

export default function AdminReviewsPage() {
  const queryClient = useQueryClient();
  const [productId, setProductId] = useState("");
  const trimmed = productId.trim();

  const reviewsQuery = useQuery({
    queryKey: ["admin-reviews", trimmed],
    queryFn: () => listProductReviewsForModeration(trimmed),
    enabled: trimmed.length > 0,
  });

  const moderateMutation = useMutation({
    mutationFn: (args: { reviewId: number; status: "APPROVED" | "REJECTED" }) =>
      moderateReview(args.reviewId, args.status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-reviews", trimmed] });
    },
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">Reviews Moderation</h1>
        <p className="text-slate-600">
          Enter a product id to load review stream including pending entries.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <label className="block text-xs uppercase tracking-widest text-slate-400 mb-2">
          Product ID
        </label>
        <input
          value={productId}
          onChange={(e) => setProductId(e.target.value)}
          placeholder="Enter product id"
          className="w-full max-w-md rounded-lg border border-slate-200 px-3 py-2"
        />
      </section>

      <section className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="p-6 border-b border-slate-100">
          <h2 className="font-display text-2xl text-slate-900">Results</h2>
        </div>
        {!trimmed ? (
          <div className="p-6 text-slate-500">Provide a product id to view reviews.</div>
        ) : reviewsQuery.isLoading ? (
          <div className="p-6 text-slate-500">Loading reviews...</div>
        ) : reviewsQuery.error ? (
          <div className="p-6 text-red-600">
            {reviewsQuery.error instanceof Error ? reviewsQuery.error.message : "Failed to load reviews"}
          </div>
        ) : !reviewsQuery.data?.length ? (
          <div className="p-6 text-slate-500">No reviews found for this product.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {reviewsQuery.data.map((review) => (
              <div key={review.id} className="p-6 space-y-3">
                <div className="flex items-center justify-between">
                  <p className="font-medium text-slate-900">
                    User {review.userId} · Rating {review.rating}/5
                  </p>
                  <span className="text-xs uppercase tracking-widest text-slate-500">
                    {review.status}
                  </span>
                </div>
                <p className="text-slate-800">{review.title || "Untitled"}</p>
                <p className="text-sm text-slate-600">{review.comment || "-"}</p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={moderateMutation.isPending}
                    onClick={() => moderateMutation.mutate({ reviewId: review.id, status: "APPROVED" })}
                    className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold uppercase tracking-widest text-white disabled:opacity-60"
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    disabled={moderateMutation.isPending}
                    onClick={() => moderateMutation.mutate({ reviewId: review.id, status: "REJECTED" })}
                    className="rounded-lg bg-red-600 px-3 py-2 text-xs font-semibold uppercase tracking-widest text-white disabled:opacity-60"
                  >
                    Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
