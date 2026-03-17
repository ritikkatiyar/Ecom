"use client";

import { usePathname } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useAuth } from "@/context/AuthContext";
import { useFlags } from "@/context/FlagsContext";

interface AdminGuardProps {
  children: ReactNode;
}

const ADMIN_ROLE = "ADMIN";

export function AdminGuard({ children }: AdminGuardProps) {
  const { isAuthenticated, roles, isLoading } = useAuth();
  const { adminConsoleEnabled } = useFlags();
  const pathname = usePathname();
  const isAdmin = roles.includes(ADMIN_ROLE);

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      const returnTo = pathname ? `?returnTo=${encodeURIComponent(pathname)}` : "";
      window.location.href = `/login${returnTo}`;
      return;
    }
    if (!isAdmin || !adminConsoleEnabled) {
      window.location.href = "/unauthorized";
    }
  }, [isAuthenticated, isAdmin, adminConsoleEnabled, isLoading, pathname]);

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#2badee] border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated || !isAdmin || !adminConsoleEnabled) {
    return null;
  }

  return <>{children}</>;
}
