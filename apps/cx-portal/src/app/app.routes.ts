import { Routes } from '@angular/router';

export const appRoutes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'pipeline',
    loadComponent: () =>
      import('./pipeline/pipeline.component').then((m) => m.PipelineComponent),
  },
  {
    path: 'investors',
    loadComponent: () =>
      import('./investors/investors.component').then((m) => m.InvestorsComponent),
  },
  {
    path: 'customers',
    loadComponent: () =>
      import('./customers/customers.component').then((m) => m.CustomersComponent),
  },
  {
    path: 'agents',
    loadComponent: () =>
      import('./agents/agents.component').then((m) => m.AgentsComponent),
  },
  {
    path: 'approvals',
    loadComponent: () =>
      import('./approvals/approval-queue.component').then(
        (m) => m.ApprovalQueueComponent
      ),
  },
  {
    path: 'approvals/history',
    loadComponent: () =>
      import('./approvals/approval-history.component').then(
        (m) => m.ApprovalHistoryComponent
      ),
  },
  {
    path: 'admin/access',
    loadComponent: () =>
      import('./admin/access-admin.component').then((m) => m.AccessAdminComponent),
  },
  {
    path: 'campaigns',
    loadComponent: () =>
      import('./campaigns/campaigns-dashboard.component').then(
        (m) => m.CampaignsDashboardComponent
      ),
  },
  {
    path: 'campaigns/new',
    loadComponent: () =>
      import('./campaigns/campaign-wizard.component').then(
        (m) => m.CampaignWizardComponent
      ),
  },
  {
    path: 'campaigns/:id',
    loadComponent: () =>
      import('./campaigns/campaign-detail.component').then(
        (m) => m.CampaignDetailComponent
      ),
  },
  {
    path: 'campaigns/:id/edit',
    loadComponent: () =>
      import('./campaigns/campaign-wizard.component').then(
        (m) => m.CampaignWizardComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
