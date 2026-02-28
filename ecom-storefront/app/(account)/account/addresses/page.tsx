"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { listUserAddresses } from "@/lib/api/users";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountAddressesPage() {
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);

  const addressesQuery = useQuery({
    queryKey: ["account-addresses-page", numericUserId],
    queryFn: () => listUserAddresses(numericUserId!),
    enabled: numericUserId != null,
  });

  return (
    <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
      <div className="p-6 border-b border-slate-100">
        <h1 className="font-display text-4xl font-bold text-slate-900">Addresses</h1>
      </div>
      {addressesQuery.isLoading ? (
        <div className="p-6 text-slate-500">Loading addresses...</div>
      ) : addressesQuery.error ? (
        <div className="p-6 text-red-600">
          {addressesQuery.error instanceof Error ? addressesQuery.error.message : "Failed to load addresses"}
        </div>
      ) : !addressesQuery.data?.length ? (
        <div className="p-6 text-slate-500">No saved addresses.</div>
      ) : (
        <div className="divide-y divide-slate-100">
          {addressesQuery.data.map((address) => (
            <div key={address.id} className="p-6">
              <p className="font-medium text-slate-900">
                {address.label || "Address"} {address.defaultAddress ? "(Default)" : ""}
              </p>
              <p className="text-sm text-slate-600 mt-1">
                {address.line1}
                {address.line2 ? `, ${address.line2}` : ""}
              </p>
              <p className="text-sm text-slate-600">
                {[address.city, address.state, address.postalCode, address.country]
                  .filter(Boolean)
                  .join(", ")}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
