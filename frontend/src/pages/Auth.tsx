import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { ErrorBox, Field, useToast } from '../ui';

export function Register() {
  const nav = useNavigate();
  const toast = useToast();
  const [f, setF] = useState({ fullName: '', email: '', phone: '', address: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (e: ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value });

  const validate = () => {
    const e: Record<string, string> = {};
    if (!f.fullName.trim()) e.fullName = 'Full name is required';
    if (!/^\S+@\S+\.\S+$/.test(f.email)) e.email = 'Enter a valid email address';
    if (!/^\+?[0-9]{10,15}$/.test(f.phone)) e.phone = 'Phone must be 10-15 digits';
    if (!f.address.trim()) e.address = 'Address is required';
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(f.password)) e.password = '8-72 characters with a letter and a digit';
    if (f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match';
    setErrors(e);
    return Object.keys(e).length === 0;
  };
  const submit = async (ev: FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setBusy(true);
    try {
      await api('/auth/register', { method: 'POST', body: f });
      toast('ok', 'Registration successful. Please log in.');
      nav('/login');
    } catch (e) { toast('err', (e as Error).message); } finally { setBusy(false); }
  };
  return (
    <form onSubmit={submit} className="card mx-auto max-w-lg space-y-4" noValidate>
      <h1 className="text-2xl font-semibold text-navy">Create your account</h1>
      <Field label="Full name" value={f.fullName} onChange={set('fullName')} error={errors.fullName} autoComplete="name" />
      <Field label="Email" type="email" value={f.email} onChange={set('email')} error={errors.email} autoComplete="email" />
      <Field label="Phone" value={f.phone} onChange={set('phone')} error={errors.phone} autoComplete="tel" />
      <Field label="Address" value={f.address} onChange={set('address')} error={errors.address} autoComplete="street-address" />
      <Field label="Password" type="password" value={f.password} onChange={set('password')} error={errors.password} autoComplete="new-password" />
      <Field label="Confirm password" type="password" value={f.confirmPassword} onChange={set('confirmPassword')} error={errors.confirmPassword} autoComplete="new-password" />
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating…' : 'Register'}</button>
      <p className="text-center text-sm text-slate-500">Already registered? <Link to="/login" className="text-teal-700 underline">Log in</Link></p>
    </form>
  );
}

export function Login({ admin = false }: { admin?: boolean }) {
  const { login, logout } = useAuth();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(params.get('expired') ? 'Your session expired. Please log in again.' : '');
  const [busy, setBusy] = useState(false);

  const submit = async (ev: FormEvent) => {
    ev.preventDefault();
    if (!email.trim() || !password) { setError('Enter your email and password.'); return; }
    setBusy(true);
    setError('');
    try {
      const u = await login(email, password);
      if (admin && u.role !== 'ADMIN') { await logout(); setError('This account is not an administrator account.'); return; }
      nav(u.role === 'ADMIN' ? '/admin' : '/dashboard');
    } catch (e) { setError((e as Error).message); } finally { setBusy(false); }
  };
  return (
    <form onSubmit={submit} className="card mx-auto max-w-md space-y-4" noValidate>
      <h1 className="text-2xl font-semibold text-navy">{admin ? 'Administrator login' : 'Welcome back'}</h1>
      {error && <ErrorBox text={error} />}
      <Field label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" />
      <Field label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Log in'}</button>
      {!admin && <p className="text-center text-sm text-slate-500">New here? <Link to="/register" className="text-teal-700 underline">Create an account</Link></p>}
    </form>
  );
}
