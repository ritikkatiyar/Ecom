import Link from "next/link";

export default function ShopPage() {
  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Shop All
        </h1>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
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
        </div>
      </main>
    </div>
  );
}
