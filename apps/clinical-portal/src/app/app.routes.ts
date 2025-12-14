import { Route } from '@angular/router';

export const appRoutes: Route[] = [
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
  },
  {
    path: 'patients',
    loadComponent: () =>
      import('./pages/patients/patients.component').then(
        (m) => m.PatientsComponent
      ),
  },
  {
    path: 'patients/:id',
    loadComponent: () =>
      import('./pages/patient-detail/patient-detail.component').then(
        (m) => m.PatientDetailComponent
      ),
  },
  {
    path: 'evaluations',
    loadComponent: () =>
      import('./pages/evaluations/evaluations.component').then(
        (m) => m.EvaluationsComponent
      ),
  },
  {
    path: 'results',
    loadComponent: () =>
      import('./pages/results/results.component').then(
        (m) => m.ResultsComponent
      ),
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./pages/reports/reports.component').then(
        (m) => m.ReportsComponent
      ),
  },
  {
    path: 'visualization',
    loadComponent: () =>
      import('./visualization/angular/visualization-layout.component').then(
        (m) => m.VisualizationLayoutComponent
      ),
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
  },
  {
    path: 'ai-assistant',
    loadComponent: () =>
      import('./pages/ai-dashboard/ai-dashboard.component').then(
        (m) => m.AIDashboardComponent
      ),
  },
  {
    path: 'knowledge-base',
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
    path: 'care-recommendations',
    loadComponent: () =>
      import('./pages/care-recommendations/care-recommendations.component').then(
        (m) => m.CareRecommendationsComponent
      ),
  },
  {
    path: 'agent-builder',
    loadComponent: () =>
      import('./pages/agent-builder/agent-builder.component').then(
        (m) => m.AgentBuilderComponent
      ),
  },
  {
    path: 'agent-builder/:id',
    loadComponent: () =>
      import('./pages/agent-builder/agent-builder.component').then(
        (m) => m.AgentBuilderComponent
      ),
  },
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
