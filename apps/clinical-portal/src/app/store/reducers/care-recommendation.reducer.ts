import { createReducer, on } from '@ngrx/store';
import * as RecommendationActions from '../actions/care-recommendation.actions';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
  RecommendationDashboardStats,
  GroupByField,
  DashboardViewMode,
  DEFAULT_FILTER_CONFIG,
  DEFAULT_SORT_CONFIG,
} from '../../models/care-recommendation.model';

/**
 * Care Recommendation State
 */
export interface CareRecommendationState {
  // Data
  recommendations: DashboardRecommendation[];
  stats: RecommendationDashboardStats | null;

  // Filtering & Sorting
  filter: Partial<RecommendationFilterConfig>;
  sort: RecommendationSortConfig;
  searchTerm: string;

  // View Configuration
  groupBy: GroupByField;
  viewMode: DashboardViewMode;

  // Selection
  selectedIds: string[];

  // Pagination
  pageIndex: number;
  pageSize: number;
  totalCount: number;

  // Loading States
  loading: boolean;
  loadingStats: boolean;
  processingAction: boolean;
  processingIds: string[];

  // Error
  error: string | null;
}

/**
 * Initial State
 */
export const initialState: CareRecommendationState = {
  recommendations: [],
  stats: null,
  filter: {},
  sort: DEFAULT_SORT_CONFIG,
  searchTerm: '',
  groupBy: 'urgency',
  viewMode: 'list',
  selectedIds: [],
  pageIndex: 0,
  pageSize: 25,
  totalCount: 0,
  loading: false,
  loadingStats: false,
  processingAction: false,
  processingIds: [],
  error: null,
};

/**
 * Care Recommendation Reducer
 */
