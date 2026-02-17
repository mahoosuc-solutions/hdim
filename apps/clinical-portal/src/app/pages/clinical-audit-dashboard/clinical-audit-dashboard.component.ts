import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { LoggerService } from '../../services/logger.service';
import { AuditService } from '../../services/audit.service';

/**
 * Clinical Audit Dashboard
 * 
 * Clinical-focused audit interface for physicians, nurses, and clinical staff:
 * - AI clinical decision support recommendations
 * - Medication optimization suggestions
 * - Care gap identification alerts
 * - Risk stratification decisions
 * - Clinical workflow audit trail
 * - Patient safety events
 * 
 * Role-based views for CLINICAL_PHYSICIAN, CLINICAL_NURSE, PROVIDER roles.
 */
@Component({
  selector: 'app-clinical-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clinical-audit-dashboard.component.html',
  styleUrls: ['./clinical-audit-dashboard.component.scss']
})
export class ClinicalAuditDashboardComponent implements OnInit, OnDestroy {

  // Contextual logger  // User role (determines visible features)
  userRole: 'CLINICAL_PHYSICIAN' | 'CLINICAL_NURSE' | 'PROVIDER' = 'CLINICAL_PHYSICIAN';
  
  // Clinical AI Decisions
  clinicalDecisions: ClinicalAIDecision[] = [];
  medicationRecommendations: MedicationRecommendation[] = [];
  careGapAlerts: CareGapAlert[] = [];
  riskStratifications: RiskStratification[] = [];
  
  // Clinical Metrics
  clinicalMetrics = {
    totalClinicalDecisions: 0,
    decisionsAccepted: 0,
    decisionsRejected: 0,
    decisionsModified: 0,
    averageResponseTime: 0,
    highPriorityAlerts: 0,
    patientSafetyEvents: 0
  };
  
  // Care quality metrics
  careQualityMetrics = {
    careGapsIdentified: 0,
    careGapsClosed: 0,
    medicationOptimizations: 0,
    preventableReadmissions: 0,
    qualityMeasureCompliance: 0
  };
  
  // Filters
  filterPriority = 'all';
  filterStatus = 'pending';
  filterPatient = '';
  filterDateRange = 'last-7-days';
  filterDecisionType = 'all';
  
  // Active tabs
  activeTab: 'decisions' | 'medications' | 'care-gaps' | 'risk' | 'metrics' = 'decisions';
  
  // Selected decision for review
  selectedDecision: ClinicalAIDecision | null = null;
  clinicalNotes = '';
  
  // Auto-refresh
  private refreshSubscription?: Subscription;
  autoRefreshEnabled = true;

  constructor(
    private logger: LoggerService,
    private auditService: AuditService
  ) {}

