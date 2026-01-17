import { createFeature, createReducer, on } from '@ngrx/store';
import type { User } from '@health-platform/shared/util-auth';
import * as AuthActions from './auth.actions';

export const AUTH_FEATURE_KEY = 'sharedAuth';

export interface AuthState {
  user: User | null;
  tenantId: string | null;
  roles: string[];
  permissions: string[];
  loading: boolean;
  error: string | null;
  initialized: boolean;
  lastLoadedAt: string | null;
}

export const initialAuthState: AuthState = {
  user: null,
  tenantId: null,
  roles: [],
  permissions: [],
  loading: false,
  error: null,
  initialized: false,
  lastLoadedAt: null,
};

const reducer = createReducer(
  initialAuthState,
  on(AuthActions.loadCurrentUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(AuthActions.loadCurrentUserSuccess, (state, { user }) => ({
    ...state,
    user,
    roles: extractRoles(user),
    permissions: extractPermissions(user),
    tenantId: deriveTenantId(user),
    loading: false,
    initialized: true,
    error: null,
    lastLoadedAt: new Date().toISOString(),
  })),
  on(AuthActions.loadCurrentUserFailure, (state, { error }) => ({
    ...state,
    user: null,
    roles: [],
    permissions: [],
    tenantId: null,
    loading: false,
    initialized: true,
    error,
  })),
  on(AuthActions.setActiveTenant, (state, { tenantId }) => ({
    ...state,
    tenantId,
  })),
  on(AuthActions.syncUserFromStorage, (state, { user }) => ({
    ...state,
    user,
    roles: extractRoles(user),
    permissions: extractPermissions(user),
    tenantId: deriveTenantId(user),
    initialized: true,
  })),
  on(AuthActions.clearAuthState, () => initialAuthState)
);

export const authFeature = createFeature({
  name: AUTH_FEATURE_KEY,
  reducer,
});

export function extractRoles(user: User | null): string[] {
  if (!user?.roles) {
    return [];
  }
  return user.roles.map((role) => role.name);
}

export function extractPermissions(user: User | null): string[] {
  if (!user?.roles) {
    return [];
  }
  return user.roles.flatMap((role) => role.permissions?.map((p) => p.name) ?? []);
}

export function deriveTenantId(user: User | null): string | null {
  if (!user) {
    return null;
  }
  if (user.tenantIds && user.tenantIds.length > 0) {
    return user.tenantIds[0];
  }
  return user.tenantId ?? null;
}
