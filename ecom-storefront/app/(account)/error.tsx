"use client";

import Link from "next/link";

export default function AccountError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <p className="text-xs font-semibold tracking-[0.18em] text-slate-500">
        ACCOUNT ERROR
      </p>
      <h1 className="mt-2 font-display text-3xl text-slate-900">
        We hit an account page error.
      </h1>
      <p className="mt-3 text-sm text-slate-600">
        {error.message || "Unexpected account rendering error."}
      </p>
      <div className="mt-6 flex items-center gap-3">
        <button
          type="button"
          onClick={reset}
          className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
          Retry
        </button>
        <Link
          href="/account"
          className="rounded-full border border-slate-300 px-5 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
        >
          Back to account
        </Link>
      </div>
    </main>
  );
}