  ngOnInit(): void {
    this.loadClinicalDecisions();
    this.loadClinicalMetrics();
    
    if (this.autoRefreshEnabled) {
      this.refreshSubscription = interval(30000).subscribe(() => {
        this.refreshData();
      });
    }
  }
  
  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }
  
  /**
   * Load clinical AI decisions
   */
  loadClinicalDecisions(): void {
    this.logger.info('Loading clinical decisions');

    const filters = {
      agentType: this.filterDecisionType !== 'all' ? this.filterDecisionType : 'CLINICAL_',
      priority: this.filterPriority !== 'all' ? this.filterPriority : undefined,
      status: this.filterStatus !== 'all' ? this.filterStatus : undefined,
      patientId: this.filterPatient || undefined,
      startDate: this.getStartDateFromFilter(),
      endDate: this.getEndDateFromFilter(),
      page: 0,
      size: 50
    };

    this.auditService.getClinicalDecisions(filters).subscribe({
      next: (response) => {
        this.clinicalDecisions = response.content || [];
        this.logger.info('Loaded clinical decisions', { count: this.clinicalDecisions.length });
      },
      error: (error) => {
        this.logger.error('Failed to load clinical decisions', error);
      }
    });
  }
  
  /**
   * Load clinical metrics
   */
  loadClinicalMetrics(): void {
    this.logger.info('Loading clinical metrics');

    const dateRange = this.getDateRangeForMetrics();
    this.auditService.getClinicalMetrics(dateRange).subscribe({
      next: (metrics) => {
        this.clinicalMetrics = { ...this.clinicalMetrics, ...metrics.clinicalMetrics };
        this.careQualityMetrics = { ...this.careQualityMetrics, ...metrics.careQualityMetrics };
        this.logger.info('Loaded clinical metrics', metrics);
      },
      error: (error) => {
        this.logger.error('Failed to load clinical metrics', error);
      }
    });
  }
  
  /**
   * Refresh all data
   */
  refreshData(): void {
    this.loadClinicalDecisions();
    this.loadClinicalMetrics();
  }
  
  /**
   * Apply filters
   */
  applyFilters(): void {
    this.logger.info('Applying filters', {
      priority: this.filterPriority,
      status: this.filterStatus,
      patient: this.filterPatient,
      dateRange: this.filterDateRange,
      decisionType: this.filterDecisionType
    });
    this.loadClinicalDecisions();
  }
  
  /**
   * Review clinical decision
   */
  reviewDecision(decision: ClinicalAIDecision): void {
    this.selectedDecision = decision;
    this.clinicalNotes = '';
  }
  
  /**
   * Accept AI recommendation
   */
  acceptRecommendation(): void {
    if (!this.selectedDecision) return;

    this.logger.info('Accepting recommendation', { eventId: this.selectedDecision.eventId, notes: this.clinicalNotes });

    this.auditService.acceptClinicalRecommendation(this.selectedDecision.eventId, {
      clinicalNotes: this.clinicalNotes
    }).subscribe({
      next: () => {
        this.clinicalMetrics.decisionsAccepted++;
        this.removeFromQueue(this.selectedDecision!.eventId);
        this.selectedDecision = null;
        this.clinicalNotes = '';
        this.logger.info('Recommendation accepted successfully');
      },
      error: (error) => {
        this.logger.error('Failed to accept recommendation', error);
      }
    });
  }
  
  /**
   * Reject AI recommendation with clinical rationale
   */
  rejectRecommendation(): void {
    if (!this.selectedDecision) return;

    this.logger.info('Rejecting recommendation', { eventId: this.selectedDecision.eventId, clinicalRationale: this.clinicalNotes });

    this.auditService.rejectClinicalRecommendation(this.selectedDecision.eventId, {
      clinicalRationale: this.clinicalNotes || 'Clinical decision overridden',
      clinicalNotes: this.clinicalNotes
    }).subscribe({
      next: () => {
        this.clinicalMetrics.decisionsRejected++;
        this.removeFromQueue(this.selectedDecision!.eventId);
        this.selectedDecision = null;
        this.clinicalNotes = '';
        this.logger.info('Recommendation rejected successfully');
      },
      error: (error) => {
        this.logger.error('Failed to reject recommendation', error);
      }
    });
  }
  
  /**
   * Modify recommendation with clinical judgment
   */
  modifyRecommendation(): void {
    if (!this.selectedDecision) return;

    this.logger.info('Modifying recommendation', { eventId: this.selectedDecision.eventId, modifications: this.clinicalNotes });

    this.auditService.modifyClinicalRecommendation(this.selectedDecision.eventId, {
      modifications: this.clinicalNotes || 'Modified by clinician',
      clinicalNotes: this.clinicalNotes
    }).subscribe({
      next: () => {
        this.clinicalMetrics.decisionsModified++;
        this.removeFromQueue(this.selectedDecision!.eventId);
        this.selectedDecision = null;
        this.clinicalNotes = '';
        this.logger.info('Recommendation modified successfully');
      },
      error: (error) => {
        this.logger.error('Failed to modify recommendation', error);
      }
    });
  }
  
  /**
   * Cancel review
   */
  cancelReview(): void {
    this.selectedDecision = null;
    this.clinicalNotes = '';
  }
  
  /**
   * Remove decision from queue
   */
  private removeFromQueue(eventId: string): void {
    this.clinicalDecisions = this.clinicalDecisions.filter(d => d.eventId !== eventId);
  }
  
  /**
   * Export clinical audit report
   */
  exportClinicalReport(): void {
    this.logger.info('Exporting clinical audit report');

    this.auditService.exportClinicalReport().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `clinical-audit-report-${new Date().toISOString().split('T')[0]}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.logger.info('Clinical report exported successfully');
      },
      error: (error) => {
        this.logger.error('Failed to export clinical report', error);
      }
    });
  }
  
  /**
   * Get priority badge class
   */
  getPriorityClass(priority: string): string {
    const priorityClasses: Record<string, string> = {
      'critical': 'critical',
      'high': 'high',
      'medium': 'medium',
      'low': 'low'
    };
    return priorityClasses[priority] || 'medium';
  }
  
  /**
   * Get clinical impact badge class
   */
  getClinicalImpactClass(impact: string): string {
    const impactClasses: Record<string, string> = {
      'high': 'high-impact',
      'medium': 'medium-impact',
      'low': 'low-impact'
    };
    return impactClasses[impact] || 'medium-impact';
  }
  
  /**
   * Get evidence strength indicator
   */
  getEvidenceStrengthLabel(strength: string): string {
    const labels: Record<string, string> = {
      'A': 'Strong Evidence (Grade A)',
      'B': 'Moderate Evidence (Grade B)',
      'C': 'Weak Evidence (Grade C)',
      'D': 'Expert Opinion (Grade D)'
    };
    return labels[strength] || strength;
  }
  
  /**
   * Get start date based on date range filter
   */
  private getStartDateFromFilter(): string | undefined {
    const now = new Date();
    switch (this.filterDateRange) {
      case 'today':
        now.setHours(0, 0, 0, 0);
        return now.toISOString();
      case 'last-7-days':
        now.setDate(now.getDate() - 7);
        return now.toISOString();
      case 'last-30-days':
        now.setDate(now.getDate() - 30);
        return now.toISOString();
      case 'last-90-days':
        now.setDate(now.getDate() - 90);
        return now.toISOString();
      default:
        return undefined;
    }
  }

  /**
   * Get end date (always now)
   */
  private getEndDateFromFilter(): string | undefined {
    return new Date().toISOString();
  }

  /**
   * Get date range for metrics API call
   */
  private getDateRangeForMetrics(): { startDate: string; endDate: string } | undefined {
    const startDate = this.getStartDateFromFilter();
    const endDate = this.getEndDateFromFilter();
    if (startDate && endDate) {
      return { startDate, endDate };
    }
    return undefined;
  }
}

// Interfaces

interface ClinicalAIDecision {
  eventId: string;
  timestamp: Date;
  patientId: string;
  patientName: string;
  decisionType: string;
  priority: 'critical' | 'high' | 'medium' | 'low';
  clinicalImpact: 'high' | 'medium' | 'low';
  recommendation: string;
  clinicalRationale: string;
  evidenceGrade: 'A' | 'B' | 'C' | 'D';
  confidenceScore: number;
  suggestedAction: string;
  reviewedBy: string | null;
  status: 'pending' | 'accepted' | 'rejected' | 'modified';
  responseTimeMinutes: number | null;
}

interface MedicationRecommendation {
  eventId: string;
  timestamp: Date;
  patientId: string;
  medication: string;
  currentDose: string;
  recommendedDose: string;
  reason: string;
  evidenceSupport: string;
  potentialBenefit: string;
  safetyConsiderations: string;
}

interface CareGapAlert {
  eventId: string;
  timestamp: Date;
  patientId: string;
  gapType: string;
  measure: string;
  dueDate: Date;
  daysOverdue: number;
  qualityMeasure: string;
  clinicalGuideline: string;
}

interface RiskStratification {
  eventId: string;
  timestamp: Date;
  patientId: string;
  riskCategory: string;
  riskScore: number;
  riskLevel: 'Low' | 'Medium' | 'High' | 'Critical';
  contributingFactors: string[];
  recommendedInterventions: string[];
}
