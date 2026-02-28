import Link from "next/link";
import { getProducts } from "@/lib/api/products";

export const dynamic = "force-dynamic";

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

export default async function ShopPage({
  searchParams,
}: {
  searchParams?: Promise<{ category?: string; brand?: string }>;
}) {
  const sp = (await searchParams) ?? {};
  const page = await getProducts({
    page: 0,
    size: 40,
    sortBy: "name",
    direction: "asc",
    category: sp.category,
    brand: sp.brand,
  });
  const products = page.content.filter((p) => p.active);

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Shop All
        </h1>
        {(sp.category || sp.brand) ? (
          <p className="mb-6 text-sm uppercase tracking-widest text-slate-500">
            {sp.category ? `Category: ${sp.category}` : ""} {sp.brand ? `Brand: ${sp.brand}` : ""}
          </p>
        ) : null}
        {!products.length ? (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
            No products are available yet.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {products.map((product) => (
              <Link
                key={product.id}
                href={`/products/${product.id}`}
                className="group aspect-[4/5] bg-[#EFEBE7] rounded-xl overflow-hidden"
              >
                <img
                  src={productImage(product.imageUrls)}
                  alt={product.name}
                  className="w-full h-full object-cover group-hover:opacity-90 transition-opacity"
                />
                <div className="p-4 bg-white">
                  <p className="font-display text-lg font-semibold text-slate-900">
                    {product.name}
                  </p>
                  <p className="text-sm text-slate-500">{formatPrice(product.price)}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
