"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { getUserProfile } from "@/lib/api/users";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountProfilePage() {
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);

  const profileQuery = useQuery({
    queryKey: ["account-profile-page", numericUserId],
    queryFn: () => getUserProfile(numericUserId!),
    enabled: numericUserId != null,
  });

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8">
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-6">Profile</h1>
      {profileQuery.isLoading ? (
        <p className="text-slate-500">Loading profile...</p>
      ) : profileQuery.error ? (
        <p className="text-red-600">
          {profileQuery.error instanceof Error ? profileQuery.error.message : "Failed to load profile"}
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
          <div>
            <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">Email</p>
            <p className="text-slate-900">{profileQuery.data?.email ?? "-"}</p>
          </div>
          <div>
            <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">First Name</p>
            <p className="text-slate-900">{profileQuery.data?.firstName ?? "-"}</p>
          </div>
          <div>
            <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">Last Name</p>
            <p className="text-slate-900">{profileQuery.data?.lastName ?? "-"}</p>
          </div>
          <div>
            <p className="text-slate-400 uppercase tracking-widest text-xs mb-1">Phone</p>
            <p className="text-slate-900">{profileQuery.data?.phoneNumber ?? "-"}</p>
          </div>
        </div>
      )}
    </div>
  );
}
