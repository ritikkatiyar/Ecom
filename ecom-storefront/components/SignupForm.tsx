"use client";

import Link from "next/link";
import { useRef, useState } from "react";

interface SignupFormProps {
  onSubmit: (email: string, password: string) => Promise<void>;
}

export function SignupForm({ onSubmit }: SignupFormProps) {
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = formRef.current;
    if (!form) return;
    const fd = new FormData(form);
    const email = (fd.get("email") as string) ?? "";
    const password = (fd.get("password") as string) ?? "";
    const confirmPassword = (fd.get("confirmPassword") as string) ?? "";
    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    setSubmitting(true);
    try {
      await onSubmit(email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Sign up failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-6 pt-24 pb-16">
      <h1 className="font-display text-3xl font-bold text-slate-900 mb-2">
        Create Account
      </h1>
      <p className="text-slate-600 mb-8">
        Register with your email. Password must be at least 8 characters.
      </p>

      <form ref={formRef} onSubmit={handleSubmit} className="space-y-6">
        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="signup-email"
            className="block text-sm font-medium text-slate-700 mb-1"
          >
            Email
          </label>
          <input
            id="signup-email"
            name="email"
            type="email"
            autoComplete="email"
            required
            defaultValue=""
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20 bg-white"
          />
        </div>

        <div>
          <label
            htmlFor="signup-password"
            className="block text-sm font-medium text-slate-700 mb-1"
          >
            Password
          </label>
          <input
            id="signup-password"
            name="password"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            defaultValue=""
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20 bg-white"
          />
        </div>

        <div>
          <label
            htmlFor="signup-confirmPassword"
            className="block text-sm font-medium text-slate-700 mb-1"
          >
            Confirm Password
          </label>
          <input
            id="signup-confirmPassword"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            defaultValue=""
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20 bg-white"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-[#2badee] py-3 font-semibold text-white hover:bg-[#2badee]/90 disabled:opacity-50 transition-colors"
        >
          {submitting ? "Creating account…" : "Create Account"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-[#2badee] hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
