/**
 * Bar Chart Component
 *
 * Displays bar charts with support for grouped and stacked modes.
 *
 * @example
 * <app-chart-bar
 *   [data]="chartData"
 *   [mode]="'grouped'"
 *   [height]="300">
 * </app-chart-bar>
 */
import {
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Bar chart dataset
 */
export interface BarChartDataset {
  label: string;
  data: number[];
  backgroundColor?: string | string[];
  borderColor?: string | string[];
  borderWidth?: number;
}

/**
 * Bar chart data structure
 */
export interface BarChartData {
  title?: string;
  labels: string[];
  datasets: BarChartDataset[];
}

@Component({
  selector: 'app-chart-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-container">
      <h3 *ngIf="data?.title" class="chart-title">{{ data.title }}</h3>
      <div class="chart-wrapper" [style.height.px]="height">
        <canvas #chartCanvas></canvas>
      </div>
    </div>
  `,
  styles: [`
    .chart-container {
      width: 100%;
      padding: 16px;
      background: white;
      border-radius: 4px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
    }

    .chart-title {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.87);
    }

    .chart-wrapper {
      position: relative;
      width: 100%;
    }

    canvas {
      max-width: 100%;
      height: auto !important;
    }

    @media (prefers-color-scheme: dark) {
      .chart-container {
        background: #424242;
      }

      .chart-title {
        color: rgba(255, 255, 255, 0.87);
      }
    }
  `]
})
export class ChartBarComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  @Input() data!: BarChartData;
  @Input() mode: 'grouped' | 'stacked' = 'grouped';
  @Input() height = 300;
  @Input() horizontal = false;

  @ViewChild('chartCanvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private chartInstance: any;

  ngOnInit(): void {
    // Chart.js would be loaded here
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && !changes['data'].firstChange) {
      this.updateChart();
    }
  }

  ngAfterViewInit(): void {
    this.renderChart();
  }

  ngOnDestroy(): void {
    if (this.chartInstance) {
      this.chartInstance = null;
    }
  }

  private renderChart(): void {
    if (!this.canvasRef || !this.data) {
      return;
    }

    const ctx = this.canvasRef.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    this.drawPlaceholderChart(ctx);
  }

  private drawPlaceholderChart(ctx: CanvasRenderingContext2D): void {
    const canvas = this.canvasRef.nativeElement;
    canvas.width = canvas.offsetWidth;
    canvas.height = this.height;

    ctx.fillStyle = '#f5f5f5';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw placeholder bars
    if (this.data?.datasets?.[0]?.data) {
      const dataset = this.data.datasets[0];
      const values = dataset.data;
      const barWidth = canvas.width / (values.length * 2);
      const maxValue = Math.max(...values);

      values.forEach((value, index) => {
        const barHeight = (value / maxValue) * canvas.height * 0.8;
        const x = index * barWidth * 2 + barWidth / 2;
        const y = canvas.height - barHeight;

        ctx.fillStyle = dataset.backgroundColor as string || '#1976d2';
        ctx.fillRect(x, y, barWidth, barHeight);
      });
    }
  }

  private updateChart(): void {
    if (this.chartInstance) {
      this.chartInstance = null;
    }
    this.renderChart();
  }
}
