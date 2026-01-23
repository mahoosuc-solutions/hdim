import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { LoggerService } from '../../services/logger.service';

/**
 * MPI Audit Dashboard
 * 
 * Master Patient Index audit interface for:
 * - Patient identity merge/unmerge operations
 * - Identity resolution decisions
 * - MPI configuration changes
 * - Data quality issues
 * - Duplicate detection events
 * - Cross-reference validations
 * 
 * Designed for MPI_ADMIN role with focus on patient identity governance.
 */
@Component({
  selector: 'app-mpi-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mpi-audit-dashboard.component.html',
  styleUrls: ['./mpi-audit-dashboard.component.scss']
})
export class MpiAuditDashboardComponent implements OnInit, OnDestroy {

  // Contextual logger
  private logger = this.loggerService.withContext('MpiAuditDashboardComponent');

  // MPI Audit Events
  mpiEvents: MPIAuditEvent[] = [];
  mergeEvents: MergeEvent[] = [];
  dataQualityIssues: DataQualityIssue[] = [];
  
  // MPI Metrics
  mpiMetrics = {
    totalMerges: 0,
    totalUnmerges: 0,
    pendingResolutions: 0,
    dataQualityIssues: 0,
    duplicatesDetected: 0,
    autoMergedRecords: 0,
    manualReviewRequired: 0,
    crossReferencesValidated: 0
  };
  
  // Match quality metrics
  matchQualityMetrics = {
    highConfidenceMatches: 0,
    mediumConfidenceMatches: 0,
    lowConfidenceMatches: 0,
    averageMatchScore: 0
  };
  
  // Filters
  filterEventType: string = 'all';
  filterDateRange: string = 'last-7-days';
  filterTenantId: string = 'all';
  filterUserId: string = '';
  
  // Active tabs
  activeTab: 'events' | 'merges' | 'data-quality' | 'metrics' = 'events';
  
  // Selected event for detail view
  selectedEvent: MPIAuditEvent | null = null;
  
  // Auto-refresh
  private refreshSubscription?: Subscription;
  autoRefreshEnabled: boolean = true;

  constructor(private loggerService: LoggerService) {}

