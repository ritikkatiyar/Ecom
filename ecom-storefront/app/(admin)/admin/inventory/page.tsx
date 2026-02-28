"use client";

import { FormEvent, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { getStock, upsertStock } from "@/lib/api/inventory";
import type { StockResponse } from "@/lib/types/inventory";
import { getProducts } from "@/lib/api/products";

export default function AdminInventoryPage() {
  const [skuLookup, setSkuLookup] = useState("");
  const [skuUpsert, setSkuUpsert] = useState("");
  const [availableQuantity, setAvailableQuantity] = useState("0");
  const [stock, setStock] = useState<StockResponse | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [selectedProductId, setSelectedProductId] = useState("");

  const productsQuery = useQuery({
    queryKey: ["admin-inventory-products"],
    queryFn: () => getProducts({ page: 0, size: 100, sortBy: "name", direction: "asc" }),
  });

  const lookupMutation = useMutation({
    mutationFn: (sku: string) => getStock(sku),
    onSuccess: (data) => {
      setLookupError(null);
      setStock(data);
    },
    onError: (err) => {
      setStock(null);
      setLookupError(err instanceof Error ? err.message : "Failed to fetch stock");
    },
  });

  const upsertMutation = useMutation({
    mutationFn: () =>
      upsertStock({
        sku: skuUpsert.trim(),
        availableQuantity: Number(availableQuantity),
      }),
    onSuccess: (data) => {
      setStock(data);
      setLookupError(null);
      setSkuLookup(data.sku);
    },
  });

  const onLookup = (e: FormEvent) => {
    e.preventDefault();
    const sku = skuLookup.trim();
    if (!sku) return;
    lookupMutation.mutate(sku);
  };

  const onUpsert = (e: FormEvent) => {
    e.preventDefault();
    if (!skuUpsert.trim()) return;
    upsertMutation.mutate();
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">Inventory</h1>
        <p className="text-slate-600">Lookup and update inventory by SKU.</p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-6 space-y-4">
        <h2 className="font-display text-2xl text-slate-900">Select Product</h2>
        {productsQuery.isLoading ? (
          <p className="text-sm text-slate-500">Loading products...</p>
        ) : productsQuery.error ? (
          <p className="text-sm text-red-600">
            {productsQuery.error instanceof Error
              ? productsQuery.error.message
              : "Failed to load products"}
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">
                Product
              </label>
              <select
                value={selectedProductId}
                onChange={(e) => {
                  const next = e.target.value;
                  setSelectedProductId(next);
                  setSkuLookup(next);
                  setSkuUpsert(next);
                }}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 bg-white text-slate-900"
              >
                <option value="">Select product</option>
                {productsQuery.data?.content.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.id})
                  </option>
                ))}
              </select>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
              Selected product id is used as SKU in current checkout/inventory flow.
            </div>
          </div>
        )}
      </section>

      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <form onSubmit={onLookup} className="rounded-xl border border-slate-200 bg-white p-6 space-y-4">
          <h2 className="font-display text-2xl text-slate-900">Lookup Stock</h2>
          <div>
            <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">SKU</label>
            <input
              value={skuLookup}
              onChange={(e) => setSkuLookup(e.target.value)}
              placeholder="Enter SKU"
              className="w-full rounded-lg border border-slate-200 px-3 py-2"
            />
          </div>
          <button
            type="submit"
            disabled={lookupMutation.isPending}
            className="rounded-lg bg-[#2badee] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            {lookupMutation.isPending ? "Loading..." : "Get Stock"}
          </button>
          {lookupError ? <p className="text-sm text-red-600">{lookupError}</p> : null}
        </form>

        <form onSubmit={onUpsert} className="rounded-xl border border-slate-200 bg-white p-6 space-y-4">
          <h2 className="font-display text-2xl text-slate-900">Update Stock</h2>
          <div>
            <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">SKU</label>
            <input
              value={skuUpsert}
              onChange={(e) => setSkuUpsert(e.target.value)}
              placeholder="Enter SKU"
              className="w-full rounded-lg border border-slate-200 px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">
              Available Quantity
            </label>
            <input
              type="number"
              min={0}
              value={availableQuantity}
              onChange={(e) => setAvailableQuantity(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2"
            />
          </div>
          <button
            type="submit"
            disabled={upsertMutation.isPending}
            className="rounded-lg bg-[#2badee] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            {upsertMutation.isPending ? "Saving..." : "Save Stock"}
          </button>
          {upsertMutation.error ? (
            <p className="text-sm text-red-600">
              {upsertMutation.error instanceof Error ? upsertMutation.error.message : "Failed to update stock"}
            </p>
          ) : null}
        </form>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="font-display text-2xl text-slate-900 mb-4">Current Stock</h2>
        {!stock ? (
          <p className="text-slate-500">No stock selected yet.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">SKU</p>
              <p className="text-slate-900">{stock.sku}</p>
            </div>
            <div>
              <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">Available</p>
              <p className="text-slate-900">{stock.availableQuantity}</p>
            </div>
            <div>
              <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">Reserved</p>
              <p className="text-slate-900">{stock.reservedQuantity}</p>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
