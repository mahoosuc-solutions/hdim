import { createAction, props } from '@ngrx/store';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
  RecommendationDashboardStats,
  BulkActionRequest,
  BulkActionResult,
  GroupByField,
  DashboardViewMode,
} from '../../models/care-recommendation.model';

/**
 * Care Recommendation Actions - NgRx actions for care recommendations dashboard
 *
 * Action Types:
 * - Load recommendations
 * - Filter and sort
 * - Selection management
 * - Individual actions (accept, decline, complete)
 * - Bulk actions
 * - View mode and grouping
 */

// ============================================
// Load Recommendations
// ============================================

export const loadRecommendations = createAction(
  '[Care Recommendations] Load Recommendations',
  props<{ refresh?: boolean }>()
);

export const loadRecommendationsSuccess = createAction(
  '[Care Recommendations] Load Recommendations Success',
  props<{ recommendations: DashboardRecommendation[] }>()
);

export const loadRecommendationsFailure = createAction(
  '[Care Recommendations] Load Recommendations Failure',
  props<{ error: string }>()
);

// ============================================
// Load Statistics
// ============================================

export const loadStats = createAction('[Care Recommendations] Load Stats');

export const loadStatsSuccess = createAction(
  '[Care Recommendations] Load Stats Success',
  props<{ stats: RecommendationDashboardStats }>()
);

export const loadStatsFailure = createAction(
  '[Care Recommendations] Load Stats Failure',
  props<{ error: string }>()
);

// ============================================
// Filtering & Sorting
// ============================================

export const setFilter = createAction(
  '[Care Recommendations] Set Filter',
  props<{ filter: Partial<RecommendationFilterConfig> }>()
);

export const clearFilters = createAction(
  '[Care Recommendations] Clear Filters'
);

export const setSort = createAction(
  '[Care Recommendations] Set Sort',
  props<{ sort: RecommendationSortConfig }>()
);

export const setGroupBy = createAction(
  '[Care Recommendations] Set Group By',
  props<{ groupBy: GroupByField }>()
);

export const setViewMode = createAction(
  '[Care Recommendations] Set View Mode',
  props<{ viewMode: DashboardViewMode }>()
);

export const setSearchTerm = createAction(
  '[Care Recommendations] Set Search Term',
  props<{ searchTerm: string }>()
);

// ============================================
// Selection Management
// ============================================

export const selectRecommendation = createAction(
  '[Care Recommendations] Select Recommendation',
  props<{ id: string }>()
);

export const deselectRecommendation = createAction(
  '[Care Recommendations] Deselect Recommendation',
  props<{ id: string }>()
);

export const selectAllRecommendations = createAction(
  '[Care Recommendations] Select All Recommendations'
);

export const clearSelection = createAction(
  '[Care Recommendations] Clear Selection'
);

export const toggleSelection = createAction(
  '[Care Recommendations] Toggle Selection',
  props<{ id: string }>()
);

// ============================================
// Individual Actions
// ============================================

export const acceptRecommendation = createAction(
  '[Care Recommendations] Accept Recommendation',
  props<{ id: string; notes?: string }>()
);

export const acceptRecommendationSuccess = createAction(
  '[Care Recommendations] Accept Recommendation Success',
  props<{ recommendation: DashboardRecommendation }>()
);

export const acceptRecommendationFailure = createAction(
  '[Care Recommendations] Accept Recommendation Failure',
  props<{ id: string; error: string }>()
);

export const declineRecommendation = createAction(
  '[Care Recommendations] Decline Recommendation',
  props<{ id: string; reason: string; notes?: string }>()
);

export const declineRecommendationSuccess = createAction(
  '[Care Recommendations] Decline Recommendation Success',
  props<{ recommendation: DashboardRecommendation }>()
);

export const declineRecommendationFailure = createAction(
  '[Care Recommendations] Decline Recommendation Failure',
  props<{ id: string; error: string }>()
);

export const completeRecommendation = createAction(
  '[Care Recommendations] Complete Recommendation',
  props<{ id: string; outcome: string; notes?: string }>()
);

export const completeRecommendationSuccess = createAction(
  '[Care Recommendations] Complete Recommendation Success',
  props<{ recommendation: DashboardRecommendation }>()
);

export const completeRecommendationFailure = createAction(
  '[Care Recommendations] Complete Recommendation Failure',
  props<{ id: string; error: string }>()
);

// ============================================
// Bulk Actions
// ============================================

export const performBulkAction = createAction(
  '[Care Recommendations] Perform Bulk Action',
  props<{ request: BulkActionRequest }>()
);

export const performBulkActionSuccess = createAction(
  '[Care Recommendations] Perform Bulk Action Success',
  props<{ result: BulkActionResult }>()
);

export const performBulkActionFailure = createAction(
  '[Care Recommendations] Perform Bulk Action Failure',
  props<{ error: string }>()
);

// ============================================
// Pagination
// ============================================

export const setPage = createAction(
  '[Care Recommendations] Set Page',
  props<{ pageIndex: number; pageSize: number }>()
);

// ============================================
// Utility Actions
// ============================================

export const refreshRecommendations = createAction(
  '[Care Recommendations] Refresh Recommendations'
);

export const clearError = createAction('[Care Recommendations] Clear Error');

export const reset = createAction('[Care Recommendations] Reset');
