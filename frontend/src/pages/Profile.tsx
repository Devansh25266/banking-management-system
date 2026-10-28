import { useAuth } from '../auth';

export default function Profile() {
  const { user } = useAuth();
  if (!user) return null;
  const rows: [string, string][] = [['Full name', user.fullName], ['Email', user.email], ['Phone', user.phone], ['Address', user.address], ['Role', user.role]];
  return (
    <div className="card mx-auto max-w-lg">
      <h1 className="mb-4 text-2xl font-semibold text-navy">Your profile</h1>
      <dl className="divide-y text-sm">
        {rows.map(([k, v]) => (<div key={k} className="flex justify-between gap-4 py-3"><dt className="text-slate-500">{k}</dt><dd className="text-right font-medium">{v}</dd></div>))}
      </dl>
    </div>
  );
}
