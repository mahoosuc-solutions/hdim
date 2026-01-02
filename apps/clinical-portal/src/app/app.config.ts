import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { appRoutes } from './app.routes';
import { tenantInterceptor } from './interceptors/tenant.interceptor';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { reducers } from './store';
import { PatientEffects } from './store/effects/patient.effects';
import { CareRecommendationEffects } from './store/effects/care-recommendation.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(appRoutes),
    provideAnimations(),
    provideHttpClient(
      withInterceptors([
        tenantInterceptor,
        authInterceptor,
        errorInterceptor,
      ])
    ),
    // NgRx Store and Effects
    provideStore(reducers),
    provideEffects([PatientEffects, CareRecommendationEffects]),
  ],
};
