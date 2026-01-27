import { Injectable } from '@angular/core';
import { LoggerService } from '../logger.service';
import { HttpClient } from '@angular/common/http';
import { LoggerService } from '../logger.service';
import { Observable, of, Subject } from 'rxjs';
import { LoggerService } from '../logger.service';
import { catchError, debounceTime, bufferTime, filter, tap } from 'rxjs/operators';
import { LoggerService } from '../logger.service';
import { AuthService } from './auth.service';
import { LoggerService } from '../logger.service';
import { API_CONFIG } from '../config/api.config';
import { LoggerService } from '../logger.service';

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
  private readonly logger: any;
  private readonly AUDIT_ENDPOINT = '/audit/events';
  private readonly BATCH_INTERVAL_MS = 5000; // Batch events every 5 seconds
  private readonly MAX_BUFFER_SIZE = 50; // Maximum events to buffer before force flush
  private readonly LOCAL_STORAGE_KEY = 'healthdata_pending_audit_events';

  private eventBuffer$ = new Subject<AuditEvent>();
  private pendingEvents: AuditEvent[] = [];

  constructor(
    private loggerService: LoggerService,
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.logger = this.loggerService.withContext(\'AuditService');
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
        this.logger.warn('[AuditService] Failed to send audit batch, storing locally:', error);
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
        this.logger.warn('[AuditService] Failed to log immediate event:', error);
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
   * Log session timeout event
   * HIPAA §164.312(a)(2)(iii) - Automatic Logoff Audit Requirement
   *
   * This method provides the audit trail required by HIPAA to prove that
   * automatic logoff occurred after a period of inactivity.
   */
  logSessionTimeout(params: {
    reason: 'IDLE_TIMEOUT' | 'EXPLICIT_LOGOUT' | 'TOKEN_EXPIRED';
    idleDurationMinutes?: number;
    warningShown?: boolean;
  }): void {
    this.log({
      action: AuditAction.LOGOUT,
      outcome: AuditOutcome.SUCCESS,
      methodName: 'sessionTimeout',
      purposeOfUse: 'AUTHENTICATION',
      metadata: {
        sessionTimeoutReason: params.reason,
        idleDurationMinutes: params.idleDurationMinutes,
        warningShown: params.warningShown,
        automaticLogoff: params.reason === 'IDLE_TIMEOUT',
        complianceNote: 'HIPAA §164.312(a)(2)(iii) - Automatic Logoff',
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

  // ==================== QA Audit Dashboard Methods ====================

  /**
   * Get AI decision review queue for QA analysts
   */
  getReviewQueue(filters: {
    agentType?: string;
    minConfidence?: number;
    maxConfidence?: number;
    startDate?: string;
    endDate?: string;
    includeReviewed?: boolean;
    page?: number;
    size?: number;
  }): Observable<any> {
    const params = new URLSearchParams();
    if (filters.agentType) params.append('agentType', filters.agentType);
    if (filters.minConfidence !== undefined) params.append('minConfidence', filters.minConfidence.toString());
    if (filters.maxConfidence !== undefined) params.append('maxConfidence', filters.maxConfidence.toString());
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.includeReviewed !== undefined) params.append('includeReviewed', filters.includeReviewed.toString());
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review-queue?${params.toString()}`
      : `/api/v1/audit/ai/qa/review-queue?${params.toString()}`;

    return this.http.get<any>(url);
  }

  /**
   * Get QA metrics dashboard data
   */
  getQAMetrics(dateRange?: { startDate: string; endDate: string }): Observable<any> {
    let url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/metrics`
      : `/api/v1/audit/ai/qa/metrics`;

    if (dateRange) {
      const params = new URLSearchParams({
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
      });
      url += `?${params.toString()}`;
    }

    return this.http.get<any>(url);
  }

  /**
   * Get QA trend analysis data
   */
  getTrendData(): Observable<any> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/trends`
      : `/api/v1/audit/ai/qa/trends`;

    return this.http.get<any>(url);
  }

  /**
   * Approve an AI decision after QA review
   */
  approveReview(id: string, request: { reviewNotes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}/approve`
      : `/api/v1/audit/ai/qa/review/${id}/approve`;

    return this.http.post<void>(url, request);
  }

  /**
   * Reject an AI decision after QA review
   */
  rejectReview(id: string, request: { rejectionReason: string; reviewNotes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}/reject`
      : `/api/v1/audit/ai/qa/review/${id}/reject`;

    return this.http.post<void>(url, request);
  }

  /**
   * Flag an AI decision for manual escalation
   */
  flagReview(id: string, request: { escalationReason: string; severity: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}/flag`
      : `/api/v1/audit/ai/qa/review/${id}/flag`;

    return this.http.post<void>(url, request);
  }

  /**
   * Mark an AI decision as false positive
   */
  markFalsePositive(id: string): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}/false-positive`
      : `/api/v1/audit/ai/qa/review/${id}/false-positive`;

    return this.http.post<void>(url, {});
  }

  /**
   * Mark an AI decision as false negative
   */
  markFalseNegative(id: string): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}/false-negative`
      : `/api/v1/audit/ai/qa/review/${id}/false-negative`;

    return this.http.post<void>(url, {});
  }

  /**
   * Get detailed review information for a specific decision
   */
  getReviewDetail(id: string): Observable<any> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/review/${id}`
      : `/api/v1/audit/ai/qa/review/${id}`;

    return this.http.get<any>(url);
  }

  /**
   * Export QA report (returns blob for file download)
   */
  exportQAReport(): Observable<Blob> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/qa/report/export`
      : `/api/v1/audit/ai/qa/report/export`;

    return this.http.get(url, { responseType: 'blob' });
  }

  // ==================== Clinical Audit Dashboard Methods ====================

  /**
   * Get clinical AI decisions (filtered by CLINICAL_* agent types)
   */
  getClinicalDecisions(filters: {
    agentType?: string;
    priority?: string;
    status?: string;
    patientId?: string;
    startDate?: string;
    endDate?: string;
    decisionType?: string;
    page?: number;
    size?: number;
  }): Observable<any> {
    const params = new URLSearchParams();

    // Filter for clinical agent types
    const agentTypeFilter = filters.agentType || 'CLINICAL_';
    params.append('agentType', agentTypeFilter);

    if (filters.priority) params.append('priority', filters.priority);
    if (filters.status) params.append('status', filters.status);
    if (filters.patientId) params.append('patientId', filters.patientId);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.decisionType) params.append('decisionType', filters.decisionType);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/decisions?${params.toString()}`
      : `/api/v1/audit/ai/decisions?${params.toString()}`;

    return this.http.get<any>(url);
  }

  /**
   * Get clinical metrics
   */
  getClinicalMetrics(dateRange?: { startDate: string; endDate: string }): Observable<any> {
    let url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/clinical/metrics`
      : `/api/v1/audit/clinical/metrics`;

    if (dateRange) {
      const params = new URLSearchParams({
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
      });
      url += `?${params.toString()}`;
    }

    return this.http.get<any>(url);
  }

  /**
   * Accept a clinical AI recommendation
   */
  acceptClinicalRecommendation(id: string, request: { clinicalNotes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/clinical/decisions/${id}/accept`
      : `/api/v1/clinical/decisions/${id}/accept`;

    return this.http.post<void>(url, request);
  }

  /**
   * Reject a clinical AI recommendation with rationale
   */
  rejectClinicalRecommendation(id: string, request: { clinicalRationale: string; clinicalNotes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/clinical/decisions/${id}/reject`
      : `/api/v1/clinical/decisions/${id}/reject`;

    return this.http.post<void>(url, request);
  }

  /**
   * Modify a clinical AI recommendation with clinical judgment
   */
  modifyClinicalRecommendation(id: string, request: { modifications: string; clinicalNotes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/clinical/decisions/${id}/modify`
      : `/api/v1/clinical/decisions/${id}/modify`;

    return this.http.post<void>(url, request);
  }

  /**
   * Export clinical audit report (returns blob for file download)
   */
  exportClinicalReport(): Observable<Blob> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/clinical/report/export`
      : `/api/v1/audit/clinical/report/export`;

    return this.http.get(url, { responseType: 'blob' });
  }

  // ==================== MPI Audit Dashboard Methods ====================

  /**
   * Get MPI audit events (filtered by MPI_* action types)
   */
  getMPIEvents(filters: {
    actionType?: string;
    eventType?: string;
    tenantId?: string;
    userId?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }): Observable<any> {
    const params = new URLSearchParams();

    // Filter for MPI action types
    const actionTypeFilter = filters.actionType || 'MPI_';
    params.append('actionType', actionTypeFilter);

    if (filters.eventType && filters.eventType !== 'all') params.append('eventType', filters.eventType);
    if (filters.tenantId && filters.tenantId !== 'all') params.append('tenantId', filters.tenantId);
    if (filters.userId) params.append('userId', filters.userId);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/ai/user-actions?${params.toString()}`
      : `/api/v1/audit/ai/user-actions?${params.toString()}`;

    return this.http.get<any>(url);
  }

  /**
   * Get MPI metrics
   */
  getMPIMetrics(dateRange?: { startDate: string; endDate: string }): Observable<any> {
    let url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/mpi/metrics`
      : `/api/v1/audit/mpi/metrics`;

    if (dateRange) {
      const params = new URLSearchParams({
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
      });
      url += `?${params.toString()}`;
    }

    return this.http.get<any>(url);
  }

  /**
   * Validate a patient merge operation
   */
  validateMerge(mergeId: string): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/mpi/merges/${mergeId}/validate`
      : `/api/v1/mpi/merges/${mergeId}/validate`;

    return this.http.post<void>(url, {});
  }

  /**
   * Rollback a patient merge operation
   */
  rollbackMerge(mergeId: string): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/mpi/merges/${mergeId}/rollback`
      : `/api/v1/mpi/merges/${mergeId}/rollback`;

    return this.http.post<void>(url, {});
  }

  /**
   * Resolve a data quality issue
   */
  resolveDataQualityIssue(issueId: string, resolution?: { notes?: string }): Observable<void> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/mpi/data-quality/${issueId}/resolve`
      : `/api/v1/mpi/data-quality/${issueId}/resolve`;

    return this.http.post<void>(url, resolution || {});
  }

  /**
   * Export MPI audit report (returns blob for file download)
   */
  exportMPIReport(): Observable<Blob> {
    const url = API_CONFIG.USE_API_GATEWAY
      ? `${API_CONFIG.API_GATEWAY_URL}/api/v1/audit/mpi/report/export`
      : `/api/v1/audit/mpi/report/export`;

    return this.http.get(url, { responseType: 'blob' });
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
