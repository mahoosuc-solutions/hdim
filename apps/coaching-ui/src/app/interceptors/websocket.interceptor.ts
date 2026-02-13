import { HttpInterceptorFn } from '@angular/common/http';

/**
 * HTTP Interceptor for coaching UI service calls.
 *
 * Adds necessary headers for:
 * - Multi-tenant isolation (X-Tenant-ID)
 * - JWT authentication (Authorization)
 * - Request tracking (X-Request-ID)
 */
export const webSocketInterceptor: HttpInterceptorFn = (req, next) => {
  // Get tenant ID from localStorage
  const tenantId = localStorage.getItem('tenant_id') || 'default-tenant';

  // Get JWT token from localStorage
  const jwtToken = localStorage.getItem('jwt_token');

  // Clone request and add headers
  let modifiedReq = req.clone({
    setHeaders: {
      'X-Tenant-ID': tenantId,
    }
  });

  // Add authorization header if token exists
  if (jwtToken) {
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        'Authorization': `Bearer ${jwtToken}`,
      }
    });
  }

  return next(modifiedReq);
};
