"use client";

import { usePathname } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useAuth } from "@/context/AuthContext";

interface AdminGuardProps {
  children: ReactNode;
}

const ADMIN_ROLE = "ADMIN";

export function AdminGuard({ children }: AdminGuardProps) {
  const { isAuthenticated, roles, isLoading } = useAuth();
  const pathname = usePathname();
  const isAdmin = roles.includes(ADMIN_ROLE);

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      const returnTo = pathname ? `?returnTo=${encodeURIComponent(pathname)}` : "";
      window.location.href = `/login${returnTo}`;
      return;
    }
    if (!isAdmin) {
      window.location.href = "/unauthorized";
    }
  }, [isAuthenticated, isAdmin, isLoading, pathname]);

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#2badee] border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated || !isAdmin) {
    return null;
  }

  return <>{children}</>;
}
