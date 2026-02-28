"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { listUserOrders } from "@/lib/api/orders";
import { getUserProfile, listUserAddresses } from "@/lib/api/users";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountPage() {
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);

  const profileQuery = useQuery({
    queryKey: ["account-profile", numericUserId],
    queryFn: () => getUserProfile(numericUserId!),
    enabled: numericUserId != null,
  });

  const addressesQuery = useQuery({
    queryKey: ["account-addresses", numericUserId],
    queryFn: () => listUserAddresses(numericUserId!),
    enabled: numericUserId != null,
  });

  const ordersQuery = useQuery({
    queryKey: ["account-orders", numericUserId],
    queryFn: () => listUserOrders(numericUserId!),
    enabled: numericUserId != null,
  });

  if (numericUserId == null) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-amber-700">
        Account id is not available for this session.
      </div>
    );
  }

  const profile = profileQuery.data;
  const orders = ordersQuery.data ?? [];
  const addresses = addressesQuery.data ?? [];
  const recentOrders = orders.slice(0, 5);

  return (
    <div className="space-y-8">
      <section className="rounded-xl border border-slate-200 bg-white p-8">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">
          Account Dashboard
        </h1>
        <p className="text-slate-600">
          {profile ? `${profile.firstName ?? ""} ${profile.lastName ?? ""}`.trim() || profile.email : "Loading profile..."}
        </p>
      </section>

      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <p className="text-xs uppercase tracking-widest text-slate-400">Recent Orders</p>
          <p className="font-display text-3xl text-slate-900 mt-2">{orders.length}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <p className="text-xs uppercase tracking-widest text-slate-400">Saved Addresses</p>
          <p className="font-display text-3xl text-slate-900 mt-2">{addresses.length}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <p className="text-xs uppercase tracking-widest text-slate-400">Wishlist</p>
          <p className="font-display text-3xl text-slate-900 mt-2">0</p>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="font-display text-2xl text-slate-900 mb-4">Manage Account</h2>
        <div className="flex flex-wrap gap-3">
          <Link href="/account/orders" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Orders
          </Link>
          <Link href="/account/profile" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Profile
          </Link>
          <Link href="/account/addresses" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Addresses
          </Link>
          <Link href="/account/preferences" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Preferences
          </Link>
          <Link href="/account/notifications" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Notifications
          </Link>
          <Link href="/account/security" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Security
          </Link>
          <Link href="/account/wishlist" className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700">
            Wishlist
          </Link>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <h2 className="font-display text-2xl text-slate-900">Recent Orders</h2>
          <Link href="/account" className="text-sm font-semibold text-[#2badee] hover:underline">
            View All
          </Link>
        </div>
        {ordersQuery.isLoading ? (
          <div className="p-6 text-slate-500">Loading orders...</div>
        ) : ordersQuery.error ? (
          <div className="p-6 text-red-600">
            {ordersQuery.error instanceof Error ? ordersQuery.error.message : "Failed to load orders"}
          </div>
        ) : recentOrders.length === 0 ? (
          <div className="p-6 text-slate-500">No orders found yet.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {recentOrders.map((order) => (
              <div key={order.id} className="p-6 flex items-center justify-between">
                <div>
                  <p className="font-medium text-slate-900">Order #{order.id.slice(0, 8)}</p>
                  <p className="text-sm text-slate-500">
                    {new Date(order.createdAt).toLocaleString()}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-slate-900">{order.status}</p>
                  <p className="text-sm text-slate-600">{order.currency} {order.totalAmount}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
