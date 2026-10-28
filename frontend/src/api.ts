import { useEffect, useState } from 'react';

export const BASE: string = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api';

export type User = { id: number; fullName: string; email: string; phone: string; address: string; role: 'CUSTOMER' | 'ADMIN' };
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type Account = { id: number; accountNumber: string; accountType: string; balance: number; currency: string; status: AccountStatus; createdAt: string };
export type AdminAccount = Account & { ownerName: string; ownerEmail: string };
export type AdminCustomer = { id: number; fullName: string; email: string; phone: string; address: string; status: 'ACTIVE' | 'DISABLED'; createdAt: string };
export type Balance = { accountId: number; accountNumber: string; balance: number; currency: string; status: string };
export type Tx = { id: number; referenceNumber: string; type: 'DEPOSIT' | 'WITHDRAWAL'; amount: number; balanceBefore: number; balanceAfter: number; status: string; description: string | null; createdAt: string; accountNumber: string };
export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };
export type Audit = { id: number; adminUserId: number; adminEmail: string; action: string; entityType: string; entityId: number; createdAt: string };
export type Dashboard = { periodDays: number; totalCustomers: number; totalAccounts: number; activeAccounts: number; frozenAccounts: number; closedAccounts: number; depositCount: number; withdrawalCount: number; depositVolume: number; withdrawalVolume: number; demoTotalBalance: number; balanceNote: string };

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

function csrfToken(): string {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : '';
}

export async function api<T = unknown>(path: string, opts: { method?: string; body?: unknown } = {}): Promise<T> {
  const method = opts.method ?? 'GET';
  const headers: Record<string, string> = {};
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrfToken();
  const res = await fetch(`${BASE}${path}`, {
    method, headers, credentials: 'include',
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });
  const text = await res.text();
  let data: { message?: string } | null = null;
  if (text) { try { data = JSON.parse(text); } catch { data = null; } }
  if (!res.ok) {
    if (res.status === 401 && !path.startsWith('/auth/')) window.location.assign('/login?expired=1');
    const fallback = res.status === 403 ? 'Not allowed, or your security token expired. Refresh the page and try again.' : 'Request failed.';
    throw new ApiError(res.status, data?.message ?? fallback);
  }
  return data as T;
}

export const qs = (o: Record<string, string | number | null | undefined>): string => {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(o)) if (v !== undefined && v !== null && v !== '') p.set(k, String(v));
  const s = p.toString();
  return s ? `?${s}` : '';
};

/** Fetches `path` whenever it changes; pass null to skip. */
export function useFetch<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(path !== null);
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (path === null) { setLoading(false); return; }
    let live = true;
    setLoading(true);
    api<T>(path)
      .then((d) => { if (live) { setData(d); setError(''); } })
      .catch((e: Error) => { if (live) setError(e.message); })
      .finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [path, tick]);
  return { data, error, loading, reload: () => setTick((t) => t + 1) };
}
