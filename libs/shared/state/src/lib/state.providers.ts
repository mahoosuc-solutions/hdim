import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideState } from '@ngrx/store';
import { authFeature } from './auth/auth.reducer';
import { AuthEffects } from './auth/auth.effects';

export function provideSharedState(): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideStore(),
    provideState(authFeature),
    provideEffects(AuthEffects),
  ]);
}
