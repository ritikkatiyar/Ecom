export default function RootLoading() {
  return (
    <main className="mx-auto max-w-7xl px-6 py-10">
      <div className="mb-6 h-10 w-56 animate-pulse rounded bg-slate-200" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="h-56 animate-pulse rounded-2xl bg-slate-200" />
        ))}
      </div>
    </main>
  );
}
