"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listUserOrders } from "@/lib/api/orders";

export default function AdminOrdersPage() {
  const [userId, setUserId] = useState("1");
  const numericUserId = Number(userId);
  const validUserId = Number.isFinite(numericUserId) && numericUserId > 0;

  const ordersQuery = useQuery({
    queryKey: ["admin-orders-by-user", numericUserId],
    queryFn: () => listUserOrders(numericUserId),
    enabled: validUserId,
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">Orders</h1>
        <p className="text-slate-600">
          Read-only admin order lookup by user id (current backend contract).
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <label className="block text-xs uppercase tracking-widest text-slate-400 mb-2">
          User ID
        </label>
        <input
          type="number"
          min={1}
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          className="w-56 rounded-lg border border-slate-200 px-3 py-2 text-slate-900"
        />
      </section>

      <section className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="p-6 border-b border-slate-100">
          <h2 className="font-display text-2xl text-slate-900">Order Results</h2>
        </div>
        {!validUserId ? (
          <div className="p-6 text-amber-700">Enter a valid user id.</div>
        ) : ordersQuery.isLoading ? (
          <div className="p-6 text-slate-500">Loading orders...</div>
        ) : ordersQuery.error ? (
          <div className="p-6 text-red-600">
            {ordersQuery.error instanceof Error ? ordersQuery.error.message : "Failed to load orders"}
          </div>
        ) : !ordersQuery.data?.length ? (
          <div className="p-6 text-slate-500">No orders found for this user.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {ordersQuery.data.map((order) => (
              <div key={order.id} className="p-6 flex items-center justify-between">
                <div>
                  <p className="font-medium text-slate-900">Order #{order.id.slice(0, 8)}</p>
                  <p className="text-sm text-slate-500">{new Date(order.createdAt).toLocaleString()}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-slate-900">{order.status}</p>
                  <p className="text-sm text-slate-600">{order.currency} {order.totalAmount}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
