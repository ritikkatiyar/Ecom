"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  getProviderOutageMode,
  getSearchDatasetHealth,
  listProviderDeadLetters,
} from "@/lib/api/admin";

export default function AdminDashboardPage() {
  const datasetQuery = useQuery({
    queryKey: ["admin-search-dataset-health"],
    queryFn: getSearchDatasetHealth,
    staleTime: 60 * 1000,
  });

  const outageModeQuery = useQuery({
    queryKey: ["admin-payment-outage-mode"],
    queryFn: getProviderOutageMode,
    staleTime: 15 * 1000,
  });

  const deadLettersQuery = useQuery({
    queryKey: ["admin-payment-dead-letters"],
    queryFn: listProviderDeadLetters,
    staleTime: 30 * 1000,
  });

  return (
    <div>
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-2">
        Dashboard Overview
      </h1>
      <p className="text-slate-500 mb-8">
        Operations view mapped to live backend health and payment reliability endpoints.
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
          <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Payment Outage Mode</p>
          <p className="mt-2 text-2xl font-display text-slate-900">
            {outageModeQuery.isLoading
              ? "..."
              : outageModeQuery.data?.outageMode
              ? "ENABLED"
              : "DISABLED"}
          </p>
        </div>
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Payment DLQ</p>
          <p className="mt-2 text-2xl font-display text-slate-900">
            {deadLettersQuery.isLoading ? "..." : deadLettersQuery.data?.length ?? 0}
          </p>
          <p className="text-xs text-slate-500 mt-1">Provider dead-letter records</p>
        </div>
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Search Dataset</p>
          <p className="mt-2 text-2xl font-display text-slate-900">
            {datasetQuery.isLoading ? "..." : datasetQuery.data?.datasetSize ?? 0}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {datasetQuery.data?.refreshRequired ? "Refresh required" : "Healthy cadence"}
          </p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <h2 className="font-display text-2xl text-slate-900 mb-4">Reliability Snapshot</h2>
        {datasetQuery.error || deadLettersQuery.error || outageModeQuery.error ? (
          <p className="text-red-600 text-sm">
            Failed to load one or more operational metrics.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Dataset Version</p>
              <p className="text-slate-900">{datasetQuery.data?.datasetVersion ?? "-"}</p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Days Since Refresh</p>
              <p className="text-slate-900">{datasetQuery.data?.daysSinceRefresh ?? "-"}</p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">Last Refreshed</p>
              <p className="text-slate-900">
                {datasetQuery.data?.lastRefreshedAt
                  ? new Date(datasetQuery.data.lastRefreshedAt).toLocaleString()
                  : "-"}
              </p>
            </div>
            <div>
              <p className="text-slate-500 uppercase tracking-widest text-xs mb-1">DLQ Pending</p>
              <p className="text-slate-900">{deadLettersQuery.data?.length ?? 0}</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
