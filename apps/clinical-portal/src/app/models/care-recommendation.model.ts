/**
 * Care Recommendation Dashboard Models
 *
 * Models for the Care Recommendations dashboard including
 * recommendations, filtering, sorting, and bulk actions.
 */

import { RiskLevel } from './patient-health.model';

/**
 * Care Recommendation Category
 */
export type RecommendationCategory =
  | 'preventive'
  | 'chronic-disease'
  | 'medication'
  | 'mental-health'
  | 'sdoh';

/**
 * Recommendation Urgency Level
 */
export type RecommendationUrgency = 'routine' | 'soon' | 'urgent' | 'emergent';

/**
 * Recommendation Status
 */
export type RecommendationStatus =
  | 'pending'
  | 'in-progress'
  | 'completed'
  | 'declined'
  | 'cancelled';

/**
 * Recommendation Source Type
 */
export type RecommendationType = 'care-gap' | 'recommendation' | 'cds-alert';

/**
 * Dashboard View Mode
 */
export type DashboardViewMode = 'list' | 'grid' | 'kanban';

/**
 * Group By Field
 */
export type GroupByField =
  | 'urgency'
  | 'category'
  | 'patientRiskLevel'
  | 'status'
  | 'none';

/**
 * Sort Field
 */
export type SortField =
  | 'urgency'
  | 'category'
  | 'dueDate'
  | 'patientName'
  | 'riskLevel'
  | 'createdDate';

/**
 * Patient Risk Summary for dashboard display
 */
export interface PatientRiskSummary {
  patientId: string;
  patientName: string;
  mrn: string;
  overallRisk: RiskLevel;
  riskScore: number;
  activeRecommendations: number;
  openCareGaps: number;
}

/**
 * Dashboard Recommendation - Combined view of care gaps and recommendations
 */
export interface DashboardRecommendation {
  id: string;
  type: RecommendationType;

  // Patient Information
  patientId: string;
  patientName: string;
  mrn: string;
  patientRiskLevel: RiskLevel;

  // Recommendation Details
  category: RecommendationCategory;
  title: string;
  description: string;
  urgency: RecommendationUrgency;
  priority: number; // 1-10, higher = more urgent

  // Status & Dates
  status: RecommendationStatus;
  createdDate: Date;
  dueDate?: Date;
  daysOverdue?: number;
  completedDate?: Date;

  // Actions
  actionItems: string[];
  suggestedIntervention?: string;

  // Source Information
  measureId?: string;
  measureName?: string;
  evidenceSource?: string;
  clinicalGuideline?: string;
  relatedCareGapId?: string;

  // Metadata
  createdBy?: string;
  lastUpdatedBy?: string;
  lastUpdatedDate?: Date;
}

/**
 * Recommendation Filter Configuration
 */
export interface RecommendationFilterConfig {
  urgency: RecommendationUrgency[];
  category: RecommendationCategory[];
  patientRiskLevel: RiskLevel[];
  status: RecommendationStatus[];
  daysOverdueRange: { min: number | null; max: number | null };
  patientSearch: string;
  dateRange: { start: Date | null; end: Date | null };
}

/**
 * Recommendation Sort Configuration
 */
export interface RecommendationSortConfig {
  field: SortField;
  direction: 'asc' | 'desc';
}

/**
 * Dashboard Statistics Summary
 */
export interface RecommendationDashboardStats {
  totalRecommendations: number;
  byUrgency: {
    emergent: number;
    urgent: number;
    soon: number;
    routine: number;
  };
  byCategory: {
    preventive: number;
    chronicDisease: number;
    medication: number;
    mentalHealth: number;
    sdoh: number;
  };
  byPatientRisk: {
    critical: number;
    high: number;
    moderate: number;
    low: number;
  };
  byStatus: {
    pending: number;
    inProgress: number;
    completed: number;
    declined: number;
  };
  overdueSummary: {
    total: number;
    critical: number; // > 30 days
    warning: number; // 15-30 days
    approaching: number; // 1-14 days
  };
}

/**
 * Bulk Action Type
 */
export type BulkActionType =
  | 'accept'
  | 'decline'
  | 'complete'
  | 'assign'
  | 'reschedule';

/**
 * Bulk Action Request
 */
