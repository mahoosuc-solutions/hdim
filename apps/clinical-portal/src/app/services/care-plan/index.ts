/**
 * Care Plan Services Module - Public API
 *
 * Exports all care plan services and domain models
 * for easy consumption by components and other services.
 *
 * Usage:
 * import { CarePlanService } from '@app/services/care-plan';
 * import { CarePlan, CarePlanGoal, ... } from '@app/services/care-plan';
 */

// Service exports
export { CarePlanService, CARE_PLAN_BASE_URL } from './care-plan.service';

// Enum exports (values)
export {
  // Care Plan
  CarePlanStatus,
  CarePlanTemplate,
  CarePlanPriority,
  // Problem
  ProblemSeverity,
  ProblemStatus,
  // Goal
  GoalStatus,
  MeasurementUnit,
  // Intervention
  InterventionType,
  InterventionFrequency,
  InterventionStatus,
  // Team
  TeamMemberRole,
  // Engagement
  PatientEngagementLevel,
  // Review
  ReviewType,
  // Transition
  TransitionType,
} from './care-plan.models';

// Type exports (interfaces)
export type {
  CarePlan,
  CarePlanProblem,
  CarePlanGoal,
  CarePlanIntervention,
  CarePlanTeamMember,
  PatientEngagement,
  CarePlanReview,
  CareTransition,
  CarePlanMetrics,
  CarePlanContext,
  PaginatedResponse,
} from './care-plan.models';
