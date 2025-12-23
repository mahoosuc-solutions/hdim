import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, interval, switchMap, takeWhile, startWith, tap, catchError } from 'rxjs';
import { API_CONFIG, QRDA_EXPORT_ENDPOINTS, buildQrdaExportUrl } from '../config/api.config';
import { AuditService } from './audit.service';

export type QrdaJobType = 'QRDA_I' | 'QRDA_III';
export type QrdaJobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface QrdaExportRequest {
  jobType: QrdaJobType;
  measureIds: string[];
  patientIds?: string[];
  periodStart: string; // ISO date format: YYYY-MM-DD
  periodEnd: string;
  validateDocuments?: boolean;
  includeSupplementalData?: boolean;
}

export interface QrdaExportJob {
  id: string;
  tenantId: string;
  jobType: QrdaJobType;
  status: QrdaJobStatus;
  measureIds: string[];
  periodStart: string;
  periodEnd: string;
  documentLocation?: string;
  documentCount?: number;
  patientCount?: number;
  errorMessage?: string;
  validationErrors?: string[];
  requestedBy: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
}

export interface QrdaExportJobPage {
  content: QrdaExportJob[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/**
 * Service for managing QRDA document exports.
 * QRDA (Quality Reporting Document Architecture) is the CMS standard for
 * electronic quality measure reporting.
 *
 * - Category I: Patient-level reports for individual measure results
 * - Category III: Aggregate population-level reports
 */
@Injectable({
  providedIn: 'root',
})
export class QrdaExportService {
  private http = inject(HttpClient);
  private auditService = inject(AuditService);

  /**
   * Generate QRDA Category I documents (patient-level).
   * Creates individual QRDA documents for each patient's measure results.
   * Audit logged for HIPAA compliance.
   */
  generateCategoryI(request: QrdaExportRequest): Observable<QrdaExportJob> {
    const payload = { ...request, jobType: 'QRDA_I' };
    return this.http.post<QrdaExportJob>(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.GENERATE_CATEGORY_I),
      payload
    ).pipe(
      tap((job) => {
        this.auditService.logQrdaExport({
          jobId: job.id,
          category: 'I',
          measureIds: request.measureIds,
          patientCount: request.patientIds?.length,
          success: true,
        });
      }),
      catchError((error) => {
        this.auditService.logQrdaExport({
          jobId: `failed-${Date.now()}`,
          category: 'I',
          measureIds: request.measureIds,
          patientCount: request.patientIds?.length,
          success: false,
          errorMessage: error.message || 'QRDA Category I export failed',
        });
        throw error;
      })
    );
  }

  /**
   * Generate QRDA Category III document (aggregate).
   * Creates a single aggregate document with population-level measure results.
   * Audit logged for HIPAA compliance.
   */
  generateCategoryIII(request: QrdaExportRequest): Observable<QrdaExportJob> {
    const payload = { ...request, jobType: 'QRDA_III' };
    return this.http.post<QrdaExportJob>(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.GENERATE_CATEGORY_III),
      payload
    ).pipe(
      tap((job) => {
        this.auditService.logQrdaExport({
          jobId: job.id,
          category: 'III',
          measureIds: request.measureIds,
          success: true,
        });
      }),
      catchError((error) => {
        this.auditService.logQrdaExport({
          jobId: `failed-${Date.now()}`,
          category: 'III',
          measureIds: request.measureIds,
          success: false,
          errorMessage: error.message || 'QRDA Category III export failed',
        });
        throw error;
      })
    );
  }

  /**
   * Get the status of a QRDA export job.
   */
  getJobStatus(jobId: string): Observable<QrdaExportJob> {
    return this.http.get<QrdaExportJob>(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.JOB_BY_ID(jobId))
    );
  }

  /**
   * List all QRDA export jobs with optional filtering.
   */
  listJobs(
    page = 0,
    size = 20,
    jobType?: QrdaJobType,
    status?: QrdaJobStatus
  ): Observable<QrdaExportJobPage> {
    const params: Record<string, string> = {
      page: page.toString(),
      size: size.toString(),
    };

    if (jobType) {
      params['jobType'] = jobType;
    }
    if (status) {
      params['status'] = status;
    }

    return this.http.get<QrdaExportJobPage>(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.JOBS, params)
    );
  }

  /**
   * Download the generated QRDA document(s) for a completed job.
   * Returns the file as a Blob for browser download.
   * Audit logged for HIPAA compliance.
   */
  downloadDocument(jobId: string): Observable<Blob> {
    return this.http.get(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.JOB_DOWNLOAD(jobId)),
      { responseType: 'blob' }
    ).pipe(
      tap(() => {
        this.auditService.logQrdaDownload(jobId, `qrda-export-${jobId}`);
      })
    );
  }

  /**
   * Cancel a pending or running export job.
   */
  cancelJob(jobId: string): Observable<QrdaExportJob> {
    return this.http.post<QrdaExportJob>(
      buildQrdaExportUrl(QRDA_EXPORT_ENDPOINTS.JOB_CANCEL(jobId)),
      {}
    );
  }

  /**
   * Poll job status until completion, failure, or cancellation.
   * Useful for showing progress in the UI.
   *
   * @param jobId The job ID to poll
   * @param pollIntervalMs Polling interval in milliseconds (default: 2000)
   */
  pollJobUntilComplete(jobId: string, pollIntervalMs = 2000): Observable<QrdaExportJob> {
    return interval(pollIntervalMs).pipe(
      startWith(0),
      switchMap(() => this.getJobStatus(jobId)),
      takeWhile(
        (job) => job.status === 'PENDING' || job.status === 'RUNNING',
        true // Include the final emission
      )
    );
  }

  /**
   * Helper to trigger browser download of a blob.
   * Note: Audit logging happens in downloadDocument() method.
   */
  triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Get a formatted filename for the export based on job type and date.
   */
  getExportFilename(job: QrdaExportJob): string {
    const dateStr = new Date().toISOString().split('T')[0];
    if (job.jobType === 'QRDA_I') {
      return `qrda-i-export-${dateStr}.zip`;
    } else {
      return `qrda-iii-export-${dateStr}.xml`;
    }
  }
}
