import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { InvestorAuthService } from '../services/investor-auth.service';

/**
 * Auth guard for the Investor Dashboard.
 * Redirects to investor-login if not authenticated.
 */
export const InvestorAuthGuard: CanActivateFn = () => {
  const authService = inject(InvestorAuthService);
  const router = inject(Router);

  if (authService.hasValidToken()) {
    return true;
  }

  // Redirect to investor login page
  router.navigate(['/investor-login']);
  return false;
};
