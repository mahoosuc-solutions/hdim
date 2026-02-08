import { Route } from '@angular/router';
import { HomePage } from './pages/home.page';

export const appRoutes: Route[] = [
  {
    path: '',
    component: HomePage,
  },
  {
    path: 'mfePatients',
    loadChildren: () =>
      import('mfePatients/Routes').then((m) => m!.remoteRoutes),
  },
  {
    path: 'mfeMeasureBuilder',
    loadChildren: () =>
      import('mfeMeasureBuilder/Routes').then((m) => m!.remoteRoutes),
  },
];
