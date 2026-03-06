"use client";

import Link from "next/link";

export default function RootError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="mx-auto max-w-3xl px-6 py-14">
      <p className="text-xs font-semibold tracking-[0.18em] text-slate-500">
        SOMETHING WENT WRONG
      </p>
      <h1 className="mt-2 font-display text-4xl text-slate-900">
        We could not load this page.
      </h1>
      <p className="mt-3 text-sm text-slate-600">
        {error.message || "Unexpected error while rendering this route."}
      </p>
      <div className="mt-6 flex items-center gap-3">
        <button
          type="button"
          onClick={reset}
          className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
          Try again
        </button>
        <Link
          href="/"
          className="rounded-full border border-slate-300 px-5 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
        >
          Go Home
        </Link>
      </div>
    </main>
  );
}
