import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CareRecommendationState } from '../reducers/care-recommendation.reducer';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
  RecommendationUrgency,
  groupRecommendations,
  sortByUrgency,
} from '../../models/care-recommendation.model';
import { RiskLevel } from '../../models/patient-health.model';

/**
 * Care Recommendation Selectors
 */

// Feature Selector
export const selectCareRecommendationState =
  createFeatureSelector<CareRecommendationState>('careRecommendation');

// ============================================
// Basic Selectors
// ============================================

export const selectAllRecommendations = createSelector(
  selectCareRecommendationState,
  (state) => state.recommendations
);

export const selectStats = createSelector(
  selectCareRecommendationState,
  (state) => state.stats
);

export const selectFilter = createSelector(
  selectCareRecommendationState,
  (state) => state.filter
);

export const selectSort = createSelector(
  selectCareRecommendationState,
  (state) => state.sort
);

export const selectSearchTerm = createSelector(
  selectCareRecommendationState,
  (state) => state.searchTerm
);

export const selectGroupBy = createSelector(
  selectCareRecommendationState,
  (state) => state.groupBy
);

export const selectViewMode = createSelector(
  selectCareRecommendationState,
  (state) => state.viewMode
);

export const selectSelectedIds = createSelector(
  selectCareRecommendationState,
  (state) => state.selectedIds
);

export const selectPageIndex = createSelector(
  selectCareRecommendationState,
  (state) => state.pageIndex
);

export const selectPageSize = createSelector(
  selectCareRecommendationState,
  (state) => state.pageSize
);

export const selectTotalCount = createSelector(
  selectCareRecommendationState,
  (state) => state.totalCount
);

// ============================================
// Loading Selectors
// ============================================

export const selectLoading = createSelector(
  selectCareRecommendationState,
  (state) => state.loading
);

export const selectLoadingStats = createSelector(
  selectCareRecommendationState,
  (state) => state.loadingStats
);

export const selectProcessingAction = createSelector(
  selectCareRecommendationState,
  (state) => state.processingAction
);

export const selectProcessingIds = createSelector(
  selectCareRecommendationState,
  (state) => state.processingIds
);

export const selectError = createSelector(
  selectCareRecommendationState,
  (state) => state.error
);

// ============================================
// Derived Selectors - Filtering
// ============================================

/**
 * Select filtered recommendations based on current filter and search term
 */
export const selectFilteredRecommendations = createSelector(
  selectAllRecommendations,
  selectFilter,
  selectSearchTerm,
  (recommendations, filter, searchTerm) => {
    let filtered = [...recommendations];

    // Apply urgency filter
    if (filter.urgency?.length) {
      filtered = filtered.filter((r) => filter.urgency!.includes(r.urgency));
    }

    // Apply category filter
    if (filter.category?.length) {
      filtered = filtered.filter((r) => filter.category!.includes(r.category));
    }

    // Apply patient risk level filter
    if (filter.patientRiskLevel?.length) {
      filtered = filtered.filter((r) =>
        filter.patientRiskLevel!.includes(r.patientRiskLevel)
      );
    }

    // Apply status filter
    if (filter.status?.length) {
      filtered = filtered.filter((r) => filter.status!.includes(r.status));
    }

    // Apply search term
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          r.patientName.toLowerCase().includes(search) ||
          r.mrn.toLowerCase().includes(search) ||
          r.title.toLowerCase().includes(search)
      );
    }

    // Apply days overdue filter
    if (filter.daysOverdueRange?.min != null) {
      filtered = filtered.filter(
        (r) =>
          r.daysOverdue != null &&
          r.daysOverdue >= filter.daysOverdueRange!.min!
      );
    }

    if (filter.daysOverdueRange?.max != null) {
      filtered = filtered.filter(
        (r) =>
          r.daysOverdue != null &&
          r.daysOverdue <= filter.daysOverdueRange!.max!
      );
    }

    return filtered;
  }
);

/**
 * Select sorted and filtered recommendations
 */
export const selectSortedRecommendations = createSelector(
  selectFilteredRecommendations,
  selectSort,
  (recommendations, sort) => {
    const sorted = [...recommendations];
    const direction = sort.direction === 'asc' ? 1 : -1;

    sorted.sort((a, b) => {
      let comparison = 0;

      switch (sort.field) {
        case 'urgency':
          const urgencyOrder: Record<RecommendationUrgency, number> = {
            emergent: 0,
            urgent: 1,
            soon: 2,
            routine: 3,
          };
          comparison = urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
          break;

        case 'category':
          comparison = a.category.localeCompare(b.category);
          break;

        case 'patientName':
          comparison = a.patientName.localeCompare(b.patientName);
          break;

        case 'riskLevel':
          const riskOrder: Record<RiskLevel, number> = {
            critical: 0,
            high: 1,
            moderate: 2,
            low: 3,
          };
          comparison =
            riskOrder[a.patientRiskLevel] - riskOrder[b.patientRiskLevel];
          break;

        case 'dueDate':
          const aDate = a.dueDate ? new Date(a.dueDate).getTime() : Infinity;
          const bDate = b.dueDate ? new Date(b.dueDate).getTime() : Infinity;
          comparison = aDate - bDate;
          break;

        case 'createdDate':
          comparison =
            new Date(a.createdDate).getTime() -
            new Date(b.createdDate).getTime();
          break;

        default:
          comparison = 0;
      }

      return comparison * direction;
    });

    return sorted;
  }
);

