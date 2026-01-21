import { Route } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { DevGuard } from './guards/dev.guard';

/**
 * Application Routes Configuration
 *
 * Route Protection:
 * - Public routes: login, forgot-password, unauthorized
 * - Protected routes: All others require AuthGuard
 * - Role-based routes: Some require specific roles via RoleGuard
 *
 * Demo Mode: When gateway.auth.enforced=false (demo mode), authentication
 * is optional and the demo login bypasses actual authentication.
 */
export const appRoutes: Route[] = [
  // ==================== Public Routes ====================
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./pages/unauthorized/unauthorized.component').then(
        (m) => m.UnauthorizedComponent
      ),
  },
  {
    path: 'compliance',
    loadComponent: () =>
      import('./pages/compliance/compliance-dashboard.component').then(
        (m) => m.ComplianceDashboardComponent
      ),
    // Public route - accessible without authentication for testing
  },
  {
    path: 'testing',
    loadComponent: () =>
      import('./pages/testing-dashboard/testing-dashboard.component').then(
        (m) => m.TestingDashboardComponent
      ),
    // Public route - accessible without authentication for testing
  },
  {
    path: 'demo-startup',
    loadComponent: () =>
      import('@clinical-portal/feature-dashboard').then(
        (m) => m.DemoStartupMonitorComponent
      ),
    // Public route - demo startup monitoring
  },

  // ==================== Protected Routes ====================
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'patients',
    loadComponent: () =>
      import('./pages/patients/patients.component').then(
        (m) => m.PatientsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_PATIENTS'] },
  },
  {
    path: 'patients/:id',
    loadComponent: () =>
      import('./pages/patient-detail/patient-detail.component').then(
        (m) => m.PatientDetailComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_PATIENTS'] },
  },
  {
    path: 'quality-measures',
    loadComponent: () =>
      import('./pages/quality-measures/quality-measures.component').then(
        (m) => m.QualityMeasuresComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_EVALUATIONS'] },
  },
  {
    path: 'measure-comparison',
    loadComponent: () =>
      import('./pages/measure-comparison/measure-comparison.component').then(
        (m) => m.MeasureComparisonComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_EVALUATIONS'] },
  },
  {
    path: 'evaluations',
    loadComponent: () =>
      import('./pages/evaluations/evaluations.component').then(
        (m) => m.EvaluationsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_EVALUATIONS'] },
  },
  {
    path: 'results',
    loadComponent: () =>
      import('./pages/results/results.component').then(
        (m) => m.ResultsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_EVALUATIONS'] },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./pages/reports/reports.component').then(
        (m) => m.ReportsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_REPORTS'] },
  },
  {
    path: 'report-builder',
    loadComponent: () =>
      import('./pages/custom-report-builder/custom-report-builder.component').then(
        (m) => m.CustomReportBuilderComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_REPORTS'] },
  },
  {
    path: 'report-builder/:id',
    loadComponent: () =>
      import('./pages/custom-report-builder/custom-report-builder.component').then(
        (m) => m.CustomReportBuilderComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_REPORTS'] },
  },
  {
    path: 'visualization',
    loadComponent: () =>
      import('./visualization/angular/visualization-layout.component').then(
        (m) => m.VisualizationLayoutComponent
      ),
    canActivate: [AuthGuard],
    children: [
      {
        path: '',
        redirectTo: 'live-monitor',
        pathMatch: 'full',
      },
      {
        path: 'live-monitor',
        loadComponent: () =>
          import('./visualization/angular/live-batch-monitor.component').then(
            (m) => m.LiveBatchMonitorComponent
          ),
      },
      {
        path: 'quality-constellation',
        loadComponent: () =>
          import('./visualization/angular/quality-constellation.component').then(
            (m) => m.QualityConstellationComponent
          ),
      },
      {
        path: 'flow-network',
        loadComponent: () =>
          import('./visualization/angular/flow-network.component').then(
            (m) => m.FlowNetworkComponent
          ),
      },
      {
        path: 'measure-matrix',
        loadComponent: () =>
          import('./visualization/angular/measure-matrix.component').then(
            (m) => m.MeasureMatrixComponent
          ),
      },
    ],
  },
  {
    path: 'measure-builder',
    loadComponent: () =>
      import('./pages/measure-builder/measure-builder.component').then(
        (m) => m.MeasureBuilderComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MEASURE_DEVELOPER'] },
  },
  {
    path: 'ai-assistant',
    loadComponent: () =>
      import('./pages/ai-dashboard/ai-dashboard.component').then(
        (m) => m.AIDashboardComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'knowledge-base',
    canActivate: [AuthGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/knowledge-base/knowledge-base.component').then(
            (m) => m.KnowledgeBaseComponent
          ),
      },
      {
        path: 'article/:id',
        loadComponent: () =>
          import('./pages/knowledge-base/article-view/article-view.component').then(
            (m) => m.ArticleViewComponent
          ),
      },
      {
        path: 'category/:categoryId',
        loadComponent: () =>
          import('./pages/knowledge-base/knowledge-base.component').then(
            (m) => m.KnowledgeBaseComponent
          ),
      },
    ],
  },
  {
    path: 'pre-visit',
    loadComponent: () =>
      import('./pages/pre-visit-planning/pre-visit-planning.component').then(
        (m) => m.PreVisitPlanningComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'EVALUATOR'] },
  },
  {
    path: 'care-recommendations',
    loadComponent: () =>
      import('./pages/care-recommendations/care-recommendations.component').then(
        (m) => m.CareRecommendationsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_CARE_GAPS'] },
  },
  {
    path: 'care-gaps',
    loadComponent: () =>
      import('./pages/care-gaps/care-gap-manager.component').then(
        (m) => m.CareGapManagerComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_CARE_GAPS'] },
  },
  {
    path: 'risk-stratification',
    loadComponent: () =>
      import('./pages/risk-stratification/risk-stratification.component').then(
        (m) => m.RiskStratificationComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_PATIENTS'] },
  },
  {
    path: 'outreach-campaigns',
    loadComponent: () =>
      import('./pages/outreach-campaigns/outreach-campaigns.component').then(
        (m) => m.OutreachCampaignsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_CARE_GAPS'] },
  },
  {
    path: 'patient-health',
    loadComponent: () =>
      import('./pages/patient-health-overview/patient-health-overview.component').then(
        (m) => m.PatientHealthOverviewComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_PATIENTS'] },
  },
  {
    path: 'agent-builder',
    loadComponent: () =>
      import('./pages/agent-builder/agent-builder.component').then(
        (m) => m.AgentBuilderComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'DEVELOPER'] },
  },
  {
    path: 'agent-builder/:id',
    loadComponent: () =>
      import('./pages/agent-builder/agent-builder.component').then(
        (m) => m.AgentBuilderComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'DEVELOPER'] },
  },
  {
    path: 'insights',
    loadComponent: () =>
      import('./pages/insights/insights.component').then(
        (m) => m.InsightsComponent
      ),
    canActivate: [AuthGuard],
    data: { permissions: ['VIEW_PATIENTS'] },
  },

  // ==================== Fallback Route ====================
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