export interface BulkActionRequest {
  recommendationIds: string[];
  action: BulkActionType;
  reason?: string;
  notes?: string;
  assignedTo?: string;
  newDueDate?: Date;
}

/**
 * Bulk Action Result
 */
export interface BulkActionResult {
  successCount: number;
  failureCount: number;
  processed: {
    id: string;
    success: boolean;
    error?: string;
  }[];
}

/**
 * Recommendation Action Request
 */
export interface RecommendationActionRequest {
  recommendationId: string;
  action: 'accept' | 'decline' | 'complete' | 'acknowledge';
  reason?: string;
  notes?: string;
  outcome?: string;
}

/**
 * Default empty filter configuration
 */
export const DEFAULT_FILTER_CONFIG: RecommendationFilterConfig = {
  urgency: [],
  category: [],
  patientRiskLevel: [],
  status: [],
  daysOverdueRange: { min: null, max: null },
  patientSearch: '',
  dateRange: { start: null, end: null },
};

/**
 * Default sort configuration
 */
export const DEFAULT_SORT_CONFIG: RecommendationSortConfig = {
  field: 'urgency',
  direction: 'desc',
};

/**
 * Urgency color mapping
 */
export const URGENCY_COLORS: Record<RecommendationUrgency, string> = {
  emergent: '#f44336', // red
  urgent: '#ff9800', // orange
  soon: '#ffc107', // amber
  routine: '#4caf50', // green
};

/**
 * Urgency icon mapping
 */
export const URGENCY_ICONS: Record<RecommendationUrgency, string> = {
  emergent: 'error',
  urgent: 'warning',
  soon: 'schedule',
  routine: 'check_circle',
};

/**
 * Category icon mapping
 */
export const CATEGORY_ICONS: Record<RecommendationCategory, string> = {
  preventive: 'vaccines',
  'chronic-disease': 'monitor_heart',
  medication: 'medication',
  'mental-health': 'psychology',
  sdoh: 'home',
};

/**
 * Category display names
 */
export const CATEGORY_DISPLAY_NAMES: Record<RecommendationCategory, string> = {
  preventive: 'Preventive Care',
  'chronic-disease': 'Chronic Disease',
  medication: 'Medication',
  'mental-health': 'Mental Health',
  sdoh: 'Social Determinants',
};

/**
 * Helper: Get urgency badge type for StatusBadgeComponent
 */
export function getUrgencyBadgeType(
  urgency: RecommendationUrgency
): 'error' | 'warning' | 'info' | 'success' {
  switch (urgency) {
    case 'emergent':
      return 'error';
    case 'urgent':
      return 'warning';
    case 'soon':
      return 'info';
    case 'routine':
      return 'success';
    default:
      return 'info';
  }
}

/**
 * Helper: Get risk level badge type
 */
export function getRiskLevelBadgeType(
  risk: RiskLevel
): 'error' | 'warning' | 'info' | 'success' {
  switch (risk) {
    case 'critical':
      return 'error';
    case 'high':
      return 'warning';
    case 'moderate':
      return 'info';
    case 'low':
      return 'success';
    default:
      return 'info';
  }
}

/**
 * Helper: Calculate days overdue
 */
export function calculateDaysOverdue(dueDate?: Date): number | undefined {
  if (!dueDate) return undefined;
  const now = new Date();
  const due = new Date(dueDate);
  const diffTime = now.getTime() - due.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 0 ? diffDays : undefined;
}

/**
 * Helper: Sort recommendations by urgency priority
 */
export function sortByUrgency(
  a: DashboardRecommendation,
  b: DashboardRecommendation
): number {
  const urgencyOrder: Record<RecommendationUrgency, number> = {
    emergent: 0,
    urgent: 1,
    soon: 2,
    routine: 3,
  };
  return urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
}

/**
 * Helper: Group recommendations by field
 */
export function groupRecommendations(
  recommendations: DashboardRecommendation[],
  field: GroupByField
): Map<string, DashboardRecommendation[]> {
  const grouped = new Map<string, DashboardRecommendation[]>();

  if (field === 'none') {
    grouped.set('all', recommendations);
    return grouped;
  }

  recommendations.forEach((rec) => {
    const key = String(rec[field] || 'unknown');
    if (!grouped.has(key)) {
      grouped.set(key, []);
    }
    grouped.get(key)!.push(rec);
  });

  return grouped;
}
