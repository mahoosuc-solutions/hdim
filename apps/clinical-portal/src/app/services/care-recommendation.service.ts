import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { map, catchError, tap, shareReplay } from 'rxjs/operators';
import { ApiService } from './api.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
} from '../config/api.config';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
  RecommendationDashboardStats,
  BulkActionRequest,
  BulkActionResult,
  PatientRiskSummary,
  RecommendationCategory,
  RecommendationStatus,
  RecommendationUrgency,
  RecommendationActionRequest,
  DEFAULT_FILTER_CONFIG,
} from '../models/care-recommendation.model';
import { RiskLevel } from '../models/patient-health.model';

/**
 * Care Recommendation Dashboard Update Event
 */
export interface RecommendationUpdate {
  type:
    | 'loaded'
    | 'filtered'
    | 'accepted'
    | 'declined'
    | 'completed'
    | 'bulk-action';
  recommendationId?: string;
  patientId?: string;
  count?: number;
  timestamp: Date;
}

/**
 * Care Recommendation Service
 *
 * Manages care recommendations dashboard including:
 * - Fetching recommendations across all patients
 * - Filtering and sorting
 * - Bulk actions (accept, decline, complete)
 * - Dashboard statistics
 */
@Injectable({
  providedIn: 'root',
})
export class CareRecommendationService {
  private readonly baseUrl = API_CONFIG.QUALITY_MEASURE_URL;

  // Update notifications
  private updateSubject = new BehaviorSubject<RecommendationUpdate | null>(
    null
  );
  public updates$ = this.updateSubject.asObservable();

  // Cache for dashboard data
  private recommendationsCache$ =
    new BehaviorSubject<DashboardRecommendation[]>([]);
  private statsCache$ =
    new BehaviorSubject<RecommendationDashboardStats | null>(null);
  private readonly cacheTimeout = 5 * 60 * 1000; // 5 minutes
  private lastFetchTime = 0;

  constructor(
    private http: HttpClient,
    private apiService: ApiService
  ) {}

  /**
   * Get all dashboard recommendations
   */
  getDashboardRecommendations(
    refresh = false
  ): Observable<DashboardRecommendation[]> {
    const now = Date.now();
    if (
      !refresh &&
      this.recommendationsCache$.value.length > 0 &&
      now - this.lastFetchTime < this.cacheTimeout
    ) {
      return of(this.recommendationsCache$.value);
    }

    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/dashboard'
    );

