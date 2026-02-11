import { Route } from '@angular/router';
import { HomePage } from './pages/home.page';

export const appRoutes: Route[] = [
  {
    path: '',
    component: HomePage,
  },
  {
    path: 'deployment',
    loadChildren: () =>
      import('mfeDeployment/Routes').then((m) => m!.remoteRoutes),
  },
];
