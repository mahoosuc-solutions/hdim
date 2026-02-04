import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  private platformId = inject(PLATFORM_ID);
  private router = inject(Router);

  canActivate(): boolean {
    // Only access localStorage in browser environment (not SSR)
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('auth_token');

      if (!token) {
        // Only allow demo mode in non-production environments
        if (environment.features.demoMode) {
          // Development: auto-set demo token for convenience
          localStorage.setItem('auth_token', 'demo-admin-token');
          localStorage.setItem('user_role', 'ADMIN');
          return true;
        }

        // Production: redirect to login page (no auto-authentication)
        this.router.navigate(['/sales/login']);
        return false;
      }

      // Token exists - allow access
      // TODO: In production, validate token with backend JWT verification
      return true;
    }

    // SSR: allow access (actual auth check happens client-side)
    return true;
  }
}