    return this.apiService.get<DashboardRecommendation[]>(url).pipe(
      map((recommendations) => this.transformRecommendations(recommendations)),
      tap((recommendations) => {
        this.recommendationsCache$.next(recommendations);
        this.lastFetchTime = now;
        this.notifyUpdate({
          type: 'loaded',
          count: recommendations.length,
          timestamp: new Date(),
        });
      }),
      shareReplay(1),
      catchError((error) => {
        console.error('Error fetching dashboard recommendations:', error);
        return of([]);
      })
    );
  }

  /**
   * Get filtered recommendations
   */
  getFilteredRecommendations(
    filter: Partial<RecommendationFilterConfig>,
    sort?: RecommendationSortConfig
  ): Observable<DashboardRecommendation[]> {
    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/filter'
    );

    let params = new HttpParams();

    if (filter.urgency?.length) {
      params = params.set('urgency', filter.urgency.join(','));
    }
    if (filter.category?.length) {
      params = params.set('category', filter.category.join(','));
    }
    if (filter.patientRiskLevel?.length) {
      params = params.set('riskLevel', filter.patientRiskLevel.join(','));
    }
    if (filter.status?.length) {
      params = params.set('status', filter.status.join(','));
    }
    if (filter.patientSearch) {
      params = params.set('patientSearch', filter.patientSearch);
    }
    if (sort) {
      params = params.set('sortField', sort.field);
      params = params.set('sortDirection', sort.direction);
    }

    return this.apiService.get<DashboardRecommendation[]>(url, params).pipe(
      map((recommendations) => this.transformRecommendations(recommendations)),
      tap((recommendations) => {
        this.notifyUpdate({
          type: 'filtered',
          count: recommendations.length,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        console.error('Error fetching filtered recommendations:', error);
        return of([]);
      })
    );
  }

  /**
   * Get recommendations for a specific patient
   */
  getPatientRecommendations(
    patientId: string
  ): Observable<DashboardRecommendation[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.CARE_RECOMMENDATIONS(patientId)
    );

    return this.apiService.get<DashboardRecommendation[]>(url).pipe(
      map((recommendations) => this.transformRecommendations(recommendations)),
      catchError((error) => {
        console.error(
          `Error fetching recommendations for patient ${patientId}:`,
          error
        );
        return of([]);
      })
    );
  }

  /**
   * Get dashboard statistics
   */
  getDashboardStats(): Observable<RecommendationDashboardStats> {
    if (this.statsCache$.value) {
      return of(this.statsCache$.value);
    }

    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/stats'
    );

    return this.apiService.get<RecommendationDashboardStats>(url).pipe(
      tap((stats) => this.statsCache$.next(stats)),
      catchError((error) => {
        console.error('Error fetching dashboard stats:', error);
        return of(this.getEmptyStats());
      })
    );
  }

  /**
   * Get high priority recommendations by patient risk
   */
  getHighPriorityRecommendations(
    limit = 50
  ): Observable<DashboardRecommendation[]> {
    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/high-priority'
    );
    const params = new HttpParams().set('limit', limit.toString());

    return this.apiService.get<DashboardRecommendation[]>(url, params).pipe(
      map((recommendations) => this.transformRecommendations(recommendations)),
      catchError((error) => {
        console.error('Error fetching high priority recommendations:', error);
        return of([]);
      })
    );
  }

  /**
   * Accept a recommendation
   */
  acceptRecommendation(
    id: string,
    notes?: string
  ): Observable<DashboardRecommendation> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS(id)
    );

    return this.apiService
      .post<DashboardRecommendation>(url, {
        status: 'in-progress',
        action: 'accept',
        notes,
      })
      .pipe(
        map((rec) => this.transformRecommendation(rec)),
        tap((recommendation) => {
          this.invalidateCache();
          this.notifyUpdate({
            type: 'accepted',
            recommendationId: id,
            patientId: recommendation.patientId,
            timestamp: new Date(),
          });
        }),
        catchError((error) => {
          console.error(`Error accepting recommendation ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Decline a recommendation
   */
  declineRecommendation(
    id: string,
    reason: string,
    notes?: string
  ): Observable<DashboardRecommendation> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS(id)
    );

    return this.apiService
      .post<DashboardRecommendation>(url, {
        status: 'declined',
        action: 'decline',
        reason,
        notes,
      })
      .pipe(
        map((rec) => this.transformRecommendation(rec)),
        tap((recommendation) => {
          this.invalidateCache();
          this.notifyUpdate({
            type: 'declined',
            recommendationId: id,
            patientId: recommendation.patientId,
            timestamp: new Date(),
          });
        }),
        catchError((error) => {
          console.error(`Error declining recommendation ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Complete a recommendation
   */
  completeRecommendation(
    id: string,
    outcome: string,
    notes?: string
  ): Observable<DashboardRecommendation> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS(id)
    );

    return this.apiService
      .post<DashboardRecommendation>(url, {
        status: 'completed',
        action: 'complete',
        outcome,
        notes,
      })
      .pipe(
        map((rec) => this.transformRecommendation(rec)),
        tap((recommendation) => {
          this.invalidateCache();
          this.notifyUpdate({
            type: 'completed',
            recommendationId: id,
            patientId: recommendation.patientId,
            timestamp: new Date(),
          });
        }),
        catchError((error) => {
          console.error(`Error completing recommendation ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Perform bulk action on recommendations
   */
  performBulkAction(request: BulkActionRequest): Observable<BulkActionResult> {
    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/bulk-action'
    );

    return this.apiService.post<BulkActionResult>(url, request).pipe(
      tap((result) => {
        this.invalidateCache();
        this.notifyUpdate({
          type: 'bulk-action',
          count: result.successCount,
          timestamp: new Date(),
        });
      }),
      catchError((error) => {
        console.error('Error performing bulk action:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get patient risk summaries for recommendations
   */
  getPatientRiskSummaries(): Observable<PatientRiskSummary[]> {
    const url = buildQualityMeasureUrl(
      '/patient-health/recommendations/patient-risks'
    );

    return this.apiService.get<PatientRiskSummary[]>(url).pipe(
      catchError((error) => {
        console.error('Error fetching patient risk summaries:', error);
        return of([]);
      })
    );
  }

  /**
   * Generate recommendations for a patient
   */
  generateRecommendations(
    patientId: string
  ): Observable<DashboardRecommendation[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.GENERATE_RECOMMENDATIONS(patientId)
    );

    return this.apiService.post<DashboardRecommendation[]>(url, {}).pipe(
      map((recommendations) => this.transformRecommendations(recommendations)),
      tap(() => this.invalidateCache()),
      catchError((error) => {
        console.error(
          `Error generating recommendations for patient ${patientId}:`,
          error
        );
        return throwError(() => error);
      })
    );
  }

  /**
   * Apply client-side filtering to recommendations
   */
  applyFilters(
    recommendations: DashboardRecommendation[],
    filter: Partial<RecommendationFilterConfig>
  ): DashboardRecommendation[] {
    let filtered = [...recommendations];

    if (filter.urgency?.length) {
      filtered = filtered.filter((r) =>
        filter.urgency!.includes(r.urgency)
      );
    }

    if (filter.category?.length) {
      filtered = filtered.filter((r) =>
        filter.category!.includes(r.category)
      );
    }

    if (filter.patientRiskLevel?.length) {
      filtered = filtered.filter((r) =>
        filter.patientRiskLevel!.includes(r.patientRiskLevel)
      );
    }

    if (filter.status?.length) {
      filtered = filtered.filter((r) =>
        filter.status!.includes(r.status)
      );
    }

    if (filter.patientSearch) {
      const search = filter.patientSearch.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          r.patientName.toLowerCase().includes(search) ||
          r.mrn.toLowerCase().includes(search)
      );
    }

    if (filter.daysOverdueRange?.min != null) {
      filtered = filtered.filter(
        (r) =>
          r.daysOverdue != null && r.daysOverdue >= filter.daysOverdueRange!.min!
      );
    }

    if (filter.daysOverdueRange?.max != null) {
      filtered = filtered.filter(
        (r) =>
          r.daysOverdue != null && r.daysOverdue <= filter.daysOverdueRange!.max!
      );
    }

    return filtered;
  }

  /**
   * Apply client-side sorting to recommendations
   */
  applySort(
    recommendations: DashboardRecommendation[],
    sort: RecommendationSortConfig
  ): DashboardRecommendation[] {
    const sorted = [...recommendations];
    const direction = sort.direction === 'asc' ? 1 : -1;

    sorted.sort((a, b) => {
      let comparison = 0;

      switch (sort.field) {
        case 'urgency':
          const urgencyOrder: Record<RecommendationUrgency, number> = {
            emergent: 0,
            urgent: 1,
            soon: 2,
            routine: 3,
          };
          comparison = urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
          break;

        case 'category':
          comparison = a.category.localeCompare(b.category);
          break;

        case 'patientName':
          comparison = a.patientName.localeCompare(b.patientName);
          break;

        case 'riskLevel':
          const riskOrder: Record<RiskLevel, number> = {
            critical: 0,
            high: 1,
            moderate: 2,
            low: 3,
          };
          comparison =
            riskOrder[a.patientRiskLevel] - riskOrder[b.patientRiskLevel];
          break;

        case 'dueDate':
          const aDate = a.dueDate ? new Date(a.dueDate).getTime() : Infinity;
          const bDate = b.dueDate ? new Date(b.dueDate).getTime() : Infinity;
          comparison = aDate - bDate;
          break;

        case 'createdDate':
          comparison =
            new Date(a.createdDate).getTime() -
            new Date(b.createdDate).getTime();
          break;

        default:
          comparison = 0;
      }

      return comparison * direction;
    });

    return sorted;
  }

  /**
   * Invalidate all caches
   */
  invalidateCache(): void {
    this.recommendationsCache$.next([]);
    this.statsCache$.next(null);
    this.lastFetchTime = 0;
  }

  /**
   * Get cached recommendations observable
   */
  getCachedRecommendations$(): Observable<DashboardRecommendation[]> {
    return this.recommendationsCache$.asObservable();
  }

  // Private helper methods

  private transformRecommendations(
    recommendations: DashboardRecommendation[]
  ): DashboardRecommendation[] {
    return recommendations.map((rec) => this.transformRecommendation(rec));
  }

  private transformRecommendation(
    rec: DashboardRecommendation
  ): DashboardRecommendation {
    return {
      ...rec,
      createdDate: rec.createdDate ? new Date(rec.createdDate) : new Date(),
      dueDate: rec.dueDate ? new Date(rec.dueDate) : undefined,
      completedDate: rec.completedDate
        ? new Date(rec.completedDate)
        : undefined,
      lastUpdatedDate: rec.lastUpdatedDate
        ? new Date(rec.lastUpdatedDate)
        : undefined,
      daysOverdue: this.calculateDaysOverdue(rec.dueDate),
    };
  }

  private calculateDaysOverdue(dueDate?: Date): number | undefined {
    if (!dueDate) return undefined;
    const now = new Date();
    const due = new Date(dueDate);
    const diffTime = now.getTime() - due.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : undefined;
  }

  private notifyUpdate(update: RecommendationUpdate): void {
    this.updateSubject.next(update);
  }

  private getEmptyStats(): RecommendationDashboardStats {
    return {
      totalRecommendations: 0,
      byUrgency: { emergent: 0, urgent: 0, soon: 0, routine: 0 },
      byCategory: {
        preventive: 0,
        chronicDisease: 0,
        medication: 0,
        mentalHealth: 0,
        sdoh: 0,
      },
      byPatientRisk: { critical: 0, high: 0, moderate: 0, low: 0 },
      byStatus: { pending: 0, inProgress: 0, completed: 0, declined: 0 },
      overdueSummary: { total: 0, critical: 0, warning: 0, approaching: 0 },
    };
  }
}
