import { useState, type FormEvent } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { api, qs, useFetch, type AdminAccount, type AdminCustomer, type Audit, type Dashboard, type Page, type Tx } from '../api';
import { Confirm, Empty, ErrorBox, Pager, Spinner, StatusBadge, TxTable, dateTime, money, useToast } from '../ui';

export function AdminLayout() {
  const tab = ({ isActive }: { isActive: boolean }) => `rounded-lg px-4 py-2 text-sm ${isActive ? 'bg-navy text-white' : 'bg-white ring-1 ring-slate-300 hover:bg-slate-50'}`;
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-navy">Administrator console</h1>
      <nav className="flex flex-wrap gap-2">
        <NavLink end to="/admin" className={tab}>Overview</NavLink>
        <NavLink to="/admin/customers" className={tab}>Customers</NavLink>
        <NavLink to="/admin/accounts" className={tab}>Accounts</NavLink>
        <NavLink to="/admin/transactions" className={tab}>Transactions</NavLink>
        <NavLink to="/admin/audit" className={tab}>Audit logs</NavLink>
      </nav>
      <Outlet />
    </div>
  );
}

const Stat = ({ label, value }: { label: string; value: string | number }) => (
  <div className="card"><p className="text-sm text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-navy">{value}</p></div>
);

export function AdminOverview() {
  const [days, setDays] = useState(30);
  const { data: d, loading, error } = useFetch<Dashboard>(`/admin/dashboard?days=${days}`);
  return (
    <div className="space-y-4">
      <label className="text-sm">Period <select className="input ml-2 inline-block w-auto" value={days} onChange={(e) => setDays(Number(e.target.value))}><option value={7}>Last 7 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option></select></label>
      {loading ? <Spinner /> : error || !d ? <ErrorBox text={error || 'No data'} /> : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Stat label="Customers" value={d.totalCustomers} /><Stat label="Accounts" value={d.totalAccounts} />
            <Stat label="Active" value={d.activeAccounts} /><Stat label="Frozen / Closed" value={`${d.frozenAccounts} / ${d.closedAccounts}`} />
            <Stat label="Deposits (count)" value={d.depositCount} /><Stat label="Withdrawals (count)" value={d.withdrawalCount} />
            <Stat label="Deposit volume" value={money(d.depositVolume)} /><Stat label="Withdrawal volume" value={money(d.withdrawalVolume)} />
          </div>
          <div className="card"><p className="text-sm text-slate-500">Total simulated balances (demonstration metric)</p><p className="text-2xl font-bold text-navy">{money(d.demoTotalBalance)}</p><p className="mt-1 text-xs text-slate-500">{d.balanceNote}</p></div>
        </>
      )}
    </div>
  );
}

