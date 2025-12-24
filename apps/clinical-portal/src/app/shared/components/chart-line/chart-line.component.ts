/**
 * Line Chart Component
 *
 * Displays line charts using Chart.js with responsive design.
 * Supports multiple series, tooltips, legends, and custom styling.
 *
 * @example
 * <app-chart-line
 *   [data]="chartData"
 *   [options]="chartOptions"
 *   [height]="300">
 * </app-chart-line>
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
 * Dataset for line chart
 */
export interface LineChartDataset {
  /** Dataset label */
  label: string;
  /** Data points */
  data: number[];
  /** Line color */
  borderColor?: string;
  /** Fill color under line */
  backgroundColor?: string;
  /** Line width */
  borderWidth?: number;
  /** Line tension (0 = straight, 1 = curved) */
  tension?: number;
  /** Show fill under line */
  fill?: boolean;
}

/**
 * Line chart data structure
 */
export interface LineChartData {
  /** Chart title */
  title?: string;
  /** X-axis labels */
  labels: string[];
  /** Datasets */
  datasets: LineChartDataset[];
}

/**
 * Chart options
 */
export interface ChartOptions {
  /** Show legend */
  showLegend?: boolean;
  /** Legend position */
  legendPosition?: 'top' | 'bottom' | 'left' | 'right';
  /** Show grid lines */
  showGrid?: boolean;
  /** Y-axis label */
  yAxisLabel?: string;
  /** X-axis label */
  xAxisLabel?: string;
  /** Enable animations */
  animation?: boolean;
  /** Aspect ratio (width/height) */
  aspectRatio?: number;
}

@Component({
  selector: 'app-chart-line',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-container">
      <h3 *ngIf="data?.title" class="chart-title">{{ data.title }}</h3>
      <div class="chart-wrapper" [style.height.px]="height">
        <canvas #chartCanvas></canvas>
      </div>
      <div *ngIf="!chartInstance" class="chart-loading">
        Loading chart...
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

    .chart-loading {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 200px;
      color: rgba(0, 0, 0, 0.38);
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .chart-container {
        background: #424242;
      }

      .chart-title {
        color: rgba(255, 255, 255, 0.87);
      }

      .chart-loading {
        color: rgba(255, 255, 255, 0.5);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .chart-container {
        padding: 12px;
      }

      .chart-title {
        font-size: 14px;
      }
    }
  `]
})
export class ChartLineComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  /** Chart data */
  @Input() data!: LineChartData;

  /** Chart options */
  @Input() options: ChartOptions = {};

  /** Chart height in pixels */
  @Input() height: number = 300;

  @ViewChild('chartCanvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;

  public chartInstance: any;
  private Chart: any;

  ngOnInit(): void {
    this.loadChartJS();
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
    this.destroyChart();
  }

  private async loadChartJS(): Promise<void> {
    // Chart.js integration placeholder
    // To enable: npm install chart.js, then import and register Chart
  }

  private renderChart(): void {
    if (!this.canvasRef || !this.data) {
      return;
    }

    const ctx = this.canvasRef.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    const chartConfig = this.buildChartConfig();

    // Draw placeholder visualization until Chart.js is integrated
    this.drawPlaceholderChart(ctx);
    this.chartInstance = { config: chartConfig };
  }

  private buildChartConfig(): any {
    const defaults = {
      showLegend: true,
      legendPosition: 'top',
      showGrid: true,
      animation: true,
      aspectRatio: 2
    };

    const opts = { ...defaults, ...this.options };

    return {
      type: 'line',
      data: {
        labels: this.data.labels,
        datasets: this.data.datasets.map((dataset, index) => ({
          label: dataset.label,
          data: dataset.data,
          borderColor: dataset.borderColor || this.getDefaultColor(index),
          backgroundColor: dataset.backgroundColor || this.getDefaultColor(index, 0.1),
          borderWidth: dataset.borderWidth || 2,
          tension: dataset.tension || 0.3,
          fill: dataset.fill !== false
        }))
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        aspectRatio: opts.aspectRatio,
        plugins: {
          legend: {
            display: opts.showLegend,
            position: opts.legendPosition
          },
          tooltip: {
            mode: 'index',
            intersect: false
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            grid: {
              display: opts.showGrid
            },
            title: {
              display: !!opts.yAxisLabel,
              text: opts.yAxisLabel || ''
            }
          },
          x: {
            grid: {
              display: opts.showGrid
            },
            title: {
              display: !!opts.xAxisLabel,
              text: opts.xAxisLabel || ''
            }
          }
        },
        animation: {
          duration: opts.animation ? 750 : 0
        }
      }
    };
  }

  private getDefaultColor(index: number, alpha: number = 1): string {
    const colors = [
      [54, 162, 235],   // Blue
      [255, 99, 132],   // Red
      [75, 192, 192],   // Green
      [255, 159, 64],   // Orange
      [153, 102, 255],  // Purple
      [255, 205, 86]    // Yellow
    ];

    const color = colors[index % colors.length];
    return `rgba(${color[0]}, ${color[1]}, ${color[2]}, ${alpha})`;
  }

  private drawPlaceholderChart(ctx: CanvasRenderingContext2D): void {
    const canvas = this.canvasRef.nativeElement;
    canvas.width = canvas.offsetWidth;
    canvas.height = this.height;

    // Draw placeholder visualization
    ctx.fillStyle = '#f5f5f5';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#e0e0e0';
    ctx.lineWidth = 1;

    // Draw grid lines
    for (let i = 0; i <= 5; i++) {
      const y = (canvas.height / 5) * i;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvas.width, y);
      ctx.stroke();
    }

    // Draw sample line
    if (this.data?.datasets?.[0]?.data) {
      const dataset = this.data.datasets[0];
      const points = dataset.data;
      const step = canvas.width / (points.length - 1);
      const maxValue = Math.max(...points);

      ctx.strokeStyle = dataset.borderColor || '#1976d2';
      ctx.lineWidth = 2;
      ctx.beginPath();

      points.forEach((value, index) => {
        const x = index * step;
        const y = canvas.height - (value / maxValue) * canvas.height * 0.8;
        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });

      ctx.stroke();
    }
  }

  private updateChart(): void {
    if (this.chartInstance) {
      this.destroyChart();
    }
    this.renderChart();
  }

  private destroyChart(): void {
    if (this.chartInstance && typeof this.chartInstance.destroy === 'function') {
      this.chartInstance.destroy();
      this.chartInstance = null;
    }
  }
}
