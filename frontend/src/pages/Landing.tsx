import { Link } from 'react-router-dom';

const features = [
  ['Open accounts', 'Create Savings or Current accounts in seconds.'],
  ['Deposit & withdraw', 'Every operation is atomic and recorded in your ledger.'],
  ['Full history', 'Filter by date and type, sort, and page through transactions.'],
  ['Secure by design', 'BCrypt passwords, server sessions, CSRF protection and ownership checks.'],
];

export default function Landing() {
  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-gradient-to-br from-navy to-navy-800 px-6 py-14 text-center text-white md:px-12">
        <h1 className="text-3xl font-bold md:text-5xl">Simple, secure <span className="text-teal-400">banking</span> simulation</h1>
        <p className="mx-auto mt-4 max-w-2xl text-slate-300">A Java, Spring Boot and MySQL demonstration project. Register, open an account, move simulated money and review your transaction history.</p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Link to="/register" className="btn-primary px-6 py-3">Get started</Link>
          <Link to="/login" className="btn bg-white/10 px-6 py-3 text-white hover:bg-white/20">Log in</Link>
        </div>
      </section>
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {features.map(([t, d]) => (
          <div key={t} className="card transition hover:-translate-y-0.5 hover:shadow-md">
            <h3 className="font-semibold text-navy">{t}</h3>
            <p className="mt-2 text-sm text-slate-600">{d}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
