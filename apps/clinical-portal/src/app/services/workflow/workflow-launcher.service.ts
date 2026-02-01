/**
 * Workflow Launcher Service
 *
 * Manages type-safe launching of multi-step workflow dialogs.
 * Routes care gap tasks to appropriate workflow components with
 * proper data transformation and completion callbacks.
 */

import { Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { LoggerService, ContextualLogger } from '../logger.service';
import { ToastService } from '../toast.service';
import { CarePlanService } from '../care-plan/care-plan.service';
import { NurseWorkflowService } from '../nurse-workflow/nurse-workflow.service';
import { MedicationService } from '../medication/medication.service';

// Import all workflow components
import {
  PatientOutreachWorkflowComponent,
  type OutreachWorkflowData,
} from '../../pages/dashboard/rn-dashboard/workflows/patient-outreach';
import {
  MedicationReconciliationWorkflowComponent,
  type MedicationReconciliationWorkflowData,
} from '../../pages/dashboard/rn-dashboard/workflows/medication-reconciliation';
import {
  PatientEducationWorkflowComponent,
  type PatientEducationWorkflowData,
} from '../../pages/dashboard/rn-dashboard/workflows/patient-education';
import {
  ReferralCoordinationWorkflowComponent,
  type ReferralCoordinationWorkflowData,
} from '../../pages/dashboard/rn-dashboard/workflows/referral-coordination';
import {
  CarePlanWorkflowComponent,
  type CarePlanWorkflowData,
} from '../../pages/dashboard/rn-dashboard/workflows/care-plan';

/**
 * Discriminated union type for all supported workflow types
 */
export type WorkflowType = 'outreach' | 'medication' | 'education' | 'referral' | 'care-plan';

/**
 * Workflow task interface for consistent task data across dashboard
 */
export interface WorkflowTask {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN?: string;
  category: string;
  gapType: string;
  priority: 'high' | 'medium' | 'low';
  dueDate: string;
  status: 'pending' | 'in-progress' | 'completed';
}

/**
 * Workflow completion result
 */
export interface WorkflowResult {
  success: boolean;
  workflowType: WorkflowType;
  taskId: string;
  result?: any;
  error?: string;
}

@Injectable({
  providedIn: 'root',
})
export class WorkflowLauncherService {

  // Component map for type-safe workflow selection
  private readonly componentMap: Record<WorkflowType, any> = {
    outreach: PatientOutreachWorkflowComponent,
    medication: MedicationReconciliationWorkflowComponent,
    education: PatientEducationWorkflowComponent,
    referral: ReferralCoordinationWorkflowComponent,
    'care-plan': CarePlanWorkflowComponent,
  };

  // Workflow configuration map
  private readonly workflowConfig = {
    outreach: { label: 'Patient Outreach', icon: 'phone' },
    medication: { label: 'Medication Reconciliation', icon: 'local_pharmacy' },
    education: { label: 'Patient Education', icon: 'school' },
    referral: { label: 'Referral Coordination', icon: 'assignment' },
    'care-plan': { label: 'Care Plan Management', icon: 'description' },
  };

  constructor(
    private dialog: MatDialog,
    private logger: LoggerService,
    private toastService: ToastService,
    private carePlanService: CarePlanService,
    private nurseWorkflowService: NurseWorkflowService,
    private medicationService: MedicationService
  ) {
  }

  /**
   * Launch a workflow dialog based on task type
   *
   * @param workflowType Type of workflow to launch
   * @param task Task containing data for the workflow
   * @param onComplete Optional callback when workflow completes
   * @returns MatDialogRef for the opened dialog
   */
  launchWorkflow(
    workflowType: WorkflowType,
    task: WorkflowTask,
    onComplete?: (result: WorkflowResult) => void
  ): MatDialogRef<any> {
    const config = this.workflowConfig[workflowType];
    this.logger.info(`Launching ${config.label} workflow for task ${task.id}`);

    // Get component for this workflow type
    const component = this.componentMap[workflowType];
    if (!component) {
      this.logger.error(`Unknown workflow type: ${workflowType}`);
      this.toastService.error(`Unknown workflow type: ${workflowType}`);
      throw new Error(`Unknown workflow type: ${workflowType}`);
    }

    // Prepare dialog data based on workflow type
    const dialogData = this.prepareWorkflowData(workflowType, task);

    // Open dialog
    const dialogRef = this.dialog.open(component, {
      width: '100%',
      maxWidth: '900px',
      data: dialogData,
      disableClose: false,
      panelClass: `workflow-dialog-${workflowType}`,
    });

    // Handle dialog completion
    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        this.logger.info(`${config.label} completed successfully`);
        this.toastService.success(`${config.label} completed successfully`);

        // Prepare result object
        const workflowResult: WorkflowResult = {
          success: true,
          workflowType,
          taskId: task.id,
          result,
        };

        // Call completion callback if provided
        if (onComplete) {
          onComplete(workflowResult);
        }
      } else if (result?.success === false) {
        this.logger.info(`${config.label} cancelled by user`);
      }
    });

    return dialogRef;
  }

  /**
   * Determine workflow type from care gap category
   *
   * @param category Care gap category
   * @returns Workflow type
   */
  mapCategoryToWorkflow(category: string): WorkflowType {
    switch (category.toLowerCase()) {
      case 'communication':
      case 'outreach':
        return 'outreach';
      case 'medication':
        return 'medication';
      case 'education':
        return 'education';
      case 'coordination':
      case 'referral':
        return 'referral';
      case 'planning':
      case 'care-plan':
        return 'care-plan';
      default:
        throw new Error(`Unknown category: ${category}`);
    }
  }

  /**
   * Get workflow configuration (label, icon, etc.)
   *
   * @param workflowType Type of workflow
   * @returns Configuration object
   */
  getWorkflowConfig(workflowType: WorkflowType) {
    return this.workflowConfig[workflowType];
  }

  /**
   * Prepare dialog data based on workflow type
   * Transforms task data to workflow-specific data format
   *
   * @param workflowType Type of workflow
   * @param task Task data
   * @returns Workflow-specific dialog data
   */
  private prepareWorkflowData(workflowType: WorkflowType, task: WorkflowTask): any {
    switch (workflowType) {
      case 'outreach':
        return {
          outreachLogId: task.id,
          patientId: task.patientId,
          patientName: task.patientName,
        } as OutreachWorkflowData;

      case 'medication':
        return {
          reconciliationId: task.id,
          patientId: task.patientId,
          patientName: task.patientName,
        } as MedicationReconciliationWorkflowData;

      case 'education':
        return {
          educationSessionId: task.id,
          patientId: task.patientId,
          patientName: task.patientName,
        } as PatientEducationWorkflowData;

      case 'referral':
        return {
          referralId: task.id,
          patientId: task.patientId,
          patientName: task.patientName,
          referralType: task.gapType,
        } as ReferralCoordinationWorkflowData;

      case 'care-plan':
        return {
          carePlanId: task.id,
          patientId: task.patientId,
          patientName: task.patientName,
        } as CarePlanWorkflowData;

      default:
        const _exhaustiveCheck: never = workflowType;
        return _exhaustiveCheck;
    }
  }
}
