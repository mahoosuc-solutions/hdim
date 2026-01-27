/**
 * Nurse Workflow Service
 *
 * Provides unified API for nurse dashboard operations:
 * - Patient outreach logging
 * - Medication reconciliation workflow
 * - Patient education tracking
 * - Referral coordination management
 *
 * Interacts with nurse-workflow-service backend (port 8093)
 * Implements HttpClient + Observable + caching patterns
 * Follows HDIM Angular service conventions
 */

import { Injectable } from '@angular/core';
import { LoggerService } from '../logger.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { LoggerService } from '../logger.service';
import { Observable, throwError, BehaviorSubject, of } from 'rxjs';
import { LoggerService } from '../logger.service';
import { catchError, tap, map, switchMap, shareReplay } from 'rxjs/operators';
import { LoggerService } from '../logger.service';

import {
  OutreachLog,
  MedicationReconciliation,
  PatientEducationLog,
  ReferralCoordination,
  NurseWorkflowContext,
  PaginatedResponse,
  OutreachMetrics,
  MedicationReconciliationMetrics,
  PatientEducationMetrics,
  ReferralMetrics,
} from './nurse-workflow.models';

/**
 * Base URL for nurse workflow service
 * Configured via environment or api.config.ts
 */
const NURSE_WORKFLOW_BASE_URL = '/nurse-workflow/api/v1';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttlMs: number;
}

@Injectable({
  providedIn: 'root',
})
export class NurseWorkflowService {
  private readonly logger: any;
  private tenantContext$ = new BehaviorSubject<string | null>(null);
  private cache = new Map<string, CacheEntry<any>>();
  private readonly DEFAULT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

  constructor(
    private loggerService: LoggerService,private http: HttpClient) {
    this.logger = this.loggerService.withContext(\'NurseWorkflowService');}

  /**
   * Set multi-tenant context for all subsequent requests
   */
  setTenantContext(tenantId: string): void {
    this.tenantContext$.next(tenantId);
  }

  /**
   * Get current tenant context
   */
  getTenantContext(): string {
    const tenant = this.tenantContext$.value;
    if (!tenant) {
      throw new Error('Tenant context not set. Call setTenantContext() first.');
    }
    return tenant;
  }

  /**
   * Clear all cached data
   */
  invalidateCache(pattern?: string): void {
    if (pattern) {
      for (const key of this.cache.keys()) {
        if (key.includes(pattern)) {
          this.cache.delete(key);
        }
      }
    } else {
      this.cache.clear();
    }
  }

  // ==================== Outreach Log Methods ====================

  /**
   * Create outreach log
   */
  createOutreachLog(log: OutreachLog): Observable<OutreachLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs`;

    return this.http
      .post<OutreachLog>(url, { ...log, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('outreach')),
        catchError((error) => this.handleError(error, 'createOutreachLog'))
      );
  }

  /**
   * Get outreach log by ID
   */
  getOutreachLogById(id: string): Observable<OutreachLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/${id}`;
    const cacheKey = `outreach:${id}`;

