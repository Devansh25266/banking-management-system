import { Link } from 'react-router-dom';

const Box = ({ code, title, text }: { code: string; title: string; text: string }) => (
  <div className="card mx-auto max-w-md text-center">
    <p className="text-5xl font-bold text-teal-600">{code}</p>
    <h1 className="mt-2 text-xl font-semibold text-navy">{title}</h1>
    <p className="mt-2 text-sm text-slate-600">{text}</p>
    <Link to="/" className="btn-primary mt-5">Back to home</Link>
  </div>
);
export const AccessDenied = () => <Box code="403" title="Access denied" text="You don't have permission to view this page." />;
export const NotFound = () => <Box code="404" title="Page not found" text="The page you're looking for doesn't exist." />;
