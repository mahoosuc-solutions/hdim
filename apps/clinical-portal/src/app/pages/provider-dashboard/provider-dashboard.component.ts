import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  highRiskCount = 3;
  totalCareGaps = 12;
  dueThisWeek = 5;
  averageQualityScore = 78;

  // Sample data - in production, this would come from API
  priorityCareGaps: CareGap[] = [
    {
      patientId: 'patient-002',
      patientName: 'Jane Smith',
      mrn: 'MRN-0002',
      age: 58,
      gap: 'Blood Pressure Reading Overdue',
      measure: 'CBP - Controlling High Blood Pressure',
      priority: 'high',
      dueDate: new Date(2025, 10, 20), // Nov 20, 2025
      recommendation: 'Schedule BP check within next 7 days. Last reading 8 months ago was 145/95.',
      category: 'Hypertension'
    },
    {
      patientId: 'patient-003',
      patientName: 'Robert Johnson',
      mrn: 'MRN-0003',
      age: 72,
      gap: 'Diabetic Eye Exam Overdue',
      measure: 'CDC-EYE - Eye Exam for Diabetes',
      priority: 'high',
      dueDate: new Date(2025, 10, 15),
      recommendation: 'Refer to ophthalmology for dilated retinal exam. Last exam was 18 months ago.',
      category: 'Diabetes'
    },
    {
      patientId: 'patient-005',
      patientName: 'Michael Brown',
      mrn: 'MRN-0005',
      age: 62,
      gap: 'Cholesterol Screening Due',
      measure: 'COL - Cholesterol Management',
      priority: 'medium',
      dueDate: new Date(2025, 11, 1),
      recommendation: 'Order lipid panel. Patient has history of CAD.',
      category: 'Cardiovascular'
    }
  ];

  highRiskPatients: PatientRiskProfile[] = [
    {
      patientId: 'patient-003',
      patientName: 'Robert Johnson',
      mrn: 'MRN-0003',
      age: 72,
      riskLevel: 'high',
      activeConditions: ['Diabetes Type 2', 'Hypertension', 'CKD Stage 3', 'CAD'],
      openCareGaps: 4,
      lastVisit: new Date(2025, 9, 15),
      qualityScore: 65
    },
    {
      patientId: 'patient-002',
      patientName: 'Jane Smith',
      mrn: 'MRN-0002',
      age: 58,
      riskLevel: 'high',
      activeConditions: ['Hypertension', 'Hyperlipidemia', 'Obesity'],
      openCareGaps: 3,
      lastVisit: new Date(2025, 10, 1),
      qualityScore: 72
    },
    {
      patientId: 'patient-005',
      patientName: 'Michael Brown',
      mrn: 'MRN-0005',
      age: 62,
      riskLevel: 'high',
      activeConditions: ['CAD', 'Heart Failure', 'AFib'],
      openCareGaps: 5,
      lastVisit: new Date(2025, 9, 28),
      qualityScore: 58
    }
  ];

  ngOnInit(): void {
    // In production, load data from API
    this.refreshData();
  }

  refreshData(): void {
    this.loading = true;
    // Simulate API call
    setTimeout(() => {
      this.loading = false;
    }, 1000);
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
    console.log('Schedule appointment for:', gap.patientName);
    // Implement scheduling logic
  }

  orderTest(gap: CareGap): void {
    console.log('Order test for:', gap.patientName);
    // Implement test ordering logic
  }

  viewPatient(patientId: string): void {
    console.log('View patient:', patientId);
    // Navigate to patient details
  }
}
