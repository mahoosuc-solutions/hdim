import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

/**
 * Notification Service - Displays toast notifications using Material Snackbar
 *
 * Features:
 * - Success, error, warning, and info notifications
 * - Customizable duration and position
 * - Action buttons support
 * - Auto-dismiss or manual dismiss
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly defaultDuration = 3000; // 3 seconds
  private readonly defaultPosition: 'top' | 'bottom' = 'bottom';
  private readonly defaultHorizontalPosition: 'start' | 'center' | 'end' | 'left' | 'right' = 'center';

  constructor(private snackBar: MatSnackBar) {}

  /**
   * Show success notification
   */
  success(message: string, duration?: number, action?: string): void {
    this.show(message, 'success-snackbar', duration, action);
  }

  /**
   * Show error notification
   */
  error(message: string, duration?: number, action?: string): void {
    this.show(message, 'error-snackbar', duration || 5000, action); // Errors stay longer
  }

  /**
   * Show warning notification
   */
  warning(message: string, duration?: number, action?: string): void {
    this.show(message, 'warning-snackbar', duration || 4000, action);
  }

  /**
   * Show info notification
   */
  info(message: string, duration?: number, action?: string): void {
    this.show(message, 'info-snackbar', duration, action);
  }

  /**
   * Show custom notification
   */
  show(
    message: string,
    panelClass: string = '',
    duration?: number,
    action?: string
  ): void {
    const config: MatSnackBarConfig = {
      duration: duration || this.defaultDuration,
      horizontalPosition: this.defaultHorizontalPosition,
      verticalPosition: this.defaultPosition,
      panelClass: panelClass ? [panelClass] : [],
    };

    this.snackBar.open(message, action || 'Close', config);
  }

  /**
   * Dismiss all notifications
   */
  dismiss(): void {
    this.snackBar.dismiss();
  }
}
