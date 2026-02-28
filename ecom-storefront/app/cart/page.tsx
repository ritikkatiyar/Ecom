"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { useCart } from "@/lib/hooks/useCart";
import { getProduct } from "@/lib/api/products";

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(price);
}

export default function CartPage() {
  const { cart, isLoading, error, addItem, removeItem, clear, isMutating } = useCart();
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
      const product = productQueries[idx]?.data;
      if (product) map.set(item.productId, product);
    });
    return map;
  }, [items, productQueries]);

  const subtotal = useMemo(
    () =>
      items.reduce((sum, item) => {
        const p = productMap.get(item.productId);
        return sum + (p?.price ?? 0) * item.quantity;
      }, 0),
    [items, productMap]
  );

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-12">
        <div className="mb-8 flex items-center justify-between">
          <h1 className="font-display text-4xl font-bold text-slate-900">
            Your Cart
          </h1>
          <button
            type="button"
            onClick={() => clear()}
            disabled={!items.length || isMutating}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-50"
          >
            Clear Cart
          </button>
        </div>

        {isLoading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
            Loading cart...
          </div>
        ) : error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-700">
            {error instanceof Error ? error.message : "Failed to load cart"}
          </div>
        ) : !items.length ? (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center">
            <p className="text-slate-600 mb-6">Your cart is empty.</p>
            <Link
              href="/shop"
              className="inline-flex rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold uppercase tracking-widest text-white"
            >
              Continue Shopping
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <section className="lg:col-span-8 space-y-4">
              {items.map((item) => {
                const product = productMap.get(item.productId);
                return (
                  <div
                    key={item.productId}
                    className="rounded-xl border border-slate-200 bg-white p-5 flex items-center justify-between gap-4"
                  >
                    <div>
                      <p className="font-display text-xl text-slate-900">
                        {product?.name ?? "Product"}
                      </p>
                      <p className="text-xs uppercase tracking-widest text-slate-400 mt-1">
                        {item.productId}
                      </p>
                      <p className="text-sm text-slate-600 mt-2">
                        {product ? formatPrice(product.price) : "Price unavailable"} x {item.quantity}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => addItem({ productId: item.productId, quantity: 1 })}
                        disabled={isMutating}
                        className="h-9 w-9 rounded-full border border-slate-200 bg-white text-slate-700"
                      >
                        +
                      </button>
                      <button
                        type="button"
                        onClick={() => removeItem(item.productId)}
                        disabled={isMutating}
                        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold uppercase tracking-widest text-slate-700"
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                );
              })}
            </section>
            <aside className="lg:col-span-4">
              <div className="rounded-xl border border-slate-200 bg-white p-6 sticky top-24">
                <h2 className="font-display text-2xl text-slate-900 mb-4">Summary</h2>
                <div className="flex items-center justify-between text-sm text-slate-600 mb-2">
                  <span>Items</span>
                  <span>{cart?.totalItems ?? 0}</span>
                </div>
                <div className="flex items-center justify-between text-lg font-semibold text-slate-900 border-t border-slate-200 pt-4 mt-4">
                  <span>Subtotal</span>
                  <span>{formatPrice(subtotal)}</span>
                </div>
                <Link
                  href="/checkout"
                  className="mt-6 inline-flex w-full justify-center rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold uppercase tracking-widest text-white"
                >
                  Proceed to Checkout
                </Link>
              </div>
            </aside>
          </div>
        )}
      </main>
    </div>
  );
}
