import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, takeUntil } from 'rxjs';
import { SystemEventsService } from '../../../services/system-events.service';
import { LiveMetrics } from '../../../models/system-event.model';
import { AnimatedCounterComponent } from '../animated-counter/animated-counter.component';
import { StatusIndicatorComponent } from '../status-indicator/status-indicator.component';

/**
 * LiveMetricsPanel Component
 *
 * Displays real-time system performance metrics with animated counters and gauges.
 *
 * Metrics displayed:
 * - Patients Processed (with change indicator)
 * - Throughput (evaluations/second)
 * - Compliance Rate (with trend)
 * - Open Care Gaps (with change)
 * - Success Rate (progress bar)
 * - Average Processing Time
 *
 * @example
 * <app-live-metrics-panel></app-live-metrics-panel>
 */
@Component({
  selector: 'app-live-metrics-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    AnimatedCounterComponent,
    StatusIndicatorComponent,
  ],
  template: `
    <mat-card class="metrics-panel">
      <mat-card-header class="panel-header">
        <mat-card-title class="panel-title">
          <mat-icon class="title-icon">speed</mat-icon>
          System Performance
        </mat-card-title>
        <app-status-indicator
          [status]="connectionStatus"
          size="small"
          [showLabel]="true">
        </app-status-indicator>
      </mat-card-header>

      <mat-card-content class="metrics-grid">
        <!-- Patients Processed -->
        <div class="metric-card" matTooltip="Total patients processed today">
          <div class="metric-icon patients">
            <mat-icon>people</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Patients</div>
            <div class="metric-value">
              <app-animated-counter
                [value]="metrics.patientsProcessed"
                [duration]="400"
                [showChange]="true"
                [changeValue]="metrics.patientsProcessedChange">
              </app-animated-counter>
            </div>
            <div class="metric-sublabel" *ngIf="metrics.patientsProcessedChange">
              +{{ metrics.patientsProcessedChange }} this session
            </div>
          </div>
        </div>

        <!-- Throughput -->
        <div class="metric-card" matTooltip="Current processing throughput">
          <div class="metric-icon throughput">
            <mat-icon>bolt</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Throughput</div>
            <div class="metric-value">
              <app-animated-counter
                [value]="metrics.throughputPerSecond"
                [duration]="300"
                [decimals]="1"
                suffix="/sec">
              </app-animated-counter>
            </div>
            <div class="throughput-bar">
              <div class="throughput-fill"
                   [style.width.%]="getThroughputPercent()">
              </div>
            </div>
          </div>
        </div>

        <!-- Compliance Rate -->
        <div class="metric-card" matTooltip="Overall compliance rate">
          <div class="metric-icon compliance">
            <mat-icon>verified</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Compliance</div>
            <div class="metric-value">
              <app-animated-counter
                [value]="metrics.complianceRate"
                [duration]="500"
                [decimals]="1"
                suffix="%">
              </app-animated-counter>
            </div>
            <div class="metric-trend" [class]="getTrendClass(metrics.complianceRateChange)">
              <mat-icon class="trend-icon">{{ getTrendIcon(metrics.complianceRateChange) }}</mat-icon>
              {{ formatTrend(metrics.complianceRateChange) }}
            </div>
          </div>
        </div>

        <!-- Care Gaps -->
        <div class="metric-card" matTooltip="Open care gaps requiring attention">
          <div class="metric-icon care-gaps">
            <mat-icon>notification_important</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Open Gaps</div>
            <div class="metric-value">
              <app-animated-counter
                [value]="metrics.openCareGaps"
                [duration]="400">
              </app-animated-counter>
            </div>
            <div class="metric-change" [class]="getGapChangeClass()">
              <mat-icon class="change-icon">{{ getGapChangeIcon() }}</mat-icon>
              {{ formatGapChange() }}
            </div>
          </div>
        </div>

        <!-- Success Rate -->
        <div class="metric-card wide" matTooltip="Success rate of last 100 operations">
          <div class="metric-icon success">
            <mat-icon>check_circle</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Success Rate</div>
            <div class="metric-value-row">
              <app-animated-counter
                [value]="metrics.successRate"
                [duration]="400"
                [decimals]="1"
                suffix="%">
              </app-animated-counter>
              <span class="metric-hint">(last 100 ops)</span>
            </div>
            <mat-progress-bar
              mode="determinate"
              [value]="metrics.successRate"
              [color]="getSuccessRateColor()">
            </mat-progress-bar>
          </div>
        </div>

        <!-- Processing Time -->
        <div class="metric-card" matTooltip="Average processing time per operation">
          <div class="metric-icon processing-time">
            <mat-icon>timer</mat-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">Avg Time</div>
            <div class="metric-value">
              <app-animated-counter
                [value]="metrics.avgProcessingTimeMs"
                [duration]="300"
                suffix="ms">
              </app-animated-counter>
            </div>
            <div class="metric-sublabel">
              {{ getProcessingTimeLabel() }}
            </div>
          </div>
        </div>
      </mat-card-content>

      <div class="panel-footer">
        <span class="last-update">
          Last updated: {{ formatLastUpdated() }}
        </span>
      </div>
    </mat-card>
  `,
  styles: [`
    .metrics-panel {
      height: 100%;
      display: flex;
      flex-direction: column;
      background-color: var(--card-background);
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color);
    }

    .panel-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1rem;
      margin: 0;
      color: var(--text-primary);
    }

    .title-icon {
      color: var(--primary-color);
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      padding: 16px;
      flex: 1;
    }

    .metric-card {
      display: flex;
      gap: 12px;
      padding: 12px;
      background-color: var(--bg-tertiary);
      border-radius: 8px;
      border: 1px solid var(--border-color);
    }

    .metric-card.wide {
      grid-column: span 2;
    }

    .metric-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .metric-icon mat-icon {
      font-size: 22px;
      width: 22px;
      height: 22px;
      color: white;
    }

    .metric-icon.patients { background: linear-gradient(135deg, #1976d2, #1565c0); }
    .metric-icon.throughput { background: linear-gradient(135deg, #7b1fa2, #6a1b9a); }
    .metric-icon.compliance { background: linear-gradient(135deg, #388e3c, #2e7d32); }
    .metric-icon.care-gaps { background: linear-gradient(135deg, #f57c00, #ef6c00); }
    .metric-icon.success { background: linear-gradient(135deg, #00897b, #00796b); }
    .metric-icon.processing-time { background: linear-gradient(135deg, #5c6bc0, #3f51b5); }

    .metric-content {
      flex: 1;
      min-width: 0;
    }

    .metric-label {
      font-size: 0.75rem;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 4px;
    }

    .metric-value {
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.2;
    }

    .metric-value-row {
      display: flex;
      align-items: baseline;
      gap: 8px;
    }

    .metric-hint {
      font-size: 0.7rem;
      color: var(--text-hint);
    }

    .metric-sublabel {
      font-size: 0.7rem;
      color: var(--text-hint);
      margin-top: 4px;
    }

    .metric-trend {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 0.75rem;
      margin-top: 4px;
    }

    .metric-trend .trend-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .metric-trend.trend-up {
      color: var(--status-success);
    }

    .metric-trend.trend-down {
      color: var(--status-error);
    }

    .metric-trend.trend-stable {
      color: var(--text-secondary);
    }

    .metric-change {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 0.75rem;
      margin-top: 4px;
    }

    .metric-change .change-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }

    .metric-change.positive {
      color: var(--status-success);
    }

    .metric-change.negative {
      color: var(--status-error);
    }

    .metric-change.neutral {
      color: var(--text-secondary);
    }

    .throughput-bar {
      height: 4px;
      background-color: var(--bg-hover);
      border-radius: 2px;
      margin-top: 6px;
      overflow: hidden;
    }

    .throughput-fill {
      height: 100%;
      background: linear-gradient(90deg, #7b1fa2, #9c27b0);
      border-radius: 2px;
      transition: width 0.3s ease;
    }

    mat-progress-bar {
      margin-top: 8px;
      border-radius: 2px;
    }

    ::ng-deep .mat-mdc-progress-bar {
      --mdc-linear-progress-track-height: 6px;
      --mdc-linear-progress-active-indicator-height: 6px;
      border-radius: 3px;
    }

    .panel-footer {
      padding: 8px 16px;
      font-size: 0.7rem;
      color: var(--text-hint);
      border-top: 1px solid var(--border-color);
      background-color: var(--bg-tertiary);
    }

    .last-update {
      font-style: italic;
    }

    @media (max-width: 600px) {
      .metrics-grid {
        grid-template-columns: 1fr;
      }

      .metric-card.wide {
        grid-column: span 1;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LiveMetricsPanelComponent implements OnInit, OnDestroy {
  metrics: LiveMetrics = {
    patientsProcessed: 0,
    patientsProcessedChange: 0,
    throughputPerSecond: 0,
    maxThroughput: 10,
    complianceRate: 0,
    complianceRateChange: 0,
    openCareGaps: 0,
    careGapsChange: 0,
    successRate: 100,
    avgProcessingTimeMs: 0,
    lastUpdated: new Date().toISOString(),
  };

  connectionStatus: 'connected' | 'disconnected' | 'simulating' = 'disconnected';

  private destroy$ = new Subject<void>();

  constructor(
    private eventsService: SystemEventsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to metrics updates
    this.eventsService.metrics$
      .pipe(takeUntil(this.destroy$))
      .subscribe(metrics => {
        this.metrics = metrics;
        this.cdr.markForCheck();
      });

    // Subscribe to connection status
    this.eventsService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.connectionStatus = status;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get throughput as percentage of max
   */
  getThroughputPercent(): number {
    return Math.min(100, (this.metrics.throughputPerSecond / this.metrics.maxThroughput) * 100);
  }

  /**
   * Get trend class based on change value
   */
  getTrendClass(change: number): string {
    if (change > 0.5) return 'trend-up';
    if (change < -0.5) return 'trend-down';
    return 'trend-stable';
  }

  /**
   * Get trend icon based on change value
   */
  getTrendIcon(change: number): string {
    if (change > 0.5) return 'trending_up';
    if (change < -0.5) return 'trending_down';
    return 'trending_flat';
  }

  /**
   * Format trend value
   */
  formatTrend(change: number): string {
    if (Math.abs(change) < 0.1) return 'Stable';
    const sign = change > 0 ? '+' : '';
    return `${sign}${change.toFixed(1)}%`;
  }

  /**
   * Get care gap change class
   */
  getGapChangeClass(): string {
    if (this.metrics.careGapsChange > 0) return 'negative'; // More gaps is bad
    if (this.metrics.careGapsChange < 0) return 'positive'; // Fewer gaps is good
    return 'neutral';
  }

  /**
   * Get care gap change icon
   */
  getGapChangeIcon(): string {
    if (this.metrics.careGapsChange > 0) return 'arrow_upward';
    if (this.metrics.careGapsChange < 0) return 'arrow_downward';
    return 'remove';
  }

  /**
   * Format care gap change
   */
  formatGapChange(): string {
    if (this.metrics.careGapsChange === 0) return 'No change';
    const sign = this.metrics.careGapsChange > 0 ? '+' : '';
    return `${sign}${this.metrics.careGapsChange} today`;
  }

  /**
   * Get success rate progress bar color
   */
  getSuccessRateColor(): 'primary' | 'accent' | 'warn' {
    if (this.metrics.successRate >= 95) return 'primary';
    if (this.metrics.successRate >= 80) return 'accent';
    return 'warn';
  }

  /**
   * Get processing time label
   */
  getProcessingTimeLabel(): string {
    const time = this.metrics.avgProcessingTimeMs;
    if (time < 50) return 'Excellent';
    if (time < 100) return 'Good';
    if (time < 200) return 'Normal';
    if (time < 500) return 'Slow';
    return 'Very slow';
  }

  /**
   * Format last updated time
   */
  formatLastUpdated(): string {
    const date = new Date(this.metrics.lastUpdated);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }
}
