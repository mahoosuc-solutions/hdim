import {
  selectAllRecommendations,
  selectCareRecommendationState,
  selectCountByCategory,
  selectCountByPatientRisk,
  selectCountByStatus,
  selectCountByUrgency,
  selectError,
  selectFilter,
  selectFilteredCount,
  selectFilteredRecommendations,
  selectGroupedRecommendations,
  selectGroupBy,
  selectHasActiveFilters,
  selectHighPriorityRecommendations,
  selectIsAllSelected,
  selectIsIndeterminate,
  selectIsRecommendationProcessing,
  selectIsRecommendationSelected,
  selectLoading,
  selectLoadingStats,
  selectPageIndex,
  selectPageSize,
  selectProcessingAction,
  selectProcessingIds,
  selectSearchTerm,
  selectOverdueRecommendations,
  selectPaginatedRecommendations,
  selectSelectedIds,
  selectSelectedCount,
  selectSelectedRecommendations,
  selectSort,
  selectStats,
  selectTotalCount,
  selectViewMode,
  selectSortedRecommendations,
} from './care-recommendation.selectors';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
} from '../../models/care-recommendation.model';
import { CareRecommendationState, initialState } from '../reducers/care-recommendation.reducer';

describe('care recommendation selectors', () => {
  const recommendations: DashboardRecommendation[] = [
    {
      id: 'rec-1',
      type: 'care-gap',
      patientId: 'patient-1',
      patientName: 'Alice Smith',
      mrn: 'MRN-1',
      patientRiskLevel: 'high',
      category: 'chronic-disease',
      title: 'A1c Overdue',
      description: 'Needs lab',
      urgency: 'urgent',
      priority: 2,
      status: 'pending',
      createdDate: new Date('2025-01-01T00:00:00Z'),
      dueDate: new Date('2025-01-10T00:00:00Z'),
      daysOverdue: 5,
      actionItems: ['Order lab'],
    },
    {
      id: 'rec-2',
      type: 'recommendation',
      patientId: 'patient-2',
      patientName: 'Bob Jones',
      mrn: 'MRN-2',
      patientRiskLevel: 'critical',
      category: 'preventive',
      title: 'Vaccination',
      description: 'Flu shot',
      urgency: 'emergent',
      priority: 1,
      status: 'completed',
      createdDate: new Date('2025-01-02T00:00:00Z'),
      dueDate: new Date('2025-01-05T00:00:00Z'),
      daysOverdue: 0,
      actionItems: ['Schedule visit'],
    },
    {
      id: 'rec-3',
      type: 'cds-alert',
      patientId: 'patient-3',
      patientName: 'Carol White',
      mrn: 'MRN-3',
      patientRiskLevel: 'low',
      category: 'sdoh',
      title: 'Food insecurity',
      description: 'Refer to resources',
      urgency: 'routine',
      priority: 5,
      status: 'in-progress',
      createdDate: new Date('2024-12-31T00:00:00Z'),
      actionItems: ['Send referral'],
    },
  ];

  const baseState: CareRecommendationState = {
    ...initialState,
    recommendations,
    totalCount: recommendations.length,
  };

  it('selects the feature state and recommendations list', () => {
    const rootState = { careRecommendation: baseState };
    expect(selectCareRecommendationState(rootState as any)).toBe(baseState);
    expect(selectAllRecommendations.projector(baseState)).toEqual(recommendations);
  });

  it('selects basic state fields', () => {
    const state: CareRecommendationState = {
      ...baseState,
      stats: { ...baseState.stats, totalRecommendations: 5 } as any,
      filter: { urgency: ['urgent'] },
      sort: { field: 'patientName', direction: 'desc' },
      searchTerm: 'bob',
      groupBy: 'status',
      viewMode: 'grid',
      selectedIds: ['rec-2'],
      pageIndex: 2,
      pageSize: 50,
      totalCount: 42,
      loading: true,
      loadingStats: true,
      processingAction: true,
      processingIds: ['rec-1'],
      error: 'err',
    };

    expect(selectStats.projector(state)).toEqual(state.stats);
    expect(selectFilter.projector(state)).toEqual(state.filter);
    expect(selectSort.projector(state)).toEqual(state.sort);
    expect(selectSearchTerm.projector(state)).toBe('bob');
    expect(selectGroupBy.projector(state)).toBe('status');
    expect(selectViewMode.projector(state)).toBe('grid');
    expect(selectSelectedIds.projector(state)).toEqual(['rec-2']);
    expect(selectPageIndex.projector(state)).toBe(2);
    expect(selectPageSize.projector(state)).toBe(50);
    expect(selectTotalCount.projector(state)).toBe(42);
    expect(selectLoading.projector(state)).toBe(true);
    expect(selectLoadingStats.projector(state)).toBe(true);
    expect(selectProcessingAction.projector(state)).toBe(true);
    expect(selectProcessingIds.projector(state)).toEqual(['rec-1']);
    expect(selectError.projector(state)).toBe('err');
  });

  it('filters recommendations by urgency, category, risk, status, search, and overdue', () => {
    const filter: Partial<RecommendationFilterConfig> = {
      urgency: ['urgent'],
      category: ['chronic-disease'],
      patientRiskLevel: ['high'],
      status: ['pending'],
      daysOverdueRange: { min: 1, max: 10 },
    };

    const filtered = selectFilteredRecommendations.projector(
      recommendations,
      filter,
      'alice'
    );

    expect(filtered).toEqual([recommendations[0]]);
  });

  it('filters by search term across patient name, MRN, and title', () => {
    const filter: Partial<RecommendationFilterConfig> = {};
    expect(
      selectFilteredRecommendations.projector(recommendations, filter, 'mrn-2').map((r) => r.id)
    ).toEqual(['rec-2']);
    expect(
      selectFilteredRecommendations.projector(recommendations, filter, 'vaccination').map((r) => r.id)
    ).toEqual(['rec-2']);
  });

  it('handles days overdue ranges independently', () => {
    const withOverdue = [
      ...recommendations,
      {
        ...recommendations[0],
        id: 'rec-4',
        daysOverdue: undefined,
      },
    ];

    const minOnly = selectFilteredRecommendations.projector(
      withOverdue,
      { daysOverdueRange: { min: 1 } },
      ''
    );
    expect(minOnly.map((r) => r.id)).toEqual(['rec-1']);

    const maxOnly = selectFilteredRecommendations.projector(
      withOverdue,
      { daysOverdueRange: { max: 0 } },
      ''
    );
    expect(maxOnly.map((r) => r.id)).toEqual(['rec-2']);
  });

  it('sorts recommendations across supported fields', () => {
    const urgencySort: RecommendationSortConfig = {
      field: 'urgency',
      direction: 'asc',
    };
    const urgencySorted = selectSortedRecommendations.projector(
      recommendations,
      urgencySort
    );
    expect(urgencySorted.map((r) => r.id)).toEqual(['rec-2', 'rec-1', 'rec-3']);

    const categorySort: RecommendationSortConfig = {
      field: 'category',
      direction: 'asc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, categorySort)
        .map((r) => r.id)
    ).toEqual(['rec-1', 'rec-2', 'rec-3']);

    const nameSort: RecommendationSortConfig = {
      field: 'patientName',
      direction: 'desc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, nameSort)
        .map((r) => r.id)
    ).toEqual(['rec-3', 'rec-2', 'rec-1']);

    const riskSort: RecommendationSortConfig = {
      field: 'riskLevel',
      direction: 'asc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, riskSort)
        .map((r) => r.id)
    ).toEqual(['rec-2', 'rec-1', 'rec-3']);

    const dueDateSort: RecommendationSortConfig = {
      field: 'dueDate',
      direction: 'asc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, dueDateSort)
        .map((r) => r.id)
    ).toEqual(['rec-2', 'rec-1', 'rec-3']);

    const createdSort: RecommendationSortConfig = {
      field: 'createdDate',
      direction: 'asc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, createdSort)
        .map((r) => r.id)
    ).toEqual(['rec-3', 'rec-1', 'rec-2']);

    const defaultSort: RecommendationSortConfig = {
      field: 'urgency',
      direction: 'desc',
    };
    expect(
      selectSortedRecommendations
        .projector(recommendations, defaultSort)
        .map((r) => r.id)
    ).toEqual(['rec-3', 'rec-1', 'rec-2']);

    const unknownSort = { field: 'unknown', direction: 'asc' } as RecommendationSortConfig;
    const unknownSorted = selectSortedRecommendations
      .projector(recommendations, unknownSort)
      .map((r) => r.id)
      .sort();
    expect(unknownSorted).toEqual(['rec-1', 'rec-2', 'rec-3']);
  });

  it('paginates and groups recommendations', () => {
    const paged = selectPaginatedRecommendations.projector(recommendations, 1, 1);
    expect(paged.map((r) => r.id)).toEqual(['rec-2']);

    const grouped = selectGroupedRecommendations.projector(recommendations, 'urgency');
    expect(grouped.get('urgent')?.[0].id).toBe('rec-1');

    const groupedNone = selectGroupedRecommendations.projector(recommendations, 'none');
    expect(groupedNone.get('all')?.length).toBe(3);
  });

  it('handles selection state derived selectors', () => {
    const selectedIds = ['rec-1', 'rec-3'];
    const selected = selectSelectedRecommendations.projector(recommendations, selectedIds);
    expect(selected.map((r) => r.id)).toEqual(['rec-1', 'rec-3']);

    expect(selectIsAllSelected.projector(recommendations, selectedIds)).toBe(false);
    expect(selectIsIndeterminate.projector(recommendations, selectedIds)).toBe(true);
    expect(selectSelectedCount.projector(selectedIds)).toBe(2);

    expect(selectIsRecommendationSelected('rec-1').projector(selectedIds)).toBe(true);
    expect(selectIsRecommendationSelected('rec-2').projector(selectedIds)).toBe(false);

    expect(selectIsRecommendationProcessing('rec-2').projector(['rec-2'])).toBe(true);
  });

  it('handles empty and full selection states', () => {
    expect(selectIsAllSelected.projector([], [])).toBe(false);
    expect(selectIsIndeterminate.projector(recommendations, [])).toBe(false);
    expect(selectIsAllSelected.projector(recommendations, ['rec-1', 'rec-2', 'rec-3'])).toBe(true);
    expect(selectIsIndeterminate.projector(recommendations, ['rec-1', 'rec-2', 'rec-3'])).toBe(false);
    expect(selectIsRecommendationProcessing('rec-2').projector([])).toBe(false);
  });

  it('computes summary counts and priority lists', () => {
    expect(selectCountByUrgency.projector(recommendations)).toEqual({
      emergent: 1,
      urgent: 1,
      soon: 0,
      routine: 1,
    });

    expect(selectCountByCategory.projector(recommendations)).toEqual({
      'chronic-disease': 1,
      preventive: 1,
      sdoh: 1,
    });

    expect(selectCountByStatus.projector(recommendations)).toEqual({
      pending: 1,
      completed: 1,
      'in-progress': 1,
    });

    expect(selectCountByPatientRisk.projector(recommendations)).toEqual({
      high: 1,
      critical: 1,
      low: 1,
    });

    expect(selectOverdueRecommendations.projector(recommendations).map((r) => r.id)).toEqual(['rec-1']);
    expect(selectHighPriorityRecommendations.projector(recommendations).map((r) => r.id)).toEqual(['rec-1', 'rec-2']);
    expect(selectFilteredCount.projector(recommendations)).toBe(3);
  });

  it('detects active filters based on search and filter config', () => {
    expect(selectHasActiveFilters.projector({}, '')).toBe(false);
    expect(selectHasActiveFilters.projector({}, 'alice')).toBe(true);
    expect(selectHasActiveFilters.projector({ urgency: ['urgent'] }, '')).toBe(true);
    expect(selectHasActiveFilters.projector({ patientRiskLevel: ['high'] }, '')).toBe(true);
    expect(selectHasActiveFilters.projector({ status: ['pending'] }, '')).toBe(true);
  });
});
