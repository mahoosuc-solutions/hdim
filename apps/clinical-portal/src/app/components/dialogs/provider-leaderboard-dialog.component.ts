/**
 * Provider Leaderboard Dialog Component
 * Issue #20: Provider Leaderboards & Quality Reporting
 *
 * Features:
 * - Provider performance ranking by various metrics
 * - Quality scores comparison
 * - Care gap closure rates
 * - Patient panel statistics
 * - Export functionality
 */
import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { LoggerService } from './logger.service';
import { CommonModule } from '@angular/common';
import { LoggerService } from './logger.service';
import { FormsModule } from '@angular/forms';
import { LoggerService } from './logger.service';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { LoggerService } from './logger.service';
import { MatIconModule } from '@angular/material/icon';
import { LoggerService } from './logger.service';
import { MatTabsModule } from '@angular/material/tabs';
import { LoggerService } from './logger.service';
import { MatTableModule } from '@angular/material/table';
import { LoggerService } from './logger.service';
import { MatSortModule, Sort } from '@angular/material/sort';
import { LoggerService } from './logger.service';
import { MatChipsModule } from '@angular/material/chips';
import { LoggerService } from './logger.service';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoggerService } from './logger.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoggerService } from './logger.service';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoggerService } from './logger.service';
import { MatSelectModule } from '@angular/material/select';
import { LoggerService } from './logger.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { LoggerService } from './logger.service';
import { MatMenuModule } from '@angular/material/menu';
import { LoggerService } from './logger.service';
import { MatDividerModule } from '@angular/material/divider';
import { LoggerService } from './logger.service';
import { Subject } from 'rxjs';
import { LoggerService } from './logger.service';
import { takeUntil, finalize } from 'rxjs/operators';
import { LoggerService } from './logger.service';

// Provider Performance Interface
export interface ProviderPerformance {
  providerId: string;
  providerName: string;
  specialty: string;
  department: string;
  panelSize: number;
  qualityScore: number;
  careGapClosureRate: number;
  patientSatisfaction: number;
  preventiveCareRate: number;
  chronicDiseaseManagement: number;
  timelyDocumentation: number;
  trend: 'improving' | 'stable' | 'declining';
  rank: number;
  previousRank: number;
  awards: string[];
}

// Leaderboard Category
export type LeaderboardCategory =
  | 'overall'
  | 'quality'
  | 'gap-closure'
  | 'preventive'
  | 'chronic';

export interface ProviderLeaderboardDialogData {
  title?: string;
  initialCategory?: LeaderboardCategory;
}

