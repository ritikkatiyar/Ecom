"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { listNotificationsByUser } from "@/lib/api/notifications";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountNotificationsPage() {
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);
  const query = useQuery({
    queryKey: ["account-notifications", numericUserId],
    queryFn: () => listNotificationsByUser(numericUserId!),
    enabled: numericUserId != null,
  });

  return (
    <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
      <div className="p-6 border-b border-slate-100">
        <h1 className="font-display text-4xl font-bold text-slate-900">Notifications</h1>
      </div>
      {query.isLoading ? (
        <div className="p-6 text-slate-500">Loading notifications...</div>
      ) : query.error ? (
        <div className="p-6 text-red-600">
          {query.error instanceof Error ? query.error.message : "Failed to load notifications"}
        </div>
      ) : !query.data?.length ? (
        <div className="p-6 text-slate-500">No notifications yet.</div>
      ) : (
        <div className="divide-y divide-slate-100">
          {query.data.map((n) => (
            <div key={n.id} className="p-6">
              <p className="font-medium text-slate-900">{n.subject}</p>
              <p className="text-sm text-slate-600 mt-1">{n.body}</p>
              <p className="text-xs text-slate-400 mt-2">
                {n.type} · {n.channel} · {n.status}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
