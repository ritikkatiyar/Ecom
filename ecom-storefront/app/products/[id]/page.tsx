import Link from "next/link";

export default async function ProductPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main>
        <nav className="max-w-7xl mx-auto px-6 py-6 flex items-center gap-2 text-xs uppercase tracking-widest text-slate-400">
          <Link href="/" className="hover:text-slate-600">
            Home
          </Link>
          <span className="material-symbols-outlined text-[10px]">chevron_right</span>
          <Link href="/shop" className="hover:text-slate-600">
            Shop
          </Link>
          <span className="material-symbols-outlined text-[10px]">chevron_right</span>
          <span className="text-slate-900">Product {id}</span>
        </nav>
        <section className="max-w-7xl mx-auto px-6 pb-20 grid grid-cols-1 lg:grid-cols-12 gap-16">
          <div className="lg:col-span-7 flex flex-col gap-4">
            <div className="aspect-[4/5] bg-[#EFEBE7] rounded-xl overflow-hidden flex items-center justify-center">
              <span className="material-symbols-outlined text-8xl text-slate-300">
                inventory_2
              </span>
            </div>
          </div>
          <div className="lg:col-span-5 flex flex-col pt-4">
            <div className="mb-2 flex items-center gap-2">
              <div className="flex text-[#2badee]">
                {[1, 2, 3, 4, 5].map((i) => (
                  <span key={i} className="material-symbols-outlined fill-1 text-sm">
                    star
                  </span>
                ))}
              </div>
              <span className="text-xs font-medium text-slate-500 uppercase tracking-widest">
                0 Reviews
              </span>
            </div>
            <h1 className="font-display text-5xl text-slate-900 leading-[1.1] mb-4">
              Product {id}
            </h1>
            <p className="text-[#2badee] font-medium tracking-widest text-sm uppercase mb-6">
              Scent
            </p>
            <div className="text-3xl font-light text-slate-800 mb-8">₹3,441.46</div>
            <div className="space-y-4">
              <div className="flex gap-4">
                <button
                  type="button"
                  className="flex-1 bg-[#2badee] hover:bg-[#2badee]/90 text-white font-bold py-5 rounded-lg transition-all tracking-widest uppercase text-sm"
                >
                  Add to Cart
                </button>
                <button
                  type="button"
                  className="px-5 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  <span className="material-symbols-outlined text-slate-600">
                    favorite
                  </span>
                </button>
              </div>
              <p className="text-center text-xs text-slate-400 font-medium">
                Free shipping on orders above ₹5,000
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
