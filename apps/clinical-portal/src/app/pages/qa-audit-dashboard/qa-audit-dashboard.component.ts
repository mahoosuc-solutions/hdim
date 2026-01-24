import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { LoggerService } from '../../services/logger.service';
import { AuditService } from '../../services/audit.service';

/**
 * QA Audit Dashboard
 * 
 * Quality Assurance-focused audit interface for:
 * - AI decision validation and review
 * - Quality metric verification
 * - Care gap recommendation assessment
 * - AI confidence score analysis
 * - False positive/negative tracking
 * 
 * Designed for QA_ANALYST role with focus on quality validation workflows.
 */
@Component({
  selector: 'app-qa-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qa-audit-dashboard.component.html',
  styleUrls: ['./qa-audit-dashboard.component.scss']
})
export class QaAuditDashboardComponent implements OnInit, OnDestroy {

  // Contextual logger
  private get logger() {
    return this.loggerService.withContext('QaAuditDashboardComponent');
  }

  // QA Review Queues
  pendingReviewDecisions: AIDecisionForReview[] = [];
  flaggedDecisions: AIDecisionForReview[] = [];
  
  // Quality Metrics
  qaMetrics = {
    totalReviewed: 0,
    approvedDecisions: 0,
    rejectedDecisions: 0,
    flaggedForEscalation: 0,
    averageConfidenceScore: 0,
    lowConfidenceCount: 0,
    falsePositiveRate: 0,
    falseNegativeRate: 0
  };
  
  // Filters
  filterAgentType: string = 'all';
  filterConfidenceRange: string = 'all'; // all, high (>80%), medium (50-80%), low (<50%)
  filterDateRange: string = 'today';
  filterReviewStatus: string = 'pending'; // pending, reviewed, flagged
  
  // Active tabs
  activeTab: 'review-queue' | 'flagged' | 'metrics' | 'trends' = 'review-queue';
  
  // Current review
  currentReviewDecision: AIDecisionForReview | null = null;
  reviewNotes: string = '';
  
  // Auto-refresh
  private refreshSubscription?: Subscription;
  autoRefreshEnabled: boolean = true;
  
  // Trend data for charts
  confidenceTrends: ConfidenceTrendData[] = [];
  accuracyTrends: AccuracyTrendData[] = [];

  constructor(
    private loggerService: LoggerService,
    private auditService: AuditService
  ) {}

