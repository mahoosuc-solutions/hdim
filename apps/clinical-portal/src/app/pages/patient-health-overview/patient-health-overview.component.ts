/**
 * Patient Health Overview Component
 *
 * Comprehensive dashboard showing patient's overall health status
 * including physical, mental, and social health metrics
 */

import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PatientHealthService } from '../../services/patient-health.service';
import {
  PatientHealthOverview,
  HealthScore,
  RiskLevel,
  HealthStatus,
  CareGap,
  CareRecommendation,
} from '../../models/patient-health.model';
import {
  CriticalAlertBannerComponent,
  CriticalAlert,
} from '../../shared/components/critical-alert-banner/critical-alert-banner.component';

@Component({
  selector: 'app-patient-health-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatDividerModule,
    MatTableModule,
    MatExpansionModule,
    MatBadgeModule,
    CriticalAlertBannerComponent,
  ],
  templateUrl: './patient-health-overview.component.html',
  styleUrls: ['./patient-health-overview.component.scss'],
})
export class PatientHealthOverviewComponent implements OnInit, OnDestroy {
  @Input() patientId!: string;

  private destroy$ = new Subject<void>();

  healthOverview: PatientHealthOverview | null = null;
  loading = true;
  error: string | null = null;
  criticalAlerts: CriticalAlert[] = [];

  // Table columns
  careGapColumns: string[] = ['priority', 'title', 'category', 'actions'];
  recommendationColumns: string[] = ['priority', 'title', 'category', 'actions'];

  constructor(private healthService: PatientHealthService) {}

  ngOnInit(): void {
    if (!this.patientId) {
      this.error = 'No patient ID provided';
      this.loading = false;
      return;
    }

    this.loadHealthOverview();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadHealthOverview(): void {
    this.loading = true;
    this.error = null;

    this.healthService.getPatientHealthOverview(this.patientId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (overview) => {
        this.healthOverview = overview;
        this.generateCriticalAlerts(overview);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading health overview:', err);
        this.error = 'Failed to load patient health overview';
        this.loading = false;
      },
    });
  }

