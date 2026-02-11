import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ElementRef,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';

// Register Chart.js components
Chart.register(...registerables);

export interface ProgressChartData {
  completed: number;
  inProgress: number;
  pending: number;
  blocked: number;
}

/**
 * Progress chart component using Chart.js.
 * Displays a doughnut chart showing task status distribution.
 */
@Component({
  selector: 'app-progress-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-container">
      <canvas #chartCanvas></canvas>
      <div class="chart-center">
        <span class="chart-value">{{ completionPercent }}%</span>
        <span class="chart-label">Complete</span>
      </div>
    </div>
  `,
  styles: [`
    .chart-container {
      position: relative;
      width: 200px;
      height: 200px;
      margin: 0 auto;
    }

    .chart-center {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
    }

    .chart-value {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: #1f2937;
    }

    .chart-label {
      display: block;
      font-size: 12px;
      color: #6b7280;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    canvas {
      max-width: 100%;
      max-height: 100%;
    }
  `],
})
export class ProgressChartComponent implements AfterViewInit, OnChanges {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @Input() data: ProgressChartData = { completed: 0, inProgress: 0, pending: 0, blocked: 0 };

  private chart: Chart<'doughnut'> | null = null;

  get completionPercent(): number {
    const total = this.data.completed + this.data.inProgress + this.data.pending + this.data.blocked;
    if (total === 0) return 0;
    return Math.round((this.data.completed / total) * 100);
  }

  ngAfterViewInit(): void {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && this.chart) {
      this.updateChart();
    }
  }

  private createChart(): void {
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'In Progress', 'Pending', 'Blocked'],
        datasets: [
          {
            data: [
              this.data.completed,
              this.data.inProgress,
              this.data.pending,
              this.data.blocked,
            ],
            backgroundColor: [
              '#10b981', // Green for completed
              '#3b82f6', // Blue for in progress
              '#f59e0b', // Amber for pending
              '#ef4444', // Red for blocked
            ],
            borderWidth: 0,
            hoverOffset: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        cutout: '70%',
        plugins: {
          legend: {
            display: false,
          },
          tooltip: {
            enabled: true,
            backgroundColor: '#1f2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = context.parsed || 0;
                const total =
                  this.data.completed +
                  this.data.inProgress +
                  this.data.pending +
                  this.data.blocked;
                const percentage = total > 0 ? Math.round((value / total) * 100) : 0;
                return `${label}: ${value} (${percentage}%)`;
              },
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }

  private updateChart(): void {
    if (!this.chart) return;

    this.chart.data.datasets[0].data = [
      this.data.completed,
      this.data.inProgress,
      this.data.pending,
      this.data.blocked,
    ];

    this.chart.update();
  }
}
