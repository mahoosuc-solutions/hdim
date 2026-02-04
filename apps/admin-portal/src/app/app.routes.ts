import { Route } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { InvestorAuthGuard } from './guards/investor-auth.guard';
import { salesAuthGuard } from './guards/sales-auth.guard';

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

  // Sales Automation Engine (separate auth flow)
  {
    path: 'sales/login',
    loadComponent: () =>
      import('./pages/sales/sales-login/sales-login.component').then(
        (m) => m.SalesLoginComponent
      ),
  },
  {
    path: 'sales',
    canActivate: [salesAuthGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/sales/sales-dashboard/sales-dashboard.component').then(
            (m) => m.SalesDashboardComponent
          ),
      },
      {
        path: 'leads',
        loadComponent: () =>
          import('./pages/sales/sales-leads/sales-leads.component').then(
            (m) => m.SalesLeadsComponent
          ),
      },
      {
        path: 'pipeline',
        loadComponent: () =>
          import('./pages/sales/sales-pipeline/sales-pipeline.component').then(
            (m) => m.SalesPipelineComponent
          ),
      },
      {
        path: 'activities',
        loadComponent: () =>
          import('./pages/sales/sales-activities/sales-activities.component').then(
            (m) => m.SalesActivitiesComponent
          ),
      },
      {
        path: 'opportunities',
        loadComponent: () =>
          import('./pages/sales/sales-pipeline/sales-pipeline.component').then(
            (m) => m.SalesPipelineComponent
          ),
      },
      {
        path: 'sequences',
        loadComponent: () =>
          import('./pages/sales/sales-sequences/sales-sequences.component').then(
            (m) => m.SalesSequencesComponent
          ),
      },
      {
        path: 'linkedin',
        loadComponent: () =>
          import('./pages/sales/sales-linkedin/sales-linkedin.component').then(
            (m) => m.SalesLinkedInComponent
          ),
      },
    ],
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
