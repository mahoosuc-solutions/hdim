import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, interval, switchMap, takeWhile, map, startWith } from 'rxjs';
import { environment } from '../../environments/environment';

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
  private baseUrl = `${environment.apiUrl}/qrda`;

  /**
   * Generate QRDA Category I documents (patient-level).
   * Creates individual QRDA documents for each patient's measure results.
   */
  generateCategoryI(request: QrdaExportRequest): Observable<QrdaExportJob> {
    const payload = { ...request, jobType: 'QRDA_I' };
    return this.http.post<QrdaExportJob>(`${this.baseUrl}/category-i/generate`, payload);
  }

  /**
   * Generate QRDA Category III document (aggregate).
   * Creates a single aggregate document with population-level measure results.
   */
  generateCategoryIII(request: QrdaExportRequest): Observable<QrdaExportJob> {
    const payload = { ...request, jobType: 'QRDA_III' };
    return this.http.post<QrdaExportJob>(`${this.baseUrl}/category-iii/generate`, payload);
  }

  /**
   * Get the status of a QRDA export job.
   */
  getJobStatus(jobId: string): Observable<QrdaExportJob> {
    return this.http.get<QrdaExportJob>(`${this.baseUrl}/jobs/${jobId}`);
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
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (jobType) {
      params = params.set('jobType', jobType);
    }
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<QrdaExportJobPage>(`${this.baseUrl}/jobs`, { params });
  }

  /**
   * Download the generated QRDA document(s) for a completed job.
   * Returns the file as a Blob for browser download.
   */
  downloadDocument(jobId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/jobs/${jobId}/download`, {
      responseType: 'blob',
    });
  }

  /**
   * Cancel a pending or running export job.
   */
  cancelJob(jobId: string): Observable<QrdaExportJob> {
    return this.http.post<QrdaExportJob>(`${this.baseUrl}/jobs/${jobId}/cancel`, {});
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
