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

export default async function Home() {
  const page = await getProducts({ page: 0, size: 4, sortBy: "name", direction: "asc" });
  const featured = page.content.filter((p) => p.active).slice(0, 4);

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-16">
        <section className="text-center py-24">
          <h1 className="font-display text-5xl md:text-6xl font-bold text-slate-900 tracking-tight mb-6">
            Anaya Candles
          </h1>
          <p className="text-xl text-slate-600 max-w-2xl mx-auto mb-10">
            Handcrafted candles for your space. Discover our collections and
            bring warmth to your home.
          </p>
          <div className="flex flex-wrap gap-4 justify-center">
            <Link
              href="/shop"
              className="px-8 py-4 bg-[#2badee] hover:bg-[#2badee]/90 text-white font-bold rounded-lg tracking-widest uppercase text-sm transition-all"
            >
              Shop Now
            </Link>
            <Link
              href="/search"
              className="px-8 py-4 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors font-medium uppercase tracking-widest text-sm"
            >
              Search Products
            </Link>
          </div>
        </section>
        {!featured.length ? (
          <section className="py-12">
            <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
              Featured products will appear here once products are added.
            </div>
          </section>
        ) : (
          <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 py-12">
            {featured.map((product) => (
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
          </section>
        )}
      </main>
    </div>
  );
}
