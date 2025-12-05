import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Help Tooltip Component
 *
 * Displays contextual help text on hover/click with a question mark icon
 *
 * Usage:
 * <app-help-tooltip
 *   text="This field shows the patient's HbA1c value"
 *   [position]="'right'">
 * </app-help-tooltip>
 */
@Component({
  selector: 'app-help-tooltip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="help-tooltip-container">
      <button
        type="button"
        class="help-icon"
        [class.active]="isVisible"
        (click)="toggleTooltip()"
        (mouseenter)="showTooltip()"
        (mouseleave)="hideTooltip()"
        [attr.aria-label]="'Help: ' + text"
        aria-describedby="tooltip-text">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <circle cx="8" cy="8" r="7" stroke="currentColor" fill="none" stroke-width="1.5"/>
          <text x="8" y="12" text-anchor="middle" font-size="12" font-weight="bold">?</text>
        </svg>
      </button>

      <div
        *ngIf="isVisible"
        class="tooltip"
        [class]="'position-' + position"
        id="tooltip-text"
        role="tooltip">
        <div class="tooltip-content">
          {{ text }}
          <a *ngIf="link" [href]="link" target="_blank" class="learn-more">
            Learn more →
          </a>
        </div>
        <div class="tooltip-arrow"></div>
      </div>
    </div>
  `,
  styles: [`
    .help-tooltip-container {
      display: inline-block;
      position: relative;
      margin-left: 4px;
      vertical-align: middle;
    }

    .help-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      height: 16px;
      padding: 0;
      border: none;
      background: none;
      color: #6b7280;
      cursor: help;
      transition: all 0.2s ease;
      border-radius: 50%;
    }

    .help-icon:hover,
    .help-icon.active {
      color: #3b82f6;
      background-color: #eff6ff;
    }

    .help-icon:focus {
      outline: 2px solid #3b82f6;
      outline-offset: 2px;
    }

    .tooltip {
      position: absolute;
      z-index: 9999;
      max-width: 320px;
      padding: 0;
      animation: fadeIn 0.2s ease;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(-4px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .tooltip-content {
      background-color: #1f2937;
      color: white;
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 13px;
      line-height: 1.5;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
    }

    .tooltip-arrow {
      position: absolute;
      width: 0;
      height: 0;
      border-style: solid;
    }

    /* Position: Right (default) */
    .position-right {
      left: calc(100% + 8px);
      top: 50%;
      transform: translateY(-50%);
    }

    .position-right .tooltip-arrow {
      left: -6px;
      top: 50%;
      transform: translateY(-50%);
      border-width: 6px 6px 6px 0;
      border-color: transparent #1f2937 transparent transparent;
    }

    /* Position: Left */
    .position-left {
      right: calc(100% + 8px);
      top: 50%;
      transform: translateY(-50%);
    }

    .position-left .tooltip-arrow {
      right: -6px;
      top: 50%;
      transform: translateY(-50%);
      border-width: 6px 0 6px 6px;
      border-color: transparent transparent transparent #1f2937;
    }

    /* Position: Top */
    .position-top {
      bottom: calc(100% + 8px);
      left: 50%;
      transform: translateX(-50%);
    }

    .position-top .tooltip-arrow {
      bottom: -6px;
      left: 50%;
      transform: translateX(-50%);
      border-width: 6px 6px 0 6px;
      border-color: #1f2937 transparent transparent transparent;
    }

    /* Position: Bottom */
    .position-bottom {
      top: calc(100% + 8px);
      left: 50%;
      transform: translateX(-50%);
    }

    .position-bottom .tooltip-arrow {
      top: -6px;
      left: 50%;
      transform: translateX(-50%);
      border-width: 0 6px 6px 6px;
      border-color: transparent transparent #1f2937 transparent;
    }

    .learn-more {
      display: inline-block;
      margin-top: 8px;
      color: #60a5fa;
      text-decoration: none;
      font-weight: 500;
      font-size: 12px;
    }

    .learn-more:hover {
      color: #93c5fd;
      text-decoration: underline;
    }

    /* Mobile responsive */
    @media (max-width: 640px) {
      .tooltip {
        max-width: 280px;
      }

      .tooltip-content {
        font-size: 12px;
        padding: 10px 12px;
      }
    }
  `]
})
export class HelpTooltipComponent {
  @Input() text = '';
  @Input() link?: string;
  @Input() position: 'top' | 'right' | 'bottom' | 'left' = 'right';
  @Input() trigger: 'hover' | 'click' | 'both' = 'both';

  isVisible = false;
  private hideTimeout?: number;

  showTooltip() {
    if (this.trigger === 'hover' || this.trigger === 'both') {
      if (this.hideTimeout) {
        clearTimeout(this.hideTimeout);
      }
      this.isVisible = true;
    }
  }

  hideTooltip() {
    if (this.trigger === 'hover' || this.trigger === 'both') {
      this.hideTimeout = window.setTimeout(() => {
        this.isVisible = false;
      }, 200);
    }
  }

  toggleTooltip() {
    if (this.trigger === 'click' || this.trigger === 'both') {
      this.isVisible = !this.isVisible;
    }
  }
}