    const cached = this.getFromCache<OutreachLog>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<OutreachLog>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((log) => this.setInCache(cacheKey, log)),
        catchError((error) => this.handleError(error, 'getOutreachLogById'))
      );
  }

  /**
   * Get outreach logs for patient
   */
  getPatientOutreachLogs(patientId: string, page: number, size: number): Observable<PaginatedResponse<OutreachLog>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/patient/${patientId}`;
    const cacheKey = `outreach:patient:${patientId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<OutreachLog>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<OutreachLog>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPatientOutreachLogs'))
      );
  }

  /**
   * Get outreach logs by outcome type
   */
  getOutreachLogsByOutcome(outcome: string, page: number, size: number): Observable<PaginatedResponse<OutreachLog>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/outcome/${outcome}`;
    const cacheKey = `outreach:outcome:${outcome}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<OutreachLog>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<OutreachLog>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getOutreachLogsByOutcome'))
      );
  }

  /**
   * Get pending outreach logs (scheduled but not completed)
   */
  getPendingOutreachLogs(page: number, size: number): Observable<PaginatedResponse<OutreachLog>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/pending`;
    const cacheKey = `outreach:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<OutreachLog>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<OutreachLog>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPendingOutreachLogs'))
      );
  }

  /**
   * Update outreach log
   */
  updateOutreachLog(id: string, log: Partial<OutreachLog>): Observable<OutreachLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/${id}`;

    return this.http
      .put<OutreachLog>(url, { ...log, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('outreach')),
        catchError((error) => this.handleError(error, 'updateOutreachLog'))
      );
  }

  /**
   * Delete outreach log
   */
  deleteOutreachLog(id: string): Observable<void> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/${id}`;

    return this.http
      .delete<void>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('outreach')),
        catchError((error) => this.handleError(error, 'deleteOutreachLog'))
      );
  }

  /**
   * Log a contact attempt
   */
  logContactAttempt(
    patientId: string,
    contactMethod: string,
    duration: number,
    notes: string
  ): Observable<{ success: boolean }> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/outreach-logs/contact-attempt`;

    return this.http
      .post<{ success: boolean }>(url, {
        patientId,
        contactMethod,
        duration,
        notes,
        tenantId,
        timestamp: new Date().toISOString()
      }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        catchError((error) => this.handleError(error, 'logContactAttempt'))
      );
  }

  // ==================== Medication Reconciliation Methods ====================

  /**
   * Start medication reconciliation
   */
  startMedicationReconciliation(medRec: MedicationReconciliation): Observable<MedicationReconciliation> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations`;

    return this.http
      .post<MedicationReconciliation>(url, { ...medRec, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('medication')),
        catchError((error) => this.handleError(error, 'startMedicationReconciliation'))
      );
  }

  /**
   * Complete medication reconciliation
   */
  completeMedicationReconciliation(medRec: MedicationReconciliation): Observable<MedicationReconciliation> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/complete`;

    return this.http
      .put<MedicationReconciliation>(url, { ...medRec, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('medication')),
        catchError((error) => this.handleError(error, 'completeMedicationReconciliation'))
      );
  }

  /**
   * Get medication reconciliation by ID
   */
  getMedicationReconciliationById(id: string): Observable<MedicationReconciliation> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/${id}`;
    const cacheKey = `medication:${id}`;

    const cached = this.getFromCache<MedicationReconciliation>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationReconciliation>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((medRec) => this.setInCache(cacheKey, medRec)),
        catchError((error) => this.handleError(error, 'getMedicationReconciliationById'))
      );
  }

  /**
   * Get pending medication reconciliations
   */
  getPendingMedicationReconciliations(page: number, size: number): Observable<PaginatedResponse<MedicationReconciliation>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/pending`;
    const cacheKey = `medication:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<MedicationReconciliation>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationReconciliation>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPendingMedicationReconciliations'))
      );
  }

  /**
   * Get medication reconciliations for patient
   */
  getPatientMedicationReconciliationHistory(patientId: string, page: number, size: number): Observable<PaginatedResponse<MedicationReconciliation>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/patient/${patientId}`;
    const cacheKey = `medication:patient:${patientId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<MedicationReconciliation>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationReconciliation>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPatientMedicationReconciliationHistory'))
      );
  }

  /**
   * Update medication reconciliation
   */
  updateMedicationReconciliation(id: string, medRec: MedicationReconciliation): Observable<MedicationReconciliation> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/${id}`;

    return this.http
      .put<MedicationReconciliation>(url, { ...medRec, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('medication')),
        catchError((error) => this.handleError(error, 'updateMedicationReconciliation'))
      );
  }

  /**
   * Get medication reconciliation metrics
   */
  getMedicationReconciliationMetrics(): Observable<MedicationReconciliationMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/medication-reconciliations/metrics/summary`;
    const cacheKey = 'medication:metrics';

    const cached = this.getFromCache<MedicationReconciliationMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationReconciliationMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) => this.setInCache(cacheKey, metrics, 10 * 60 * 1000)), // 10 min cache
        catchError((error) => this.handleError(error, 'getMedicationReconciliationMetrics'))
      );
  }

  // ==================== Patient Education Methods ====================

  /**
   * Get available education topics
   */
  getEducationTopics(): Observable<any[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/education-topics`;
    const cacheKey = 'education:topics';

    const cached = this.getFromCache<any[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<any[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((topics) => this.setInCache(cacheKey, topics, 30 * 60 * 1000)), // 30 min cache
        catchError((error: unknown) => this.handleError(error, 'getEducationTopics'))
      );
  }

  /**
   * Log patient education delivery
   */
  logPatientEducation(education: PatientEducationLog): Observable<PatientEducationLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education`;

    return this.http
      .post<PatientEducationLog>(url, { ...education, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('education')),
        catchError((error) => this.handleError(error, 'logPatientEducation'))
      );
  }

  /**
   * Get patient education log by ID
   */
  getPatientEducationLogById(id: string): Observable<PatientEducationLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/${id}`;
    const cacheKey = `education:${id}`;

    const cached = this.getFromCache<PatientEducationLog>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<PatientEducationLog>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((log) => this.setInCache(cacheKey, log)),
        catchError((error) => this.handleError(error, 'getPatientEducationLogById'))
      );
  }

  /**
   * Get patient education history
   */
  getPatientEducationHistory(patientId: string, page: number, size: number): Observable<PaginatedResponse<PatientEducationLog>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/patient/${patientId}`;
    const cacheKey = `education:patient:${patientId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<PatientEducationLog>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<PatientEducationLog>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPatientEducationHistory'))
      );
  }

  /**
   * Get education sessions with poor understanding
   */
  getEducationSessionsWithPoorUnderstanding(): Observable<PatientEducationLog[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/poor-understanding`;
    const cacheKey = 'education:poor-understanding';

    const cached = this.getFromCache<PatientEducationLog[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<PatientEducationLog[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((logs) => this.setInCache(cacheKey, logs)),
        catchError((error) => this.handleError(error, 'getEducationSessionsWithPoorUnderstanding'))
      );
  }

  /**
   * Update patient education log
   */
  updatePatientEducationLog(id: string, education: PatientEducationLog): Observable<PatientEducationLog> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/${id}`;

    return this.http
      .put<PatientEducationLog>(url, { ...education, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('education')),
        catchError((error) => this.handleError(error, 'updatePatientEducationLog'))
      );
  }

  /**
   * Delete patient education log
   */
  deletePatientEducationLog(id: string): Observable<void> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/${id}`;

    return this.http
      .delete<void>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('education')),
        catchError((error) => this.handleError(error, 'deletePatientEducationLog'))
      );
  }

  /**
   * Get patient education metrics
   */
  getPatientEducationMetrics(patientId: string): Observable<PatientEducationMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/patient-education/metrics/${patientId}`;
    const cacheKey = `education:metrics:${patientId}`;

    const cached = this.getFromCache<PatientEducationMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<PatientEducationMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) => this.setInCache(cacheKey, metrics, 10 * 60 * 1000)), // 10 min cache
        catchError((error) => this.handleError(error, 'getPatientEducationMetrics'))
      );
  }

  // ==================== Referral Coordination Methods ====================

  /**
   * Create referral
   */
  createReferral(referral: ReferralCoordination): Observable<ReferralCoordination> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations`;

    return this.http
      .post<ReferralCoordination>(url, { ...referral, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('referral')),
        catchError((error) => this.handleError(error, 'createReferral'))
      );
  }

  /**
   * Get referral by ID
   */
  getReferralById(id: string): Observable<ReferralCoordination> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/${id}`;
    const cacheKey = `referral:${id}`;

    const cached = this.getFromCache<ReferralCoordination>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ReferralCoordination>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((referral) => this.setInCache(cacheKey, referral)),
        catchError((error) => this.handleError(error, 'getReferralById'))
      );
  }

  /**
   * Get pending referrals
   */
  getPendingReferrals(page: number, size: number): Observable<PaginatedResponse<ReferralCoordination>> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/pending`;
    const cacheKey = `referral:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<ReferralCoordination>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http
      .get<PaginatedResponse<ReferralCoordination>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPendingReferrals'))
      );
  }

  /**
   * Get referrals awaiting scheduling
   */
  getReferralsAwaitingScheduling(): Observable<ReferralCoordination[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/awaiting-appointment-scheduling`;
    const cacheKey = 'referral:awaiting-scheduling';

    const cached = this.getFromCache<ReferralCoordination[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ReferralCoordination[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((referrals) => this.setInCache(cacheKey, referrals)),
        catchError((error) => this.handleError(error, 'getReferralsAwaitingScheduling'))
      );
  }

  /**
   * Get referrals awaiting results
   */
  getReferralsAwaitingResults(): Observable<ReferralCoordination[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/awaiting-results`;
    const cacheKey = 'referral:awaiting-results';

    const cached = this.getFromCache<ReferralCoordination[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ReferralCoordination[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((referrals) => this.setInCache(cacheKey, referrals)),
        catchError((error) => this.handleError(error, 'getReferralsAwaitingResults'))
      );
  }

  /**
   * Get urgent referrals awaiting scheduling
   */
  getUrgentReferralsAwaitingScheduling(): Observable<ReferralCoordination[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/urgent-awaiting-scheduling`;
    const cacheKey = 'referral:urgent-awaiting-scheduling';

    const cached = this.getFromCache<ReferralCoordination[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ReferralCoordination[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((referrals) => this.setInCache(cacheKey, referrals)),
        catchError((error) => this.handleError(error, 'getUrgentReferralsAwaitingScheduling'))
      );
  }

  /**
   * Update referral
   */
  updateReferral(id: string, referral: ReferralCoordination): Observable<ReferralCoordination> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/${id}`;

    return this.http
      .put<ReferralCoordination>(url, { ...referral, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('referral')),
        catchError((error) => this.handleError(error, 'updateReferral'))
      );
  }

  /**
   * Get referral metrics
   */
  getReferralMetrics(): Observable<ReferralMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referral-coordinations/metrics/summary`;
    const cacheKey = 'referral:metrics';

    const cached = this.getFromCache<ReferralMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ReferralMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) => this.setInCache(cacheKey, metrics, 10 * 60 * 1000)), // 10 min cache
        catchError((error) => this.handleError(error, 'getReferralMetrics'))
      );
  }

  // ==================== Helper Methods ====================

  /**
   * Get data from cache if valid
   */
  private getFromCache<T>(key: string): T | null {
    const entry = this.cache.get(key);
    if (!entry) {
      return null;
    }

    const now = Date.now();
    if (now - entry.timestamp > entry.ttlMs) {
      this.cache.delete(key);
      return null;
    }

    return entry.data as T;
  }

  /**
   * Store data in cache with TTL
   */
  private setInCache<T>(key: string, data: T, ttlMs: number = this.DEFAULT_CACHE_TTL): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttlMs,
    });
  }

  /**
   * Handle HTTP errors with typed response
   */
  private handleError(error: any, context: string): Observable<never> {
    this.logger.error(`[NurseWorkflowService] Error in ${context}:`, { error });

    return throwError(() => ({
      status: error.status || 0,
      statusText: error.statusText || 'Unknown Error',
      message: error.error?.message || error.message || 'An unknown error occurred',
      context,
      error,
    }));
  }

  // =========================================================================
  // REFERRAL COORDINATION METHODS
  // =========================================================================

  /**
   * Get available specialists for a referral type
   */
  getSpecialistsForReferral(
    referralType: string,
    patientId: string
  ): Observable<{ id: string; name: string; specialty: string; acceptingPatients: boolean }[]> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referrals/specialists`;

    return this.http.get<{ id: string; name: string; specialty: string; acceptingPatients: boolean }[]>(
      url,
      {
        params: { referralType, patientId },
        headers: { 'X-Tenant-ID': tenantId },
      }
    ).pipe(
      catchError((error) => this.handleError(error, 'getSpecialistsForReferral'))
    );
  }

  /**
   * Verify insurance coverage for a referral
   */
  verifyInsuranceCoverage(
    patientId: string,
    specialistId: string,
    referralType: string
  ): Observable<{ covered: boolean; requiresPriorAuth?: boolean; reason?: string }> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referrals/verify-insurance`;

    return this.http.post<{ covered: boolean; requiresPriorAuth?: boolean; reason?: string }>(
      url,
      { patientId, specialistId, referralType },
      { headers: { 'X-Tenant-ID': tenantId } }
    ).pipe(
      catchError((error) => this.handleError(error, 'verifyInsuranceCoverage'))
    );
  }

  /**
   * Send a referral to a specialist
   */
  sendReferral(referralData: {
    referralId: string;
    patientId: string;
    specialistId: string;
    referralType: string;
    urgencyLevel?: string;
    requiresPriorAuth?: boolean;
    priorAuthNumber?: string;
    sentAt: Date;
  }): Observable<{ success: boolean; referralId: string }> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referrals/send`;

    return this.http.post<{ success: boolean; referralId: string }>(
      url,
      { ...referralData, tenantId },
      { headers: { 'X-Tenant-ID': tenantId } }
    ).pipe(
      catchError((error) => this.handleError(error, 'sendReferral'))
    );
  }

  /**
   * Get the status of a referral
   */
  getReferralStatus(referralId: string): Observable<{ status: string; appointmentDate?: string }> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referrals/${referralId}/status`;

    return this.http.get<{ status: string; appointmentDate?: string }>(
      url,
      { headers: { 'X-Tenant-ID': tenantId } }
    ).pipe(
      catchError((error) => this.handleError(error, 'getReferralStatus'))
    );
  }

  /**
   * Complete referral coordination workflow
   */
  completeReferralCoordination(
    referralId: string,
    completionData: {
      referralId: string;
      specialistId?: string;
      appointmentDate?: string;
      postVisitNotes?: string;
      completedAt: Date;
    }
  ): Observable<{ success: boolean }> {
    const tenantId = this.getTenantContext();
    const url = `${NURSE_WORKFLOW_BASE_URL}/referrals/${referralId}/complete`;

    return this.http.post<{ success: boolean }>(
      url,
      { ...completionData, tenantId },
      { headers: { 'X-Tenant-ID': tenantId } }
    ).pipe(
      catchError((error) => this.handleError(error, 'completeReferralCoordination'))
    );
  }
}
