export default function AdminLoading() {
  return (
    <main className="mx-auto max-w-7xl px-6 py-8">
      <div className="mb-5 h-9 w-72 animate-pulse rounded bg-slate-200" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-36 animate-pulse rounded-2xl bg-slate-200" />
        ))}
      </div>
    </main>
  );
}
