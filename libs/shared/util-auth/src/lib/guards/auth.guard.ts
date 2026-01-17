import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Auth Guard - Protects routes from unauthorized access
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

/**
 * Role Guard Factory - Creates a guard that checks for specific roles
 */
export function roleGuard(roles: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }

    if (authService.hasAnyRole(roles)) {
      return true;
    }

    router.navigate(['/unauthorized']);
    return false;
  };
}

/**
 * Permission Guard Factory - Creates a guard that checks for specific permissions
 */
export function permissionGuard(permissions: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }

    const hasPermission = permissions.some((permission) =>
      authService.hasPermission(permission)
    );

    if (hasPermission) {
      return true;
    }

    router.navigate(['/unauthorized']);
    return false;
  };
}
