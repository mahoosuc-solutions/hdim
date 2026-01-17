/**
 * Nurse Workflow Domain Models
 *
 * Interfaces for patient outreach, medication reconciliation,
 * patient education, and referral coordination workflows.
 *
 * Aligns with backend nurse-workflow-service data structures.
 */

// ==================== Outreach Log Models ====================

/**
 * Contact method for patient outreach
 */
export enum ContactMethod {
  PHONE = 'PHONE',
  EMAIL = 'EMAIL',
  SMS = 'SMS',
  IN_PERSON = 'IN_PERSON',
  PATIENT_PORTAL = 'PATIENT_PORTAL',
  FAMILY_MEMBER = 'FAMILY_MEMBER',
}

/**
 * Outcome of outreach attempt
 */
export enum OutcomeType {
  SUCCESSFUL_CONTACT = 'SUCCESSFUL_CONTACT',
  NO_ANSWER = 'NO_ANSWER',
  LEFT_MESSAGE = 'LEFT_MESSAGE',
  CALL_REFUSED = 'CALL_REFUSED',
  LANGUAGE_BARRIER = 'LANGUAGE_BARRIER',
  ESCALATION_NEEDED = 'ESCALATION_NEEDED',
}

/**
 * Reason for outreach
 */
export enum OutreachReason {
  CARE_GAP = 'CARE_GAP',
  APPOINTMENT = 'APPOINTMENT',
  MEDICATION = 'MEDICATION',
  EDUCATION = 'EDUCATION',
  REFERRAL = 'REFERRAL',
  PREVENTIVE_CARE = 'PREVENTIVE_CARE',
  OTHER = 'OTHER',
}

/**
 * Patient outreach log entry
 */
