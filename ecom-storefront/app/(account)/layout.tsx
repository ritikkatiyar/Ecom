import type { Metadata } from "next";
import { AccountGuard } from "@/components/guards/AccountGuard";

export const metadata: Metadata = {
  robots: {
    index: false,
    follow: false,
  },
};

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AccountGuard>
      <main className="max-w-7xl mx-auto px-6 py-8">{children}</main>
    </AccountGuard>
  );
}
