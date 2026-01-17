/**
 * Care Plan Management Domain Models
 *
 * Interfaces and enums for comprehensive care plan operations including:
 * - Care plan creation and management
 * - Problem/diagnosis tracking
 * - Goal setting and progress tracking
 * - Intervention/action items
 * - Patient/caregiver engagement
 * - Multidisciplinary team coordination
 *
 * Aligns with backend care-plan-service data structures.
 */

// ==================== Care Plan Status Models ====================

/**
 * Overall care plan status
 */
export enum CarePlanStatus {
  DRAFT = 'DRAFT',
  IN_DEVELOPMENT = 'IN_DEVELOPMENT',
  ACTIVE = 'ACTIVE',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/**
 * Care plan template type
 */
export enum CarePlanTemplate {
  CHRONIC_DISEASE = 'CHRONIC_DISEASE',
  POST_ACUTE = 'POST_ACUTE',
  BEHAVIORAL_HEALTH = 'BEHAVIORAL_HEALTH',
  MATERNITY = 'MATERNITY',
  PEDIATRIC = 'PEDIATRIC',
  COMPLEX_CARE = 'COMPLEX_CARE',
  PALLIATIVE = 'PALLIATIVE',
  TRANSITION_OF_CARE = 'TRANSITION_OF_CARE',
  CUSTOM = 'CUSTOM',
}

/**
 * Care plan priority level
 */
export enum CarePlanPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

/**
 * Master care plan record
 */
export interface CarePlan {
  id?: string;
  patientId: string;
  tenantId?: string;
  primaryCaregiverId: string;
  templateType: CarePlanTemplate;
  status: CarePlanStatus;
  priority: CarePlanPriority;
  title: string;
  description?: string;
  planStartDate: Date | string;
  planEndDate?: Date | string;
  reviewSchedule?: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY';
  lastReviewDate?: Date | string;
  nextReviewDate?: Date | string;
  problemCount: number;
  goalCount: number;
  interventionCount: number;
  patientEngaged: boolean;
  caregiverEngaged?: boolean;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Problem/Diagnosis Models ====================

/**
 * Problem/diagnosis severity
 */
export enum ProblemSeverity {
  MILD = 'MILD',
  MODERATE = 'MODERATE',
  SEVERE = 'SEVERE',
  CRITICAL = 'CRITICAL',
}

/**
 * Problem/diagnosis status
 */
export enum ProblemStatus {
  ACTIVE = 'ACTIVE',
  RESOLVED = 'RESOLVED',
  RESOLVED_WITH_SEQUELAE = 'RESOLVED_WITH_SEQUELAE',
  INACTIVE = 'INACTIVE',
}

/**
 * Problem/diagnosis entry
 */
export interface CarePlanProblem {
  id?: string;
  carePlanId: string;
  tenantId?: string;
  icd10Code?: string;
  snomedCode?: string;
  problemName: string;
  description?: string;
  severity: ProblemSeverity;
  status: ProblemStatus;
  onsetDate: Date | string;
  resolutionDate?: Date | string;
  clinicalNotes?: string;
  relatedProblems?: string[]; // IDs of related problems
  priorityOrder: number;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Goal Models ====================

/**
 * Care plan goal status
 */
export enum GoalStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  ACHIEVED = 'ACHIEVED',
  NOT_ACHIEVED = 'NOT_ACHIEVED',
}

/**
 * Goal measurement unit type
 */
export enum MeasurementUnit {
  NUMERIC = 'NUMERIC',
  PERCENTAGE = 'PERCENTAGE',
  BINARY = 'BINARY', // Yes/No
  CATEGORICAL = 'CATEGORICAL',
  QUALITATIVE = 'QUALITATIVE',
}

/**
 * Care plan goal/objective
 */
export interface CarePlanGoal {
  id?: string;
  carePlanId: string;
  problemId?: string;
  tenantId?: string;
  goalStatement: string;
  description?: string;
  targetDate: Date | string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: GoalStatus;
  measurementType: MeasurementUnit;
  baselineValue?: string | number;
  targetValue?: string | number;
  currentValue?: string | number;
  progressNotes?: string;
  achievedDate?: Date | string;
  followUpNeeded: boolean;
  createdBy?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Intervention Models ====================

/**
 * Intervention type
 */
export enum InterventionType {
  MEDICATION_MANAGEMENT = 'MEDICATION_MANAGEMENT',
  SELF_MONITORING = 'SELF_MONITORING',
  PATIENT_EDUCATION = 'PATIENT_EDUCATION',
  LIFESTYLE_MODIFICATION = 'LIFESTYLE_MODIFICATION',
  REFERRAL = 'REFERRAL',
  FOLLOW_UP_VISIT = 'FOLLOW_UP_VISIT',
  ASSESSMENT = 'ASSESSMENT',
  CARE_COORDINATION = 'CARE_COORDINATION',
  PSYCHOSOCIAL_SUPPORT = 'PSYCHOSOCIAL_SUPPORT',
  DEVICE_MANAGEMENT = 'DEVICE_MANAGEMENT',
}

/**
 * Intervention frequency/schedule
 */
export enum InterventionFrequency {
  DAILY = 'DAILY',
  TWICE_DAILY = 'TWICE_DAILY',
  WEEKLY = 'WEEKLY',
  BIWEEKLY = 'BIWEEKLY',
  MONTHLY = 'MONTHLY',
  AS_NEEDED = 'AS_NEEDED',
  ONCE = 'ONCE',
}

/**
 * Intervention status
 */
export enum InterventionStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  DISCONTINUED = 'DISCONTINUED',
  DEFERRED = 'DEFERRED',
}

/**
 * Care plan intervention/action item
 */
export interface CarePlanIntervention {
  id?: string;
  carePlanId: string;
  goalId?: string;
  tenantId?: string;
  interventionType: InterventionType;
  description: string;
  actionStatement: string;
  assignedTo?: string; // User/role ID
  targetBehavior?: string;
  rationale?: string;
  status: InterventionStatus;
  startDate?: Date | string;
  expectedCompletionDate?: Date | string;
  completionDate?: Date | string;
  frequency: InterventionFrequency;
  durationDays?: number;
  successCriteria?: string;
  responsibleParty?: 'PATIENT' | 'CAREGIVER' | 'PROVIDER' | 'TEAM';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Team Member Models ====================

/**
 * Care team member role
 */
export enum TeamMemberRole {
  PRIMARY_CARE_PHYSICIAN = 'PRIMARY_CARE_PHYSICIAN',
  SPECIALIST = 'SPECIALIST',
  NURSE = 'NURSE',
  SOCIAL_WORKER = 'SOCIAL_WORKER',
  NUTRITIONIST = 'NUTRITIONIST',
  PHARMACIST = 'PHARMACIST',
  MENTAL_HEALTH_PROVIDER = 'MENTAL_HEALTH_PROVIDER',
  PHYSICAL_THERAPIST = 'PHYSICAL_THERAPIST',
  OCCUPATIONAL_THERAPIST = 'OCCUPATIONAL_THERAPIST',
  CARE_COORDINATOR = 'CARE_COORDINATOR',
  CASE_MANAGER = 'CASE_MANAGER',
  COMMUNITY_HEALTH_WORKER = 'COMMUNITY_HEALTH_WORKER',
  CAREGIVER = 'CAREGIVER',
  FAMILY_MEMBER = 'FAMILY_MEMBER',
}

/**
 * Care team member
 */
export interface CarePlanTeamMember {
  id?: string;
  carePlanId: string;
  tenantId?: string;
  userId: string;
  fullName?: string;
  role: TeamMemberRole;
  organization?: string;
  contactNumber?: string;
  email?: string;
  isPrimaryCoordinator: boolean;
  assignedDate: Date | string;
  removedDate?: Date | string;
}

// ==================== Patient Engagement Models ====================

/**
 * Patient engagement status
 */
export enum PatientEngagementLevel {
  NOT_ENGAGED = 'NOT_ENGAGED',
  MINIMALLY_ENGAGED = 'MINIMALLY_ENGAGED',
  MODERATELY_ENGAGED = 'MODERATELY_ENGAGED',
  HIGHLY_ENGAGED = 'HIGHLY_ENGAGED',
}

/**
 * Patient engagement record
 */
export interface PatientEngagement {
  id?: string;
  carePlanId: string;
  patientId: string;
  tenantId?: string;
  engagementLevel: PatientEngagementLevel;
  planReviewedWithPatient: boolean;
  planReviewDate?: Date | string;
  patientAgreement: boolean;
  agreedDate?: Date | string;
  barriers?: string[];
  motivations?: string[];
  preferredCommunicationChannel?: string;
  lastEngagementDate?: Date | string;
  engagementNotes?: string;
  updatedAt?: Date | string;
}

// ==================== Care Plan Review Models ====================

/**
 * Care plan review type
 */
export enum ReviewType {
  SCHEDULED = 'SCHEDULED',
  UNSCHEDULED = 'UNSCHEDULED',
  PROGRESS_CHECK = 'PROGRESS_CHECK',
  TRANSITION_REVIEW = 'TRANSITION_REVIEW',
  ESCALATION = 'ESCALATION',
}

/**
 * Care plan review record
 */
export interface CarePlanReview {
  id?: string;
  carePlanId: string;
  tenantId?: string;
  reviewType: ReviewType;
  reviewDate: Date | string;
  reviewedBy: string;
  goalProgressSummary?: string;
  interventionCompletionRate?: number;
  planModifications?: string;
  newProblemsIdentified?: string;
  resolvedProblems?: string[];
  recommendedActions?: string;
  needsEscalation: boolean;
  escalationReason?: string;
  nextReviewDate?: Date | string;
  reviewNotes?: string;
  createdAt?: Date | string;
}

// ==================== Care Transition Models ====================

/**
 * Transition type
 */
export enum TransitionType {
  HOSPITAL_TO_HOME = 'HOSPITAL_TO_HOME',
  FACILITY_TO_HOME = 'FACILITY_TO_HOME',
  HOME_TO_FACILITY = 'HOME_TO_FACILITY',
  SPECIALIST_REFERRAL = 'SPECIALIST_REFERRAL',
  CARE_LEVEL_CHANGE = 'CARE_LEVEL_CHANGE',
}

/**
 * Care transition plan
 */
export interface CareTransition {
  id?: string;
  carePlanId: string;
  patientId: string;
  tenantId?: string;
  transitionType: TransitionType;
  fromLocation?: string;
  toLocation?: string;
  fromProvider?: string;
  toProvider?: string;
  transitionDate: Date | string;
  confirmationDeadline?: Date | string;
  medicationReconciliationCompleted: boolean;
  handoffSummaryProvided: boolean;
  followUpAppointmentScheduled: boolean;
  followUpAppointmentDate?: Date | string;
  educationProvided: boolean;
  contactInformationShared: boolean;
  transitionNotes?: string;
  status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Care Plan Metrics ====================

/**
 * Care plan metrics and KPIs
 */
export interface CarePlanMetrics {
  patientId: string;
  totalCarePlans: number;
  activeCarePlans: number;
  completedCarePlans: number;
  totalGoals: number;
  achievedGoals: number;
  inProgressGoals: number;
  goalCompletionRate: number; // percentage
  interventionCompletionRate: number; // percentage
  patientEngagementScore: number; // 0-100
  teamEngagementScore: number; // 0-100
  averagePlanReviewFrequency?: number; // days
  lastCarePlanStartDate?: Date;
}

// ==================== Unified Care Plan Context ====================

/**
 * Aggregated care plan context for clinical dashboard
 */
export interface CarePlanContext {
  patientId: string;
  patientName: string;
  coordinatorId: string;

  // Active Plans
  activePlans: {
    plans: CarePlan[];
    totalCount: number;
    criticalCount: number;
  };

  // Problems
  problems: {
    activeProblems: CarePlanProblem[];
    resolvedProblems: CarePlanProblem[];
    totalCount: number;
  };

  // Goals
  goals: {
    allGoals: CarePlanGoal[];
    notStartedCount: number;
    inProgressCount: number;
    achievedCount: number;
    achievementRate: number; // percentage
  };

  // Interventions
  interventions: {
    allInterventions: CarePlanIntervention[];
    plannedCount: number;
    inProgressCount: number;
    completedCount: number;
    completionRate: number; // percentage
  };

  // Team
  teamMembers: {
    members: CarePlanTeamMember[];
    primaryCoordinator?: CarePlanTeamMember;
    specialistCount: number;
  };

  // Engagement
  patientEngagement?: PatientEngagement;
  lastReviewDate?: Date;
  nextReviewDate?: Date;

  // Metrics
  metrics: CarePlanMetrics;

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
