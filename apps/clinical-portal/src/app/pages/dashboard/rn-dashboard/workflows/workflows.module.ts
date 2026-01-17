/**
 * Workflows Module - Barrel Export
 *
 * Central export point for all Nurse Dashboard workflow components
 * Provides unified access to all 5 multi-step workflow components
 */

// Import components for typeof usage in WorkflowConfig
import { PatientOutreachWorkflowComponent, type OutreachWorkflowData as _OutreachWorkflowData } from './patient-outreach';
import { MedicationReconciliationWorkflowComponent, type MedicationReconciliationWorkflowData as _MedicationReconciliationWorkflowData } from './medication-reconciliation';
import { PatientEducationWorkflowComponent, type PatientEducationWorkflowData as _PatientEducationWorkflowData } from './patient-education';
import { ReferralCoordinationWorkflowComponent, type ReferralCoordinationWorkflowData as _ReferralCoordinationWorkflowData } from './referral-coordination';
import { CarePlanWorkflowComponent, type CarePlanWorkflowData as _CarePlanWorkflowData } from './care-plan';

// Patient Outreach Workflow
export {
  PatientOutreachWorkflowComponent,
  type OutreachWorkflowData,
  type OutreachWorkflowResult,
} from './patient-outreach';

// Medication Reconciliation Workflow
export {
  MedicationReconciliationWorkflowComponent,
  type MedicationReconciliationWorkflowData,
  type MedicationReconciliationResult,
  type PatientReportedMedication,
  type MedicationDiscrepancy,
} from './medication-reconciliation';

// Patient Education Workflow
export {
  PatientEducationWorkflowComponent,
  type PatientEducationWorkflowData,
  type PatientEducationResult,
  type EducationTopic,
  type EducationMaterial,
} from './patient-education';

// Referral Coordination Workflow
export {
  ReferralCoordinationWorkflowComponent,
  type ReferralCoordinationWorkflowData,
  type ReferralCoordinationResult,
  type Specialist,
  type InsuranceCoverage,
} from './referral-coordination';

// Care Plan Workflow
export {
  CarePlanWorkflowComponent,
  type CarePlanWorkflowData,
  type CarePlanResult,
  type CarePlanSummary,
  type Problem,
  type Goal,
  type Intervention,
  type TeamMember,
  type CarePlanTemplate,
} from './care-plan';

/**
 * Workflow Type - Union of all workflow types for discriminated union usage
 */
export type WorkflowType = 'outreach' | 'medication' | 'education' | 'referral' | 'care-plan';

/**
 * Workflow Configuration - Maps workflow types to their data types
 * Useful for type-safe workflow selection in dashboard
 */
export interface WorkflowConfig {
  outreach: { component: typeof PatientOutreachWorkflowComponent; data: _OutreachWorkflowData };
  medication: { component: typeof MedicationReconciliationWorkflowComponent; data: _MedicationReconciliationWorkflowData };
  education: { component: typeof PatientEducationWorkflowComponent; data: _PatientEducationWorkflowData };
  referral: { component: typeof ReferralCoordinationWorkflowComponent; data: _ReferralCoordinationWorkflowData };
  'care-plan': { component: typeof CarePlanWorkflowComponent; data: _CarePlanWorkflowData };
}

/**
 * Workflow Metadata - Configuration for each workflow
 * Useful for UI hints, labels, and routing
 */
export const WORKFLOW_METADATA = {
  outreach: {
    label: 'Patient Outreach',
    description: 'Log patient contact attempts and schedule follow-ups',
    icon: 'phone',
    category: 'communication',
  },
  medication: {
    label: 'Medication Reconciliation',
    description: 'Verify medications and identify discrepancies',
    icon: 'local_pharmacy',
    category: 'medication',
  },
  education: {
    label: 'Patient Education',
    description: 'Provide educational materials and assess understanding',
    icon: 'school',
    category: 'education',
  },
  referral: {
    label: 'Referral Coordination',
    description: 'Manage specialist referrals and appointments',
    icon: 'assignment',
    category: 'coordination',
  },
  'care-plan': {
    label: 'Care Plan Management',
    description: 'Create comprehensive care plans with goals and interventions',
    icon: 'description',
    category: 'planning',
  },
} as const;
