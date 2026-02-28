"use client";

import { useQuery } from "@tanstack/react-query";
import { getSearchDatasetHealth } from "@/lib/api/admin";

export default function AdminSystemPage() {
  const healthQuery = useQuery({
    queryKey: ["admin-system-search-health"],
    queryFn: getSearchDatasetHealth,
    staleTime: 60 * 1000,
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">System Health</h1>
        <p className="text-slate-500">
          Operational reliability panel for search dataset and release-health surfaces.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="font-display text-2xl text-slate-900 mb-4">Search Dataset Health</h2>
        {healthQuery.isLoading ? (
          <p className="text-slate-500">Loading dataset health...</p>
        ) : healthQuery.error ? (
          <p className="text-red-600">Failed to load dataset health.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Dataset Version</p>
              <p className="text-slate-900">{healthQuery.data?.datasetVersion ?? "-"}</p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Dataset Size</p>
              <p className="text-slate-900">{healthQuery.data?.datasetSize ?? 0}</p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Days Since Refresh</p>
              <p className="text-slate-900">{healthQuery.data?.daysSinceRefresh ?? "-"}</p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Refresh Required</p>
              <p className={healthQuery.data?.refreshRequired ? "text-amber-600" : "text-emerald-600"}>
                {healthQuery.data?.refreshRequired ? "YES" : "NO"}
              </p>
            </div>
          </div>
        )}
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="font-display text-2xl text-slate-900 mb-2">Release Gate Metrics</h2>
        <p className="text-slate-500 text-sm">
          Dashboard-level release gate metrics will be added once gateway metric APIs are exposed for UI consumption.
        </p>
      </section>
    </div>
  );
}
