"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";

export function Header() {
  const { isAuthenticated, roles } = useAuth();
  const isAdmin = roles.includes("ADMIN");

  return (
    <header className="sticky top-0 z-50 bg-[#F8F6F3]/80 backdrop-blur-md border-b border-black/5">
      <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
        <div className="flex items-center gap-12">
          <Link
            href="/"
            className="font-display text-2xl font-bold tracking-tight text-slate-900"
          >
            ANAYA CANDLES
          </Link>
          <nav className="hidden md:flex items-center gap-8 text-sm font-medium uppercase tracking-widest text-slate-600">
            <Link href="/shop" className="hover:text-[#2badee] transition-colors">
              Shop
            </Link>
            <Link href="/collections" className="hover:text-[#2badee] transition-colors">
              Collections
            </Link>
            <Link href="/search" className="hover:text-[#2badee] transition-colors">
              Search
            </Link>
            {isAdmin && (
              <Link href="/admin/dashboard" className="hover:text-[#2badee] transition-colors">
                Admin
              </Link>
            )}
          </nav>
        </div>
        <div className="flex items-center gap-6">
          <Link
            href="/search"
            className="material-symbols-outlined text-slate-700 hover:text-[#2badee] transition-colors"
          >
            search
          </Link>
          {isAuthenticated ? (
            <Link
              href="/account"
              className="material-symbols-outlined text-slate-700 hover:text-[#2badee] transition-colors"
            >
              person
            </Link>
          ) : (
            <Link
              href="/login"
              className="text-sm font-medium uppercase tracking-widest text-slate-600 hover:text-[#2badee] transition-colors"
            >
              Sign In
            </Link>
          )}
          <Link
            href="/cart"
            className="material-symbols-outlined text-slate-700 hover:text-[#2badee] transition-colors relative"
          >
            shopping_bag
            <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-[#2badee] text-[10px] text-white">
              0
            </span>
          </Link>
        </div>
      </div>
    </header>
  );
}
