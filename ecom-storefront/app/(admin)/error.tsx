"use client";

export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <p className="text-xs font-semibold tracking-[0.18em] text-slate-500">
        ADMIN ERROR
      </p>
      <h1 className="mt-2 font-display text-3xl text-slate-900">
        Admin page failed to load.
      </h1>
      <p className="mt-3 text-sm text-slate-600">
        {error.message || "Unexpected admin rendering error."}
      </p>
      <button
        type="button"
        onClick={reset}
        className="mt-6 rounded-full bg-slate-900 px-5 py-2 text-sm font-semibold text-white hover:opacity-90"
      >
        Retry admin page
      </button>
    </main>
  );
}
