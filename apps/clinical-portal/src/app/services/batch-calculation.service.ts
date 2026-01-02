import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { buildQualityMeasureUrl, QUALITY_MEASURE_ENDPOINTS } from '../config/api.config';

/**
 * Batch Calculation Job Status
 */
export enum BatchJobStatus {
  PENDING = 'PENDING',
  FETCHING_PATIENTS = 'FETCHING_PATIENTS',
  CALCULATING = 'CALCULATING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

/**
 * Batch Calculation Job
 */
export interface BatchCalculationJob {
  jobId: string;
  tenantId: string;
  status: BatchJobStatus;
  createdBy: string;
  startedAt: string;
  completedAt: string | null;
  totalPatients: number;
  totalMeasures: number;
  totalCalculations: number;
  completedCalculations: number;
  successfulCalculations: number;
  failedCalculations: number;
  progressPercent: number;
  duration: string;
  errors?: string[];
}

/**
 * Start Batch Calculation Response
 */
export interface StartBatchCalculationResponse {
  jobId: string;
  status: string;
  message: string;
  tenantId: string;
}

/**
 * Cancel Job Response
 */
export interface CancelJobResponse {
  jobId: string;
  status: string;
  message: string;
}

/**
 * Batch Calculation Service
 *
 * Handles batch calculation of quality measures for entire patient population
 */
@Injectable({
  providedIn: 'root'
})
export class BatchCalculationService {

  constructor(private http: HttpClient) {}

  /**
   * Start batch calculation for all patients and all measures
   *
   * @param fhirServerUrl FHIR server URL (optional)
   * @param createdBy User who triggered the calculation
   * @returns Observable of start response with job ID
   */
  startBatchCalculation(
    fhirServerUrl?: string,
    createdBy?: string
  ): Observable<StartBatchCalculationResponse> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.POPULATION_CALCULATE);

    let params = new HttpParams();
    if (fhirServerUrl) {
      params = params.set('fhirServerUrl', fhirServerUrl);
    }
    if (createdBy) {
      params = params.set('createdBy', createdBy);
    }

    return this.http.post<StartBatchCalculationResponse>(url, null, { params });
  }

  /**
   * Get status of a specific batch calculation job
   *
   * @param jobId Job ID
   * @returns Observable of job status
   */
  getJobStatus(jobId: string): Observable<BatchCalculationJob> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.POPULATION_JOB_BY_ID(jobId));
    return this.http.get<BatchCalculationJob>(url);
  }

  /**
   * Get all batch calculation jobs for the current tenant
   *
   * @returns Observable of job list
   */
  getAllJobs(): Observable<BatchCalculationJob[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.POPULATION_JOBS);
    return this.http.get<BatchCalculationJob[]>(url);
  }

  /**
   * Cancel a running batch calculation job
   *
   * @param jobId Job ID to cancel
   * @returns Observable of cancel response
   */
  cancelJob(jobId: string): Observable<CancelJobResponse> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.POPULATION_JOB_CANCEL(jobId));
    return this.http.post<CancelJobResponse>(url, null);
  }

  /**
   * Poll job status until completion
   *
   * @param jobId Job ID
   * @param intervalMs Polling interval in milliseconds (default: 2000)
   * @returns Observable that emits job status updates
   */
  pollJobStatus(jobId: string, intervalMs: number = 2000): Observable<BatchCalculationJob> {
    return new Observable(observer => {
      const poll = () => {
        this.getJobStatus(jobId).subscribe({
          next: (job) => {
            observer.next(job);

            // Stop polling if job is complete
            if (job.status === BatchJobStatus.COMPLETED ||
                job.status === BatchJobStatus.FAILED ||
                job.status === BatchJobStatus.CANCELLED) {
              observer.complete();
            } else {
              // Continue polling
              setTimeout(poll, intervalMs);
            }
          },
          error: (err) => {
            observer.error(err);
          }
        });
      };

      // Start polling
      poll();
    });
  }

  /**
   * Format duration string (e.g., "PT1.906862298S" -> "1.9s")
   *
   * @param duration ISO 8601 duration string
   * @returns Formatted duration
   */
  formatDuration(duration: string): string {
    if (!duration) return '-';

    // Parse ISO 8601 duration (PT1.906862298S)
    const match = duration.match(/PT(\d+(?:\.\d+)?)[HMS]/);
    if (match) {
      const value = parseFloat(match[1]);
      if (duration.includes('H')) return `${value.toFixed(1)}h`;
      if (duration.includes('M')) return `${value.toFixed(1)}m`;
      if (duration.includes('S')) return `${value.toFixed(1)}s`;
    }

    return duration;
  }

  /**
   * Get status badge color for job status
   *
   * @param status Job status
   * @returns CSS class for badge color
   */
  getStatusColor(status: BatchJobStatus): string {
    switch (status) {
      case BatchJobStatus.PENDING:
      case BatchJobStatus.FETCHING_PATIENTS:
        return 'bg-blue-100 text-blue-800';
      case BatchJobStatus.CALCULATING:
        return 'bg-yellow-100 text-yellow-800';
      case BatchJobStatus.COMPLETED:
        return 'bg-green-100 text-green-800';
      case BatchJobStatus.FAILED:
        return 'bg-red-100 text-red-800';
      case BatchJobStatus.CANCELLED:
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}