  ngOnInit(): void {
    this.loadQAReviewQueue();
    this.loadQAMetrics();
    this.loadTrendData();
    
    if (this.autoRefreshEnabled) {
      this.refreshSubscription = interval(60000).subscribe(() => {
        this.refreshQAData();
      });
    }
    
    this.loadMockData();
  }
  
  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }
  
  /**
   * Load decisions pending QA review
   */
  loadQAReviewQueue(): void {
    this.logger.info('Loading QA review queue');

    const filters = {
      agentType: this.filterAgentType !== 'all' ? this.filterAgentType : undefined,
      minConfidence: this.getMinConfidenceFromFilter(),
      maxConfidence: this.getMaxConfidenceFromFilter(),
      startDate: this.getStartDateFromFilter(),
      endDate: this.getEndDateFromFilter(),
      includeReviewed: this.filterReviewStatus !== 'pending',
      page: 0,
      size: 50
    };

    this.auditService.getReviewQueue(filters).subscribe({
      next: (response) => {
        this.pendingReviewDecisions = response.content || [];
        this.logger.info('Loaded QA review queue', { count: this.pendingReviewDecisions.length });
      },
      error: (error) => {
        this.logger.error('Failed to load QA review queue', error);
      }
    });
  }
  
  /**
   * Load QA quality metrics
   */
  loadQAMetrics(): void {
    this.logger.info('Loading QA metrics');

    const dateRange = this.getDateRangeForMetrics();
    this.auditService.getQAMetrics(dateRange).subscribe({
      next: (metrics) => {
        this.qaMetrics = { ...this.qaMetrics, ...metrics };
        this.logger.info('Loaded QA metrics', metrics);
      },
      error: (error) => {
        this.logger.error('Failed to load QA metrics', error);
      }
    });
  }
  
  /**
   * Load trend analysis data
   */
  loadTrendData(): void {
    this.logger.info('Loading trend data');

    this.auditService.getTrendData().subscribe({
      next: (trends) => {
        this.confidenceTrends = trends.confidenceTrends || [];
        this.accuracyTrends = trends.accuracyTrends || [];
        this.logger.info('Loaded trend data', trends);
      },
      error: (error) => {
        this.logger.error('Failed to load trend data', error);
      }
    });
  }
  
  /**
   * Refresh all QA data
   */
  refreshQAData(): void {
    this.loadQAReviewQueue();
    this.loadQAMetrics();
    this.loadTrendData();
  }
  
  /**
   * Start reviewing a specific AI decision
   */
  startReview(decision: AIDecisionForReview): void {
    this.currentReviewDecision = decision;
    this.reviewNotes = '';
  }
  
  /**
   * Approve an AI decision after review
   */
  approveDecision(): void {
    if (!this.currentReviewDecision) return;

    this.logger.info('Approving decision', { eventId: this.currentReviewDecision.eventId, notes: this.reviewNotes });

    this.auditService.approveReview(this.currentReviewDecision.eventId, {
      reviewNotes: this.reviewNotes
    }).subscribe({
      next: () => {
        this.qaMetrics.approvedDecisions++;
        this.qaMetrics.totalReviewed++;
        this.removeFromQueue(this.currentReviewDecision!.eventId);
        this.currentReviewDecision = null;
        this.reviewNotes = '';
        this.logger.info('Decision approved successfully');
      },
      error: (error) => {
        this.logger.error('Failed to approve decision', error);
      }
    });
  }
  
  /**
   * Reject an AI decision after review
   */
  rejectDecision(): void {
    if (!this.currentReviewDecision) return;

    this.logger.info('Rejecting decision', { eventId: this.currentReviewDecision.eventId, notes: this.reviewNotes });

    this.auditService.rejectReview(this.currentReviewDecision.eventId, {
      rejectionReason: this.reviewNotes || 'QA review failed',
      reviewNotes: this.reviewNotes
    }).subscribe({
      next: () => {
        this.qaMetrics.rejectedDecisions++;
        this.qaMetrics.totalReviewed++;
        this.removeFromQueue(this.currentReviewDecision!.eventId);
        this.currentReviewDecision = null;
        this.reviewNotes = '';
        this.logger.info('Decision rejected successfully');
      },
      error: (error) => {
        this.logger.error('Failed to reject decision', error);
      }
    });
  }
  
  /**
   * Flag decision for clinical escalation
   */
  flagForEscalation(): void {
    if (!this.currentReviewDecision) return;

    this.logger.info('Flagging decision for escalation', { eventId: this.currentReviewDecision.eventId, notes: this.reviewNotes });

    this.auditService.flagReview(this.currentReviewDecision.eventId, {
      escalationReason: this.reviewNotes || 'Requires clinical review',
      severity: 'HIGH'
    }).subscribe({
      next: () => {
        this.qaMetrics.flaggedForEscalation++;
        this.flaggedDecisions.push(this.currentReviewDecision!);
        this.removeFromQueue(this.currentReviewDecision!.eventId);
        this.currentReviewDecision = null;
        this.reviewNotes = '';
        this.logger.info('Decision flagged for escalation successfully');
      },
      error: (error) => {
        this.logger.error('Failed to flag decision', error);
      }
    });
  }
  
  /**
   * Mark decision as false positive
   */
  markFalsePositive(decision: AIDecisionForReview): void {
    this.logger.info('Marking as false positive', decision.eventId);

    this.auditService.markFalsePositive(decision.eventId).subscribe({
      next: () => {
        decision.qaReviewStatus = 'false-positive';
        this.removeFromQueue(decision.eventId);
        this.logger.info('Marked as false positive successfully');
      },
      error: (error) => {
        this.logger.error('Failed to mark as false positive', error);
      }
    });
  }
  
  /**
   * Mark decision as false negative
   */
  markFalseNegative(decision: AIDecisionForReview): void {
    this.logger.info('Marking as false negative', decision.eventId);

    this.auditService.markFalseNegative(decision.eventId).subscribe({
      next: () => {
        decision.qaReviewStatus = 'false-negative';
        this.removeFromQueue(decision.eventId);
        this.logger.info('Marked as false negative successfully');
      },
      error: (error) => {
        this.logger.error('Failed to mark as false negative', error);
      }
    });
  }
  
  /**
   * Cancel current review
   */
  cancelReview(): void {
    this.currentReviewDecision = null;
    this.reviewNotes = '';
  }
  
  /**
   * Remove decision from review queue
   */
  private removeFromQueue(eventId: string): void {
    this.pendingReviewDecisions = this.pendingReviewDecisions.filter(d => d.eventId !== eventId);
  }
  
  /**
   * Apply filters to review queue
   */
  applyFilters(): void {
    // TODO: Call backend with filter parameters
    this.logger.info('Applying filters', {
      agentType: this.filterAgentType,
      confidenceRange: this.filterConfidenceRange,
      dateRange: this.filterDateRange,
      reviewStatus: this.filterReviewStatus
    });
    this.loadQAReviewQueue();
  }
  
  /**
   * Export QA report
   */
  exportQAReport(): void {
    this.logger.info('Exporting QA report');

    this.auditService.exportQAReport().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `qa-audit-report-${new Date().toISOString().split('T')[0]}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.logger.info('QA report exported successfully');
      },
      error: (error) => {
        this.logger.error('Failed to export QA report', error);
      }
    });
  }
  
  /**
   * Get confidence score color class
   */
  getConfidenceClass(score: number): string {
    if (score >= 0.8) return 'high-confidence';
    if (score >= 0.5) return 'medium-confidence';
    return 'low-confidence';
  }
  
  /**
   * Get review priority label
   */
  getPriorityLabel(priority: string): string {
    const labels: Record<string, string> = {
      'critical': 'Critical - Review Immediately',
      'high': 'High Priority',
      'medium': 'Standard Review',
      'low': 'Low Priority'
    };
    return labels[priority] || priority;
  }
  
  /**
   * Get min confidence score based on filter selection
   */
  private getMinConfidenceFromFilter(): number | undefined {
    switch (this.filterConfidenceRange) {
      case 'high': return 0.8;
      case 'medium': return 0.5;
      case 'low': return 0;
      default: return undefined;
    }
  }

  /**
   * Get max confidence score based on filter selection
   */
  private getMaxConfidenceFromFilter(): number | undefined {
    switch (this.filterConfidenceRange) {
      case 'high': return 1.0;
      case 'medium': return 0.8;
      case 'low': return 0.5;
      default: return undefined;
    }
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
      case 'week':
        now.setDate(now.getDate() - 7);
        return now.toISOString();
      case 'month':
        now.setMonth(now.getMonth() - 1);
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

  /**
   * Load mock data for demonstration
   */
  private loadMockData(): void {
    this.pendingReviewDecisions = [
      {
        eventId: '550e8400-e29b-41d4-a716-446655440001',
        timestamp: new Date(),
        agentType: 'CARE_GAP_IDENTIFIER',
        decisionType: 'CARE_GAP_IDENTIFICATION',
        patientId: 'PAT-12345',
        confidenceScore: 0.65,
        recommendedAction: 'Schedule diabetic eye exam',
        reasoning: 'Patient has diabetes diagnosis, no eye exam in past 12 months',
        reviewPriority: 'high',
        qaReviewStatus: 'pending',
        clinicalImpact: 'medium',
        requiresPhysicianReview: false
      },
      {
        eventId: '550e8400-e29b-41d4-a716-446655440002',
        timestamp: new Date(Date.now() - 3600000),
        agentType: 'MEDICATION_OPTIMIZATION',
        decisionType: 'MEDICATION_RECOMMENDATION',
        patientId: 'PAT-67890',
        confidenceScore: 0.45,
        recommendedAction: 'Consider switching ACE inhibitor',
        reasoning: 'Patient has persistent cough, potential ACE inhibitor side effect',
        reviewPriority: 'critical',
        qaReviewStatus: 'pending',
        clinicalImpact: 'high',
        requiresPhysicianReview: true
      },
      {
        eventId: '550e8400-e29b-41d4-a716-446655440003',
        timestamp: new Date(Date.now() - 7200000),
        agentType: 'RISK_STRATIFICATION',
        decisionType: 'RISK_SCORE_CALCULATION',
        patientId: 'PAT-11111',
        confidenceScore: 0.92,
        recommendedAction: 'Elevated readmission risk - care management referral',
        reasoning: 'Multiple risk factors: age 75+, CHF, 2 hospitalizations in 30 days',
        reviewPriority: 'medium',
        qaReviewStatus: 'pending',
        clinicalImpact: 'high',
        requiresPhysicianReview: false
      }
    ];
    
    this.qaMetrics = {
      totalReviewed: 127,
      approvedDecisions: 98,
      rejectedDecisions: 18,
      flaggedForEscalation: 11,
      averageConfidenceScore: 0.74,
      lowConfidenceCount: 23,
      falsePositiveRate: 0.08,
      falseNegativeRate: 0.03
    };
    
    this.confidenceTrends = [
      { date: '2026-01-06', avgConfidence: 0.71, lowConfidenceCount: 8 },
      { date: '2026-01-07', avgConfidence: 0.73, lowConfidenceCount: 6 },
      { date: '2026-01-08', avgConfidence: 0.75, lowConfidenceCount: 5 },
      { date: '2026-01-09', avgConfidence: 0.72, lowConfidenceCount: 7 },
      { date: '2026-01-10', avgConfidence: 0.76, lowConfidenceCount: 4 },
      { date: '2026-01-13', avgConfidence: 0.74, lowConfidenceCount: 6 }
    ];
    
    this.accuracyTrends = [
      { date: '2026-01-06', approvalRate: 0.82, rejectionRate: 0.12, flaggedRate: 0.06 },
      { date: '2026-01-07', approvalRate: 0.85, rejectionRate: 0.10, flaggedRate: 0.05 },
      { date: '2026-01-08', approvalRate: 0.79, rejectionRate: 0.14, flaggedRate: 0.07 },
      { date: '2026-01-09', approvalRate: 0.83, rejectionRate: 0.11, flaggedRate: 0.06 },
      { date: '2026-01-10', approvalRate: 0.87, rejectionRate: 0.09, flaggedRate: 0.04 },
      { date: '2026-01-13', approvalRate: 0.84, rejectionRate: 0.10, flaggedRate: 0.06 }
    ];
  }
}

// Interfaces

interface AIDecisionForReview {
  eventId: string;
  timestamp: Date;
  agentType: string;
  decisionType: string;
  patientId: string;
  confidenceScore: number;
  recommendedAction: string;
  reasoning: string;
  reviewPriority: 'critical' | 'high' | 'medium' | 'low';
  qaReviewStatus: 'pending' | 'approved' | 'rejected' | 'flagged' | 'false-positive' | 'false-negative';
  clinicalImpact: 'high' | 'medium' | 'low';
  requiresPhysicianReview: boolean;
}

interface ConfidenceTrendData {
  date: string;
  avgConfidence: number;
  lowConfidenceCount: number;
}

interface AccuracyTrendData {
  date: string;
  approvalRate: number;
  rejectionRate: number;
  flaggedRate: number;
}
