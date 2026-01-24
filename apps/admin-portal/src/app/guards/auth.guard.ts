import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  private platformId = inject(PLATFORM_ID);
  private router = inject(Router);

  canActivate(): boolean {
    // Only access localStorage in browser environment (not SSR)
    if (isPlatformBrowser(this.platformId)) {
      // For demo purposes, always allow access
      // In production, check for valid JWT token
      const token = localStorage.getItem('auth_token');

      // Demo mode: allow without token
      if (!token) {
        // For demo, auto-set a demo token
        localStorage.setItem('auth_token', 'demo-admin-token');
        localStorage.setItem('user_role', 'ADMIN');
      }
    }

    return true;
  }
}
