import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

export type BannerType = 'error' | 'warning' | 'info' | 'success';

/**
 * ErrorBanner Component
 *
 * A reusable banner component for displaying messages with different severity levels.
 * Supports auto-dismiss, manual dismissal, and retry actions.
 *
 * Features:
 * - Multiple severity types (error, warning, info, success)
 * - Auto-dismiss with configurable timeout
 * - Manual dismissal option
 * - Optional retry button
 * - Icon-based visual indicators
 * - Accessible with ARIA attributes
 *
 * @example
 * <app-error-banner
 *   message="Failed to load patient data"
 *   type="error"
 *   [dismissible]="true"
 *   [retryButton]="true"
 *   (retry)="onRetry()"
 *   (dismiss)="onDismiss()">
 * </app-error-banner>
 */
@Component({
  selector: 'app-error-banner',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './error-banner.component.html',
  styleUrls: ['./error-banner.component.scss']
})
export class ErrorBannerComponent implements OnInit, OnDestroy {
  /** Message text to display */
  @Input() message = '';

  /** Banner type/severity level */
  @Input() type: BannerType = 'error';

  /** Whether banner can be manually dismissed */
  @Input() dismissible = true;

  /** Whether to show retry button */
  @Input() retryButton = false;

  /** Auto-dismiss timeout in milliseconds (0 to disable) */
  @Input() autoDismissTimeout = 0;

  /** Emits when retry button is clicked */
  @Output() retry = new EventEmitter<void>();

  /** Emits when banner is dismissed */
  @Output() dismiss = new EventEmitter<void>();

  /** Internal visibility state */
  visible = true;

  private dismissTimeout?: number;

  ngOnInit(): void {
    if (this.autoDismissTimeout > 0) {
      this.dismissTimeout = window.setTimeout(() => {
        this.onDismiss();
      }, this.autoDismissTimeout);
    }
  }

  ngOnDestroy(): void {
    if (this.dismissTimeout) {
      clearTimeout(this.dismissTimeout);
    }
  }

  /**
   * Get icon based on banner type
   */
  getIcon(): string {
    switch (this.type) {
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
        return 'info';
      case 'success':
        return 'check_circle';
      default:
        return 'info';
    }
  }

  /**
   * Get ARIA role based on banner type
   */
  getAriaRole(): string {
    return this.type === 'error' ? 'alert' : 'status';
  }

  /**
   * Handle retry button click
   */
  onRetry(): void {
    this.retry.emit();
  }

  /**
   * Handle dismiss action
   */
  onDismiss(): void {
    this.visible = false;
    this.dismiss.emit();
  }
}
