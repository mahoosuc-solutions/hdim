import { Injectable, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpClient } from '@angular/common/http';
import { ErrorCode, ErrorSeverity } from '../models/error.model';
import { COMPLIANCE_CONFIG, isFallbackAllowed } from '../config/compliance.config';
import { LoggerService } from '../logger.service';
import { Subject, interval, Subscription, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

/**
 * Error context for compliance tracking
 */
export interface ErrorContext {
  service: string;
  endpoint?: string;
  operation: string;
  errorCode: ErrorCode;
  severity: ErrorSeverity;
  userId?: string;
  tenantId?: string;
  additionalData?: Record<string, unknown>;
}

/**
 * Tracked error entry
 */
export interface TrackedError {
  id: string;
  timestamp: Date;
  error: Error | HttpErrorResponse;
  context: ErrorContext;
  message: string;
  stack?: string;
}

/**
 * Error summary statistics
 */
export interface ErrorSummary {
  totalErrors: number;
  errorsByService: Map<string, number>;
  errorsByCode: Map<ErrorCode, number>;
  errorsBySeverity: Map<ErrorSeverity, number>;
  recentErrors: TrackedError[];
  failedEndpoints: Set<string>;
  complianceScore: number; // 0-100, based on error rate and severity
}

/**
 * Compliance report
 */
export interface ComplianceReport {
  generatedAt: Date;
  summary: ErrorSummary;
  errors: TrackedError[];
  recommendations: string[];
}

/**
 * Error alert for threshold violations
 */
export interface ErrorAlert {
  type: 'critical-threshold' | 'error-rate-threshold';
  message: string;
  severity: 'warning' | 'critical';
  timestamp: Date;
  details: {
    threshold: number;
    actual: number;
    service?: string;
  };
}

/**
 * Error Validation Service
 * 
 * Centralized service for error tracking and compliance validation.
 * Tracks all errors when compliance mode is enabled and provides
 * compliance reporting capabilities.
 */
@Injectable({
  providedIn: 'root',
})
export class ErrorValidationService implements OnDestroy {
  private readonly STORAGE_KEY = 'hdim-error-validation';
  private readonly MAX_STORED_ERRORS = 1000; // Limit stored errors to prevent memory issues
  private trackedErrors: TrackedError[] = [];
  private readonly logger: ReturnType<LoggerService['withContext']>;
  private syncSubscription?: Subscription;
  private retentionCleanupSubscription?: Subscription;
  private errorRateWindow: TrackedError[] = []; // Errors in last hour
  private alertSubject = new Subject<ErrorAlert>();
  public alerts$ = this.alertSubject.asObservable();

  constructor(
    private loggerService: LoggerService,
    private http: HttpClient
  ) {
    this.logger = loggerService.withContext('ErrorValidationService');
    this.loadFromStorage();
    this.startRetentionCleanup();
    if (COMPLIANCE_CONFIG.syncToBackend) {
      this.startBackendSync();
    }
  }

  ngOnDestroy(): void {
    this.syncSubscription?.unsubscribe();
    this.retentionCleanupSubscription?.unsubscribe();
  }

  /**
   * Track an error for compliance validation
   */
  trackError(error: Error | HttpErrorResponse, context: ErrorContext): void {
    if (!COMPLIANCE_CONFIG.enableErrorTracking) {
      return;
    }

    const trackedError: TrackedError = {
      id: this.generateErrorId(),
      timestamp: new Date(),
      error,
      context,
      message: this.extractErrorMessage(error),
      stack: error instanceof Error ? error.stack : undefined,
    };

    // Add to tracked errors
    this.trackedErrors.push(trackedError);
    this.errorRateWindow.push(trackedError);

    // Clean up old errors from rate window (keep last hour)
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
    this.errorRateWindow = this.errorRateWindow.filter(
      e => e.timestamp >= oneHourAgo
    );

    // Check error rate threshold
    this.checkErrorRateThreshold();

    // Check critical error threshold
    if (context.severity === ErrorSeverity.CRITICAL) {
      this.checkCriticalErrorThreshold();
    }

    // Limit stored errors
    if (this.trackedErrors.length > this.MAX_STORED_ERRORS) {
      this.trackedErrors = this.trackedErrors.slice(-this.MAX_STORED_ERRORS);
    }

    // Log error
    this.logger.error(
      `Error tracked: ${context.service}.${context.operation}`,
      {
        errorCode: context.errorCode,
        severity: context.severity,
        endpoint: context.endpoint,
        error: trackedError.message,
      }
    );

    // Persist to storage
    this.saveToStorage();
  }

  /**
   * Check if fallback is allowed for a service
   */
  isFallbackAllowed(serviceName: string): boolean {
    return isFallbackAllowed(serviceName);
  }

  /**
   * Get error summary statistics
   */
  getErrorSummary(): ErrorSummary {
    const errorsByService = new Map<string, number>();
    const errorsByCode = new Map<ErrorCode, number>();
    const errorsBySeverity = new Map<ErrorSeverity, number>();
    const failedEndpoints = new Set<string>();

    this.trackedErrors.forEach((error) => {
      // Count by service
      const serviceCount = errorsByService.get(error.context.service) || 0;
      errorsByService.set(error.context.service, serviceCount + 1);

      // Count by error code
      const codeCount = errorsByCode.get(error.context.errorCode) || 0;
      errorsByCode.set(error.context.errorCode, codeCount + 1);

      // Count by severity
      const severityCount = errorsBySeverity.get(error.context.severity) || 0;
      errorsBySeverity.set(error.context.severity, severityCount + 1);

      // Track failed endpoints
      if (error.context.endpoint) {
        failedEndpoints.add(error.context.endpoint);
      }
    });

    // Calculate compliance score (0-100)
    // Lower score = more errors, higher severity = lower score
    const complianceScore = this.calculateComplianceScore(
      this.trackedErrors.length,
      errorsBySeverity
    );

    return {
      totalErrors: this.trackedErrors.length,
      errorsByService,
      errorsByCode,
      errorsBySeverity,
      recentErrors: this.trackedErrors.slice(-50).reverse(), // Last 50 errors, most recent first
      failedEndpoints,
      complianceScore,
    };
  }

  /**
   * Generate compliance report
   */
  validateCompliance(): ComplianceReport {
    const summary = this.getErrorSummary();
    const recommendations: string[] = [];

    // Generate recommendations based on error patterns
    if (summary.totalErrors > 100) {
      recommendations.push('High error rate detected. Review system health and service availability.');
    }

    if (summary.errorsBySeverity.get(ErrorSeverity.CRITICAL) || 0 > 0) {
      recommendations.push('Critical errors detected. Immediate attention required.');
    }

    if (summary.failedEndpoints.size > 10) {
      recommendations.push('Multiple endpoints failing. Check API gateway and service connectivity.');
    }

    if (summary.complianceScore < 70) {
      recommendations.push('Compliance score below threshold. Review error patterns and system stability.');
    }

    // Service-specific recommendations
    summary.errorsByService.forEach((count, service) => {
      if (count > 20) {
        recommendations.push(`Service "${service}" has high error rate (${count} errors). Review implementation.`);
      }
    });

    return {
      generatedAt: new Date(),
      summary,
      errors: this.trackedErrors,
      recommendations,
    };
  }

  /**
   * Clear all tracked errors
   */
  clearErrors(): void {
    this.trackedErrors = [];
    this.saveToStorage();
  }

  /**
   * Export errors as JSON
   */
  exportErrors(): string {
    const report = this.validateCompliance();
    return JSON.stringify(report, null, 2);
  }

  /**
   * Export errors as CSV
   */
  exportErrorsAsCsv(): string {
    const headers = ['Timestamp', 'Service', 'Operation', 'Endpoint', 'Error Code', 'Severity', 'Message'];
    const rows = this.trackedErrors.map((error) => [
      error.timestamp.toISOString(),
      error.context.service,
      error.context.operation,
      error.context.endpoint || '',
      error.context.errorCode,
      error.context.severity,
      this.escapeCsvField(error.message),
    ]);

    return [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');
  }

  /**
   * Calculate compliance score (0-100)
   */
  private calculateComplianceScore(
    totalErrors: number,
    errorsBySeverity: Map<ErrorSeverity, number>
  ): number {
    if (totalErrors === 0) {
      return 100;
    }

    // Base score starts at 100
    let score = 100;

    // Deduct points for total errors (logarithmic scale)
    score -= Math.min(30, Math.log10(totalErrors + 1) * 10);

    // Deduct points for severity
    // Note: ErrorSeverity enum values are: INFO, WARNING, ERROR, CRITICAL
    const critical = errorsBySeverity.get(ErrorSeverity.CRITICAL) || 0;
    const error = errorsBySeverity.get(ErrorSeverity.ERROR) || 0;
    const warning = errorsBySeverity.get(ErrorSeverity.WARNING) || 0;
    const info = errorsBySeverity.get(ErrorSeverity.INFO) || 0;

    score -= critical * 5; // -5 points per critical error
    score -= error * 2; // -2 points per error severity error
    score -= warning * 0.5; // -0.5 points per warning severity error
    score -= info * 0.1; // -0.1 points per info severity error

    return Math.max(0, Math.min(100, Math.round(score)));
  }

  /**
   * Extract error message from error object
   */
  private extractErrorMessage(error: Error | HttpErrorResponse): string {
    if (error instanceof Error) {
      return error.message;
    }
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.statusText) {
      return `${error.status} ${error.statusText}`;
    }
    return 'Unknown error';
  }

  /**
   * Generate unique error ID
   */
  private generateErrorId(): string {
    return `err-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Escape CSV field
   */
  private escapeCsvField(field: string): string {
    if (field.includes(',') || field.includes('"') || field.includes('\n')) {
      return `"${field.replace(/"/g, '""')}"`;
    }
    return field;
  }

  /**
   * Load errors from localStorage
   */
  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        this.trackedErrors = parsed.map((e: any) => ({
          id: e.id,
          timestamp: new Date(e.timestamp),
          context: e.context,
          message: e.message,
          stack: e.stack,
          error: new Error(e.message), // Reconstruct error object
        }));
        
        // Restore error rate window (last hour)
        const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
        this.errorRateWindow = this.trackedErrors.filter(
          e => e.timestamp >= oneHourAgo
        );
      }
    } catch (error) {
      this.logger.warn('Failed to load errors from storage', error);
    }
  }

  /**
   * Save errors to localStorage
   */
  private saveToStorage(): void {
    try {
      // Only save essential data (errors can't be fully serialized)
      const serializable = this.trackedErrors.map((e) => ({
        id: e.id,
        timestamp: e.timestamp.toISOString(),
        context: e.context,
        message: e.message,
        stack: e.stack,
      }));
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(serializable));
    } catch (error) {
      this.logger.warn('Failed to save errors to storage', error);
    }
  }

  /**
   * Start retention cleanup timer
   */
  private startRetentionCleanup(): void {
    const retentionDays = COMPLIANCE_CONFIG.errorRetentionDays || 30;
    // Run cleanup every hour
    this.retentionCleanupSubscription = interval(60 * 60 * 1000).subscribe(() => {
      this.cleanupOldErrors(retentionDays);
    });
    // Run initial cleanup
    this.cleanupOldErrors(retentionDays);
  }

  /**
   * Clean up errors older than retention period
   */
  private cleanupOldErrors(retentionDays: number): void {
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - retentionDays);

    const beforeCount = this.trackedErrors.length;
    this.trackedErrors = this.trackedErrors.filter(
      error => error.timestamp >= cutoffDate
    );
    const afterCount = this.trackedErrors.length;

    if (beforeCount !== afterCount) {
      this.logger.info(
        `Cleaned up ${beforeCount - afterCount} errors older than ${retentionDays} days`
      );
      this.saveToStorage();
    }
  }

  /**
   * Start backend sync timer
   */
  private startBackendSync(): void {
    const intervalMs = COMPLIANCE_CONFIG.syncIntervalMs || 60000;
    this.syncSubscription = interval(intervalMs).subscribe(() => {
      this.syncToBackend();
    });
  }

  /**
   * Sync errors to backend service
   */
  private syncToBackend(): void {
    if (!COMPLIANCE_CONFIG.syncToBackend || this.trackedErrors.length === 0) {
      return;
    }

    // Get errors that haven't been synced yet (we'll track this with a flag)
    // For now, sync all errors and let backend handle deduplication
    const errorsToSync = this.trackedErrors.slice(-100); // Sync last 100 errors

    const payload = {
      errors: errorsToSync.map(e => ({
        id: e.id,
        timestamp: e.timestamp.toISOString(),
        context: e.context,
        message: e.message,
        stack: e.stack,
      })),
      syncedAt: new Date().toISOString(),
    };

    // Use gateway-clinical-service URL (port 8080) for compliance endpoint
    // COMPLIANCE_URL already includes /api/v1/compliance, so just append /errors
    const baseUrl = API_CONFIG.COMPLIANCE_URL || API_CONFIG.API_GATEWAY_URL || 'http://localhost:8080';
    const url = baseUrl.endsWith('/api/v1/compliance') 
      ? `${baseUrl}/errors`
      : `${baseUrl}/api/v1/compliance/errors`;
    
    this.http.post(url, payload).pipe(
      catchError((error) => {
        this.logger.warn('Failed to sync errors to backend', error);
        return of(null);
      })
    ).subscribe(() => {
      this.logger.debug(`Synced ${errorsToSync.length} errors to backend`);
    });
  }

  /**
   * Check critical error threshold
   */
  private checkCriticalErrorThreshold(): void {
    const threshold = COMPLIANCE_CONFIG.criticalErrorThreshold || 10;
    const criticalCount = this.trackedErrors.filter(
      e => e.context.severity === ErrorSeverity.CRITICAL
    ).length;

    if (criticalCount >= threshold) {
      const alert: ErrorAlert = {
        type: 'critical-threshold',
        message: `Critical error threshold exceeded: ${criticalCount} critical errors (threshold: ${threshold})`,
        severity: 'critical',
        timestamp: new Date(),
        details: {
          threshold,
          actual: criticalCount,
        },
      };
      this.alertSubject.next(alert);
      this.logger.error(alert.message);
    }
  }

  /**
   * Check error rate threshold
   */
  private checkErrorRateThreshold(): void {
    const threshold = COMPLIANCE_CONFIG.errorRateThreshold || 100;
    const errorCount = this.errorRateWindow.length;

    if (errorCount >= threshold) {
      const alert: ErrorAlert = {
        type: 'error-rate-threshold',
        message: `Error rate threshold exceeded: ${errorCount} errors in the last hour (threshold: ${threshold})`,
        severity: 'warning',
        timestamp: new Date(),
        details: {
          threshold,
          actual: errorCount,
        },
      };
      this.alertSubject.next(alert);
      this.logger.warn(alert.message);
    }
  }
}
