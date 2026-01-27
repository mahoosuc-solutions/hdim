import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { map, catchError, tap, shareReplay } from 'rxjs/operators';
import { ApiService } from './api.service';
import { LoggerService, ContextualLogger } from '../logger.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
} from '../config/api.config';
import { ErrorValidationService } from './error-validation.service';
import { COMPLIANCE_CONFIG } from '../config/compliance.config';
import { ErrorCode, ErrorSeverity } from '../models/error.model';

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
  private readonly careGapApiUrl = API_CONFIG.CARE_GAP_URL;
  private gapUpdatesSubject = new BehaviorSubject<CareGapUpdate | null>(null);
  public gapUpdates$ = this.gapUpdatesSubject.asObservable();
  private readonly logger: ContextualLogger;

  // Cache for patient care gaps
  private patientGapsCache = new Map<string, { data: CareGap[]; timestamp: number }>();
  private readonly cacheTimeout = 5 * 60 * 1000; // 5 minutes

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    loggerService: LoggerService,
    private errorValidationService: ErrorValidationService
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
        
        // Check if fallbacks are disabled
        if (COMPLIANCE_CONFIG.disableFallbacks && 
            !this.errorValidationService.isFallbackAllowed('CareGapService')) {
          // Track error for compliance
          this.errorValidationService.trackError(error, {
            service: 'CareGapService',
            endpoint: url,
            operation: 'getGapsByStatus',
            errorCode: ErrorCode.QUALITY_MEASURE_ERROR,
            severity: ErrorSeverity.WARNING,
          });
          // Throw error instead of fallback
          return throwError(() => error);
        }
        
        // Return empty array for demo/fallback (only if allowed)
        return of([]);
      })
    );
  }

  /**
   * Get high-priority care gaps across all patients
   * Uses care-gap-service endpoint: /api/v1/care-gaps?priority=HIGH
   */
  getHighPriorityGaps(limit = 50): Observable<CareGap[]> {
    // Use care-gap-service endpoint, not quality-measure-service
    const url = `${API_CONFIG.CARE_GAP_URL}/api/v1/care-gaps`;
    const params = new HttpParams()
      .set('priority', 'HIGH')
      .set('page', '0')
      .set('size', limit.toString());

    return this.apiService.get<CareGap[]>(url, params).pipe(
      map((response: any) => {
        // Handle paginated response
        if (response && response.content && Array.isArray(response.content)) {
          return response.content as CareGap[];
        }
        // Fallback: if response is already an array
        if (Array.isArray(response)) {
          return response as CareGap[];
        }
        return [];
      }),
      catchError((error) => {
        this.logger.error('Error getting high priority care gaps', error);
        
        // Check if fallbacks are disabled
        if (COMPLIANCE_CONFIG.disableFallbacks && 
            !this.errorValidationService.isFallbackAllowed('CareGapService')) {
          // Track error for compliance
          this.errorValidationService.trackError(error, {
            service: 'CareGapService',
            endpoint: url,
            operation: 'getHighPriorityGaps',
            errorCode: ErrorCode.QUALITY_MEASURE_ERROR,
            severity: ErrorSeverity.ERROR,
          });
          // Throw error instead of fallback
          return throwError(() => error);
        }
        
        // Return mock data for development/fallback (only if allowed)
        return of(this.getMockHighPriorityGaps(limit));
      })
    );
  }

  /**
   * Get mock high-priority care gaps for development/fallback
   * Note: Using placeholder UUIDs to prevent invalid API calls
   */
  private getMockHighPriorityGaps(limit: number): CareGap[] {
    // Use placeholder UUIDs that won't trigger real patient lookups
    // These are clearly mock IDs and won't match any real patients
    const MOCK_PATIENT_ID_PREFIX = '00000000-0000-0000-0000-00000000';
    const mockGaps: CareGap[] = [
      {
        id: 'gap-001',
        patientId: `${MOCK_PATIENT_ID_PREFIX}0001`,
        measureId: 'HEDIS-BCS',
        measureName: 'Breast Cancer Screening',
        gapType: CareGapType.PREVENTIVE_SCREENING,
        status: CareGapStatus.OPEN,
        priority: GapPriority.HIGH,
        priorityScore: 85,
        description: 'Mammogram overdue - last screening was 26 months ago',
        recommendation: 'Schedule mammogram screening',
        dueDate: '2026-02-15',
        detectedDate: '2026-01-01',
      },
      {
        id: 'gap-002',
        patientId: 'pat-002',
        measureId: 'HEDIS-CDC-A1C',
        measureName: 'Diabetes HbA1c Control',
        gapType: CareGapType.CHRONIC_DISEASE_MANAGEMENT,
        status: CareGapStatus.OPEN,
        priority: GapPriority.HIGH,
        priorityScore: 92,
        description: 'HbA1c test overdue - diabetic patient needs monitoring',
        recommendation: 'Order HbA1c lab test',
        dueDate: '2026-01-31',
        detectedDate: '2026-01-05',
      },
      {
        id: 'gap-003',
        patientId: `${MOCK_PATIENT_ID_PREFIX}0003`,
        measureId: 'HEDIS-COL',
        measureName: 'Colorectal Cancer Screening',
        gapType: CareGapType.PREVENTIVE_SCREENING,
        status: CareGapStatus.OPEN,
        priority: GapPriority.HIGH,
        priorityScore: 78,
        description: 'Colonoscopy overdue - patient age 52, no screening on record',
        recommendation: 'Schedule colonoscopy or FIT test',
        dueDate: '2026-03-01',
        detectedDate: '2025-12-15',
      },
      {
        id: 'gap-004',
        patientId: `${MOCK_PATIENT_ID_PREFIX}0004`,
        measureId: 'HEDIS-CBP',
        measureName: 'Controlling Blood Pressure',
        gapType: CareGapType.CHRONIC_DISEASE_MANAGEMENT,
        status: CareGapStatus.OPEN,
        priority: GapPriority.HIGH,
        priorityScore: 88,
        description: 'Blood pressure not at goal - last reading 152/94',
        recommendation: 'Schedule follow-up for BP management',
        dueDate: '2026-01-20',
        detectedDate: '2026-01-08',
      },
      {
        id: 'gap-005',
        patientId: `${MOCK_PATIENT_ID_PREFIX}0005`,
        measureId: 'HEDIS-MMA',
        measureName: 'Medication Adherence - Statins',
        gapType: CareGapType.MEDICATION_ADHERENCE,
        status: CareGapStatus.OPEN,
        priority: GapPriority.HIGH,
        priorityScore: 82,
        description: 'Statin adherence below 80% - refill overdue',
        recommendation: 'Contact patient about medication refill',
        dueDate: '2026-01-25',
        detectedDate: '2026-01-03',
      },
    ];

    return mockGaps.slice(0, Math.min(limit, mockGaps.length));
  }

  /**
   * Get care gap trend data for a time period
   * Returns historical data points for trend analysis
   */
  getCareGapTrends(days = 30): Observable<CareGapTrendPoint[]> {
    const url = buildQualityMeasureUrl('/patient-health/care-gaps/trends');
    const params = new HttpParams().set('days', days.toString());

    return this.apiService.get<CareGapTrendPoint[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error('Error getting care gap trends', { days, error });
        
        // Check if fallbacks are disabled
        if (COMPLIANCE_CONFIG.disableFallbacks && 
            !this.errorValidationService.isFallbackAllowed('CareGapService')) {
          // Track error for compliance
          this.errorValidationService.trackError(error, {
            service: 'CareGapService',
            endpoint: url,
            operation: 'getGapTrends',
            errorCode: ErrorCode.QUALITY_MEASURE_ERROR,
            severity: ErrorSeverity.WARNING,
          });
          // Throw error instead of fallback
          return throwError(() => error);
        }
        
        // Return mock data for development/fallback (only if allowed)
        return of(this.getMockTrendData(days));
      })
    );
  }

  /**
   * Get mock trend data for development/fallback
   */
  private getMockTrendData(days: number): CareGapTrendPoint[] {
    const data: CareGapTrendPoint[] = [];
    const now = new Date();
    let baseTotalGaps = 45;

    for (let i = days; i >= 0; i--) {
      const date = new Date(now);
      date.setDate(date.getDate() - i);

      // Simulate trend: generally improving with some variation
      const variation = Math.floor(Math.random() * 6) - 2; // -2 to +3
      const closed = Math.floor(Math.random() * 3) + 1;
      const newGaps = Math.floor(Math.random() * 4);

      baseTotalGaps = Math.max(10, baseTotalGaps + newGaps - closed + variation);

      // Distribute by urgency
      const high = Math.floor(baseTotalGaps * 0.2);
      const medium = Math.floor(baseTotalGaps * 0.4);
      const low = baseTotalGaps - high - medium;

      // Distribute by type
      const byType = {
        screening: Math.floor(baseTotalGaps * 0.3),
        medication: Math.floor(baseTotalGaps * 0.25),
        followup: Math.floor(baseTotalGaps * 0.2),
        lab: Math.floor(baseTotalGaps * 0.15),
        assessment: baseTotalGaps - Math.floor(baseTotalGaps * 0.9),
      };

      data.push({
        date: date,
        totalGaps: baseTotalGaps,
        closedGaps: closed,
        newGaps: newGaps,
        byUrgency: {
          high,
          medium,
          low,
        },
        byType,
      });
    }

    return data;
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

  /**
   * Get care gaps from the Care Gap Service (demo/live list view).
   */
  getCareGapsPage(params: {
    page?: number;
    size?: number;
    priority?: GapPriority;
    status?: CareGapStatus;
    patientId?: string;
  } = {}): Observable<CareGapPageResponse> {
    const url = `${this.careGapApiUrl}/api/v1/care-gaps`;
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 200).toString());

    if (params.priority) {
      httpParams = httpParams.set('priority', params.priority);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.patientId) {
      httpParams = httpParams.set('patientId', params.patientId);
    }

    return this.apiService.get<CareGapPageResponse>(url, httpParams).pipe(
      catchError((error) => {
        this.logger.error('Error fetching care gap page', { params, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get care gap stats for a patient from the Care Gap Service.
   */
  getCareGapStats(patientId: string): Observable<CareGapStatsResponse> {
    const url = `${this.careGapApiUrl}/care-gap/stats`;
    const httpParams = new HttpParams().set('patient', patientId);

    return this.apiService.get<CareGapStatsResponse>(url, httpParams).pipe(
      catchError((error) => {
        this.logger.error('Error fetching care gap stats', { patientId, error });
        return throwError(() => error);
      })
    );
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

/**
 * Care Gap Trend Data Point
 * Used for trend analysis and visualization
 */
export interface CareGapTrendPoint {
  date: Date;
  totalGaps: number;
  closedGaps: number;
  newGaps: number;
  byUrgency: {
    high: number;
    medium: number;
    low: number;
  };
  byType: {
    screening: number;
    medication: number;
    followup: number;
    lab: number;
    assessment: number;
  };
}

export interface CareGapPageResponse {
  content: CareGapApiItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CareGapApiItem {
  id: string;
  tenantId: string;
  patientId: string;
  measureId: string;
  measureName: string;
  gapCategory?: string;
  gapStatus?: string;
  gapDescription?: string;
  priority?: string;
  identifiedDate?: number;
  dueDate?: number[] | string;
}

export interface CareGapStatsResponse {
  openGapsCount: number;
  highPriorityCount: number;
  overdueCount: number;
  hasOpenGaps: boolean;
  hasHighPriorityGaps: boolean;
}
