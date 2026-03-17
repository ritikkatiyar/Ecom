"use client";

import Link from "next/link";
import { useFlags } from "@/context/FlagsContext";

export function BetaBanner() {
  const { betaBannerEnabled } = useFlags();

  if (!betaBannerEnabled) return null;

  return (
    <div
      role="banner"
      className="bg-[#2badee] text-white py-2.5 px-4 text-center text-sm font-medium"
    >
      <span className="uppercase tracking-widest">
        You&apos;re viewing our beta.{" "}
        <Link href="/feedback" className="underline hover:no-underline font-semibold">
          Share feedback
        </Link>
      </span>
    </div>
  );
}
