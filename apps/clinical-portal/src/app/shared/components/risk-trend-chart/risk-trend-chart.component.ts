import {
  Component,
  Input,
  OnInit,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ElementRef,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { PatientHealthService } from '../../../services/patient-health.service';
import { RiskAssessmentService } from '../../../services/risk-assessment.service';
import { HealthScoreHistory, RiskLevel } from '../../../models/patient-health.model';
import { RiskAssessment } from '../../../models/risk-assessment.model';
import { LoggerService } from '../../../services/logger.service';

/**
 * Time range options for the chart
 */
export type TimeRange = '30d' | '90d' | '6m' | '1y' | 'custom';

/**
 * Risk data point for charting
 */
export interface RiskDataPoint {
  date: Date;
  value: number;
  label?: string;
}

/**
 * Chart series configuration
 */
export interface ChartSeries {
  id: string;
  label: string;
  color: string;
  data: RiskDataPoint[];
  visible: boolean;
}

/**
 * Trend indicator
 */
export interface TrendIndicator {
  direction: 'improving' | 'stable' | 'declining';
  percentChange: number;
  periodDescription: string;
}

/**
 * Risk threshold marker
 */
export interface RiskThreshold {
  level: RiskLevel;
  value: number;
  color: string;
  label: string;
}

@Component({
  selector: 'app-risk-trend-chart',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonToggleModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatCheckboxModule,
  ],
  template: `
    <mat-card class="risk-trend-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>trending_up</mat-icon>
          Risk Score Trend
        </mat-card-title>

        <div class="header-controls">
          <!-- Time Range Selector -->
          <mat-button-toggle-group
            [(ngModel)]="selectedRange"
            (change)="onRangeChange()"
            aria-label="Time range">
            <mat-button-toggle value="30d">30D</mat-button-toggle>
            <mat-button-toggle value="90d">90D</mat-button-toggle>
            <mat-button-toggle value="6m">6M</mat-button-toggle>
            <mat-button-toggle value="1y">1Y</mat-button-toggle>
            <mat-button-toggle value="custom">Custom</mat-button-toggle>
          </mat-button-toggle-group>
        </div>
      </mat-card-header>

      <mat-card-content>
        <!-- Custom Date Range -->
        @if (selectedRange === 'custom') {
          <div class="custom-date-range">
            <mat-form-field appearance="outline">
              <mat-label>Start Date</mat-label>
              <input
                matInput
                [matDatepicker]="startPicker"
                [(ngModel)]="customStartDate"
                (dateChange)="onCustomDateChange()">
              <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
              <mat-datepicker #startPicker></mat-datepicker>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>End Date</mat-label>
              <input
                matInput
                [matDatepicker]="endPicker"
                [(ngModel)]="customEndDate"
                (dateChange)="onCustomDateChange()">
              <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
              <mat-datepicker #endPicker></mat-datepicker>
            </mat-form-field>
          </div>
        }

        <!-- Trend Indicator -->
        @if (trendIndicator && !loading) {
          <div class="trend-indicator" [class]="trendIndicator.direction">
            <mat-icon>{{ getTrendIcon() }}</mat-icon>
            <span class="change">{{ trendIndicator.percentChange | number:'1.1-1' }}%</span>
            <span class="period">{{ trendIndicator.periodDescription }}</span>
          </div>
        }

        <!-- Loading State -->
        @if (loading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <span>Loading trend data...</span>
          </div>
        }

        <!-- Chart Container -->
        @if (!loading && dataSeries.length > 0) {
          <div class="chart-container" [style.height.px]="height">
            <canvas #chartCanvas></canvas>
          </div>

          <!-- Threshold Legend -->
          @if (showThresholds) {
            <div class="threshold-legend">
              @for (threshold of thresholds; track threshold.level) {
                <div class="threshold-item">
                  <span class="threshold-line" [style.backgroundColor]="threshold.color"></span>
                  <span class="threshold-label">{{ threshold.label }}</span>
                </div>
              }
            </div>
          }

          <!-- Series Legend -->
          <div class="series-legend">
            @for (series of dataSeries; track series.id) {
              <div
                class="legend-item"
                [class.hidden]="!series.visible"
                (click)="toggleSeries(series.id)">
                <span class="color-dot" [style.backgroundColor]="series.color"></span>
                <span>{{ series.label }}</span>
                <mat-checkbox
                  [checked]="series.visible"
                  (click)="$event.stopPropagation()"
                  (change)="toggleSeries(series.id)">
                </mat-checkbox>
              </div>
            }
          </div>
        }

        <!-- No Data State -->
        @if (!loading && dataSeries.length === 0) {
          <div class="no-data">
            <mat-icon>show_chart</mat-icon>
            <p>No trend data available for this period</p>
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .risk-trend-card {
        mat-card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 16px;

          mat-card-title {
            display: flex;
            align-items: center;
            gap: 8px;

            mat-icon {
              color: var(--primary-color, #1976d2);
            }
          }
        }

        mat-card-content {
          padding-top: 16px;
        }
      }

      .custom-date-range {
        display: flex;
        gap: 16px;
        margin-bottom: 16px;

        mat-form-field {
          flex: 1;
          max-width: 200px;
        }
      }

      .trend-indicator {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;
        border-radius: 4px;
        margin-bottom: 16px;
        width: fit-content;

        &.improving {
          background: #e8f5e9;
          color: #2e7d32;

          mat-icon {
            color: #2e7d32;
          }
        }

        &.stable {
          background: #fff3e0;
          color: #e65100;

          mat-icon {
            color: #e65100;
          }
        }

        &.declining {
          background: #ffebee;
          color: #c62828;

          mat-icon {
            color: #c62828;
          }
        }

        .change {
          font-weight: 600;
          font-size: 16px;
        }

        .period {
          font-size: 13px;
          color: inherit;
          opacity: 0.8;
        }
      }

      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px;
        gap: 16px;
        color: var(--text-secondary);
      }

      .chart-container {
        position: relative;
        width: 100%;
        min-height: 250px;

        canvas {
          width: 100% !important;
          height: 100% !important;
        }
      }

      .threshold-legend {
        display: flex;
        gap: 16px;
        margin-top: 12px;
        padding: 8px 0;
        border-top: 1px solid var(--divider-color, #e0e0e0);

        .threshold-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: var(--text-secondary);

          .threshold-line {
            width: 20px;
            height: 2px;
            border-style: dashed;
          }
        }
      }

      .series-legend {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        margin-top: 16px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          padding: 4px 8px;
          border-radius: 4px;
          transition: opacity 0.2s ease;

          &:hover {
            background: var(--hover-background, rgba(0, 0, 0, 0.04));
          }

          &.hidden {
            opacity: 0.5;
          }

          .color-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
          }

          mat-checkbox {
            margin-left: auto;
          }
        }
      }

      .no-data {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px;
        color: var(--text-secondary);

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          margin-bottom: 16px;
        }
      }

      // Dark mode
      :host-context(.dark-theme) {
        .trend-indicator {
          &.improving {
            background: #1b5e20;
            color: #a5d6a7;
          }

          &.stable {
            background: #e65100;
            color: #ffcc80;
          }

          &.declining {
            background: #b71c1c;
            color: #ef9a9a;
          }
        }
      }
    `,
  ],
})
export class RiskTrendChartComponent
  implements OnInit, OnChanges, OnDestroy, AfterViewInit
{
  @Input() patientId!: string;
  @Input() height = 300;
  @Input() showThresholds = true;
  @Input() showComponents = true;

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  // Time range
  selectedRange: TimeRange = '90d';
  customStartDate: Date | null = null;
  customEndDate: Date | null = null;

  // Data
  dataSeries: ChartSeries[] = [];
  trendIndicator: TrendIndicator | null = null;
  loading = false;

  // Chart instance
  private chart: any = null;

  // Thresholds
  readonly thresholds: RiskThreshold[] = [
    { level: 'critical', value: 75, color: '#f44336', label: 'Very High (75+)' },
    { level: 'high', value: 50, color: '#ff9800', label: 'High (50-74)' },
    { level: 'moderate', value: 25, color: '#8bc34a', label: 'Moderate (25-49)' },
  ];

  // Series colors
  private readonly seriesColors = {
    overall: '#1976d2',
    physical: '#4caf50',
    mental: '#9c27b0',
    social: '#ff9800',
    preventive: '#00bcd4',
    chronic: '#795548',
  };

  private destroy$ = new Subject<void>();
  private logger = this.loggerService.withContext('RiskTrendChartComponent');

  constructor(
    private patientHealthService: PatientHealthService,
    private riskAssessmentService: RiskAssessmentService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patientId'] && !changes['patientId'].firstChange) {
      this.loadData();
    }
  }

  ngAfterViewInit(): void {
    // Chart will be initialized after data loads
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.chart) {
      this.chart.destroy();
    }
  }

  onRangeChange(): void {
    if (this.selectedRange !== 'custom') {
      this.loadData();
    }
  }

  onCustomDateChange(): void {
    if (this.customStartDate && this.customEndDate) {
      this.loadData();
    }
  }

  toggleSeries(seriesId: string): void {
    const series = this.dataSeries.find((s) => s.id === seriesId);
    if (series) {
      series.visible = !series.visible;
      this.updateChart();
    }
  }

  getTrendIcon(): string {
    if (!this.trendIndicator) return 'trending_flat';

    switch (this.trendIndicator.direction) {
      case 'improving':
        return 'trending_down'; // Lower risk is better
      case 'declining':
        return 'trending_up'; // Higher risk is worse
      default:
        return 'trending_flat';
    }
  }

  private loadData(): void {
    if (!this.patientId) return;

    const { startDate, endDate } = this.getDateRange();
    this.loading = true;

    // Primary source: RiskAssessmentService for real risk data
    this.riskAssessmentService
      .getRiskHistory(this.patientId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (riskHistory) => {
          if (riskHistory && riskHistory.length > 0) {
            // Use risk assessment data
            this.processRiskAssessmentData(riskHistory);
            this.loading = false;
            this.initializeChart();
          } else {
            // Fallback to PatientHealthService for backwards compatibility
            this.loadHealthScoreData();
          }
        },
        error: (error) => {
          this.logger.error('Error loading risk assessment data', error);
          // Fallback to PatientHealthService on error
          this.loadHealthScoreData();
        },
      });
  }

  /**
   * Fallback method to load health score data when risk assessment data is unavailable
   */
  private loadHealthScoreData(): void {
    this.patientHealthService
      .getHealthScoreHistory(this.patientId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (history) => {
          this.processHealthScoreData(history);
          this.loading = false;
          this.initializeChart();
        },
        error: (error) => {
          this.logger.error('Error loading health score data', error);
          this.loading = false;
          this.dataSeries = [];
        },
      });
  }

  private getDateRange(): { startDate: Date; endDate: Date } {
    const endDate = new Date();
    let startDate: Date;

    if (this.selectedRange === 'custom' && this.customStartDate && this.customEndDate) {
      return {
        startDate: this.customStartDate,
        endDate: this.customEndDate,
      };
    }

    switch (this.selectedRange) {
      case '30d':
        startDate = new Date();
        startDate.setDate(startDate.getDate() - 30);
        break;
      case '90d':
        startDate = new Date();
        startDate.setDate(startDate.getDate() - 90);
        break;
      case '6m':
        startDate = new Date();
        startDate.setMonth(startDate.getMonth() - 6);
        break;
      case '1y':
        startDate = new Date();
        startDate.setFullYear(startDate.getFullYear() - 1);
        break;
      default:
        startDate = new Date();
        startDate.setDate(startDate.getDate() - 90);
    }

    return { startDate, endDate };
  }

  /**
   * Process risk assessment history data from RiskAssessmentService
   */
  private processRiskAssessmentData(history: RiskAssessment[]): void {
    if (!history || history.length === 0) {
      this.dataSeries = [];
      this.trendIndicator = null;
      return;
    }

    // Build overall risk series from risk assessments
    const overallData: RiskDataPoint[] = history.map((assessment) => ({
      date: new Date(assessment.assessmentDate),
      value: assessment.riskScore,
      label: assessment.riskLevel,
    }));

    this.dataSeries = [
      {
        id: 'overall',
        label: 'Overall Risk',
        color: this.seriesColors.overall,
        data: overallData,
        visible: true,
      },
    ];

    // Calculate trend
    this.trendIndicator = this.calculateTrend(overallData);
  }

  /**
   * Process health score history data from PatientHealthService (fallback)
   */
  private processHealthScoreData(history: HealthScoreHistory[]): void {
    if (!history || history.length === 0) {
      this.dataSeries = [];
      this.trendIndicator = null;
      return;
    }

    // Build overall risk series (inverse of health score)
    const overallData: RiskDataPoint[] = history.map((h) => ({
      date: new Date(h.calculatedAt),
      value: 100 - h.score, // Convert health score to risk score
    }));

    this.dataSeries = [
      {
        id: 'overall',
        label: 'Overall Risk',
        color: this.seriesColors.overall,
        data: overallData,
        visible: true,
      },
    ];

    // Calculate trend
    this.trendIndicator = this.calculateTrend(overallData);
  }

  private calculateTrend(data: RiskDataPoint[]): TrendIndicator | null {
    if (data.length < 2) return null;

    const sortedData = [...data].sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );

    const firstValue = sortedData[0].value;
    const lastValue = sortedData[sortedData.length - 1].value;
    const change = lastValue - firstValue;
    const percentChange = firstValue > 0 ? (change / firstValue) * 100 : 0;

    let direction: 'improving' | 'stable' | 'declining';
    if (percentChange < -5) {
      direction = 'improving'; // Risk decreased
    } else if (percentChange > 5) {
      direction = 'declining'; // Risk increased
    } else {
      direction = 'stable';
    }

    const { startDate, endDate } = this.getDateRange();
    const daysDiff = Math.ceil(
      (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)
    );

    let periodDescription: string;
    if (daysDiff <= 30) {
      periodDescription = 'over last 30 days';
    } else if (daysDiff <= 90) {
      periodDescription = 'over last 90 days';
    } else if (daysDiff <= 180) {
      periodDescription = 'over last 6 months';
    } else {
      periodDescription = 'over last year';
    }

    return {
      direction,
      percentChange: Math.abs(percentChange),
      periodDescription,
    };
  }

  private initializeChart(): void {
    if (!this.chartCanvas?.nativeElement) return;

    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    // Destroy existing chart
    if (this.chart) {
      this.chart.destroy();
    }

    // Build datasets
    const datasets = this.dataSeries
      .filter((s) => s.visible)
      .map((series) => ({
        label: series.label,
        data: series.data.map((d) => ({
          x: d.date,
          y: d.value,
        })),
        borderColor: series.color,
        backgroundColor: series.color + '20',
        fill: true,
        tension: 0.3,
        pointRadius: 3,
        pointHoverRadius: 5,
      }));

    // Build threshold annotations
    const annotations: any = {};
    if (this.showThresholds) {
      this.thresholds.forEach((threshold, index) => {
        annotations[`threshold${index}`] = {
          type: 'line',
          yMin: threshold.value,
          yMax: threshold.value,
          borderColor: threshold.color,
          borderWidth: 1,
          borderDash: [5, 5],
          label: {
            display: false,
          },
        };
      });
    }

    // Create chart using basic Chart.js
    // Note: In a real implementation, you'd import Chart.js
    // For now, we'll create a simple canvas-based visualization
    this.drawSimpleChart(ctx, datasets);
  }

  private drawSimpleChart(
    ctx: CanvasRenderingContext2D,
    datasets: any[]
  ): void {
    const canvas = ctx.canvas;
    const width = canvas.width;
    const height = canvas.height;

    // Clear canvas
    ctx.clearRect(0, 0, width, height);

    // Draw background
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);

    // Chart area
    const padding = { top: 20, right: 20, bottom: 40, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    // Draw grid lines
    ctx.strokeStyle = '#e0e0e0';
    ctx.lineWidth = 1;

    // Horizontal grid lines
    for (let i = 0; i <= 4; i++) {
      const y = padding.top + (chartHeight * i) / 4;
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();

      // Y-axis labels
      ctx.fillStyle = '#666666';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText((100 - i * 25).toString(), padding.left - 8, y + 4);
    }

    // Draw threshold lines
    if (this.showThresholds) {
      this.thresholds.forEach((threshold) => {
        const y = padding.top + chartHeight * (1 - threshold.value / 100);
        ctx.strokeStyle = threshold.color;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(width - padding.right, y);
        ctx.stroke();
        ctx.setLineDash([]);
      });
    }

    // Draw data lines
    datasets.forEach((dataset) => {
      if (dataset.data.length < 2) return;

      const data = dataset.data.sort(
        (a: any, b: any) => a.x.getTime() - b.x.getTime()
      );
      const minTime = data[0].x.getTime();
      const maxTime = data[data.length - 1].x.getTime();
      const timeRange = maxTime - minTime || 1;

      ctx.strokeStyle = dataset.borderColor;
      ctx.lineWidth = 2;
      ctx.beginPath();

      data.forEach((point: any, index: number) => {
        const x =
          padding.left + ((point.x.getTime() - minTime) / timeRange) * chartWidth;
        const y = padding.top + chartHeight * (1 - point.y / 100);

        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });

      ctx.stroke();

      // Draw points
      ctx.fillStyle = dataset.borderColor;
      data.forEach((point: any) => {
        const x =
          padding.left + ((point.x.getTime() - minTime) / timeRange) * chartWidth;
        const y = padding.top + chartHeight * (1 - point.y / 100);

        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();
      });
    });

    // Draw axes
    ctx.strokeStyle = '#333333';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padding.left, padding.top);
    ctx.lineTo(padding.left, height - padding.bottom);
    ctx.lineTo(width - padding.right, height - padding.bottom);
    ctx.stroke();

    // Y-axis label
    ctx.save();
    ctx.translate(15, height / 2);
    ctx.rotate(-Math.PI / 2);
    ctx.fillStyle = '#666666';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Risk Score', 0, 0);
    ctx.restore();
  }

  private updateChart(): void {
    this.initializeChart();
  }
}
