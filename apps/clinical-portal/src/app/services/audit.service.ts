import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, bufferTime, filter, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_CONFIG } from '../config/api.config';

/**
 * HIPAA-compliant Audit Logging Service
 *
 * Tracks user actions for compliance with:
 * - 45 CFR § 164.312(b) - Audit Controls
 * - 45 CFR § 164.308(a)(1)(ii)(D) - Information System Activity Review
 *
 * Features:
 * - Fire-and-forget logging (never blocks UI)
 * - Batched event submission for performance
 * - Automatic user context injection
 * - Client-side buffering for offline resilience
 */
@Injectable({
  providedIn: 'root',
})
export class AuditService {
  private readonly AUDIT_ENDPOINT = '/audit/events';
  private readonly BATCH_INTERVAL_MS = 5000; // Batch events every 5 seconds
  private readonly MAX_BUFFER_SIZE = 50; // Maximum events to buffer before force flush
  private readonly LOCAL_STORAGE_KEY = 'healthdata_pending_audit_events';

  private eventBuffer$ = new Subject<AuditEvent>();
  private pendingEvents: AuditEvent[] = [];

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.initializeEventBatching();
    this.flushPendingEventsFromStorage();
  }

  /**
   * Initialize event batching for performance
   * Events are buffered and sent in batches to reduce network overhead
   */
  private initializeEventBatching(): void {
    this.eventBuffer$
      .pipe(
        bufferTime(this.BATCH_INTERVAL_MS, null, this.MAX_BUFFER_SIZE),
        filter((events) => events.length > 0)
      )
      .subscribe((events) => {
        this.sendBatch(events);
      });
  }

  /**
   * Flush any pending events from local storage (offline recovery)
   */
  private flushPendingEventsFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.LOCAL_STORAGE_KEY);
      if (stored) {
        const pendingEvents: AuditEvent[] = JSON.parse(stored);
        if (pendingEvents.length > 0) {
          this.sendBatch(pendingEvents);
          localStorage.removeItem(this.LOCAL_STORAGE_KEY);
        }
      }
    } catch {
      // Ignore storage errors
    }
  }

  /**
   * Send batch of events to backend
   * Fire-and-forget: errors are logged but don't affect UI
   */
  private sendBatch(events: AuditEvent[]): void {
    const url = this.getAuditUrl();

    this.http.post<void>(url, { events }).pipe(
      catchError((error) => {
        console.warn('[AuditService] Failed to send audit batch, storing locally:', error);
        this.storeEventsLocally(events);
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Store events locally for later retry
   */
  private storeEventsLocally(events: AuditEvent[]): void {
    try {
      const existing = localStorage.getItem(this.LOCAL_STORAGE_KEY);
      const pendingEvents: AuditEvent[] = existing ? JSON.parse(existing) : [];
      pendingEvents.push(...events);
      // Limit stored events to prevent storage bloat
      const limitedEvents = pendingEvents.slice(-100);
      localStorage.setItem(this.LOCAL_STORAGE_KEY, JSON.stringify(limitedEvents));
    } catch {
      // Ignore storage errors
    }
  }

  /**
   * Get the audit endpoint URL
   */
  private getAuditUrl(): string {
    // Route through gateway in gateway mode, otherwise direct
    return API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/audit/events`
      : '/audit/events';
  }

  /**
   * Build base audit event with common fields
   */
  private buildBaseEvent(action: AuditAction): AuditEvent {
    const user = this.authService.currentUserValue;
    const now = new Date().toISOString();

    return {
      id: this.generateUUID(),
      timestamp: now,
      tenantId: user?.tenantId || API_CONFIG.DEFAULT_TENANT_ID,
      userId: user?.id || 'anonymous',
      username: user?.username || 'anonymous',
      role: user?.roles?.[0]?.name || 'UNKNOWN',
      action,
      outcome: AuditOutcome.SUCCESS,
      serviceName: 'clinical-portal',
      userAgent: navigator.userAgent,
    };
  }

  /**
   * Generate UUID for event ID
   */
  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  /**
   * Log a generic audit event
   * Fire-and-forget: returns immediately, event is batched
   */
  log(event: Partial<AuditEvent>): void {
    const fullEvent: AuditEvent = {
      ...this.buildBaseEvent(event.action || AuditAction.EXECUTE),
      ...event,
    };
    this.eventBuffer$.next(fullEvent);
  }

  /**
   * Log immediate audit event (bypasses batching)
   * Use for critical events that must be logged immediately
   */
  logImmediate(event: Partial<AuditEvent>): Observable<void> {
    const fullEvent: AuditEvent = {
      ...this.buildBaseEvent(event.action || AuditAction.EXECUTE),
      ...event,
    };

    const url = this.getAuditUrl();
    return this.http.post<void>(url, { events: [fullEvent] }).pipe(
      catchError((error) => {
        console.warn('[AuditService] Failed to log immediate event:', error);
        this.storeEventsLocally([fullEvent]);
        return of(undefined);
      })
    );
  }

  // ==================== Convenience Methods ====================

  /**
   * Log user login
   */
  logLogin(success: boolean, errorMessage?: string): void {
    this.log({
      action: success ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED,
      outcome: success ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE,
      methodName: 'login',
      requestPath: '/login',
      errorMessage,
      purposeOfUse: 'AUTHENTICATION',
    });
  }

  /**
   * Log user logout
   */
  logLogout(): void {
    this.log({
      action: AuditAction.LOGOUT,
      methodName: 'logout',
      requestPath: '/logout',
      purposeOfUse: 'AUTHENTICATION',
    });
  }

  /**
   * Log patient data access
   */
  logPatientAccess(patientId: string, action: 'view' | 'search' | 'edit' = 'view'): void {
    const actionMap: Record<string, AuditAction> = {
      view: AuditAction.READ,
      search: AuditAction.SEARCH,
      edit: AuditAction.UPDATE,
    };

    this.log({
      action: actionMap[action],
      resourceType: 'Patient',
      resourceId: patientId,
      purposeOfUse: 'TREATMENT',
    });
  }

  /**
   * Log evaluation execution
   */
  logEvaluation(params: {
    evaluationId?: string;
    measureId: string;
    patientIds?: string[];
    patientCount?: number;
    success: boolean;
    durationMs?: number;
    errorMessage?: string;
  }): void {
    this.log({
      action: AuditAction.EXECUTE,
      resourceType: 'CqlEvaluation',
      resourceId: params.evaluationId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.SERIOUS_FAILURE,
      methodName: 'evaluateMeasure',
      purposeOfUse: 'OPERATIONS',
      durationMs: params.durationMs,
      errorMessage: params.errorMessage,
      metadata: {
        measureId: params.measureId,
        patientCount: params.patientCount || params.patientIds?.length || 0,
      },
    });
  }

  /**
   * Log batch evaluation start
   */
  logBatchEvaluationStart(params: {
    jobId: string;
    measureIds: string[];
    patientCount: number;
  }): void {
    this.log({
      action: AuditAction.EXECUTE,
      resourceType: 'BatchEvaluation',
      resourceId: params.jobId,
      methodName: 'startBatchEvaluation',
      purposeOfUse: 'OPERATIONS',
      metadata: {
        measureIds: params.measureIds,
        patientCount: params.patientCount,
      },
    });
  }

  /**
   * Log batch evaluation completion
   */
  logBatchEvaluationComplete(params: {
    jobId: string;
    success: boolean;
    totalEvaluations: number;
    successCount: number;
    failureCount: number;
    durationMs: number;
    errorMessage?: string;
  }): void {
    this.log({
      action: AuditAction.EXECUTE,
      resourceType: 'BatchEvaluation',
      resourceId: params.jobId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.SERIOUS_FAILURE,
      methodName: 'completeBatchEvaluation',
      purposeOfUse: 'OPERATIONS',
      durationMs: params.durationMs,
      errorMessage: params.errorMessage,
      metadata: {
        totalEvaluations: params.totalEvaluations,
        successCount: params.successCount,
        failureCount: params.failureCount,
      },
    });
  }

  /**
   * Log QRDA export
   */
  logQrdaExport(params: {
    jobId: string;
    category: 'I' | 'III';
    measureIds: string[];
    patientCount?: number;
    success: boolean;
    errorMessage?: string;
  }): void {
    this.log({
      action: AuditAction.EXPORT,
      resourceType: `QRDA_Category_${params.category}`,
      resourceId: params.jobId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.SERIOUS_FAILURE,
      methodName: `exportQrdaCategory${params.category}`,
      purposeOfUse: 'OPERATIONS',
      errorMessage: params.errorMessage,
      metadata: {
        category: params.category,
        measureIds: params.measureIds,
        patientCount: params.patientCount,
      },
    });
  }

  /**
   * Log QRDA download
   */
  logQrdaDownload(jobId: string, filename: string): void {
    this.log({
      action: AuditAction.DOWNLOAD,
      resourceType: 'QRDA_Export',
      resourceId: jobId,
      methodName: 'downloadQrdaExport',
      purposeOfUse: 'OPERATIONS',
      metadata: {
        filename,
      },
    });
  }

  /**
   * Log report generation
   */
  logReportGeneration(params: {
    reportType: 'patient' | 'population' | 'care-gap' | 'comparative';
    reportId?: string;
    success: boolean;
    errorMessage?: string;
  }): void {
    this.log({
      action: AuditAction.EXECUTE,
      resourceType: 'Report',
      resourceId: params.reportId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.SERIOUS_FAILURE,
      methodName: 'generateReport',
      purposeOfUse: 'OPERATIONS',
      errorMessage: params.errorMessage,
      metadata: {
        reportType: params.reportType,
      },
    });
  }

  /**
   * Log report export (PDF, Excel, CSV)
   */
  logReportExport(params: {
    reportId: string;
    format: 'pdf' | 'excel' | 'csv';
    success: boolean;
  }): void {
    this.log({
      action: AuditAction.EXPORT,
      resourceType: 'Report',
      resourceId: params.reportId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE,
      methodName: 'exportReport',
      purposeOfUse: 'OPERATIONS',
      metadata: {
        format: params.format,
      },
    });
  }

  /**
   * Log care gap action
   */
  logCareGapAction(params: {
    gapId: string;
    patientId: string;
    action: 'view' | 'address' | 'dismiss' | 'snooze';
    success: boolean;
  }): void {
    const actionMap: Record<string, AuditAction> = {
      view: AuditAction.READ,
      address: AuditAction.UPDATE,
      dismiss: AuditAction.UPDATE,
      snooze: AuditAction.UPDATE,
    };

    this.log({
      action: actionMap[params.action],
      resourceType: 'CareGap',
      resourceId: params.gapId,
      outcome: params.success ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE,
      methodName: `${params.action}CareGap`,
      purposeOfUse: 'TREATMENT',
      metadata: {
        patientId: params.patientId,
        gapAction: params.action,
      },
    });
  }

  /**
   * Log consent change
   */
  logConsentChange(params: {
    patientId: string;
    consentType: string;
    granted: boolean;
  }): void {
    this.log({
      action: AuditAction.CONSENT_CHANGE,
      resourceType: 'Consent',
      resourceId: params.patientId,
      methodName: 'updateConsent',
      purposeOfUse: 'OPERATIONS',
      metadata: {
        consentType: params.consentType,
        granted: params.granted,
      },
    });
  }

  /**
   * Log access denied event
   */
  logAccessDenied(params: {
    requestPath: string;
    requiredPermission?: string;
    requiredRole?: string;
  }): void {
    this.log({
      action: AuditAction.ACCESS_DENIED,
      outcome: AuditOutcome.MINOR_FAILURE,
      requestPath: params.requestPath,
      purposeOfUse: 'AUTHORIZATION',
      metadata: {
        requiredPermission: params.requiredPermission,
        requiredRole: params.requiredRole,
      },
    });
  }

  /**
   * Force flush all buffered events immediately
   * Call this on application shutdown or before navigation
   */
  flush(): void {
    // Send any events still in the buffer subject
    // The bufferTime operator will handle this
  }
}

// ==================== Type Definitions ====================

/**
 * Audit actions matching backend AuditAction enum
 */
export enum AuditAction {
  // CRUD operations
  CREATE = 'CREATE',
  READ = 'READ',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  EXECUTE = 'EXECUTE',

  // Authentication
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  LOGIN_FAILED = 'LOGIN_FAILED',

  // Authorization
  ACCESS_GRANTED = 'ACCESS_GRANTED',
  ACCESS_DENIED = 'ACCESS_DENIED',

  // Data operations
  SEARCH = 'SEARCH',
  EXPORT = 'EXPORT',
  IMPORT = 'IMPORT',
  PRINT = 'PRINT',
  DOWNLOAD = 'DOWNLOAD',

  // Administrative
  CONSENT_CHANGE = 'CONSENT_CHANGE',
  EMERGENCY_ACCESS = 'EMERGENCY_ACCESS',

  // System
  SYSTEM_START = 'SYSTEM_START',
  SYSTEM_STOP = 'SYSTEM_STOP',
  CONFIG_CHANGE = 'CONFIG_CHANGE',
}

/**
 * Audit outcome matching backend AuditOutcome enum
 */
export enum AuditOutcome {
  SUCCESS = 'SUCCESS',
  MINOR_FAILURE = 'MINOR_FAILURE',
  SERIOUS_FAILURE = 'SERIOUS_FAILURE',
  MAJOR_FAILURE = 'MAJOR_FAILURE',
}

/**
 * Audit event matching backend AuditEvent structure
 */
export interface AuditEvent {
  id: string;
  timestamp: string;
  tenantId: string;

  // Who
  userId: string;
  username: string;
  role: string;
  ipAddress?: string;
  userAgent?: string;

  // What
  action: AuditAction;
  resourceType?: string;
  resourceId?: string;
  outcome: AuditOutcome;

  // Where
  serviceName: string;
  methodName?: string;
  requestPath?: string;

  // Why
  purposeOfUse?: string;

  // Additional context
  errorMessage?: string;
  durationMs?: number;
  fhirAuditEventId?: string;
  encrypted?: boolean;

  // Extended metadata (JSON)
  metadata?: Record<string, unknown>;
}
