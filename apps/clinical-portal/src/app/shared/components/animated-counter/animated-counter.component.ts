import { Component, Input, OnChanges, SimpleChanges, OnDestroy, ElementRef, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * AnimatedCounter Component
 *
 * Displays a number with smooth counting animation when the value changes.
 * Uses requestAnimationFrame for 60fps performance.
 *
 * @example
 * <app-animated-counter
 *   [value]="1234"
 *   [duration]="500"
 *   [prefix]="'$'"
 *   [suffix]="'/sec'"
 *   [decimals]="1">
 * </app-animated-counter>
 */
@Component({
  selector: 'app-animated-counter',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="animated-counter" [class.counting]="isAnimating">
      <span class="prefix" *ngIf="prefix">{{ prefix }}</span>
      <span class="value">{{ displayValue }}</span>
      <span class="suffix" *ngIf="suffix">{{ suffix }}</span>
      <span class="change-indicator" *ngIf="showChange && changeValue !== 0"
            [class.positive]="changeValue > 0"
            [class.negative]="changeValue < 0">
        {{ changeValue > 0 ? '+' : '' }}{{ formatNumber(changeValue) }}
      </span>
    </span>
  `,
  styles: [`
    .animated-counter {
      display: inline-flex;
      align-items: baseline;
      gap: 2px;
      font-variant-numeric: tabular-nums;
    }

    .value {
      transition: color 0.2s ease;
    }

    .counting .value {
      color: var(--primary-color, #1976d2);
    }

    .prefix, .suffix {
      opacity: 0.8;
      font-size: 0.85em;
    }

    .change-indicator {
      font-size: 0.75em;
      margin-left: 4px;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 500;
    }

    .change-indicator.positive {
      color: #2e7d32;
      background-color: rgba(46, 125, 50, 0.1);
    }

    .change-indicator.negative {
      color: #c62828;
      background-color: rgba(198, 40, 40, 0.1);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnimatedCounterComponent implements OnChanges, OnDestroy {
  /** The target value to display */
  @Input() value: number = 0;

  /** Animation duration in milliseconds */
  @Input() duration: number = 500;

  /** Prefix to display before the number */
  @Input() prefix: string = '';

  /** Suffix to display after the number */
  @Input() suffix: string = '';

  /** Number of decimal places */
  @Input() decimals: number = 0;

  /** Whether to use thousands separators */
  @Input() useGrouping: boolean = true;

  /** Show change indicator (+X or -X) */
  @Input() showChange: boolean = false;

  /** The change amount to display */
  @Input() changeValue: number = 0;

  /** Current display value (animated) */
  displayValue: string = '0';

  /** Whether animation is in progress */
  isAnimating: boolean = false;

  private animationFrameId: number | null = null;
  private currentValue: number = 0;
  private startValue: number = 0;
  private startTime: number = 0;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['value']) {
      const newValue = changes['value'].currentValue ?? 0;
      const oldValue = changes['value'].previousValue ?? 0;

      // Skip animation on first load or if values are the same
      if (changes['value'].firstChange || newValue === oldValue) {
        this.currentValue = newValue;
        this.displayValue = this.formatNumber(newValue);
        return;
      }

      this.animateValue(oldValue, newValue);
    }
  }

  ngOnDestroy(): void {
    this.cancelAnimation();
  }

  /**
   * Animate from old value to new value
   */
  private animateValue(from: number, to: number): void {
    this.cancelAnimation();

    this.startValue = from;
    this.currentValue = from;
    this.startTime = performance.now();
    this.isAnimating = true;

    const animate = (currentTime: number) => {
      const elapsed = currentTime - this.startTime;
      const progress = Math.min(elapsed / this.duration, 1);

      // Ease-out cubic for smooth deceleration
      const easeOut = 1 - Math.pow(1 - progress, 3);

      this.currentValue = this.startValue + (to - this.startValue) * easeOut;
      this.displayValue = this.formatNumber(this.currentValue);
      this.cdr.markForCheck();

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate);
      } else {
        this.currentValue = to;
        this.displayValue = this.formatNumber(to);
        this.isAnimating = false;
        this.animationFrameId = null;
        this.cdr.markForCheck();
      }
    };

    this.animationFrameId = requestAnimationFrame(animate);
  }

  /**
   * Cancel ongoing animation
   */
  private cancelAnimation(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
      this.isAnimating = false;
    }
  }

  /**
   * Format number for display
   */
  formatNumber(value: number): string {
    const rounded = Number(value.toFixed(this.decimals));

    if (this.useGrouping) {
      return rounded.toLocaleString('en-US', {
        minimumFractionDigits: this.decimals,
        maximumFractionDigits: this.decimals,
      });
    }

    return rounded.toFixed(this.decimals);
  }
}
