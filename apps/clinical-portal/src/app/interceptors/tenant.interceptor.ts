import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';
import { AuthService } from '../services/auth.service';

/**
 * Tenant Interceptor
 * Automatically adds X-Tenant-ID header to all outgoing HTTP requests
 * to CQL Engine, Quality Measure, and FHIR services
 */
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  // Check if the request is to our backend services
  const isCqlEngine = req.url.includes(API_CONFIG.CQL_ENGINE_URL);
  const isQualityMeasure = req.url.includes(API_CONFIG.QUALITY_MEASURE_URL);
  const isFhir = req.url.includes(API_CONFIG.FHIR_SERVER_URL);
  const isCareGap = req.url.includes(API_CONFIG.CARE_GAP_URL);
  const isPatient = req.url.includes(API_CONFIG.PATIENT_URL);

  // Only add tenant header for backend API calls
  if (isCqlEngine || isQualityMeasure || isFhir || isCareGap || isPatient) {
    const authService = inject(AuthService);
    const tenantId = authService.getTenantId() || API_CONFIG.DEFAULT_TENANT_ID;

    const clonedRequest = req.clone({
      setHeaders: {
        [HTTP_HEADERS.TENANT_ID]: tenantId,
      },
    });

    return next(clonedRequest);
  }

  // Pass through for non-backend requests
  return next(req);
};
