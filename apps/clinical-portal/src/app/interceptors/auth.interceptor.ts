import { HttpInterceptorFn } from '@angular/common/http';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';

/**
 * Auth Interceptor
 * Adds HTTP Basic Authentication to backend service requests
 *
 * For development, uses static credentials configured in backend application-docker.yml:
 * - Username: cql-service-user
 * - Password: cql-service-dev-password-change-in-prod
 *
 * In production, this should be replaced with proper JWT/OAuth authentication
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Check if request is to CQL Engine or Quality Measure Service
  const isCqlEngine = req.url.includes(API_CONFIG.CQL_ENGINE_URL);
  const isQualityMeasure = req.url.includes(API_CONFIG.QUALITY_MEASURE_URL);

  if (isCqlEngine || isQualityMeasure) {
    // Development credentials from backend application-docker.yml
    // TODO: Replace with environment-specific configuration
    let username: string;
    let password: string;

    if (isQualityMeasure) {
      // Quality Measure Service credentials
      username = 'qm-service-user';
      password = 'qm-service-dev-password-change-in-prod';
    } else {
      // CQL Engine Service credentials
      username = 'cql-service-user';
      password = 'cql-service-dev-password-change-in-prod';
    }

    const credentials = btoa(`${username}:${password}`);

    const clonedRequest = req.clone({
      setHeaders: {
        [HTTP_HEADERS.AUTHORIZATION]: `Basic ${credentials}`,
      },
    });

    return next(clonedRequest);
  }

  // Pass through without auth for other requests (e.g., FHIR server)
  return next(req);
};
