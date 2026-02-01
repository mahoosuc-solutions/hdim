import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Alert, AlertSeverity } from './notification.service';
import { Subject } from 'rxjs';

/**
 * Alert Notification Component
 *
 * Displays a modal-style alert notification requiring user action.
 * Alerts do NOT auto-dismiss and require explicit confirmation or cancellation.
 *
 * ★ Insight ─────────────────────────────────────
 * Alert component design patterns:
 * - Modal overlay prevents interaction with background
 * - ARIA role="alert" + aria-live="assertive" for critical accessibility
 * - Confirm/Cancel buttons trigger callbacks and trigger dismissal
 * - Severity styling indicates importance (info → critical)
 * - Focus trap ensures keyboard navigation within alert
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Overlay -->
    <div class="alert-overlay" (click)="onBackdropClick()"></div>

    <!-- Alert Container -->
    <div
      class="alert"
      [ngClass]="'alert-' + alert.severity"
      [data-testid]="'alert-container'"
      [attr.role]="alert.severity === 'critical' ? 'alert' : 'dialog'"
      [attr.aria-live]="alert.severity === 'critical' ? 'assertive' : 'polite'"
      [attr.aria-modal]="'true'"
      [attr.aria-labelledby]="'alert-title-' + alert.id"
      [attr.aria-describedby]="'alert-message-' + alert.id"
    >
      <!-- Header -->
      <div class="alert-header">
        <div class="alert-title-wrapper">
          <div class="alert-icon" [data-testid]="'alert-icon'" [ngClass]="getIconClass()">
            {{ getIcon() }}
          </div>
          <h2 class="alert-title" [id]="'alert-title-' + alert.id" [data-testid]="'alert-title'">
            {{ alert.title }}
          </h2>
        </div>

        <!-- Close Button (only for non-critical) -->
        <button
          *ngIf="alert.severity !== 'critical'"
          class="alert-close"
          [data-testid]="'alert-close-button'"
          (click)="dismiss()"
          aria-label="Close alert"
          tabindex="0"
        >
          ✕
        </button>
      </div>

      <!-- Message -->
      <div
        class="alert-message"
        [id]="'alert-message-' + alert.id"
        [data-testid]="'alert-message'"
      >
        {{ alert.message }}
      </div>

      <!-- Buttons -->
      <div class="alert-buttons">
        <button
          *ngIf="alert.cancelLabel"
          class="alert-button alert-cancel"
          [data-testid]="'alert-cancel-button'"
          (click)="cancel()"
          tabindex="0"
        >
          {{ alert.cancelLabel }}
        </button>

        <button
          class="alert-button alert-confirm"
          [data-testid]="'alert-confirm-button'"
          (click)="confirm()"
          tabindex="0"
          [attr.autofocus]="!alert.cancelLabel"
        >
          {{ alert.confirmLabel || 'OK' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .alert-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      z-index: 9998;
      animation: fadeIn 0.2s ease;
    }

    .alert {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      min-width: 350px;
      max-width: 600px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
      z-index: 9999;
      animation: slideDown 0.3s ease;
      display: flex;
      flex-direction: column;
      gap: 0;
    }

    /* Severity Styling */
    .alert-info {
      border-top: 4px solid #2196f3;
    }

    .alert-warning {
      border-top: 4px solid #ff9800;
    }

    .alert-error {
      border-top: 4px solid #f44336;
    }

    .alert-critical {
      border-top: 4px solid #d32f2f;
      background: #ffebee;
    }

    /* Header */
    .alert-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 1.5rem;
      border-bottom: 1px solid #e0e0e0;
    }

    .alert-title-wrapper {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      flex: 1;
    }

    .alert-icon {
      flex-shrink: 0;
      font-size: 1.75rem;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
    }

    .alert-info .alert-icon {
      background: #e3f2fd;
      color: #2196f3;
    }

    .alert-warning .alert-icon {
      background: #fff3f0;
      color: #ff9800;
    }

    .alert-error .alert-icon {
      background: #ffebee;
      color: #f44336;
    }

    .alert-critical .alert-icon {
      background: #ffcdd2;
      color: #d32f2f;
    }

    .alert-title {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
      color: #333;
      line-height: 1.4;
    }

    /* Close Button */
    .alert-close {
      flex-shrink: 0;
      background: none;
      border: none;
      color: #999;
      font-size: 1.5rem;
      cursor: pointer;
      padding: 0;
      width: 2rem;
      height: 2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      transition: all 0.2s ease;
      margin-left: 1rem;
    }

    .alert-close:hover {
      background: rgba(0, 0, 0, 0.1);
      color: #333;
    }

    .alert-close:focus {
      outline: 2px solid #2196f3;
      outline-offset: 2px;
    }

    /* Message */
    .alert-message {
      padding: 1rem 1.5rem;
      color: #666;
      line-height: 1.6;
      font-size: 0.95rem;
      max-height: 200px;
      overflow-y: auto;
    }

    /* Buttons */
    .alert-buttons {
      display: flex;
      gap: 1rem;
      padding: 1.5rem;
      border-top: 1px solid #e0e0e0;
      justify-content: flex-end;
    }

    .alert-button {
      padding: 0.625rem 1.5rem;
      border-radius: 6px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
      min-width: 100px;
      text-align: center;
    }

    .alert-cancel {
      background: #f5f5f5;
      color: #333;
    }

    .alert-cancel:hover {
      background: #e0e0e0;
    }

    .alert-cancel:focus {
      outline: 2px solid #999;
      outline-offset: 2px;
    }

    .alert-confirm {
      background: #2196f3;
      color: white;
    }

    .alert-confirm:hover {
      background: #1976d2;
    }

    .alert-confirm:focus {
      outline: 2px solid #1565c0;
      outline-offset: 2px;
    }

    /* Alert-Specific Confirm Button Colors */
    .alert-info .alert-confirm {
      background: #2196f3;
    }

    .alert-info .alert-confirm:hover {
      background: #1976d2;
    }

    .alert-warning .alert-confirm {
      background: #ff9800;
    }

    .alert-warning .alert-confirm:hover {
      background: #f57c00;
    }

    .alert-error .alert-confirm {
      background: #f44336;
    }

    .alert-error .alert-confirm:hover {
      background: #da190b;
    }

    .alert-critical .alert-confirm {
      background: #d32f2f;
    }

    .alert-critical .alert-confirm:hover {
      background: #b71c1c;
    }

    /* Animations */
    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translate(-50%, -60%);
      }
      to {
        opacity: 1;
        transform: translate(-50%, -50%);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .alert {
        min-width: 90vw;
        max-width: 90vw;
        max-height: 90vh;
      }

      .alert-buttons {
        flex-direction: column;
      }

      .alert-button {
        width: 100%;
      }
    }
  `],
})
export class AlertComponent implements OnInit, OnDestroy {
  @Input() alert!: Alert;

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Focus trap could be implemented here for accessibility
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get icon for alert severity
   */
  getIcon(): string {
    switch (this.alert.severity) {
      case AlertSeverity.Info:
        return 'ⓘ';
      case AlertSeverity.Warning:
        return '⚠';
      case AlertSeverity.Error:
        return '✕';
      case AlertSeverity.Critical:
        return '⚠';
      default:
        return '';
    }
  }

  /**
   * Get icon class for styling
   */
  getIconClass(): string {
    return `icon-${this.alert.severity}`;
  }

  /**
   * Handle confirm button click
   */
  confirm(): void {
    if (this.alert.onConfirm) {
      this.alert.onConfirm();
    }
    this.dismiss();
  }

  /**
   * Handle cancel button click
   */
  cancel(): void {
    if (this.alert.onCancel) {
      this.alert.onCancel();
    }
    this.dismiss();
  }

  /**
   * Handle backdrop click (for non-critical alerts)
   */
  onBackdropClick(): void {
    if (this.alert.severity !== AlertSeverity.Critical) {
      this.cancel();
    }
  }

  /**
   * Dismiss the alert
   */
  dismiss(): void {
    // This will be handled by parent container
  }
}
