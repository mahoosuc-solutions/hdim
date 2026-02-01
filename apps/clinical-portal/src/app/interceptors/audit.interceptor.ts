import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { AuditService, AuditAction, AuditOutcome } from '../services/audit.service';

/**
 * HTTP Audit Logging Interceptor
 *
 * HIPAA Compliance: 45 CFR § 164.312(b) - Audit Controls
 * Automatically logs all API calls to backend services for audit trail compliance
 *
 * Features:
 * - Automatic audit logging for all HTTP requests
 * - No manual instrumentation required in services
 * - Resource type extraction from URL patterns
 * - Success/failure outcome tracking
 * - Request duration measurement
 * - Fire-and-forget logging (non-blocking)
 *
 * Logged events include:
 * - Patient data access (GET /patient/*)
 * - Care gap operations (GET/POST /care-gap/*)
 * - Evaluation execution (POST /cql-engine/evaluations)
 * - Report generation (POST /reports/*)
 * - FHIR resource access (GET/POST /fhir/*)
 * - All other backend API calls
 *
 * NOT logged:
 * - Static assets (images, CSS, JS)
 * - External API calls (non-backend URLs)
 * - Health check endpoints
 *
 * Usage:
 * This interceptor is automatically registered in app.config.ts
 * No additional code is required in services
 */
export const auditInterceptor: HttpInterceptorFn = (req, next) => {
  const auditService = inject(AuditService);
  const startTime = Date.now();

  // Filter: Only audit backend service calls
  if (!shouldAuditRequest(req.url)) {
    return next(req);
  }

  // Extract resource information from URL
  const resourceInfo = extractResourceInfo(req.url, req.method);

  return next(req).pipe(
    tap(event => {
      // Only log successful responses (not progress events)
      if (event instanceof HttpResponse) {
        const durationMs = Date.now() - startTime;

        auditService.log({
          action: mapMethodToAction(req.method),
          resourceType: resourceInfo.resourceType,
          resourceId: resourceInfo.resourceId,
          requestPath: req.url,
          methodName: req.method,
          outcome: AuditOutcome.SUCCESS,
          durationMs,
          metadata: {
            statusCode: event.status,
            responseSize: getResponseSize(event.body),
          },
        });
      }
    }),
    catchError(error => {
      // Log failed requests
      if (error instanceof HttpErrorResponse) {
        const durationMs = Date.now() - startTime;

        auditService.log({
          action: mapMethodToAction(req.method),
          resourceType: resourceInfo.resourceType,
          resourceId: resourceInfo.resourceId,
          requestPath: req.url,
          methodName: req.method,
          outcome: determineOutcome(error.status),
          errorMessage: error.message,
          durationMs,
          metadata: {
            statusCode: error.status,
            errorCode: error.error?.code,
          },
        });
      }

      return throwError(() => error);
    })
  );
};

/**
 * Determine if request should be audited
 */
function shouldAuditRequest(url: string): boolean {
  // Exclude static assets
  if (url.match(/\.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$/i)) {
    return false;
  }

  // Exclude health checks
  if (url.includes('/health') || url.includes('/ping')) {
    return false;
  }

  // Exclude audit endpoint itself (prevent recursion)
  if (url.includes('/audit/events')) {
    return false;
  }

  // Include backend API calls
  return url.includes('/cql-engine') ||
         url.includes('/patient') ||
         url.includes('/care-gap') ||
         url.includes('/quality-measure') ||
         url.includes('/fhir') ||
         url.includes('/reports') ||
         url.includes('/evaluations') ||
         url.includes('/batch') ||
         url.includes('/qrda') ||
         url.includes('/api/');
}

/**
 * Extract resource type and ID from URL
 */
