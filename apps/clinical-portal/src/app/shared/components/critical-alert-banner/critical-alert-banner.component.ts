import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

export type AlertSeverity = 'critical' | 'high' | 'medium';
export type AlertType = 'suicide-risk' | 'care-gap' | 'vital-sign' | 'lab-result' | 'substance-use' | 'medication';

/**
 * Critical Alert Item
 */
export interface CriticalAlert {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  description: string;
  actionLabel?: string;
  actionIcon?: string;
  metadata?: Record<string, any>;
  timestamp?: Date;
}

/**
 * Critical Alert Banner Component
 *
 * Displays urgent patient safety alerts at the top of the patient detail page.
 * Designed for maximum visibility of life-threatening or time-sensitive clinical issues.
 *
 * Features:
 * - Color-coded by severity (critical = red, high = orange, medium = yellow)
 * - Dismissible with confirmation
 * - Action buttons for quick intervention
 * - Accessible with ARIA live regions
 * - Mobile-responsive
 *
 * Triggers:
 * - Suicide risk: high or critical
 * - Substance use: high risk + not in treatment
 * - Care gap: urgent priority + overdue
 * - Vital sign: critical status
 * - Lab result: critical status
 *
 * @example
 * <app-critical-alert-banner
 *   [alerts]="criticalAlerts"
 *   [dismissible]="true"
 *   (alertAction)="handleAlertAction($event)"
 *   (alertDismiss)="handleAlertDismiss($event)">
 * </app-critical-alert-banner>
 */
@Component({
  selector: 'app-critical-alert-banner',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './critical-alert-banner.component.html',
  styleUrls: ['./critical-alert-banner.component.scss']
})
export class CriticalAlertBannerComponent {
  /** Array of critical alerts to display */
  @Input() alerts: CriticalAlert[] = [];

  /** Whether alerts can be dismissed */
  @Input() dismissible = true;

  /** Whether to show timestamp */
  @Input() showTimestamp = false;

  /** Emitted when action button is clicked */
  @Output() alertAction = new EventEmitter<CriticalAlert>();

  /** Emitted when alert is dismissed */
  @Output() alertDismiss = new EventEmitter<CriticalAlert>();

  /**
   * Get severity-specific icon
   */
  getSeverityIcon(severity: AlertSeverity): string {
    switch (severity) {
      case 'critical':
        return 'error';
      case 'high':
        return 'warning';
      case 'medium':
        return 'info';
      default:
        return 'info';
    }
  }

  /**
   * Get alert type-specific icon
   */
  getTypeIcon(type: AlertType): string {
    switch (type) {
      case 'suicide-risk':
        return 'psychology';
      case 'care-gap':
        return 'event_busy';
      case 'vital-sign':
        return 'favorite';
      case 'lab-result':
        return 'biotech';
      case 'substance-use':
        return 'medication';
      case 'medication':
        return 'medication';
      default:
        return 'warning';
    }
  }

  /**
   * Get severity class for styling
   */
  getSeverityClass(severity: AlertSeverity): string {
    return `severity-${severity}`;
  }

  /**
   * Handle action button click
   */
  onAction(alert: CriticalAlert): void {
    this.alertAction.emit(alert);
  }

  /**
   * Handle dismiss button click
   */
  onDismiss(alert: CriticalAlert): void {
    this.alertDismiss.emit(alert);
  }

  /**
   * Format timestamp
   */
  formatTimestamp(date?: Date): string {
    if (!date) return '';

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  }

  /**
   * Get ARIA live region politeness based on severity
   */
  getAriaLive(severity: AlertSeverity): 'assertive' | 'polite' {
    return severity === 'critical' ? 'assertive' : 'polite';
  }
}
