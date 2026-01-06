import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import {
  PopulationInsightsService,
  PopulationInsight,
  PopulationSummary,
  PredictedCareGap,
  InsightsResponse,
} from '../../services/population-insights.service';
import { ToastService } from '../../services/toast.service';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';

@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTabsModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule,
    MatExpansionModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDialogModule,
    LoadingOverlayComponent,
    LoadingButtonComponent,
  ],
  template: `
    <div class="page">
      <!-- Page Header -->
      <div class="page-header">
        <div class="title">
          <mat-icon color="primary">insights</mat-icon>
          <div>
            <h1>Population Health Insights</h1>
            <p>AI-powered insights and recommendations for your patient panel</p>
          </div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button color="primary" (click)="refreshInsights()">
            <mat-icon>refresh</mat-icon>
            Refresh
          </button>
        </div>
      </div>

      <!-- Loading Overlay -->
      <app-loading-overlay [isLoading]="loading" message="Analyzing population health data..."></app-loading-overlay>

      <!-- Summary Section -->
      <div class="summary-section">
        <!-- Population Overview Cards -->
        <div class="summary-cards">
          <mat-card class="summary-card patients">
            <div class="card-icon">
              <mat-icon>groups</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ summary?.totalPatients || 0 }}</div>
              <div class="card-label">Total Patients</div>
            </div>
          </mat-card>

          <mat-card class="summary-card critical">
            <div class="card-icon">
              <mat-icon>warning</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ summary?.riskDistribution?.critical || 0 }}</div>
              <div class="card-label">Critical Risk</div>
            </div>
          </mat-card>

          <mat-card class="summary-card high">
            <div class="card-icon">
              <mat-icon>trending_up</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ summary?.riskDistribution?.high || 0 }}</div>
              <div class="card-label">High Risk</div>
            </div>
          </mat-card>

          <mat-card class="summary-card insights">
            <div class="card-icon">
              <mat-icon>lightbulb</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ insightsResponse?.insights?.length || 0 }}</div>
              <div class="card-label">Active Insights</div>
            </div>
          </mat-card>
        </div>

        <!-- Risk Pyramid & Care Gap Distribution -->
        <div class="charts-row">
          <!-- Risk Pyramid -->
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Risk Stratification</mat-card-title>
              <mat-card-subtitle>Patient distribution by risk level</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="risk-pyramid">
                <div class="pyramid-level critical" [style.width.%]="getRiskWidth('critical')">
                  <span class="level-label">Critical</span>
                  <span class="level-count">{{ summary?.riskDistribution?.critical || 0 }}</span>
                </div>
                <div class="pyramid-level high" [style.width.%]="getRiskWidth('high')">
                  <span class="level-label">High</span>
                  <span class="level-count">{{ summary?.riskDistribution?.high || 0 }}</span>
                </div>
                <div class="pyramid-level medium" [style.width.%]="getRiskWidth('medium')">
                  <span class="level-label">Medium</span>
                  <span class="level-count">{{ summary?.riskDistribution?.medium || 0 }}</span>
                </div>
                <div class="pyramid-level low" [style.width.%]="getRiskWidth('low')">
                  <span class="level-label">Low</span>
                  <span class="level-count">{{ summary?.riskDistribution?.low || 0 }}</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Care Gap Distribution -->
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Care Gap Distribution</mat-card-title>
              <mat-card-subtitle>Open gaps by category</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="gap-distribution">
                @for (gap of summary?.careGapDistribution || []; track gap.category) {
                  <div class="gap-bar-row">
                    <div class="gap-label">{{ gap.category }}</div>
                    <div class="gap-bar-container">
                      <div class="gap-bar" [style.width.%]="gap.percentage * 2"></div>
                      <span class="gap-count">{{ gap.count }}</span>
                    </div>
                  </div>
                }
              </div>
            </mat-card-content>
          </mat-card>
        </div>
      </div>

      <!-- Main Content Tabs -->
      <mat-card class="insights-card">
        <mat-tab-group animationDuration="200ms">
          <!-- Active Insights Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">lightbulb</mat-icon>
              Active Insights
              @if (insightsResponse?.summary?.critical) {
                <span class="badge critical">{{ insightsResponse?.summary?.critical }}</span>
              }
            </ng-template>

            <div class="tab-content">
              <!-- Filter Bar -->
              <div class="filter-bar">
                <mat-chip-listbox [(ngModel)]="selectedImpacts" multiple (change)="filterInsights()">
                  <mat-chip-option value="CRITICAL" color="warn">Critical</mat-chip-option>
                  <mat-chip-option value="HIGH" color="primary">High</mat-chip-option>
                  <mat-chip-option value="MEDIUM" color="accent">Medium</mat-chip-option>
                  <mat-chip-option value="LOW">Low</mat-chip-option>
                </mat-chip-listbox>
              </div>

              <!-- Insights List -->
              <div class="insights-list">
                @if (filteredInsights.length === 0) {
                  <div class="no-data">
                    <mat-icon>check_circle</mat-icon>
                    <p>No insights match your filter criteria</p>
                  </div>
                } @else {
                  @for (insight of filteredInsights; track insight.id) {
                    <mat-expansion-panel class="insight-panel" [class]="insight.impact.toLowerCase()">
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          <div class="insight-header">
                            <mat-icon [class]="'impact-icon ' + insight.impact.toLowerCase()">
                              {{ getInsightIcon(insight.type) }}
                            </mat-icon>
                            <div class="insight-title-content">
                              <span class="insight-title">{{ insight.title }}</span>
                              <span class="insight-category">{{ insight.category }}</span>
                            </div>
                          </div>
                        </mat-panel-title>
                        <mat-panel-description>
                          <mat-chip [class]="insight.impact.toLowerCase()">{{ insight.impact }}</mat-chip>
                          <span class="affected-count">{{ insight.affectedPatients }} patients</span>
                        </mat-panel-description>
                      </mat-expansion-panel-header>

                      <div class="insight-body">
                        <p class="insight-description">{{ insight.description }}</p>

                        <!-- Metrics -->
                        @if (insight.metrics) {
                          <div class="metrics-row">
                            @if (insight.metrics.currentValue !== undefined) {
                              <div class="metric">
                                <span class="metric-value">{{ insight.metrics.currentValue }}%</span>
                                <span class="metric-label">Current</span>
                              </div>
                            }
                            @if (insight.metrics.targetValue !== undefined) {
                              <div class="metric">
                                <span class="metric-value">{{ insight.metrics.targetValue }}%</span>
                                <span class="metric-label">Target</span>
                              </div>
                            }
                            @if (insight.metrics.changePercent !== undefined) {
                              <div class="metric" [class.negative]="insight.metrics.changePercent < 0">
                                <span class="metric-value">
                                  {{ insight.metrics.changePercent > 0 ? '+' : '' }}{{ insight.metrics.changePercent }}%
                                </span>
                                <span class="metric-label">Change</span>
                              </div>
                            }
                            @if (insight.metrics.potentialImprovement !== undefined) {
                              <div class="metric positive">
                                <span class="metric-value">+{{ insight.metrics.potentialImprovement }}%</span>
                                <span class="metric-label">Potential</span>
                              </div>
                            }
                          </div>
                        }

                        <!-- Risk Factors -->
                        @if (insight.riskFactors && insight.riskFactors.length > 0) {
                          <div class="risk-factors">
                            <h4>Contributing Factors</h4>
                            @for (factor of insight.riskFactors; track factor.factor) {
                              <div class="factor-row">
                                <span class="factor-name">{{ factor.factor }}</span>
                                <div class="factor-bar-container">
                                  <div class="factor-bar" [style.width.%]="factor.contribution"></div>
                                </div>
                                <span class="factor-value">{{ factor.contribution }}%</span>
                              </div>
                            }
                          </div>
                        }

                        <!-- Suggested Actions -->
                        <div class="suggested-actions">
                          <h4>Recommended Actions</h4>
                          <div class="actions-grid">
                            @for (action of insight.suggestedActions; track action.id) {
                              <button mat-stroked-button color="primary" class="action-button"
                                      (click)="executeAction(insight, action)">
                                <mat-icon>{{ getActionIcon(action.type) }}</mat-icon>
                                <span class="action-text">
                                  <span class="action-label">{{ action.label }}</span>
                                  <span class="action-impact">+{{ action.estimatedImpact }}% impact</span>
                                </span>
                              </button>
                            }
                          </div>
                        </div>

                        <!-- Dismiss Button -->
                        <div class="insight-footer">
                          <button mat-button color="warn" (click)="dismissInsight(insight)">
                            <mat-icon>visibility_off</mat-icon>
                            Dismiss
                          </button>
                          <span class="insight-date">Generated {{ formatDate(insight.createdAt) }}</span>
                        </div>
                      </div>
                    </mat-expansion-panel>
                  }
                }
              </div>
            </div>
          </mat-tab>

          <!-- Predicted Gaps Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">trending_up</mat-icon>
              Predicted Gaps
              @if (predictedGaps.length > 0) {
                <span class="badge">{{ predictedGaps.length }}</span>
              }
            </ng-template>

            <div class="tab-content">
              <div class="predicted-gaps-intro">
                <mat-icon color="primary">psychology</mat-icon>
                <p>
                  These patients are predicted to develop care gaps based on historical patterns and behavior analysis.
                  Proactive intervention can prevent these gaps from occurring.
                </p>
              </div>

              @if (loadingPredictions) {
                <div class="loading-container">
                  <mat-spinner diameter="40"></mat-spinner>
                  <p>Analyzing patient patterns...</p>
                </div>
              } @else if (predictedGaps.length === 0) {
                <div class="no-data">
                  <mat-icon>check_circle</mat-icon>
                  <p>No predicted gaps at this time</p>
                </div>
              } @else {
                <div class="predicted-list">
                  @for (gap of predictedGaps; track gap.id) {
                    <mat-card class="predicted-card">
                      <div class="predicted-header">
                        <div class="patient-info">
                          <span class="patient-name">{{ gap.patientName }}</span>
                          <span class="patient-mrn">{{ gap.patientMRN }}</span>
                        </div>
                        <div class="risk-indicator" [class]="getRiskClass(gap.riskScore)">
                          <span class="risk-score">{{ gap.riskScore }}</span>
                          <span class="risk-label">Risk Score</span>
                        </div>
                      </div>

                      <div class="predicted-body">
                        <div class="measure-info">
                          <mat-icon color="primary">assessment</mat-icon>
                          <span>{{ gap.measureName }}</span>
                        </div>
                        <div class="prediction-info">
                          <div class="prediction-date">
                            <mat-icon>event</mat-icon>
                            <span>Predicted gap in <strong>{{ gap.daysUntilGap }} days</strong></span>
                          </div>
                          <div class="confidence">
                            <mat-icon>psychology</mat-icon>
                            <span>{{ (gap.confidence * 100).toFixed(0) }}% confidence</span>
                          </div>
                        </div>

                        <!-- Risk Factors -->
                        <div class="predicted-factors">
                          @for (factor of gap.riskFactors.slice(0, 2); track factor.factor) {
                            <div class="factor-chip">
                              <mat-icon>info</mat-icon>
                              {{ factor.factor }}
                            </div>
                          }
                        </div>
                      </div>

                      <div class="predicted-footer">
                        <span class="recommendation">{{ gap.recommendedIntervention }}</span>
                        <button mat-flat-button color="primary" (click)="scheduleIntervention(gap)">
                          <mat-icon>calendar_today</mat-icon>
                          Schedule
                        </button>
                      </div>
                    </mat-card>
                  }
                </div>
              }
            </div>
          </mat-tab>

          <!-- Quality Scores Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">speed</mat-icon>
              Quality Scores
            </ng-template>

            <div class="tab-content">
              @if (!summary?.qualityScores) {
                <div class="loading-container">
                  <mat-spinner diameter="40"></mat-spinner>
                  <p>Loading quality scores...</p>
                </div>
              } @else {
                <div class="quality-scores">
                  @for (score of summary.qualityScores; track score.measureId) {
                    <mat-card class="score-card" [class.below-target]="score.score < score.target">
                      <div class="score-header">
                        <span class="measure-name">{{ score.measureName }}</span>
                        <mat-icon [class]="'trend-icon ' + score.trend" [matTooltip]="getTrendTooltip(score.trend)">
                          {{ getTrendIcon(score.trend) }}
                        </mat-icon>
                      </div>
                      <div class="score-body">
                        <div class="score-gauge">
                          <div class="gauge-fill" [style.width.%]="score.score"></div>
                          <div class="target-marker" [style.left.%]="score.target"></div>
                        </div>
                        <div class="score-values">
                          <span class="current-score">{{ score.score }}%</span>
                          <span class="target-score">Target: {{ score.target }}%</span>
                        </div>
                      </div>
                      <div class="score-footer">
                        @if (score.score >= score.target) {
                          <mat-chip color="primary" selected>Meeting Target</mat-chip>
                        } @else {
                          <mat-chip color="warn" selected>{{ score.target - score.score }}% below target</mat-chip>
                        }
                      </div>
                    </mat-card>
                  }
                </div>
              }
            </div>
          </mat-tab>

          <!-- Dismissed Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">visibility_off</mat-icon>
              Dismissed
              @if (insightsResponse?.summary?.dismissed) {
                <span class="badge muted">{{ insightsResponse?.summary?.dismissed }}</span>
              }
            </ng-template>

            <div class="tab-content">
              <div class="dismissed-intro">
                <p>Dismissed insights are hidden from the active view but can be restored if needed.</p>
              </div>
              <!-- Dismissed insights would be shown here -->
              <div class="no-data">
                <mat-icon>check_circle</mat-icon>
                <p>No dismissed insights</p>
              </div>
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
    </div>
  `,
  styles: [`
    .page {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 1600px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }

    .title {
      display: flex;
      align-items: center;
      gap: 12px;

      mat-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
      }

      h1 {
        margin: 0;
        font-size: 28px;
        font-weight: 500;
        color: #333;
      }

      p {
        margin: 4px 0 0 0;
        color: #666;
        font-size: 14px;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }

    /* Summary Section */
    .summary-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }

    .summary-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;

      .card-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: white;
        }
      }

      &.patients .card-icon { background: linear-gradient(135deg, #1976d2, #42a5f5); }
      &.critical .card-icon { background: linear-gradient(135deg, #d32f2f, #ef5350); }
      &.high .card-icon { background: linear-gradient(135deg, #f57c00, #ffb74d); }
      &.insights .card-icon { background: linear-gradient(135deg, #7b1fa2, #ba68c8); }

      .card-value {
        font-size: 32px;
        font-weight: 600;
        color: #333;
      }

      .card-label {
        font-size: 14px;
        color: #666;
      }
    }

    .charts-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
      gap: 16px;
    }

    .chart-card {
      height: 100%;
    }

    /* Risk Pyramid */
    .risk-pyramid {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 16px 0;
    }

    .pyramid-level {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 20px;
      border-radius: 8px;
      min-width: 120px;
      transition: width 0.3s ease;

      &.critical { background: linear-gradient(90deg, #ffebee, #ffcdd2); border-left: 4px solid #d32f2f; }
      &.high { background: linear-gradient(90deg, #fff3e0, #ffe0b2); border-left: 4px solid #f57c00; }
      &.medium { background: linear-gradient(90deg, #fff8e1, #ffecb3); border-left: 4px solid #ffc107; }
      &.low { background: linear-gradient(90deg, #e8f5e9, #c8e6c9); border-left: 4px solid #4caf50; }

      .level-label {
        font-weight: 500;
        font-size: 14px;
      }

      .level-count {
        font-weight: 600;
        font-size: 18px;
      }
    }

    /* Care Gap Distribution */
    .gap-distribution {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 8px 0;
    }

    .gap-bar-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .gap-label {
        width: 140px;
        font-size: 13px;
        color: #555;
        text-align: right;
      }

      .gap-bar-container {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .gap-bar {
        height: 24px;
        background: linear-gradient(90deg, #1976d2, #42a5f5);
        border-radius: 4px;
        min-width: 4px;
        transition: width 0.3s ease;
      }

      .gap-count {
        font-weight: 600;
        font-size: 14px;
        color: #333;
        min-width: 30px;
      }
    }

    /* Insights Card */
    .insights-card {
      .tab-icon {
        margin-right: 8px;
      }

      .badge {
        margin-left: 8px;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
        background: #e3f2fd;
        color: #1976d2;

        &.critical {
          background: #ffebee;
          color: #d32f2f;
        }

        &.muted {
          background: #f5f5f5;
          color: #757575;
        }
      }
    }

    .tab-content {
      padding: 24px;
    }

    .filter-bar {
      margin-bottom: 20px;
    }

    /* Insight Panels */
    .insights-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .insight-panel {
      border-radius: 8px !important;
      border-left: 4px solid #1976d2;

      &.critical { border-left-color: #d32f2f; }
      &.high { border-left-color: #f57c00; }
      &.medium { border-left-color: #ffc107; }
      &.low { border-left-color: #4caf50; }

      .insight-header {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .impact-icon {
        padding: 8px;
        border-radius: 8px;
        font-size: 24px;
        width: 24px;
        height: 24px;

        &.critical { background: #ffebee; color: #d32f2f; }
        &.high { background: #fff3e0; color: #f57c00; }
        &.medium { background: #fff8e1; color: #ffc107; }
        &.low { background: #e8f5e9; color: #4caf50; }
      }

      .insight-title-content {
        display: flex;
        flex-direction: column;
      }

      .insight-title {
        font-weight: 500;
        font-size: 15px;
      }

      .insight-category {
        font-size: 12px;
        color: #666;
      }

      mat-chip {
        &.critical { background: #ffebee !important; color: #d32f2f !important; }
        &.high { background: #fff3e0 !important; color: #f57c00 !important; }
        &.medium { background: #fff8e1 !important; color: #b8860b !important; }
        &.low { background: #e8f5e9 !important; color: #2e7d32 !important; }
      }

      .affected-count {
        margin-left: 12px;
        font-size: 13px;
        color: #666;
      }
    }

    .insight-body {
      padding: 16px 0;
    }

    .insight-description {
      color: #555;
      line-height: 1.6;
      margin-bottom: 16px;
    }

    .metrics-row {
      display: flex;
      gap: 24px;
      margin-bottom: 20px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;

      .metric {
        display: flex;
        flex-direction: column;
        align-items: center;

        .metric-value {
          font-size: 24px;
          font-weight: 600;
          color: #333;
        }

        .metric-label {
          font-size: 12px;
          color: #666;
        }

        &.negative .metric-value { color: #d32f2f; }
        &.positive .metric-value { color: #4caf50; }
      }
    }

    .risk-factors {
      margin-bottom: 20px;

      h4 {
        margin: 0 0 12px 0;
        font-size: 14px;
        font-weight: 500;
        color: #333;
      }
    }

    .factor-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;

      .factor-name {
        width: 250px;
        font-size: 13px;
        color: #555;
      }

      .factor-bar-container {
        flex: 1;
        height: 8px;
        background: #e0e0e0;
        border-radius: 4px;
        overflow: hidden;
      }

      .factor-bar {
        height: 100%;
        background: linear-gradient(90deg, #f57c00, #ffb74d);
        border-radius: 4px;
      }

      .factor-value {
        width: 40px;
        font-size: 13px;
        font-weight: 500;
        color: #333;
        text-align: right;
      }
    }

    .suggested-actions {
      h4 {
        margin: 0 0 12px 0;
        font-size: 14px;
        font-weight: 500;
        color: #333;
      }

      .actions-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }

      .action-button {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;

        .action-text {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          text-align: left;
        }

        .action-label {
          font-weight: 500;
        }

        .action-impact {
          font-size: 11px;
          color: #4caf50;
        }
      }
    }

    .insight-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;

      .insight-date {
        font-size: 12px;
        color: #999;
      }
    }

    /* Predicted Gaps */
    .predicted-gaps-intro {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #e3f2fd;
      border-radius: 8px;
      margin-bottom: 20px;

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
      }

      p {
        margin: 0;
        color: #1565c0;
        line-height: 1.5;
      }
    }

    .predicted-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 16px;
    }

    .predicted-card {
      padding: 16px;

      .predicted-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;

        .patient-info {
          display: flex;
          flex-direction: column;

          .patient-name {
            font-weight: 600;
            font-size: 16px;
          }

          .patient-mrn {
            font-size: 12px;
            color: #666;
          }
        }

        .risk-indicator {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 8px 12px;
          border-radius: 8px;

          &.high { background: #ffebee; }
          &.medium { background: #fff8e1; }
          &.low { background: #e8f5e9; }

          .risk-score {
            font-size: 20px;
            font-weight: 600;
          }

          .risk-label {
            font-size: 10px;
            color: #666;
          }
        }
      }

      .predicted-body {
        .measure-info {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 12px;
          font-weight: 500;
        }

        .prediction-info {
          display: flex;
          gap: 20px;
          margin-bottom: 12px;
          font-size: 13px;
          color: #555;

          > div {
            display: flex;
            align-items: center;
            gap: 4px;

            mat-icon {
              font-size: 18px;
              width: 18px;
              height: 18px;
            }
          }
        }

        .predicted-factors {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .factor-chip {
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 4px 10px;
            background: #f5f5f5;
            border-radius: 12px;
            font-size: 12px;
            color: #666;

            mat-icon {
              font-size: 14px;
              width: 14px;
              height: 14px;
            }
          }
        }
      }

      .predicted-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #e0e0e0;

        .recommendation {
          font-size: 13px;
          color: #1976d2;
          font-style: italic;
        }
      }
    }

    /* Quality Scores */
    .quality-scores {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }

    .score-card {
      padding: 16px;

      &.below-target {
        border-left: 4px solid #f57c00;
      }

      .score-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .measure-name {
          font-weight: 500;
          font-size: 15px;
        }

        .trend-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;

          &.improving { color: #4caf50; }
          &.worsening { color: #d32f2f; }
          &.stable { color: #9e9e9e; }
        }
      }

      .score-body {
        margin-bottom: 12px;
      }

      .score-gauge {
        height: 12px;
        background: #e0e0e0;
        border-radius: 6px;
        position: relative;
        overflow: visible;
        margin-bottom: 8px;

        .gauge-fill {
          height: 100%;
          background: linear-gradient(90deg, #1976d2, #42a5f5);
          border-radius: 6px;
          transition: width 0.5s ease;
        }

        .target-marker {
          position: absolute;
          top: -4px;
          width: 4px;
          height: 20px;
          background: #333;
          border-radius: 2px;
          transform: translateX(-50%);
        }
      }

      .score-values {
        display: flex;
        justify-content: space-between;
        font-size: 13px;

        .current-score {
          font-weight: 600;
          font-size: 18px;
        }

        .target-score {
          color: #666;
        }
      }
    }

    /* Loading & No Data */
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #666;

      p {
        margin-top: 16px;
      }
    }

    .no-data {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #999;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        margin-bottom: 12px;
        color: #4caf50;
      }
    }

    .dismissed-intro {
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      margin-bottom: 20px;

      p {
        margin: 0;
        color: #666;
      }
    }

    /* Responsive */
    @media (max-width: 768px) {
      .page {
        padding: 16px;
      }

      .charts-row {
        grid-template-columns: 1fr;
      }

      .predicted-list {
        grid-template-columns: 1fr;
      }

      .quality-scores {
        grid-template-columns: 1fr;
      }

      .factor-row .factor-name {
        width: 150px;
      }
    }
  `],
})
export class InsightsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  loading = false;
  loadingPredictions = false;

  insightsResponse: InsightsResponse | null = null;
  summary: PopulationSummary | null = null;
  predictedGaps: PredictedCareGap[] = [];
  filteredInsights: PopulationInsight[] = [];

  selectedImpacts: string[] = [];

  constructor(
    private insightsService: PopulationInsightsService,
    private toast: ToastService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all insights data
   */
  loadData(): void {
    this.loading = true;
    this.loadingPredictions = true;

    // Load insights
    this.insightsService.getInsights().pipe(
      takeUntil(this.destroy$),
      finalize(() => (this.loading = false))
    ).subscribe({
      next: (response) => {
        this.insightsResponse = response;
        this.filteredInsights = response.insights;
      },
      error: () => {
        this.toast.error('Failed to load insights');
      },
    });

    // Load summary
    this.insightsService.getPopulationSummary().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.summary = summary;
      },
    });

    // Load predicted gaps
    this.insightsService.getPredictedCareGaps().pipe(
      takeUntil(this.destroy$),
      finalize(() => (this.loadingPredictions = false))
    ).subscribe({
      next: (gaps) => {
        this.predictedGaps = gaps;
      },
    });
  }

  /**
   * Refresh all data
   */
  refreshInsights(): void {
    this.loadData();
    this.toast.info('Refreshing insights...');
  }

  /**
   * Filter insights by impact level
   */
  filterInsights(): void {
    if (!this.insightsResponse) return;

    if (this.selectedImpacts.length === 0) {
      this.filteredInsights = this.insightsResponse.insights;
    } else {
      this.filteredInsights = this.insightsResponse.insights.filter(
        (i) => this.selectedImpacts.includes(i.impact)
      );
    }
  }

  /**
   * Get risk width percentage for pyramid
   */
  getRiskWidth(level: 'critical' | 'high' | 'medium' | 'low'): number {
    if (!this.summary) return 30;
    const total = this.summary.totalPatients || 1;
    const count = this.summary.riskDistribution[level] || 0;
    // Scale to make pyramid shape: critical narrowest, low widest
    const baseWidth = (count / total) * 100;
    const scales = { critical: 0.4, high: 0.6, medium: 0.8, low: 1.0 };
    return Math.max(30, baseWidth * scales[level] * 3);
  }

  /**
   * Get icon for insight type
   */
  getInsightIcon(type: string): string {
    const icons: Record<string, string> = {
      CARE_GAP_CLUSTER: 'health_and_safety',
      PERFORMANCE_TREND: 'trending_down',
      AT_RISK_POPULATION: 'warning',
      INTERVENTION_OPPORTUNITY: 'tips_and_updates',
      QUALITY_ALERT: 'notification_important',
      PREDICTED_GAP: 'psychology',
    };
    return icons[type] || 'lightbulb';
  }

  /**
   * Get icon for action type
   */
  getActionIcon(type: string): string {
    const icons: Record<string, string> = {
      BATCH_OUTREACH: 'campaign',
      SCHEDULE_VISITS: 'event',
      ORDER_LABS: 'science',
      MEDICATION_REVIEW: 'medication',
      REFER_SPECIALIST: 'person_search',
      PATIENT_EDUCATION: 'school',
    };
    return icons[type] || 'play_arrow';
  }

  /**
   * Get trend icon
   */
  getTrendIcon(trend: string): string {
    const icons: Record<string, string> = {
      improving: 'trending_up',
      worsening: 'trending_down',
      stable: 'trending_flat',
    };
    return icons[trend] || 'trending_flat';
  }

  /**
   * Get trend tooltip
   */
  getTrendTooltip(trend: string): string {
    const tooltips: Record<string, string> = {
      improving: 'Improving trend',
      worsening: 'Declining trend',
      stable: 'Stable',
    };
    return tooltips[trend] || 'Unknown trend';
  }

  /**
   * Get risk class based on score
   */
  getRiskClass(score: number): string {
    if (score >= 80) return 'high';
    if (score >= 60) return 'medium';
    return 'low';
  }

  /**
   * Format date for display
   */
  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString();
  }

  /**
   * Execute an action from an insight
   */
  executeAction(insight: PopulationInsight, action: any): void {
    this.insightsService.executeAction(insight.id, action.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        this.toast.success(`${action.label} initiated for ${insight.affectedPatients} patients`);
      },
      error: () => {
        this.toast.error('Failed to execute action');
      },
    });
  }

  /**
   * Dismiss an insight
   */
  dismissInsight(insight: PopulationInsight): void {
    this.insightsService.dismissInsight(insight.id, 'User dismissed').pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.filteredInsights = this.filteredInsights.filter((i) => i.id !== insight.id);
        if (this.insightsResponse) {
          this.insightsResponse.insights = this.insightsResponse.insights.filter((i) => i.id !== insight.id);
          this.insightsResponse.summary.dismissed++;
        }
        this.toast.info('Insight dismissed');
      },
    });
  }

  /**
   * Schedule intervention for predicted gap
   */
  scheduleIntervention(gap: PredictedCareGap): void {
    this.toast.success(`Scheduling ${gap.measureName} intervention for ${gap.patientName}`);
    // In real implementation, this would open a scheduling dialog
  }
}
