import Link from "next/link";

export default function UnauthorizedPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 px-6">
      <h1 className="font-display text-3xl font-bold text-slate-900">
        Unauthorized
      </h1>
      <p className="text-slate-600 text-center max-w-md">
        You do not have permission to access this page.
      </p>
      <Link
        href="/"
        className="px-6 py-3 bg-[#2badee] hover:bg-[#2badee]/90 text-white font-medium rounded-lg transition-colors"
      >
        Return to Home
      </Link>
    </div>
  );
}
