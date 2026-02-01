import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ClinicalDecisionFilter {
  decisionType?: string;
  alertSeverity?: string;
  reviewStatus?: string;
  startDate?: string;
  endDate?: string;
  evidenceGrade?: string;
  hasOverride?: boolean;
  specialtyArea?: string;
}

export interface ClinicalDecisionEvent {
  decisionId: string;
  decisionType: string;
  patientId: string;
  patientName: string;
  alertSeverity: string;
  reviewStatus: string;
  decisionTimestamp: string;
  evidenceGrade: string;
  confidenceScore: number;
  specialtyArea: string;
  clinicalRecommendation: string;
  priority: string;
  hasOverride: boolean;
  overrideReason?: string;
  relatedAlertsCount: number;
}

export interface ClinicalDecisionDetail {
  decisionId: string;
  decisionType: string;
  reviewStatus: string;
  decisionTimestamp: string;
  patientContext: any;
  recommendation: any;
  evidence: any[];
  drugInteractions: any[];
  careGaps: any[];
  riskAssessment: any;
  reviewHistory: any[];
}

export interface ClinicalReviewRequest {
  reviewOutcome: string;
  reviewNotes?: string;
  applyOverride?: boolean;
  overrideReason?: string;
  alternativeRecommendation?: string;
  reviewerAssessment?: any;
}

export interface ClinicalReviewResult {
  decisionId: string;
  reviewOutcome: string;
  reviewedBy: string;
  reviewedAt: string;
  success: boolean;
  message: string;
  overrideApplied: boolean;
}

export interface MedicationAlertDTO {
  alertId: string;
  patientId: string;
  alertType: string;
  severity: string;
  involvedMedications: string[];
  alertMessage: string;
  clinicalRecommendation: string;
  evidenceGrade: string;
  acknowledged: boolean;
  acknowledgedBy?: string;
}

export interface CareGapDTO {
  gapId: string;
  patientId: string;
  gapType: string;
  serviceDescription: string;
  dueDate: string;
  daysPastDue: number;
  priority: string;
  guidelineReference: string;
  status: string;
  evidenceGrade: string;
}

export interface RiskStratificationDTO {
  stratificationId: string;
  patientId: string;
  riskCategory: string;
  overallRiskLevel: string;
  riskScore: number;
  contributingFactors: any[];
  assessmentModel: string;
  evidenceGrade: string;
  recommendedInterventions: string[];
}

export interface ClinicalMetrics {
  totalDecisions: number;
  approvedDecisions: number;
  rejectedDecisions: number;
  pendingReview: number;
  approvalRate: number;
  overrideRate: number;
  averageConfidenceScore: number;
  averageReviewTimeHours: number;
  decisionTypeDistribution: any;
  severityDistribution: any;
  evidenceGradeDistribution: any;
}

export interface ClinicalTrendData {
  dailyTrends: any[];
  averageApprovalRate: number;
  averageOverrideRate: number;
  averageConfidenceScore: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Service for Clinical Decision Audit API operations
 */
@Injectable({
  providedIn: 'root'
})
export class ClinicalDecisionService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/clinical`;

  constructor(private http: HttpClient) {}

  /**
   * Get clinical decision history with filtering
   */
  getDecisionHistory(
    filter: ClinicalDecisionFilter = {},
    page: number = 0,
    size: number = 20,
    sort: string = 'decisionTimestamp,desc'
  ): Observable<PageResponse<ClinicalDecisionEvent>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filter.decisionType) params = params.set('decisionType', filter.decisionType);
    if (filter.alertSeverity) params = params.set('alertSeverity', filter.alertSeverity);
    if (filter.reviewStatus) params = params.set('reviewStatus', filter.reviewStatus);
    if (filter.startDate) params = params.set('startDate', filter.startDate);
    if (filter.endDate) params = params.set('endDate', filter.endDate);
    if (filter.evidenceGrade) params = params.set('evidenceGrade', filter.evidenceGrade);
    if (filter.hasOverride !== undefined) {
      params = params.set('hasOverride', filter.hasOverride.toString());
    }
    if (filter.specialtyArea) params = params.set('specialtyArea', filter.specialtyArea);

    return this.http.get<PageResponse<ClinicalDecisionEvent>>(`${this.baseUrl}/decisions`, { params });
  }

  /**
   * Get detailed information about a specific clinical decision
   */
  getDecisionDetail(decisionId: string): Observable<ClinicalDecisionDetail> {
    return this.http.get<ClinicalDecisionDetail>(`${this.baseUrl}/decisions/${decisionId}`);
  }

  /**
   * Review a clinical decision (approve, reject, request revision)
   */
  reviewDecision(decisionId: string, request: ClinicalReviewRequest): Observable<ClinicalReviewResult> {
    return this.http.post<ClinicalReviewResult>(`${this.baseUrl}/decisions/${decisionId}/review`, request);
  }

  /**
   * Get medication alerts
   */
  getMedicationAlerts(
    severity?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MedicationAlertDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (severity) params = params.set('severity', severity);

    return this.http.get<PageResponse<MedicationAlertDTO>>(`${this.baseUrl}/medication-alerts`, { params });
  }

  /**
   * Get care gaps
   */
  getCareGaps(
    status?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<CareGapDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) params = params.set('status', status);

    return this.http.get<PageResponse<CareGapDTO>>(`${this.baseUrl}/care-gaps`, { params });
  }

  /**
   * Get risk stratifications
   */
  getRiskStratifications(
    riskLevel?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<RiskStratificationDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (riskLevel) params = params.set('riskLevel', riskLevel);

    return this.http.get<PageResponse<RiskStratificationDTO>>(`${this.baseUrl}/risk-stratifications`, { params });
  }

  /**
   * Get clinical decision metrics
   */
  getMetrics(startDate?: string, endDate?: string): Observable<ClinicalMetrics> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<ClinicalMetrics>(`${this.baseUrl}/metrics`, { params });
  }

  /**
   * Get performance trends over time
   */
  getPerformanceTrends(days: number = 30): Observable<ClinicalTrendData> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<ClinicalTrendData>(`${this.baseUrl}/trends`, { params });
  }
}
