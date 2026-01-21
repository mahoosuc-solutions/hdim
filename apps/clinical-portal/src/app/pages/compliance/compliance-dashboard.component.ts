import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, KeyValuePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { Subject, takeUntil } from 'rxjs';
import { ErrorValidationService, ErrorSummary, ComplianceReport, TrackedError, ErrorAlert } from '../../services/error-validation.service';
import { ErrorCode, ErrorSeverity } from '../../models/error.model';
import { COMPLIANCE_CONFIG } from '../../config/compliance.config';

@Component({
  selector: 'app-compliance-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatSnackBarModule,
    MatDividerModule,
    KeyValuePipe,
  ],
  templateUrl: './compliance-dashboard.component.html',
  styleUrl: './compliance-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComplianceDashboardComponent implements OnInit, OnDestroy {
  summary: ErrorSummary | null = null;
  report: ComplianceReport | null = null;
  complianceConfig = COMPLIANCE_CONFIG;
  displayedColumns: string[] = ['timestamp', 'service', 'operation', 'endpoint', 'errorCode', 'severity', 'message'];
  recentErrors: TrackedError[] = [];
  alerts: ErrorAlert[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private errorValidationService: ErrorValidationService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadComplianceData();
    this.subscribeToAlerts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Subscribe to error alerts
   */
  private subscribeToAlerts(): void {
    this.errorValidationService.alerts$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(alert => {
      this.alerts.push(alert);
      // Keep only last 10 alerts
      if (this.alerts.length > 10) {
        this.alerts = this.alerts.slice(-10);
      }
      this.cdr.markForCheck();
      
      // Show snackbar notification
      this.showMessage(alert.message, alert.severity === 'critical' ? 'error' : 'info');
    });
  }

  /**
   * Load compliance data from error validation service
   */
  loadComplianceData(): void {
    this.summary = this.errorValidationService.getErrorSummary();
    this.report = this.errorValidationService.validateCompliance();
    this.recentErrors = this.summary.recentErrors.slice(0, 50); // Show last 50 errors
    this.cdr.markForCheck();
  }

  /**
   * Refresh compliance data
   */
  refresh(): void {
    this.loadComplianceData();
    this.showMessage('Compliance data refreshed', 'success');
  }

  /**
   * Clear all tracked errors
   */
  clearErrors(): void {
    if (confirm('Are you sure you want to clear all tracked errors? This action cannot be undone.')) {
      this.errorValidationService.clearErrors();
      this.loadComplianceData();
      this.showMessage('All errors cleared', 'success');
    }
  }

  /**
   * Export compliance report as JSON
   */
  exportAsJson(): void {
    if (!this.report) {
      this.showMessage('No compliance data available to export', 'error');
      return;
    }

    const json = JSON.stringify(this.report, null, 2);
    this.downloadFile(json, `compliance-report-${new Date().toISOString()}.json`, 'application/json');
    this.showMessage('Compliance report exported as JSON', 'success');
  }

  /**
   * Export compliance report as CSV
   */
  exportAsCsv(): void {
    if (!this.report || this.report.errors.length === 0) {
      this.showMessage('No errors available to export', 'error');
      return;
    }

    // CSV header
    const headers = ['Timestamp', 'Service', 'Operation', 'Endpoint', 'Error Code', 'Severity', 'Message'];
    const rows = this.report.errors.map((error) => [
      error.timestamp.toISOString(),
      error.context.service,
      error.context.operation,
      error.context.endpoint || '',
      error.context.errorCode,
      error.context.severity,
      `"${error.message.replace(/"/g, '""')}"`, // Escape quotes in CSV
    ]);

    const csv = [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');
    this.downloadFile(csv, `compliance-errors-${new Date().toISOString()}.csv`, 'text/csv');
    this.showMessage('Compliance errors exported as CSV', 'success');
  }

  /**
   * Get color for severity chip
   */
  getSeverityColor(severity: ErrorSeverity): string {
    switch (severity) {
      case ErrorSeverity.CRITICAL:
        return 'warn';
      case ErrorSeverity.ERROR:
        return 'warn';
      case ErrorSeverity.WARNING:
        return 'accent';
      case ErrorSeverity.INFO:
        return 'primary';
      default:
        return '';
    }
  }

  /**
   * Get color for compliance score progress bar
   */
  getComplianceScoreColor(score: number): string {
    if (score >= 90) return 'primary';
    if (score >= 70) return 'accent';
    return 'warn';
  }

  /**
   * Get error code name (without ERR- prefix)
   */
  getErrorCodeName(code: ErrorCode): string {
    return code.replace('ERR-', '');
  }

  /**
   * Download file with given content
   */
  private downloadFile(content: string, filename: string, mimeType: string): void {
    const blob = new Blob([content], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  /**
   * Show snackbar message
   */
  private showMessage(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: [`snackbar-${type}`],
    });
  }
}
