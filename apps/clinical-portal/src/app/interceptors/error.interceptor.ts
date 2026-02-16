import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, timer, retry, delayWhen } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';
import { SentryService } from '../services/sentry.service';
import { API_CONFIG } from '../config/api.config';
import { ErrorValidationService } from '../services/error-validation.service';
import { ErrorCode, ErrorSeverity } from '../models/error.model';
import { COMPLIANCE_CONFIG } from '../config/compliance.config';

/**
 * Retry configuration
 */
const RETRY_CONFIG = {
  maxRetries: API_CONFIG.RETRY_ATTEMPTS,
  initialDelay: API_CONFIG.RETRY_DELAY_MS,
  maxDelay: 10000, // 10 seconds max delay
  backoffMultiplier: 2,
};

/**
 * HTTP status codes that should trigger a retry
 * - 408: Request Timeout
 * - 429: Too Many Requests
 * - 500: Internal Server Error
 * - 502: Bad Gateway
 * - 503: Service Unavailable
 * - 504: Gateway Timeout
 */
const RETRYABLE_STATUS_CODES = [408, 429, 500, 502, 503, 504];

/**
 * HTTP methods that are safe to retry (idempotent)
 */
const RETRYABLE_METHODS = ['GET', 'HEAD', 'OPTIONS', 'PUT', 'DELETE'];

/**
 * Check if an error is retryable
 */
function isRetryable(error: HttpErrorResponse, method: string): boolean {
  // Only retry idempotent methods by default
  if (!RETRYABLE_METHODS.includes(method.toUpperCase())) {
    return false;
  }

  // Network error - retry
  if (error.status === 0) {
    return true;
  }

  return RETRYABLE_STATUS_CODES.includes(error.status);
}

/**
 * Calculate exponential backoff delay
 */
function calculateBackoffDelay(attempt: number): number {
  const delay = RETRY_CONFIG.initialDelay * Math.pow(RETRY_CONFIG.backoffMultiplier, attempt);
  // Add jitter (0-25% random delay) to prevent thundering herd
  const jitter = delay * Math.random() * 0.25;
  return Math.min(delay + jitter, RETRY_CONFIG.maxDelay);
}

