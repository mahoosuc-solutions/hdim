import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { map, catchError, tap, shareReplay } from 'rxjs/operators';
import { ApiService } from './api.service';
import { LoggerService, ContextualLogger } from './logger.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
} from '../config/api.config';

/**
 * Care Gap Service - Manages care gap detection, tracking, and closure
 *
 * Features:
 * - Detect care gaps for patients
 * - Track gap status and priority
 * - Close gaps with intervention tracking
 * - Bulk gap operations
 * - Real-time gap updates via WebSocket
 */
@Injectable({
  providedIn: 'root',
})
export class CareGapService {
  private readonly baseUrl = API_CONFIG.QUALITY_MEASURE_URL;
  private gapUpdatesSubject = new BehaviorSubject<CareGapUpdate | null>(null);
  public gapUpdates$ = this.gapUpdatesSubject.asObservable();
  private readonly logger: ContextualLogger;

  // Cache for patient care gaps
  private patientGapsCache = new Map<string, { data: CareGap[]; timestamp: number }>();
  private readonly cacheTimeout = 5 * 60 * 1000; // 5 minutes

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    loggerService: LoggerService
  ) {
    this.logger = loggerService.withContext('CareGapService');
  }

  /**
   * Get all care gaps for a specific patient
   */
  getPatientCareGaps(patientId: string, refresh = false): Observable<CareGap[]> {
    // Check cache first
    if (!refresh) {
      const cached = this.getCachedGaps(patientId);
      if (cached) {
        return of(cached);
      }
    }

    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.CARE_GAPS_BY_PATIENT(patientId)
    );

    return this.apiService.get<CareGap[]>(url).pipe(
      tap((gaps) => this.cachePatientGaps(patientId, gaps)),
      shareReplay(1),
      catchError((error) => {
        this.logger.error('Error fetching care gaps for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Detect care gaps for a single patient
   */
  detectGapsForPatient(patientId: string): Observable<CareGap[]> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/detect');
    const params = new HttpParams().set('patientId', patientId);

    return this.apiService.post<CareGap[]>(url, {}, params).pipe(
      tap((gaps) => {
        this.cachePatientGaps(patientId, gaps);
        this.notifyGapUpdate({
          type: 'detected',
          patientId,
          gapCount: gaps.length,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        this.logger.error('Error detecting care gaps for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Detect care gaps for multiple patients in batch
   */
  detectGapsBatch(patientIds: string[]): Observable<CareGapBatchResult> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/detect-batch');

    return this.apiService.post<CareGapBatchResult>(url, { patientIds }).pipe(
      tap((result) => {
        // Cache results for each patient
        result.patientGaps.forEach(({ patientId, gaps }) => {
          this.cachePatientGaps(patientId, gaps);
        });

        this.notifyGapUpdate({
          type: 'batch-detected',
          batchId: result.batchId,
          totalPatients: patientIds.length,
          successCount: result.successCount,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        this.logger.error('Error detecting care gaps in batch', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Close a care gap with reason and notes
   */
  closeGap(gapId: string, closure: CareGapClosureRequest): Observable<CareGapClosure> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.ADDRESS_CARE_GAP(gapId)
    );

    return this.apiService.post<CareGapClosure>(url, closure).pipe(
      tap((closureResult) => {
        this.invalidatePatientCache(closureResult.patientId);
        this.notifyGapUpdate({
          type: 'closed',
          gapId,
          patientId: closureResult.patientId,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        this.logger.error('Error closing care gap', { gapId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Close multiple care gaps in bulk
   */
  bulkCloseGaps(gapIds: string[], closure: CareGapClosureRequest): Observable<BulkClosure> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/bulk-close');

    return this.apiService.post<BulkClosure>(url, { gapIds, ...closure }).pipe(
      tap((result) => {
        // Invalidate cache for affected patients
        result.closedGaps.forEach((gap) => {
          this.invalidatePatientCache(gap.patientId);
        });

        this.notifyGapUpdate({
          type: 'bulk-closed',
          gapIds,
          successCount: result.successCount,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        this.logger.error('Error bulk closing care gaps', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Assign an intervention to a care gap
   */
  assignIntervention(gapId: string, intervention: Intervention): Observable<CareGapIntervention> {
    const url = buildQualityMeasureUrl(`/patient-health/care-gaps/${gapId}/intervention`);

    return this.apiService.post<CareGapIntervention>(url, intervention).pipe(
      tap((result) => {
        this.invalidatePatientCache(result.patientId);
        this.notifyGapUpdate({
          type: 'intervention-assigned',
          gapId,
          patientId: result.patientId,
          interventionType: intervention.type,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        this.logger.error('Error assigning intervention to gap', { gapId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get care gap priority score
   */
  getGapPriorityScore(gapId: string): Observable<GapPriorityScore> {
    const url = buildQualityMeasureUrl(`/patient-health/care-gaps/${gapId}/priority`);

    return this.apiService.get<GapPriorityScore>(url).pipe(
      catchError((error) => {
        this.logger.error('Error getting priority score for gap', { gapId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get care gaps by status
   */
  getGapsByStatus(status: CareGapStatus, limit = 100): Observable<CareGap[]> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/by-status');
    const params = new HttpParams()
      .set('status', status)
      .set('limit', limit.toString());

    return this.apiService.get<CareGap[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error('Error getting care gaps by status', { status, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get high-priority care gaps across all patients
   */
  getHighPriorityGaps(limit = 50): Observable<CareGap[]> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/high-priority');
    const params = new HttpParams().set('limit', limit.toString());

    return this.apiService.get<CareGap[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error('Error getting high priority care gaps', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Invalidate cache for a specific patient
   */
  invalidatePatientCache(patientId: string): void {
    this.patientGapsCache.delete(patientId);
  }

  /**
   * Clear all cached care gaps
   */
  clearCache(): void {
    this.patientGapsCache.clear();
  }

  /**
   * Get cached care gaps for a patient
   */
  private getCachedGaps(patientId: string): CareGap[] | null {
    const cached = this.patientGapsCache.get(patientId);
    if (!cached) return null;

    const now = Date.now();
    if (now - cached.timestamp > this.cacheTimeout) {
      this.patientGapsCache.delete(patientId);
      return null;
    }

    return cached.data;
  }

  /**
   * Cache care gaps for a patient
   */
  private cachePatientGaps(patientId: string, gaps: CareGap[]): void {
    this.patientGapsCache.set(patientId, {
      data: gaps,
      timestamp: Date.now(),
    });
  }

  /**
   * Notify subscribers of gap updates
   */
  private notifyGapUpdate(update: CareGapUpdate): void {
    this.gapUpdatesSubject.next(update);
  }
}

// Types and Interfaces

export interface CareGap {
  id: string;
  patientId: string;
  measureId: string;
  measureName: string;
  gapType: CareGapType;
  status: CareGapStatus;
  priority: GapPriority;
  priorityScore: number;
  description: string;
  recommendation: string;
  dueDate?: string;
  detectedDate: string;
  closedDate?: string;
  closureReason?: string;
  intervention?: Intervention;
  metadata?: Record<string, unknown>;
}

export enum CareGapType {
  PREVENTIVE_SCREENING = 'PREVENTIVE_SCREENING',
  CHRONIC_DISEASE_MANAGEMENT = 'CHRONIC_DISEASE_MANAGEMENT',
  MEDICATION_ADHERENCE = 'MEDICATION_ADHERENCE',
  FOLLOW_UP_CARE = 'FOLLOW_UP_CARE',
  BEHAVIORAL_HEALTH = 'BEHAVIORAL_HEALTH',
  OTHER = 'OTHER',
}

export enum CareGapStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  CLOSED = 'CLOSED',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED',
}

export enum GapPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export interface CareGapClosureRequest {
  reason: string;
  notes?: string;
  closedBy: string;
  closureDate?: string;
  intervention?: Intervention;
}

export interface CareGapClosure {
  gapId: string;
  patientId: string;
  closedDate: string;
  closedBy: string;
  reason: string;
  notes?: string;
  intervention?: Intervention;
}

export interface BulkClosure {
  successCount: number;
  failureCount: number;
  closedGaps: CareGapClosure[];
  errors: { gapId: string; error: string }[];
}

export interface Intervention {
  type: InterventionType;
  description: string;
  scheduledDate?: string;
  completedDate?: string;
  assignedTo?: string;
  outcome?: string;
}

export enum InterventionType {
  OUTREACH = 'OUTREACH',
  REMINDER = 'REMINDER',
  EDUCATION = 'EDUCATION',
  REFERRAL = 'REFERRAL',
  APPOINTMENT_SCHEDULED = 'APPOINTMENT_SCHEDULED',
  MEDICATION_REVIEW = 'MEDICATION_REVIEW',
  OTHER = 'OTHER',
}

export interface CareGapIntervention {
  gapId: string;
  patientId: string;
  intervention: Intervention;
  assignedDate: string;
  status: string;
}

export interface CareGapBatchResult {
  batchId: string;
  totalPatients: number;
  successCount: number;
  failureCount: number;
  patientGaps: { patientId: string; gaps: CareGap[] }[];
  errors: { patientId: string; error: string }[];
}

export interface GapPriorityScore {
  gapId: string;
  priority: GapPriority;
  score: number;
  factors: Record<string, number>;
  calculation: string;
}

export interface CareGapUpdate {
  type: 'detected' | 'closed' | 'batch-detected' | 'bulk-closed' | 'intervention-assigned';
  gapId?: string;
  gapIds?: string[];
  patientId?: string;
  batchId?: string;
  gapCount?: number;
  totalPatients?: number;
  successCount?: number;
  interventionType?: InterventionType;
  timestamp: Date;
}
