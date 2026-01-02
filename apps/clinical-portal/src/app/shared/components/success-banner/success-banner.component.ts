/**
 * Success Banner Component
 *
 * Displays a dismissible success message banner with Material Design.
 * Auto-dismisses after 5 seconds by default.
 *
 * @example
 * <app-success-banner
 *   [message]="'Patient record updated successfully'"
 *   [dismissible]="true"
 *   [autoDismiss]="true"
 *   [autoDismissTime]="5000"
 *   (dismissed)="onDismissed()">
 * </app-success-banner>
 */
import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { trigger, state, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-success-banner',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <mat-card
      *ngIf="visible"
      class="success-banner"
      [@slideIn]="animationState"
      role="alert"
      aria-live="polite">
      <div class="banner-content">
        <div class="icon-wrapper">
          <mat-icon class="success-icon" [@checkmark]="iconAnimationState">
            check_circle
          </mat-icon>
        </div>
        <div class="message-wrapper">
          <p class="message">{{ message }}</p>
        </div>
        <button
          *ngIf="dismissible"
          mat-icon-button
          class="dismiss-button"
          (click)="dismiss()"
          aria-label="Dismiss success message">
          <mat-icon>close</mat-icon>
        </button>
      </div>
      <div *ngIf="autoDismiss && showProgress" class="progress-bar">
        <div class="progress-fill" [style.animation-duration.ms]="autoDismissTime"></div>
      </div>
    </mat-card>
  `,
  styles: [`
    .success-banner {
      background-color: #4caf50;
      color: white;
      margin: 16px 0;
      padding: 0;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
      overflow: hidden;
    }

    .banner-content {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
    }

    .icon-wrapper {
      flex-shrink: 0;
    }

    .success-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: white;
    }

    .message-wrapper {
      flex: 1;
    }

    .message {
      margin: 0;
      font-size: 14px;
      line-height: 1.5;
      font-weight: 500;
    }

    .dismiss-button {
      flex-shrink: 0;
      color: white;
    }

    .dismiss-button:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }

    .progress-bar {
      height: 4px;
      background-color: rgba(255, 255, 255, 0.3);
      position: relative;
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background-color: white;
      width: 100%;
      animation: progress-countdown linear;
      transform-origin: left;
    }

    @keyframes progress-countdown {
      from {
        transform: scaleX(1);
      }
      to {
        transform: scaleX(0);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .banner-content {
        padding: 12px;
        gap: 12px;
      }

      .success-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
      }

      .message {
        font-size: 13px;
      }
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .success-banner {
        background-color: #2e7d32;
      }
    }
  `],
  animations: [
    trigger('slideIn', [
      state('void', style({
        transform: 'translateY(-100%)',
        opacity: 0
      })),
      state('visible', style({
        transform: 'translateY(0)',
        opacity: 1
      })),
      transition('void => visible', animate('300ms ease-out')),
      transition('visible => void', animate('200ms ease-in'))
    ]),
    trigger('checkmark', [
      state('void', style({
        transform: 'scale(0) rotate(0deg)',
        opacity: 0
      })),
      state('visible', style({
        transform: 'scale(1) rotate(360deg)',
        opacity: 1
      })),
      transition('void => visible', animate('400ms 100ms cubic-bezier(0.175, 0.885, 0.32, 1.275)'))
    ])
  ]
})
export class SuccessBannerComponent implements OnInit, OnDestroy {
  /** Success message to display */
  @Input() message: string = 'Operation completed successfully';

  /** Allow user to dismiss banner */
  @Input() dismissible: boolean = true;

  /** Auto-dismiss after timeout */
  @Input() autoDismiss: boolean = true;

  /** Auto-dismiss timeout in milliseconds */
  @Input() autoDismissTime: number = 5000;

  /** Show progress bar for auto-dismiss */
  @Input() showProgress: boolean = true;

  /** Event emitted when banner is dismissed */
  @Output() dismissed = new EventEmitter<void>();

  visible: boolean = true;
  animationState: string = 'visible';
  iconAnimationState: string = 'visible';
  private dismissTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (this.autoDismiss) {
      this.startAutoDismissTimer();
    }
  }

  ngOnDestroy(): void {
    this.clearAutoDismissTimer();
  }

  private startAutoDismissTimer(): void {
    this.dismissTimer = setTimeout(() => {
      this.dismiss();
    }, this.autoDismissTime);
  }

  private clearAutoDismissTimer(): void {
    if (this.dismissTimer) {
      clearTimeout(this.dismissTimer);
      this.dismissTimer = undefined;
    }
  }

  dismiss(): void {
    this.clearAutoDismissTimer();
    this.animationState = 'void';
    this.visible = false;
    this.dismissed.emit();
  }

  /**
   * Reset the banner to visible state
   * Useful for reusing the same component instance
   */
  show(newMessage?: string): void {
    if (newMessage) {
      this.message = newMessage;
    }
    this.visible = true;
    this.animationState = 'visible';
    this.iconAnimationState = 'visible';

    if (this.autoDismiss) {
      this.startAutoDismissTimer();
    }
  }
}
