import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Error Interceptor
 * Handles HTTP errors globally and provides user-friendly error messages
 * Also adds required headers for multi-tenancy support
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Get tenant ID from current user or use default
  const tenantId = authService.getTenantId() || 'default-tenant';

  // Add X-Tenant-ID header for multi-tenancy support
  const modifiedReq = req.clone({
    setHeaders: {
      'X-Tenant-ID': tenantId,
    },
  });

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unknown error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side or network error
        errorMessage = `Network Error: ${error.error.message}`;
      } else {
        // Backend returned an unsuccessful response code
        switch (error.status) {
          case 400:
            errorMessage = 'Bad Request: Please check your input and try again.';
            break;
          case 401:
            errorMessage = 'Unauthorized: Please log in to access this resource.';
            // Clear auth state and redirect to login
            // Skip redirect if we're already on the login page or this is a login request
            if (!req.url.includes('/auth/login') && !router.url.includes('/login')) {
              authService.logout();
            }
            break;
          case 403:
            errorMessage = 'Forbidden: You do not have permission to access this resource.';
            break;
          case 404:
            errorMessage = 'Not Found: The requested resource was not found.';
            break;
          case 500:
            errorMessage = 'Internal Server Error: Please try again later.';
            break;
          case 503:
            errorMessage = 'Service Unavailable: The service is temporarily unavailable.';
            break;
          default:
            if (error.error?.message) {
              errorMessage = error.error.message;
            } else {
              errorMessage = `Error Code: ${error.status}\nMessage: ${error.statusText}`;
            }
        }
      }

      // Log error to console for debugging
      console.error('HTTP Error:', {
        url: req.url,
        status: error.status,
        statusText: error.statusText,
        message: errorMessage,
        error: error.error,
      });

      // Re-throw the error with user-friendly message
      return throwError(() => ({
        ...error,
        userMessage: errorMessage,
      }));
    })
  );
};
