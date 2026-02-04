import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { SalesAuthService } from '../services/sales-auth.service';

/**
 * Guard that protects sales routes.
 * Redirects to sales login if user is not authenticated.
 */
export const salesAuthGuard: CanActivateFn = () => {
  const authService = inject(SalesAuthService);
  const router = inject(Router);

  if (authService.hasValidToken()) {
    return true;
  }

  router.navigate(['/sales/login']);
  return false;
};

/**
 * Guard that checks if user has required sales role.
 * Usage: canActivate: [salesRoleGuard(['SALES_ADMIN', 'SALES_REP'])]
 */
export const salesRoleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(SalesAuthService);
    const router = inject(Router);

    if (!authService.hasValidToken()) {
      router.navigate(['/sales/login']);
      return false;
    }

    if (authService.hasAnyRole(allowedRoles)) {
      return true;
    }

    router.navigate(['/sales/unauthorized']);
    return false;
  };
};