/**
 * Error Interceptor
 * Handles HTTP errors globally with:
 * - Automatic retry with exponential backoff for transient failures
 * - User-friendly error messages
 * - Multi-tenancy header injection
 * - Authentication error handling
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const errorValidationService = inject(ErrorValidationService);
  const loggerService = inject(LoggerService);
  const sentryService = inject(SentryService);
  const logger = loggerService.withContext('ErrorInterceptor');

  // Get tenant ID from current user or use default
  const tenantId = authService.getTenantId() || API_CONFIG.DEFAULT_TENANT_ID;

  // Add X-Tenant-ID header for multi-tenancy support
  const modifiedReq = req.clone({
    setHeaders: {
      'X-Tenant-ID': tenantId,
    },
  });

  // Track retry attempts for logging
  let retryAttempt = 0;

  return next(modifiedReq).pipe(
    // Retry logic with exponential backoff
    retry({
      count: RETRY_CONFIG.maxRetries,
      delay: (error: HttpErrorResponse, count: number) => {
        retryAttempt = count;

        // Only retry if the error is retryable
        if (!isRetryable(error, req.method)) {
          return throwError(() => error);
        }

        const delay = calculateBackoffDelay(count - 1);
        logger.warn(
          `[Retry ${count}/${RETRY_CONFIG.maxRetries}] Request to ${req.url} failed with status ${error.status}. ` +
          `Retrying in ${Math.round(delay)}ms...`
        );

        return timer(delay);
      },
    }),
    // Handle final error after all retries exhausted
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unknown error occurred';
      const isRetryExhausted = retryAttempt >= RETRY_CONFIG.maxRetries;

      if (error.error instanceof ErrorEvent) {
        // Client-side or network error
        errorMessage = `Network Error: ${error.error.message}`;
        if (isRetryExhausted) {
          errorMessage += ' (all retry attempts exhausted)';
        }
      } else {
        // Backend returned an unsuccessful response code
        switch (error.status) {
          case 0:
            errorMessage = 'Network Error: Unable to connect to the server. Please check your internet connection.';
            break;
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
          case 408:
            errorMessage = 'Request Timeout: The server took too long to respond.';
            break;
          case 429:
            errorMessage = 'Too Many Requests: Please slow down and try again later.';
            break;
          case 500:
            errorMessage = 'Internal Server Error: Please try again later.';
            break;
          case 502:
            errorMessage = 'Bad Gateway: The server received an invalid response.';
            break;
          case 503:
            errorMessage = 'Service Unavailable: The service is temporarily unavailable.';
            break;
          case 504:
            errorMessage = 'Gateway Timeout: The server did not respond in time.';
            break;
          default:
            if (error.error?.message) {
              errorMessage = error.error.message;
            } else {
              errorMessage = `Error Code: ${error.status}\nMessage: ${error.statusText}`;
            }
        }

        if (isRetryExhausted && RETRYABLE_STATUS_CODES.includes(error.status)) {
          errorMessage += ' (all retry attempts exhausted)';
        }
      }

      // Skip error logging/tracking for expected failures in development
      const isDemoEndpoint = req.url.includes('/demo/api/v1/demo');
      const isAuditEndpoint = req.url.includes('/audit/events');
      const isComplianceEndpoint = req.url.includes('/api/v1/compliance/errors');

      // Log error for debugging (PHI-filtered by LoggerService)
      if (!isDemoEndpoint && !isAuditEndpoint && !isComplianceEndpoint) {
        logger.error('HTTP Error', {
          url: req.url,
          method: req.method,
          status: error.status,
          statusText: error.statusText,
          message: errorMessage,
          retryAttempts: retryAttempt,
          error: error.error,
        });

        if (error.status >= 500 || error.status === 0) {
          sentryService.captureHttpError(error, {
            requestUrl: req.url,
            requestMethod: req.method,
            retryAttempts: retryAttempt,
          });
        }
      }

      // Track error for compliance validation if enabled
      if (COMPLIANCE_CONFIG.enableErrorTracking && !isDemoEndpoint && !isAuditEndpoint && !isComplianceEndpoint) {
        const errorCode = mapHttpStatusToErrorCode(error.status);
        const severity = mapHttpStatusToSeverity(error.status);
        const currentUser = authService.currentUserValue;
        const tenantId = authService.getTenantId();

        errorValidationService.trackError(error, {
          service: extractServiceName(req.url),
          endpoint: req.url,
          operation: `${req.method} ${req.url}`,
          errorCode,
          severity,
          userId: currentUser?.id,
          tenantId: tenantId ?? undefined,
          additionalData: {
            retryAttempts: retryAttempt,
            status: error.status,
            statusText: error.statusText,
          },
        });
      }

      // Re-throw the error with user-friendly message
      return throwError(() => ({
        ...error,
        userMessage: errorMessage,
        retryAttempts: retryAttempt,
      }));
    })
  );
};

/**
 * Map HTTP status code to ErrorCode
 */
function mapHttpStatusToErrorCode(status: number): ErrorCode {
  if (status === 0) return ErrorCode.NETWORK_ERROR;
  if (status === 401) return ErrorCode.UNAUTHORIZED;
  if (status === 403) return ErrorCode.FORBIDDEN;
  if (status === 404) return ErrorCode.RESOURCE_NOT_FOUND;
  if (status >= 400 && status < 500) return ErrorCode.VALIDATION_ERROR;
  if (status >= 500) return ErrorCode.INTERNAL_ERROR;
  return ErrorCode.UNKNOWN_ERROR;
}

/**
 * Map HTTP status code to ErrorSeverity
 */
function mapHttpStatusToSeverity(status: number): ErrorSeverity {
  if (status === 0) return ErrorSeverity.ERROR; // Network errors are high severity
  if (status >= 500) return ErrorSeverity.CRITICAL; // Server errors are critical
  if (status === 401 || status === 403) return ErrorSeverity.ERROR; // Auth errors are high
  if (status === 404) return ErrorSeverity.WARNING; // Not found is medium
  if (status >= 400) return ErrorSeverity.WARNING; // Client errors are medium
  return ErrorSeverity.INFO;
}

/**
 * Extract service name from URL
 */
function extractServiceName(url: string): string {
  // Extract service name from URL patterns
  if (url.includes('/fhir/')) return 'FHIR Service';
  if (url.includes('/care-gap/')) return 'Care Gap Service';
  if (url.includes('/quality-measure/')) return 'Quality Measure Service';
  if (url.includes('/patient/')) return 'Patient Service';
  if (url.includes('/cql-engine/')) return 'CQL Engine Service';
  if (url.includes('/demo/')) return 'Demo Seeding Service';
  
  // Try to extract from path segments
  const segments = url.split('/').filter(s => s);
  if (segments.length > 0) {
    return segments[0].charAt(0).toUpperCase() + segments[0].slice(1) + ' Service';
  }
  
  return 'Unknown Service';
}
