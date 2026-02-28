"use client";

import { useAuth } from "@/context/AuthContext";

export default function AccountSecurityPage() {
  const { logout } = useAuth();

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8">
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-4">Security</h1>
      <p className="text-slate-600 mb-6">
        Backend endpoints for password change and session inventory are not exposed yet.
        Current action available is secure logout.
      </p>
      <button
        type="button"
        onClick={() => logout()}
        className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold uppercase tracking-widest text-red-700"
      >
        Logout Current Session
      </button>
    </div>
  );
}
