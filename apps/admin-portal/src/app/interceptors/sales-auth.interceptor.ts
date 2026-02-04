import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { SalesAuthService } from '../services/sales-auth.service';
import { environment } from '../../environments/environment';

/**
 * HTTP interceptor that adds JWT Bearer token and tenant header to sales API requests.
 * Also handles 401 responses by attempting token refresh.
 */
export const salesAuthInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(SalesAuthService);

  // Only intercept requests to the sales API
  const salesApiUrl = environment.apiConfig.salesApiUrl;
  const isSalesApi =
    req.url.startsWith(salesApiUrl) ||
    req.url.includes('localhost:8106/sales') ||
    req.url.includes('/sales/api/');

  if (!isSalesApi) {
    return next(req);
  }

  // Skip authentication for public endpoints
  const publicEndpoints = ['/api/auth/login', '/api/auth/refresh', '/api/health'];
  if (publicEndpoints.some((endpoint) => req.url.includes(endpoint))) {
    return next(req);
  }

  // Add Bearer token and tenant header to request
  const token = authService.getAccessToken();
  const tenantId = authService.tenantId();

  if (token) {
    req = addHeadersToRequest(req, token, tenantId);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/refresh')) {
        // Token expired - attempt refresh
        return authService.refreshToken().pipe(
          switchMap((response) => {
            // Retry original request with new token
            const newReq = addHeadersToRequest(req, response.accessToken, tenantId);
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

function addHeadersToRequest(
  req: HttpRequest<unknown>,
  token: string,
  tenantId: string
): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-ID': tenantId,
    },
  });
}