/**
 * Select paginated recommendations
 */
export const selectPaginatedRecommendations = createSelector(
  selectSortedRecommendations,
  selectPageIndex,
  selectPageSize,
  (recommendations, pageIndex, pageSize) => {
    const start = pageIndex * pageSize;
    const end = start + pageSize;
    return recommendations.slice(start, end);
  }
);

/**
 * Select grouped recommendations
 */
export const selectGroupedRecommendations = createSelector(
  selectSortedRecommendations,
  selectGroupBy,
  (recommendations, groupBy) => {
    return groupRecommendations(recommendations, groupBy);
  }
);

// ============================================
// Derived Selectors - Selection
// ============================================

/**
 * Select selected recommendations
 */
export const selectSelectedRecommendations = createSelector(
  selectAllRecommendations,
  selectSelectedIds,
  (recommendations, selectedIds) =>
    recommendations.filter((r) => selectedIds.includes(r.id))
);

/**
 * Check if all recommendations are selected
 */
export const selectIsAllSelected = createSelector(
  selectFilteredRecommendations,
  selectSelectedIds,
  (recommendations, selectedIds) =>
    recommendations.length > 0 &&
    recommendations.every((r) => selectedIds.includes(r.id))
);

/**
 * Check if some but not all recommendations are selected
 */
export const selectIsIndeterminate = createSelector(
  selectFilteredRecommendations,
  selectSelectedIds,
  (recommendations, selectedIds) => {
    const selectedCount = recommendations.filter((r) =>
      selectedIds.includes(r.id)
    ).length;
    return selectedCount > 0 && selectedCount < recommendations.length;
  }
);

/**
 * Get count of selected recommendations
 */
export const selectSelectedCount = createSelector(
  selectSelectedIds,
  (selectedIds) => selectedIds.length
);

/**
 * Check if a specific recommendation is selected
 */
export const selectIsRecommendationSelected = (id: string) =>
  createSelector(selectSelectedIds, (selectedIds) => selectedIds.includes(id));

/**
 * Check if a specific recommendation is being processed
 */
export const selectIsRecommendationProcessing = (id: string) =>
  createSelector(selectProcessingIds, (processingIds) =>
    processingIds.includes(id)
  );

// ============================================
// Derived Selectors - Statistics
// ============================================

/**
 * Select count by urgency
 */
export const selectCountByUrgency = createSelector(
  selectFilteredRecommendations,
  (recommendations) => {
    const counts = { emergent: 0, urgent: 0, soon: 0, routine: 0 };
    recommendations.forEach((r) => {
      if (counts.hasOwnProperty(r.urgency)) {
        counts[r.urgency]++;
      }
    });
    return counts;
  }
);

/**
 * Select count by category
 */
export const selectCountByCategory = createSelector(
  selectFilteredRecommendations,
  (recommendations) => {
    const counts: Record<string, number> = {};
    recommendations.forEach((r) => {
      counts[r.category] = (counts[r.category] || 0) + 1;
    });
    return counts;
  }
);

/**
 * Select count by status
 */
export const selectCountByStatus = createSelector(
  selectFilteredRecommendations,
  (recommendations) => {
    const counts: Record<string, number> = {};
    recommendations.forEach((r) => {
      counts[r.status] = (counts[r.status] || 0) + 1;
    });
    return counts;
  }
);

/**
 * Select count by patient risk level
 */
export const selectCountByPatientRisk = createSelector(
  selectFilteredRecommendations,
  (recommendations) => {
    const counts: Record<string, number> = {};
    recommendations.forEach((r) => {
      counts[r.patientRiskLevel] = (counts[r.patientRiskLevel] || 0) + 1;
    });
    return counts;
  }
);

/**
 * Select overdue recommendations
 */
export const selectOverdueRecommendations = createSelector(
  selectFilteredRecommendations,
  (recommendations) =>
    recommendations.filter((r) => r.daysOverdue && r.daysOverdue > 0)
);

/**
 * Select high priority (emergent + urgent) recommendations
 */
export const selectHighPriorityRecommendations = createSelector(
  selectFilteredRecommendations,
  (recommendations) =>
    recommendations.filter(
      (r) => r.urgency === 'emergent' || r.urgency === 'urgent'
    )
);

/**
 * Get filtered count
 */
export const selectFilteredCount = createSelector(
  selectFilteredRecommendations,
  (recommendations) => recommendations.length
);

/**
 * Check if any filters are active
 */
export const selectHasActiveFilters = createSelector(
  selectFilter,
  selectSearchTerm,
  (filter, searchTerm) => {
    return (
      searchTerm.length > 0 ||
      (filter.urgency?.length ?? 0) > 0 ||
      (filter.category?.length ?? 0) > 0 ||
      (filter.patientRiskLevel?.length ?? 0) > 0 ||
      (filter.status?.length ?? 0) > 0
    );
  }
);
