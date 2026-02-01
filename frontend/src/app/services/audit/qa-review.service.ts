import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface QAReviewFilter {
  reviewStatus?: string;
  aiDecisionType?: string;
  startDate?: string;
  endDate?: string;
  priority?: string;
  hasDiscrepancy?: boolean;
}

export interface QAReviewEvent {
  reviewId: string;
  aiDecisionType: string;
  aiRecommendation: string;
  confidenceScore: number;
  reviewStatus: string;
  priority: string;
  createdAt: string;
  patientId?: string;
  hasDiscrepancy: boolean;
}

export interface QAReviewDetail {
  reviewId: string;
  aiDecisionType: string;
  reviewStatus: string;
  createdAt: string;
  aiDecisionContext: any;
  reviewerAssessment: any;
  discrepancyAnalysis: any;
  reviewHistory: any[];
}

export interface QAReviewRequest {
  reviewOutcome: string;
  reviewNotes?: string;
  hasDiscrepancy?: boolean;
  discrepancyType?: string;
  discrepancyDescription?: string;
  correctedRecommendation?: string;
}

export interface QAReviewResult {
  reviewId: string;
  reviewOutcome: string;
  reviewedBy: string;
  reviewedAt: string;
  success: boolean;
  message: string;
}

export interface QAMetrics {
  totalReviews: number;
  approvedReviews: number;
  rejectedReviews: number;
  pendingReviews: number;
  approvalRate: number;
  discrepancyRate: number;
  averageConfidenceScore: number;
  averageReviewTimeHours: number;
  decisionTypeDistribution: any;
  priorityDistribution: any;
}

export interface QATrendData {
  dailyTrends: any[];
  averageApprovalRate: number;
  averageDiscrepancyRate: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Service for QA Review API operations
 */
@Injectable({
  providedIn: 'root'
})
export class QaReviewService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/qa-review`;

  constructor(private http: HttpClient) {}

  /**
   * Get QA review history with filtering
   */
  getReviewHistory(
    filter: QAReviewFilter = {},
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,desc'
  ): Observable<PageResponse<QAReviewEvent>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filter.reviewStatus) params = params.set('reviewStatus', filter.reviewStatus);
    if (filter.aiDecisionType) params = params.set('aiDecisionType', filter.aiDecisionType);
    if (filter.startDate) params = params.set('startDate', filter.startDate);
    if (filter.endDate) params = params.set('endDate', filter.endDate);
    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.hasDiscrepancy !== undefined) {
      params = params.set('hasDiscrepancy', filter.hasDiscrepancy.toString());
    }

    return this.http.get<PageResponse<QAReviewEvent>>(`${this.baseUrl}/reviews`, { params });
  }

  /**
   * Get detailed information about a specific review
   */
  getReviewDetail(reviewId: string): Observable<QAReviewDetail> {
    return this.http.get<QAReviewDetail>(`${this.baseUrl}/reviews/${reviewId}`);
  }

  /**
   * Submit a QA review
   */
  submitReview(reviewId: string, request: QAReviewRequest): Observable<QAReviewResult> {
    return this.http.post<QAReviewResult>(`${this.baseUrl}/reviews/${reviewId}/submit`, request);
  }

  /**
   * Get QA metrics
   */
  getMetrics(startDate?: string, endDate?: string): Observable<QAMetrics> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<QAMetrics>(`${this.baseUrl}/metrics`, { params });
  }

  /**
   * Get accuracy trends over time
   */
  getAccuracyTrends(days: number = 30): Observable<QATrendData> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<QATrendData>(`${this.baseUrl}/trends`, { params });
  }
}
