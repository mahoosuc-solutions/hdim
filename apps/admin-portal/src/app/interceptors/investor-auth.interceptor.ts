import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { InvestorAuthService } from '../services/investor-auth.service';

/**
 * HTTP interceptor that adds JWT Bearer token to investor API requests.
 * Also handles 401 responses by attempting token refresh.
 */
export const investorAuthInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(InvestorAuthService);

  // Only intercept requests to the investor API
  if (!req.url.includes('/investor/api/')) {
    return next(req);
  }

  // Skip authentication for public endpoints
  const publicEndpoints = ['/api/auth/login', '/api/auth/refresh', '/api/health'];
  if (publicEndpoints.some((endpoint) => req.url.includes(endpoint))) {
    return next(req);
  }

  // Add Bearer token to request
  const token = authService.getAccessToken();
  if (token) {
    req = addTokenToRequest(req, token);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/refresh')) {
        // Token expired - attempt refresh
        return authService.refreshToken().pipe(
          switchMap((response) => {
            // Retry original request with new token
            const newReq = addTokenToRequest(req, response.accessToken);
            return next(newReq);
          }),
          catchError((refreshError) => {
            // Refresh failed - logout user
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }

      return throwError(() => error);
    })
  );
};

function addTokenToRequest(
  req: HttpRequest<unknown>,
  token: string
): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
