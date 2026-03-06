import Link from "next/link";
import { getProducts } from "@/lib/api/products";

export const revalidate = 60;

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
  const page = await getProducts(
    { page: 0, size: 4, sortBy: "name", direction: "asc" },
    { revalidateSeconds: 60 }
  );
  const featured = page.content.filter((p) => p.active).slice(0, 4);
  const hero = featured[0];

  return (
    <main className="min-h-screen bg-[#F8F6F3]">
      <section className="max-w-7xl mx-auto px-6 py-12 grid grid-cols-1 lg:grid-cols-12 gap-14">
        <div className="lg:col-span-7">
          <div className="aspect-[4/5] bg-[#EFEBE7] rounded-xl overflow-hidden">
            <img
              src={productImage(hero?.imageUrls)}
              alt={hero?.name ?? "Anaya featured candle"}
              className="w-full h-full object-cover"
            />
          </div>
        </div>
        <div className="lg:col-span-5 flex flex-col pt-2">
          <p className="text-primary font-medium tracking-widest text-sm uppercase mb-3">
            Signature Collection
          </p>
          <h1 className="font-display text-5xl text-slate-900 leading-[1.1] mb-5">
            Handcrafted fragrances for the modern home
          </h1>
          <p className="text-lg text-slate-600 mb-8">
            Curated candles and home scents designed with clean ingredients,
            long burn times, and timeless vessels.
          </p>
          <div className="flex flex-wrap gap-3 mb-10">
            <Link
              href="/shop"
              className="px-8 py-4 bg-primary hover:bg-primary/90 text-white font-bold rounded-lg tracking-widest uppercase text-sm transition-all shadow-lg shadow-primary/20"
            >
              Shop Now
            </Link>
            <Link
              href="/collections"
              className="px-8 py-4 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors font-medium uppercase tracking-widest text-sm"
            >
              Explore Collections
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-y-6 pt-8 border-t border-slate-200">
            <div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400 mb-2">Burn Profile</h3>
              <p className="text-slate-800">Slow & Even</p>
            </div>
            <div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400 mb-2">Wax Blend</h3>
              <p className="text-slate-800">Clean Vegan</p>
            </div>
            <div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400 mb-2">Sustainability</h3>
              <p className="text-slate-800">Reusable Vessels</p>
            </div>
            <div>
              <h3 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400 mb-2">Availability</h3>
              <p className="text-slate-800 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-green-500" /> In Stock
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-[#EFEBE7]/50 py-24">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <span className="material-symbols-outlined text-primary text-4xl mb-5">waves</span>
          <h2 className="font-display text-5xl text-slate-900 mb-8 leading-tight">
            A fragrance story in every flame
          </h2>
          <p className="text-xl font-display italic leading-relaxed text-slate-600">
            Our scents are built in layers and burn with intention, filling your
            space with soft depth instead of overwhelming notes.
          </p>
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-6 py-20">
        {!featured.length ? (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-slate-500">
            Featured products will appear here once products are added.
          </div>
        ) : (
          <>
            <div className="flex items-end justify-between mb-10 gap-4">
              <h2 className="font-display text-4xl text-slate-900">Complete the Collection</h2>
              <Link href="/shop" className="text-xs font-bold uppercase tracking-widest text-primary hover:underline">
                View All
              </Link>
            </div>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
              {featured.map((product) => (
                <Link key={product.id} href={`/products/${product.id}`} className="group flex flex-col gap-4">
                  <div className="aspect-[3/4] bg-[#EFEBE7] rounded-xl overflow-hidden relative">
                    <img
                      src={productImage(product.imageUrls)}
                      alt={product.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                    />
                    <div className="absolute bottom-4 left-4 right-4 bg-white/90 backdrop-blur text-[10px] font-bold uppercase tracking-widest py-3 rounded opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 text-center">
                      Quick View
                    </div>
                  </div>
                  <div>
                    <h3 className="text-xs font-bold uppercase tracking-widest text-slate-800 mb-1">
                      {product.name}
                    </h3>
                    <p className="text-xs text-slate-500">{formatPrice(product.price)}</p>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </section>
    </main>
  );
}