export const careRecommendationReducer = createReducer(
  initialState,

  // ============================================
  // Load Recommendations
  // ============================================

  on(RecommendationActions.loadRecommendations, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(
    RecommendationActions.loadRecommendationsSuccess,
    (state, { recommendations }) => ({
      ...state,
      recommendations,
      totalCount: recommendations.length,
      loading: false,
      error: null,
    })
  ),

  on(RecommendationActions.loadRecommendationsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // ============================================
  // Load Statistics
  // ============================================

  on(RecommendationActions.loadStats, (state) => ({
    ...state,
    loadingStats: true,
  })),

  on(RecommendationActions.loadStatsSuccess, (state, { stats }) => ({
    ...state,
    stats,
    loadingStats: false,
  })),

  on(RecommendationActions.loadStatsFailure, (state, { error }) => ({
    ...state,
    loadingStats: false,
    error,
  })),

  // ============================================
  // Filtering & Sorting
  // ============================================

  on(RecommendationActions.setFilter, (state, { filter }) => ({
    ...state,
    filter: { ...state.filter, ...filter },
    pageIndex: 0, // Reset to first page on filter change
  })),

  on(RecommendationActions.clearFilters, (state) => ({
    ...state,
    filter: {},
    searchTerm: '',
    pageIndex: 0,
  })),

  on(RecommendationActions.setSort, (state, { sort }) => ({
    ...state,
    sort,
  })),

  on(RecommendationActions.setGroupBy, (state, { groupBy }) => ({
    ...state,
    groupBy,
  })),

  on(RecommendationActions.setViewMode, (state, { viewMode }) => ({
    ...state,
    viewMode,
  })),

  on(RecommendationActions.setSearchTerm, (state, { searchTerm }) => ({
    ...state,
    searchTerm,
    pageIndex: 0,
  })),

  // ============================================
  // Selection Management
  // ============================================

  on(RecommendationActions.selectRecommendation, (state, { id }) => ({
    ...state,
    selectedIds: state.selectedIds.includes(id)
      ? state.selectedIds
      : [...state.selectedIds, id],
  })),

  on(RecommendationActions.deselectRecommendation, (state, { id }) => ({
    ...state,
    selectedIds: state.selectedIds.filter((selectedId) => selectedId !== id),
  })),

  on(RecommendationActions.selectAllRecommendations, (state) => ({
    ...state,
    selectedIds: state.recommendations.map((r) => r.id),
  })),

  on(RecommendationActions.clearSelection, (state) => ({
    ...state,
    selectedIds: [],
  })),

  on(RecommendationActions.toggleSelection, (state, { id }) => ({
    ...state,
    selectedIds: state.selectedIds.includes(id)
      ? state.selectedIds.filter((selectedId) => selectedId !== id)
      : [...state.selectedIds, id],
  })),

  // ============================================
  // Individual Actions - Accept
  // ============================================

  on(RecommendationActions.acceptRecommendation, (state, { id }) => ({
    ...state,
    processingAction: true,
    processingIds: [...state.processingIds, id],
  })),

  on(
    RecommendationActions.acceptRecommendationSuccess,
    (state, { recommendation }) => ({
      ...state,
      recommendations: state.recommendations.map((r) =>
        r.id === recommendation.id ? recommendation : r
      ),
      processingAction: false,
      processingIds: state.processingIds.filter(
        (id) => id !== recommendation.id
      ),
    })
  ),

  on(
    RecommendationActions.acceptRecommendationFailure,
    (state, { id, error }) => ({
      ...state,
      processingAction: false,
      processingIds: state.processingIds.filter((pId) => pId !== id),
      error,
    })
  ),

  // ============================================
  // Individual Actions - Decline
  // ============================================

  on(RecommendationActions.declineRecommendation, (state, { id }) => ({
    ...state,
    processingAction: true,
    processingIds: [...state.processingIds, id],
  })),

  on(
    RecommendationActions.declineRecommendationSuccess,
    (state, { recommendation }) => ({
      ...state,
      recommendations: state.recommendations.map((r) =>
        r.id === recommendation.id ? recommendation : r
      ),
      processingAction: false,
      processingIds: state.processingIds.filter(
        (id) => id !== recommendation.id
      ),
    })
  ),

  on(
    RecommendationActions.declineRecommendationFailure,
    (state, { id, error }) => ({
      ...state,
      processingAction: false,
      processingIds: state.processingIds.filter((pId) => pId !== id),
      error,
    })
  ),

  // ============================================
  // Individual Actions - Complete
  // ============================================

  on(RecommendationActions.completeRecommendation, (state, { id }) => ({
    ...state,
    processingAction: true,
    processingIds: [...state.processingIds, id],
  })),

  on(
    RecommendationActions.completeRecommendationSuccess,
    (state, { recommendation }) => ({
      ...state,
      recommendations: state.recommendations.map((r) =>
        r.id === recommendation.id ? recommendation : r
      ),
      processingAction: false,
      processingIds: state.processingIds.filter(
        (id) => id !== recommendation.id
      ),
    })
  ),

  on(
    RecommendationActions.completeRecommendationFailure,
    (state, { id, error }) => ({
      ...state,
      processingAction: false,
      processingIds: state.processingIds.filter((pId) => pId !== id),
      error,
    })
  ),

  // ============================================
  // Bulk Actions
  // ============================================

  on(RecommendationActions.performBulkAction, (state, { request }) => ({
    ...state,
    processingAction: true,
    processingIds: request.recommendationIds,
  })),

  on(RecommendationActions.performBulkActionSuccess, (state) => ({
    ...state,
    processingAction: false,
    processingIds: [],
    selectedIds: [], // Clear selection after bulk action
  })),

  on(RecommendationActions.performBulkActionFailure, (state, { error }) => ({
    ...state,
    processingAction: false,
    processingIds: [],
    error,
  })),

  // ============================================
  // Pagination
  // ============================================

  on(RecommendationActions.setPage, (state, { pageIndex, pageSize }) => ({
    ...state,
    pageIndex,
    pageSize,
  })),

  // ============================================
  // Utility Actions
  // ============================================

  on(RecommendationActions.refreshRecommendations, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(RecommendationActions.clearError, (state) => ({
    ...state,
    error: null,
  })),

  on(RecommendationActions.reset, () => initialState)
);
