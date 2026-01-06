import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap, shareReplay, catchError, finalize } from 'rxjs/operators';
import {
  CqlEvaluation,
  EvaluationRequest,
  BatchEvaluationRequest,
  EvaluationResponse,
  BatchEvaluationResponse,
} from '../models/evaluation.model';
import {
  CalculateMeasureRequest,
  QualityMeasureResult,
  QualityScore,
  QualityReport,
  PopulationQualityReport,
  SavedReport,
  SavePatientReportRequest,
  SavePopulationReportRequest,
  ReportType,
  ExportFormat,
  LocalMeasureResult,
} from '../models/quality-result.model';
import {
  API_CONFIG,
  CQL_ENGINE_ENDPOINTS,
  QUALITY_MEASURE_ENDPOINTS,
  buildCqlEngineUrl,
  buildQualityMeasureUrl,
} from '../config/api.config';
import { AuditService } from './audit.service';

/**
 * EvaluationService - Handles patient evaluations and quality measure calculations
 * Communicates with both CQL Engine Service and Quality Measure Service
 *
 * Implements caching with 3-minute TTL and automatic invalidation on mutations
 */
@Injectable({
  providedIn: 'root',
})
export class EvaluationService {
  // Caching infrastructure
  private evaluationsCache$: Observable<CqlEvaluation[]> | null = null;
  private resultsCache$: Observable<QualityMeasureResult[]> | null = null;
  private evalCacheTimestamp = 0;
  private resultsCacheTimestamp = 0;
  private readonly EVAL_CACHE_TTL = 3 * 60 * 1000; // 3 minutes (shorter due to frequent updates)
  private readonly RESULTS_CACHE_TTL = 3 * 60 * 1000; // 3 minutes

  constructor(
    private http: HttpClient,
    private auditService: AuditService
  ) {}

  // ===== CQL Engine Evaluation Endpoints =====

