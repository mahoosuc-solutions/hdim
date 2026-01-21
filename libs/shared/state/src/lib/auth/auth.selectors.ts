import { createSelector } from '@ngrx/store';
import { authFeature } from './auth.reducer';

export const selectSharedAuthState = authFeature.selectSharedAuthState;

export const selectSharedAuthUser = createSelector(
  selectSharedAuthState,
  (state) => state.user
);

export const selectSharedAuthRoles = createSelector(
  selectSharedAuthState,
  (state) => state.roles
);

export const selectSharedAuthPermissions = createSelector(
  selectSharedAuthState,
  (state) => state.permissions
);

export const selectSharedAuthTenantId = createSelector(
  selectSharedAuthState,
  (state) => state.tenantId
);

export const selectSharedAuthLoading = createSelector(
  selectSharedAuthState,
  (state) => state.loading
);

export const selectSharedAuthError = createSelector(
  selectSharedAuthState,
  (state) => state.error
);

export const selectIsAuthenticated = createSelector(
  selectSharedAuthUser,
  (user) => !!user
);

export const selectCan = (permission: string) =>
  createSelector(selectSharedAuthPermissions, (permissions) =>
    permissions.includes(permission)
  );

export const selectHasRole = (role: string) =>
  createSelector(selectSharedAuthRoles, (roles) => roles.includes(role));
