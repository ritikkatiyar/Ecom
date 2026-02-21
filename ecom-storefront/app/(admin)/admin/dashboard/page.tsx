import Link from "next/link";

export default function AdminDashboardPage() {
  return (
    <div>
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">
        Dashboard Overview
      </h1>
      <p className="text-slate-500 mb-8">
        Welcome back. Here is what&apos;s happening with Essence today.
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Quick Actions</p>
          <Link
            href="/admin/products/new"
            className="mt-2 inline-flex items-center gap-2 text-[#2badee] font-semibold hover:underline"
          >
            <span className="material-symbols-outlined text-xl">add</span>
            Add Product
          </Link>
        </div>
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Products</p>
          <Link href="/admin/products" className="mt-2 inline-flex items-center gap-2 text-[#2badee] font-semibold hover:underline">
            Manage Products
          </Link>
        </div>
      </div>
    </div>
  );
}
