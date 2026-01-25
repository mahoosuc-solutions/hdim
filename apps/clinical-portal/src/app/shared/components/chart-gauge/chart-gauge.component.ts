/**
 * Gauge Chart Component
 *
 * Displays a circular gauge/progress indicator showing percentage values.
 * Color-coded: Red <40%, Orange 40-70%, Green >70%
 *
 * @example
 * <app-chart-gauge
 *   [value]="85"
 *   [label]="'Quality Score'"
 *   [color]="'auto'">
 * </app-chart-gauge>
 */
import {
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  ViewChild,
  ElementRef,
  AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-chart-gauge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="gauge-container">
      <div class="gauge-wrapper">
        <svg #gaugeSvg class="gauge-svg" viewBox="0 0 200 200">
          <!-- Background arc -->
          <path
            [attr.d]="backgroundArc"
            fill="none"
            stroke="#e0e0e0"
            stroke-width="20"
            stroke-linecap="round">
          </path>
          <!-- Value arc -->
          <path
            [attr.d]="valueArc"
            fill="none"
            [attr.stroke]="gaugeColor"
            stroke-width="20"
            stroke-linecap="round"
            class="value-arc">
          </path>
          <!-- Center text -->
          <text
            x="100"
            y="95"
            text-anchor="middle"
            class="gauge-value"
            [attr.fill]="gaugeColor">
            {{ displayValue }}%
          </text>
          <text
            x="100"
            y="115"
            text-anchor="middle"
            class="gauge-label">
            {{ label }}
          </text>
        </svg>
      </div>
    </div>
  `,
  styles: [`
    .gauge-container {
      width: 100%;
      padding: 16px;
      background: white;
      border-radius: 4px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
    }

    .gauge-wrapper {
      width: 100%;
      max-width: 300px;
      margin: 0 auto;
    }

    .gauge-svg {
      width: 100%;
      height: auto;
    }

    .value-arc {
      transition: stroke-dashoffset 1s ease-out;
    }

    .gauge-value {
      font-size: 32px;
      font-weight: 700;
      transition: fill 0.3s ease;
    }

    .gauge-label {
      font-size: 14px;
      fill: rgba(0, 0, 0, 0.6);
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .gauge-container {
        background: #424242;
      }

      .gauge-label {
        fill: rgba(255, 255, 255, 0.7);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .gauge-container {
        padding: 12px;
      }

      .gauge-value {
        font-size: 28px;
      }

      .gauge-label {
        font-size: 12px;
      }
    }
  `]
})
export class ChartGaugeComponent implements OnInit, OnChanges, AfterViewInit {
  /** Value to display (0-100) */
  @Input() value = 0;

  /** Label text */
  @Input() label = 'Score';

  /** Color mode: 'auto', 'primary', 'accent', 'warn', or custom hex */
  @Input() color = 'auto';

  /** Minimum value */
  @Input() min = 0;

  /** Maximum value */
  @Input() max = 100;

  @ViewChild('gaugeSvg', { static: false }) svgRef!: ElementRef<SVGElement>;

  displayValue = 0;
  backgroundArc = '';
  valueArc = '';
  gaugeColor = '#4caf50';

  ngOnInit(): void {
    this.validateValue();
    this.calculateArcs();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['value'] || changes['min'] || changes['max']) {
      this.validateValue();
      this.calculateArcs();
      this.animateValue();
    }
  }

  ngAfterViewInit(): void {
    this.animateValue();
  }

  private validateValue(): void {
    this.value = Math.max(this.min, Math.min(this.max, this.value));
    this.displayValue = Math.round(this.value);
    this.updateColor();
  }

  private updateColor(): void {
    if (this.color === 'auto') {
      // Auto color based on value
      if (this.value < 40) {
        this.gaugeColor = '#f44336'; // Red
      } else if (this.value < 70) {
        this.gaugeColor = '#ff9800'; // Orange
      } else {
        this.gaugeColor = '#4caf50'; // Green
      }
    } else if (this.color === 'primary') {
      this.gaugeColor = '#1976d2';
    } else if (this.color === 'accent') {
      this.gaugeColor = '#ff4081';
    } else if (this.color === 'warn') {
      this.gaugeColor = '#f44336';
    } else {
      this.gaugeColor = this.color;
    }
  }

  private calculateArcs(): void {
    const startAngle = -135; // Start at bottom-left
    const endAngle = 135; // End at bottom-right
    const totalAngle = endAngle - startAngle;

    // Background arc (full gauge)
    this.backgroundArc = this.describeArc(100, 100, 80, startAngle, endAngle);

    // Value arc
    const valueAngle = startAngle + (this.value / this.max) * totalAngle;
    this.valueArc = this.describeArc(100, 100, 80, startAngle, valueAngle);
  }

  private describeArc(x: number, y: number, radius: number, startAngle: number, endAngle: number): string {
    const start = this.polarToCartesian(x, y, radius, endAngle);
    const end = this.polarToCartesian(x, y, radius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1';

    return [
      'M', start.x, start.y,
      'A', radius, radius, 0, largeArcFlag, 0, end.x, end.y
    ].join(' ');
  }

  private polarToCartesian(centerX: number, centerY: number, radius: number, angleInDegrees: number): { x: number, y: number } {
    const angleInRadians = (angleInDegrees - 90) * Math.PI / 180.0;
    return {
      x: centerX + (radius * Math.cos(angleInRadians)),
      y: centerY + (radius * Math.sin(angleInRadians))
    };
  }

  private animateValue(): void {
    const start = 0;
    const end = this.value;
    const duration = 1000;
    const startTime = performance.now();

    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Easing function (ease-out)
      const easeProgress = 1 - Math.pow(1 - progress, 3);

      this.displayValue = Math.round(start + (end - start) * easeProgress);

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };

    requestAnimationFrame(animate);
  }
}
