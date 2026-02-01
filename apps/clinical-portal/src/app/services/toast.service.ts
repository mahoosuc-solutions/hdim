import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

/**
 * Toast Notification Service
 *
 * Provides user-friendly toast notifications using Material Snackbar
 *
 * Usage:
 * ```typescript
 * constructor(private toast: ToastService) {}
 *
 * // Success notification
 * this.toast.success('Report deleted successfully');
 *
 * // Error notification
 * this.toast.error('Failed to delete report');
 *
 * // Info notification
 * this.toast.info('Loading report data...');
 *
 * // Warning notification
 * this.toast.warning('Report generation may take a few moments');
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private defaultConfig: MatSnackBarConfig = {
    duration: 3000,
    horizontalPosition: 'end',
    verticalPosition: 'bottom',
  };

  constructor(private snackBar: MatSnackBar) {}

  /**
   * Show success notification (green)
   */
  success(message: string, duration = 3000): void {
    this.snackBar.open(message, 'Close', {
      ...this.defaultConfig,
      duration,
      panelClass: ['toast-success']
    });
  }

  /**
   * Show error notification (red)
   */
  error(message: string, duration = 5000): void {
    this.snackBar.open(message, 'Close', {
      ...this.defaultConfig,
      duration,
      panelClass: ['toast-error']
    });
  }

  /**
   * Show info notification (blue)
   */
  info(message: string, duration = 3000): void {
    this.snackBar.open(message, 'Close', {
      ...this.defaultConfig,
      duration,
      panelClass: ['toast-info']
    });
  }

  /**
   * Show warning notification (orange)
   */
  warning(message: string, duration = 4000): void {
    this.snackBar.open(message, 'Close', {
      ...this.defaultConfig,
      duration,
      panelClass: ['toast-warning']
    });
  }
}
