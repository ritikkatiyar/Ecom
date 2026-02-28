"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { LoginForm } from "@/components/LoginForm";
import { useAuth } from "@/context/AuthContext";

function LoginPageContent() {
  const { login, isAuthenticated, isLoading, roles } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnTo = searchParams.get("returnTo") ?? "/";
  const isAdmin = roles.includes("ADMIN");

  useEffect(() => {
    if (isAuthenticated && !isLoading) {
      router.replace(isAdmin ? "/admin/dashboard" : returnTo);
    }
  }, [isAuthenticated, isLoading, isAdmin, returnTo, router]);

  if (isAuthenticated && !isLoading) {
    return null;
  }

  async function handleLogin(email: string, password: string) {
    const userRoles = await login(email, password);
    router.replace(userRoles.includes("ADMIN") ? "/admin/dashboard" : returnTo);
  }

  return <LoginForm onSubmit={handleLogin} />;
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginPageContent />
    </Suspense>
  );
}
