import { Component, Input, Output, EventEmitter, OnChanges, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

export type ButtonVariant = 'raised' | 'stroked' | 'flat' | 'icon';
export type ButtonColor = 'primary' | 'accent' | 'warn' | undefined;
// Force rebuild

/**
 * LoadingButton Component
 *
 * A reusable button component that handles loading, success, and disabled states
 * with built-in accessibility features (aria-busy, aria-label).
 *
 * Features:
 * - Automatic spinner display during loading
 * - Text changes during loading/success states
 * - Success state with auto-clear
 * - Full Material Design button variant support
 * - Built-in ARIA attributes for accessibility
 *
 * @example
 * <app-loading-button
 *   text="Save"
 *   loadingText="Saving..."
 *   successText="Saved!"
 *   icon="save"
 *   color="primary"
 *   [loading]="isSaving"
 *   [success]="saveSuccess"
 *   ariaLabel="Save changes"
 *   (buttonClick)="onSave()">
 * </app-loading-button>
 */
@Component({
  selector: 'app-loading-button',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './loading-button.component.html',
  styles: [`
    .inline-spinner {
      display: inline-block;
      margin-right: 8px;
      vertical-align: middle;
    }

    .button-text {
      vertical-align: middle;
    }

    .success-icon {
      color: #4caf50;
      margin-right: 8px;
      vertical-align: middle;
    }

    /* Ensure spinner color matches button color */
    :host ::ng-deep .mat-mdc-progress-spinner {
      --mdc-circular-progress-active-indicator-color: currentColor;
    }

    /* Icon button specific styles */
    button[mat-icon-button] {
      .inline-spinner,
      mat-icon {
        margin: 0;
      }
    }
  `]
})
export class LoadingButtonComponent implements OnChanges, OnDestroy {
  /** Button text (for non-icon buttons) */
  @Input() text = '';

  /** Text to display during loading state */
  @Input() loadingText?: string;

  /** Text to display during success state */
  @Input() successText?: string;

  /** Material icon name */
  @Input() icon?: string;

  /** Loading state - shows spinner and loading text */
  @Input() loading = false;

  /** Success state - shows check icon and success text */
  @Input() success = false;

  /** Disabled state */
  @Input() disabled = false;

  /** Material button color */
  @Input() color: ButtonColor;

  /** Material button variant */
  @Input() variant: ButtonVariant | 'text' = 'raised';

  /** Button type attribute */
  @Input() type: 'button' | 'submit' = 'button';

  /** Aria-label for accessibility (required for icon buttons) */
  @Input() ariaLabel?: string;

  /** Tooltip text */
  @Input() tooltip?: string;

  /** Additional CSS classes */
  @Input() customClass?: string;

  /** Duration to show success state before auto-clearing (ms). Set to 0 to disable auto-clear. */
  @Input() successDuration = 2000;

  /** Emits when button is clicked (only when not loading/success) */
  @Output() buttonClick = new EventEmitter<Event>();

  private successTimeout?: number;

  /**
   * Handle button click - only emit if not in loading or success state
   */
  handleClick(event: Event): void {
    if (!this.loading && !this.success && !this.disabled) {
      this.buttonClick.emit(event);
    }
  }

  /**
   * Watch for success state changes and auto-clear after duration
   */
  ngOnChanges(): void {
    if (this.success && this.successDuration > 0) {
      // Clear any existing timeout
      if (this.successTimeout) {
        clearTimeout(this.successTimeout);
      }

      // Set new timeout to clear success state
      this.successTimeout = window.setTimeout(() => {
        this.success = false;
      }, this.successDuration);
    }
  }

  /**
   * Clean up timeout on component destroy
   */
  ngOnDestroy(): void {
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
    }
  }
}
