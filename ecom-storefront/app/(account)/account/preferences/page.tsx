"use client";

import { FormEvent, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { getUserPreferences, updateUserPreferences } from "@/lib/api/users";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

export default function AccountPreferencesPage() {
  const queryClient = useQueryClient();
  const { userId } = useAuth();
  const numericUserId = parseUserId(userId);

  const prefQuery = useQuery({
    queryKey: ["account-preferences", numericUserId],
    queryFn: () => getUserPreferences(numericUserId!),
    enabled: numericUserId != null,
  });

  const [marketingEmailsEnabled, setMarketingEmailsEnabled] = useState(false);
  const [smsEnabled, setSmsEnabled] = useState(false);
  const [preferredLanguage, setPreferredLanguage] = useState("en");
  const [preferredCurrency, setPreferredCurrency] = useState("INR");

  useEffect(() => {
    if (!prefQuery.data) return;
    setMarketingEmailsEnabled(prefQuery.data.marketingEmailsEnabled);
    setSmsEnabled(prefQuery.data.smsEnabled);
    setPreferredLanguage(prefQuery.data.preferredLanguage);
    setPreferredCurrency(prefQuery.data.preferredCurrency);
  }, [prefQuery.data]);

  const updateMutation = useMutation({
    mutationFn: () =>
      updateUserPreferences(numericUserId!, {
        marketingEmailsEnabled,
        smsEnabled,
        preferredLanguage,
        preferredCurrency,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["account-preferences", numericUserId] });
    },
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    updateMutation.mutate();
  };

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8">
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-6">Preferences</h1>
      {prefQuery.error ? (
        <p className="text-red-600 mb-4">
          {prefQuery.error instanceof Error ? prefQuery.error.message : "Failed to load preferences"}
        </p>
      ) : null}
      <form onSubmit={onSubmit} className="space-y-5 max-w-xl">
        <label className="flex items-center gap-3 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={marketingEmailsEnabled}
            onChange={(e) => setMarketingEmailsEnabled(e.target.checked)}
          />
          Marketing Emails
        </label>
        <label className="flex items-center gap-3 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={smsEnabled}
            onChange={(e) => setSmsEnabled(e.target.checked)}
          />
          SMS Alerts
        </label>
        <div>
          <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">Preferred Language</label>
          <input
            value={preferredLanguage}
            onChange={(e) => setPreferredLanguage(e.target.value)}
            maxLength={10}
            className="w-full rounded-lg border border-slate-200 px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-xs uppercase tracking-widest text-slate-400 mb-1">Preferred Currency</label>
          <input
            value={preferredCurrency}
            onChange={(e) => setPreferredCurrency(e.target.value)}
            maxLength={10}
            className="w-full rounded-lg border border-slate-200 px-3 py-2"
          />
        </div>
        <button
          type="submit"
          disabled={updateMutation.isPending}
          className="rounded-lg bg-[#2badee] px-5 py-3 text-sm font-semibold uppercase tracking-widest text-white disabled:opacity-60"
        >
          {updateMutation.isPending ? "Saving..." : "Save Preferences"}
        </button>
      </form>
    </div>
  );
}
