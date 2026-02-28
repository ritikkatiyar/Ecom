"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { listUserOrders } from "@/lib/api/orders";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountOrdersPage() {
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);
  const query = useQuery({
    queryKey: ["account-orders-page", numericUserId],
    queryFn: () => listUserOrders(numericUserId!),
    enabled: numericUserId != null,
  });

  return (
    <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
      <div className="p-6 border-b border-slate-100">
        <h1 className="font-display text-4xl font-bold text-slate-900">Order History</h1>
      </div>
      {query.isLoading ? (
        <div className="p-6 text-slate-500">Loading orders...</div>
      ) : query.error ? (
        <div className="p-6 text-red-600">
          {query.error instanceof Error ? query.error.message : "Failed to load orders"}
        </div>
      ) : !query.data?.length ? (
        <div className="p-6 text-slate-500">No orders found.</div>
      ) : (
        <div className="divide-y divide-slate-100">
          {query.data.map((order) => (
            <div key={order.id} className="p-6 flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Order #{order.id.slice(0, 8)}</p>
                <p className="text-sm text-slate-500">{new Date(order.createdAt).toLocaleString()}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-slate-900">{order.status}</p>
                <Link href={`/account/orders/${order.id}`} className="text-sm text-[#2badee] hover:underline">
                  View Details
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
