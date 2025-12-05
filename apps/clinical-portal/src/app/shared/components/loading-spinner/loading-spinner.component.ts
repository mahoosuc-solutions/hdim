/**
 * Loading Spinner Component
 *
 * Displays a centered Material spinner with optional loading message.
 * Used throughout the application for async operations.
 *
 * @example
 * <app-loading-spinner
 *   [message]="'Loading patient data...'"
 *   [diameter]="50">
 * </app-loading-spinner>
 */
import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Loading spinner configuration interface
 */
export interface LoadingSpinnerConfig {
  message?: string;
  diameter?: number;
  color?: 'primary' | 'accent' | 'warn';
}

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="loading-spinner-container" [attr.aria-label]="ariaLabel">
      <div class="spinner-wrapper">
        <mat-spinner
          [diameter]="diameter"
          [color]="color"
          aria-label="Loading content">
        </mat-spinner>
        <p *ngIf="message" class="loading-message">
          {{ message }}
        </p>
      </div>
    </div>
  `,
  styles: [`
    .loading-spinner-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 200px;
      width: 100%;
      padding: 24px;
    }

    .spinner-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }

    .loading-message {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
      font-size: 14px;
      text-align: center;
      max-width: 300px;
      line-height: 1.5;
    }

    /* Dark theme support */
    @media (prefers-color-scheme: dark) {
      .loading-message {
        color: rgba(255, 255, 255, 0.7);
      }
    }

    /* Responsive sizing */
    @media (max-width: 600px) {
      .loading-spinner-container {
        min-height: 150px;
        padding: 16px;
      }

      .loading-message {
        font-size: 13px;
      }
    }
  `]
})
export class LoadingSpinnerComponent implements OnInit {
  /**
   * Optional message to display below spinner
   */
  @Input() message?: string;

  /**
   * Diameter of the spinner in pixels
   * @default 40
   */
  @Input() diameter: number = 40;

  /**
   * Color theme for the spinner
   * @default 'primary'
   */
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';

  /**
   * Computed aria-label for accessibility
   */
  get ariaLabel(): string {
    return this.message || 'Loading content, please wait';
  }

  ngOnInit(): void {
    // Validate diameter input
    if (this.diameter < 20 || this.diameter > 200) {
      console.warn('LoadingSpinner: diameter should be between 20 and 200 pixels');
      this.diameter = Math.max(20, Math.min(200, this.diameter));
    }
  }
}
