import type { Metadata } from "next";
import { ShopProductGrid } from "@/components/shop/ShopProductGrid";
import { buildMetadata } from "@/lib/seo";

export const revalidate = 60;
export const metadata: Metadata = buildMetadata({
  title: "Shop",
  description: "Browse the full Anaya Candles catalog of handcrafted candles and home fragrances.",
  path: "/shop",
});

export default async function ShopPage({
  searchParams,
}: {
  searchParams?: Promise<{ category?: string; brand?: string }>;
}) {
  const sp = (await searchParams) ?? {};

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Shop All
        </h1>
        {(sp.category || sp.brand) ? (
          <p className="mb-6 text-sm uppercase tracking-widest text-slate-500">
            {sp.category ? `Category: ${sp.category}` : ""}{" "}
            {sp.brand ? `Brand: ${sp.brand}` : ""}
          </p>
        ) : null}
        <ShopProductGrid category={sp.category} brand={sp.brand} />
      </main>
    </div>
  );
}
