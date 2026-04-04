import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "Sign Up",
  description: "Create your Anaya Candles account.",
  path: "/signup",
  noIndex: true,
});

export default function SignupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
