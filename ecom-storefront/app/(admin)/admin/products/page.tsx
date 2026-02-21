"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { getProducts } from "@/lib/api/products";

export default function AdminProductsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-products"],
    queryFn: () => getProducts({ page: 0, size: 20 }),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#2badee] border-t-transparent" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg bg-red-50 px-4 py-3 text-red-700">
        {error instanceof Error ? error.message : "Failed to load products"}
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="font-display text-4xl font-bold text-slate-900">
          Products
        </h1>
        <Link
          href="/admin/products/new"
          className="px-4 py-2 bg-[#2badee] text-white rounded-lg font-semibold hover:bg-[#2badee]/90 transition-colors"
        >
          Add Product
        </Link>
      </div>

      {!data?.content?.length ? (
        <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
          No products yet. Add your first product to get started.
        </div>
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50 text-[10px] uppercase tracking-widest text-slate-400 font-bold">
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Category</th>
                <th className="px-6 py-4">Brand</th>
                <th className="px-6 py-4">Price</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.content.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 font-medium text-slate-900">{p.name}</td>
                  <td className="px-6 py-4 text-slate-600">{p.category}</td>
                  <td className="px-6 py-4 text-slate-600">{p.brand}</td>
                  <td className="px-6 py-4 text-slate-900">
                    ₹{typeof p.price === "number" ? p.price.toFixed(2) : p.price}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase ${
                        p.active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      {p.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <Link
                      href={`/admin/products/${p.id}/edit`}
                      className="text-[#2badee] hover:underline text-sm font-medium"
                    >
                      Edit
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <p className="mt-4 text-sm text-slate-500">
          Page {data.number + 1} of {data.totalPages} ({data.totalElements} products)
        </p>
      )}
    </div>
  );
}
