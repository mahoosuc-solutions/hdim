import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * LoadingOverlay Component
 *
 * A reusable loading overlay that can be used for full-screen or section loading states.
 * Displays a centered spinner with an optional message.
 *
 * Features:
 * - Fullscreen or section overlay modes
 * - Customizable spinner size
 * - Optional loading message
 * - Semi-transparent backdrop
 * - High z-index for proper layering
 *
 * @example
 * <!-- Section overlay -->
 * <div class="my-section" style="position: relative;">
 *   <app-loading-overlay
 *     [isLoading]="loading"
 *     message="Loading patient data...">
 *   </app-loading-overlay>
 *   <!-- section content -->
 * </div>
 *
 * @example
 * <!-- Fullscreen overlay -->
 * <app-loading-overlay
 *   [isLoading]="loading"
 *   [fullscreen]="true"
 *   message="Processing...">
 * </app-loading-overlay>
 */
@Component({
  selector: 'app-loading-overlay',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div
      class="loading-overlay"
      *ngIf="isLoading"
      [class.fullscreen]="fullscreen"
      [attr.role]="'status'"
      [attr.aria-live]="'polite'"
      [attr.aria-busy]="true">
      <div class="loading-content">
        <mat-spinner
          [diameter]="spinnerSize"
          [attr.aria-label]="message || 'Loading'">
        </mat-spinner>
        <p *ngIf="message" class="loading-message">{{ message }}</p>
      </div>
    </div>
  `,
  styles: [`
    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.8);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
      transition: opacity 200ms ease-in-out;

      &.fullscreen {
        position: fixed;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(4px);
        z-index: 1000;
      }
    }

    .loading-content {
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }

    .loading-message {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
      font-size: 14px;
      font-weight: 500;
      max-width: 300px;
    }

    /* Dark theme support */
    @media (prefers-color-scheme: dark) {
      .loading-overlay {
        background: rgba(0, 0, 0, 0.8);

        &.fullscreen {
          background: rgba(0, 0, 0, 0.95);
        }
      }

      .loading-message {
        color: rgba(255, 255, 255, 0.7);
      }
    }
  `]
})
export class LoadingOverlayComponent {
  /** Controls visibility of the overlay */
  @Input() isLoading = false;

  /** Optional loading message displayed below spinner */
  @Input() message?: string;

  /** If true, overlay covers entire viewport. If false, covers parent container. */
  @Input() fullscreen = false;

  /** Spinner diameter in pixels */
  @Input() spinnerSize = 48;
}
