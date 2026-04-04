import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "Cart",
  description: "Review the items in your shopping cart.",
  path: "/cart",
  noIndex: true,
});

export default function CartLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
