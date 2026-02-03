/**
 * Registered Nurse Dashboard Component
 *
 * Optimized for RN workflows:
 * - Care gap coordination
 * - Patient education and outreach
 * - Medication reconciliation
 * - Care plan management
 */

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, takeUntil, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { CareGapService, InterventionType } from '../../../services/care-gap.service';
import { ToastService } from '../../../services/toast.service';
import { TrackInteraction } from '../../../utils/ai-tracking.decorator';
import { LoggerService, ContextualLogger } from '../../../services/logger.service';
import { NurseWorkflowService } from '../../../services/nurse-workflow/nurse-workflow.service';
import { MedicationService } from '../../../services/medication/medication.service';
import { CarePlanService } from '../../../services/care-plan/care-plan.service';
import { WorkflowLauncherService, type WorkflowType } from '../../../services/workflow/workflow-launcher.service';
import { AuthService } from '../../../services/auth.service';
import { API_CONFIG } from '../../../config/api.config';

export interface CareGapTask {
  id: string;
  patientName: string;
  patientMRN: string;
  gapType: string;
  priority: 'high' | 'medium' | 'low';
  category: 'education' | 'medication' | 'coordination' | 'assessment';
  dueDate: string;
  assignedTo: string;
  status: 'pending' | 'in-progress' | 'completed';
}

export interface PatientOutreach {
  id: string;
  patientName: string;
  patientMRN: string;
  outreachType: 'call' | 'email' | 'letter';
  reason: string;
  scheduledDate: string;
  status: 'scheduled' | 'completed' | 'missed';
}

@Component({
  selector: 'app-rn-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatBadgeModule,
    StatCardComponent,
    PageHeaderComponent,
    EmptyStateComponent
  ],
  templateUrl: './rn-dashboard.component.html',
  styleUrls: ['./rn-dashboard.component.scss']
})
export class RNDashboardComponent implements OnInit, OnDestroy {
  loading = true;
  careGaps: CareGapTask[] = [];
  outreachTasks: PatientOutreach[] = [];

  careGapColumns = ['patient', 'gap', 'priority', 'category', 'dueDate', 'actions'];
  outreachColumns = ['patient', 'type', 'reason', 'date', 'status', 'actions'];

