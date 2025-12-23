import { careRecommendationReducer, initialState } from './care-recommendation.reducer';
import * as Actions from '../actions/care-recommendation.actions';
import {
  DashboardRecommendation,
  RecommendationDashboardStats,
  RecommendationSortConfig,
} from '../../models/care-recommendation.model';

describe('careRecommendationReducer', () => {
  const recommendation: DashboardRecommendation = {
    id: 'rec-1',
    type: 'care-gap',
    patientId: 'patient-1',
    patientName: 'Alice Smith',
    mrn: 'MRN-1',
    patientRiskLevel: 'high',
    category: 'preventive',
    title: 'Vaccination',
    description: 'Flu shot',
    urgency: 'urgent',
    priority: 3,
    status: 'pending',
    createdDate: new Date('2025-01-01T00:00:00Z'),
    dueDate: new Date('2025-01-10T00:00:00Z'),
    daysOverdue: 2,
    actionItems: ['Schedule visit'],
  };

  const stats: RecommendationDashboardStats = {
    totalRecommendations: 1,
    byUrgency: { emergent: 0, urgent: 1, soon: 0, routine: 0 },
    byCategory: { preventive: 1, chronicDisease: 0, medication: 0, mentalHealth: 0, sdoh: 0 },
    byPatientRisk: { critical: 0, high: 1, moderate: 0, low: 0 },
    byStatus: { pending: 1, inProgress: 0, completed: 0, declined: 0 },
    overdueSummary: { total: 1, critical: 0, warning: 1, approaching: 0 },
  };

  it('handles load recommendations lifecycle', () => {
    const loadingState = careRecommendationReducer(initialState, Actions.loadRecommendations({}));
    expect(loadingState.loading).toBe(true);

    const successState = careRecommendationReducer(
      loadingState,
      Actions.loadRecommendationsSuccess({ recommendations: [recommendation] })
    );
    expect(successState.loading).toBe(false);
    expect(successState.totalCount).toBe(1);

    const failureState = careRecommendationReducer(
      loadingState,
      Actions.loadRecommendationsFailure({ error: 'Failed' })
    );
    expect(failureState.loading).toBe(false);
    expect(failureState.error).toBe('Failed');
  });

  it('handles load stats lifecycle', () => {
    const loadingStats = careRecommendationReducer(initialState, Actions.loadStats());
    expect(loadingStats.loadingStats).toBe(true);

    const successState = careRecommendationReducer(
      loadingStats,
      Actions.loadStatsSuccess({ stats })
    );
    expect(successState.loadingStats).toBe(false);
    expect(successState.stats).toEqual(stats);

    const failureState = careRecommendationReducer(
      loadingStats,
      Actions.loadStatsFailure({ error: 'Stats failed' })
    );
    expect(failureState.loadingStats).toBe(false);
    expect(failureState.error).toBe('Stats failed');
  });

  it('updates filter, sort, and view settings', () => {
    const filtered = careRecommendationReducer(
      initialState,
      Actions.setFilter({ filter: { urgency: ['urgent'] } })
    );
    expect(filtered.filter.urgency).toEqual(['urgent']);
    expect(filtered.pageIndex).toBe(0);

    const cleared = careRecommendationReducer(filtered, Actions.clearFilters());
    expect(cleared.filter).toEqual({});
    expect(cleared.searchTerm).toBe('');

    const sort: RecommendationSortConfig = { field: 'patientName', direction: 'desc' };
    const sorted = careRecommendationReducer(cleared, Actions.setSort({ sort }));
    expect(sorted.sort).toEqual(sort);

    const grouped = careRecommendationReducer(sorted, Actions.setGroupBy({ groupBy: 'status' }));
    expect(grouped.groupBy).toBe('status');

    const viewMode = careRecommendationReducer(grouped, Actions.setViewMode({ viewMode: 'grid' }));
    expect(viewMode.viewMode).toBe('grid');

    const searched = careRecommendationReducer(viewMode, Actions.setSearchTerm({ searchTerm: 'alice' }));
    expect(searched.searchTerm).toBe('alice');
    expect(searched.pageIndex).toBe(0);
  });

  it('manages selection state', () => {
    const withData = { ...initialState, recommendations: [recommendation] };
    const selected = careRecommendationReducer(withData, Actions.selectRecommendation({ id: 'rec-1' }));
    expect(selected.selectedIds).toEqual(['rec-1']);

    const selectedAgain = careRecommendationReducer(selected, Actions.selectRecommendation({ id: 'rec-1' }));
    expect(selectedAgain.selectedIds).toEqual(['rec-1']);

    const deselected = careRecommendationReducer(selected, Actions.deselectRecommendation({ id: 'rec-1' }));
    expect(deselected.selectedIds).toEqual([]);

    const toggledOn = careRecommendationReducer(withData, Actions.toggleSelection({ id: 'rec-1' }));
    const toggledOff = careRecommendationReducer(toggledOn, Actions.toggleSelection({ id: 'rec-1' }));
    expect(toggledOff.selectedIds).toEqual([]);

    const selectedAll = careRecommendationReducer(withData, Actions.selectAllRecommendations());
    expect(selectedAll.selectedIds).toEqual(['rec-1']);

    const cleared = careRecommendationReducer(selectedAll, Actions.clearSelection());
    expect(cleared.selectedIds).toEqual([]);
  });

  it('handles individual action flows', () => {
    const secondRecommendation = { ...recommendation, id: 'rec-2', status: 'pending' };
    const withData = { ...initialState, recommendations: [recommendation, secondRecommendation] };
    const accepting = careRecommendationReducer(withData, Actions.acceptRecommendation({ id: 'rec-1' }));
    expect(accepting.processingIds).toContain('rec-1');

    const updated = { ...recommendation, status: 'completed' };
    const accepted = careRecommendationReducer(
      accepting,
      Actions.acceptRecommendationSuccess({ recommendation: updated })
    );
    expect(accepted.processingIds).not.toContain('rec-1');
    expect(accepted.recommendations[0].status).toBe('completed');
    expect(accepted.recommendations[1].status).toBe('pending');

    const acceptFail = careRecommendationReducer(
      accepting,
      Actions.acceptRecommendationFailure({ id: 'rec-1', error: 'fail' })
    );
    expect(acceptFail.error).toBe('fail');

    const declining = careRecommendationReducer(withData, Actions.declineRecommendation({ id: 'rec-1', reason: 'no' }));
    expect(declining.processingIds).toContain('rec-1');

    const declined = careRecommendationReducer(
      declining,
      Actions.declineRecommendationSuccess({ recommendation: updated })
    );
    expect(declined.processingIds).not.toContain('rec-1');
    expect(declined.recommendations[1].status).toBe('pending');

    const declineFail = careRecommendationReducer(
      declining,
      Actions.declineRecommendationFailure({ id: 'rec-1', error: 'fail' })
    );
    expect(declineFail.error).toBe('fail');

    const completing = careRecommendationReducer(withData, Actions.completeRecommendation({ id: 'rec-1', outcome: 'ok' }));
    expect(completing.processingIds).toContain('rec-1');

    const completed = careRecommendationReducer(
      completing,
      Actions.completeRecommendationSuccess({ recommendation: updated })
    );
    expect(completed.processingIds).not.toContain('rec-1');
    expect(completed.recommendations[1].status).toBe('pending');

    const completeFail = careRecommendationReducer(
      completing,
      Actions.completeRecommendationFailure({ id: 'rec-1', error: 'fail' })
    );
    expect(completeFail.error).toBe('fail');
  });

  it('handles bulk actions, paging, and utilities', () => {
    const bulkState = careRecommendationReducer(
      initialState,
      Actions.performBulkAction({ request: { recommendationIds: ['rec-1'], action: 'complete' } })
    );
    expect(bulkState.processingIds).toEqual(['rec-1']);

    const bulkSuccess = careRecommendationReducer(bulkState, Actions.performBulkActionSuccess({ result: { successCount: 1, failureCount: 0, processed: [] } }));
    expect(bulkSuccess.processingIds).toEqual([]);
    expect(bulkSuccess.selectedIds).toEqual([]);

    const bulkFail = careRecommendationReducer(bulkState, Actions.performBulkActionFailure({ error: 'bulk fail' }));
    expect(bulkFail.error).toBe('bulk fail');

    const paged = careRecommendationReducer(initialState, Actions.setPage({ pageIndex: 2, pageSize: 50 }));
    expect(paged.pageIndex).toBe(2);
    expect(paged.pageSize).toBe(50);

    const refreshed = careRecommendationReducer(initialState, Actions.refreshRecommendations());
    expect(refreshed.loading).toBe(true);

    const cleared = careRecommendationReducer({ ...initialState, error: 'err' }, Actions.clearError());
    expect(cleared.error).toBeNull();

    const reset = careRecommendationReducer({ ...initialState, loading: true }, Actions.reset());
    expect(reset).toEqual(initialState);
  });
});
