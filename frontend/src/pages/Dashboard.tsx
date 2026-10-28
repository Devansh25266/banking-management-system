import { Link } from 'react-router-dom';
import { useFetch, type Account, type Page, type Tx } from '../api';
import { useAuth } from '../auth';
import { Empty, ErrorBox, Spinner, StatusBadge, TxTable, money } from '../ui';

export default function Dashboard() {
  const { user } = useAuth();
  const accounts = useFetch<Account[]>('/accounts');
  const first = accounts.data?.[0];
  const recent = useFetch<Page<Tx>>(first ? `/accounts/${first.id}/transactions?size=5` : null);

  if (accounts.loading) return <Spinner />;
  if (accounts.error) return <ErrorBox text={accounts.error} />;
  const list = accounts.data ?? [];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-navy">Hello, {user?.fullName}</h1>
        <Link to="/accounts/new" className="btn-primary">Open new account</Link>
      </div>
      {list.length === 0 ? (
        <Empty text="You don't have any accounts yet."><Link to="/accounts/new" className="btn-primary">Open your first account</Link></Empty>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {list.map((a) => (
              <div key={a.id} className="card transition hover:shadow-md">
                <div className="flex items-center justify-between text-sm text-slate-500"><span>{a.accountType}</span><StatusBadge status={a.status} /></div>
                <p className="mt-2 font-mono text-sm">{a.accountNumber}</p>
                <p className="mt-3 text-2xl font-bold text-navy">{money(a.balance, a.currency)}</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Link to={`/accounts/${a.id}/deposit`} className="btn-primary">Deposit</Link>
                  <Link to={`/accounts/${a.id}/withdraw`} className="btn-ghost">Withdraw</Link>
                  <Link to={`/accounts/${a.id}`} className="btn-ghost">Details</Link>
                </div>
              </div>
            ))}
          </div>
          <section className="card">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-semibold text-navy">Recent transactions · {first?.accountNumber}</h2>
              {first && <Link to={`/accounts/${first.id}/transactions`} className="text-sm text-teal-700 underline">View all</Link>}
            </div>
            {recent.loading ? <Spinner /> : recent.data && recent.data.content.length > 0 ? <TxTable rows={recent.data.content} /> : <Empty text="No transactions yet. Make a deposit to get started." />}
          </section>
        </>
      )}
    </div>
  );
}
