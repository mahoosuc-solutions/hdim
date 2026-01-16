import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { CareGapService, CareGapApiItem, GapPriority } from '../../services/care-gap.service';
import { PatientService } from '../../services/patient.service';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

interface CareGap {
  patientId: string;
  patientName: string;
  mrn: string;
  age: number;
  gap: string;
  measure: string;
  priority: 'high' | 'medium' | 'low';
  dueDate: Date;
  recommendation: string;
  category: string;
}

interface PatientRiskProfile {
  patientId: string;
  patientName: string;
  mrn: string;
  age: number;
  riskLevel: 'high' | 'medium' | 'low';
  activeConditions: string[];
  openCareGaps: number;
  lastVisit: Date;
  nextAppointment?: Date;
  qualityScore: number;
}

@Component({
  selector: 'app-provider-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatBadgeModule,
    MatTooltipModule,
    MatDividerModule,
    MatTableModule,
    LoadingButtonComponent,
    StatCardComponent
  ],
  template: `
    <div class="provider-dashboard">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">
            <mat-icon>medical_services</mat-icon>
            Provider Dashboard
          </h1>
          <p class="page-subtitle">Patient care gaps and quality insights</p>
        </div>
        <div class="header-actions">
          <app-loading-button
            text="Refresh Data"
            icon="refresh"
            variant="raised"
            color="primary"
            [loading]="loading"
            (buttonClick)="refreshData()">
          </app-loading-button>
        </div>
      </div>

      <!-- Quick Stats -->
      <div class="stats-grid">
        <app-stat-card
          title="High-Risk Patients"
          [value]="highRiskCount.toString()"
          icon="warning"
          iconClass="high-risk-icon"
          color="warn"
          [tooltip]="'Patients with multiple open care gaps'">
        </app-stat-card>

        <app-stat-card
          title="Open Care Gaps"
          [value]="totalCareGaps.toString()"
          icon="flag"
          iconClass="care-gap-icon"
          color="accent"
          [tooltip]="'Total actionable care gaps requiring attention'">
        </app-stat-card>

        <app-stat-card
          title="Due This Week"
          [value]="dueThisWeek.toString()"
          icon="event"
          iconClass="due-icon"
          color="primary"
          [tooltip]="'Care gaps due within the next 7 days'">
        </app-stat-card>

        <app-stat-card
          title="Quality Score"
          [value]="(averageQualityScore | number: '1.0-0') + '%'"
          icon="stars"
          iconClass="quality-icon"
          [color]="averageQualityScore >= 80 ? 'primary' : 'warn'"
          [tooltip]="'Average quality measure compliance rate'">
        </app-stat-card>
      </div>

      <!-- Priority Care Gaps -->
      <mat-card class="care-gaps-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>priority_high</mat-icon>
            Priority Care Gaps Requiring Action
          </mat-card-title>
          <mat-card-subtitle>Sorted by priority and due date</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="care-gaps-list">
            <div *ngFor="let gap of priorityCareGaps" class="care-gap-item"
                 [class.high-priority]="gap.priority === 'high'"
                 [class.medium-priority]="gap.priority === 'medium'"
                 [class.low-priority]="gap.priority === 'low'">

              <div class="gap-header">
                <div class="patient-info">
                  <span class="patient-name">{{ gap.patientName }}</span>
                  <span class="patient-mrn">MRN: {{ gap.mrn }}</span>
                  <mat-chip class="age-chip">Age {{ gap.age }}</mat-chip>
                </div>
                <mat-chip [class]="'priority-chip priority-' + gap.priority">
                  {{ gap.priority | uppercase }} PRIORITY
                </mat-chip>
              </div>

              <div class="gap-content">
                <div class="gap-details">
                  <div class="gap-title">
                    <mat-icon>warning</mat-icon>
                    <strong>{{ gap.gap }}</strong>
                  </div>
                  <div class="gap-measure">
                    <mat-icon>assessment</mat-icon>
                    Measure: {{ gap.measure }}
                  </div>
                  <div class="gap-recommendation">
                    <mat-icon>lightbulb</mat-icon>
                    {{ gap.recommendation }}
                  </div>
                </div>

                <div class="gap-meta">
                  <div class="gap-category">
                    <mat-chip>{{ gap.category }}</mat-chip>
                  </div>
                  <div class="gap-due-date"
                       [class.overdue]="isOverdue(gap.dueDate)"
                       [class.due-soon]="isDueSoon(gap.dueDate)">
                    <mat-icon>event</mat-icon>
                    Due: {{ gap.dueDate | date: 'MMM d, y' }}
                  </div>
                </div>
              </div>

              <div class="gap-actions">
                <app-loading-button
                  text="Schedule"
                  icon="calendar_today"
                  variant="raised"
                  color="primary"
                  (buttonClick)="scheduleAppointment(gap)">
                </app-loading-button>
                <app-loading-button
                  text="Order Test"
                  icon="science"
                  variant="raised"
                  (buttonClick)="orderTest(gap)">
                </app-loading-button>
                <app-loading-button
                  text="View Patient"
                  icon="person"
                  variant="stroked"
                  (buttonClick)="viewPatient(gap.patientId)">
                </app-loading-button>
              </div>
            </div>

            <div *ngIf="priorityCareGaps.length === 0" class="no-gaps">
              <mat-icon>check_circle</mat-icon>
              <p>Great work! No high-priority care gaps at this time.</p>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- High-Risk Patient Panel -->
      <mat-card class="risk-panel-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>health_and_safety</mat-icon>
            High-Risk Patient Panel
          </mat-card-title>
          <mat-card-subtitle>Patients requiring close monitoring</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="risk-patients-grid">
            <div *ngFor="let patient of highRiskPatients" class="risk-patient-card">
              <div class="patient-header">
                <div class="patient-identity">
                  <h3>{{ patient.patientName }}</h3>
                  <span class="patient-mrn">{{ patient.mrn }}</span>
                </div>
                <mat-chip class="risk-badge risk-high">
                  <mat-icon>warning</mat-icon>
                  HIGH RISK
                </mat-chip>
              </div>

              <mat-divider></mat-divider>

              <div class="patient-details">
                <div class="detail-row">
                  <mat-icon>cake</mat-icon>
                  <span>Age {{ patient.age }}</span>
                </div>
                <div class="detail-row">
                  <mat-icon>medical_information</mat-icon>
                  <span>{{ patient.activeConditions.length }} Active Conditions</span>
                </div>
                <div class="detail-row">
                  <mat-icon>flag</mat-icon>
                  <span class="care-gaps-count">{{ patient.openCareGaps }} Open Care Gaps</span>
                </div>
                <div class="detail-row">
                  <mat-icon>event</mat-icon>
                  <span>Last Visit: {{ patient.lastVisit | date: 'MMM d' }}</span>
                </div>
                <div class="detail-row">
                  <mat-icon>score</mat-icon>
                  <span>Quality Score: {{ patient.qualityScore }}%</span>
                </div>
              </div>

              <div class="conditions-tags">
                <mat-chip *ngFor="let condition of patient.activeConditions.slice(0, 3)">
                  {{ condition }}
                </mat-chip>
                <mat-chip *ngIf="patient.activeConditions.length > 3">
                  +{{ patient.activeConditions.length - 3 }} more
                </mat-chip>
              </div>

              <div class="patient-actions">
                <app-loading-button
                  text="View Chart"
                  icon="description"
                  variant="raised"
                  color="primary"
                  (buttonClick)="viewPatient(patient.patientId)">
                </app-loading-button>
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Quick Actions -->
      <mat-card class="quick-actions-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>bolt</mat-icon>
            Quick Actions
          </mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="quick-actions-grid">
            <button mat-raised-button color="primary" class="action-button">
              <mat-icon>group</mat-icon>
              <div class="action-content">
                <span class="action-title">My Patient Panel</span>
                <span class="action-subtitle">View all assigned patients</span>
              </div>
            </button>

            <button mat-raised-button class="action-button">
              <mat-icon>calendar_month</mat-icon>
              <div class="action-content">
                <span class="action-title">Today's Schedule</span>
                <span class="action-subtitle">View appointments</span>
              </div>
            </button>

            <button mat-raised-button class="action-button">
              <mat-icon>pending_actions</mat-icon>
              <div class="action-content">
                <span class="action-title">Pending Orders</span>
                <span class="action-subtitle">Review lab results</span>
              </div>
            </button>

            <button mat-raised-button class="action-button">
              <mat-icon>assessment</mat-icon>
              <div class="action-content">
                <span class="action-title">Quality Reports</span>
                <span class="action-subtitle">Generate reports</span>
              </div>
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .provider-dashboard {
      padding: 24px;
      max-width: 1600px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
    }

    .header-content h1 {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 32px;
      font-weight: 500;
    }

    .header-content mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: #1976d2;
    }

    .page-subtitle {
      margin: 8px 0 0 48px;
      color: #666;
      font-size: 16px;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 24px;
    }

    .care-gaps-card,
    .risk-panel-card,
    .quick-actions-card {
      margin-bottom: 24px;
    }

    mat-card-header mat-card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
    }

    mat-card-header mat-icon {
      color: #1976d2;
    }

    /* Care Gap Items */
    .care-gaps-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .care-gap-item {
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      padding: 16px;
      transition: all 0.3s ease;
    }

    .care-gap-item:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      transform: translateY(-2px);
    }

    .care-gap-item.high-priority {
      border-left: 4px solid #f44336;
      background: #ffebee;
    }

    .care-gap-item.medium-priority {
      border-left: 4px solid #ff9800;
      background: #fff3e0;
    }

    .care-gap-item.low-priority {
      border-left: 4px solid #4caf50;
      background: #f1f8e9;
    }

    .gap-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .patient-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .patient-name {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }

    .patient-mrn {
      color: #666;
      font-size: 14px;
    }

    .age-chip {
      background: #e3f2fd;
      color: #1976d2;
      font-weight: 500;
    }

    .priority-chip {
      font-weight: 600;
    }

    .priority-chip.priority-high {
      background: #f44336;
      color: white;
    }

    .priority-chip.priority-medium {
      background: #ff9800;
      color: white;
    }

    .priority-chip.priority-low {
      background: #4caf50;
      color: white;
    }

    .gap-content {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 16px;
    }

    .gap-details {
      flex: 1;
    }

    .gap-title,
    .gap-measure,
    .gap-recommendation {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin-bottom: 8px;
    }

    .gap-title mat-icon {
      color: #f44336;
    }

    .gap-measure mat-icon {
      color: #1976d2;
    }

    .gap-recommendation mat-icon {
      color: #ff9800;
    }

    .gap-meta {
      display: flex;
      flex-direction: column;
      gap: 8px;
      align-items: flex-end;
    }

    .gap-due-date {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 12px;
      border-radius: 4px;
      background: #e0e0e0;
      font-weight: 500;
    }

    .gap-due-date.overdue {
      background: #f44336;
      color: white;
    }

    .gap-due-date.due-soon {
      background: #ff9800;
      color: white;
    }

    .gap-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .no-gaps {
      text-align: center;
      padding: 48px 24px;
      color: #666;
    }

    .no-gaps mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #4caf50;
    }

    /* High-Risk Patients */
    .risk-patients-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 20px;
    }

    .risk-patient-card {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 16px;
      transition: all 0.3s ease;
    }

    .risk-patient-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      border-color: #f44336;
    }

    .patient-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
    }

    .patient-identity h3 {
      margin: 0;
      font-size: 18px;
      color: #333;
    }

    .risk-badge {
      font-weight: 600;
    }

    .risk-badge.risk-high {
      background: #f44336;
      color: white;
    }

    .risk-badge mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .patient-details {
      margin: 16px 0;
    }

    .detail-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 0;
      color: #666;
    }

    .detail-row mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: #1976d2;
    }

    .care-gaps-count {
      color: #f44336;
      font-weight: 600;
    }

    .conditions-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 16px 0;
    }

    .patient-actions {
      margin-top: 16px;
    }

    /* Quick Actions */
    .quick-actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
    }

    .action-button {
      height: auto;
      padding: 16px;
      text-align: left;
    }

    .action-button mat-icon {
      margin-right: 12px;
      font-size: 32px;
      width: 32px;
      height: 32px;
    }

    .action-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .action-title {
      font-size: 16px;
      font-weight: 600;
    }

    .action-subtitle {
      font-size: 12px;
      opacity: 0.8;
    }

    @media (max-width: 768px) {
      .provider-dashboard {
        padding: 16px;
      }

      .stats-grid,
      .risk-patients-grid,
      .quick-actions-grid {
        grid-template-columns: 1fr;
      }

      .gap-content {
        flex-direction: column;
      }

      .gap-meta {
        align-items: flex-start;
      }
    }
  `]
})
export class ProviderDashboardComponent implements OnInit {
  loading = false;

