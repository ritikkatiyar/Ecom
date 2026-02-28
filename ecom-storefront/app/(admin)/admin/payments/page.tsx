"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getProviderOutageMode,
  listProviderDeadLetters,
  requeueProviderDeadLetter,
  setProviderOutageMode,
} from "@/lib/api/admin";

export default function AdminPaymentsPage() {
  const queryClient = useQueryClient();
  const outageQuery = useQuery({
    queryKey: ["admin-payment-outage-mode"],
    queryFn: getProviderOutageMode,
  });
  const dlqQuery = useQuery({
    queryKey: ["admin-payment-dead-letters"],
    queryFn: listProviderDeadLetters,
  });

  const toggleMutation = useMutation({
    mutationFn: (enabled: boolean) => setProviderOutageMode(enabled),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-payment-outage-mode"] }),
  });

  const requeueMutation = useMutation({
    mutationFn: (id: number) => requeueProviderDeadLetter(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-payment-dead-letters"] }),
  });

  const outageMode = outageQuery.data?.outageMode ?? false;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">Payment Operations</h1>
        <p className="text-slate-500">Manage provider outage mode and dead-letter requeue operations.</p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <p className="text-xs uppercase tracking-widest text-slate-400 mb-2">Provider Outage Mode</p>
        <p className="text-2xl font-display text-slate-900 mb-4">{outageMode ? "ENABLED" : "DISABLED"}</p>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => toggleMutation.mutate(true)}
            disabled={toggleMutation.isPending}
            className="rounded-lg bg-[#2badee] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            Enable
          </button>
          <button
            type="button"
            onClick={() => toggleMutation.mutate(false)}
            disabled={toggleMutation.isPending}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:opacity-60"
          >
            Disable
          </button>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white overflow-hidden">
        <div className="p-6 border-b border-slate-100">
          <h2 className="font-display text-2xl text-slate-900">Provider Dead Letters</h2>
        </div>
        {dlqQuery.isLoading ? (
          <div className="p-6 text-slate-500">Loading dead letters...</div>
        ) : dlqQuery.error ? (
          <div className="p-6 text-red-600">Failed to load dead letters.</div>
        ) : !dlqQuery.data?.length ? (
          <div className="p-6 text-slate-500">No dead-letter records.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {dlqQuery.data.map((row) => (
              <div key={row.id} className="p-6 flex items-center justify-between gap-4">
                <div>
                  <p className="font-medium text-slate-900">Order {row.orderId}</p>
                  <p className="text-sm text-slate-500">
                    Attempts: {row.attempts} | Status: {row.status}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => requeueMutation.mutate(row.id)}
                  disabled={requeueMutation.isPending}
                  className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-xs font-semibold uppercase tracking-widest text-slate-700 disabled:opacity-60"
                >
                  Requeue
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
