import { Route } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { InvestorAuthGuard } from './guards/investor-auth.guard';

export const appRoutes: Route[] = [
  // Investor Dashboard (separate auth flow)
  {
    path: 'investor-login',
    loadComponent: () =>
      import('./pages/investor-login/investor-login.component').then(
        (m) => m.InvestorLoginComponent
      ),
  },
  {
    path: 'investor-launch',
    canActivate: [InvestorAuthGuard],
    loadComponent: () =>
      import('./pages/investor-launch/investor-launch.component').then(
        (m) => m.InvestorLaunchComponent
      ),
  },
  // Main admin portal routes
  {
    path: '',
    canActivate: [AuthGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./pages/users/users.component').then((m) => m.UsersComponent),
      },
      {
        path: 'tenants',
        loadComponent: () =>
          import('./pages/tenants/tenants.component').then(
            (m) => m.TenantsComponent
          ),
      },
      {
        path: 'system-health',
        loadComponent: () =>
          import('./pages/system-health/system-health.component').then(
            (m) => m.SystemHealthComponent
          ),
      },
      {
        path: 'audit-logs',
        loadComponent: () =>
          import('./pages/audit-logs/audit-logs-enhanced.component').then(
            (m) => m.AuditLogsEnhancedComponent
          ),
      },
      {
        path: 'config-versions',
        loadComponent: () =>
          import('./pages/config-versions/config-versions.component').then(
            (m) => m.ConfigVersionsComponent
          ),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