  // Quick stats
  highRiskCount = 0;
  totalCareGaps = 0;
  dueThisWeek = 0;
  averageQualityScore = 0;

  priorityCareGaps: CareGap[] = [];
  highRiskPatients: PatientRiskProfile[] = [];

  constructor(
    private careGapService: CareGapService,
    private patientService: PatientService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refreshData();
  }

  refreshData(): void {
    this.loading = true;
    forkJoin({
      careGapPage: this.careGapService.getCareGapsPage({ size: 200 }),
      patients: this.patientService.getPatientsSummaryCached(),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe(({ careGapPage, patients }) => {
        this.applyCareGapData(careGapPage.content, patients);
      });
  }

  private applyCareGapData(gaps: CareGapApiItem[], patients: Array<{ id: string; fullName: string; mrn?: string; age?: number }>): void {
    const patientById = new Map(patients.map((p) => [p.id, p]));
    const gapByPatient = new Map<string, CareGapApiItem[]>();

    gaps.forEach((gap) => {
      const list = gapByPatient.get(gap.patientId) || [];
      list.push(gap);
      gapByPatient.set(gap.patientId, list);
    });

    const now = new Date();
    const sortedGaps = [...gaps].sort((a, b) => {
      const priorityRank = this.getPriorityRank(b.priority) - this.getPriorityRank(a.priority);
      if (priorityRank !== 0) return priorityRank;
      const dueA = this.parseGapDate(a.dueDate)?.getTime() ?? 0;
      const dueB = this.parseGapDate(b.dueDate)?.getTime() ?? 0;
      return dueA - dueB;
    });

    this.priorityCareGaps = sortedGaps.slice(0, 6).map((gap) => {
      const patient = patientById.get(gap.patientId);
      return {
        patientId: gap.patientId,
        patientName: patient?.fullName || gap.patientId,
        mrn: patient?.mrn || 'N/A',
        age: patient?.age ?? 0,
        gap: gap.gapDescription || gap.measureName || gap.measureId,
        measure: `${gap.measureId} - ${gap.measureName}`,
        priority: this.mapPriority(gap.priority),
        dueDate: this.parseGapDate(gap.dueDate) || now,
        recommendation: 'Review patient chart and schedule recommended service.',
        category: this.mapCategoryLabel(gap),
      };
    });

    const openGaps = gaps.filter((gap) => (gap.gapStatus || 'OPEN') === 'OPEN');
    this.totalCareGaps = openGaps.length;

    this.dueThisWeek = openGaps.filter((gap) => {
      const dueDate = this.parseGapDate(gap.dueDate);
      if (!dueDate) return false;
      const daysUntil = Math.ceil((dueDate.getTime() - now.getTime()) / 86400000);
      return daysUntil <= 7;
    }).length;

    const patientGapCounts = patients.map((patient) => {
      const count = gapByPatient.get(patient.id)?.length || 0;
      return { patient, count };
    });

    this.highRiskCount = patientGapCounts.filter((entry) => entry.count >= 2).length;
    this.highRiskPatients = patientGapCounts
      .filter((entry) => entry.count > 0)
      .sort((a, b) => b.count - a.count)
      .slice(0, 3)
      .map((entry) => {
        const patientGaps = gapByPatient.get(entry.patient.id) || [];
        return {
          patientId: entry.patient.id,
          patientName: entry.patient.fullName,
          mrn: entry.patient.mrn || 'N/A',
          age: entry.patient.age ?? 0,
          riskLevel: 'high',
          activeConditions: this.deriveConditions(patientGaps),
          openCareGaps: entry.count,
          lastVisit: this.latestIdentifiedDate(patientGaps) || now,
          qualityScore: this.calculateQualityScore(entry.count),
        };
      });

    const scores = patientGapCounts.map((entry) => this.calculateQualityScore(entry.count));
    this.averageQualityScore = scores.length
      ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
      : 0;
  }

  private calculateQualityScore(openGaps: number): number {
    return Math.max(0, 100 - openGaps * 5);
  }

  private latestIdentifiedDate(gaps: CareGapApiItem[]): Date | null {
    const timestamps = gaps
      .map((gap) => this.parseEpochSeconds(gap.identifiedDate))
      .filter((value): value is Date => Boolean(value));
    if (!timestamps.length) return null;
    return new Date(Math.max(...timestamps.map((date) => date.getTime())));
  }

  private parseEpochSeconds(value?: number): Date | null {
    if (!value) return null;
    return new Date(value * 1000);
  }

  private parseGapDate(value?: number[] | string): Date | null {
    if (Array.isArray(value) && value.length >= 3) {
      return new Date(value[0], value[1] - 1, value[2]);
    }
    if (typeof value === 'string') {
      const parsed = new Date(value);
      return isNaN(parsed.getTime()) ? null : parsed;
    }
    return null;
  }

  private getPriorityRank(priority?: string): number {
    switch (priority) {
      case 'HIGH':
      case GapPriority.HIGH:
        return 3;
      case 'MEDIUM':
      case GapPriority.MEDIUM:
        return 2;
      case 'LOW':
      case GapPriority.LOW:
        return 1;
      default:
        return 0;
    }
  }

  private mapPriority(priority?: string): 'high' | 'medium' | 'low' {
    switch (priority) {
      case 'HIGH':
      case GapPriority.HIGH:
        return 'high';
      case 'MEDIUM':
      case GapPriority.MEDIUM:
        return 'medium';
      default:
        return 'low';
    }
  }

  private mapCategoryLabel(gap: CareGapApiItem): string {
    if (gap.measureName) return gap.measureName;
    return gap.gapCategory || 'Preventive';
  }

  private deriveConditions(gaps: CareGapApiItem[]): string[] {
    const conditions = new Set<string>();
    gaps.forEach((gap) => {
      switch (gap.measureId) {
        case 'CDC':
        case 'EED':
        case 'KED':
          conditions.add('Diabetes');
          break;
        case 'SPC':
        case 'BPD':
          conditions.add('Cardiovascular');
          break;
        case 'DSF':
          conditions.add('Behavioral Health');
          break;
        default:
          conditions.add('Preventive Care');
      }
    });
    return Array.from(conditions);
  }

  isOverdue(dueDate: Date): boolean {
    return dueDate < new Date();
  }

  isDueSoon(dueDate: Date): boolean {
    const weekFromNow = new Date();
    weekFromNow.setDate(weekFromNow.getDate() + 7);
    return dueDate >= new Date() && dueDate <= weekFromNow;
  }

  scheduleAppointment(gap: CareGap): void {
    this.router.navigate(['/appointments'], {
      queryParams: { patientId: gap.patientId, source: 'provider-dashboard' }
    });
  }

  orderTest(gap: CareGap): void {
    this.router.navigate(['/orders'], {
      queryParams: { patientId: gap.patientId, source: 'provider-dashboard' }
    });
  }

  viewPatient(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }
}
