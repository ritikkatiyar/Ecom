import Link from "next/link";

export default function Home() {
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
        <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 py-12">
          {[1, 2, 3, 4].map((i) => (
            <Link
              key={i}
              href={`/products/${i}`}
              className="group aspect-[4/5] bg-[#EFEBE7] rounded-xl overflow-hidden"
            >
              <div className="w-full h-full flex items-center justify-center text-slate-400 group-hover:bg-[#EFEBE7]/80 transition-colors">
                <span className="material-symbols-outlined text-6xl">inventory_2</span>
              </div>
              <div className="p-4 bg-white">
                <p className="font-display text-lg font-semibold text-slate-900">
                  Product {i}
                </p>
                <p className="text-sm text-slate-500">From ₹2,499</p>
              </div>
            </Link>
          ))}
        </section>
      </main>
    </div>
  );
}
