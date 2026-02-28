"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { getOrder } from "@/lib/api/orders";

export default function AccountOrderDetailPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const [orderId, setOrderId] = useState<string>("");
  useEffect(() => {
    params.then((p) => setOrderId(p.orderId));
  }, [params]);

  const query = useQuery({
    queryKey: ["account-order-detail", orderId],
    queryFn: () => getOrder(orderId),
    enabled: orderId.length > 0,
  });

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8">
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-4">Order Details</h1>
      {query.isLoading ? (
        <p className="text-slate-500">Loading order...</p>
      ) : query.error ? (
        <p className="text-red-600">
          {query.error instanceof Error ? query.error.message : "Failed to load order"}
        </p>
      ) : (
        <div className="space-y-4 text-sm">
          <p><span className="text-slate-500">Order ID:</span> {query.data?.id}</p>
          <p><span className="text-slate-500">Status:</span> {query.data?.status}</p>
          <p><span className="text-slate-500">Amount:</span> {query.data?.currency} {query.data?.totalAmount}</p>
          <div>
            <p className="text-slate-500 mb-2">Items</p>
            <ul className="space-y-2">
              {query.data?.items.map((item, idx) => (
                <li key={`${item.productId}-${idx}`} className="rounded-lg border border-slate-200 p-3">
                  {item.productId} · Qty {item.quantity} · Unit Price {item.unitPrice}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}