function extractResourceInfo(url: string, method: string): {
  resourceType?: string;
  resourceId?: string;
} {
  // Pattern matching for common resource URLs
  // Examples:
  // /patient/123 -> { resourceType: 'Patient', resourceId: '123' }
  // /care-gap/patient/456 -> { resourceType: 'CareGap', resourceId: '456' }
  // /cql-engine/evaluations -> { resourceType: 'Evaluation' }
  // /fhir/Patient/789 -> { resourceType: 'Patient', resourceId: '789' }

  const patterns: Array<{
    regex: RegExp;
    resourceType: string;
    idGroup?: number;
  }> = [
    // Patient endpoints
    { regex: /\/patient\/([a-f0-9-]+)/i, resourceType: 'Patient', idGroup: 1 },
    { regex: /\/patient$/i, resourceType: 'Patient' },

    // Care gap endpoints
    { regex: /\/care-gap\/([a-f0-9-]+)/i, resourceType: 'CareGap', idGroup: 1 },
    { regex: /\/care-gap/i, resourceType: 'CareGap' },

    // Evaluation endpoints
    { regex: /\/evaluations\/([a-f0-9-]+)/i, resourceType: 'Evaluation', idGroup: 1 },
    { regex: /\/evaluations/i, resourceType: 'Evaluation' },

    // Quality measure endpoints
    { regex: /\/quality-measure\/([a-f0-9-]+)/i, resourceType: 'QualityMeasure', idGroup: 1 },
    { regex: /\/quality-measure/i, resourceType: 'QualityMeasure' },

    // FHIR resource endpoints
    { regex: /\/fhir\/Patient\/([a-f0-9-]+)/i, resourceType: 'Patient', idGroup: 1 },
    { regex: /\/fhir\/Condition\/([a-f0-9-]+)/i, resourceType: 'Condition', idGroup: 1 },
    { regex: /\/fhir\/Observation\/([a-f0-9-]+)/i, resourceType: 'Observation', idGroup: 1 },
    { regex: /\/fhir\/Procedure\/([a-f0-9-]+)/i, resourceType: 'Procedure', idGroup: 1 },
    { regex: /\/fhir\/(\w+)/i, resourceType: 'FhirResource', idGroup: 1 },

    // Report endpoints
    { regex: /\/reports\/([a-f0-9-]+)/i, resourceType: 'Report', idGroup: 1 },
    { regex: /\/reports/i, resourceType: 'Report' },

    // Batch evaluation endpoints
    { regex: /\/batch\/([a-f0-9-]+)/i, resourceType: 'BatchEvaluation', idGroup: 1 },
    { regex: /\/batch/i, resourceType: 'BatchEvaluation' },

    // QRDA export endpoints
    { regex: /\/qrda\/([a-f0-9-]+)/i, resourceType: 'QrdaExport', idGroup: 1 },
    { regex: /\/qrda/i, resourceType: 'QrdaExport' },
  ];

  for (const pattern of patterns) {
    const match = url.match(pattern.regex);
    if (match) {
      return {
        resourceType: pattern.resourceType,
        resourceId: pattern.idGroup ? match[pattern.idGroup] : undefined,
      };
    }
  }

  // Default: extract from path
  const pathParts = url.split('/').filter(Boolean);
  if (pathParts.length > 0) {
    const lastPart = pathParts[pathParts.length - 1];
    // If last part looks like an ID (UUID), use second-to-last as resource type
    if (lastPart.match(/^[a-f0-9-]{36}$/i) && pathParts.length > 1) {
      return {
        resourceType: capitalize(pathParts[pathParts.length - 2]),
        resourceId: lastPart,
      };
    }
    return {
      resourceType: capitalize(lastPart),
    };
  }

  return {};
}

/**
 * Map HTTP method to audit action
 */
function mapMethodToAction(method: string): AuditAction {
  switch (method.toUpperCase()) {
    case 'GET':
      return AuditAction.READ;
    case 'POST':
      return AuditAction.CREATE;
    case 'PUT':
      return AuditAction.UPDATE;
    case 'PATCH':
      return AuditAction.UPDATE;
    case 'DELETE':
      return AuditAction.DELETE;
    default:
      return AuditAction.EXECUTE;
  }
}

/**
 * Determine outcome based on HTTP status code
 */
function determineOutcome(statusCode: number): AuditOutcome {
  if (statusCode >= 200 && statusCode < 300) {
    return AuditOutcome.SUCCESS;
  }
  if (statusCode >= 400 && statusCode < 500) {
    // Client errors (bad request, unauthorized, not found)
    return AuditOutcome.MINOR_FAILURE;
  }
  if (statusCode >= 500) {
    // Server errors
    return AuditOutcome.SERIOUS_FAILURE;
  }
  return AuditOutcome.MINOR_FAILURE;
}

/**
 * Get response size for metadata
 */
function getResponseSize(body: unknown): number {
  if (!body) return 0;
  try {
    return JSON.stringify(body).length;
  } catch {
    return 0;
  }
}

/**
 * Capitalize first letter
 */
function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1);
}
