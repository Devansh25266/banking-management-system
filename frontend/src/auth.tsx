import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, type User } from './api';

type AuthState = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
};
const Ctx = createContext<AuthState>({ user: null, loading: true, login: async () => { throw new Error('no provider'); }, logout: async () => {} });

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Also sets the XSRF-TOKEN cookie that later POST/PATCH requests need.
  useEffect(() => {
    api<User>('/auth/me').then(setUser).catch(() => setUser(null)).finally(() => setLoading(false));
  }, []);

  const login = async (email: string, password: string) => {
    const u = await api<User>('/auth/login', { method: 'POST', body: { email, password } });
    setUser(u);
    return u;
  };
  const logout = async () => {
    try { await api('/auth/logout', { method: 'POST' }); } catch { /* session may already be gone */ }
    setUser(null);
  };
  return <Ctx.Provider value={{ user, loading, login, logout }}>{children}</Ctx.Provider>;
}
export const useAuth = () => useContext(Ctx);
