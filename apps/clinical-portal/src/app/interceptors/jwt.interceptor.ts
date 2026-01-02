import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/**
 * JWT Interceptor - Handles 401 errors with automatic token refresh
 *
 * SECURITY (HIPAA Compliant):
 * - JWT tokens are stored in HttpOnly cookies (XSS protected)
 * - Cookies are automatically sent with requests via withCredentials: true
 * - No explicit Authorization header needed - browser handles cookie transmission
 * - On 401, refreshToken() is called which sets new HttpOnly cookies
 *
 * Features:
 * - Handles 401 Unauthorized responses with automatic token refresh
 * - Redirects to login on authentication failure
 * - Skips refresh logic for public endpoints
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // List of public endpoints that don't require authentication
  const publicEndpoints = [
    '/auth/login',
    '/auth/register',
    '/auth/forgot-password',
    '/auth/reset-password',
  ];

  // Check if this is a public endpoint
  const isPublicEndpoint = publicEndpoints.some((endpoint) => req.url.includes(endpoint));

  if (isPublicEndpoint) {
    return next(req);
  }

  // Pass request through - JWT auth is handled via HttpOnly cookies
  // Cookies are automatically sent with withCredentials: true (set in api.service.ts)
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expired or invalid, try to refresh
        // refreshToken() will get new HttpOnly cookies from the server
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Retry the request - new cookies are automatically included
            return next(req);
          }),
          catchError((refreshError) => {
            // Refresh failed, logout and redirect to login
            authService.logout();
            router.navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }

      return throwError(() => error);
    })
  );
};
