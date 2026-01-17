import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { appRoutes } from './app.routes';
import { tenantInterceptor, authInterceptor } from '@health-platform/shared/data-access';
import { provideSharedState } from '@health-platform/shared/state';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(appRoutes),
    provideHttpClient(
      withInterceptors([tenantInterceptor, authInterceptor])
    ),
    provideSharedState(),
  ],
};
