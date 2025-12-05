import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { HTTP_HEADERS } from '../config/api.config';

/**
 * JWT Interceptor - Adds JWT token to requests and handles 401 errors
 *
 * Features:
 * - Adds Authorization header with JWT token
 * - Handles 401 Unauthorized responses with token refresh
 * - Redirects to login on authentication failure
 * - Skips authentication for public endpoints
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

  // Get the JWT token
  const token = authService.getToken();

  if (token) {
    // Clone the request and add the Authorization header
    const clonedRequest = req.clone({
      setHeaders: {
        [HTTP_HEADERS.AUTHORIZATION]: `Bearer ${token}`,
      },
    });

    return next(clonedRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expired or invalid, try to refresh
          return authService.refreshToken().pipe(
            switchMap((tokenResponse) => {
              // Retry the request with new token
              const retryRequest = req.clone({
                setHeaders: {
                  [HTTP_HEADERS.AUTHORIZATION]: `Bearer ${tokenResponse.accessToken}`,
                },
              });
              return next(retryRequest);
            }),
            catchError((refreshError) => {
              // Refresh failed, logout and redirect to login
              console.error('Token refresh failed:', refreshError);
              authService.logout();
              router.navigate(['/login']);
              return throwError(() => refreshError);
            })
          );
        }

        return throwError(() => error);
      })
    );
  }

  // No token available, pass the request as is
  return next(req);
};
