import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, qs, useFetch, type Account, type Balance as BalanceDto, type Page, type Tx } from '../api';
import { Confirm, Empty, ErrorBox, Field, Pager, Spinner, StatusBadge, TxTable, dateTime, money, useToast } from '../ui';

export function CreateAccount() {
  const nav = useNavigate();
  const toast = useToast();
  const [type, setType] = useState<'SAVINGS' | 'CURRENT'>('SAVINGS');
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const create = async () => {
    setBusy(true);
    try {
      const a = await api<Account>('/accounts', { method: 'POST', body: { accountType: type } });
      toast('ok', `Account ${a.accountNumber} created.`);
      nav(`/accounts/${a.id}`);
    } catch (e) { toast('err', (e as Error).message); setOpen(false); } finally { setBusy(false); }
  };
  return (
    <div className="card mx-auto max-w-md space-y-4">
      <h1 className="text-2xl font-semibold text-navy">Open a new account</h1>
      <label className="block text-sm"><span className="mb-1 block font-medium">Account type</span>
        <select className="input" value={type} onChange={(e) => setType(e.target.value as 'SAVINGS' | 'CURRENT')}>
          <option value="SAVINGS">Savings</option><option value="CURRENT">Current</option>
        </select>
      </label>
      <p className="text-sm text-slate-500">Your account starts with a balance of ₹0.00 (INR).</p>
      <button className="btn-primary w-full" onClick={() => setOpen(true)}>Create account</button>
      <Confirm open={open} busy={busy} title="Create account?" body={`Open a new ${type.toLowerCase()} account?`} onYes={create} onNo={() => setOpen(false)} />
    </div>
  );
}

export function AccountDetails() {
  const { id } = useParams();
  const { data: a, loading, error } = useFetch<Account>(`/accounts/${id}`);
  if (loading) return <Spinner />;
  if (error || !a) return <ErrorBox text={error || 'Account not found.'} />;
  return (
    <div className="space-y-6">
      <div className="card">
        <div className="flex items-center justify-between"><h1 className="text-2xl font-semibold text-navy">{a.accountType} account</h1><StatusBadge status={a.status} /></div>
        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3">
          <div><dt className="text-slate-500">Account number</dt><dd className="font-mono">{a.accountNumber}</dd></div>
          <div><dt className="text-slate-500">Opened</dt><dd>{dateTime(a.createdAt)}</dd></div>
          <div><dt className="text-slate-500">Currency</dt><dd>{a.currency}</dd></div>
        </dl>
        <p className="mt-5 text-3xl font-bold text-navy">{money(a.balance, a.currency)}</p>
      </div>
      <div className="flex flex-wrap gap-2">
        <Link to={`/accounts/${a.id}/deposit`} className="btn-primary">Deposit</Link>
        <Link to={`/accounts/${a.id}/withdraw`} className="btn-ghost">Withdraw</Link>
        <Link to={`/accounts/${a.id}/balance`} className="btn-ghost">Balance inquiry</Link>
        <Link to={`/accounts/${a.id}/transactions`} className="btn-ghost">Transaction history</Link>
      </div>
    </div>
  );
}

export function Balance() {
  const { id } = useParams();
  const { data: b, loading, error, reload } = useFetch<BalanceDto>(`/accounts/${id}/balance`);
  if (loading) return <Spinner />;
  if (error || !b) return <ErrorBox text={error || 'Account not found.'} />;
  return (
    <div className="card mx-auto max-w-md text-center">
      <p className="text-sm text-slate-500">Available balance · <span className="font-mono">{b.accountNumber}</span></p>
      <p className="my-4 text-4xl font-bold text-navy">{money(b.balance, b.currency)}</p>
      <StatusBadge status={b.status} />
      <div className="mt-6 flex justify-center gap-2"><button className="btn-ghost" onClick={reload}>Refresh</button><Link to={`/accounts/${b.accountId}`} className="btn-ghost">Back</Link></div>
    </div>
  );
}

export function MoneyForm({ mode }: { mode: 'deposit' | 'withdraw' }) {
  const { id } = useParams();
  const nav = useNavigate();
  const toast = useToast();
  const { data: bal } = useFetch<BalanceDto>(`/accounts/${id}/balance`);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const deposit = mode === 'deposit';

  const submit = (ev: FormEvent) => {
    ev.preventDefault();
    if (!/^\d{1,15}(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) { setError('Enter a positive amount with at most 2 decimals.'); return; }
    setError('');
    if (deposit) void send(); else setConfirm(true);
  };
  const send = async () => {
    setBusy(true);
    try {
      const r = await api<{ balance: number }>(`/accounts/${id}/${mode}`, { method: 'POST', body: { amount, description: description.trim() || undefined } });
      toast('ok', `${deposit ? 'Deposit' : 'Withdrawal'} successful. New balance ${money(r.balance, bal?.currency)}.`);
      nav(`/accounts/${id}`);
    } catch (e) { setError((e as Error).message); setConfirm(false); } finally { setBusy(false); }
  };
  return (
    <form onSubmit={submit} className="card mx-auto max-w-md space-y-4" noValidate>
      <h1 className="text-2xl font-semibold text-navy">{deposit ? 'Deposit money' : 'Withdraw money'}</h1>
      {bal && <p className="text-sm text-slate-500">Current balance: <strong>{money(bal.balance, bal.currency)}</strong> · {bal.accountNumber}</p>}
      {error && <ErrorBox text={error} />}
      <Field label="Amount (INR)" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" />
      <Field label="Description (optional)" maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} />
      <button className="btn-primary w-full" disabled={busy}>{deposit ? 'Deposit' : 'Withdraw'}</button>
      <Confirm open={confirm} busy={busy} danger title="Confirm withdrawal" body={`Withdraw ${money(amount || 0)} from this account?`} onYes={send} onNo={() => setConfirm(false)} />
    </form>
  );
}

export function History() {
  const { id } = useParams();
  const [f, setF] = useState({ type: '', from: '', to: '', sort: 'desc', page: 0 });
  const set = (patch: Partial<typeof f>) => setF({ ...f, page: 0, ...patch });
  const { data, loading, error } = useFetch<Page<Tx>>(`/accounts/${id}/transactions${qs({ ...f, size: 10 })}`);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between"><h1 className="text-2xl font-semibold text-navy">Transaction history</h1><Link to={`/accounts/${id}`} className="btn-ghost">Back</Link></div>
      <div className="card grid gap-3 sm:grid-cols-4">
        <label className="text-sm">Type<select className="input" value={f.type} onChange={(e) => set({ type: e.target.value })}><option value="">All</option><option value="DEPOSIT">Deposits</option><option value="WITHDRAWAL">Withdrawals</option></select></label>
        <label className="text-sm">From<input type="date" className="input" value={f.from} onChange={(e) => set({ from: e.target.value })} /></label>
        <label className="text-sm">To<input type="date" className="input" value={f.to} onChange={(e) => set({ to: e.target.value })} /></label>
        <label className="text-sm">Order<select className="input" value={f.sort} onChange={(e) => set({ sort: e.target.value })}><option value="desc">Newest first</option><option value="asc">Oldest first</option></select></label>
      </div>
      <div className="card">
        {loading ? <Spinner /> : error ? <ErrorBox text={error} /> : data && data.content.length > 0 ? (<><TxTable rows={data.content} /><Pager page={data.page} totalPages={data.totalPages} onPage={(p) => setF({ ...f, page: p })} /></>) : <Empty text="No transactions match these filters." />}
      </div>
    </div>
  );
}
