import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Toast, NotificationType } from './notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Toast Notification Component
 *
 * Displays a toast notification with auto-dismiss, action button, and progress indicator.
 * Toasts stack vertically and can be dismissed manually.
 *
 * ★ Insight ─────────────────────────────────────
 * Toast component lifecycle:
 * - Created when NotificationService emits toast$
 * - Progress bar animates from 100% to 0% during duration
 * - Auto-dismisses when animation completes
 * - Can be dismissed early by user or by action button click
 * - Cleanup pattern prevents memory leaks from animation listeners
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="toast"
      [ngClass]="['toast-' + toast.type]"
      [data-testid]="'toast-container'"
      role="status"
      aria-live="polite"
      [attr.aria-label]="getAriaLabel()"
      @slideInRight
      @slideOutRight
    >
      <!-- Icon -->
      <div class="toast-icon" [data-testid]="'toast-icon'" [ngClass]="getIconClass()">
        {{ getIcon() }}
      </div>

      <!-- Content -->
      <div class="toast-content">
        <div class="toast-message" [data-testid]="'toast-message'">
          {{ toast.message }}
        </div>
        <div
          *ngIf="toast.actionLabel"
          class="toast-action"
          [data-testid]="'toast-action-button'"
          (click)="onAction()"
        >
          {{ toast.actionLabel }}
        </div>
      </div>

      <!-- Close Button -->
      <button
        class="toast-close"
        [data-testid]="'toast-close-button'"
        (click)="dismiss()"
        aria-label="Close notification"
        tabindex="0"
      >
        ✕
      </button>

      <!-- Progress Bar -->
      <div
        *ngIf="toast.duration && toast.duration > 0"
        class="toast-progress"
        [data-testid]="'toast-progress-bar'"
        [style.animation]="'progress ' + (toast.duration / 1000) + 's linear forwards'"
        (mouseenter)="pauseProgress()"
        (mouseleave)="resumeProgress()"
      ></div>
    </div>
  `,
  styles: [`
    .toast {
      position: fixed;
      bottom: 1rem;
      right: 1rem;
      min-width: 300px;
      max-width: 500px;
      padding: 1rem;
      border-radius: 8px;
      background: white;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      z-index: 9999;
      transition: all 0.3s ease;
    }

    /* Toast Types */
    .toast-success {
      border-left: 4px solid #4caf50;
      background: #f1f8f4;
    }

    .toast-error {
      border-left: 4px solid #f44336;
      background: #ffebee;
    }

    .toast-warning {
      border-left: 4px solid #ff9800;
      background: #fff3f0;
    }

    .toast-info {
      border-left: 4px solid #2196f3;
      background: #e3f2fd;
    }

    /* Icon */
    .toast-icon {
      flex-shrink: 0;
      font-size: 1.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 1.5rem;
      height: 1.5rem;
    }

    .toast-success .toast-icon {
      color: #4caf50;
    }

    .toast-error .toast-icon {
      color: #f44336;
    }

    .toast-warning .toast-icon {
      color: #ff9800;
    }

    .toast-info .toast-icon {
      color: #2196f3;
    }

    /* Content */
    .toast-content {
      flex: 1;
      min-width: 0;
    }

    .toast-message {
      font-size: 0.95rem;
      color: #333;
      line-height: 1.4;
      word-wrap: break-word;
      font-weight: 500;
    }

    .toast-action {
      font-size: 0.85rem;
      color: #2196f3;
      margin-top: 0.5rem;
      cursor: pointer;
      font-weight: 600;
      padding: 0.25rem 0;
      border-radius: 4px;
      transition: background 0.2s ease;
    }

    .toast-action:hover {
      background: rgba(33, 150, 243, 0.1);
    }

    .toast-action:active {
      background: rgba(33, 150, 243, 0.2);
    }

    /* Close Button */
    .toast-close {
      flex-shrink: 0;
      background: none;
      border: none;
      color: #999;
      font-size: 1.2rem;
      cursor: pointer;
      padding: 0;
      width: 1.5rem;
      height: 1.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      transition: background 0.2s ease, color 0.2s ease;
    }

    .toast-close:hover {
      background: rgba(0, 0, 0, 0.1);
      color: #333;
    }

    .toast-close:focus {
      outline: 2px solid #2196f3;
      outline-offset: 2px;
    }

    /* Progress Bar */
    .toast-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: currentColor;
      border-radius: 0 0 8px 8px;
    }

    .toast-success .toast-progress {
      background: #4caf50;
    }

    .toast-error .toast-progress {
      background: #f44336;
    }

    .toast-warning .toast-progress {
      background: #ff9800;
    }

    .toast-info .toast-progress {
      background: #2196f3;
    }

    /* Progress Animation */
    @keyframes progress {
      from {
        width: 100%;
      }
      to {
        width: 0%;
      }
    }

    /* Hover State */
    .toast:hover {
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
    }

    /* Responsive */
    @media (max-width: 600px) {
      .toast {
        min-width: unset;
        max-width: 90vw;
        bottom: 0.5rem;
        right: 0.5rem;
        left: 0.5rem;
      }
    }
  `],
})
export class ToastComponent implements OnInit, OnDestroy {
  @Input() toast!: Toast;

  private destroy$ = new Subject<void>();
  private progressPaused = false;
  private progressTimeRemaining = 0;

  ngOnInit(): void {
    // Initialize progress tracking if needed
    if (this.toast.duration && this.toast.duration > 0) {
      this.progressTimeRemaining = this.toast.duration;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get icon for toast type
   */
  getIcon(): string {
    switch (this.toast.type) {
      case NotificationType.Success:
        return '✓';
      case NotificationType.Error:
        return '✕';
      case NotificationType.Warning:
        return '⚠';
      case NotificationType.Info:
        return 'ⓘ';
      default:
        return '';
    }
  }

  /**
   * Get icon class for styling
   */
  getIconClass(): string {
    return `icon-${this.toast.type}`;
  }

  /**
   * Get aria label for accessibility
   */
  getAriaLabel(): string {
    const typeLabel = this.toast.type.charAt(0).toUpperCase() + this.toast.type.slice(1);
    return `${typeLabel}: ${this.toast.message}`;
  }

  /**
   * Handle action button click
   */
  onAction(): void {
    if (this.toast.onAction) {
      this.toast.onAction();
    }
    this.dismiss();
  }

  /**
   * Dismiss the toast
   */
  dismiss(): void {
    // This will be handled by parent container
    // which will remove this component from view
  }

  /**
   * Pause progress animation on hover
   */
  pauseProgress(): void {
    this.progressPaused = true;
  }

  /**
   * Resume progress animation on mouse leave
   */
  resumeProgress(): void {
    this.progressPaused = false;
  }
}
