import { AdminSidebar } from "@/components/AdminSidebar";

export default function AdminAppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-[#f6f7f8]">
      <AdminSidebar />
      <main className="flex-1 ml-64 flex flex-col min-h-screen">
        <header className="h-16 border-b border-slate-200 bg-white/80 backdrop-blur-md sticky top-0 z-10 flex items-center justify-between px-8">
          <div className="flex items-center bg-slate-100 px-3 py-1.5 rounded-xl">
            <span className="material-symbols-outlined text-slate-400 text-xl">search</span>
            <input type="text" placeholder="Search..." className="bg-transparent border-none focus:ring-0 text-sm w-64 placeholder:text-slate-400" />
          </div>
        </header>
        <div className="flex-1 p-8">{children}</div>
      </main>
    </div>
  );
}
