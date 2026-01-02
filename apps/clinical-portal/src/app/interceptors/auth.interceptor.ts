import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';
import { AuthService } from '../services/auth.service';

/**
 * Auth Interceptor
 * Adds tenant context to backend service requests.
 *
 * SECURITY (HIPAA Compliant):
 * - JWT tokens are stored in HttpOnly cookies (XSS protected)
 * - Cookies are automatically sent with requests via withCredentials: true
 * - No explicit Authorization header needed - cookies handle auth
 * - Tenant ID header is still added for multi-tenancy support
 *
 * The API Gateway validates JWT from cookies and injects trusted headers
 * for downstream services.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Get tenant context (still needed for multi-tenancy)
  const tenantId = authService.getTenantId();

  // Check if request is to backend services
  const isBackendService =
    req.url.includes(API_CONFIG.CQL_ENGINE_URL) ||
    req.url.includes(API_CONFIG.QUALITY_MEASURE_URL) ||
    req.url.includes(API_CONFIG.FHIR_SERVER_URL);

  if (isBackendService && tenantId) {
    // Add tenant context header
    // JWT auth is handled via HttpOnly cookies (withCredentials: true in api.service.ts)
    const clonedRequest = req.clone({
      setHeaders: {
        [HTTP_HEADERS.TENANT_ID]: tenantId,
      },
    });

    return next(clonedRequest);
  }

  // For requests without tenant context, pass through
  // Cookies with JWT token are automatically included via withCredentials
  return next(req);
};
