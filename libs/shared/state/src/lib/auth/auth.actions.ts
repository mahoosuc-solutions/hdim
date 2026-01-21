import { createAction, props } from '@ngrx/store';
import { User } from '@health-platform/shared/util-auth';

export const loadCurrentUser = createAction('[Shared Auth] Load Current User');

export const loadCurrentUserSuccess = createAction(
  '[Shared Auth] Load Current User Success',
  props<{ user: User }>()
);

export const loadCurrentUserFailure = createAction(
  '[Shared Auth] Load Current User Failure',
  props<{ error: string }>()
);

export const setActiveTenant = createAction(
  '[Shared Auth] Set Active Tenant',
  props<{ tenantId: string | null }>()
);

export const syncUserFromStorage = createAction(
  '[Shared Auth] Sync User From Storage',
  props<{ user: User | null }>()
);

export const clearAuthState = createAction('[Shared Auth] Clear Auth State');

export const logout = createAction('[Shared Auth] Logout');
