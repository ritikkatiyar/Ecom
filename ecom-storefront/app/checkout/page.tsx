"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQueries, useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/lib/hooks/useCart";
import { getProduct } from "@/lib/api/products";
import { createOrder, getOrder } from "@/lib/api/orders";
import { createPaymentIntent } from "@/lib/api/payments";
import { generateCorrelationId } from "@/lib/utils/uuid";

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(price);
}

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function CheckoutPage() {
  const { isAuthenticated, userId } = useAuth();
  const numericUserId = parseUserId(userId);
  const { cart, isLoading: cartLoading } = useCart();
  const [orderId, setOrderId] = useState<string | null>(null);
  const [paymentId, setPaymentId] = useState<string | null>(null);

  const items = cart?.items ?? [];
  const productQueries = useQueries({
    queries: items.map((item) => ({
      queryKey: ["product", item.productId],
      queryFn: () => getProduct(item.productId),
      staleTime: 60 * 1000,
    })),
  });

  const productMap = useMemo(() => {
    const map = new Map<string, Awaited<ReturnType<typeof getProduct>>>();
    items.forEach((item, idx) => {
      const p = productQueries[idx]?.data;
      if (p) map.set(item.productId, p);
    });
    return map;
  }, [items, productQueries]);

  const totalAmount = useMemo(
    () =>
      items.reduce((sum, item) => {
        const p = productMap.get(item.productId);
        return sum + (p?.price ?? 0) * item.quantity;
      }, 0),
    [items, productMap]
  );

  const orderMutation = useMutation({
    mutationFn: async () => {
      if (!numericUserId) throw new Error("User id is missing or invalid");
      const order = await createOrder({
        userId: numericUserId,
        currency: "INR",
        items: items.map((item) => {
          const product = productMap.get(item.productId);
          return {
            productId: item.productId,
            sku: item.productId,
            quantity: item.quantity,
            unitPrice: product?.price ?? 0,
          };
        }),
      });

      const payment = await createPaymentIntent({
        orderId: order.id,
        userId: numericUserId,
        amount: order.totalAmount,
        currency: order.currency,
        idempotencyKey: `checkout-${generateCorrelationId()}`,
      });

      setOrderId(order.id);
      setPaymentId(payment.paymentId);
      return { order, payment };
    },
  });

  const orderStatusQuery = useQuery({
    queryKey: ["order-status", orderId],
    queryFn: () => getOrder(orderId!),
    enabled: !!orderId,
    refetchInterval: (q) => {
      const status = q.state.data?.status;
      if (!status) return 3000;
      if (status === "CONFIRMED" || status === "FAILED" || status === "TIMED_OUT" || status === "CANCELLED") {
        return false;
      }
      return 3000;
    },
  });

  const liveStatus = orderStatusQuery.data?.status ?? orderMutation.data?.order.status ?? null;

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-[#F8F6F3]">
        <main className="max-w-3xl mx-auto px-6 py-16">
          <h1 className="font-display text-4xl font-bold text-slate-900 mb-4">Checkout</h1>
          <p className="text-slate-600 mb-8">Please sign in to place your order.</p>
          <Link
            href="/login?returnTo=%2Fcheckout"
            className="inline-flex rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold uppercase tracking-widest text-white"
          >
            Sign In
          </Link>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-5xl mx-auto px-6 py-12">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">Checkout</h1>

        {cartLoading ? (
          <p className="text-slate-600">Loading cart...</p>
        ) : !items.length ? (
          <div className="rounded-xl border border-slate-200 bg-white p-10 text-center">
            <p className="text-slate-600 mb-6">Your cart is empty.</p>
            <Link
              href="/shop"
              className="inline-flex rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold uppercase tracking-widest text-white"
            >
              Go to Shop
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <section className="lg:col-span-7 rounded-xl border border-slate-200 bg-white p-6">
              <h2 className="font-display text-2xl text-slate-900 mb-4">Order Items</h2>
              <div className="space-y-4">
                {items.map((item) => {
                  const product = productMap.get(item.productId);
                  return (
                    <div key={item.productId} className="flex items-center justify-between text-sm">
                      <div>
                        <p className="font-medium text-slate-900">{product?.name ?? item.productId}</p>
                        <p className="text-slate-500">Qty {item.quantity}</p>
                      </div>
                      <p className="text-slate-700">
                        {formatPrice((product?.price ?? 0) * item.quantity)}
                      </p>
                    </div>
                  );
                })}
              </div>
            </section>

            <aside className="lg:col-span-5 rounded-xl border border-slate-200 bg-white p-6 h-fit">
              <h2 className="font-display text-2xl text-slate-900 mb-4">Payment</h2>
              <div className="flex items-center justify-between text-sm text-slate-600 mb-2">
                <span>Total</span>
                <span className="text-slate-900 font-semibold">{formatPrice(totalAmount)}</span>
              </div>
              <button
                type="button"
                onClick={() => orderMutation.mutate()}
                disabled={orderMutation.isPending || !numericUserId}
                className="mt-6 w-full rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold uppercase tracking-widest text-white disabled:opacity-60"
              >
                {orderMutation.isPending ? "Processing..." : "Place Order"}
              </button>
              {orderMutation.error ? (
                <p className="mt-3 text-sm text-red-600">
                  {orderMutation.error instanceof Error ? orderMutation.error.message : "Checkout failed"}
                </p>
              ) : null}
            </aside>
          </div>
        )}

        {orderId ? (
          <section className="mt-10 rounded-xl border border-slate-200 bg-white p-6">
            <h3 className="font-display text-2xl text-slate-900 mb-4">Saga Order Status</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              <p className="text-slate-600">Order ID: <span className="text-slate-900">{orderId}</span></p>
              <p className="text-slate-600">Payment ID: <span className="text-slate-900">{paymentId ?? "-"}</span></p>
              <p className="text-slate-600">Current Status: <span className="text-slate-900 font-semibold">{liveStatus ?? "PAYMENT_PENDING"}</span></p>
              <p className="text-slate-500">Polling every 3s until terminal state.</p>
            </div>
            <div className="mt-4 flex gap-2 text-xs uppercase tracking-widest">
              {["PAYMENT_PENDING", "CONFIRMED", "FAILED", "TIMED_OUT"].map((s) => (
                <span
                  key={s}
                  className={`rounded-full px-3 py-1 border ${
                    liveStatus === s
                      ? "border-[#2badee] text-[#2badee] bg-[#2badee]/10"
                      : "border-slate-200 text-slate-400"
                  }`}
                >
                  {s}
                </span>
              ))}
            </div>
          </section>
        ) : null}
      </main>
    </div>
  );
}
