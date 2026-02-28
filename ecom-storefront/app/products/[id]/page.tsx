import Link from "next/link";
import { notFound } from "next/navigation";
import { getProduct } from "@/lib/api/products";
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
    : "https://images.unsplash.com/photo-1603006905393-cb2df4e7f5f4?auto=format&fit=crop&w=1400&q=80";
}

export default async function ProductPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let product: Awaited<ReturnType<typeof getProduct>>;
  try {
    product = await getProduct(id);
  } catch {
    notFound();
  }

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
          <span className="text-slate-900">{product.name}</span>
        </nav>
        <section className="max-w-7xl mx-auto px-6 pb-20 grid grid-cols-1 lg:grid-cols-12 gap-16">
          <div className="lg:col-span-7 flex flex-col gap-4">
            <div className="aspect-[4/5] bg-[#EFEBE7] rounded-xl overflow-hidden">
              <img
                src={productImage(product.imageUrls)}
                alt={product.name}
                className="w-full h-full object-cover"
              />
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
              {product.name}
            </h1>
            <p className="text-[#2badee] font-medium tracking-widest text-sm uppercase mb-6">
              {product.category}
            </p>
            <div className="text-3xl font-light text-slate-800 mb-8">
              {formatPrice(product.price)}
            </div>
            <div className="space-y-4">
              <div className="flex gap-4">
                <AddToCartButton productId={product.id} />
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
                Free shipping on orders above INR 5,000
              </p>
            </div>
            <div className="mt-12 pt-12 border-t border-slate-200 grid grid-cols-2 gap-y-8">
              <div>
                <h4 className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2">
                  Brand
                </h4>
                <p className="text-slate-800">{product.brand}</p>
              </div>
              <div>
                <h4 className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2">
                  Availability
                </h4>
                <p className="text-slate-800 flex items-center gap-1">
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      product.active ? "bg-green-500" : "bg-slate-400"
                    }`}
                  />
                  {product.active ? "In Stock" : "Out of Stock"}
                </p>
              </div>
              {product.colors && product.colors.length > 0 ? (
                <div>
                  <h4 className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2">
                    Colors
                  </h4>
                  <p className="text-slate-800">{product.colors.join(", ")}</p>
                </div>
              ) : null}
              {product.sizes && product.sizes.length > 0 ? (
                <div>
                  <h4 className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2">
                    Sizes
                  </h4>
                  <p className="text-slate-800">{product.sizes.join(", ")}</p>
                </div>
              ) : null}
            </div>
            {product.description ? (
              <div className="mt-10 text-slate-600 leading-relaxed">
                {product.description}
              </div>
            ) : null}
          </div>
        </section>
      </main>
    </div>
  );
}
