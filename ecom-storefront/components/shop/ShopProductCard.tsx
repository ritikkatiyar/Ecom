"use client";

import Link from "next/link";
import { AddToCartButton } from "@/components/cart/AddToCartButton";

function formatPrice(price: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(price);
}

function productImage(urls?: string[]): string {
  return urls && urls.length > 0
    ? urls[0]
    : "https://images.unsplash.com/photo-1603006905393-cb2df4e7f5f4?auto=format&fit=crop&w=1200&q=80";
}

interface ShopProductCardProps {
  id: string;
  name: string;
  price: number;
  imageUrls?: string[];
}

export function ShopProductCard({ id, name, price, imageUrls }: ShopProductCardProps) {
  return (
    <div className="group rounded-xl overflow-hidden border border-slate-200 bg-white hover:shadow-md transition-shadow">
      <Link href={`/products/${id}`} className="block aspect-[4/5] bg-[#EFEBE7] overflow-hidden">
        <img
          src={productImage(imageUrls)}
          alt={name}
          className="w-full h-full object-cover group-hover:opacity-90 transition-opacity"
        />
      </Link>
      <div className="p-4">
        <Link href={`/products/${id}`}>
          <p className="font-display text-lg font-semibold text-slate-900 hover:text-[#2badee] transition-colors">
            {name}
          </p>
        </Link>
        <p className="text-sm text-slate-500 mt-1">{formatPrice(price)}</p>
        <div className="mt-3" onClick={(e) => e.preventDefault()}>
          <AddToCartButton productId={id} variant="compact" />
        </div>
      </div>
    </div>
  );
}
