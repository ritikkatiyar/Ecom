"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

const KEY = "ecom_wishlist_product_ids";

export default function AccountWishlistPage() {
  const [ids, setIds] = useState<string[]>([]);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(KEY);
      setIds(raw ? JSON.parse(raw) : []);
    } catch {
      setIds([]);
    }
  }, []);

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8">
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-4">Wishlist</h1>
      {!ids.length ? (
        <p className="text-slate-500">No wishlist items yet.</p>
      ) : (
        <div className="space-y-3">
          {ids.map((id) => (
            <Link key={id} href={`/products/${id}`} className="block text-[#2badee] hover:underline">
              Product {id}
            </Link>
          ))}
        </div>
      )}
      <p className="text-xs text-slate-400 mt-6">
        Local-storage wishlist is active. Backend wishlist service integration is scheduled next.
      </p>
    </div>
  );
}
