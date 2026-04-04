import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "Feedback",
  description: "Share product and shopping feedback with the Anaya Candles team.",
  path: "/feedback",
  noIndex: true,
});

export default function FeedbackLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
