/**
 * Nurse Workflow Services Module - Public API
 *
 * Exports all nurse workflow services and domain models
 * for easy consumption by components and other services.
 *
 * Usage:
 * import { NurseWorkflowService } from '@app/services/nurse-workflow';
 * import { OutreachLog, MedicationReconciliation, ... } from '@app/services/nurse-workflow';
 */

// Service exports
export { NurseWorkflowService } from './nurse-workflow.service';

// Enum exports (values)
export {
  // Outreach
  ContactMethod,
  OutcomeType,
  OutreachReason,
  // Medication Reconciliation
  MedicationReconciliationStatus,
  MedicationReconciliationTrigger,
  PatientUnderstanding,
  AuthorizationStatus,
  // Patient Education
  EducationMaterialType,
  EducationDeliveryMethod,
  // Referral Coordination
  ReferralStatus,
  ReferralPriority,
  AppointmentStatus,
  ResultsStatus,
} from './nurse-workflow.models';

// Type exports (interfaces)
export type {
  OutreachLog,
  OutreachMetrics,
  MedicationReconciliation,
  MedicationReconciliationMetrics,
  EducationBarriers,
  PatientEducationLog,
  PatientEducationMetrics,
  ReferralCoordination,
  ReferralMetrics,
  NurseWorkflowContext,
  PaginatedResponse,
} from './nurse-workflow.models';
