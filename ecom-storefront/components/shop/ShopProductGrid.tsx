"use client";

import { useEffect, useRef } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { getProducts } from "@/lib/api/products";
import { ShopProductCard } from "./ShopProductCard";

const PAGE_SIZE = 20;

interface ShopProductGridProps {
  category?: string;
  brand?: string;
}

export function ShopProductGrid({ category, brand }: ShopProductGridProps) {
  const loadMoreRef = useRef<HTMLDivElement>(null);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    error,
  } = useInfiniteQuery({
    queryKey: ["shop-products", category, brand],
    queryFn: ({ pageParam }) =>
      getProducts({
        page: pageParam,
        size: PAGE_SIZE,
        sortBy: "name",
        direction: "asc",
        category: category || undefined,
        brand: brand || undefined,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.number + 1,
  });

  useEffect(() => {
    if (!hasNextPage || isFetchingNextPage) return;
    const el = loadMoreRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) fetchNextPage();
      },
      { rootMargin: "200px" }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const products =
    data?.pages.flatMap((p) => p.content.filter((pr) => pr.active)) ?? [];

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="rounded-xl border border-slate-200 bg-white overflow-hidden animate-pulse"
          >
            <div className="aspect-[4/5] bg-slate-100" />
            <div className="p-4 space-y-2">
              <div className="h-4 bg-slate-100 rounded w-3/4" />
              <div className="h-3 bg-slate-100 rounded w-1/3" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-700">
        {error instanceof Error ? error.message : "Failed to load products"}
      </div>
    );
  }

  if (!products.length) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
        No products are available yet.
      </div>
    );
  }

  return (
    <>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {products.map((product) => (
          <ShopProductCard
            key={product.id}
            id={product.id}
            name={product.name}
            price={typeof product.price === "number" ? product.price : Number(product.price)}
            imageUrls={product.imageUrls}
          />
        ))}
      </div>
      <div ref={loadMoreRef} className="h-20 flex items-center justify-center py-8">
        {isFetchingNextPage ? (
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#2badee] border-t-transparent" />
        ) : null}
      </div>
    </>
  );
}
