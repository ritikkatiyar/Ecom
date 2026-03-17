"use client";

import Link from "next/link";
import { useState } from "react";

export default function FeedbackPage() {
  const [status, setStatus] = useState<"idle" | "submitted">("idle");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!message.trim()) return;

    // Use mailto for now - no backend feedback API yet. Opens user's email client.
    const subject = encodeURIComponent(`[Beta Feedback] from ${name || "Anonymous"}`);
    const body = encodeURIComponent(
      `${message}\n\n---\nName: ${name || "N/A"}\nEmail: ${email || "N/A"}`
    );
    const to = process.env.NEXT_PUBLIC_FEEDBACK_EMAIL ?? "feedback@example.com";
    const mailto = `mailto:${to}?subject=${subject}&body=${body}`;

    window.location.href = mailto;
    setStatus("submitted");
  }

  return (
    <div className="min-h-screen bg-[#F8F6F3] flex items-center justify-center px-6 py-12">
      <div className="max-w-md w-full">
        <h1 className="font-display text-3xl font-bold text-slate-900 mb-2 text-center">
          Beta Feedback
        </h1>
        <p className="text-slate-600 mb-8 text-center">
          Help us improve. Share what you like, what&apos;s missing, or what we could do better.
        </p>

        {status === "submitted" ? (
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center">
            <p className="text-slate-700 mb-6">
              Thank you! Your email client should open. Send the draft to share your feedback.
            </p>
            <Link
              href="/"
              className="inline-flex rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold text-white hover:bg-[#2badee]/90"
            >
              Back to Home
            </Link>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-slate-200 bg-white p-6 space-y-4"
          >
            <div>
              <label htmlFor="feedback-name" className="block text-sm font-medium text-slate-700 mb-1">
                Name (optional)
              </label>
              <input
                id="feedback-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-lg border border-slate-200 px-4 py-2 text-slate-900 placeholder:text-slate-400"
                placeholder="Your name"
              />
            </div>
            <div>
              <label htmlFor="feedback-email" className="block text-sm font-medium text-slate-700 mb-1">
                Email (optional)
              </label>
              <input
                id="feedback-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-lg border border-slate-200 px-4 py-2 text-slate-900 placeholder:text-slate-400"
                placeholder="you@example.com"
              />
            </div>
            <div>
              <label htmlFor="feedback-message" className="block text-sm font-medium text-slate-700 mb-1">
                Message *
              </label>
              <textarea
                id="feedback-message"
                required
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="w-full rounded-lg border border-slate-200 px-4 py-2 text-slate-900 placeholder:text-slate-400 resize-none"
                placeholder="Your feedback..."
              />
            </div>
            <button
              type="submit"
              className="w-full rounded-lg bg-[#2badee] px-6 py-3 text-sm font-semibold text-white hover:bg-[#2badee]/90 disabled:opacity-50"
            >
              Share Feedback
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
