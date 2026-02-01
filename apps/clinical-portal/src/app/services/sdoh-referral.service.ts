import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { ApiService } from './api.service';
import { LoggerService, ContextualLogger } from './logger.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
} from '../config/api.config';
import {
  SDOHReferralRequest,
  SDOHReferralDetail,
  ReferralDestination,
  ReferralStatus,
  ReferralOutcomeDocumentation,
  StaffMember,
  InternalReferralDestination,
  ExternalResourceSearchResult,
  ReferralSearchCriteria,
  ReferralUrgency,
} from '../models/sdoh-referral.model';
import { SDOHCategory, CommunityResource } from '../models/patient-health.model';

/**
 * SDOH Referral Service
 *
 * Orchestrates the SDOH referral workflow including:
 * - Internal referrals to staff members
 * - External referrals to community resources
 * - Referral status tracking
 * - Outcome documentation
 */
@Injectable({
  providedIn: 'root',
})
export class SDOHReferralService {
  private readonly baseUrl = API_CONFIG.QUALITY_MEASURE_URL;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private logger: LoggerService
  ) {
  }

  // ============================================
  // Internal Referral Methods
  // ============================================

  /**
   * Get available internal staff by role
   */
  getAvailableStaff(
    role?: InternalReferralDestination
  ): Observable<StaffMember[]> {
    const url = buildQualityMeasureUrl('/patient-health/sdoh/staff');
    let params = new HttpParams();
    if (role) {
      params = params.set('role', role);
    }

    return this.apiService.get<StaffMember[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error('Error fetching available staff:', error);
        // Return mock data for development
        return of(this.getMockStaffMembers(role));
      })
    );
  }

  /**
   * Get staff member availability/workload
   */
  getStaffAvailability(
    staffId: string
  ): Observable<{ available: boolean; currentCaseload: number }> {
    const url = buildQualityMeasureUrl(
      `/patient-health/sdoh/staff/${staffId}/availability`
    );

    return this.apiService
      .get<{ available: boolean; currentCaseload: number }>(url)
      .pipe(
        catchError((error) => {
          this.logger.error('Error fetching staff availability:', error);
          return of({ available: true, currentCaseload: 0 });
        })
      );
  }

  // ============================================
  // External Referral Methods
  // ============================================

  /**
   * Search community resources
   */
  searchCommunityResources(
    category: SDOHCategory,
    zipCode?: string,
    radius?: number
  ): Observable<CommunityResource[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.COMMUNITY_RESOURCES_SEARCH
    );
    let params = new HttpParams().set('category', category);
    if (zipCode) {
      params = params.set('zipCode', zipCode);
    }
    if (radius) {
      params = params.set('radius', radius.toString());
    }

    return this.apiService.get<CommunityResource[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error('Error searching community resources:', error);
        return of([]);
      })
    );
  }

  /**
   * Search 211 database
   */
  search211Resources(
    category: SDOHCategory,
    location: string
  ): Observable<ExternalResourceSearchResult[]> {
    const url = buildQualityMeasureUrl('/patient-health/sdoh/search-211');
    const params = new HttpParams()
      .set('category', category)
      .set('location', location);

    return this.apiService.get<ExternalResourceSearchResult[]>(url, params).pipe(
      map((results) =>
        results.map((r) => ({ ...r, source: '211' as const }))
      ),
      catchError((error) => {
        this.logger.error('Error searching 211 resources:', error);
        return of([]);
      })
    );
  }

  /**
   * Search findhelp.org
   */
  searchFindHelpResources(
    category: SDOHCategory,
    zipCode: string
  ): Observable<ExternalResourceSearchResult[]> {
    const url = buildQualityMeasureUrl('/patient-health/sdoh/search-findhelp');
    const params = new HttpParams()
      .set('category', category)
      .set('zipCode', zipCode);

    return this.apiService.get<ExternalResourceSearchResult[]>(url, params).pipe(
      map((results) =>
        results.map((r) => ({ ...r, source: 'findhelp' as const }))
      ),
      catchError((error) => {
        this.logger.error('Error searching findhelp resources:', error);
        return of([]);
      })
    );
  }

  // ============================================
  // Referral Management
  // ============================================

  /**
   * Submit a new referral
   */
  submitReferral(request: SDOHReferralRequest): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRALS);

    return this.apiService.post<SDOHReferralDetail>(url, request).pipe(
      tap((referral) => {
        this.logger.debug('Referral submitted:', referral.id);
      }),
      catchError((error) => {
        this.logger.error('Error submitting referral:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Save referral as draft
   */
  saveDraft(
    request: Partial<SDOHReferralRequest>
  ): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(
      `${QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRALS}/draft`
    );

    return this.apiService.post<SDOHReferralDetail>(url, request).pipe(
      catchError((error) => {
        this.logger.error('Error saving draft:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get referral by ID
   */
  getReferral(referralId: string): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_BY_ID(referralId)
    );

    return this.apiService.get<SDOHReferralDetail>(url).pipe(
      catchError((error) => {
        this.logger.error(`Error fetching referral ${referralId}:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get patient referral history
   */
  getPatientReferrals(
    patientId: string,
    criteria?: ReferralSearchCriteria
  ): Observable<SDOHReferralDetail[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_HISTORY(patientId)
    );

    let params = new HttpParams();
    if (criteria?.status?.length) {
      params = params.set('status', criteria.status.join(','));
    }
    if (criteria?.urgency?.length) {
      params = params.set('urgency', criteria.urgency.join(','));
    }
    if (criteria?.dateRange) {
      params = params.set('startDate', criteria.dateRange.start.toISOString());
      params = params.set('endDate', criteria.dateRange.end.toISOString());
    }

    return this.apiService.get<SDOHReferralDetail[]>(url, params).pipe(
      catchError((error) => {
        this.logger.error(
          `Error fetching referrals for patient ${patientId}:`,
          error
        );
        return of([]);
      })
    );
  }

  /**
   * Update referral status
   */
  updateStatus(
    referralId: string,
    status: ReferralStatus,
    notes?: string
  ): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_STATUS(referralId)
    );

    return this.apiService
      .post<SDOHReferralDetail>(url, { status, notes })
      .pipe(
        tap((referral) => {
          this.logger.debug(`Referral ${referralId} status updated to ${status}`);
        }),
        catchError((error) => {
          this.logger.error(`Error updating referral ${referralId} status:`, error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Send referral to destination
   */
  sendReferral(referralId: string): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_SEND(referralId)
    );

    return this.apiService.post<SDOHReferralDetail>(url, {}).pipe(
      tap((referral) => {
        this.logger.debug(`Referral ${referralId} sent`);
      }),
      catchError((error) => {
        this.logger.error(`Error sending referral ${referralId}:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Document outcome
   */
  documentOutcome(
    referralId: string,
    outcome: ReferralOutcomeDocumentation
  ): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(
      `${QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_BY_ID(referralId)}/outcome`
    );

    return this.apiService.post<SDOHReferralDetail>(url, outcome).pipe(
      tap(() => {
        this.logger.debug(`Outcome documented for referral ${referralId}`);
      }),
      catchError((error) => {
        this.logger.error(
          `Error documenting outcome for referral ${referralId}:`,
          error
        );
        return throwError(() => error);
      })
    );
  }

  /**
   * Get referral metrics for a patient
   */
  getReferralMetrics(
    patientId: string
  ): Observable<{
    totalReferrals: number;
    activeReferrals: number;
    completedReferrals: number;
    successRate: number;
  }> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_METRICS(patientId)
    );

    return this.apiService
      .get<{
        totalReferrals: number;
        activeReferrals: number;
        completedReferrals: number;
        successRate: number;
      }>(url)
      .pipe(
        catchError((error) => {
          this.logger.error(
            `Error fetching referral metrics for patient ${patientId}:`,
            error
          );
          return of({
            totalReferrals: 0,
            activeReferrals: 0,
            completedReferrals: 0,
            successRate: 0,
          });
        })
      );
  }

  // ============================================
  // Helper Methods
  // ============================================

  /**
   * Get urgency options with descriptions
   */
  getUrgencyOptions(): {
    value: ReferralUrgency;
    label: string;
    description: string;
    icon: string;
    color: string;
  }[] {
    return [
      {
        value: 'emergent',
        label: 'Emergent',
        description: 'Requires immediate attention within 24 hours',
        icon: 'error',
        color: '#f44336',
      },
      {
        value: 'urgent',
        label: 'Urgent',
        description: 'Should be addressed within 1 week',
        icon: 'warning',
        color: '#ff9800',
      },
      {
        value: 'soon',
        label: 'Soon',
        description: 'Should be addressed within 2-4 weeks',
        icon: 'schedule',
        color: '#ffc107',
      },
      {
        value: 'routine',
        label: 'Routine',
        description: 'Can be addressed at next opportunity',
        icon: 'check_circle',
        color: '#4caf50',
      },
    ];
  }

  /**
   * Get suggested destinations based on SDOH category
   */
  getSuggestedDestinations(
    category: SDOHCategory
  ): InternalReferralDestination[] {
    const mappings: Record<SDOHCategory, InternalReferralDestination[]> = {
      'food-insecurity': ['social-worker', 'community-health-worker'],
      'housing-instability': ['social-worker', 'case-manager'],
      transportation: ['care-coordinator', 'community-health-worker'],
      'utility-assistance': ['social-worker', 'case-manager'],
      'interpersonal-safety': ['social-worker', 'behavioral-health'],
      education: ['patient-navigator', 'community-health-worker'],
      employment: ['social-worker', 'patient-navigator'],
      'social-isolation': ['behavioral-health', 'community-health-worker'],
      'financial-strain': ['social-worker', 'case-manager'],
      food: ['social-worker', 'community-health-worker'],
      housing: ['social-worker', 'case-manager'],
      financial: ['social-worker', 'case-manager'],
      social: ['behavioral-health', 'community-health-worker'],
      safety: ['social-worker', 'behavioral-health'],
    };

    return mappings[category] || ['social-worker'];
  }

  // ============================================
  // Mock Data (for development)
  // ============================================

  private getMockStaffMembers(
    role?: InternalReferralDestination
  ): StaffMember[] {
    const allStaff: StaffMember[] = [
      {
        id: 'staff-1',
        name: 'Sarah Johnson',
        role: 'social-worker',
        email: 'sjohnson@clinic.com',
        department: 'Social Services',
        availableCapacity: 75,
      },
      {
        id: 'staff-2',
        name: 'Michael Chen',
        role: 'care-coordinator',
        email: 'mchen@clinic.com',
        department: 'Care Coordination',
        availableCapacity: 60,
      },
      {
        id: 'staff-3',
        name: 'Emily Rodriguez',
        role: 'behavioral-health',
        email: 'erodriguez@clinic.com',
        department: 'Behavioral Health',
        availableCapacity: 40,
      },
      {
        id: 'staff-4',
        name: 'David Kim',
        role: 'case-manager',
        email: 'dkim@clinic.com',
        department: 'Case Management',
        availableCapacity: 80,
      },
      {
        id: 'staff-5',
        name: 'Lisa Thompson',
        role: 'community-health-worker',
        email: 'lthompson@clinic.com',
        department: 'Community Outreach',
        availableCapacity: 90,
      },
      {
        id: 'staff-6',
        name: 'James Wilson',
        role: 'patient-navigator',
        email: 'jwilson@clinic.com',
        department: 'Patient Navigation',
        availableCapacity: 65,
      },
    ];

    if (role) {
      return allStaff.filter((s) => s.role === role);
    }
    return allStaff;
  }
}
