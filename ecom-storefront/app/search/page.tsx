"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { searchProducts } from "@/lib/api/search";

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(price);
}

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 12;
  const debouncedQuery = useDebouncedValue(query.trim(), 350);

  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: ["search-products", debouncedQuery, page, pageSize],
    queryFn: () =>
      searchProducts({
        q: debouncedQuery || undefined,
        page,
        size: pageSize,
        activeOnly: true,
      }),
  });

  const hasNext = data ? (data.page + 1) * data.size < data.totalElements : false;
  const canPrev = page > 0;

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-10">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Search
        </h1>

        <div className="mb-8">
          <input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Search by name or description..."
            className="w-full rounded-xl border border-slate-200 bg-white px-5 py-4 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
          />
          <p className="mt-2 text-xs uppercase tracking-widest text-slate-400">
            Debounced query with live backend search
          </p>
        </div>

        {error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-700">
            {error instanceof Error ? error.message : "Search failed"}
          </div>
        ) : null}

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {Array.from({ length: 8 }).map((_, idx) => (
              <div key={idx} className="rounded-xl bg-white border border-slate-200 overflow-hidden">
                <div className="aspect-[4/5] bg-slate-100 animate-pulse" />
                <div className="p-4 space-y-2">
                  <div className="h-4 bg-slate-100 rounded animate-pulse" />
                  <div className="h-4 w-1/2 bg-slate-100 rounded animate-pulse" />
                </div>
              </div>
            ))}
          </div>
        ) : data && data.content.length > 0 ? (
          <>
            <div className="mb-4 text-sm text-slate-500">
              {isFetching ? "Refreshing..." : `${data.totalElements} results`}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {data.content.map((product) => (
                <Link
                  key={product.productId}
                  href={`/products/${product.productId}`}
                  className="group rounded-xl bg-white border border-slate-200 overflow-hidden hover:shadow-md transition-shadow"
                >
                  <div className="aspect-[4/5] bg-[#EFEBE7] flex items-center justify-center text-slate-400 group-hover:bg-[#e9e4de] transition-colors">
                    <span className="material-symbols-outlined text-6xl">inventory_2</span>
                  </div>
                  <div className="p-4">
                    <p className="font-display text-lg font-semibold text-slate-900 line-clamp-1">
                      {product.name}
                    </p>
                    <p className="text-xs uppercase tracking-widest text-slate-400 mt-1 line-clamp-1">
                      {product.category || "Uncategorized"} · {product.brand || "Brand"}
                    </p>
                    <p className="text-sm text-slate-600 mt-2">{formatPrice(product.price)}</p>
                  </div>
                </Link>
              ))}
            </div>

            <div className="mt-8 flex items-center justify-center gap-3">
              <button
                type="button"
                disabled={!canPrev}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-4 py-2 rounded-lg border border-slate-200 bg-white text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="text-sm text-slate-500">Page {page + 1}</span>
              <button
                type="button"
                disabled={!hasNext}
                onClick={() => setPage((p) => p + 1)}
                className="px-4 py-2 rounded-lg border border-slate-200 bg-white text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </>
        ) : (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
            No products found for this search.
          </div>
        )}
      </main>
    </div>
  );
}