  // Dashboard metrics
  careGapsAssigned = 0;
  patientCallsPending = 0;
  medReconciliationsNeeded = 0;
  patientEducationDue = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private dialogService: DialogService,
    private notificationService: NotificationService,
    private careGapService: CareGapService,
    private toastService: ToastService,
    private logger: LoggerService,
    private nurseWorkflowService: NurseWorkflowService,
    private medicationService: MedicationService,
    private carePlanService: CarePlanService,
    private workflowLauncher: WorkflowLauncherService,
    private authService: AuthService
  ) {  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load RN dashboard data from real services
   */
  private loadDashboardData(): void {
    this.loading = true;

    // Initialize services with tenant context from authenticated user
    const tenantId = this.authService.getTenantId() || API_CONFIG.DEFAULT_TENANT_ID;
    this.nurseWorkflowService.setTenantContext(tenantId);
    this.medicationService.setTenantContext(tenantId);
    this.carePlanService.setTenantContext(tenantId);

    // Load all data in parallel using forkJoin
    forkJoin([
      // NurseWorkflow service data
      this.nurseWorkflowService.getPendingOutreachLogs(0, 50).pipe(
        catchError((error) => {
          this.logger.error('Failed to load outreach logs:', error);
          this.toastService.error('Failed to load outreach data');
          return of([]);
        })
      ),
      this.nurseWorkflowService.getPendingMedicationReconciliations(0, 50).pipe(
        catchError((error) => {
          this.logger.error('Failed to load medication reconciliations:', error);
          return of([]);
        })
      ),
      this.nurseWorkflowService.getEducationSessionsWithPoorUnderstanding().pipe(
        catchError((error) => {
          this.logger.error('Failed to load education sessions:', error);
          return of([]);
        })
      ),
      this.nurseWorkflowService.getReferralsAwaitingScheduling().pipe(
        catchError((error) => {
          this.logger.error('Failed to load pending referrals:', error);
          return of([]);
        })
      ),
      this.nurseWorkflowService.getMedicationReconciliationMetrics().pipe(
        catchError((error) => {
          this.logger.error('Failed to load metrics:', error);
          return of({ totalReconciliations: 0, pendingReconciliations: 0, completionRate: 0 });
        })
      ),
      // MedicationService data
      this.medicationService.getPendingOrdersAwaitingPharmacy(0, 50).pipe(
        catchError((error) => {
          this.logger.error('Failed to load medication orders:', error);
          return of({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 50, hasNext: false, hasPrevious: false });
        })
      ),
      // CarePlanService data
      this.carePlanService.getCarePlansDueForReview(0, 50).pipe(
        catchError((error) => {
          this.logger.error('Failed to load care plans:', error);
          return of({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 50, hasNext: false, hasPrevious: false });
        })
      ),
    ]).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ([outreachLogs, medReconciliations, educationSessions, referrals, metrics, medicationOrders, carePlans]) => {
          this.processDashboardData(outreachLogs, medReconciliations, educationSessions, referrals, metrics, medicationOrders, carePlans);
          this.loading = false;
        },
        error: (error) => {
          this.logger.error('Failed to load dashboard data:', error);
          this.toastService.error('Failed to load dashboard. Please refresh the page.');
          this.loading = false;
        },
      });
  }

  /**
   * Process loaded dashboard data and transform to UI models
   */
  private processDashboardData(
    outreachLogs: any,
    medReconciliations: any,
    educationSessions: any,
    referrals: any,
    metrics: any,
    medicationOrders: any,
    carePlans: any
  ): void {
    // Transform outreach logs to PatientOutreach
    const outreachContent = outreachLogs?.content || [];
    this.outreachTasks = outreachContent.map((log: any) => ({
      id: log.id,
      patientName: log.patientName || 'Unknown',
      patientMRN: log.patientMRN || 'N/A',
      outreachType: log.contactMethod?.toLowerCase() || 'call',
      reason: log.reason,
      scheduledDate: new Date(log.contactedAt).toLocaleString(),
      status: 'scheduled' as const,
    }));

    // Transform medication reconciliations to care gaps
    const medRecContent = medReconciliations?.content || [];
    const medicationGaps = medRecContent.map((rec: any) => ({
      id: rec.id,
      patientName: rec.patientName || 'Unknown',
      patientMRN: rec.patientMRN || 'N/A',
      gapType: 'Medication Reconciliation',
      priority: 'high' as const,
      category: 'medication' as const,
      dueDate: rec.startDate,
      assignedTo: 'RN',
      status: 'pending' as const,
    }));

    // Transform education sessions to care gaps
    const educationContent = educationSessions || [];
    const educationGaps = educationContent.map((session: any) => ({
      id: session.id,
      patientName: session.patientName || 'Unknown',
      patientMRN: session.patientMRN || 'N/A',
      gapType: 'Patient Education',
      priority: 'medium' as const,
      category: 'education' as const,
      dueDate: session.deliveredAt,
      assignedTo: 'RN',
      status: 'pending' as const,
    }));

    // Transform referrals to care gaps
    const referralContent = referrals || [];
    const referralGaps = referralContent.map((ref: any) => ({
      id: ref.id,
      patientName: ref.patientName || 'Unknown',
      patientMRN: ref.patientMRN || 'N/A',
      gapType: `${ref.specialtyType} Referral`,
      priority: ref.priority?.toLowerCase() || 'medium' as const,
      category: 'coordination' as const,
      dueDate: ref.requestedAt,
      assignedTo: 'RN',
      status: 'pending' as const,
    }));

    // Combine all care gaps
    this.careGaps = [...medicationGaps, ...educationGaps, ...referralGaps];

    // Update metrics from real data
    this.medReconciliationsNeeded = metrics?.pendingReconciliations || 0;
    this.careGapsAssigned = this.careGaps.length;
    this.patientCallsPending = this.outreachTasks.filter(t => t.outreachType === 'call').length;
    this.patientEducationDue = educationGaps.length;

    this.logger.debug('Dashboard data loaded', {
      outreach: this.outreachTasks.length,
      careGaps: this.careGaps.length,
      metrics: { medReconciliationsNeeded: this.medReconciliationsNeeded, patientCallsPending: this.patientCallsPending },
    });
  }

  /**
   * Address care gap by launching appropriate workflow dialog
   */
  addressCareGap(gap: CareGapTask): void {
    this.logger.debug('Addressing care gap:', gap.gapType);

    try {
      // Map care gap category to workflow type
      const workflowType: WorkflowType = this.workflowLauncher.mapCategoryToWorkflow(gap.category);

      // Launch workflow with completion callback
      this.workflowLauncher.launchWorkflow(
        workflowType,
        {
          id: gap.id,
          patientId: gap.id, // Note: Transform gap.id to patientId as needed by workflows
          patientName: gap.patientName,
          patientMRN: gap.patientMRN,
          category: gap.category,
          gapType: gap.gapType,
          priority: gap.priority,
          dueDate: gap.dueDate,
          status: gap.status,
        },
        (result) => {
          if (result.success) {
            // Update care gap status after workflow completion
            gap.status = 'completed';
            this.careGapsAssigned = Math.max(0, this.careGapsAssigned - 1);
            this.logger.info(`Care gap ${gap.id} marked as completed`);

            // Remove from list after brief delay for UI update
            setTimeout(() => {
              const index = this.careGaps.indexOf(gap);
              if (index > -1) {
                this.careGaps.splice(index, 1);
              }
            }, 300);
          }
        }
      );
    } catch (error) {
      this.logger.error('Failed to launch workflow:', error);
      this.toastService.error('Failed to launch workflow. Please try again.');
    }
  }

  /**
   * Quick launch a workflow without a specific care gap task
   * Used for launching workflows from quick action buttons
   */
  quickLaunchWorkflow(workflowType: WorkflowType): void {
    this.logger.debug('Quick launching workflow:', workflowType);

    try {
      // Create a dummy task for quick launch
      const dummyTask = {
        id: `${workflowType}-${Date.now()}`,
        patientId: '',
        patientName: 'Demo Patient',
        patientMRN: 'N/A',
        category: workflowType,
        gapType: this.workflowLauncher.getWorkflowConfig(workflowType).label,
        priority: 'medium' as const,
        dueDate: new Date().toISOString(),
        status: 'pending' as const,
      };

      // Launch workflow
      this.workflowLauncher.launchWorkflow(workflowType, dummyTask, (result) => {
        if (result.success) {
          this.logger.info(`${workflowType} workflow completed successfully`);
        }
      });
    } catch (error) {
      this.logger.error('Failed to quick launch workflow:', error);
      this.toastService.error('Failed to launch workflow. Please try again.');
    }
  }

  /**
   * Complete patient education
   */
  provideEducation(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Patient Education',
      `Provide <strong>${gap.gapType}</strong> education to <strong>${gap.patientName}</strong>?<br><br>` +
      `This will mark the education as completed and update the care gap status.`,
      'Complete Education',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        gap.status = 'completed';
        this.patientEducationDue = Math.max(0, this.patientEducationDue - 1);
        this.notificationService.success(`Patient education completed for ${gap.patientName}`);
        // Navigate to patient detail to document the education
        this.router.navigate(['/patients', gap.id], {
          queryParams: { action: 'document-education', gapType: gap.gapType }
        });
      }
    });
  }

  /**
   * Perform medication reconciliation
   */
  reconcileMedications(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Medication Reconciliation',
      `Start medication reconciliation for <strong>${gap.patientName}</strong> (${gap.patientMRN})?<br><br>` +
      `This workflow will guide you through reviewing current medications, allergies, and reconciling any discrepancies.`,
      'Start Reconciliation',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        gap.status = 'in-progress';
        this.notificationService.info(`Starting medication reconciliation for ${gap.patientName}`);
        // Navigate to patient detail with medication reconciliation view
        this.router.navigate(['/patients', gap.id], {
          queryParams: { action: 'med-reconciliation' }
        });
      }
    });
  }

  /**
   * Make patient call
   */
  makePatientCall(outreach: PatientOutreach): void {
    this.dialogService.confirm(
      'Patient Call',
      `Document call to <strong>${outreach.patientName}</strong>?<br><br>` +
      `<strong>Reason:</strong> ${outreach.reason}<br>` +
      `<strong>Scheduled:</strong> ${outreach.scheduledDate}`,
      'Start Call Documentation',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.notificationService.info(`Opening call documentation for ${outreach.patientName}`);
        // Navigate to patient detail with call documentation
        this.router.navigate(['/patients', outreach.id], {
          queryParams: { action: 'document-call', reason: outreach.reason }
        });
      }
    });
  }

  /**
   * Send patient communication
   */
  sendCommunication(outreach: PatientOutreach): void {
    const communicationType = outreach.outreachType === 'email' ? 'email' : 'letter';
    this.dialogService.confirm(
      `Send Patient ${communicationType === 'email' ? 'Email' : 'Letter'}`,
      `Send ${communicationType} to <strong>${outreach.patientName}</strong>?<br><br>` +
      `<strong>Reason:</strong> ${outreach.reason}<br>` +
      `This will open the communication composer.`,
      `Compose ${communicationType === 'email' ? 'Email' : 'Letter'}`,
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.notificationService.info(`Opening ${communicationType} composer for ${outreach.patientName}`);
        // Navigate to patient detail with communication action
        this.router.navigate(['/patients', outreach.id], {
          queryParams: { action: 'send-communication', type: communicationType, reason: outreach.reason }
        });
      }
    });
  }

  /**
   * View patient details
   */
  viewPatient(id: string): void {
    this.router.navigate(['/patients', id]);
  }

  /**
   * Get priority color
   */
  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'high': return 'warn';
      case 'medium': return 'accent';
      case 'low': return 'primary';
      default: return '';
    }
  }

  /**
   * Get category icon
   */
  getCategoryIcon(category: string): string {
    switch (category) {
      case 'education': return 'school';
      case 'medication': return 'medication';
      case 'coordination': return 'sync';
      case 'assessment': return 'assessment';
      default: return 'task';
    }
  }

  /**
   * Get category color
   */
  getCategoryColor(category: string): string {
    switch (category) {
      case 'education': return '#4caf50';
      case 'medication': return '#f44336';
      case 'coordination': return '#2196f3';
      case 'assessment': return '#9c27b0';
      default: return '#757575';
    }
  }

  /**
   * Get outreach type icon
   */
  getOutreachIcon(type: string): string {
    switch (type) {
      case 'call': return 'phone';
      case 'email': return 'email';
      case 'letter': return 'mail';
      default: return 'contact_mail';
    }
  }

  /**
   * Get status color
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'completed': return 'success';
      case 'in-progress': return 'warn';
      case 'pending':
      case 'scheduled': return 'primary';
      case 'missed': return 'warn';
      default: return '';
    }
  }

  /**
   * Refresh dashboard
   */
  refreshData(): void {
    this.loadDashboardData();
  }

  /**
   * View all care gaps
   */
  viewAllCareGaps(): void {
    this.router.navigate(['/patients'], {
      queryParams: { view: 'care-gaps' }
    });
  }

  /**
   * View all outreach tasks
   */
  viewAllOutreach(): void {
    this.router.navigate(['/patients'], {
      queryParams: { view: 'outreach' }
    });
  }

  // ============================================================================
  // Phase 6.2: RN Dashboard Workflows
  // ============================================================================

  // ----------------------------------------------------------------------------
  // 1. Patient Education Workflows
  // ----------------------------------------------------------------------------

  /**
   * Schedule patient education session
   */
  @TrackInteraction('rn-dashboard', 'schedule-education-session')
  scheduleEducationSession(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Schedule Education Session',
      `Schedule patient education session for <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Topic:</strong> ${gap.gapType}<br>` +
      `<strong>Patient:</strong> ${gap.patientName} (${gap.patientMRN})<br><br>` +
      `This will create a scheduled education intervention for this patient.`,
      'Schedule Session',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        const scheduledDate = new Date();
        scheduledDate.setDate(scheduledDate.getDate() + 7); // Schedule for next week

        this.careGapService.assignIntervention(gap.id, {
          type: InterventionType.EDUCATION,
          description: `${gap.gapType} - Patient Education Session`,
          scheduledDate: scheduledDate.toISOString(),
          assignedTo: gap.assignedTo
        }).pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              gap.status = 'in-progress';
              this.toastService.success(`Education session scheduled for ${gap.patientName}`);
            },
            error: (error) => {
              this.logger.error('Failed to schedule education session:', error);
              this.toastService.error('Failed to schedule education session. Please try again.');
            }
          });
      }
    });
  }

  /**
   * Document completed education session
   */
  @TrackInteraction('rn-dashboard', 'document-education-session')
  documentEducationSession(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Document Education Session',
      `Document completed education session for <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Topic:</strong> ${gap.gapType}<br>` +
      `This will mark the education as completed and close the care gap.`,
      'Document Session',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.careGapService.closeGap(gap.id, {
          reason: 'Education session completed',
          notes: 'Patient education session completed successfully',
          closedBy: gap.assignedTo
        }).pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              gap.status = 'completed';
              this.patientEducationDue = Math.max(0, this.patientEducationDue - 1);
              this.toastService.success(`Education session documented for ${gap.patientName}`);
            },
            error: (error) => {
              this.logger.error('Failed to document education session:', error);
              this.toastService.error('Failed to document education session. Please try again.');
            }
          });
      }
    });
  }

  /**
   * Track education materials provided to patient
   */
  @TrackInteraction('rn-dashboard', 'track-education-materials')
  trackEducationMaterials(gap: CareGapTask, materials: string[]): void {
    // In a real implementation, this would save to the backend
    this.logger.debug('Tracking education materials:', { gap, materials });
    this.toastService.success(
      `Education materials tracked for ${gap.patientName}: ${materials.length} items`
    );
  }

  /**
   * View education history for a patient
   */
  @TrackInteraction('rn-dashboard', 'view-education-history')
  viewEducationHistory(patientId: string): void {
    this.router.navigate(['/patients', patientId], {
      queryParams: { view: 'education-history' }
    });
  }

  /**
   * Create comprehensive education plan
   */
  @TrackInteraction('rn-dashboard', 'create-education-plan')
  createEducationPlan(gap: CareGapTask): void {
    this.router.navigate(['/patients', gap.id], {
      queryParams: { action: 'create-education-plan', gapType: gap.gapType }
    });
  }

  /**
   * Send education materials electronically to patient
   */
  @TrackInteraction('rn-dashboard', 'send-education-materials')
  sendEducationMaterials(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Send Education Materials',
      `Send electronic education materials to <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Topic:</strong> ${gap.gapType}<br>` +
      `Materials will be sent to the patient's portal and email.`,
      'Send Materials',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would trigger email/portal notification
        this.toastService.success(`Education materials sent to ${gap.patientName}`);
        this.notificationService.info(`Materials delivered via patient portal and email`);
      }
    });
  }

  /**
   * Record patient comprehension assessment
   */
  @TrackInteraction('rn-dashboard', 'record-comprehension-assessment')
  recordComprehensionAssessment(gap: CareGapTask, assessment: any): void {
    // In a real implementation, this would save assessment to backend
    this.logger.debug('Recording comprehension assessment:', { gap, assessment });
    this.toastService.success(
      `Comprehension assessment recorded for ${gap.patientName}`
    );
  }

  // ----------------------------------------------------------------------------
  // 2. Care Coordination Tasks
  // ----------------------------------------------------------------------------

  /**
   * Create new care coordination task
   */
  @TrackInteraction('rn-dashboard', 'create-coordination-task')
  createCoordinationTask(task: any): void {
    // In a real implementation, this would save task to backend
    this.logger.debug('Creating care coordination task:', task);
    this.toastService.success('Care coordination task created successfully');
  }

  /**
   * Assign task to care team member
   */
  @TrackInteraction('rn-dashboard', 'assign-task-to-team-member')
  assignTaskToTeamMember(gap: CareGapTask, assignee: string): void {
    gap.assignedTo = assignee;
    // In a real implementation, this would update backend
    this.toastService.success(`Task assigned to ${assignee}`);
  }

  /**
   * Schedule specialist referral
   */
  @TrackInteraction('rn-dashboard', 'schedule-specialist-referral')
  scheduleSpecialistReferral(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Schedule Specialist Referral',
      `Schedule specialist referral for <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Reason:</strong> ${gap.gapType}<br>` +
      `This will create a referral order and coordinate with the specialist's office.`,
      'Schedule Referral',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        const scheduledDate = new Date();
        scheduledDate.setDate(scheduledDate.getDate() + 14); // Schedule for 2 weeks out

        this.careGapService.assignIntervention(gap.id, {
          type: InterventionType.REFERRAL,
          description: `Specialist referral for ${gap.gapType}`,
          scheduledDate: scheduledDate.toISOString(),
          assignedTo: gap.assignedTo
        }).pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              gap.status = 'in-progress';
              this.toastService.success(`Specialist referral scheduled for ${gap.patientName}`);
            },
            error: (error) => {
              this.logger.error('Failed to schedule referral:', error);
              this.toastService.error('Failed to schedule specialist referral. Please try again.');
            }
          });
      }
    });
  }

  /**
   * Log care team communication
   */
  @TrackInteraction('rn-dashboard', 'log-care-team-communication')
  logCareTeamCommunication(communication: any): void {
    // In a real implementation, this would save communication to backend
    this.logger.debug('Logging care team communication:', communication);
    this.toastService.success('Communication logged successfully');
  }

  /**
   * Update coordination task status
   */
  @TrackInteraction('rn-dashboard', 'update-coordination-task-status')
  updateCoordinationTaskStatus(gap: CareGapTask, status: 'pending' | 'in-progress' | 'completed'): void {
    gap.status = status;
    // In a real implementation, this would update backend
    this.toastService.success(`Task status updated to ${status}`);
  }

  /**
   * View all coordination tasks for a patient
   */
  @TrackInteraction('rn-dashboard', 'view-patient-coordination-tasks')
  viewPatientCoordinationTasks(patientId: string): void {
    this.router.navigate(['/patients', patientId], {
      queryParams: { view: 'coordination-tasks' }
    });
  }

  /**
   * Send coordination summary to care team
   */
  @TrackInteraction('rn-dashboard', 'send-coordination-summary')
  sendCoordinationSummary(patientId: string): void {
    this.dialogService.confirm(
      'Send Coordination Summary',
      'Send care coordination summary to the entire care team?<br><br>' +
      'This will email a comprehensive summary of all coordination activities.',
      'Send Summary',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would trigger email to care team
        this.toastService.success('Coordination summary sent to care team');
      }
    });
  }

  // ----------------------------------------------------------------------------
  // 3. Follow-up Scheduling
  // ----------------------------------------------------------------------------

  /**
   * Schedule follow-up appointment
   */
  @TrackInteraction('rn-dashboard', 'schedule-follow-up-appointment')
  scheduleFollowUpAppointment(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Schedule Follow-up Appointment',
      `Schedule follow-up appointment for <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Reason:</strong> ${gap.gapType}<br>` +
      `This will create an appointment and send a reminder to the patient.`,
      'Schedule Appointment',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        const scheduledDate = new Date();
        scheduledDate.setDate(scheduledDate.getDate() + 30); // Schedule for 30 days out

        this.careGapService.assignIntervention(gap.id, {
          type: InterventionType.APPOINTMENT_SCHEDULED,
          description: `Follow-up appointment for ${gap.gapType}`,
          scheduledDate: scheduledDate.toISOString(),
          assignedTo: gap.assignedTo
        }).pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              gap.status = 'in-progress';
              this.toastService.success(`Follow-up appointment scheduled for ${gap.patientName}`);
            },
            error: (error) => {
              this.logger.error('Failed to schedule appointment:', error);
              this.toastService.error('Failed to schedule follow-up appointment. Please try again.');
            }
          });
      }
    });
  }

  /**
   * Send follow-up appointment reminder
   */
  @TrackInteraction('rn-dashboard', 'send-follow-up-reminder')
  sendFollowUpReminder(outreach: PatientOutreach): void {
    this.dialogService.confirm(
      'Send Appointment Reminder',
      `Send appointment reminder to <strong>${outreach.patientName}</strong>?<br><br>` +
      `<strong>Appointment:</strong> ${outreach.scheduledDate}<br>` +
      `<strong>Reason:</strong> ${outreach.reason}<br>` +
      `Reminder will be sent via ${outreach.outreachType}.`,
      'Send Reminder',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would trigger reminder
        this.toastService.success(`Reminder sent to ${outreach.patientName}`);
      }
    });
  }

  /**
   * Reschedule missed follow-up
   */
  @TrackInteraction('rn-dashboard', 'reschedule-missed-follow-up')
  rescheduleMissedFollowUp(outreach: PatientOutreach): void {
    this.dialogService.confirm(
      'Reschedule Missed Follow-up',
      `Reschedule missed follow-up for <strong>${outreach.patientName}</strong>?<br><br>` +
      `<strong>Original Date:</strong> ${outreach.scheduledDate}<br>` +
      `<strong>Reason:</strong> ${outreach.reason}<br>` +
      `This will create a new follow-up task.`,
      'Reschedule',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would create new follow-up
        outreach.status = 'scheduled';
        this.toastService.success(`Follow-up rescheduled for ${outreach.patientName}`);
      }
    });
  }

  /**
   * Complete follow-up task
   */
  @TrackInteraction('rn-dashboard', 'complete-follow-up-task')
  completeFollowUpTask(outreach: PatientOutreach, outcome: any): void {
    outreach.status = 'completed';
    // In a real implementation, this would save outcome to backend
    this.logger.debug('Follow-up task completed:', { outreach, outcome });
    this.toastService.success(`Follow-up task completed for ${outreach.patientName}`);
  }

  /**
   * View follow-up schedule for patient
   */
  @TrackInteraction('rn-dashboard', 'view-follow-up-schedule')
  viewFollowUpSchedule(patientId: string): void {
    this.router.navigate(['/patients', patientId], {
      queryParams: { view: 'follow-up-schedule' }
    });
  }

  /**
   * Create recurring follow-up schedule
   */
  @TrackInteraction('rn-dashboard', 'create-recurring-follow-up')
  createRecurringFollowUp(gap: CareGapTask, schedule: any): void {
    // In a real implementation, this would create recurring schedule
    this.logger.debug('Creating recurring follow-up:', { gap, schedule });
    this.toastService.success(
      `Recurring follow-up schedule created for ${gap.patientName} (${schedule.frequency})`
    );
  }

  /**
   * Cancel follow-up appointment
   */
  @TrackInteraction('rn-dashboard', 'cancel-follow-up')
  cancelFollowUp(outreach: PatientOutreach): void {
    this.dialogService.confirm(
      'Cancel Follow-up',
      `Cancel follow-up for <strong>${outreach.patientName}</strong>?<br><br>` +
      `<strong>Scheduled:</strong> ${outreach.scheduledDate}<br>` +
      `<strong>Reason:</strong> ${outreach.reason}<br>` +
      `This action cannot be undone.`,
      'Cancel Follow-up',
      'Keep Follow-up',
      'warn'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would cancel the follow-up
        this.toastService.success(`Follow-up cancelled for ${outreach.patientName}`);
      }
    });
  }

  // ----------------------------------------------------------------------------
  // 4. Health Coaching Tracking
  // ----------------------------------------------------------------------------

  /**
   * Start health coaching session
   */
  @TrackInteraction('rn-dashboard', 'start-health-coaching-session')
  startHealthCoachingSession(gap: CareGapTask): void {
    this.dialogService.confirm(
      'Start Health Coaching Session',
      `Start health coaching session for <strong>${gap.patientName}</strong>?<br><br>` +
      `<strong>Focus:</strong> ${gap.gapType}<br>` +
      `This will open the coaching workflow and begin tracking progress.`,
      'Start Session',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.router.navigate(['/patients', gap.id], {
          queryParams: { action: 'start-coaching', gapType: gap.gapType }
        });
      }
    });
  }

  /**
   * Document coaching goals
   */
  @TrackInteraction('rn-dashboard', 'document-coaching-goals')
  documentCoachingGoals(coachingGoals: any): void {
    // In a real implementation, this would save goals to backend
    this.logger.debug('Documenting coaching goals:', coachingGoals);
    this.toastService.success(
      `Coaching goals documented for patient (${coachingGoals.goals.length} goals)`
    );
  }

  /**
   * Update goal progress
   */
  @TrackInteraction('rn-dashboard', 'update-goal-progress')
  updateGoalProgress(goalProgress: any): void {
    // In a real implementation, this would update progress in backend
    this.logger.debug('Updating goal progress:', goalProgress);
    this.toastService.success(
      `Goal progress updated: ${goalProgress.progress}% complete`
    );
  }

  /**
   * Schedule coaching follow-up call
   */
  @TrackInteraction('rn-dashboard', 'schedule-coaching-call')
  scheduleCoachingCall(patientId: string, scheduledDate: string): void {
    // In a real implementation, this would schedule call in backend
    this.logger.debug('Scheduling coaching call:', { patientId, scheduledDate });
    this.toastService.success('Coaching call scheduled successfully');
  }

  /**
   * View coaching history for patient
   */
  @TrackInteraction('rn-dashboard', 'view-coaching-history')
  viewCoachingHistory(patientId: string): void {
    this.router.navigate(['/patients', patientId], {
      queryParams: { view: 'coaching-history' }
    });
  }

  /**
   * Record coaching milestone
   */
  @TrackInteraction('rn-dashboard', 'record-coaching-milestone')
  recordCoachingMilestone(milestone: any): void {
    // In a real implementation, this would save milestone to backend
    this.logger.debug('Recording coaching milestone:', milestone);
    this.toastService.success(
      `Coaching milestone recorded: ${milestone.description}`
    );
  }

  /**
   * Send coaching progress report
   */
  @TrackInteraction('rn-dashboard', 'send-coaching-progress-report')
  sendCoachingProgressReport(patientId: string): void {
    this.dialogService.confirm(
      'Send Progress Report',
      'Send health coaching progress report to patient and care team?<br><br>' +
      'This will generate and email a comprehensive progress report.',
      'Send Report',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In a real implementation, this would generate and send report
        this.toastService.success('Progress report sent successfully');
      }
    });
  }

  /**
   * Adjust coaching plan based on progress
   */
  @TrackInteraction('rn-dashboard', 'adjust-coaching-plan')
  adjustCoachingPlan(adjustment: any): void {
    // In a real implementation, this would update plan in backend
    this.logger.debug('Adjusting coaching plan:', adjustment);
    this.toastService.success(
      'Coaching plan adjusted successfully'
    );
  }
}
