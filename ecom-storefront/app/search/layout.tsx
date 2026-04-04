import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "Search",
  description: "Search the Anaya Candles catalog by name, brand, and category.",
  path: "/search",
});

export default function SearchLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