  /**
   * Submit a single patient evaluation
   * Endpoint: POST /api/v1/cql/evaluations?libraryId={uuid}&patientId={id}
   * Automatically invalidates cache on success
   * Audit logged for HIPAA compliance
   */
  submitEvaluation(libraryId: string, patientId: string, contextData?: Record<string, any>): Observable<CqlEvaluation> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
      libraryId,
      patientId,
    });
    const startTime = Date.now();

    return this.http.post<CqlEvaluation>(url, contextData || {}).pipe(
      tap((result) => {
        this.invalidateEvaluationCache();
        this.auditService.logEvaluation({
          evaluationId: result.id,
          measureId: libraryId,
          patientIds: [patientId],
          success: true,
          durationMs: Date.now() - startTime,
        });
      }),
      catchError((error) => {
        this.auditService.logEvaluation({
          measureId: libraryId,
          patientIds: [patientId],
          success: false,
          durationMs: Date.now() - startTime,
          errorMessage: error.message || 'Evaluation failed',
        });
        throw error;
      })
    );
  }

  /**
   * Batch evaluate multiple patients
   * Endpoint: POST /api/v1/cql/evaluations/batch?libraryId={uuid}
   * Automatically invalidates cache on success
   * Audit logged for HIPAA compliance
   */
  batchEvaluate(libraryId: string, patientIds: string[]): Observable<BatchEvaluationResponse> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH, {
      libraryId,
    });
    const startTime = Date.now();
    const jobId = `batch-${Date.now()}`;

    // Log batch start
    this.auditService.logBatchEvaluationStart({
      jobId,
      measureIds: [libraryId],
      patientCount: patientIds.length,
    });

    return this.http.post<BatchEvaluationResponse>(url, patientIds).pipe(
      tap((results) => {
        this.invalidateEvaluationCache();
        const successCount = results.filter((e: CqlEvaluation) => e.status === 'SUCCESS').length;
        this.auditService.logBatchEvaluationComplete({
          jobId,
          success: true,
          totalEvaluations: results.length,
          successCount,
          failureCount: results.length - successCount,
          durationMs: Date.now() - startTime,
        });
      }),
      catchError((error) => {
        this.auditService.logBatchEvaluationComplete({
          jobId,
          success: false,
          totalEvaluations: patientIds.length,
          successCount: 0,
          failureCount: patientIds.length,
          durationMs: Date.now() - startTime,
          errorMessage: error.message || 'Batch evaluation failed',
        });
        throw error;
      })
    );
  }

  /**
   * Get all evaluations for a patient
   * Endpoint: GET /api/v1/cql/evaluations/patient/{patientId}
   */
  getPatientEvaluations(patientId: string): Observable<CqlEvaluation[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BY_PATIENT(patientId));
    return this.http.get<CqlEvaluation[]>(url);
  }

  /**
   * Get all evaluations for a library
   * Endpoint: GET /api/v1/cql/evaluations/library/{libraryId}
   */
  getLibraryEvaluations(libraryId: string): Observable<CqlEvaluation[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BY_LIBRARY(libraryId));
    return this.http.get<CqlEvaluation[]>(url);
  }

  /**
   * Get all evaluations (paginated)
   * Endpoint: GET /api/v1/cql/evaluations
   */
  getAllEvaluations(page: number = 0, size: number = 1000): Observable<CqlEvaluation[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
      page: page.toString(),
      size: size.toString(),
    });
    return this.http.get<any>(url).pipe(
      map((response: any) => {
        // Handle Spring Data REST paginated response format
        if (response && response.content && Array.isArray(response.content)) {
          return response.content as CqlEvaluation[];
        }
        // Fallback: if response is already an array
        if (Array.isArray(response)) {
          return response as CqlEvaluation[];
        }
        return [];
      })
    );
  }

  /**
   * Get all evaluations with caching (3-minute TTL)
   * Use this for dashboard and lists that don't need real-time data
   */
  getAllEvaluationsCached(page: number = 0, size: number = 1000): Observable<CqlEvaluation[]> {
    const now = Date.now();
    if (this.evaluationsCache$ && (now - this.evalCacheTimestamp) < this.EVAL_CACHE_TTL) {
      return this.evaluationsCache$;
    }
    this.evalCacheTimestamp = now;
    this.evaluationsCache$ = this.getAllEvaluations(page, size).pipe(shareReplay(1));
    return this.evaluationsCache$;
  }

  /**
   * Get recent evaluations (last N days) - lightweight method for dashboard
   */
  getRecentEvaluations(days: number = 30, limit: number = 10): Observable<CqlEvaluation[]> {
    return this.getAllEvaluationsCached(0, 100).pipe(
      map((evaluations) => {
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - days);
        return evaluations
          .filter((e) => new Date(e.createdAt) >= cutoffDate)
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, limit);
      })
    );
  }

  /**
   * Get evaluation statistics for dashboard (lightweight)
   */
  getEvaluationStats(): Observable<{ total: number; last30Days: number; successRate: number }> {
    return this.getAllEvaluationsCached(0, 1000).pipe(
      map((evaluations) => {
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 30);
        const recentEvals = evaluations.filter((e) => new Date(e.createdAt) >= cutoffDate);
        const successCount = evaluations.filter((e) => e.status === 'SUCCESS').length;

        return {
          total: evaluations.length,
          last30Days: recentEvals.length,
          successRate: evaluations.length > 0 ? Math.round((successCount / evaluations.length) * 100) : 0,
        };
      })
    );
  }

  /**
   * Invalidate evaluation cache
   * Called automatically after submit and batch operations
   */
  invalidateEvaluationCache(): void {
    this.evaluationsCache$ = null;
    this.evalCacheTimestamp = 0;
  }

  /**
   * Invalidate results cache
   */
  invalidateResultsCache(): void {
    this.resultsCache$ = null;
    this.resultsCacheTimestamp = 0;
  }

  /**
   * Invalidate all caches
   */
  invalidateAllCaches(): void {
    this.invalidateEvaluationCache();
    this.invalidateResultsCache();
  }

  /**
   * Check if evaluation cache is valid
   */
  isEvaluationCacheValid(): boolean {
    return (Date.now() - this.evalCacheTimestamp) < this.EVAL_CACHE_TTL;
  }

  /**
   * Simplified evaluation (used internally by Quality Measure Service)
   * Endpoint: POST /evaluate?library={name}&patient={id}
   */
  evaluateSimple(libraryName: string, patientId: string, params?: Record<string, any>): Observable<any> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATE_SIMPLE, {
      library: libraryName,
      patient: patientId,
    });
    return this.http.post<any>(url, params || {});
  }

  // ===== Quality Measure Service Endpoints =====

  /**
   * Calculate quality measure for a patient
   * Endpoint: POST /quality-measure/calculate?patient={patientId}&measure={measureId}
   * Automatically invalidates cache on success
   * Audit logged for HIPAA compliance
   */
  calculateQualityMeasure(
    patientId: string,
    measureId: string,
    createdBy: string = 'system'
  ): Observable<QualityMeasureResult> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE, {
      patient: patientId,
      measure: measureId,
      createdBy,
    });
    const startTime = Date.now();

    return this.http.post<QualityMeasureResult>(url, {}).pipe(
      tap((result) => {
        this.invalidateAllCaches();
        this.auditService.logEvaluation({
          evaluationId: result.id,
          measureId,
          patientIds: [patientId],
          success: true,
          durationMs: Date.now() - startTime,
        });
      }),
      catchError((error) => {
        this.auditService.logEvaluation({
          measureId,
          patientIds: [patientId],
          success: false,
          durationMs: Date.now() - startTime,
          errorMessage: error.message || 'Quality measure calculation failed',
        });
        throw error;
      })
    );
  }

  /**
   * Calculate quality measure using local Java implementation (bypasses CQL Engine)
   * Endpoint: POST /quality-measure/calculate-local?patient={patientId}&measure={measureId}
   * This is the preferred method for measures registered in MeasureRegistry (e.g., CDC)
   * Audit logged for HIPAA compliance
   */
  calculateLocalMeasure(
    patientId: string,
    measureId: string
  ): Observable<LocalMeasureResult> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE_LOCAL, {
      patient: patientId,
      measure: measureId,
    });
    const startTime = Date.now();

    return this.http.post<LocalMeasureResult>(url, {}).pipe(
      tap((result) => {
        this.invalidateAllCaches();
        this.auditService.logEvaluation({
          evaluationId: `local-${measureId}-${patientId}`,
          measureId,
          patientIds: [patientId],
          success: true,
          durationMs: Date.now() - startTime,
        });
      }),
      catchError((error) => {
        this.auditService.logEvaluation({
          measureId,
          patientIds: [patientId],
          success: false,
          durationMs: Date.now() - startTime,
          errorMessage: error.message || 'Local measure calculation failed',
        });
        throw error;
      })
    );
  }

  /**
   * Get all quality measure results for a patient
   * Endpoint: GET /quality-measure/results?patient={patientId}
   * If patientId is null/undefined, returns all results for the tenant
   */
  getPatientResults(patientId?: string | null, page?: number, size?: number): Observable<QualityMeasureResult[]> {
    const params: Record<string, string> = {};

    // Only add patient parameter if provided
    if (patientId) {
      params['patient'] = patientId;
    }

    // Add pagination parameters if provided
    if (page !== undefined) {
      params['page'] = page.toString();
    }
    if (size !== undefined) {
      params['size'] = size.toString();
    }

    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.RESULTS_BY_PATIENT, params);
    return this.http.get<QualityMeasureResult[]>(url);
  }

  /**
   * Get all quality measure results (all patients)
   * Endpoint: GET /quality-measure/results
   */
  getAllResults(page: number = 0, size: number = 20): Observable<QualityMeasureResult[]> {
    return this.getPatientResults(null, page, size);
  }

  /**
   * Get quality score for a patient
   * Endpoint: GET /quality-measure/score?patient={patientId}
   */
  getQualityScore(patientId: string): Observable<QualityScore> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.QUALITY_SCORE, {
      patient: patientId,
    });
    return this.http.get<QualityScore>(url);
  }

  /**
   * Get patient quality report
   * Endpoint: GET /quality-measure/report/patient?patient={patientId}
   */
  getPatientReport(patientId: string): Observable<QualityReport> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_REPORT, {
      patient: patientId,
    });
    return this.http.get<QualityReport>(url);
  }

  /**
   * Get population quality report
   * Endpoint: GET /quality-measure/report/population?year={year}
   */
  getPopulationReport(year: number): Observable<PopulationQualityReport> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.POPULATION_REPORT, {
      year: year.toString(),
    });
    return this.http.get<PopulationQualityReport>(url);
  }

  /**
   * Check Quality Measure Service health
   * Endpoint: GET /quality-measure/_health
   */
  checkHealth(): Observable<any> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.HEALTH);
    return this.http.get<any>(url);
  }

  // ===== Saved Reports Endpoints =====

  /**
   * Save patient quality report
   * Endpoint: POST /quality-measure/report/patient/save?patient={patientId}&name={reportName}
   */
  savePatientReport(
    patientId: string,
    reportName: string,
    createdBy: string = 'system'
  ): Observable<SavedReport> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVE_PATIENT_REPORT, {
      patient: patientId,
      name: reportName,
      createdBy,
    });
    return this.http.post<SavedReport>(url, {});
  }

  /**
   * Save population quality report
   * Endpoint: POST /quality-measure/report/population/save?year={year}&name={reportName}
   */
  savePopulationReport(
    year: number,
    reportName: string,
    createdBy: string = 'system'
  ): Observable<SavedReport> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVE_POPULATION_REPORT, {
      year: year.toString(),
      name: reportName,
      createdBy,
    });
    return this.http.post<SavedReport>(url, {});
  }

  /**
   * Get all saved reports for the tenant (optionally filtered by type)
   * Endpoint: GET /quality-measure/reports?type={reportType}
   */
  getSavedReports(reportType?: ReportType): Observable<SavedReport[]> {
    const params: Record<string, string> | undefined = reportType ? { type: reportType } : undefined;
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS, params);
    return this.http.get<SavedReport[]>(url);
  }

  /**
   * Get a saved report by ID
   * Endpoint: GET /quality-measure/reports/{reportId}
   */
  getSavedReport(reportId: string): Observable<SavedReport> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId));
    return this.http.get<SavedReport>(url);
  }

  /**
   * Delete a saved report
   * Endpoint: DELETE /quality-measure/reports/{reportId}
   */
  deleteSavedReport(reportId: string): Observable<void> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId));
    return this.http.delete<void>(url);
  }

  /**
   * Export report to CSV
   * Endpoint: GET /quality-measure/reports/{reportId}/export/csv
   * Returns a Blob that can be downloaded
   */
  exportReportToCsv(reportId: string): Observable<Blob> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.EXPORT_CSV(reportId));
    return this.http.get(url, {
      responseType: 'blob',
    });
  }

  /**
   * Export report to Excel
   * Endpoint: GET /quality-measure/reports/{reportId}/export/excel
   * Returns a Blob that can be downloaded
   */
  exportReportToExcel(reportId: string): Observable<Blob> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.EXPORT_EXCEL(reportId));
    return this.http.get(url, {
      responseType: 'blob',
    });
  }

  /**
   * Download exported report file
   * Helper method to trigger browser download
   */
  downloadReport(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Export and download report in specified format
   * Convenience method that combines export and download
   */
  exportAndDownloadReport(
    reportId: string,
    reportName: string,
    format: ExportFormat
  ): Observable<void> {
    const exportObservable =
      format === 'csv'
        ? this.exportReportToCsv(reportId)
        : this.exportReportToExcel(reportId);

    return new Observable((observer) => {
      exportObservable.subscribe({
        next: (blob) => {
          const extension = format === 'csv' ? 'csv' : 'xlsx';
          const filename = `${reportName}.${extension}`;
          this.downloadReport(blob, filename);
          observer.next();
          observer.complete();
        },
        error: (error) => {
          observer.error(error);
        },
      });
    });
  }
}
