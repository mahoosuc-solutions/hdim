import { NavLink, Outlet } from 'react-router-dom';
import { BarChart3, Users, Mail, FileText, Building2 } from 'lucide-react';

const nav = [
  { to: '/', icon: BarChart3, label: 'Dashboard' },
  { to: '/contacts', icon: Users, label: 'Contacts' },
  { to: '/compose', icon: Mail, label: 'Compose' },
  { to: '/templates', icon: FileText, label: 'Templates' },
  { to: '/partnerships', icon: Building2, label: 'Partnerships' },
];

export default function Layout() {
  return (
    <div className="min-h-screen">
      <nav className="border-b border-border bg-card">
        <div className="mx-auto flex items-center justify-between px-6 py-3">
          <div className="flex items-center gap-2 font-semibold text-lg">
            <BarChart3 className="h-5 w-5 text-primary" />
            HDIM Investor Dashboard
          </div>
          <div className="flex gap-1">
            {nav.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `flex items-center gap-1.5 px-3 py-2 rounded-md text-sm transition-colors ${
                    isActive
                      ? 'bg-primary/10 text-primary font-medium'
                      : 'text-muted-foreground hover:text-foreground hover:bg-accent'
                  }`
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </div>
        </div>
      </nav>
      <main className="mx-auto max-w-[1400px] px-6 py-6">
        <Outlet />
      </main>
    </div>
  );
}
