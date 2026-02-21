"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { SignupForm } from "@/components/SignupForm";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/apiClient";

export default function SignupPage() {
  const { signup, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated && !isLoading) {
      router.replace("/");
    }
  }, [isAuthenticated, isLoading, router]);

  if (isAuthenticated && !isLoading) {
    return null;
  }

  async function handleSignup(email: string, password: string) {
    await signup(email, password);
    router.replace("/");
  }

  return <SignupForm onSubmit={handleSignup} />;
}
