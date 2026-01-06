import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import {
  MeasureAnalyticsService,
  MeasurePerformanceMetrics,
  PerformanceSummary,
  MeasureComparisonResult,
} from '../../../services/measure-analytics.service';
import { CustomMeasure } from '../../../services/custom-measure.service';

export interface PerformanceDashboardDialogData {
  measures: CustomMeasure[];
}

@Component({
  selector: 'app-performance-dashboard-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressBarModule,
    MatDividerModule,
  ],
  template: `
    <div class="dashboard-dialog">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon color="primary">analytics</mat-icon>
          <div>
            <h2>Measure Performance Dashboard</h2>
            <p class="subtitle">Compare and analyze measure performance metrics</p>
          </div>
        </div>
        <button mat-icon-button matTooltip="Close" (click)="close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <mat-tab-group animationDuration="200ms">
          <!-- Overview Tab -->
          <mat-tab label="Overview">
            <div class="tab-content">
              @if (loadingSummary) {
                <div class="loading-container">
                  <mat-spinner diameter="40"></mat-spinner>
                  <p>Loading performance summary...</p>
                </div>
              } @else if (summary) {
                <!-- Summary Cards -->
                <div class="summary-cards">
                  <div class="summary-card primary">
                    <div class="card-icon">
                      <mat-icon>assessment</mat-icon>
                    </div>
                    <div class="card-content">
                      <div class="card-value">{{ summary.totalMeasures }}</div>
                      <div class="card-label">Total Measures</div>
                    </div>
                  </div>

                  <div class="summary-card accent">
                    <div class="card-icon">
                      <mat-icon>play_circle</mat-icon>
                    </div>
                    <div class="card-content">
                      <div class="card-value">{{ summary.totalEvaluations }}</div>
                      <div class="card-label">Evaluations</div>
                    </div>
                  </div>

                  <div class="summary-card success">
                    <div class="card-icon">
                      <mat-icon>trending_up</mat-icon>
                    </div>
                    <div class="card-content">
                      <div class="card-value">{{ summary.averagePassRate | number:'1.1-1' }}%</div>
                      <div class="card-label">Avg Pass Rate</div>
                    </div>
                  </div>
                </div>

                <!-- Top Performers -->
                <div class="section">
                  <h3>
                    <mat-icon color="primary">emoji_events</mat-icon>
                    Top Performers
                  </h3>
                  <div class="performer-list">
                    @for (performer of summary.topPerformers; track performer.measureId; let i = $index) {
                      <div class="performer-item">
                        <div class="rank">{{ i + 1 }}</div>
                        <div class="performer-info">
                          <span class="name">{{ performer.measureName }}</span>
                          <mat-progress-bar
                            mode="determinate"
                            [value]="performer.passRate"
                            color="primary">
                          </mat-progress-bar>
                        </div>
                        <div class="pass-rate">{{ performer.passRate | number:'1.1-1' }}%</div>
                      </div>
                    }
                  </div>
                </div>

                <!-- Needs Attention -->
                <div class="section">
                  <h3>
                    <mat-icon color="warn">warning</mat-icon>
                    Needs Attention
                  </h3>
                  @if (summary.needsAttention.length > 0) {
                    <div class="attention-list">
                      @for (item of summary.needsAttention; track item.measureId) {
                        <div class="attention-item">
                          <div class="attention-info">
                            <span class="name">{{ item.measureName }}</span>
                            <span class="reason">{{ item.reason }}</span>
                          </div>
                          <div class="pass-rate low">{{ item.passRate | number:'1.1-1' }}%</div>
                        </div>
                      }
                    </div>
                  } @else {
                    <div class="no-issues">
                      <mat-icon>check_circle</mat-icon>
                      <span>All measures are performing well</span>
                    </div>
                  }
                </div>

                <!-- Trends Chart -->
                <div class="section">
                  <h3>
                    <mat-icon color="primary">show_chart</mat-icon>
                    Performance Trends
                  </h3>
                  <div class="trends-chart">
                    <div class="chart-bars">
                      @for (trend of summary.trendsOverview; track trend.period) {
                        <div class="chart-bar-group">
                          <div class="bar-container">
                            <div
                              class="bar"
                              [style.height.%]="trend.averagePassRate"
                              [matTooltip]="trend.averagePassRate | number:'1.1-1' + '%'">
                            </div>
                          </div>
                          <div class="bar-label">{{ trend.period }}</div>
                          <div class="bar-value">{{ trend.evaluations }}</div>
                        </div>
                      }
                    </div>
                    <div class="chart-legend">
                      <span class="legend-item">
                        <span class="legend-color bar"></span>
                        Pass Rate %
                      </span>
                      <span class="legend-item">
                        <span class="legend-color value"></span>
                        Evaluations
                      </span>
                    </div>
                  </div>
                </div>
              }
            </div>
          </mat-tab>

          <!-- Comparison Tab -->
          <mat-tab label="Compare">
            <div class="tab-content">
              <!-- Measure Selection -->
              <div class="comparison-selector">
                <mat-form-field appearance="outline">
                  <mat-label>First Measure</mat-label>
                  <mat-select [(ngModel)]="selectedMeasure1" (selectionChange)="onSelectionChange()">
                    @for (measure of data.measures; track measure.id) {
                      <mat-option [value]="measure" [disabled]="measure.id === selectedMeasure2?.id">
                        {{ measure.name }}
                      </mat-option>
                    }
                  </mat-select>
                </mat-form-field>

                <mat-icon class="vs-icon">compare_arrows</mat-icon>

                <mat-form-field appearance="outline">
                  <mat-label>Second Measure</mat-label>
                  <mat-select [(ngModel)]="selectedMeasure2" (selectionChange)="onSelectionChange()">
                    @for (measure of data.measures; track measure.id) {
                      <mat-option [value]="measure" [disabled]="measure.id === selectedMeasure1?.id">
                        {{ measure.name }}
                      </mat-option>
                    }
                  </mat-select>
                </mat-form-field>

                <button
                  mat-raised-button
                  color="primary"
                  [disabled]="!selectedMeasure1 || !selectedMeasure2 || loadingComparison"
                  (click)="compareMeasures()">
                  @if (loadingComparison) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>compare</mat-icon>
                  }
                  Compare
                </button>
              </div>

              <!-- Comparison Results -->
              @if (comparisonResult) {
                <div class="comparison-results">
                  <!-- Pass Rate Comparison -->
                  <div class="comparison-section">
                    <h4>Pass Rate Comparison</h4>
                    <div class="comparison-bars">
                      <div class="comparison-bar-group">
                        <div class="bar-label">{{ comparisonResult.measure1.measureName }}</div>
                        <div class="bar-wrapper">
                          <div
                            class="bar measure1"
                            [style.width.%]="comparisonResult.measure1.metrics.passRate">
                          </div>
                          <span class="bar-value">{{ comparisonResult.measure1.metrics.passRate | number:'1.1-1' }}%</span>
                        </div>
                      </div>
                      <div class="comparison-bar-group">
                        <div class="bar-label">{{ comparisonResult.measure2.measureName }}</div>
                        <div class="bar-wrapper">
                          <div
                            class="bar measure2"
                            [style.width.%]="comparisonResult.measure2.metrics.passRate">
                          </div>
                          <span class="bar-value">{{ comparisonResult.measure2.metrics.passRate | number:'1.1-1' }}%</span>
                        </div>
                      </div>
                    </div>
                    <div class="diff-indicator" [class.positive]="comparisonResult.comparison.passRateDiff > 0" [class.negative]="comparisonResult.comparison.passRateDiff < 0">
                      <mat-icon>{{ comparisonResult.comparison.passRateDiff >= 0 ? 'arrow_upward' : 'arrow_downward' }}</mat-icon>
                      <span>{{ comparisonResult.comparison.passRateDiff | number:'1.1-1' }}% difference</span>
                    </div>
                  </div>

                  <!-- Volume Comparison -->
                  <div class="comparison-section">
                    <h4>Patient Volume</h4>
                    <div class="volume-comparison">
                      <div class="volume-item">
                        <span class="volume-label">{{ comparisonResult.measure1.measureName }}</span>
                        <span class="volume-value">{{ comparisonResult.measure1.metrics.totalPatients | number }}</span>
                      </div>
                      <div class="volume-item">
                        <span class="volume-label">{{ comparisonResult.measure2.measureName }}</span>
                        <span class="volume-value">{{ comparisonResult.measure2.metrics.totalPatients | number }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- Execution Time Comparison -->
                  <div class="comparison-section">
                    <h4>Average Execution Time</h4>
                    <div class="volume-comparison">
                      <div class="volume-item">
                        <span class="volume-label">{{ comparisonResult.measure1.measureName }}</span>
                        <span class="volume-value">{{ comparisonResult.measure1.metrics.averageExecutionTimeMs }}ms</span>
                      </div>
                      <div class="volume-item">
                        <span class="volume-label">{{ comparisonResult.measure2.measureName }}</span>
                        <span class="volume-value">{{ comparisonResult.measure2.metrics.averageExecutionTimeMs }}ms</span>
                      </div>
                    </div>
                  </div>

                  <!-- Insights -->
                  <div class="insights-section">
                    <h4>
                      <mat-icon>lightbulb</mat-icon>
                      Key Insights
                    </h4>
                    @if (comparisonResult.comparison.improvementAreas.length > 0) {
                      <div class="insights-group positive">
                        <h5>Strengths</h5>
                        @for (item of comparisonResult.comparison.improvementAreas; track item) {
                          <div class="insight-item">
                            <mat-icon>check_circle</mat-icon>
                            <span>{{ item }}</span>
                          </div>
                        }
                      </div>
                    }
                    @if (comparisonResult.comparison.concerns.length > 0) {
                      <div class="insights-group negative">
                        <h5>Areas for Improvement</h5>
                        @for (item of comparisonResult.comparison.concerns; track item) {
                          <div class="insight-item">
                            <mat-icon>warning</mat-icon>
                            <span>{{ item }}</span>
                          </div>
                        }
                      </div>
                    }
                    @if (comparisonResult.comparison.improvementAreas.length === 0 && comparisonResult.comparison.concerns.length === 0) {
                      <div class="no-insights">
                        <mat-icon>balance</mat-icon>
                        <span>Both measures are performing similarly</span>
                      </div>
                    }
                  </div>
                </div>
              } @else {
                <div class="no-comparison">
                  <mat-icon>compare</mat-icon>
                  <p>Select two measures above to compare their performance</p>
                </div>
              }
            </div>
          </mat-tab>

          <!-- Individual Details Tab -->
          <mat-tab label="Details">
            <div class="tab-content">
              <!-- Measure Selection -->
              <div class="detail-selector">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Select Measure</mat-label>
                  <mat-select [(ngModel)]="selectedDetailMeasure" (selectionChange)="loadMeasureDetails()">
                    @for (measure of data.measures; track measure.id) {
                      <mat-option [value]="measure">
                        {{ measure.name }}
                      </mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              </div>

              @if (loadingDetails) {
                <div class="loading-container small">
                  <mat-spinner diameter="32"></mat-spinner>
                </div>
              } @else if (selectedMetrics) {
                <!-- Detailed Metrics -->
                <div class="detail-content">
                  <!-- Header -->
                  <div class="detail-header">
                    <h3>{{ selectedMetrics.measureName }}</h3>
                    <mat-chip size="small">v{{ selectedMetrics.measureVersion }}</mat-chip>
                    <mat-chip [class]="'status-chip ' + selectedMetrics.status.toLowerCase()" size="small">
                      {{ selectedMetrics.status }}
                    </mat-chip>
                  </div>

                  <!-- Key Metrics -->
                  <div class="metrics-grid">
                    <div class="metric-card">
                      <div class="metric-value success">{{ selectedMetrics.metrics.passRate | number:'1.1-1' }}%</div>
                      <div class="metric-label">Pass Rate</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-value">{{ selectedMetrics.metrics.totalPatients | number }}</div>
                      <div class="metric-label">Total Patients</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-value">{{ selectedMetrics.metrics.totalEvaluations }}</div>
                      <div class="metric-label">Evaluations</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-value">{{ selectedMetrics.metrics.averageExecutionTimeMs }}ms</div>
                      <div class="metric-label">Avg Execution</div>
                    </div>
                  </div>

                  <!-- Population Breakdown -->
                  <div class="section">
                    <h4>Population Criteria</h4>
                    <div class="population-flow">
                      <div class="population-item">
                        <div class="pop-value">{{ selectedMetrics.populations.initialPopulation | number }}</div>
                        <div class="pop-label">Initial Population</div>
                      </div>
                      <mat-icon>arrow_forward</mat-icon>
                      <div class="population-item">
                        <div class="pop-value">{{ selectedMetrics.populations.denominator | number }}</div>
                        <div class="pop-label">Denominator</div>
                      </div>
                      <mat-icon>arrow_forward</mat-icon>
                      <div class="population-item highlight">
                        <div class="pop-value">{{ selectedMetrics.populations.numerator | number }}</div>
                        <div class="pop-label">Numerator</div>
                      </div>
                    </div>
                    <div class="exclusions">
                      <span>Exclusions: {{ selectedMetrics.populations.denominatorExclusions | number }}</span>
                      <span>Exceptions: {{ selectedMetrics.populations.denominatorExceptions | number }}</span>
                    </div>
                  </div>

                  <!-- Breakdown by Demographics -->
                  <div class="section">
                    <h4>Performance by Age Group</h4>
                    <div class="breakdown-bars">
                      @for (group of selectedMetrics.breakdown.byAgeGroup; track group.label) {
                        <div class="breakdown-item">
                          <div class="breakdown-label">{{ group.label }}</div>
                          <div class="breakdown-bar-wrapper">
                            <div class="breakdown-bar" [style.width.%]="group.passRate"></div>
                          </div>
                          <div class="breakdown-value">{{ group.passRate | number:'1.1-1' }}%</div>
                          <div class="breakdown-count">({{ group.count | number }})</div>
                        </div>
                      }
                    </div>
                  </div>

                  <div class="section">
                    <h4>Performance by Payer</h4>
                    <div class="breakdown-bars">
                      @for (payer of selectedMetrics.breakdown.byPayer; track payer.label) {
                        <div class="breakdown-item">
                          <div class="breakdown-label">{{ payer.label }}</div>
                          <div class="breakdown-bar-wrapper">
                            <div class="breakdown-bar" [style.width.%]="payer.passRate"></div>
                          </div>
                          <div class="breakdown-value">{{ payer.passRate | number:'1.1-1' }}%</div>
                          <div class="breakdown-count">({{ payer.count | number }})</div>
                        </div>
                      }
                    </div>
                  </div>
                </div>
              } @else {
                <div class="no-selection">
                  <mat-icon>analytics</mat-icon>
                  <p>Select a measure to view detailed performance metrics</p>
                </div>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-dialog-content>

      <!-- Footer -->
      <mat-dialog-actions align="end">
        <button mat-button (click)="exportReport()">
          <mat-icon>download</mat-icon>
          Export Report
        </button>
        <button mat-button (click)="close()">
          <mat-icon>close</mat-icon>
          Close
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dashboard-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
      max-height: 85vh;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
        }

        .subtitle {
          margin: 4px 0 0 0;
          color: #666;
          font-size: 13px;
        }
      }
    }

    .dialog-content {
      flex: 1;
      overflow: hidden;
      padding: 0;
    }

    .tab-content {
      padding: 24px;
      overflow-y: auto;
      max-height: calc(85vh - 180px);
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;

      &.small {
        padding: 24px;
      }

      p {
        margin-top: 16px;
        color: #666;
      }
    }

    /* Summary Cards */
    .summary-cards {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;

      .summary-card {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 20px;
        border-radius: 12px;
        background-color: #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

        .card-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;

          mat-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;
            color: #fff;
          }
        }

        .card-content {
          .card-value {
            font-size: 28px;
            font-weight: 600;
          }

          .card-label {
            font-size: 12px;
            color: #666;
            text-transform: uppercase;
          }
        }

        &.primary .card-icon {
          background-color: #1976d2;
        }

        &.accent .card-icon {
          background-color: #ff9800;
        }

        &.success .card-icon {
          background-color: #2e7d32;
        }
      }
    }

    /* Sections */
    .section {
      margin-bottom: 24px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 16px 0;
        font-size: 16px;
        font-weight: 600;
      }

      h4 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 12px 0;
        font-size: 14px;
        font-weight: 600;
      }
    }

    /* Performer List */
    .performer-list {
      .performer-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        background-color: #fafafa;
        border-radius: 8px;
        margin-bottom: 8px;

        .rank {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background-color: #1976d2;
          color: #fff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 600;
          font-size: 14px;
        }

        .performer-info {
          flex: 1;

          .name {
            display: block;
            font-weight: 500;
            margin-bottom: 6px;
          }

          mat-progress-bar {
            height: 8px;
            border-radius: 4px;
          }
        }

        .pass-rate {
          font-weight: 600;
          color: #2e7d32;
        }
      }
    }

    /* Attention List */
    .attention-list {
      .attention-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px;
        background-color: #fff3e0;
        border-radius: 8px;
        margin-bottom: 8px;

        .attention-info {
          .name {
            display: block;
            font-weight: 500;
          }

          .reason {
            font-size: 12px;
            color: #666;
          }
        }

        .pass-rate.low {
          color: #c62828;
          font-weight: 600;
        }
      }
    }

    .no-issues {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      background-color: #e8f5e9;
      border-radius: 8px;
      color: #2e7d32;
    }

    /* Trends Chart */
    .trends-chart {
      background-color: #fafafa;
      border-radius: 12px;
      padding: 20px;

      .chart-bars {
        display: flex;
        justify-content: space-around;
        align-items: flex-end;
        height: 150px;
        margin-bottom: 12px;

        .chart-bar-group {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 60px;

          .bar-container {
            width: 40px;
            height: 120px;
            background-color: #e0e0e0;
            border-radius: 4px 4px 0 0;
            display: flex;
            align-items: flex-end;

            .bar {
              width: 100%;
              background: linear-gradient(180deg, #1976d2, #42a5f5);
              border-radius: 4px 4px 0 0;
              transition: height 0.3s ease;
            }
          }

          .bar-label {
            font-size: 11px;
            color: #666;
            margin-top: 8px;
          }

          .bar-value {
            font-size: 10px;
            color: #999;
          }
        }
      }

      .chart-legend {
        display: flex;
        justify-content: center;
        gap: 24px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #666;

          .legend-color {
            width: 12px;
            height: 12px;
            border-radius: 2px;

            &.bar {
              background: linear-gradient(180deg, #1976d2, #42a5f5);
            }

            &.value {
              background-color: #999;
            }
          }
        }
      }
    }

    /* Comparison Tab */
    .comparison-selector {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 24px;

      mat-form-field {
        flex: 1;
      }

      .vs-icon {
        color: #666;
      }
    }

    .comparison-results {
      .comparison-section {
        margin-bottom: 24px;
        padding: 16px;
        background-color: #fafafa;
        border-radius: 12px;

        h4 {
          margin: 0 0 16px 0;
          font-size: 14px;
          font-weight: 600;
        }
      }

      .comparison-bars {
        .comparison-bar-group {
          margin-bottom: 12px;

          .bar-label {
            font-size: 13px;
            margin-bottom: 6px;
          }

          .bar-wrapper {
            display: flex;
            align-items: center;
            gap: 8px;
            height: 24px;
            background-color: #e0e0e0;
            border-radius: 4px;

            .bar {
              height: 100%;
              border-radius: 4px;
              transition: width 0.5s ease;

              &.measure1 {
                background: linear-gradient(90deg, #1976d2, #42a5f5);
              }

              &.measure2 {
                background: linear-gradient(90deg, #ff9800, #ffb74d);
              }
            }

            .bar-value {
              font-weight: 500;
              font-size: 13px;
              padding-right: 8px;
            }
          }
        }
      }

      .diff-indicator {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 12px;
        font-size: 13px;
        font-weight: 500;

        &.positive {
          color: #2e7d32;
        }

        &.negative {
          color: #c62828;
        }
      }

      .volume-comparison {
        display: flex;
        gap: 24px;

        .volume-item {
          flex: 1;
          text-align: center;
          padding: 12px;
          background-color: #fff;
          border-radius: 8px;

          .volume-label {
            display: block;
            font-size: 12px;
            color: #666;
            margin-bottom: 4px;
          }

          .volume-value {
            font-size: 20px;
            font-weight: 600;
          }
        }
      }
    }

    .insights-section {
      .insights-group {
        margin-bottom: 16px;
        padding: 12px;
        border-radius: 8px;

        &.positive {
          background-color: #e8f5e9;

          h5 { color: #2e7d32; }
          mat-icon { color: #2e7d32; }
        }

        &.negative {
          background-color: #fff3e0;

          h5 { color: #e65100; }
          mat-icon { color: #e65100; }
        }

        h5 {
          margin: 0 0 8px 0;
          font-size: 12px;
          text-transform: uppercase;
        }

        .insight-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 0;
          font-size: 13px;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }
        }
      }
    }

    .no-comparison, .no-selection {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #999;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        margin-bottom: 16px;
      }

      p {
        margin: 0;
        text-align: center;
      }
    }

    .no-insights {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      background-color: #e3f2fd;
      border-radius: 8px;
      color: #1565c0;
    }

    /* Details Tab */
    .detail-selector {
      margin-bottom: 24px;

      .full-width {
        width: 100%;
      }
    }

    .detail-content {
      .detail-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 24px;

        h3 {
          margin: 0;
          font-size: 18px;
        }
      }

      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
        margin-bottom: 24px;

        .metric-card {
          text-align: center;
          padding: 16px;
          background-color: #fafafa;
          border-radius: 12px;

          .metric-value {
            font-size: 24px;
            font-weight: 600;

            &.success {
              color: #2e7d32;
            }
          }

          .metric-label {
            font-size: 12px;
            color: #666;
            margin-top: 4px;
          }
        }
      }

      .population-flow {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 16px;
        margin-bottom: 12px;

        .population-item {
          text-align: center;
          padding: 16px 24px;
          background-color: #e3f2fd;
          border-radius: 8px;

          &.highlight {
            background-color: #e8f5e9;

            .pop-value {
              color: #2e7d32;
            }
          }

          .pop-value {
            font-size: 20px;
            font-weight: 600;
          }

          .pop-label {
            font-size: 11px;
            color: #666;
            margin-top: 4px;
          }
        }

        mat-icon {
          color: #bdbdbd;
        }
      }

      .exclusions {
        display: flex;
        justify-content: center;
        gap: 24px;
        font-size: 12px;
        color: #666;
      }

      .breakdown-bars {
        .breakdown-item {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 12px;

          .breakdown-label {
            width: 80px;
            font-size: 13px;
          }

          .breakdown-bar-wrapper {
            flex: 1;
            height: 20px;
            background-color: #e0e0e0;
            border-radius: 4px;

            .breakdown-bar {
              height: 100%;
              background: linear-gradient(90deg, #1976d2, #42a5f5);
              border-radius: 4px;
              transition: width 0.3s ease;
            }
          }

          .breakdown-value {
            width: 50px;
            font-weight: 500;
            font-size: 13px;
            text-align: right;
          }

          .breakdown-count {
            width: 60px;
            font-size: 11px;
            color: #999;
          }
        }
      }
    }

    .status-chip {
      font-size: 11px;

      &.draft {
        background-color: #fff3e0;
        color: #e65100;
      }

      &.published {
        background-color: #e8f5e9;
        color: #2e7d32;
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
    }
  `],
})
export class PerformanceDashboardDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Loading states
  loadingSummary = false;
  loadingComparison = false;
  loadingDetails = false;

  // Summary data
  summary: PerformanceSummary | null = null;

  // Comparison data
  selectedMeasure1: CustomMeasure | null = null;
  selectedMeasure2: CustomMeasure | null = null;
  comparisonResult: MeasureComparisonResult | null = null;

  // Details data
  selectedDetailMeasure: CustomMeasure | null = null;
  selectedMetrics: MeasurePerformanceMetrics | null = null;

  constructor(
    private dialogRef: MatDialogRef<PerformanceDashboardDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PerformanceDashboardDialogData,
    private analyticsService: MeasureAnalyticsService
  ) {}

  ngOnInit(): void {
    this.loadSummary();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load performance summary
   */
  private loadSummary(): void {
    this.loadingSummary = true;

    this.analyticsService
      .getPerformanceSummary()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loadingSummary = false))
      )
      .subscribe((summary) => {
        this.summary = summary;
      });
  }

  /**
   * Handle selection change for comparison
   */
  onSelectionChange(): void {
    this.comparisonResult = null;
  }

  /**
   * Compare two selected measures
   */
  compareMeasures(): void {
    if (!this.selectedMeasure1 || !this.selectedMeasure2) return;

    this.loadingComparison = true;
    this.comparisonResult = null;

    this.analyticsService
      .compareMeasures(this.selectedMeasure1.id, this.selectedMeasure2.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loadingComparison = false))
      )
      .subscribe((result) => {
        // Update names from selected measures
        result.measure1.measureName = this.selectedMeasure1!.name;
        result.measure2.measureName = this.selectedMeasure2!.name;
        this.comparisonResult = result;
      });
  }

  /**
   * Load detailed metrics for selected measure
   */
  loadMeasureDetails(): void {
    if (!this.selectedDetailMeasure) return;

    this.loadingDetails = true;
    this.selectedMetrics = null;

    this.analyticsService
      .getMeasurePerformance(this.selectedDetailMeasure.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loadingDetails = false))
      )
      .subscribe((metrics) => {
        metrics.measureName = this.selectedDetailMeasure!.name;
        metrics.measureVersion = this.selectedDetailMeasure!.version || '1.0.0';
        metrics.measureCategory = this.selectedDetailMeasure!.category || 'CUSTOM';
        metrics.status = this.selectedDetailMeasure!.status || 'DRAFT';
        this.selectedMetrics = metrics;
      });
  }

  /**
   * Export performance report
   */
  exportReport(): void {
    // Build report data
    const report: any = {
      generatedAt: new Date().toISOString(),
      summary: this.summary,
    };

    if (this.comparisonResult) {
      report.comparison = this.comparisonResult;
    }

    if (this.selectedMetrics) {
      report.selectedMeasure = this.selectedMetrics;
    }

    // Convert to JSON and download
    const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `measure-performance-report-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Close dialog
   */
  close(): void {
    this.dialogRef.close();
  }
}
