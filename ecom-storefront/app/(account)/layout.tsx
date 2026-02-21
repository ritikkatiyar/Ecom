import { AccountGuard } from "@/components/guards/AccountGuard";

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
