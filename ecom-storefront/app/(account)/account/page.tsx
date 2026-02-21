export default function AccountPage() {
  return (
    <>
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
        Account
      </h1>
      <p className="text-slate-600">
        Account dashboard. Protected by AccountGuard (USER or ADMIN).
      </p>
    </>
  );
}
