"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useFlags } from "@/context/FlagsContext";

const STORAGE_KEY = "beta-onboarding-seen";

export function BetaOnboardingModal() {
  const { betaBannerEnabled } = useFlags();
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (!betaBannerEnabled) return;
    if (typeof window === "undefined") return;
    const seen = sessionStorage.getItem(STORAGE_KEY);
    if (!seen) setShow(true);
  }, [betaBannerEnabled]);

  function handleDismiss() {
    sessionStorage.setItem(STORAGE_KEY, "1");
    setShow(false);
  }

  if (!show) return null;

  return (
    <div
      role="dialog"
      aria-labelledby="beta-modal-title"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50"
    >
      <div className="max-w-md w-full rounded-xl bg-white p-8 shadow-xl">
        <h2 id="beta-modal-title" className="font-display text-2xl font-bold text-slate-900 mb-3">
          Welcome to our Beta
        </h2>
        <p className="text-slate-600 mb-6">
          You are among the first to try our new store. Things may change as we improve. We would love to hear from you.
        </p>
        <div className="flex flex-col sm:flex-row gap-3">
          <Link
            href="/feedback"
            className="flex-1 text-center rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold text-white hover:bg-[#2badee]/90"
          >
            Share Feedback
          </Link>
          <button
            type="button"
            onClick={handleDismiss}
            className="rounded-lg border border-slate-200 px-6 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  );
}
