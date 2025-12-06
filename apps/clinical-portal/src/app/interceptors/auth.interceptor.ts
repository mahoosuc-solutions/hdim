import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';
import { AuthService } from '../services/auth.service';

/**
 * Auth Interceptor
 * Adds JWT Bearer token authentication to backend service requests
 *
 * SECURITY: Credentials are no longer hardcoded. Authentication is handled via:
 * 1. JWT tokens obtained through the AuthService login flow
 * 2. Tokens are stored securely (HttpOnly cookies in production)
 * 3. Service-to-service auth is handled by the API Gateway
 *
 * For backend services without user context (scheduled jobs, etc.),
 * the API Gateway uses mTLS or service account tokens configured via
 * environment variables (never hardcoded).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Get the JWT token from auth service
  const token = authService.getToken();
  const tenantId = authService.getTenantId();

  // Check if request is to backend services
  const isBackendService =
    req.url.includes(API_CONFIG.CQL_ENGINE_URL) ||
    req.url.includes(API_CONFIG.QUALITY_MEASURE_URL) ||
    req.url.includes(API_CONFIG.FHIR_SERVER_URL);

  if (isBackendService && token) {
    // Add JWT Bearer token for authenticated requests
    const headers: { [key: string]: string } = {
      [HTTP_HEADERS.AUTHORIZATION]: `Bearer ${token}`,
    };

    // Add tenant context if available
    if (tenantId) {
      headers[HTTP_HEADERS.TENANT_ID] = tenantId;
    }

    const clonedRequest = req.clone({
      setHeaders: headers,
    });

    return next(clonedRequest);
  }

  // For unauthenticated requests or public endpoints, pass through
  // The API Gateway will handle service-to-service authentication
  return next(req);
};