@Component({
  selector: 'app-provider-leaderboard-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatTableModule,
    MatSortModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSelectModule,
    MatFormFieldModule,
    MatMenuModule,
    MatDividerModule,
  ],
  template: `
    <div class="leaderboard-dialog">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon color="primary">leaderboard</mat-icon>
          <div>
            <h2>Provider Performance Leaderboard</h2>
            <p class="subtitle">Quality metrics and rankings across your organization</p>
          </div>
        </div>
        <button mat-icon-button matTooltip="Close" (click)="close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <!-- Summary Cards -->
        <div class="summary-row">
          <div class="summary-card gold">
            <div class="card-medal">
              <mat-icon>emoji_events</mat-icon>
            </div>
            <div class="card-content">
              <span class="card-label">Top Performer</span>
              <span class="card-value">{{ topPerformer?.providerName || 'Loading...' }}</span>
              <span class="card-stat">{{ topPerformer?.qualityScore || 0 }}% Quality Score</span>
            </div>
          </div>

          <div class="summary-card silver">
            <div class="card-medal">
              <mat-icon>workspace_premium</mat-icon>
            </div>
            <div class="card-content">
              <span class="card-label">Org Average</span>
              <span class="card-value">{{ orgAverageQuality }}%</span>
              <span class="card-stat">Quality Score</span>
            </div>
          </div>

          <div class="summary-card bronze">
            <div class="card-medal">
              <mat-icon>trending_up</mat-icon>
            </div>
            <div class="card-content">
              <span class="card-label">Most Improved</span>
              <span class="card-value">{{ mostImproved?.providerName || 'Loading...' }}</span>
              <span class="card-stat">+{{ improvementPercent }}% this quarter</span>
            </div>
          </div>

          <div class="summary-card info">
            <div class="card-medal">
              <mat-icon>groups</mat-icon>
            </div>
            <div class="card-content">
              <span class="card-label">Total Providers</span>
              <span class="card-value">{{ providers.length }}</span>
              <span class="card-stat">Active in panel</span>
            </div>
          </div>
        </div>

        <!-- Category Tabs -->
        <mat-tab-group
          [(selectedIndex)]="selectedTabIndex"
          animationDuration="200ms"
          class="leaderboard-tabs">

          <!-- Overall Rankings Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">leaderboard</mat-icon>
              Overall Rankings
            </ng-template>
            <div class="tab-content">
              <ng-container *ngTemplateOutlet="leaderboardTable; context: { category: 'overall' }"></ng-container>
            </div>
          </mat-tab>

          <!-- Quality Scores Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">verified</mat-icon>
              Quality Scores
            </ng-template>
            <div class="tab-content">
              <ng-container *ngTemplateOutlet="leaderboardTable; context: { category: 'quality' }"></ng-container>
            </div>
          </mat-tab>

          <!-- Care Gap Closure Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">task_alt</mat-icon>
              Gap Closure
            </ng-template>
            <div class="tab-content">
              <ng-container *ngTemplateOutlet="leaderboardTable; context: { category: 'gap-closure' }"></ng-container>
            </div>
          </mat-tab>

          <!-- Preventive Care Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">health_and_safety</mat-icon>
              Preventive Care
            </ng-template>
            <div class="tab-content">
              <ng-container *ngTemplateOutlet="leaderboardTable; context: { category: 'preventive' }"></ng-container>
            </div>
          </mat-tab>

          <!-- Chronic Disease Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">monitor_heart</mat-icon>
              Chronic Care
            </ng-template>
            <div class="tab-content">
              <ng-container *ngTemplateOutlet="leaderboardTable; context: { category: 'chronic' }"></ng-container>
            </div>
          </mat-tab>
        </mat-tab-group>

        <!-- Leaderboard Table Template -->
        <ng-template #leaderboardTable let-category="category">
          @if (loading) {
            <div class="loading-container">
              <mat-spinner diameter="40"></mat-spinner>
              <p>Loading provider data...</p>
            </div>
          } @else {
            <div class="leaderboard-list">
              @for (provider of getSortedProviders(category); track provider.providerId; let i = $index) {
                <div class="provider-row" [class.top-three]="i < 3" [class.current-user]="provider.providerId === currentProviderId">
                  <!-- Rank -->
                  <div class="rank-cell">
                    @if (i === 0) {
                      <div class="rank-medal gold">
                        <mat-icon>emoji_events</mat-icon>
                      </div>
                    } @else if (i === 1) {
                      <div class="rank-medal silver">
                        <mat-icon>workspace_premium</mat-icon>
                      </div>
                    } @else if (i === 2) {
                      <div class="rank-medal bronze">
                        <mat-icon>military_tech</mat-icon>
                      </div>
                    } @else {
                      <div class="rank-number">{{ i + 1 }}</div>
                    }
                    <div class="rank-change" [class]="getRankChangeClass(provider)">
                      @if (provider.rank < provider.previousRank) {
                        <mat-icon>arrow_upward</mat-icon>
                        <span>{{ provider.previousRank - provider.rank }}</span>
                      } @else if (provider.rank > provider.previousRank) {
                        <mat-icon>arrow_downward</mat-icon>
                        <span>{{ provider.rank - provider.previousRank }}</span>
                      } @else {
                        <mat-icon>remove</mat-icon>
                      }
                    </div>
                  </div>

                  <!-- Provider Info -->
                  <div class="provider-info">
                    <div class="provider-avatar" [style.background-color]="getAvatarColor(provider.providerName)">
                      {{ getInitials(provider.providerName) }}
                    </div>
                    <div class="provider-details">
                      <span class="provider-name">{{ provider.providerName }}</span>
                      <span class="provider-specialty">{{ provider.specialty }} &bull; {{ provider.department }}</span>
                    </div>
                    @if (provider.awards.length > 0) {
                      <div class="provider-awards">
                        @for (award of provider.awards.slice(0, 2); track award) {
                          <mat-chip size="small" [matTooltip]="award">
                            <mat-icon>star</mat-icon>
                            {{ getAwardShortName(award) }}
                          </mat-chip>
                        }
                      </div>
                    }
                  </div>

                  <!-- Metrics -->
                  <div class="metrics-cell">
                    <div class="metric-item">
                      <span class="metric-label">Panel</span>
                      <span class="metric-value">{{ provider.panelSize }}</span>
                    </div>
                    <div class="metric-item primary">
                      <span class="metric-label">{{ getCategoryLabel(category) }}</span>
                      <span class="metric-value">{{ getCategoryValue(provider, category) }}%</span>
                    </div>
                  </div>

                  <!-- Progress Bar -->
                  <div class="progress-cell">
                    <div class="progress-wrapper">
                      <mat-progress-bar
                        mode="determinate"
                        [value]="getCategoryValue(provider, category)"
                        [color]="getProgressColor(getCategoryValue(provider, category))">
                      </mat-progress-bar>
                      <span class="progress-label">{{ getCategoryValue(provider, category) }}%</span>
                    </div>
                  </div>

                  <!-- Trend -->
                  <div class="trend-cell">
                    <mat-icon [class]="'trend-icon ' + provider.trend">
                      {{ getTrendIcon(provider.trend) }}
                    </mat-icon>
                    <span class="trend-label">{{ provider.trend | titlecase }}</span>
                  </div>

                  <!-- Actions -->
                  <div class="actions-cell">
                    <button mat-icon-button matTooltip="View Details" (click)="viewProviderDetails(provider)">
                      <mat-icon>visibility</mat-icon>
                    </button>
                    <button mat-icon-button [matMenuTriggerFor]="actionMenu">
                      <mat-icon>more_vert</mat-icon>
                    </button>
                    <mat-menu #actionMenu="matMenu">
                      <button mat-menu-item (click)="compareWithOrg(provider)">
                        <mat-icon>compare</mat-icon>
                        <span>Compare with Org Average</span>
                      </button>
                      <button mat-menu-item (click)="viewPatientPanel(provider)">
                        <mat-icon>people</mat-icon>
                        <span>View Patient Panel</span>
                      </button>
                      <button mat-menu-item (click)="viewCareGaps(provider)">
                        <mat-icon>assignment_late</mat-icon>
                        <span>View Care Gaps</span>
                      </button>
                      <mat-divider></mat-divider>
                      <button mat-menu-item (click)="exportProviderReport(provider)">
                        <mat-icon>download</mat-icon>
                        <span>Export Report</span>
                      </button>
                    </mat-menu>
                  </div>
                </div>
              }
            </div>
          }
        </ng-template>
      </mat-dialog-content>

      <!-- Footer -->
      <mat-dialog-actions align="end">
        <button mat-button (click)="exportLeaderboard()">
          <mat-icon>download</mat-icon>
          Export Leaderboard
        </button>
        <button mat-button (click)="printLeaderboard()">
          <mat-icon>print</mat-icon>
          Print
        </button>
        <button mat-raised-button color="primary" (click)="close()">
          Close
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
    .leaderboard-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
      max-height: 85vh;
      width: 100%;
      min-width: 900px;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
      color: white;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
          color: white;
        }

        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
        }

        .subtitle {
          margin: 4px 0 0 0;
          font-size: 13px;
          opacity: 0.9;
        }
      }

      button {
        color: white;
      }
    }

    .dialog-content {
      flex: 1;
      overflow: auto;
      padding: 0 !important;
    }

    /* Summary Cards */
    .summary-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      padding: 24px;
      background: #f5f5f5;
    }

    .summary-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

      .card-medal {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
          color: white;
        }
      }

      &.gold .card-medal { background: linear-gradient(135deg, #ffd700, #ffb300); }
      &.silver .card-medal { background: linear-gradient(135deg, #c0c0c0, #9e9e9e); }
      &.bronze .card-medal { background: linear-gradient(135deg, #cd7f32, #8b4513); }
      &.info .card-medal { background: linear-gradient(135deg, #1976d2, #42a5f5); }

      .card-content {
        display: flex;
        flex-direction: column;

        .card-label {
          font-size: 11px;
          color: #666;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .card-value {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin: 2px 0;
        }

        .card-stat {
          font-size: 12px;
          color: #888;
        }
      }
    }

    /* Tabs */
    .leaderboard-tabs {
      .tab-icon {
        margin-right: 8px;
      }
    }

    .tab-content {
      padding: 0;
    }

    /* Leaderboard List */
    .leaderboard-list {
      display: flex;
      flex-direction: column;
    }

    .provider-row {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 24px;
      border-bottom: 1px solid #e0e0e0;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: #fafafa;
      }

      &.top-three {
        background-color: #fffde7;
      }

      &.current-user {
        background-color: #e3f2fd;
        border-left: 4px solid #1976d2;
      }
    }

    .rank-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 60px;

      .rank-medal {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
          color: white;
        }

        &.gold { background: linear-gradient(135deg, #ffd700, #ffb300); }
        &.silver { background: linear-gradient(135deg, #c0c0c0, #9e9e9e); }
        &.bronze { background: linear-gradient(135deg, #cd7f32, #8b4513); }
      }

      .rank-number {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: #f5f5f5;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        font-weight: 600;
        color: #666;
      }

      .rank-change {
        display: flex;
        align-items: center;
        gap: 2px;
        font-size: 11px;
        margin-top: 4px;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }

        &.up {
          color: #4caf50;
        }

        &.down {
          color: #f44336;
        }

        &.same {
          color: #9e9e9e;
        }
      }
    }

    .provider-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      min-width: 0;

      .provider-avatar {
        width: 44px;
        height: 44px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        font-weight: 600;
        color: white;
        flex-shrink: 0;
      }

      .provider-details {
        display: flex;
        flex-direction: column;
        min-width: 0;

        .provider-name {
          font-weight: 600;
          font-size: 15px;
          color: #333;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .provider-specialty {
          font-size: 12px;
          color: #666;
        }
      }

      .provider-awards {
        display: flex;
        gap: 4px;

        mat-chip {
          font-size: 10px;
          height: 22px;
          min-height: 22px;

          mat-icon {
            font-size: 12px;
            width: 12px;
            height: 12px;
            margin-right: 2px;
            color: #ffc107;
          }
        }
      }
    }

    .metrics-cell {
      display: flex;
      gap: 20px;
      width: 160px;

      .metric-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .metric-label {
          font-size: 10px;
          color: #888;
          text-transform: uppercase;
        }

        .metric-value {
          font-size: 16px;
          font-weight: 600;
          color: #333;
        }

        &.primary .metric-value {
          color: #1976d2;
        }
      }
    }

    .progress-cell {
      width: 180px;

      .progress-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-progress-bar {
          flex: 1;
          height: 8px;
          border-radius: 4px;
        }

        .progress-label {
          font-size: 14px;
          font-weight: 600;
          color: #333;
          min-width: 40px;
          text-align: right;
        }
      }
    }

    .trend-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      width: 100px;

      .trend-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;

        &.improving { color: #4caf50; }
        &.stable { color: #9e9e9e; }
        &.declining { color: #f44336; }
      }

      .trend-label {
        font-size: 12px;
        color: #666;
      }
    }

    .actions-cell {
      display: flex;
      gap: 4px;
    }

    /* Loading */
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;

      p {
        margin-top: 16px;
        color: #666;
      }
    }

    /* Footer */
    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
    }

    /* Responsive */
    @media (max-width: 1200px) {
      .leaderboard-dialog {
        min-width: 100%;
      }

      .summary-row {
        grid-template-columns: repeat(2, 1fr);
      }

      .provider-awards {
        display: none;
      }
    }

    @media (max-width: 768px) {
      .summary-row {
        grid-template-columns: 1fr;
      }

      .progress-cell,
      .trend-cell {
        display: none;
      }

      .metrics-cell {
        width: auto;
      }
    }
  `,
  ],
})
export class ProviderLeaderboardDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  loading = false;
  selectedTabIndex = 0;

  providers: ProviderPerformance[] = [];
  topPerformer: ProviderPerformance | null = null;
  mostImproved: ProviderPerformance | null = null;
  orgAverageQuality = 0;
  improvementPercent = 0;
  currentProviderId = 'provider-current'; // Would come from auth service

  constructor(
    private loggerService: LoggerService,
    private dialogRef: MatDialogRef<ProviderLeaderboardDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProviderLeaderboardDialogData
  ) {}

  ngOnInit(): void {
    this.loadProviderData();

    // Set initial tab based on data
    if (this.data?.initialCategory) {
      const categoryMap: Record<LeaderboardCategory, number> = {
        overall: 0,
        quality: 1,
        'gap-closure': 2,
        preventive: 3,
        chronic: 4,
      };
      this.selectedTabIndex = categoryMap[this.data.initialCategory] || 0;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load provider performance data
   */
  private loadProviderData(): void {
    this.loading = true;

    // Simulated data - would come from API
    setTimeout(() => {
      this.providers = this.generateMockProviders();
      this.calculateSummaryStats();
      this.loading = false;
    }, 500);
  }

  /**
   * Generate mock provider data
   */
  private generateMockProviders(): ProviderPerformance[] {
    const specialties = [
      'Family Medicine',
      'Internal Medicine',
      'Pediatrics',
      'OB/GYN',
      'Geriatrics',
    ];
    const departments = [
      'Primary Care',
      'Specialty Care',
      'Urgent Care',
      'Community Health',
    ];
    const awards = [
      'Top Performer Q4',
      'Patient Satisfaction Award',
      'Quality Champion',
      'Care Gap Hero',
      'Most Improved',
    ];

    const providers: ProviderPerformance[] = [
      {
        providerId: 'p1',
        providerName: 'Dr. Sarah Chen',
        specialty: 'Family Medicine',
        department: 'Primary Care',
        panelSize: 1847,
        qualityScore: 94,
        careGapClosureRate: 89,
        patientSatisfaction: 4.8,
        preventiveCareRate: 91,
        chronicDiseaseManagement: 87,
        timelyDocumentation: 96,
        trend: 'improving',
        rank: 1,
        previousRank: 2,
        awards: ['Top Performer Q4', 'Quality Champion'],
      },
      {
        providerId: 'p2',
        providerName: 'Dr. Michael Rodriguez',
        specialty: 'Internal Medicine',
        department: 'Primary Care',
        panelSize: 1623,
        qualityScore: 91,
        careGapClosureRate: 86,
        patientSatisfaction: 4.6,
        preventiveCareRate: 88,
        chronicDiseaseManagement: 92,
        timelyDocumentation: 89,
        trend: 'stable',
        rank: 2,
        previousRank: 1,
        awards: ['Patient Satisfaction Award'],
      },
      {
        providerId: 'p3',
        providerName: 'Dr. Emily Thompson',
        specialty: 'Pediatrics',
        department: 'Primary Care',
        panelSize: 2105,
        qualityScore: 89,
        careGapClosureRate: 84,
        patientSatisfaction: 4.9,
        preventiveCareRate: 93,
        chronicDiseaseManagement: 78,
        timelyDocumentation: 91,
        trend: 'improving',
        rank: 3,
        previousRank: 5,
        awards: ['Most Improved'],
      },
      {
        providerId: 'p4',
        providerName: 'Dr. James Wilson',
        specialty: 'Geriatrics',
        department: 'Specialty Care',
        panelSize: 892,
        qualityScore: 87,
        careGapClosureRate: 91,
        patientSatisfaction: 4.5,
        preventiveCareRate: 82,
        chronicDiseaseManagement: 94,
        timelyDocumentation: 85,
        trend: 'stable',
        rank: 4,
        previousRank: 4,
        awards: [],
      },
      {
        providerId: 'p5',
        providerName: 'Dr. Lisa Park',
        specialty: 'OB/GYN',
        department: 'Specialty Care',
        panelSize: 1456,
        qualityScore: 85,
        careGapClosureRate: 82,
        patientSatisfaction: 4.7,
        preventiveCareRate: 89,
        chronicDiseaseManagement: 76,
        timelyDocumentation: 88,
        trend: 'improving',
        rank: 5,
        previousRank: 7,
        awards: [],
      },
      {
        providerId: 'p6',
        providerName: 'Dr. Robert Martinez',
        specialty: 'Family Medicine',
        department: 'Community Health',
        panelSize: 2341,
        qualityScore: 83,
        careGapClosureRate: 79,
        patientSatisfaction: 4.4,
        preventiveCareRate: 81,
        chronicDiseaseManagement: 85,
        timelyDocumentation: 82,
        trend: 'declining',
        rank: 6,
        previousRank: 3,
        awards: [],
      },
      {
        providerId: 'p7',
        providerName: 'Dr. Jennifer Lee',
        specialty: 'Internal Medicine',
        department: 'Primary Care',
        panelSize: 1789,
        qualityScore: 81,
        careGapClosureRate: 77,
        patientSatisfaction: 4.5,
        preventiveCareRate: 79,
        chronicDiseaseManagement: 83,
        timelyDocumentation: 79,
        trend: 'stable',
        rank: 7,
        previousRank: 6,
        awards: [],
      },
      {
        providerId: 'provider-current',
        providerName: 'Dr. Current User',
        specialty: 'Family Medicine',
        department: 'Primary Care',
        panelSize: 1534,
        qualityScore: 79,
        careGapClosureRate: 75,
        patientSatisfaction: 4.3,
        preventiveCareRate: 77,
        chronicDiseaseManagement: 81,
        timelyDocumentation: 84,
        trend: 'improving',
        rank: 8,
        previousRank: 10,
        awards: [],
      },
      {
        providerId: 'p9',
        providerName: 'Dr. David Kim',
        specialty: 'Pediatrics',
        department: 'Community Health',
        panelSize: 1923,
        qualityScore: 77,
        careGapClosureRate: 73,
        patientSatisfaction: 4.6,
        preventiveCareRate: 85,
        chronicDiseaseManagement: 71,
        timelyDocumentation: 76,
        trend: 'stable',
        rank: 9,
        previousRank: 9,
        awards: [],
      },
      {
        providerId: 'p10',
        providerName: 'Dr. Amanda Brown',
        specialty: 'Geriatrics',
        department: 'Specialty Care',
        panelSize: 756,
        qualityScore: 75,
        careGapClosureRate: 71,
        patientSatisfaction: 4.2,
        preventiveCareRate: 73,
        chronicDiseaseManagement: 79,
        timelyDocumentation: 71,
        trend: 'declining',
        rank: 10,
        previousRank: 8,
        awards: [],
      },
    ];

    return providers;
  }

  /**
   * Calculate summary statistics
   */
  private calculateSummaryStats(): void {
    if (this.providers.length === 0) return;

    // Top performer
    this.topPerformer = this.providers[0];

    // Org average
    const totalQuality = this.providers.reduce((sum, p) => sum + p.qualityScore, 0);
    this.orgAverageQuality = Math.round(totalQuality / this.providers.length);

    // Most improved (largest positive rank change)
    this.mostImproved = this.providers.reduce((best, current) => {
      const currentImprovement = current.previousRank - current.rank;
      const bestImprovement = best.previousRank - best.rank;
      return currentImprovement > bestImprovement ? current : best;
    }, this.providers[0]);

    this.improvementPercent = this.mostImproved
      ? Math.round(
          ((this.mostImproved.previousRank - this.mostImproved.rank) /
            this.mostImproved.previousRank) *
            100
        )
      : 0;
  }

  /**
   * Get sorted providers by category
   */
  getSortedProviders(category: LeaderboardCategory): ProviderPerformance[] {
    const sorted = [...this.providers];

    switch (category) {
      case 'quality':
        return sorted.sort((a, b) => b.qualityScore - a.qualityScore);
      case 'gap-closure':
        return sorted.sort((a, b) => b.careGapClosureRate - a.careGapClosureRate);
      case 'preventive':
        return sorted.sort((a, b) => b.preventiveCareRate - a.preventiveCareRate);
      case 'chronic':
        return sorted.sort(
          (a, b) => b.chronicDiseaseManagement - a.chronicDiseaseManagement
        );
      default:
        return sorted.sort((a, b) => a.rank - b.rank);
    }
  }

  /**
   * Get category label
   */
  getCategoryLabel(category: LeaderboardCategory): string {
    const labels: Record<LeaderboardCategory, string> = {
      overall: 'Quality',
      quality: 'Quality',
      'gap-closure': 'Gap Closure',
      preventive: 'Preventive',
      chronic: 'Chronic',
    };
    return labels[category];
  }

  /**
   * Get category value for a provider
   */
  getCategoryValue(
    provider: ProviderPerformance,
    category: LeaderboardCategory
  ): number {
    switch (category) {
      case 'quality':
        return provider.qualityScore;
      case 'gap-closure':
        return provider.careGapClosureRate;
      case 'preventive':
        return provider.preventiveCareRate;
      case 'chronic':
        return provider.chronicDiseaseManagement;
      default:
        return provider.qualityScore;
    }
  }

  /**
   * Get rank change class
   */
  getRankChangeClass(provider: ProviderPerformance): string {
    if (provider.rank < provider.previousRank) return 'up';
    if (provider.rank > provider.previousRank) return 'down';
    return 'same';
  }

  /**
   * Get avatar color from name
   */
  getAvatarColor(name: string): string {
    const colors = [
      '#1976d2',
      '#388e3c',
      '#f57c00',
      '#7b1fa2',
      '#c62828',
      '#00838f',
      '#5d4037',
    ];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  }

  /**
   * Get initials from name
   */
  getInitials(name: string): string {
    return name
      .split(' ')
      .filter((part) => part.length > 0)
      .map((part) => part[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  /**
   * Get award short name
   */
  getAwardShortName(award: string): string {
    const shortNames: Record<string, string> = {
      'Top Performer Q4': 'Top',
      'Patient Satisfaction Award': 'PSA',
      'Quality Champion': 'QC',
      'Care Gap Hero': 'CGH',
      'Most Improved': 'MI',
    };
    return shortNames[award] || award.substring(0, 3);
  }

  /**
   * Get trend icon
   */
  getTrendIcon(trend: string): string {
    const icons: Record<string, string> = {
      improving: 'trending_up',
      stable: 'trending_flat',
      declining: 'trending_down',
    };
    return icons[trend] || 'trending_flat';
  }

  /**
   * Get progress bar color
   */
  getProgressColor(value: number): 'primary' | 'accent' | 'warn' {
    if (value >= 85) return 'primary';
    if (value >= 70) return 'accent';
    return 'warn';
  }

  /**
   * View provider details
   */
  viewProviderDetails(provider: ProviderPerformance): void {
    this.logger.info('View provider details:', provider);
    // Would open a detailed provider performance dialog
  }

  /**
   * Compare provider with org average
   */
  compareWithOrg(provider: ProviderPerformance): void {
    this.logger.info('Compare with org:', provider);
  }

  /**
   * View provider's patient panel
   */
  viewPatientPanel(provider: ProviderPerformance): void {
    this.logger.info('View patient panel:', provider);
  }

  /**
   * View provider's care gaps
   */
  viewCareGaps(provider: ProviderPerformance): void {
    this.logger.info('View care gaps:', provider);
  }

  /**
   * Export provider report
   */
  exportProviderReport(provider: ProviderPerformance): void {
    const report = {
      exportDate: new Date().toISOString(),
      provider: provider,
    };

    const blob = new Blob([JSON.stringify(report, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `provider-report-${provider.providerName.replace(/\s+/g, '-')}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Export full leaderboard
   */
  exportLeaderboard(): void {
    const report = {
      exportDate: new Date().toISOString(),
      summary: {
        totalProviders: this.providers.length,
        orgAverageQuality: this.orgAverageQuality,
        topPerformer: this.topPerformer?.providerName,
        mostImproved: this.mostImproved?.providerName,
      },
      providers: this.providers,
    };

    const blob = new Blob([JSON.stringify(report, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `provider-leaderboard-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Print leaderboard
   */
  printLeaderboard(): void {
    window.print();
  }

  /**
   * Close dialog
   */
  close(): void {
    this.dialogRef.close();
  }
}