export interface OutreachLog {
  id?: string;
  patientId: string;
  tenantId?: string;
  nurseId: string;
  contactMethod: ContactMethod;
  outcomeType: OutcomeType;
  reason: OutreachReason;
  contactedAt: Date | string;
  nextFollowUpDate?: Date | string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Outreach metrics for patient
 */
export interface OutreachMetrics {
  totalAttempts: number;
  successfulContacts: number;
  successRate: number; // percentage
  lastContactDate?: Date;
  nextFollowUpDate?: Date;
}

// ==================== Medication Reconciliation Models ====================

/**
 * Medication reconciliation status
 */
export enum MedicationReconciliationStatus {
  REQUESTED = 'REQUESTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/**
 * What triggered medication reconciliation
 */
export enum MedicationReconciliationTrigger {
  HOSPITAL_ADMISSION = 'HOSPITAL_ADMISSION',
  HOSPITAL_DISCHARGE = 'HOSPITAL_DISCHARGE',
  ED_VISIT = 'ED_VISIT',
  SPECIALTY_REFERRAL = 'SPECIALTY_REFERRAL',
  MEDICATION_CHANGE = 'MEDICATION_CHANGE',
  ROUTINE = 'ROUTINE',
  PATIENT_REQUEST = 'PATIENT_REQUEST',
}

/**
 * Patient understanding assessment via teach-back
 */
export enum PatientUnderstanding {
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR',
}

/**
 * Insurance authorization status
 */
export enum AuthorizationStatus {
  NOT_REQUIRED = 'NOT_REQUIRED',
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  APPROVED_LIMITED = 'APPROVED_LIMITED',
  DENIED = 'DENIED',
  APPEAL_PENDING = 'APPEAL_PENDING',
}

/**
 * Medication reconciliation workflow
 */
export interface MedicationReconciliation {
  id?: string;
  patientId: string;
  tenantId?: string;
  reconcilerId: string;
  status: MedicationReconciliationStatus;
  triggerType: MedicationReconciliationTrigger;
  medicationCount: number;
  discrepancyCount: number;
  patientEducationProvided: boolean;
  patientUnderstanding: PatientUnderstanding;
  authorizationNumber?: string;
  authorizationStatus: AuthorizationStatus;
  startedAt: Date | string;
  completedAt?: Date | string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Medication reconciliation metrics
 */
export interface MedicationReconciliationMetrics {
  totalReconciliations: number;
  pendingReconciliations: number;
  completionRate: number; // percentage
}

// ==================== Patient Education Models ====================

/**
 * Education material type
 */
export enum EducationMaterialType {
  DIABETES_MANAGEMENT = 'DIABETES_MANAGEMENT',
  HYPERTENSION_CONTROL = 'HYPERTENSION_CONTROL',
  HEART_FAILURE = 'HEART_FAILURE',
  COPD = 'COPD',
  ASTHMA = 'ASTHMA',
  MENTAL_HEALTH = 'MENTAL_HEALTH',
  MEDICATION_ADHERENCE = 'MEDICATION_ADHERENCE',
  NUTRITION = 'NUTRITION',
  EXERCISE = 'EXERCISE',
  SMOKING_CESSATION = 'SMOKING_CESSATION',
  PREVENTIVE_CARE = 'PREVENTIVE_CARE',
  PAIN_MANAGEMENT = 'PAIN_MANAGEMENT',
  WOUND_CARE = 'WOUND_CARE',
  INFECTION_PREVENTION = 'INFECTION_PREVENTION',
}

/**
 * How education was delivered
 */
export enum EducationDeliveryMethod {
  IN_PERSON = 'IN_PERSON',
  PHONE = 'PHONE',
  VIDEO_CALL = 'VIDEO_CALL',
  EMAIL = 'EMAIL',
  PATIENT_PORTAL = 'PATIENT_PORTAL',
  PRINTED_MATERIALS = 'PRINTED_MATERIALS',
  MULTIMEDIA = 'MULTIMEDIA',
  GROUP_SESSION = 'GROUP_SESSION',
  ONE_ON_ONE = 'ONE_ON_ONE',
}

/**
 * Learning barriers identified
 */
export interface EducationBarriers {
  healthLiteracy?: boolean;
  language?: boolean;
  cognitive?: boolean;
  emotional?: boolean;
}

/**
 * Patient education log entry
 */
export interface PatientEducationLog {
  id?: string;
  patientId: string;
  tenantId?: string;
  educatorId: string;
  materialType: EducationMaterialType;
  deliveryMethod: EducationDeliveryMethod;
  patientUnderstanding: PatientUnderstanding;
  teachBackAssessment?: string;
  interpreterUsed: boolean;
  caregiverInvolved: boolean;
  barriers?: EducationBarriers;
  followUpNeeded: boolean;
  followUpScheduled?: Date | string;
  deliveredAt: Date | string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Patient education metrics
 */
export interface PatientEducationMetrics {
  totalEducationSessions: number;
  sessionsWithPoorUnderstanding: number;
  materialTypesCovered: EducationMaterialType[];
  lastEducationDate?: Date;
}

// ==================== Referral Coordination Models ====================

/**
 * Referral status (closed-loop tracking)
 */
export enum ReferralStatus {
  PENDING_AUTHORIZATION = 'PENDING_AUTHORIZATION',
  AUTHORIZED = 'AUTHORIZED',
  SCHEDULED = 'SCHEDULED',
  AWAITING_APPOINTMENT = 'AWAITING_APPOINTMENT',
  COMPLETED = 'COMPLETED',
  NO_SHOW = 'NO_SHOW',
  CANCELLED = 'CANCELLED',
}

/**
 * Priority level for referral
 */
export enum ReferralPriority {
  ROUTINE = 'ROUTINE',
  URGENT = 'URGENT',
  STAT = 'STAT',
}

/**
 * Appointment status within referral
 */
export enum AppointmentStatus {
  NOT_SCHEDULED = 'NOT_SCHEDULED',
  SCHEDULED = 'SCHEDULED',
  RESCHEDULED = 'RESCHEDULED',
  NO_SHOW = 'NO_SHOW',
  ATTENDED = 'ATTENDED',
}

/**
 * Results receipt status
 */
export enum ResultsStatus {
  NOT_RECEIVED = 'NOT_RECEIVED',
  RECEIVED = 'RECEIVED',
  REVIEWED = 'REVIEWED',
  COMMUNICATED_TO_PATIENT = 'COMMUNICATED_TO_PATIENT',
}

/**
 * Referral coordination workflow
 */
export interface ReferralCoordination {
  id?: string;
  patientId: string;
  tenantId?: string;
  coordinatorId: string;
  specialtyType: string; // e.g., 'Cardiology', 'Dermatology'
  status: ReferralStatus;
  priority: ReferralPriority;
  authorizationNumber?: string;
  authorizationStatus: AuthorizationStatus;
  appointmentStatus: AppointmentStatus;
  appointmentDate?: Date | string;
  resultsStatus: ResultsStatus;
  resultsReceivedDate?: Date | string;
  medicalRecordsTransmitted: boolean;
  specialistContactEncrypted?: string;
  requestedAt: Date | string;
  authorizedAt?: Date | string;
  scheduledAt?: Date | string;
  completedAt?: Date | string;
  noShowReason?: string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Referral metrics
 */
export interface ReferralMetrics {
  totalReferrals: number;
  pendingReferrals: number;
  completionRate: number; // percentage
  awaitingScheduling: number;
  awaitingResults: number;
}

// ==================== Unified Nurse Workflow Context ====================

/**
 * Aggregated nurse workflow context for dashboard
 */
export interface NurseWorkflowContext {
  patientId: string;
  patientName: string;
  assignedNurseId: string;

  // Outreach
  outreach: {
    logs: OutreachLog[];
    metrics: OutreachMetrics;
    pendingFollowUps: OutreachLog[];
  };

  // Medication Reconciliation
  medicationReconciliation: {
    current?: MedicationReconciliation;
    pending: MedicationReconciliation[];
    metrics: MedicationReconciliationMetrics;
  };

  // Patient Education
  patientEducation: {
    logs: PatientEducationLog[];
    metrics: PatientEducationMetrics;
    needsFollowUp: PatientEducationLog[];
  };

  // Referral Coordination
  referralCoordination: {
    referrals: ReferralCoordination[];
    metrics: ReferralMetrics;
    awaitingScheduling: ReferralCoordination[];
    awaitingResults: ReferralCoordination[];
  };

  lastUpdated: Date;
}

/**
 * Pagination wrapper for list responses
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
