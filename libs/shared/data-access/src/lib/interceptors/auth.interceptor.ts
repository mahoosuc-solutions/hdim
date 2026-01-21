import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Auth Interceptor (Shared)
 * Handles authentication concerns for backend requests.
 *
 * SECURITY (HIPAA Compliant):
 * - JWT tokens are stored in HttpOnly cookies (XSS protected)
 * - Cookies are automatically sent with requests via withCredentials: true
 * - No explicit Authorization header needed - cookies handle auth
 *
 * The API Gateway validates JWT from cookies and injects trusted headers
 * for downstream services.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Auth is handled via HttpOnly cookies
  // No action needed in interceptor - cookies are automatically included
  // when HttpClient is configured with withCredentials: true
  return next(req);
};
