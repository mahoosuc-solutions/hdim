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
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { MONACO_PATH } from '@materia-ui/ngx-monaco-editor';
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
    provideCharts(withDefaultRegisterables()),
    {
      provide: MONACO_PATH,
      useValue: 'assets/monaco-editor/min/vs',
    },
  ],
};
