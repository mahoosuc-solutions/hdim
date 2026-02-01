import { Injectable } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { Patient } from '../models/patient.model';
import { PatientEditDialogComponent, PatientEditDialogData } from '../dialogs/patient-edit-dialog/patient-edit-dialog.component';
import { EvaluationDetailsDialogComponent, EvaluationDetailsDialogData } from '../dialogs/evaluation-details-dialog/evaluation-details-dialog.component';
import { AdvancedFilterDialogComponent, AdvancedFilterDialogData, FilterConfig, FilterField } from '../dialogs/advanced-filter-dialog/advanced-filter-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../components/dialogs/confirm-dialog.component';
import { LoggerService } from './logger.service';

/**
 * Batch evaluation result
 */
export interface BatchResult {
  successCount: number;
  errorCount: number;
  results: any[];
}

/**
 * Export configuration
 */
export interface ExportConfig {
  format: 'csv' | 'excel' | 'pdf';
  columns: string[];
  fileName: string;
  dateRange?: { start: Date; end: Date };
}

/**
 * Error information
 */
export interface ErrorInfo {
  message: string;
  stack?: string;
  timestamp: Date;
  severity: 'error' | 'warning' | 'info';
  requestDetails?: any;
  responseDetails?: any;
}

/**
 * Centralized Dialog Service
 *
 * Provides a unified interface for opening all application dialogs with
 * consistent configuration, error handling, and responsive behavior.
 *
 * Benefits:
 * - Single point of configuration for all dialogs
 * - Consistent mobile responsiveness
 * - Type-safe dialog interfaces
 * - Simplified component code
 * - Easy to mock for testing
 *
 * Usage:
 * ```typescript
 * constructor(private dialogService: DialogService) {}
 *
 * openDialog(): void {
 *   this.dialogService.openPatientEdit().subscribe(patient => {
 *     if (patient) {
 *       // Handle saved patient
 *     }
 *   });
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class DialogService {
  private readonly defaultConfig: MatDialogConfig = {
    disableClose: false,
    autoFocus: true,
    restoreFocus: true,
    panelClass: 'custom-dialog-container',
  };  constructor(
    private dialog: MatDialog,
    private logger: LoggerService
  ) {}

  /**
   * Open Patient Edit Dialog
   *
   * @param patient Optional patient to edit (omit for create mode)
   * @returns Observable that emits the saved patient or null if cancelled
   */
  openPatientEdit(patient?: Patient): Observable<Patient | null> {
    const data: PatientEditDialogData = {
      mode: patient ? 'edit' : 'create',
      patient,
    };

    const dialogRef = this.dialog.open(PatientEditDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '95vh',
    });

    return dialogRef.afterClosed();
  }

  /**
   * Open Evaluation Details Dialog
   *
   * @param evaluationId ID of the evaluation to display
   * @param patientName Optional patient name for display
   * @param measureName Optional measure name for display
   */
  openEvaluationDetails(
    evaluationId: string,
    patientName?: string,
    measureName?: string
  ): void {
    const data: EvaluationDetailsDialogData = {
      evaluationId,
      patientName,
      measureName,
    };

    this.dialog.open(EvaluationDetailsDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '90vh',
    });
  }

  /**
   * Open Advanced Filter Dialog
   *
   * @param availableFields Array of fields that can be filtered
   * @param currentFilters Optional existing filter configuration
   * @returns Observable that emits the filter configuration or null if cancelled
   */
  openAdvancedFilter(
    availableFields: FilterField[],
    currentFilters?: FilterConfig
  ): Observable<FilterConfig | null> {
    const data: AdvancedFilterDialogData = {
      availableFields,
      currentFilters,
    };

    const dialogRef = this.dialog.open(AdvancedFilterDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '85vh',
    });

    return dialogRef.afterClosed();
  }

  /**
   * Open Batch Evaluation Dialog
   *
   * @returns Observable that emits batch result or null if cancelled
   */
  openBatchEvaluation(): Observable<BatchResult | null> {
    // Note: BatchEvaluationDialogComponent would be imported here
    // For now, returning mock implementation
    return new Observable((observer) => {
      // Mock implementation - replace with actual dialog
      setTimeout(() => {
        observer.next({
          successCount: 10,
          errorCount: 0,
          results: [],
        });
        observer.complete();
      }, 1000);
    });
  }

  /**
   * Open Export Configuration Dialog
   *
   * @param columns Available columns for export
   * @param data Data to export
   * @returns Observable that emits export configuration or null if cancelled
   */
  openExportConfig(
    columns: string[],
    data: any[]
  ): Observable<ExportConfig | null> {
    // Note: ExportConfigDialogComponent would be imported here
    // Mock implementation
    return new Observable((observer) => {
      setTimeout(() => {
        observer.next({
          format: 'csv',
          columns,
          fileName: 'export.csv',
        });
        observer.complete();
      }, 500);
    });
  }

  /**
   * Open Error Details Dialog
   *
   * @param error Error object or error information
   */
  openErrorDetails(error: Error | ErrorInfo): void {
    const errorInfo: ErrorInfo =
      error instanceof Error
        ? {
            message: error.message,
            stack: error.stack,
            timestamp: new Date(),
            severity: 'error',
          }
        : error;

    // Note: ErrorDetailsDialogComponent would be imported here
    this.logger.error('Error Details', errorInfo);
  }

  /**
   * Open Help Dialog
   *
   * @param topic Help topic to display
   */
  openHelp(topic: string): void {
    // Note: HelpDialogComponent would be imported here
    this.logger.info('Opening help for topic', topic);
  }

  /**
   * Open generic confirmation dialog
   *
   * @param title Dialog title
   * @param message Confirmation message (can include HTML)
   * @param confirmText Text for confirm button (default: 'Confirm')
   * @param cancelText Text for cancel button (default: 'Cancel')
   * @param confirmColor Color for confirm button (default: 'primary')
   * @returns Observable that emits true if confirmed, false if cancelled
   */
  confirm(
    title: string,
    message: string,
    confirmText = 'Confirm',
    cancelText = 'Cancel',
    confirmColor: 'primary' | 'accent' | 'warn' = 'primary'
  ): Observable<boolean> {
    const data: ConfirmDialogData = {
      title,
      message,
      confirmText,
      cancelText,
      confirmColor,
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '450px',
      maxWidth: '90vw',
    });

    return dialogRef.afterClosed().pipe(map((result) => !!result));
  }

  /**
   * Open warning confirmation dialog
   *
   * @param title Dialog title
   * @param message Warning message
   * @returns Observable that emits true if confirmed, false if cancelled
   */
  confirmWarning(title: string, message: string): Observable<boolean> {
    const data: ConfirmDialogData = {
      title,
      message,
      confirmText: 'Continue',
      cancelText: 'Cancel',
      confirmColor: 'warn',
      icon: 'warning',
      iconColor: '#ff9800',
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '450px',
      maxWidth: '90vw',
    });

    return dialogRef.afterClosed().pipe(map((result) => !!result));
  }

  /**
   * Open delete confirmation dialog
   *
   * @param itemName Name of item to delete
   * @param itemType Type of item (e.g., 'patient', 'report')
   * @returns Observable that emits true if confirmed, false if cancelled
   */
  confirmDelete(itemName: string, itemType = 'item'): Observable<boolean> {
    const data: ConfirmDialogData = {
      title: `Delete ${itemType}?`,
      message: `Are you sure you want to delete "<strong>${itemName}</strong>"?<br><br>This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmColor: 'warn',
      icon: 'warning',
      iconColor: '#f44336',
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '450px',
      maxWidth: '90vw',
    });

    return dialogRef.afterClosed().pipe(map((result) => !!result));
  }

  /**
   * Close all open dialogs
   */
  closeAll(): void {
    this.dialog.closeAll();
  }

  /**
   * Check if any dialogs are currently open
   */
  hasOpenDialogs(): boolean {
    return this.dialog.openDialogs.length > 0;
  }

  /**
   * Get count of open dialogs
   */
  getOpenDialogCount(): number {
    return this.dialog.openDialogs.length;
  }
}
