import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, takeUntil } from 'rxjs';
import {
  PredictiveCareGapService,
  PredictedCareGap,
  RiskTier,
  PredictionFactor,
} from '../../services/predictive-care-gap.service';

/**
 * Predicted Care Gaps Widget Component
 *
 * Displays predicted care gaps on the provider dashboard with risk scores,
 * prediction factors, and recommended interventions.
 *
 * Issue #157: Implement Predictive Care Gap Detection
 */
@Component({
  selector: 'app-predicted-care-gaps',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressBarModule,
    MatBadgeModule,
  ],
  template: `
    <mat-card class="predicted-gaps-card" [class.collapsed]="isCollapsed">
      <mat-card-header (click)="toggleCollapse()">
        <mat-card-title>
          <mat-icon class="card-icon">psychology</mat-icon>
          Predicted Care Gaps
          <span class="prediction-badge" [class.urgent]="urgentCount > 0">
            {{ predictions.length }}
          </span>
          @if (urgentCount > 0) {
            <span class="urgent-indicator" matTooltip="{{ urgentCount }} urgent predictions">
              <mat-icon>priority_high</mat-icon>
              {{ urgentCount }}
            </span>
          }
        </mat-card-title>
        <div class="header-actions">
          <button mat-icon-button
                  matTooltip="Refresh predictions"
                  (click)="refreshPredictions(); $event.stopPropagation()">
            <mat-icon>refresh</mat-icon>
          </button>
          <button mat-icon-button class="collapse-toggle" (click)="toggleCollapse(); $event.stopPropagation()">
            <mat-icon>{{ isCollapsed ? 'expand_more' : 'expand_less' }}</mat-icon>
          </button>
        </div>
      </mat-card-header>

      @if (!isCollapsed) {
        <mat-card-content>
          @if (loading) {
            <div class="loading-container">
              <mat-spinner diameter="40"></mat-spinner>
              <p>Analyzing patient data...</p>
            </div>
          } @else if (predictions.length === 0) {
            <div class="empty-state">
              <mat-icon>verified</mat-icon>
              <h3>No Predicted Gaps</h3>
              <p>All patients are on track with their care schedules.</p>
            </div>
          } @else {
            <!-- Stats Summary -->
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-value">{{ predictions.length }}</span>
                <span class="stat-label">Total Predictions</span>
              </div>
              <div class="stat-item urgent">
                <span class="stat-value">{{ urgentCount }}</span>
                <span class="stat-label">Urgent (≤7 days)</span>
              </div>
              <div class="stat-item warning">
                <span class="stat-value">{{ interventionWindowCount }}</span>
                <span class="stat-label">Window Closing</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ averageRiskScore | number:'1.0-0' }}%</span>
                <span class="stat-label">Avg Risk Score</span>
              </div>
            </div>

            <!-- Predictions List -->
            <div class="predictions-list">
              @for (prediction of displayedPredictions; track prediction.id) {
                <div class="prediction-item"
                     [class.urgent]="prediction.daysUntilGap <= 7 && prediction.riskScore >= 70"
                     (click)="viewPatient.emit(prediction.patientId)">

                  <!-- Risk Score Indicator -->
                  <div class="risk-indicator"
                       [style.background-color]="getRiskColor(prediction.riskTier)">
                    <span class="risk-score">{{ prediction.riskScore | number:'1.0-0' }}</span>
                    <span class="risk-label">{{ prediction.riskTier }}</span>
                  </div>

                  <!-- Prediction Details -->
                  <div class="prediction-details">
                    <div class="patient-measure">
                      <strong class="patient-name">{{ prediction.patientName }}</strong>
                      <span class="measure-name">{{ prediction.measureName }}</span>
                    </div>

                    <div class="prediction-info">
                      <div class="days-until">
                        <mat-icon [class.urgent]="prediction.daysUntilGap <= 7">schedule</mat-icon>
                        <span [class.urgent]="prediction.daysUntilGap <= 7">
                          {{ prediction.daysUntilGap }} days until gap
                        </span>
                      </div>
                      <div class="confidence">
                        <mat-icon>insights</mat-icon>
                        <span>{{ prediction.confidence * 100 | number:'1.0-0' }}% confidence</span>
                      </div>
                    </div>

                    <!-- Primary Factor -->
                    @if (getPrimaryFactor(prediction); as factor) {
                      <div class="primary-factor" [class.concerning]="factor.isConcerning">
                        <mat-icon>{{ getFactorIcon(factor.factorType) }}</mat-icon>
                        <span class="factor-name">{{ factor.name }}:</span>
                        <span class="factor-context">{{ truncate(factor.context, 60) }}</span>
                      </div>
                    }

                    <!-- Priority Intervention -->
                    @if (prediction.priorityIntervention) {
                      <div class="intervention-chip">
                        <mat-icon>lightbulb</mat-icon>
                        <span>{{ prediction.priorityIntervention }}</span>
                        <span class="success-rate">({{ prediction.interventionSuccessRate }}% success)</span>
                      </div>
                    }
                  </div>

                  <!-- Actions -->
                  <div class="prediction-actions">
                    <button mat-icon-button
                            matTooltip="View patient"
                            (click)="viewPatient.emit(prediction.patientId); $event.stopPropagation()">
                      <mat-icon>person</mat-icon>
                    </button>
                    <button mat-icon-button
                            matTooltip="Schedule intervention"
                            (click)="scheduleIntervention.emit(prediction); $event.stopPropagation()">
                      <mat-icon>event</mat-icon>
                    </button>
                    <button mat-icon-button
                            matTooltip="View details"
                            (click)="viewDetails.emit(prediction); $event.stopPropagation()">
                      <mat-icon>info</mat-icon>
                    </button>
                  </div>
                </div>
              }
            </div>

            <!-- Show More / Less -->
            @if (predictions.length > maxDisplayed) {
              <div class="show-more">
                <button mat-button color="primary" (click)="toggleShowAll()">
                  @if (showAll) {
                    <mat-icon>expand_less</mat-icon>
                    Show Less
                  } @else {
                    <mat-icon>expand_more</mat-icon>
                    Show All {{ predictions.length }} Predictions
                  }
                </button>
              </div>
            }
          }
        </mat-card-content>
      }
    </mat-card>
  `,
  styles: [`
    .predicted-gaps-card {
      margin-bottom: 16px;
      border-left: 4px solid #7c3aed;
    }

    mat-card-header {
      cursor: pointer;
      padding: 16px;
      background: linear-gradient(135deg, #7c3aed10 0%, #a78bfa10 100%);
    }

    mat-card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
    }

    .card-icon {
      color: #7c3aed;
    }

    .prediction-badge {
      background: #7c3aed;
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }

    .prediction-badge.urgent {
      background: #dc2626;
      animation: pulse 2s infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.7; }
    }

    .urgent-indicator {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #dc2626;
      font-size: 12px;
      font-weight: 600;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    .header-actions {
      display: flex;
      gap: 4px;
      margin-left: auto;
    }

    mat-card-content {
      padding: 16px;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px;
      color: #6b7280;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px;
      text-align: center;
      color: #6b7280;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: #10b981;
        margin-bottom: 12px;
      }

      h3 {
        margin: 0 0 8px 0;
        color: #374151;
      }

      p {
        margin: 0;
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid #e5e7eb;
    }

    .stat-item {
      text-align: center;
      padding: 8px;
      background: #f9fafb;
      border-radius: 8px;

      &.urgent {
        background: #fef2f2;
        .stat-value { color: #dc2626; }
      }

      &.warning {
        background: #fffbeb;
        .stat-value { color: #f59e0b; }
      }
    }

    .stat-value {
      display: block;
      font-size: 20px;
      font-weight: 700;
      color: #7c3aed;
    }

    .stat-label {
      font-size: 11px;
      color: #6b7280;
    }

    .predictions-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .prediction-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px;
      background: #f9fafb;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #f3f4f6;
        transform: translateX(4px);
      }

      &.urgent {
        background: #fef2f2;
        border-left: 3px solid #dc2626;

        &:hover {
          background: #fee2e2;
        }
      }
    }

    .risk-indicator {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-width: 60px;
      padding: 8px;
      border-radius: 8px;
      color: white;
    }

    .risk-score {
      font-size: 20px;
      font-weight: 700;
    }

    .risk-label {
      font-size: 9px;
      text-transform: uppercase;
      opacity: 0.9;
    }

    .prediction-details {
      flex: 1;
      min-width: 0;
    }

    .patient-measure {
      display: flex;
      flex-direction: column;
      margin-bottom: 8px;
    }

    .patient-name {
      font-size: 14px;
      color: #1f2937;
    }

    .measure-name {
      font-size: 12px;
      color: #6b7280;
    }

    .prediction-info {
      display: flex;
      gap: 16px;
      margin-bottom: 8px;
      font-size: 12px;
      color: #6b7280;

      div {
        display: flex;
        align-items: center;
        gap: 4px;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }
      }

      .urgent {
        color: #dc2626;
        font-weight: 600;
      }
    }

    .primary-factor {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      color: #6b7280;
      padding: 6px 8px;
      background: white;
      border-radius: 4px;
      margin-bottom: 8px;

      mat-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
        color: #9ca3af;
      }

      &.concerning {
        background: #fef3c7;
        color: #92400e;

        mat-icon {
          color: #f59e0b;
        }
      }
    }

    .factor-name {
      font-weight: 600;
    }

    .factor-context {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .intervention-chip {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #059669;
      padding: 4px 8px;
      background: #ecfdf5;
      border-radius: 4px;

      mat-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
        color: #10b981;
      }

      .success-rate {
        color: #6b7280;
        margin-left: 4px;
      }
    }

    .prediction-actions {
      display: flex;
      flex-direction: column;
      gap: 4px;

      button {
        width: 32px;
        height: 32px;
        line-height: 32px;

        mat-icon {
          font-size: 18px;
        }
      }
    }

    .show-more {
      display: flex;
      justify-content: center;
      padding-top: 12px;
      border-top: 1px solid #e5e7eb;
      margin-top: 12px;
    }

    .collapsed mat-card-content {
      display: none;
    }

    @media (max-width: 768px) {
      .stats-row {
        grid-template-columns: repeat(2, 1fr);
      }

      .prediction-item {
        flex-direction: column;
      }

      .risk-indicator {
        flex-direction: row;
        gap: 8px;
        min-width: auto;
        width: 100%;
      }

      .prediction-actions {
        flex-direction: row;
        width: 100%;
        justify-content: flex-end;
      }
    }
  `],
})
export class PredictedCareGapsComponent implements OnInit, OnDestroy {
  @Input() providerId = '';
  @Input() maxDisplayed = 5;

