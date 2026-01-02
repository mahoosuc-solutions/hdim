import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';

export type StatCardColor = 'primary' | 'accent' | 'warn' | 'success';
export type TrendDirection = 'up' | 'down' | 'stable' | 'none';

/**
 * Action button configuration for StatCard
 */
export interface StatCardAction {
  label: string;
  icon?: string;
  tooltip?: string;
  ariaLabel?: string;
}

/**
 * StatCard Component
 *
 * A reusable card component for displaying statistics with optional trend indicators.
 * Commonly used on dashboard pages to show key metrics.
 *
 * Features:
 * - Material card with icon on left, value on right
 * - Support for trend indicators (up/down/stable)
 * - Color variants: primary, accent, warn, success
 * - Quick action buttons for direct navigation
 * - Responsive design
 * - Tooltip support
 * - Accessible with ARIA labels
 *
 * @example
 * <app-stat-card
 *   title="Total Patients"
 *   value="1,234"
 *   subtitle="+12% from last month"
 *   icon="people"
 *   color="primary"
 *   trend="up"
 *   [primaryAction]="{label: 'View All', icon: 'arrow_forward'}"
 *   [secondaryAction]="{label: 'Details', icon: 'info'}"
 *   (primaryActionClick)="navigateToPatients()"
 *   (secondaryActionClick)="viewDetails()">
 * </app-stat-card>
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule
  ],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.scss']
})
export class StatCardComponent {
  /** Card title (e.g., "Total Patients") */
  @Input() title = '';

  /** Main value to display (e.g., "1,234") */
  @Input() value = '';

  /** Optional subtitle (e.g., "+12% from last month") */
  @Input() subtitle?: string;

  /** Material icon name */
  @Input() icon?: string;

  /** Icon CSS class for custom styling */
  @Input() iconClass?: string;

  /** Icon color (hex color string) */
  @Input() iconColor?: string;

  /** Color variant for the card */
  @Input() color: StatCardColor = 'primary';

  /** Trend direction indicator */
  @Input() trend: TrendDirection = 'none';

  /** Optional tooltip text */
  @Input() tooltip?: string;

  /** Whether the card is clickable */
  @Input() clickable = false;

  /** Primary action button configuration */
  @Input() primaryAction?: StatCardAction;

  /** Secondary action button configuration */
  @Input() secondaryAction?: StatCardAction;

  /** Target value for comparison (e.g., 85 for 85% target) */
  @Input() targetValue?: number;

  /** Whether higher values are better (true) or lower values are better (false) */
  @Input() higherIsBetter = true;

  /** Emitted when primary action button is clicked */
  @Output() primaryActionClick = new EventEmitter<void>();

  /** Emitted when secondary action button is clicked */
  @Output() secondaryActionClick = new EventEmitter<void>();

  /**
   * Get numeric value from string value for comparison
   */
  private getNumericValue(): number | null {
    const match = this.value.match(/[\d.]+/);
    return match ? parseFloat(match[0]) : null;
  }

  /**
   * Check if current value meets target
   */
  meetsTarget(): boolean | null {
    if (this.targetValue === undefined) return null;
    const numValue = this.getNumericValue();
    if (numValue === null) return null;

    return this.higherIsBetter
      ? numValue >= this.targetValue
      : numValue <= this.targetValue;
  }

  /**
   * Get the difference from target as a string
   */
  getTargetDifference(): string {
    if (this.targetValue === undefined) return '';
    const numValue = this.getNumericValue();
    if (numValue === null) return '';

    const diff = numValue - this.targetValue;
    const absDiff = Math.abs(diff);
    const sign = diff >= 0 ? '+' : '-';

    // If value is percentage, show difference as percentage
    if (this.value.includes('%')) {
      return `${sign}${absDiff.toFixed(1)}%`;
    }
    return `${sign}${absDiff}`;
  }

  /**
   * Get the target status class for styling
   */
  getTargetStatusClass(): string {
    const meets = this.meetsTarget();
    if (meets === null) return '';
    return meets ? 'target-met' : 'target-not-met';
  }

  /**
   * Get the trend icon based on direction
   */
  getTrendIcon(): string {
    switch (this.trend) {
      case 'up':
        return 'trending_up';
      case 'down':
        return 'trending_down';
      case 'stable':
        return 'trending_flat';
      default:
        return '';
    }
  }

  /**
   * Get the trend color class
   */
  getTrendClass(): string {
    switch (this.trend) {
      case 'up':
        return 'trend-up';
      case 'down':
        return 'trend-down';
      case 'stable':
        return 'trend-stable';
      default:
        return '';
    }
  }
}
