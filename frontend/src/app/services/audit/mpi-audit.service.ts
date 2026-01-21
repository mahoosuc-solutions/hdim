import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MPIMergeFilter {
  mergeStatus?: string;
  validationStatus?: string;
  startDate?: string;
  endDate?: string;
  mergeType?: string;
  minConfidenceScore?: number;
  maxConfidenceScore?: number;
}

export interface MPIMergeEvent {
  mergeId: string;
  sourcePatientId: string;
  targetPatientId: string;
  confidenceScore: number;
  mergeStatus: string;
  validationStatus: string;
  mergeTimestamp: string;
  dataQualityIssueCount: number;
  priority: string;
}

export interface MPIMergeDetail {
  mergeId: string;
  mergeStatus: string;
  validationStatus: string;
  confidenceScore: number;
  sourcePatientSnapshot: any;
  targetPatientSnapshot: any;
  mergedPatientSnapshot: any;
  matchingAlgorithmDetails: any;
  attributeMatches: any[];
  attributeConflicts: any[];
  dataQualityIssues: any[];
  validationHistory: any[];
}

export interface MPIValidationRequest {
  validationOutcome: string;
  validationNotes: string;
  hasMergeErrors?: boolean;
  hasDataQualityIssues?: boolean;
  dataQualityAssessment?: string;
}

export interface MPIValidationResult {
  mergeId: string;
  validationOutcome: string;
  validatedBy: string;
  validatedAt: string;
  success: boolean;
  message: string;
}

export interface MPIRollbackRequest {
  rollbackReason: string;
  recreateSourcePatient?: boolean;
  preserveTargetPatient?: boolean;
  rollbackStrategy?: string;
}

export interface MPIRollbackResult {
  mergeId: string;
  rollbackStatus: string;
  rolledBackBy: string;
  rolledBackAt: string;
  success: boolean;
  message: string;
  restoredSourcePatientId?: string;
  updatedTargetPatientId?: string;
}

export interface DataQualityIssueDTO {
  issueId: string;
  patientId: string;
  issueType: string;
  severity: string;
  status: string;
  affectedField: string;
  currentValue: string;
  suggestedValue?: string;
  recommendation: string;
  detectedAt: string;
}

export interface DataQualityResolveRequest {
  resolutionAction: string;
  correctedValue?: string;
  resolutionNotes: string;
}

export interface MPIMetrics {
  totalMerges: number;
  validatedMerges: number;
  rolledBackMerges: number;
  pendingValidation: number;
  validationRate: number;
  rollbackRate: number;
  averageConfidenceScore: number;
  averageValidationTimeMinutes: number;
  mergeTypeDistribution: any;
  dataQualityMetrics: any;
}

export interface MPITrendData {
  dailyTrends: any[];
  averageValidationRate: number;
  averageRollbackRate: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Service for MPI Audit API operations
 */
@Injectable({
  providedIn: 'root'
})
export class MpiAuditService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/mpi`;

  constructor(private http: HttpClient) {}

  /**
   * Get MPI merge history with filtering
   */
  getMergeHistory(
    filter: MPIMergeFilter = {},
    page: number = 0,
    size: number = 20,
    sort: string = 'mergeTimestamp,desc'
  ): Observable<PageResponse<MPIMergeEvent>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filter.mergeStatus) params = params.set('mergeStatus', filter.mergeStatus);
    if (filter.validationStatus) params = params.set('validationStatus', filter.validationStatus);
    if (filter.startDate) params = params.set('startDate', filter.startDate);
    if (filter.endDate) params = params.set('endDate', filter.endDate);
    if (filter.mergeType) params = params.set('mergeType', filter.mergeType);
    if (filter.minConfidenceScore !== undefined) {
      params = params.set('minConfidenceScore', filter.minConfidenceScore.toString());
    }
    if (filter.maxConfidenceScore !== undefined) {
      params = params.set('maxConfidenceScore', filter.maxConfidenceScore.toString());
    }

    return this.http.get<PageResponse<MPIMergeEvent>>(`${this.baseUrl}/merges`, { params });
  }

  /**
   * Get detailed information about a specific merge
   */
  getMergeDetail(mergeId: string): Observable<MPIMergeDetail> {
    return this.http.get<MPIMergeDetail>(`${this.baseUrl}/merges/${mergeId}`);
  }

  /**
   * Validate a merge operation
   */
  validateMerge(mergeId: string, request: MPIValidationRequest): Observable<MPIValidationResult> {
    return this.http.post<MPIValidationResult>(`${this.baseUrl}/merges/${mergeId}/validate`, request);
  }

  /**
   * Rollback a merge operation
   */
  rollbackMerge(mergeId: string, request: MPIRollbackRequest): Observable<MPIRollbackResult> {
    return this.http.post<MPIRollbackResult>(`${this.baseUrl}/merges/${mergeId}/rollback`, request);
  }

  /**
   * Get data quality issues with filtering
   */
  getDataQualityIssues(
    status?: string,
    severity?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<DataQualityIssueDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) params = params.set('status', status);
    if (severity) params = params.set('severity', severity);

    return this.http.get<PageResponse<DataQualityIssueDTO>>(`${this.baseUrl}/data-quality/issues`, { params });
  }

  /**
   * Resolve a data quality issue
   */
  resolveDataQualityIssue(issueId: string, request: DataQualityResolveRequest): Observable<DataQualityIssueDTO> {
    return this.http.post<DataQualityIssueDTO>(`${this.baseUrl}/data-quality/issues/${issueId}/resolve`, request);
  }

  /**
   * Get MPI performance metrics
   */
  getMetrics(startDate?: string, endDate?: string): Observable<MPIMetrics> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<MPIMetrics>(`${this.baseUrl}/metrics`, { params });
  }

  /**
   * Get accuracy trends over time
   */
  getAccuracyTrends(days: number = 30): Observable<MPITrendData> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<MPITrendData>(`${this.baseUrl}/trends`, { params });
  }
}
