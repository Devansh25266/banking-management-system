import { createContext, useCallback, useContext, useState, type InputHTMLAttributes, type ReactNode } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import type { Tx } from './api';

type Kind = 'ok' | 'err';
const ToastCtx = createContext<(k: Kind, text: string) => void>(() => {});
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<{ id: number; kind: Kind; text: string }[]>([]);
  const push = useCallback((kind: Kind, text: string) => {
    const id = Date.now() + Math.random();
    setItems((i) => [...i, { id, kind, text }]);
    setTimeout(() => setItems((i) => i.filter((t) => t.id !== id)), 4500);
  }, []);
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="fixed right-4 top-4 z-50 w-72 space-y-2" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} role="status" className={`animate-fade rounded-lg px-4 py-3 text-sm text-white shadow-lg ${t.kind === 'ok' ? 'bg-teal-600' : 'bg-red-600'}`}>{t.text}</div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

export const money = (n: number | string, cur = 'INR') => new Intl.NumberFormat('en-IN', { style: 'currency', currency: cur }).format(Number(n));
export const dateTime = (s: string) => new Date(s).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });

export const Spinner = () => (
  <div className="flex justify-center py-10" role="status" aria-label="Loading">
    <div className="h-8 w-8 animate-spin rounded-full border-4 border-teal-500 border-t-transparent" />
  </div>
);
export const Empty = ({ text, children }: { text: string; children?: ReactNode }) => (
  <div className="rounded-xl border border-dashed border-slate-300 py-10 text-center text-slate-500">
    <p>{text}</p>
    {children && <div className="mt-4">{children}</div>}
  </div>
);
export const ErrorBox = ({ text }: { text: string }) => <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{text}</div>;

export function Field({ label, error, ...props }: { label: string; error?: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{label}</span>
      <input className={`input ${error ? 'border-red-400' : ''}`} {...props} />
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const c = status === 'ACTIVE' ? 'bg-teal-100 text-teal-800' : status === 'FROZEN' ? 'bg-amber-100 text-amber-800' : 'bg-slate-200 text-slate-700';
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${c}`}>{status}</span>;
}

export function Confirm(p: { open: boolean; title: string; body: string; busy?: boolean; danger?: boolean; onYes: () => void; onNo: () => void }) {
  if (!p.open) return null;
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" aria-label={p.title}>
      <div className="card w-full max-w-sm animate-fade">
        <h3 className="text-lg font-semibold text-navy">{p.title}</h3>
        <p className="mt-2 text-sm text-slate-600">{p.body}</p>
        <div className="mt-5 flex justify-end gap-2">
          <button className="btn-ghost" onClick={p.onNo} disabled={p.busy}>Cancel</button>
          <button className={p.danger ? 'btn-danger' : 'btn-primary'} onClick={p.onYes} disabled={p.busy}>{p.busy ? 'Working…' : 'Confirm'}</button>
        </div>
      </div>
    </div>
  );
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <button className="btn-ghost" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span className="text-slate-500">Page {page + 1} of {totalPages}</span>
      <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  );
}

export function TxTable({ rows, showAccount = false }: { rows: Tx[]; showAccount?: boolean }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left text-slate-500">
            <th className="py-2 pr-3">Date</th><th className="pr-3">Reference</th>
            {showAccount && <th className="pr-3">Account</th>}
            <th className="pr-3">Type</th><th className="pr-3 text-right">Amount</th><th className="text-right">Balance after</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((t) => (
            <tr key={t.id} className="border-b last:border-0">
              <td className="whitespace-nowrap py-2 pr-3">{dateTime(t.createdAt)}</td>
              <td className="pr-3 font-mono text-xs">{t.referenceNumber}</td>
              {showAccount && <td className="pr-3 font-mono text-xs">{t.accountNumber}</td>}
              <td className="pr-3">{t.type === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'}</td>
              <td className={`pr-3 text-right font-medium ${t.type === 'DEPOSIT' ? 'text-teal-700' : 'text-red-600'}`}>{t.type === 'DEPOSIT' ? '+' : '−'}{money(t.amount)}</td>
              <td className="text-right">{money(t.balanceAfter)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Shell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const out = async () => { await logout(); nav('/'); };
  const link = ({ isActive }: { isActive: boolean }) => `rounded-md px-3 py-2 text-sm ${isActive ? 'bg-white/15' : 'hover:bg-white/10'}`;
  return (
    <div className="flex min-h-screen flex-col">
      <header className="bg-navy text-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-3">
          <Link to="/" className="text-lg font-semibold tracking-tight">Banking<span className="text-teal-400"> System</span> <span className="text-xs font-normal text-slate-300">BY Devansh</span></Link>
          <nav className="flex flex-wrap items-center gap-1">
            {!user && (<>
              <NavLink to="/login" className={link}>Login</NavLink>
              <NavLink to="/register" className={link}>Register</NavLink>
              <NavLink to="/admin/login" className={link}>Admin</NavLink>
            </>)}
            {user?.role === 'CUSTOMER' && (<>
              <NavLink to="/dashboard" className={link}>Dashboard</NavLink>
              <NavLink to="/profile" className={link}>Profile</NavLink>
            </>)}
            {user?.role === 'ADMIN' && <NavLink to="/admin" className={link}>Admin console</NavLink>}
            {user && <button onClick={out} className="rounded-md px-3 py-2 text-sm hover:bg-white/10">Logout</button>}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 animate-fade px-4 py-8">{children}</main>
      <footer className="py-6 text-center text-xs text-slate-500">Educational banking simulation. Not a real bank; no real money is involved.</footer>
    </div>
  );
}
