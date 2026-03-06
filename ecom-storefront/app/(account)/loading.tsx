export default function AccountLoading() {
  return (
    <main className="mx-auto max-w-7xl px-6 py-8">
      <div className="mb-6 h-9 w-64 animate-pulse rounded bg-slate-200" />
      <div className="space-y-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-28 animate-pulse rounded-2xl bg-slate-200" />
        ))}
      </div>
    </main>
  );
}
