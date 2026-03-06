import Link from "next/link";

export function Footer() {
  return (
    <footer className="bg-[#EFEBE7] pt-16 pb-10">
      <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-4 gap-10 mb-14">
        <div>
          <Link href="/" className="font-display text-2xl font-bold tracking-tight text-slate-900">
            ANAYA CANDLES
          </Link>
          <p className="mt-5 text-xs leading-6 text-slate-500 uppercase tracking-[0.15em]">
            Handcrafted sensory luxury for modern homes. Thoughtful fragrance, clean burn, timeless design.
          </p>
        </div>
        <div>
          <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-900 mb-5">Shop</h4>
          <ul className="space-y-3 text-xs font-medium text-slate-500 uppercase tracking-widest">
            <li><Link href="/shop" className="hover:text-primary transition-colors">All Products</Link></li>
            <li><Link href="/collections" className="hover:text-primary transition-colors">Collections</Link></li>
            <li><Link href="/search" className="hover:text-primary transition-colors">Search</Link></li>
          </ul>
        </div>
        <div>
          <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-900 mb-5">Account</h4>
          <ul className="space-y-3 text-xs font-medium text-slate-500 uppercase tracking-widest">
            <li><Link href="/account" className="hover:text-primary transition-colors">My Account</Link></li>
            <li><Link href="/account/orders" className="hover:text-primary transition-colors">Orders</Link></li>
            <li><Link href="/account/addresses" className="hover:text-primary transition-colors">Addresses</Link></li>
          </ul>
        </div>
        <div>
          <h4 className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-900 mb-5">Newsletter</h4>
          <p className="text-xs text-slate-500 mb-4 tracking-widest">
            Get early drops and signature fragrance stories.
          </p>
          <div className="flex border-b border-slate-300 py-2">
            <input
              type="email"
              placeholder="Email Address"
              className="bg-transparent border-none focus:ring-0 text-xs w-full placeholder:text-slate-400 uppercase tracking-widest"
            />
            <button className="material-symbols-outlined text-slate-500 hover:text-primary transition-colors">
              arrow_forward
            </button>
          </div>
        </div>
      </div>
      <div className="max-w-7xl mx-auto px-6 pt-8 border-t border-slate-200 flex flex-col md:flex-row justify-between items-center gap-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">
        <p>© 2026 Anaya Candles. All Rights Reserved.</p>
        <div className="flex gap-8">
          <a href="#" className="hover:text-slate-600">Privacy Policy</a>
          <a href="#" className="hover:text-slate-600">Terms of Service</a>
        </div>
      </div>
    </footer>
  );
}
