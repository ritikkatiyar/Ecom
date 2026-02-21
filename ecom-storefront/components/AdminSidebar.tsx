"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: "dashboard" },
  { href: "/admin/products", label: "Products", icon: "inventory_2" },
  { href: "/admin/orders", label: "Orders", icon: "shopping_bag" },
];

export function AdminSidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 border-r border-slate-200 bg-white flex flex-col fixed h-full">
      <div className="p-8 flex items-center gap-3">
        <div className="size-8 rounded-full bg-[#2badee] flex items-center justify-center text-white">
          <span className="material-symbols-outlined text-lg">auto_awesome</span>
        </div>
        <h1 className="font-display text-xl font-bold tracking-tight text-slate-900">
          Essence Admin
        </h1>
      </div>
      <nav className="flex-1 px-4 space-y-1">
        {navItems.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                active
                  ? "bg-[#2badee]/10 text-[#2badee] border-r-2 border-[#2badee]"
                  : "text-slate-500 hover:bg-slate-50"
              }`}
            >
              <span className="material-symbols-outlined text-[20px]">
                {item.icon}
              </span>
              <span className="text-sm font-medium">{item.label}</span>
            </Link>
          );
        })}
      </nav>
      <div className="p-6">
        <Link
          href="/"
          className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-[#2badee] text-white rounded-lg text-sm font-semibold hover:bg-[#2badee]/90 transition-all"
        >
          <span className="material-symbols-outlined !text-[18px]">visibility</span>
          View Store
        </Link>
      </div>
    </aside>
  );
}
