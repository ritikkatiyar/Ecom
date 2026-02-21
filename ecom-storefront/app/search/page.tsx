export default function SearchPage() {
  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-12">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Search
        </h1>
        <p className="text-slate-600">
          Search will connect to backend <code className="bg-slate-100 px-1 rounded">/api/search/products</code>.
        </p>
      </main>
    </div>
  );
}
