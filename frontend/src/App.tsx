import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import { Shell, Spinner } from './ui';
import Landing from './pages/Landing';
import { Login, Register } from './pages/Auth';
import Dashboard from './pages/Dashboard';
import { AccountDetails, Balance, CreateAccount, History, MoneyForm } from './pages/Accounts';
import Profile from './pages/Profile';
import { AdminAccounts, AdminAudit, AdminCustomers, AdminLayout, AdminOverview, AdminTransactions } from './pages/Admin';
import { AccessDenied, NotFound } from './pages/Errors';

function Protected({ role, children }: { role: 'CUSTOMER' | 'ADMIN'; children: JSX.Element }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!user) return <Navigate to={role === 'ADMIN' ? '/admin/login' : '/login'} replace />;
  if (user.role !== role) return <Navigate to="/access-denied" replace />;
  return children;
}

const Layout = () => <Shell><Outlet /></Shell>;
const C = ({ children }: { children: JSX.Element }) => <Protected role="CUSTOMER">{children}</Protected>;

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Landing />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/admin/login" element={<Login admin />} />
        <Route path="/dashboard" element={<C><Dashboard /></C>} />
        <Route path="/accounts/new" element={<C><CreateAccount /></C>} />
        <Route path="/accounts/:id" element={<C><AccountDetails /></C>} />
        <Route path="/accounts/:id/balance" element={<C><Balance /></C>} />
        <Route path="/accounts/:id/deposit" element={<C><MoneyForm mode="deposit" /></C>} />
        <Route path="/accounts/:id/withdraw" element={<C><MoneyForm mode="withdraw" /></C>} />
        <Route path="/accounts/:id/transactions" element={<C><History /></C>} />
        <Route path="/profile" element={<C><Profile /></C>} />
        <Route path="/admin" element={<Protected role="ADMIN"><AdminLayout /></Protected>}>
          <Route index element={<AdminOverview />} />
          <Route path="customers" element={<AdminCustomers />} />
          <Route path="accounts" element={<AdminAccounts />} />
          <Route path="transactions" element={<AdminTransactions />} />
          <Route path="audit" element={<AdminAudit />} />
        </Route>
        <Route path="/access-denied" element={<AccessDenied />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