  @Output() viewPatient = new EventEmitter<string>();
  @Output() scheduleIntervention = new EventEmitter<PredictedCareGap>();
  @Output() viewDetails = new EventEmitter<PredictedCareGap>();

  private readonly destroy$ = new Subject<void>();

  predictions: PredictedCareGap[] = [];
  loading = true;
  isCollapsed = false;
  showAll = false;

  // Computed stats
  urgentCount = 0;
  interventionWindowCount = 0;
  averageRiskScore = 0;

  constructor(private predictiveCareGapService: PredictiveCareGapService) {}

  ngOnInit(): void {
    this.loadPredictions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get displayedPredictions(): PredictedCareGap[] {
    if (this.showAll) {
      return this.predictions;
    }
    return this.predictions.slice(0, this.maxDisplayed);
  }

  loadPredictions(): void {
    this.loading = true;

    // Mock provider data for demo
    const providerData = {
      patients: [],
    };

    this.predictiveCareGapService
      .getPredictedGapsForProvider(this.providerId || 'demo-provider', providerData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (predictions) => {
          this.predictions = predictions;
          this.calculateStats();
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  refreshPredictions(): void {
    this.predictiveCareGapService.invalidateCache();
    this.loadPredictions();
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleShowAll(): void {
    this.showAll = !this.showAll;
  }

  getPrimaryFactor(prediction: PredictedCareGap): PredictionFactor | null {
    return this.predictiveCareGapService.getPrimaryFactor(prediction);
  }

  getRiskColor(tier: RiskTier): string {
    return this.predictiveCareGapService.getRiskTierColor(tier);
  }

  getFactorIcon(factorType: string): string {
    const icons: Record<string, string> = {
      HISTORICAL_PATTERN: 'history',
      APPOINTMENT_ADHERENCE: 'event_busy',
      MEDICATION_REFILLS: 'medication',
      SIMILAR_PATIENT_BEHAVIOR: 'group',
    };
    return icons[factorType] || 'info';
  }

  truncate(text: string, length: number): string {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
  }

  private calculateStats(): void {
    this.urgentCount = this.predictiveCareGapService.getUrgentPredictions(this.predictions).length;
    this.interventionWindowCount = this.predictiveCareGapService.getInterventionWindowClosing(this.predictions).length;

    if (this.predictions.length > 0) {
      const totalScore = this.predictions.reduce((sum, p) => sum + p.riskScore, 0);
      this.averageRiskScore = totalScore / this.predictions.length;
    } else {
      this.averageRiskScore = 0;
    }
  }
}
