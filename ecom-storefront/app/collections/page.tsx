import Link from "next/link";
import { getProducts } from "@/lib/api/products";

export const dynamic = "force-dynamic";

interface GroupCard {
  label: string;
  count: number;
  href: string;
}

function topGroups(values: string[], kind: "category" | "brand"): GroupCard[] {
  const freq = new Map<string, number>();
  values
    .filter((v) => v && v.trim().length > 0)
    .forEach((v) => freq.set(v, (freq.get(v) ?? 0) + 1));

  return Array.from(freq.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([label, count]) => ({
      label,
      count,
      href: kind === "category" ? `/shop?category=${encodeURIComponent(label)}` : `/shop?brand=${encodeURIComponent(label)}`,
    }));
}

export default async function CollectionsPage() {
  const page = await getProducts({ page: 0, size: 100, sortBy: "name", direction: "asc" });
  const products = page.content.filter((p) => p.active);

  const categoryCards = topGroups(products.map((p) => p.category), "category");
  const brandCards = topGroups(products.map((p) => p.brand), "brand");

  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">
          Collections
        </h1>
        <p className="text-slate-600 mb-10">
          Browse curated collections from live catalog data.
        </p>

        <section className="mb-12">
          <h2 className="font-display text-2xl text-slate-900 mb-5">By Category</h2>
          {categoryCards.length === 0 ? (
            <div className="rounded-xl border border-slate-200 bg-white p-8 text-slate-500">
              No categories available yet.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {categoryCards.map((card) => (
                <Link
                  key={`cat-${card.label}`}
                  href={card.href}
                  className="rounded-xl border border-slate-200 bg-white p-5 hover:shadow-md transition-shadow"
                >
                  <p className="text-xs uppercase tracking-widest text-[#2badee] mb-2">
                    Category
                  </p>
                  <p className="font-display text-2xl text-slate-900">{card.label}</p>
                  <p className="text-sm text-slate-500 mt-2">{card.count} products</p>
                </Link>
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="font-display text-2xl text-slate-900 mb-5">By Brand</h2>
          {brandCards.length === 0 ? (
            <div className="rounded-xl border border-slate-200 bg-white p-8 text-slate-500">
              No brands available yet.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {brandCards.map((card) => (
                <Link
                  key={`brand-${card.label}`}
                  href={card.href}
                  className="rounded-xl border border-slate-200 bg-white p-5 hover:shadow-md transition-shadow"
                >
                  <p className="text-xs uppercase tracking-widest text-[#2badee] mb-2">
                    Brand
                  </p>
                  <p className="font-display text-2xl text-slate-900">{card.label}</p>
                  <p className="text-sm text-slate-500 mt-2">{card.count} products</p>
                </Link>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