export function AdminAccounts() {
  const toast = useToast();
  const [q, setQ] = useState('');
  const [applied, setApplied] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [pending, setPending] = useState<{ a: AdminAccount; to: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newAccount, setNewAccount] = useState({ customerId: '', accountType: 'SAVINGS' });
  const { data: customerData, loading: customersLoading, error: customersError } = useFetch<Page<AdminCustomer>>('/admin/customers?status=ACTIVE&page=0&size=100');
  const { data, loading, error, reload } = useFetch<Page<AdminAccount>>(`/admin/accounts${qs({ search: applied, status, page, size: 10 })}`);

  const search = (ev: FormEvent) => { ev.preventDefault(); setPage(0); setApplied(q); };
  const createAccount = async (ev: FormEvent) => {
    ev.preventDefault();
    if (!newAccount.customerId) { toast('err', 'Select a customer first.'); return; }
    setBusy(true);
    try {
      const created = await api<AdminAccount>('/admin/accounts', { method: 'POST', body: { customerId: Number(newAccount.customerId), accountType: newAccount.accountType } });
      toast('ok', `Account ${created.accountNumber} created successfully with zero balance.`);
      setShowCreate(false); setNewAccount({ customerId: '', accountType: 'SAVINGS' }); reload();
    } catch (e) { toast('err', (e as Error).message); } finally { setBusy(false); }
  };
  const apply = async () => {
    if (!pending) return;
    setBusy(true);
    try {
      await api(`/admin/accounts/${pending.a.id}/status`, { method: 'PATCH', body: { status: pending.to } });
      toast('ok', `Account ${pending.a.accountNumber} is now ${pending.to.toLowerCase()}.`);
      reload();
    } catch (e) { toast('err', (e as Error).message); } finally { setBusy(false); setPending(null); }
  };
  const btn = (a: AdminAccount, to: string, label: string, danger = false) => (
    <button key={to} className={danger ? 'btn-danger px-3 py-1' : 'btn-ghost px-3 py-1'} onClick={() => setPending({ a, to })}>{label}</button>
  );
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-semibold text-navy">Bank accounts</h2><p className="text-sm text-slate-500">Open accounts for active customers and manage their status.</p></div><button className="btn-primary" onClick={() => setShowCreate(v => !v)}>{showCreate ? 'Cancel' : '+ Open new account'}</button></div>
      {showCreate && <form onSubmit={createAccount} className="card grid gap-3 sm:grid-cols-3 sm:items-end">
        <label className="text-sm">Customer<select className="input" required value={newAccount.customerId} onChange={e => setNewAccount({ ...newAccount, customerId: e.target.value })}><option value="">Select customer</option>{customerData?.content.map(c => <option key={c.id} value={c.id}>{c.fullName} — {c.email}</option>)}</select></label>
        <label className="text-sm">Account type<select className="input" value={newAccount.accountType} onChange={e => setNewAccount({ ...newAccount, accountType: e.target.value })}><option value="SAVINGS">Savings</option><option value="CURRENT">Current</option></select></label>
        <button className="btn-primary" disabled={busy || customersLoading || !customerData?.content.length}>{busy ? 'Creating…' : 'Create account'}</button>
        {customersLoading && <p className="text-sm text-slate-500">Loading customers…</p>}{customersError && <p className="text-sm text-red-600">{customersError}</p>}{!customersLoading && customerData?.content.length === 0 && <p className="text-sm text-amber-700">No active customers found. Add a customer first.</p>}
        <p className="text-xs text-slate-500 sm:col-span-3">New accounts start with a ₹0.00 balance. Use the deposit feature when it is available to add demo funds.</p>
      </form>}
      <form onSubmit={search} className="card flex flex-wrap gap-3">
        <input className="input min-w-[12rem] flex-1" placeholder="Search by account number, name or email" value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="input w-auto" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}><option value="">All statuses</option><option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option></select>
        <button className="btn-primary">Search</button>
      </form>
      <div className="card overflow-x-auto">
        {loading ? <Spinner /> : error ? <ErrorBox text={error} /> : data && data.content.length > 0 ? (
          <>
            <table className="w-full text-sm">
              <thead><tr className="border-b text-left text-slate-500"><th className="py-2">Account</th><th>Owner</th><th>Type</th><th className="text-right">Balance</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {data.content.map((a) => (
                  <tr key={a.id} className="border-b last:border-0">
                    <td className="py-2 font-mono text-xs">{a.accountNumber}</td>
                    <td>{a.ownerName}<br /><span className="text-xs text-slate-500">{a.ownerEmail}</span></td>
                    <td>{a.accountType}</td><td className="text-right">{money(a.balance, a.currency)}</td><td><StatusBadge status={a.status} /></td>
                    <td className="space-x-1 whitespace-nowrap">
                      {a.status === 'ACTIVE' && btn(a, 'FROZEN', 'Freeze')}
                      {a.status === 'FROZEN' && btn(a, 'ACTIVE', 'Unfreeze')}
                      {a.status !== 'CLOSED' && btn(a, 'CLOSED', 'Close', true)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </>
        ) : <Empty text="No accounts found." />}
      </div>
      <Confirm open={!!pending} busy={busy} danger={pending?.to === 'CLOSED'} title="Change account status"
        body={pending ? `Set account ${pending.a.accountNumber} to ${pending.to}? This action is recorded in the audit log.` : ''} onYes={apply} onNo={() => setPending(null)} />
    </div>
  );
}

export function AdminTransactions() {
  const [f, setF] = useState({ type: '', from: '', to: '', page: 0 });
  const set = (p: Partial<typeof f>) => setF({ ...f, page: 0, ...p });
  const { data, loading, error } = useFetch<Page<Tx>>(`/admin/transactions${qs({ ...f, size: 15 })}`);
  return (
    <div className="space-y-4">
      <div className="card grid gap-3 sm:grid-cols-3">
        <label className="text-sm">Type<select className="input" value={f.type} onChange={(e) => set({ type: e.target.value })}><option value="">All</option><option value="DEPOSIT">Deposits</option><option value="WITHDRAWAL">Withdrawals</option></select></label>
        <label className="text-sm">From<input type="date" className="input" value={f.from} onChange={(e) => set({ from: e.target.value })} /></label>
        <label className="text-sm">To<input type="date" className="input" value={f.to} onChange={(e) => set({ to: e.target.value })} /></label>
      </div>
      <div className="card">
        {loading ? <Spinner /> : error ? <ErrorBox text={error} /> : data && data.content.length > 0 ? (<><TxTable rows={data.content} showAccount /><Pager page={data.page} totalPages={data.totalPages} onPage={(p) => setF({ ...f, page: p })} /></>) : <Empty text="No transactions found." />}
      </div>
    </div>
  );
}

export function AdminAudit() {
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch<Page<Audit>>(`/admin/audit-logs${qs({ page, size: 15 })}`);
  return (
    <div className="card overflow-x-auto">
      {loading ? <Spinner /> : error ? <ErrorBox text={error} /> : data && data.content.length > 0 ? (
        <>
          <table className="w-full text-sm">
            <thead><tr className="border-b text-left text-slate-500"><th className="py-2">When</th><th>Administrator</th><th>Action</th><th>Entity</th></tr></thead>
            <tbody>{data.content.map((l) => (<tr key={l.id} className="border-b last:border-0"><td className="py-2">{dateTime(l.createdAt)}</td><td>{l.adminEmail}</td><td className="font-mono text-xs">{l.action}</td><td>{l.entityType} #{l.entityId}</td></tr>))}</tbody>
          </table>
          <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />
        </>
      ) : <Empty text="No administrator actions recorded yet." />}
    </div>
  );
}


export function AdminCustomers() {
  const toast = useToast();
  const [q, setQ] = useState('');
  const [applied, setApplied] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [f, setF] = useState({ fullName: '', email: '', phone: '', address: '', password: '', confirmPassword: '' });
  const { data, loading, error, reload } = useFetch<Page<AdminCustomer>>(`/admin/customers${qs({ search: applied, status, page, size: 10 })}`);
  const search = (ev: FormEvent) => { ev.preventDefault(); setPage(0); setApplied(q); };
  const create = async (ev: FormEvent) => {
    ev.preventDefault();
    if (f.password !== f.confirmPassword) { toast('err', 'Passwords do not match.'); return; }
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(f.password)) { toast('err', 'Password must be 8–72 characters and contain a letter and a digit.'); return; }
    setBusy(true);
    try {
      await api('/admin/customers', { method: 'POST', body: f });
      toast('ok', 'Customer created successfully.');
      setF({ fullName: '', email: '', phone: '', address: '', password: '', confirmPassword: '' });
      setShowForm(false); setPage(0); setApplied(''); setQ(''); reload();
    } catch (e) { toast('err', (e as Error).message); } finally { setBusy(false); }
  };
  const changeStatus = async (c: AdminCustomer) => {
    const next = c.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    if (!window.confirm(`${next === 'DISABLED' ? 'Disable' : 'Reactivate'} ${c.fullName}?`)) return;
    try {
      await api(`/admin/customers/${c.id}/status`, { method: 'PATCH', body: { status: next } });
      toast('ok', `Customer ${next.toLowerCase()}.`); reload();
    } catch (e) { toast('err', (e as Error).message); }
  };
  return <div className="space-y-4">
    <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-xl font-semibold text-navy">Customer management</h2><p className="text-sm text-slate-500">Create customers and manage access. Financial records are preserved.</p></div><button className="btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Cancel' : '+ Add customer'}</button></div>
    {showForm && <form onSubmit={create} className="card grid gap-3 sm:grid-cols-2">
      <label className="text-sm">Full name<input required maxLength={100} className="input mt-1" value={f.fullName} onChange={e=>setF({...f,fullName:e.target.value})}/></label>
      <label className="text-sm">Email<input required type="email" maxLength={150} className="input mt-1" value={f.email} onChange={e=>setF({...f,email:e.target.value})}/></label>
      <label className="text-sm">Phone<input required pattern="\+?[0-9]{10,15}" title="10–15 digits, optional leading +" className="input mt-1" value={f.phone} onChange={e=>setF({...f,phone:e.target.value})}/></label>
      <label className="text-sm">Address<input required maxLength={255} className="input mt-1" value={f.address} onChange={e=>setF({...f,address:e.target.value})}/></label>
      <label className="text-sm">Temporary password<input required type="password" minLength={8} maxLength={72} className="input mt-1" value={f.password} onChange={e=>setF({...f,password:e.target.value})}/></label>
      <label className="text-sm">Confirm password<input required type="password" minLength={8} maxLength={72} className="input mt-1" value={f.confirmPassword} onChange={e=>setF({...f,confirmPassword:e.target.value})}/></label>
      <p className="text-xs text-slate-500 sm:col-span-2">The customer will use this password to sign in. It is stored as a password hash, not plain text.</p>
      <div className="sm:col-span-2"><button className="btn-primary" disabled={busy}>{busy ? 'Creating…' : 'Create customer'}</button></div>
    </form>}
    <form onSubmit={search} className="card flex flex-wrap gap-3"><input className="input min-w-[12rem] flex-1" placeholder="Search name, email or phone" value={q} onChange={e=>setQ(e.target.value)}/><select className="input w-auto" value={status} onChange={e=>{setPage(0);setStatus(e.target.value)}}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="DISABLED">Disabled</option></select><button className="btn-primary">Search</button></form>
    <div className="card overflow-x-auto">{loading ? <Spinner/> : error ? <ErrorBox text={error}/> : data && data.content.length ? <><table className="w-full text-sm"><thead><tr className="border-b text-left text-slate-500"><th className="py-2 pr-3">Customer</th><th className="pr-3">Phone</th><th className="pr-3">Address</th><th className="pr-3">Joined</th><th className="pr-3">Status</th><th>Actions</th></tr></thead><tbody>{data.content.map(c=><tr key={c.id} className="border-b last:border-0"><td className="py-3 pr-3"><div className="font-medium">{c.fullName}</div><div className="text-xs text-slate-500">{c.email}</div></td><td className="pr-3">{c.phone}</td><td className="max-w-48 pr-3">{c.address}</td><td className="whitespace-nowrap pr-3">{dateTime(c.createdAt)}</td><td className="pr-3"><StatusBadge status={c.status}/></td><td><button className={c.status==='ACTIVE'?'btn-danger px-3 py-1':'btn-ghost px-3 py-1'} onClick={()=>changeStatus(c)}>{c.status==='ACTIVE'?'Disable':'Reactivate'}</button></td></tr>)}</tbody></table><Pager page={data.page} totalPages={data.totalPages} onPage={setPage}/></> : <Empty text="No customers found.">Use Add customer to create your first customer.</Empty>}</div>
  </div>;
}
