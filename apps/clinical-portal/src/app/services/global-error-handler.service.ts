import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { LoggerService } from './logger.service';
import { AuditService, AuditAction, AuditOutcome } from './audit.service';
import { environment } from '../../environments/environment';

/**
 * Global Error Handler - Catches all unhandled exceptions
 *
 * SECURITY FEATURES:
 * - Logs errors without exposing PHI (uses LoggerService with PHI filtering)
 * - Prevents application crashes from propagating to user
 * - Integrates with audit logging for security incident tracking
 * - Production mode: No stack traces in user-facing messages
 *
 * COMPLIANCE:
 * - HIPAA §164.312(b): All errors logged for incident response
 * - HIPAA §164.308(a)(1)(ii)(D): Information system activity review
 * - Audit trail for unexpected failures and security events
 *
 * ERROR CATEGORIES:
 * - HTTP errors (already handled by error.interceptor, but caught here as fallback)
 * - Component exceptions (e.g., undefined property access, null reference)
 * - Service exceptions (e.g., unhandled promise rejections)
 * - Third-party library errors
 *
 * INTEGRATION:
 * - Uses LoggerService for structured logging with PHI filtering
 * - Uses AuditService for compliance audit trails
 * - Placeholder for external error tracking (Sentry, LogRocket, etc.)
 *
 * USER EXPERIENCE:
 * - Development: Full error details in console
 * - Production: User-friendly error message, no stack traces
 * - Error boundary: Prevents full application crash
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private injector: Injector) {}

  handleError(error: Error | HttpErrorResponse): void {
    // Lazy-load services to avoid circular dependency issues
    // (GlobalErrorHandler is provided in app.config, services may depend on HTTP client)
    const logger = this.injector.get(LoggerService);
    const auditService = this.injector.get(AuditService);

    // Determine error type and context
    const isHttpError = error instanceof HttpErrorResponse;
    const errorMessage = this.getErrorMessage(error);
    const errorContext = this.getErrorContext(error);

    // Log to console in development (for debugging)
    if (!environment.production) {
      // eslint-disable-next-line no-console
      console.error('[Global Error Handler] Uncaught exception:', error);
    }

    // Log via LoggerService (with automatic PHI filtering)
    logger.withContext('GlobalErrorHandler').error(errorMessage, {
      name: error.name,
      message: errorMessage,
      stack: this.sanitizeStackTrace(error.stack),
      url: errorContext.url,
      httpStatus: isHttpError ? error.status : undefined,
      errorType: isHttpError ? 'HTTP' : 'Application',
    });

    // Audit critical errors (non-HTTP or HTTP 5xx)
    if (!isHttpError || (isHttpError && error.status >= 500)) {
      auditService.log({
        action: AuditAction.EXECUTE,
        resourceType: 'Application',
        outcome: AuditOutcome.SERIOUS_FAILURE,
        methodName: 'handleError',
        errorMessage: errorMessage,
        metadata: {
          errorType: error.name,
          httpStatus: isHttpError ? error.status : undefined,
          url: errorContext.url,
          component: errorContext.component,
        },
      });
    }

    // Send to external error tracking service (e.g., Sentry)
    if (environment.production) {
      this.sendToErrorTrackingService(error);
    }

    // Show user-friendly error message
    this.showUserFriendlyError(errorMessage, isHttpError);
  }

  /**
   * Extract error message (safe for user display)
   */
  private getErrorMessage(error: Error | HttpErrorResponse): string {
    if (error instanceof HttpErrorResponse) {
      // HTTP errors
      if (error.status === 0) {
        return 'Network error - unable to reach server';
      }
      if (error.status >= 400 && error.status < 500) {
        return error.error?.message || `Client error (${error.status})`;
      }
      if (error.status >= 500) {
        return `Server error (${error.status})`;
      }
      return error.message;
    }

    // Application errors
    return error.message || 'An unexpected error occurred';
  }

  /**
   * Get error context (URL, component, etc.)
   */
  private getErrorContext(error: Error | HttpErrorResponse): {
    url: string;
    component?: string;
  } {
    const url = error instanceof HttpErrorResponse
      ? error.url || window.location.href
      : window.location.href;

    // Try to extract component name from stack trace
    const component = this.extractComponentFromStack(error.stack);

    return { url, component };
  }

  /**
   * Extract component name from stack trace
   */
  private extractComponentFromStack(stack?: string): string | undefined {
    if (!stack) return undefined;

    // Look for Angular component pattern: ComponentName.methodName
    const match = stack.match(/at\s+(\w+Component)\./);
    return match ? match[1] : undefined;
  }

  /**
   * Sanitize stack trace for logging
   * - Truncate to first 500 characters
   * - Remove potential PHI (using basic patterns)
   */
  private sanitizeStackTrace(stack?: string): string {
    if (!stack) return '';

    // Truncate
    let sanitized = stack.substring(0, 500);

    // Remove potential PHI patterns (basic sanitization)
    // Note: LoggerService also performs PHI filtering, this is defense-in-depth
    const phiPatterns = [
      /\b\d{3}-\d{2}-\d{4}\b/g,  // SSN
      /\b\d{9}\b/g,               // SSN without dashes
      /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g,  // Email
    ];

    for (const pattern of phiPatterns) {
      sanitized = sanitized.replace(pattern, '[REDACTED]');
    }

    return sanitized;
  }

  /**
   * Send to external error tracking service
   * Placeholder for integration with Sentry, LogRocket, Datadog, etc.
   */
  private sendToErrorTrackingService(error: Error | HttpErrorResponse): void {
    // Example Sentry integration:
    // if (typeof Sentry !== 'undefined') {
    //   Sentry.captureException(error, {
    //     level: 'error',
    //     tags: {
    //       errorType: error instanceof HttpErrorResponse ? 'HTTP' : 'Application',
    //     },
    //     extra: {
    //       url: window.location.href,
    //       httpStatus: error instanceof HttpErrorResponse ? error.status : undefined,
    //     },
    //   });
    // }

    // Example LogRocket integration:
    // if (typeof LogRocket !== 'undefined') {
    //   LogRocket.captureException(error, {
    //     tags: {
    //       errorType: error instanceof HttpErrorResponse ? 'HTTP' : 'Application',
    //     },
    //   });
    // }

    // For now, just log that we would send to external service
    if (!environment.production) {
      // eslint-disable-next-line no-console
      console.log('[Global Error Handler] Would send to external error tracking service:', error.message);
    }
  }

  /**
   * Show user-friendly error message
   * - Development: Alert with error details
   * - Production: Generic error message via MatSnackBar (if available)
   */
  private showUserFriendlyError(message: string, isHttpError: boolean): void {
    // In production, show a user-friendly message
    // Note: We can't inject MatSnackBar directly due to circular dependency
    // So we use a simple alert for now (can be enhanced with dynamic injection)

    if (environment.production) {
      // Production: Generic message, no technical details
      alert(
        'An unexpected error occurred. Please refresh the page or contact support if the problem persists.'
      );
    } else {
      // Development: Show actual error message for debugging
      alert(`Development Error:\n\n${message}\n\nCheck console for details.`);
    }

    // TODO: Enhance with MatSnackBar
    // Try to get MatSnackBar dynamically (may fail if not yet initialized)
    // try {
    //   const snackBar = this.injector.get(MatSnackBar, null, { optional: true });
    //   if (snackBar) {
    //     snackBar.open(
    //       environment.production
    //         ? 'An unexpected error occurred. Please try again.'
    //         : `Error: ${message}`,
    //       'Close',
    //       {
    //         duration: 5000,
    //         panelClass: ['error-snackbar'],
    //       }
    //     );
    //     return;
    //   }
    // } catch {
    //   // MatSnackBar not available, fall back to alert
    // }
  }
}
