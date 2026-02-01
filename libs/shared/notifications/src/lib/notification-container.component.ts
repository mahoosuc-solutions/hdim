import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, Toast, Alert, AlertSeverity } from './notification.service';
import { ToastComponent } from './toast.component';
import { AlertComponent } from './alert.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Notification Container Component
 *
 * Root component that manages all toast and alert notifications.
 * Subscribes to NotificationService observables and creates/destroys
 * notification components dynamically.
 *
 * ★ Insight ─────────────────────────────────────
 * Container pattern benefits:
 * - Single root component handles all notifications globally
 * - Manages lifecycle of toast/alert components (create, destroy)
 * - Handles stacking behavior for multiple notifications
 * - Prevents duplicate notifications and memory leaks
 * - Positioned fixed to be visible above all content
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-notification-container',
  standalone: true,
  imports: [CommonModule, ToastComponent, AlertComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Toast Container -->
    <div class="toast-stack" [data-testid]="'toast-stack'">
      <app-toast
        *ngFor="let toast of activeToasts"
        [toast]="toast"
        [data-testid]="'toast-' + toast.id"
      ></app-toast>
    </div>

    <!-- Alert Container (Only one alert at a time) -->
    <div *ngIf="activeAlert" [data-testid]="'alert-stack'">
      <app-alert
        [alert]="activeAlert"
        [data-testid]="'alert-' + activeAlert.id"
      ></app-alert>
    </div>
  `,
  styles: [`
    :host {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      pointer-events: none;
      z-index: 9000;
    }

    .toast-stack {
      position: fixed;
      bottom: 1rem;
      right: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      pointer-events: auto;
      max-height: 90vh;
      overflow: hidden;
    }

    @media (max-width: 600px) {
      .toast-stack {
        bottom: 0.5rem;
        right: 0.5rem;
        left: 0.5rem;
      }
    }
  `],
})
export class NotificationContainerComponent implements OnInit, OnDestroy {
  activeToasts: Toast[] = [];
  activeAlert: Alert | null = null;

  private destroy$ = new Subject<void>();
  private toastQueue: Toast[] = [];
  private alertQueue: Alert[] = [];

  constructor(
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to toasts
    this.notificationService.toast$.pipe(takeUntil(this.destroy$)).subscribe((toast) => {
      this.addToast(toast);
    });

    // Subscribe to alerts
    this.notificationService.alert$.pipe(takeUntil(this.destroy$)).subscribe((alert) => {
      this.queueAlert(alert);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Add a toast to the active list
   */
  private addToast(toast: Toast): void {
    this.activeToasts.push(toast);
    this.cdr.markForCheck();

    // If duration is 0, never auto-dismiss
    // Component will handle auto-dismiss via duration animation
  }

  /**
   * Remove a toast from the active list
   */
  removeToast(id: string): void {
    this.activeToasts = this.activeToasts.filter((t) => t.id !== id);
    this.notificationService.dismissToast(id);
    this.cdr.markForCheck();
  }

  /**
   * Queue an alert (only one can be active at a time)
   */
  private queueAlert(alert: Alert): void {
    if (!this.activeAlert) {
      this.activeAlert = alert;
      this.cdr.markForCheck();
    } else {
      this.alertQueue.push(alert);
    }
  }

  /**
   * Remove the active alert and show next from queue
   */
  removeAlert(id: string): void {
    if (this.activeAlert?.id === id) {
      this.notificationService.confirmAlert(id);

      if (this.alertQueue.length > 0) {
        this.activeAlert = this.alertQueue.shift() || null;
      } else {
        this.activeAlert = null;
      }
      this.cdr.markForCheck();
    }
  }

  /**
   * Cancel the active alert and show next from queue
   */
  cancelAlert(id: string): void {
    if (this.activeAlert?.id === id) {
      this.notificationService.cancelAlert(id);

      if (this.alertQueue.length > 0) {
        this.activeAlert = this.alertQueue.shift() || null;
      } else {
        this.activeAlert = null;
      }
      this.cdr.markForCheck();
    }
  }
}