  ngOnInit(): void {
    this.loadMPIEvents();
    this.loadMPIMetrics();
    
    if (this.autoRefreshEnabled) {
      this.refreshSubscription = interval(45000).subscribe(() => {
        this.refreshData();
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
   * Load MPI audit events
   */
  loadMPIEvents(): void {
    // TODO: Call backend API /api/v1/audit/ai/user-actions?actionType=MPI_*
    this.logger.info('Loading MPI audit events');
  }
  
  /**
   * Load MPI metrics
   */
  loadMPIMetrics(): void {
    // TODO: Call backend API /api/v1/audit/mpi/metrics
    this.logger.info('Loading MPI metrics');
  }
  
  /**
   * Refresh all data
   */
  refreshData(): void {
    this.loadMPIEvents();
    this.loadMPIMetrics();
  }
  
  /**
   * Apply filters
   */
  applyFilters(): void {
    this.logger.info('Applying filters', {
      eventType: this.filterEventType,
      dateRange: this.filterDateRange,
      tenantId: this.filterTenantId,
      userId: this.filterUserId
    });
    this.loadMPIEvents();
  }
  
  /**
   * View event details
   */
  viewEventDetails(event: MPIAuditEvent): void {
    this.selectedEvent = event;
  }
  
  /**
   * Close event details
   */
  closeEventDetails(): void {
    this.selectedEvent = null;
  }
  
  /**
   * Validate merge operation
   */
  validateMerge(mergeEvent: MergeEvent): void {
    // TODO: Call backend API POST /api/v1/mpi/merges/{id}/validate
    this.logger.info('Validating merge', mergeEvent.eventId);
  }
  
  /**
   * Rollback merge operation
   */
  rollbackMerge(mergeEvent: MergeEvent): void {
    if (confirm('Are you sure you want to rollback this merge operation?')) {
      // TODO: Call backend API POST /api/v1/mpi/merges/{id}/rollback
      this.logger.info('Rolling back merge', mergeEvent.eventId);
    }
  }
  
  /**
   * Resolve data quality issue
   */
  resolveDataQualityIssue(issue: DataQualityIssue): void {
    // TODO: Call backend API POST /api/v1/mpi/data-quality/{id}/resolve
    this.logger.info('Resolving data quality issue', issue.issueId);
  }
  
  /**
   * Export MPI audit report
   */
  exportMPIReport(): void {
    // TODO: Call backend API GET /api/v1/audit/mpi/report/export
    this.logger.info('Exporting MPI audit report');
  }
  
  /**
   * Get event type badge class
   */
  getEventTypeClass(eventType: string): string {
    const typeClasses: Record<string, string> = {
      'PATIENT_MERGE': 'merge',
      'PATIENT_UNMERGE': 'unmerge',
      'IDENTITY_RESOLUTION': 'resolution',
      'DUPLICATE_DETECTION': 'duplicate',
      'CROSS_REFERENCE_UPDATE': 'cross-ref',
      'DATA_QUALITY_FLAG': 'quality'
    };
    return typeClasses[eventType] || 'default';
  }
  
  /**
   * Get match score color class
   */
  getMatchScoreClass(score: number): string {
    if (score >= 0.9) return 'high-match';
    if (score >= 0.7) return 'medium-match';
    return 'low-match';
  }
  
  /**
   * Get status badge class
   */
  getStatusClass(status: string): string {
    const statusClasses: Record<string, string> = {
      'COMPLETED': 'success',
      'PENDING': 'warning',
      'FAILED': 'danger',
      'ROLLED_BACK': 'info'
    };
    return statusClasses[status] || 'default';
  }
  
  /**
   * Load mock data for demonstration
   */
  private loadMockData(): void {
    this.mpiEvents = [
      {
        eventId: '660e8400-e29b-41d4-a716-446655440001',
        timestamp: new Date(),
        eventType: 'PATIENT_MERGE',
        userId: 'mpi-admin-001',
        tenantId: 'tenant-001',
        sourcePatientId: 'PAT-12345',
        targetPatientId: 'PAT-67890',
        matchScore: 0.95,
        mergeReason: 'Duplicate detected - same SSN, DOB, and name',
        automatedMerge: false,
        status: 'COMPLETED',
        validationErrors: []
      },
      {
        eventId: '660e8400-e29b-41d4-a716-446655440002',
        timestamp: new Date(Date.now() - 3600000),
        eventType: 'IDENTITY_RESOLUTION',
        userId: 'mpi-admin-002',
        tenantId: 'tenant-001',
        sourcePatientId: 'PAT-11111',
        targetPatientId: 'PAT-22222',
        matchScore: 0.68,
        mergeReason: 'Similar demographics - requires manual review',
        automatedMerge: false,
        status: 'PENDING',
        validationErrors: ['SSN mismatch', 'Middle name differs']
      },
      {
        eventId: '660e8400-e29b-41d4-a716-446655440003',
        timestamp: new Date(Date.now() - 7200000),
        eventType: 'DUPLICATE_DETECTION',
        userId: 'system',
        tenantId: 'tenant-002',
        sourcePatientId: 'PAT-33333',
        targetPatientId: 'PAT-44444',
        matchScore: 0.88,
        mergeReason: 'Automated duplicate detection - phonetic name match',
        automatedMerge: true,
        status: 'COMPLETED',
        validationErrors: []
      }
    ];
    
    this.mergeEvents = [
      {
        eventId: '770e8400-e29b-41d4-a716-446655440001',
        timestamp: new Date(Date.now() - 86400000),
        mergedPatientId: 'PAT-12345',
        survivingRecordId: 'PAT-67890',
        mergedBy: 'mpi-admin-001',
        matchScore: 0.95,
        fieldsConflicted: ['address', 'phone'],
        resolutionStrategy: 'KEEP_MOST_RECENT',
        canRollback: true,
        rolledBack: false
      },
      {
        eventId: '770e8400-e29b-41d4-a716-446655440002',
        timestamp: new Date(Date.now() - 172800000),
        mergedPatientId: 'PAT-33333',
        survivingRecordId: 'PAT-44444',
        mergedBy: 'system',
        matchScore: 0.88,
        fieldsConflicted: ['email'],
        resolutionStrategy: 'AUTOMATED_RULE',
        canRollback: true,
        rolledBack: false
      }
    ];
    
    this.dataQualityIssues = [
      {
        issueId: '880e8400-e29b-41d4-a716-446655440001',
        timestamp: new Date(),
        patientId: 'PAT-55555',
        issueType: 'MISSING_SSN',
        severity: 'HIGH',
        description: 'Patient record missing SSN - impacts identity matching',
        resolved: false,
        resolvedBy: null,
        resolutionDate: null
      },
      {
        issueId: '880e8400-e29b-41d4-a716-446655440002',
        timestamp: new Date(Date.now() - 3600000),
        patientId: 'PAT-66666',
        issueType: 'INVALID_DOB',
        severity: 'MEDIUM',
        description: 'Date of birth is in the future',
        resolved: false,
        resolvedBy: null,
        resolutionDate: null
      }
    ];
    
    this.mpiMetrics = {
      totalMerges: 247,
      totalUnmerges: 12,
      pendingResolutions: 8,
      dataQualityIssues: 34,
      duplicatesDetected: 156,
      autoMergedRecords: 112,
      manualReviewRequired: 8,
      crossReferencesValidated: 523
    };
    
    this.matchQualityMetrics = {
      highConfidenceMatches: 112,
      mediumConfidenceMatches: 87,
      lowConfidenceMatches: 48,
      averageMatchScore: 0.78
    };
  }
}

// Interfaces

interface MPIAuditEvent {
  eventId: string;
  timestamp: Date;
  eventType: 'PATIENT_MERGE' | 'PATIENT_UNMERGE' | 'IDENTITY_RESOLUTION' | 'DUPLICATE_DETECTION' | 'CROSS_REFERENCE_UPDATE' | 'DATA_QUALITY_FLAG';
  userId: string;
  tenantId: string;
  sourcePatientId: string;
  targetPatientId?: string;
  matchScore: number;
  mergeReason: string;
  automatedMerge: boolean;
  status: 'COMPLETED' | 'PENDING' | 'FAILED' | 'ROLLED_BACK';
  validationErrors: string[];
}

interface MergeEvent {
  eventId: string;
  timestamp: Date;
  mergedPatientId: string;
  survivingRecordId: string;
  mergedBy: string;
  matchScore: number;
  fieldsConflicted: string[];
  resolutionStrategy: string;
  canRollback: boolean;
  rolledBack: boolean;
}

interface DataQualityIssue {
  issueId: string;
  timestamp: Date;
  patientId: string;
  issueType: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  resolved: boolean;
  resolvedBy: string | null;
  resolutionDate: Date | null;
}
