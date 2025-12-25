import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Care Gap Trend Data Point
 */
export interface CareGapTrendPoint {
  date: Date;
  totalGaps: number;
  closedGaps: number;
  newGaps: number;
  byUrgency: {
    high: number;
    medium: number;
    low: number;
  };
  byType: {
    screening: number;
    medication: number;
    followup: number;
    lab: number;
    assessment: number;
  };
}

/**
 * Care Gap Trend Analysis Result
 */
export interface CareGapTrendAnalysis {
  direction: 'improving' | 'stable' | 'worsening';
  percentChange: number;
  pointsChange: number;
  period: string;
  insights: string[];
}

/**
 * Care Gap Trend Component
 *
 * Displays care gap trends over time with visual indicators
 * for improvement, stability, or worsening trends.
 *
 * Features:
 * - Trend direction indicators (improving/stable/worsening)
 * - Historical data visualization
 * - Key insights and recommendations
 * - Period comparison (week-over-week, month-over-month)
 */
@Component({
  selector: 'app-care-gap-trend',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <mat-card class="care-gap-trend-card" [class]="getTrendClass()">
      <mat-card-header>
        <mat-icon mat-card-avatar class="trend-icon">{{ getTrendIcon() }}</mat-icon>
        <mat-card-title>{{ title }}</mat-card-title>
        <mat-card-subtitle>{{ subtitle || 'Care gap trend analysis' }}</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        @if (loading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <span>Analyzing trends...</span>
          </div>
        } @else {
          <!-- Trend Summary -->
          <div class="trend-summary">
            <div class="trend-indicator" [class]="getTrendClass()">
              <mat-icon>{{ getTrendIcon() }}</mat-icon>
              <span class="trend-label">{{ getTrendLabel() }}</span>
            </div>
            <div class="trend-stats">
              <div class="stat">
                <span class="stat-value" [class.positive]="trendAnalysis.percentChange < 0" [class.negative]="trendAnalysis.percentChange > 0">
                  {{ formatPercentChange(trendAnalysis.percentChange) }}
                </span>
                <span class="stat-label">vs {{ trendAnalysis.period }}</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ currentTotal }}</span>
                <span class="stat-label">Current Gaps</span>
              </div>
            </div>
          </div>

          <!-- Mini Trend Chart -->
          @if (trendData.length > 1) {
            <div class="mini-chart">
              <div class="chart-bars">
                @for (point of getLastNPoints(7); track point.date) {
                  <div
                    class="bar-container"
                    [matTooltip]="getBarTooltip(point)">
                    <div
                      class="bar"
                      [style.height.%]="getBarHeight(point)"
                      [class]="getBarClass(point)">
                    </div>
                    <span class="bar-label">{{ formatBarLabel(point.date) }}</span>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Urgency Breakdown -->
          <div class="urgency-breakdown">
            <div class="breakdown-item high" [matTooltip]="'High urgency gaps'">
              <mat-icon>priority_high</mat-icon>
              <span class="count">{{ getUrgencyCount('high') }}</span>
              <span class="label">High</span>
              <span class="trend-indicator-mini" [class]="getUrgencyTrend('high')">
                {{ getUrgencyTrendIcon('high') }}
              </span>
            </div>
            <div class="breakdown-item medium" [matTooltip]="'Medium urgency gaps'">
              <mat-icon>remove</mat-icon>
              <span class="count">{{ getUrgencyCount('medium') }}</span>
              <span class="label">Medium</span>
              <span class="trend-indicator-mini" [class]="getUrgencyTrend('medium')">
                {{ getUrgencyTrendIcon('medium') }}
              </span>
            </div>
            <div class="breakdown-item low" [matTooltip]="'Low urgency gaps'">
              <mat-icon>keyboard_arrow_down</mat-icon>
              <span class="count">{{ getUrgencyCount('low') }}</span>
              <span class="label">Low</span>
              <span class="trend-indicator-mini" [class]="getUrgencyTrend('low')">
                {{ getUrgencyTrendIcon('low') }}
              </span>
            </div>
          </div>

          <!-- Insights -->
          @if (trendAnalysis.insights.length > 0 && showInsights) {
            <div class="insights-section">
              <div class="insights-header">
                <mat-icon>lightbulb</mat-icon>
                <span>Key Insights</span>
              </div>
              <ul class="insights-list">
                @for (insight of trendAnalysis.insights; track insight) {
                  <li>{{ insight }}</li>
                }
              </ul>
            </div>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .care-gap-trend-card {
      border-left: 4px solid #9e9e9e;
      transition: border-color 0.3s ease;

      &.improving {
        border-color: #4caf50;
      }

      &.stable {
        border-color: #ff9800;
      }

      &.worsening {
        border-color: #f44336;
      }
    }

    .trend-icon {
      font-size: 32px;
      width: 40px;
      height: 40px;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 32px;
      color: #666;
    }

    .trend-summary {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .trend-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 20px;
      font-weight: 600;
      font-size: 14px;

      &.improving {
        background: #e8f5e9;
        color: #2e7d32;

        mat-icon {
          color: #4caf50;
        }
      }

      &.stable {
        background: #fff3e0;
        color: #ef6c00;

        mat-icon {
          color: #ff9800;
        }
      }

      &.worsening {
        background: #ffebee;
        color: #c62828;

        mat-icon {
          color: #f44336;
        }
      }
    }

    .trend-stats {
      display: flex;
      gap: 24px;

      .stat {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;

        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: #333;

          &.positive {
            color: #4caf50;
          }

          &.negative {
            color: #f44336;
          }
        }

        .stat-label {
          font-size: 12px;
          color: #666;
          margin-top: 4px;
        }
      }
    }

    .mini-chart {
      margin-bottom: 16px;
      padding: 16px;
      background: #fafafa;
      border-radius: 8px;

      .chart-bars {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        height: 80px;
        gap: 8px;
      }

      .bar-container {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        height: 100%;
        justify-content: flex-end;
      }

      .bar {
        width: 100%;
        min-height: 4px;
        border-radius: 4px 4px 0 0;
        transition: all 0.3s ease;
        cursor: pointer;

        &.high {
          background: linear-gradient(to top, #f44336, #e57373);
        }

        &.medium {
          background: linear-gradient(to top, #ff9800, #ffb74d);
        }

        &.low {
          background: linear-gradient(to top, #4caf50, #81c784);
        }

        &:hover {
          opacity: 0.8;
          transform: scaleY(1.05);
        }
      }

      .bar-label {
        font-size: 10px;
        color: #999;
      }
    }

    .urgency-breakdown {
      display: flex;
      justify-content: space-around;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      margin-bottom: 16px;

      .breakdown-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 12px;
        border-radius: 8px;
        transition: background 0.2s ease;

        &:hover {
          background: rgba(0, 0, 0, 0.05);
        }

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
        }

        &.high mat-icon {
          color: #f44336;
        }

        &.medium mat-icon {
          color: #ff9800;
        }

        &.low mat-icon {
          color: #4caf50;
        }

        .count {
          font-size: 24px;
          font-weight: 600;
          color: #333;
        }

        .label {
          font-size: 12px;
          color: #666;
        }

        .trend-indicator-mini {
          font-size: 12px;
          font-weight: 600;

          &.improving {
            color: #4caf50;
          }

          &.worsening {
            color: #f44336;
          }

          &.stable {
            color: #ff9800;
          }
        }
      }
    }

    .insights-section {
      padding: 16px;
      background: #e3f2fd;
      border-radius: 8px;
      border-left: 3px solid #1976d2;

      .insights-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        font-weight: 600;
        color: #1565c0;

        mat-icon {
          color: #1976d2;
        }
      }

      .insights-list {
        margin: 0;
        padding-left: 24px;
        font-size: 13px;
        color: #333;

        li {
          margin-bottom: 6px;

          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }

    @media (max-width: 768px) {
      .trend-summary {
        flex-direction: column;
        gap: 16px;
      }

      .trend-stats {
        width: 100%;
        justify-content: center;
      }

      .urgency-breakdown {
        flex-wrap: wrap;
        gap: 8px;

        .breakdown-item {
          flex: 1;
          min-width: 80px;
        }
      }
    }
  `],
})
export class CareGapTrendComponent implements OnInit, OnChanges {
  @Input() title = 'Care Gap Trends';
  @Input() subtitle?: string;
  @Input() trendData: CareGapTrendPoint[] = [];
  @Input() loading = false;
  @Input() showInsights = true;

  trendAnalysis: CareGapTrendAnalysis = {
    direction: 'stable',
    percentChange: 0,
    pointsChange: 0,
    period: 'last week',
    insights: [],
  };

  currentTotal = 0;

  ngOnInit(): void {
    this.analyzeTrend();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['trendData']) {
      this.analyzeTrend();
    }
  }

  /**
   * Analyze trend data and generate insights
   */
  private analyzeTrend(): void {
    if (this.trendData.length < 2) {
      this.trendAnalysis = {
        direction: 'stable',
        percentChange: 0,
        pointsChange: 0,
        period: 'N/A',
        insights: ['Not enough data to determine trend'],
      };
      this.currentTotal = this.trendData.length > 0 ? this.trendData[this.trendData.length - 1].totalGaps : 0;
      return;
    }

    // Sort by date (oldest first)
    const sorted = [...this.trendData].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

    const oldest = sorted[0];
    const newest = sorted[sorted.length - 1];

    this.currentTotal = newest.totalGaps;
    const pointsChange = newest.totalGaps - oldest.totalGaps;
    const percentChange = oldest.totalGaps > 0 ? (pointsChange / oldest.totalGaps) * 100 : 0;

    // Determine period
    const daysDiff = Math.floor((new Date(newest.date).getTime() - new Date(oldest.date).getTime()) / (1000 * 60 * 60 * 24));
    let period = 'last week';
    if (daysDiff > 25) period = 'last month';
    else if (daysDiff > 80) period = 'last quarter';

    // Determine direction
    let direction: 'improving' | 'stable' | 'worsening';
    if (Math.abs(percentChange) < 5 && Math.abs(pointsChange) < 3) {
      direction = 'stable';
    } else if (pointsChange < 0) {
      direction = 'improving'; // Fewer gaps is better
    } else {
      direction = 'worsening';
    }

    // Generate insights
    const insights: string[] = [];

    if (direction === 'improving') {
      insights.push(`Care gaps reduced by ${Math.abs(pointsChange)} (${Math.abs(percentChange).toFixed(1)}%) compared to ${period}`);
      const closedTotal = sorted.reduce((sum, p) => sum + p.closedGaps, 0);
      if (closedTotal > 0) {
        insights.push(`${closedTotal} care gaps were successfully closed in this period`);
      }
    } else if (direction === 'worsening') {
      insights.push(`Care gaps increased by ${pointsChange} (${percentChange.toFixed(1)}%) compared to ${period}`);
      const newTotal = sorted.reduce((sum, p) => sum + p.newGaps, 0);
      if (newTotal > 0) {
        insights.push(`${newTotal} new care gaps were identified - consider increasing outreach`);
      }
    } else {
      insights.push(`Care gaps have remained relatively stable over ${period}`);
    }

    // High urgency insight
    const highUrgency = newest.byUrgency.high;
    if (highUrgency > 0) {
      insights.push(`${highUrgency} high-urgency gap${highUrgency > 1 ? 's' : ''} require${highUrgency === 1 ? 's' : ''} immediate attention`);
    }

    this.trendAnalysis = {
      direction,
      percentChange,
      pointsChange,
      period,
      insights,
    };
  }

  getTrendClass(): string {
    return this.trendAnalysis.direction;
  }

  getTrendIcon(): string {
    switch (this.trendAnalysis.direction) {
      case 'improving':
        return 'trending_down';
      case 'worsening':
        return 'trending_up';
      default:
        return 'trending_flat';
    }
  }

  getTrendLabel(): string {
    switch (this.trendAnalysis.direction) {
      case 'improving':
        return 'Improving';
      case 'worsening':
        return 'Needs Attention';
      default:
        return 'Stable';
    }
  }

  formatPercentChange(value: number): string {
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(1)}%`;
  }

  getLastNPoints(n: number): CareGapTrendPoint[] {
    return this.trendData.slice(-n);
  }

  getBarHeight(point: CareGapTrendPoint): number {
    const max = Math.max(...this.trendData.map(p => p.totalGaps), 1);
    return (point.totalGaps / max) * 100;
  }

  getBarClass(point: CareGapTrendPoint): string {
    const highRatio = point.byUrgency.high / (point.totalGaps || 1);
    if (highRatio > 0.3) return 'high';
    if (highRatio > 0.1) return 'medium';
    return 'low';
  }

  getBarTooltip(point: CareGapTrendPoint): string {
    const date = new Date(point.date).toLocaleDateString();
    return `${date}: ${point.totalGaps} total gaps\nHigh: ${point.byUrgency.high}, Medium: ${point.byUrgency.medium}, Low: ${point.byUrgency.low}`;
  }

  formatBarLabel(date: Date): string {
    const d = new Date(date);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  }

  getUrgencyCount(urgency: 'high' | 'medium' | 'low'): number {
    if (this.trendData.length === 0) return 0;
    const latest = this.trendData[this.trendData.length - 1];
    return latest.byUrgency[urgency];
  }

  getUrgencyTrend(urgency: 'high' | 'medium' | 'low'): string {
    if (this.trendData.length < 2) return 'stable';
    const sorted = [...this.trendData].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    const oldest = sorted[0].byUrgency[urgency];
    const newest = sorted[sorted.length - 1].byUrgency[urgency];
    if (newest < oldest) return 'improving';
    if (newest > oldest) return 'worsening';
    return 'stable';
  }

  getUrgencyTrendIcon(urgency: 'high' | 'medium' | 'low'): string {
    const trend = this.getUrgencyTrend(urgency);
    switch (trend) {
      case 'improving':
        return '↓';
      case 'worsening':
        return '↑';
      default:
        return '→';
    }
  }
}