  /**
   * Generate critical alerts based on patient health data
   */
  private generateCriticalAlerts(overview: PatientHealthOverview): void {
    const alerts: CriticalAlert[] = [];

    // 1. Suicide Risk Alert (CRITICAL - highest priority)
    if (overview.mentalHealth?.suicideRisk) {
      const suicideRisk = overview.mentalHealth.suicideRisk;
      if (suicideRisk.level === 'high' || suicideRisk.level === 'critical') {
        const severity = suicideRisk.level === 'critical' ? 'critical' : 'high';
        const lastAssessedDate = suicideRisk.lastAssessed ? new Date(suicideRisk.lastAssessed) : undefined;

        alerts.push({
          id: `suicide-risk-${this.patientId}`,
          type: 'suicide-risk',
          severity,
          title: `${severity.toUpperCase()} Suicide Risk Detected`,
          description: `Patient has ${suicideRisk.level} suicide risk. ${
            suicideRisk.factors?.length
              ? 'Risk factors: ' + suicideRisk.factors.slice(0, 3).map(f => f.factor).join(', ')
              : ''
          }`,
          actionLabel: 'Crisis Protocol',
          actionIcon: 'emergency',
          timestamp: lastAssessedDate,
          metadata: {
            'Last Assessed': lastAssessedDate ? this.formatDate(lastAssessedDate) : 'Unknown',
            'Risk Factors': suicideRisk.factors?.length || 0,
          },
        });
      }
    }

    // 2. Substance Use Alert (HIGH priority if not in treatment)
    if (overview.mentalHealth?.substanceUse) {
      const substanceUse = overview.mentalHealth.substanceUse;
      const hasHighRisk = substanceUse.overallRisk === 'high' || substanceUse.overallRisk === 'critical';
      const untreatedSubstances = substanceUse.substances?.filter(
        (s) => (s.severity === 'moderate' || s.severity === 'severe') && !s.inTreatment
      );

      if (hasHighRisk && untreatedSubstances && untreatedSubstances.length > 0) {
        alerts.push({
          id: `substance-use-${this.patientId}`,
          type: 'substance-use',
          severity: 'high',
          title: 'High-Risk Substance Use - Not in Treatment',
          description: `Patient has ${untreatedSubstances.length} untreated substance use issue(s): ${untreatedSubstances.map(s => s.substance).join(', ')}. Immediate intervention recommended.`,
          actionLabel: 'Refer to Treatment',
          actionIcon: 'local_hospital',
        });
      }
    }

    // 3. Urgent Care Gaps (HIGH priority if overdue >180 days)
    if (overview.careGaps && overview.careGaps.length > 0) {
      const urgentGaps = overview.careGaps.filter(
        (gap) => gap.priority === 'urgent' && gap.overdueDays && gap.overdueDays > 180
      );

      if (urgentGaps.length > 0) {
        const topGap = urgentGaps[0];
        alerts.push({
          id: `care-gap-${topGap.id}`,
          type: 'care-gap',
          severity: 'high',
          title: `Urgent Care Gap: ${topGap.title}`,
          description: `${topGap.description || topGap.title} is ${topGap.overdueDays} days overdue. Immediate action required.`,
          actionLabel: 'Schedule Now',
          actionIcon: 'event',
          metadata: {
            'Days Overdue': topGap.overdueDays,
            'Category': topGap.category,
          },
        });
      }
    }

    // 4. Critical Vital Signs
    if (overview.physicalHealth?.vitals) {
      const vitals = overview.physicalHealth.vitals;
      const criticalVitals: string[] = [];

      if (vitals.bloodPressure?.status === 'critical') {
        criticalVitals.push(`BP: ${vitals.bloodPressure.value}`);
      }
      if (vitals.heartRate?.status === 'critical') {
        criticalVitals.push(`HR: ${vitals.heartRate.value}`);
      }
      if (vitals.temperature?.status === 'critical') {
        criticalVitals.push(`Temp: ${vitals.temperature.value}`);
      }

      if (criticalVitals.length > 0) {
        alerts.push({
          id: `vital-signs-${this.patientId}`,
          type: 'vital-sign',
          severity: 'critical',
          title: 'Critical Vital Signs',
          description: `Critical vital signs detected: ${criticalVitals.join(', ')}. Immediate assessment required.`,
          actionLabel: 'Document',
          actionIcon: 'edit_note',
        });
      }
    }

    // 5. Critical Lab Results
    if (overview.physicalHealth?.labs) {
      const criticalLabs = overview.physicalHealth.labs.filter(
        (lab) => lab.status === 'critical'
      );

      if (criticalLabs.length > 0) {
        const topLab = criticalLabs[0];
        const labName = topLab.code.text || topLab.code.coding?.[0]?.display || 'Unknown Lab';
        const labCode = topLab.code.coding?.[0]?.code || 'unknown';
        const refRange = topLab.referenceRange?.text ||
          (topLab.referenceRange?.low && topLab.referenceRange?.high
            ? `${topLab.referenceRange.low}-${topLab.referenceRange.high}`
            : 'N/A');

        alerts.push({
          id: `lab-result-${labCode}`,
          type: 'lab-result',
          severity: 'critical',
          title: `Critical Lab Result: ${labName}`,
          description: `${labName}: ${topLab.value} ${topLab.unit || ''} (Normal: ${refRange}). Immediate review required.`,
          actionLabel: 'Review Lab',
          actionIcon: 'biotech',
          timestamp: topLab.date ? new Date(topLab.date) : undefined,
        });
      }
    }

    this.criticalAlerts = alerts;
  }

  /**
   * Handle alert action button clicks
   */
  onAlertAction(alert: CriticalAlert): void {
    console.log('Alert action triggered:', alert);
    // TODO: Implement specific actions based on alert type
    // e.g., navigate to crisis protocol, open scheduling dialog, etc.
  }

  /**
   * Handle alert dismissal
   */
  onAlertDismiss(alert: CriticalAlert): void {
    console.log('Alert dismissed:', alert);
    // TODO: Implement dismissal logic (requires documentation/reason)
    this.criticalAlerts = this.criticalAlerts.filter((a) => a.id !== alert.id);
  }

  // Helper methods for display

  getHealthScoreColor(score: number | undefined): string {
    if (score === undefined) return '#9e9e9e'; // gray for unknown
    if (score >= 85) return '#4caf50'; // green
    if (score >= 70) return '#8bc34a'; // light green
    if (score >= 50) return '#ff9800'; // orange
    return '#f44336'; // red
  }

  getHealthStatusIcon(status: HealthStatus): string {
    switch (status) {
      case 'excellent': return 'sentiment_very_satisfied';
      case 'good': return 'sentiment_satisfied';
      case 'fair': return 'sentiment_neutral';
      case 'poor': return 'sentiment_dissatisfied';
      default: return 'help_outline';
    }
  }

  getHealthStatusColor(status: HealthStatus): string {
    switch (status) {
      case 'excellent': return '#4caf50';
      case 'good': return '#8bc34a';
      case 'fair': return '#ff9800';
      case 'poor': return '#f44336';
      default: return '#9e9e9e';
    }
  }

  getRiskLevelIcon(risk: RiskLevel): string {
    switch (risk) {
      case 'low': return 'check_circle';
      case 'moderate': return 'warning';
      case 'high': return 'error';
      case 'critical': return 'emergency';
    }
  }

