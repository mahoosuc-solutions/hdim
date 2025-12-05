import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export type StatusType = 'success' | 'warning' | 'error' | 'info' | 'neutral';

/**
 * StatusBadge Component
 *
 * A reusable badge component for displaying status information with color coding.
 * Uses Material chips for consistent styling.
 *
 * Features:
 * - Material chip styling
 * - Multiple status types with automatic color coding
 * - Optional icon display
 * - Tooltip support
 * - Accessible with ARIA attributes
 * - Compact and standard sizes
 *
 * @example
 * <app-status-badge
 *   status="Active"
 *   type="success"
 *   icon="check_circle"
 *   tooltip="Patient is currently active in the system">
 * </app-status-badge>
 */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [
    CommonModule,
    MatChipsModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss']
})
export class StatusBadgeComponent {
  /** Status text to display */
  @Input() status = '';

  /** Badge type for color coding */
  @Input() type: StatusType = 'neutral';

  /** Optional Material icon name */
  @Input() icon?: string;

  /** Optional tooltip text */
  @Input() tooltip?: string;

  /** Compact size (smaller badge) */
  @Input() compact = false;

  /** Custom CSS class */
  @Input() customClass?: string;

  get ariaLabel(): string | null {
    if (!this.status) {
      return null;
    }
    return this.tooltip ? `${this.status}: ${this.tooltip}` : this.status;
  }

  /**
   * Get the appropriate icon for the status type if no icon is provided
   */
  getDefaultIcon(): string | undefined {
    if (this.icon) {
      return this.icon;
    }

    // Return undefined for neutral type (no default icon)
    if (this.type === 'neutral') {
      return undefined;
    }

    switch (this.type) {
      case 'success':
        return 'check_circle';
      case 'warning':
        return 'warning';
      case 'error':
        return 'error';
      case 'info':
        return 'info';
      default:
        return undefined;
    }
  }

  /**
   * Check if the badge should display an icon
   */
  shouldShowIcon(): boolean {
    return !!this.getDefaultIcon();
  }
}
