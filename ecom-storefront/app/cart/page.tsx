export default function CartPage() {
  return (
    <div className="min-h-screen bg-[#F8F6F3]">
      <main className="max-w-7xl mx-auto px-6 py-12">
        <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
          Your Cart
        </h1>
        <p className="text-slate-600">
          Cart will connect to backend <code className="bg-slate-100 px-1 rounded">/api/cart</code>.
        </p>
      </main>
    </div>
  );
}
