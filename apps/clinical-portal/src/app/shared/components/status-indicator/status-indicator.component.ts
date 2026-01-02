import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';

/**
 * Status type for the indicator
 * Note: Renamed from StatusType to avoid conflict with status-badge.component
 */
export type IndicatorStatusType = 'active' | 'idle' | 'processing' | 'error' | 'success' | 'warning' | 'connected' | 'disconnected' | 'simulating';

/**
 * StatusIndicator Component
 *
 * A small animated dot that indicates status with color and optional pulse animation.
 *
 * @example
 * <app-status-indicator
 *   status="active"
 *   [showLabel]="true"
 *   size="medium">
 * </app-status-indicator>
 */
@Component({
  selector: 'app-status-indicator',
  standalone: true,
  imports: [CommonModule, MatTooltipModule],
  template: `
    <span class="status-indicator"
          [class]="getStatusClass()"
          [class.small]="size === 'small'"
          [class.medium]="size === 'medium'"
          [class.large]="size === 'large'"
          [matTooltip]="tooltip || getDefaultTooltip()">
      <span class="dot" [class.pulse]="shouldPulse()"></span>
      <span class="label" *ngIf="showLabel">{{ label || getDefaultLabel() }}</span>
    </span>
  `,
  styles: [`
    .status-indicator {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    .dot {
      border-radius: 50%;
      display: inline-block;
      position: relative;
    }

    /* Sizes */
    .small .dot {
      width: 8px;
      height: 8px;
    }

    .medium .dot {
      width: 10px;
      height: 10px;
    }

    .large .dot {
      width: 12px;
      height: 12px;
    }

    /* Status colors */
    .status-active .dot {
      background-color: #4caf50;
      box-shadow: 0 0 4px rgba(76, 175, 80, 0.4);
    }

    .status-idle .dot {
      background-color: #9e9e9e;
    }

    .status-processing .dot {
      background-color: #2196f3;
      box-shadow: 0 0 4px rgba(33, 150, 243, 0.4);
    }

    .status-error .dot {
      background-color: #f44336;
      box-shadow: 0 0 4px rgba(244, 67, 54, 0.4);
    }

    .status-success .dot {
      background-color: #4caf50;
      box-shadow: 0 0 4px rgba(76, 175, 80, 0.4);
    }

    .status-warning .dot {
      background-color: #ff9800;
      box-shadow: 0 0 4px rgba(255, 152, 0, 0.4);
    }

    .status-connected .dot {
      background-color: #4caf50;
      box-shadow: 0 0 4px rgba(76, 175, 80, 0.4);
    }

    .status-disconnected .dot {
      background-color: #9e9e9e;
    }

    .status-simulating .dot {
      background-color: #ff9800;
      box-shadow: 0 0 4px rgba(255, 152, 0, 0.4);
    }

    /* Pulse animation */
    .dot.pulse {
      animation: pulse 1.5s ease-in-out infinite;
    }

    @keyframes pulse {
      0% {
        transform: scale(1);
        opacity: 1;
      }
      50% {
        transform: scale(1.2);
        opacity: 0.8;
      }
      100% {
        transform: scale(1);
        opacity: 1;
      }
    }

    /* Processing spinner effect */
    .status-processing .dot.pulse::after {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      border-radius: 50%;
      border: 2px solid transparent;
      border-top-color: #2196f3;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    .label {
      font-size: 0.85em;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 500;
    }

    .status-active .label { color: #4caf50; }
    .status-idle .label { color: #9e9e9e; }
    .status-processing .label { color: #2196f3; }
    .status-error .label { color: #f44336; }
    .status-success .label { color: #4caf50; }
    .status-warning .label { color: #ff9800; }
    .status-connected .label { color: #4caf50; }
    .status-disconnected .label { color: #9e9e9e; }
    .status-simulating .label { color: #ff9800; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusIndicatorComponent {
  /** Current status */
  @Input() status: IndicatorStatusType = 'idle';

  /** Size variant */
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  /** Whether to show the text label */
  @Input() showLabel: boolean = false;

  /** Custom label text */
  @Input() label?: string;

  /** Custom tooltip text */
  @Input() tooltip?: string;

  /**
   * Get CSS class for current status
   */
  getStatusClass(): string {
    return `status-${this.status}`;
  }

  /**
   * Determine if dot should pulse
   */
  shouldPulse(): boolean {
    return ['active', 'processing', 'connected', 'simulating'].includes(this.status);
  }

  /**
   * Get default label text for status
   */
  getDefaultLabel(): string {
    const labels: Record<IndicatorStatusType, string> = {
      active: 'Active',
      idle: 'Idle',
      processing: 'Processing',
      error: 'Error',
      success: 'Success',
      warning: 'Warning',
      connected: 'Connected',
      disconnected: 'Disconnected',
      simulating: 'Demo Mode',
    };
    return labels[this.status] || this.status;
  }

  /**
   * Get default tooltip for status
   */
  getDefaultTooltip(): string {
    const tooltips: Record<IndicatorStatusType, string> = {
      active: 'Service is active and healthy',
      idle: 'Service is idle',
      processing: 'Currently processing data',
      error: 'An error has occurred',
      success: 'Operation completed successfully',
      warning: 'Attention may be required',
      connected: 'Connected to real-time updates',
      disconnected: 'Not connected to real-time updates',
      simulating: 'Running in demo simulation mode',
    };
    return tooltips[this.status] || '';
  }
}