  getRiskLevelColor(risk: RiskLevel): string {
    switch (risk) {
      case 'low': return '#4caf50';
      case 'moderate': return '#ff9800';
      case 'high': return '#f44336';
      case 'critical': return '#d32f2f';
    }
  }

  getTrendIcon(trend: 'improving' | 'stable' | 'declining' | 'unknown'): string {
    switch (trend) {
      case 'improving': return 'trending_up';
      case 'stable': return 'trending_flat';
      case 'declining': return 'trending_down';
      default: return 'help_outline';
    }
  }

  getTrendColor(trend: 'improving' | 'stable' | 'declining' | 'unknown'): string {
    switch (trend) {
      case 'improving': return '#4caf50';
      case 'stable': return '#2196f3';
      case 'declining': return '#f44336';
      default: return '#9e9e9e';
    }
  }

  getPriorityColor(priority: 'low' | 'medium' | 'high' | 'urgent'): string {
    switch (priority) {
      case 'low': return '#4caf50';
      case 'medium': return '#ff9800';
      case 'high': return '#f44336';
      case 'urgent': return '#d32f2f';
    }
  }

  getPriorityIcon(priority: 'low' | 'medium' | 'high' | 'urgent'): string {
    switch (priority) {
      case 'low': return 'flag';
      case 'medium': return 'flag';
      case 'high': return 'priority_high';
      case 'urgent': return 'emergency';
    }
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  getVitalStatus(status: 'normal' | 'abnormal' | 'critical'): { icon: string; color: string } {
    switch (status) {
      case 'normal':
        return { icon: 'check_circle', color: '#4caf50' };
      case 'abnormal':
        return { icon: 'warning', color: '#ff9800' };
      case 'critical':
        return { icon: 'error', color: '#f44336' };
    }
  }

  getMedicationAdherenceColor(rate: number): string {
    if (rate >= 80) return '#4caf50';
    if (rate >= 60) return '#ff9800';
    return '#f44336';
  }

  getAssessmentSeverityColor(
    severity: 'none' | 'minimal' | 'mild' | 'moderate' | 'moderately-severe' | 'severe'
  ): string {
    switch (severity) {
      case 'none':
      case 'minimal':
        return '#4caf50';
      case 'mild':
        return '#8bc34a';
      case 'moderate':
        return '#ff9800';
      case 'moderately-severe':
        return '#ff5722';
      case 'severe':
        return '#f44336';
    }
  }

  getCareGapsByPriority(): CareGap[] {
    if (!this.healthOverview) return [];
    return [...this.healthOverview.careGaps].sort((a, b) => {
      const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
      return priorityOrder[a.priority] - priorityOrder[b.priority];
    });
  }

  getRecommendationsByPriority(): CareRecommendation[] {
    if (!this.healthOverview) return [];
    return [...this.healthOverview.recommendations].sort((a, b) => {
      const priorityOrder: Record<string, number> = { high: 0, medium: 1, low: 2 };
      const aPriority = a.priority || 'low';
      const bPriority = b.priority || 'low';
      return (priorityOrder[aPriority] ?? 2) - (priorityOrder[bPriority] ?? 2);
    });
  }

  getUrgentCareGapsCount(): number {
    if (!this.healthOverview) return 0;
    return this.healthOverview.careGaps.filter(
      (gap) => gap.priority === 'urgent' || gap.priority === 'high'
    ).length;
  }

  getChronicConditionIcon(display: string): string {
    const lower = display.toLowerCase();
    if (lower.includes('diabetes')) return 'local_hospital';
    if (lower.includes('hypertension') || lower.includes('blood pressure')) return 'favorite';
    if (lower.includes('asthma') || lower.includes('copd')) return 'air';
    if (lower.includes('arthritis')) return 'accessibility';
    return 'medical_services';
  }

  getMentalHealthCategoryIcon(category: string): string {
    switch (category) {
      case 'mood': return 'psychology';
      case 'anxiety': return 'psychology_alt';
      case 'substance': return 'local_pharmacy';
      case 'trauma': return 'healing';
      default: return 'mental_health';
    }
  }

  getSDOHCategoryIcon(category: string): string {
    switch (category) {
      case 'food-insecurity': return 'restaurant';
      case 'housing-instability': return 'home';
      case 'transportation': return 'directions_car';
      case 'utility-assistance': return 'bolt';
      case 'interpersonal-safety': return 'shield';
      case 'education': return 'school';
      case 'employment': return 'work';
      case 'social-isolation': return 'people';
      case 'financial-strain': return 'attach_money';
      default: return 'help_outline';
    }
  }

  formatCategory(category: string): string {
    return category.replace(/-/g, ' ');
  }
}
