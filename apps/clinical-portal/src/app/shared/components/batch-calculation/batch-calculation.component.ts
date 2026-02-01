import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import {
  BatchCalculationService,
  BatchCalculationJob,
  BatchJobStatus
} from '../../../services/batch-calculation.service';
import { LoggerService } from '../../../services/logger.service';

/**
 * Batch Calculation Component
 *
 * Provides UI for triggering and monitoring population-wide quality measure calculations
 *
 * Features:
 * - Start batch calculation for all patients
 * - Real-time progress monitoring
 * - Job history display
 * - Error reporting
 * - Job cancellation
 */
@Component({
  selector: 'app-batch-calculation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './batch-calculation.component.html',
  styleUrls: ['./batch-calculation.component.scss']
})
export class BatchCalculationComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private get logger() {
    return this.loggerService.withContext('BatchCalculationComponent');
  }

  // Current active job
  activeJob: BatchCalculationJob | null = null;
  isCalculating = false;
  isLoading = false;
  error: string | null = null;

  // Job history
  jobHistory: BatchCalculationJob[] = [];
  showHistory = true;

  // Error display
  showErrors = false;
  selectedJobErrors: string[] = [];

  // Expose enum to template
  BatchJobStatus = BatchJobStatus;

  constructor(
    private batchCalculationService: BatchCalculationService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadJobHistory();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Start batch calculation
   */
  startBatchCalculation(): void {
    if (this.isCalculating) {
      return;
    }

    this.isLoading = true;
    this.error = null;

    this.batchCalculationService
      .startBatchCalculation('http://fhir-service-mock:8080/fhir', 'clinical-portal-user')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isCalculating = true;
          this.isLoading = false;

          // Start polling for job status
          this.pollJobStatus(response.jobId);
        },
        error: (err) => {
          this.loggerService.error('Failed to start batch calculation', err);
          this.error = err.error?.message || 'Failed to start batch calculation';
          this.isLoading = false;
        }
      });
  }

  /**
   * Poll job status until completion
   */
  private pollJobStatus(jobId: string): void {
    this.batchCalculationService
      .pollJobStatus(jobId, 1000) // Poll every 1 second
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job) => {
          this.activeJob = job;

          // Check if job is complete
          if (job.status === BatchJobStatus.COMPLETED ||
              job.status === BatchJobStatus.FAILED ||
              job.status === BatchJobStatus.CANCELLED) {
            this.isCalculating = false;
            this.loadJobHistory(); // Refresh job history
          }
        },
        error: (err) => {
          this.loggerService.error('Error polling job status', err);
          this.error = 'Failed to get job status';
          this.isCalculating = false;
        },
        complete: () => {
          this.isCalculating = false;
        }
      });
  }

  /**
   * Cancel active job
   */
  cancelJob(): void {
    if (!this.activeJob) {
      return;
    }

    this.batchCalculationService
      .cancelJob(this.activeJob.jobId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isCalculating = false;
          this.loadJobHistory();
        },
        error: (err) => {
          this.loggerService.error('Failed to cancel job', err);
          this.error = err.error?.message || 'Failed to cancel job';
        }
      });
  }

  /**
   * Load job history
   */
  loadJobHistory(): void {
    this.batchCalculationService
      .getAllJobs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (jobs) => {
          this.jobHistory = jobs;

          // If no active job, check if there's a running job in history
          if (!this.activeJob && jobs.length > 0) {
            const runningJob = jobs.find(j =>
              j.status === BatchJobStatus.CALCULATING ||
              j.status === BatchJobStatus.FETCHING_PATIENTS ||
              j.status === BatchJobStatus.PENDING
            );

            if (runningJob) {
              this.activeJob = runningJob;
              this.isCalculating = true;
              this.pollJobStatus(runningJob.jobId);
            }
          }
        },
        error: (err) => {
          this.loggerService.error('Failed to load job history', err);
        }
      });
  }

  /**
   * Show errors for a job
   */
  showJobErrors(job: BatchCalculationJob): void {
    this.selectedJobErrors = job.errors || [];
    this.showErrors = true;
  }

  /**
   * Close error dialog
   */
  closeErrors(): void {
    this.showErrors = false;
    this.selectedJobErrors = [];
  }

  /**
   * Get status badge CSS class
   */
  getStatusClass(status: BatchJobStatus): string {
    return this.batchCalculationService.getStatusColor(status);
  }

  /**
   * Format duration
   */
  formatDuration(duration: string): string {
    return this.batchCalculationService.formatDuration(duration);
  }

  /**
   * Format timestamp
   */
  formatTimestamp(timestamp: string | null): string {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString();
  }

  /**
   * Calculate success rate
   */
  getSuccessRate(job: BatchCalculationJob): number {
    if (job.totalCalculations === 0) return 0;
    return Math.round((job.successfulCalculations / job.totalCalculations) * 100);
  }

  /**
   * Toggle job history visibility
   */
  toggleHistory(): void {
    this.showHistory = !this.showHistory;
  }

  /**
   * Refresh job history
   */
  refreshHistory(): void {
    this.loadJobHistory();
  }
}
