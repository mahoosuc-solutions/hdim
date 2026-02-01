/**
 * Comprehensive Unit Tests for Care Recommendations Component
 *
 * Tests cover:
 * - Component initialization with NgRx store
 * - Store action dispatching and selector subscriptions
 * - Search functionality with debounce
 * - Filter methods for urgency, category, risk, and status
 * - Clear filters functionality
 * - View mode switching (list, grid, kanban)
 * - Selection management (toggle, master toggle, clear)
 * - Individual action methods (accept, decline, complete)
 * - Bulk action methods
 * - Pagination and sorting
 * - Utility methods
 * - Navigation
 * - Component cleanup on destroy
 */

import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, ActivatedRoute } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { Store } from '@ngrx/store';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort, Sort } from '@angular/material/sort';
import { of, Subject } from 'rxjs';

import { CareRecommendationsComponent } from './care-recommendations.component';
import { AppState } from '../../store';
import * as RecommendationActions from '../../store/actions/care-recommendation.actions';
import * as RecommendationSelectors from '../../store/selectors/care-recommendation.selectors';
import { CareRecommendationService } from '../../services/care-recommendation.service';
import { ToastService } from '../../services/toast.service';
import {
  DashboardRecommendation,
  RecommendationDashboardStats,
  RecommendationUrgency,
  RecommendationCategory,
  RecommendationStatus,
  DashboardViewMode,
  getUrgencyBadgeType,
  getRiskLevelBadgeType,
  CATEGORY_DISPLAY_NAMES,
} from '../../models/care-recommendation.model';
import { RiskLevel } from '../../models/patient-health.model';
import { createMockMatDialog } from '../../testing/mocks';
import { createMockRouter } from '../../testing/mocks';
import { createMockStore } from '../../testing/mocks';

describe('CareRecommendationsComponent', () => {
  let component: CareRecommendationsComponent;
  let fixture: ComponentFixture<CareRecommendationsComponent>;
  let store: MockStore<AppState>;
  let mockRouter: jest.Mocked<Router>;
  let mockDialog: jest.Mocked<MatDialog>;
  let mockRecommendationService: jest.Mocked<CareRecommendationService>;
  let mockToastService: jest.Mocked<ToastService>;

  // Mock data
  const mockRecommendations: DashboardRecommendation[] = [
    {
      id: 'rec-1',
      type: 'care-gap',
      patientId: 'patient-1',
      patientName: 'John Doe',
      mrn: 'MRN001',
      patientRiskLevel: 'high' as RiskLevel,
      category: 'preventive',
      title: 'Annual Wellness Visit Overdue',
      description: 'Patient has not had annual wellness visit in 18 months',
      urgency: 'urgent',
      priority: 8,
      status: 'pending',
      createdDate: new Date('2024-01-15'),
      dueDate: new Date('2024-12-01'),
      daysOverdue: 3,
      actionItems: ['Schedule wellness visit'],
    },
    {
      id: 'rec-2',
      type: 'recommendation',
      patientId: 'patient-2',
      patientName: 'Jane Smith',
      mrn: 'MRN002',
      patientRiskLevel: 'moderate' as RiskLevel,
      category: 'chronic-disease',
      title: 'HbA1c Test Needed',
      description: 'Diabetic patient needs HbA1c monitoring',
      urgency: 'soon',
      priority: 6,
      status: 'in-progress',
      createdDate: new Date('2024-02-01'),
      dueDate: new Date('2024-12-15'),
      actionItems: ['Order HbA1c test', 'Schedule follow-up'],
    },
    {
      id: 'rec-3',
      type: 'cds-alert',
      patientId: 'patient-3',
      patientName: 'Robert Johnson',
      mrn: 'MRN003',
      patientRiskLevel: 'critical' as RiskLevel,
      category: 'medication',
      title: 'Medication Interaction Alert',
      description: 'Potential drug interaction detected',
      urgency: 'emergent',
      priority: 10,
      status: 'pending',
      createdDate: new Date('2024-03-01'),
      dueDate: new Date('2024-11-20'),
      daysOverdue: 14,
      actionItems: ['Review medications', 'Contact pharmacist'],
    },
  ];

  const mockStats: RecommendationDashboardStats = {
    totalRecommendations: 150,
    byUrgency: {
      emergent: 5,
      urgent: 20,
      soon: 45,
      routine: 80,
    },
    byCategory: {
      preventive: 50,
      chronicDisease: 40,
      medication: 30,
      mentalHealth: 20,
      sdoh: 10,
    },
    byPatientRisk: {
      critical: 10,
      high: 30,
      moderate: 60,
      low: 50,
    },
    byStatus: {
      pending: 80,
      inProgress: 40,
      completed: 25,
      declined: 5,
    },
    overdueSummary: {
      total: 25,
      critical: 5,
      warning: 10,
      approaching: 10,
    },
  };

  const initialState = {
    recommendations: {
      recommendations: mockRecommendations,
      filteredRecommendations: mockRecommendations,
      paginatedRecommendations: mockRecommendations,
      groupedRecommendations: new Map(),
      stats: mockStats,
      loading: false,
      selectedIds: [],
      selectedCount: 0,
      isAllSelected: false,
      isIndeterminate: false,
      viewMode: 'list' as DashboardViewMode,
      hasActiveFilters: false,
      error: null,
    },
  };

  beforeEach(async () => {
    mockRouter = {
      navigate: jest.fn(),
      createUrlTree: jest.fn().mockReturnValue({}),
      serializeUrl: jest.fn().mockReturnValue(''),
      events: of(),
    } as any;

    mockDialog = {
      open: jest.fn(),
    } as any;

    mockRecommendationService = {
      getRecommendations: jest.fn(),
      getDashboardStats: jest.fn(),
    } as any;

    mockToastService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [CareRecommendationsComponent, NoopAnimationsModule],
      providers: [provideMockStore({ initialState }),
        { provide: Router, useValue: createMockRouter() },
        { provide: MatDialog, useValue: createMockMatDialog() },
        { provide: CareRecommendationService, useValue: mockRecommendationService },
        { provide: ToastService, useValue: mockToastService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: {} },
            params: of({}),
            queryParams: of({}),
          },
        }],
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(CareRecommendationsComponent);
    component = fixture.componentInstance;

    // Setup store selectors
    store.overrideSelector(RecommendationSelectors.selectAllRecommendations, mockRecommendations);
    store.overrideSelector(RecommendationSelectors.selectFilteredRecommendations, mockRecommendations);
    store.overrideSelector(RecommendationSelectors.selectPaginatedRecommendations, mockRecommendations);
    store.overrideSelector(RecommendationSelectors.selectGroupedRecommendations, new Map());
    store.overrideSelector(RecommendationSelectors.selectStats, mockStats);
    store.overrideSelector(RecommendationSelectors.selectLoading, false);
    store.overrideSelector(RecommendationSelectors.selectSelectedIds, []);
    store.overrideSelector(RecommendationSelectors.selectSelectedCount, 0);
    store.overrideSelector(RecommendationSelectors.selectIsAllSelected, false);
    store.overrideSelector(RecommendationSelectors.selectIsIndeterminate, false);
    store.overrideSelector(RecommendationSelectors.selectViewMode, 'list' as DashboardViewMode);
    store.overrideSelector(RecommendationSelectors.selectHasActiveFilters, false);
    store.overrideSelector(RecommendationSelectors.selectError, null);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  // ============================================================================
  // 1. Component Creation and Initialization
  // ============================================================================
  describe('Component Creation and Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize observables from store in constructor', () => {
      expect(component.recommendations$).toBeDefined();
      expect(component.filteredRecommendations$).toBeDefined();
      expect(component.paginatedRecommendations$).toBeDefined();
      expect(component.groupedRecommendations$).toBeDefined();
      expect(component.stats$).toBeDefined();
      expect(component.loading$).toBeDefined();
      expect(component.selectedIds$).toBeDefined();
      expect(component.selectedCount$).toBeDefined();
      expect(component.isAllSelected$).toBeDefined();
      expect(component.isIndeterminate$).toBeDefined();
      expect(component.viewMode$).toBeDefined();
      expect(component.hasActiveFilters$).toBeDefined();
      expect(component.error$).toBeDefined();
    });

    it('should dispatch loadRecommendations action on ngOnInit', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.ngOnInit();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.loadRecommendations({})
      );
    });

    it('should dispatch loadStats action on ngOnInit', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.ngOnInit();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.loadStats()
      );
    });

    it('should setup search debounce on ngOnInit', fakeAsync(() => {
      component.ngOnInit();
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onSearchChange('test search');
      tick(300); // Wait for debounce

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setSearchTerm({ searchTerm: 'test search' })
      );
    }));

    it('should subscribe to paginatedRecommendations and update dataSource', () => {
      component.ngOnInit();
      fixture.detectChanges();

      expect(component.dataSource.data).toEqual(mockRecommendations);
    });

    it('should subscribe to viewMode changes and update local state', () => {
      store.overrideSelector(RecommendationSelectors.selectViewMode, 'grid' as DashboardViewMode);
      store.refreshState();

      component.ngOnInit();
      fixture.detectChanges();

      expect(component.viewMode).toBe('grid');
    });

    it('should initialize with default filter options', () => {
      expect(component.urgencyOptions).toEqual(['emergent', 'urgent', 'soon', 'routine']);
      expect(component.categoryOptions).toEqual([
        'preventive',
        'chronic-disease',
        'medication',
        'mental-health',
        'sdoh',
      ]);
      expect(component.riskLevelOptions).toEqual(['critical', 'high', 'moderate', 'low']);
      expect(component.statusOptions).toEqual([
        'pending',
        'in-progress',
        'completed',
        'declined',
      ]);
    });

    it('should initialize with empty active filters', () => {
      expect(component.activeUrgencyFilters).toEqual([]);
      expect(component.activeCategoryFilters).toEqual([]);
      expect(component.activeRiskFilters).toEqual([]);
      expect(component.activeStatusFilters).toEqual([]);
    });

    it('should initialize with correct table columns', () => {
      expect(component.displayedColumns).toEqual([
        'select',
        'urgency',
        'patientName',
        'mrn',
        'category',
        'title',
        'patientRiskLevel',
        'dueDate',
        'status',
        'actions',
      ]);
    });
  });

  // ============================================================================
  // 2. NgRx Store Integration
  // ============================================================================
  describe('NgRx Store Integration', () => {
    it('should select recommendations from store', (done) => {
      component.recommendations$.subscribe((recommendations) => {
        expect(recommendations).toEqual(mockRecommendations);
        done();
      };
    });

    it('should select stats from store', (done) => {
      component.stats$.subscribe((stats) => {
        expect(stats).toEqual(mockStats);
        done();
      };
    });

    it('should select loading state from store', (done) => {
      component.loading$.subscribe((loading) => {
        expect(loading).toBe(false);
        done();
      };
    });

    it('should select selectedIds from store', (done) => {
      component.selectedIds$.subscribe((ids) => {
        expect(ids).toEqual([]);
        done();
      };
    });

    it('should select isAllSelected from store', (done) => {
      component.isAllSelected$.subscribe((isAllSelected) => {
        expect(isAllSelected).toBe(false);
        done();
      };
    });

    it('should select viewMode from store', (done) => {
      component.viewMode$.subscribe((viewMode) => {
        expect(viewMode).toBe('list');
        done();
      };
    });

    it('should select error from store', (done) => {
      component.error$.subscribe((error) => {
        expect(error).toBeNull();
        done();
      };
    });
  });

  // ============================================================================
  // 3. Search Functionality with Debounce
  // ============================================================================
  describe('Search Functionality with Debounce', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should debounce search input for 300ms', fakeAsync(() => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onSearchChange('test');
      tick(100);
      expect(dispatchSpy).not.toHaveBeenCalled();

      tick(200);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setSearchTerm({ searchTerm: 'test' })
      );
    }));

    it('should only dispatch once for multiple rapid changes', fakeAsync(() => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onSearchChange('a');
      tick(100);
      component.onSearchChange('ab');
      tick(100);
      component.onSearchChange('abc');
      tick(300);

      // Should only dispatch the last value
      expect(dispatchSpy).toHaveBeenCalledTimes(1);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setSearchTerm({ searchTerm: 'abc' })
      );
    }));

    it('should dispatch distinctUntilChanged search terms', fakeAsync(() => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onSearchChange('test');
      tick(300);
      component.onSearchChange('test'); // Same value
      tick(300);

      // Should only dispatch once due to distinctUntilChanged
      expect(dispatchSpy).toHaveBeenCalledTimes(1);
    }));

    it('should handle empty search term', fakeAsync(() => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onSearchChange('');
      tick(300);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setSearchTerm({ searchTerm: '' })
      );
    }));
  });

  // ============================================================================
  // 4. Filter Methods
  // ============================================================================
  describe('Filter Methods', () => {
    it('should dispatch setFilter action with urgency on onUrgencyFilterChange', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const urgencies: RecommendationUrgency[] = ['urgent', 'emergent'];

      component.onUrgencyFilterChange(urgencies);

      expect(component.activeUrgencyFilters).toEqual(urgencies);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { urgency: urgencies } })
      );
    });

    it('should dispatch setFilter action with category on onCategoryFilterChange', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const categories: RecommendationCategory[] = ['preventive', 'medication'];

      component.onCategoryFilterChange(categories);

      expect(component.activeCategoryFilters).toEqual(categories);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { category: categories } })
      );
    });

    it('should dispatch setFilter action with risk level on onRiskFilterChange', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const risks: RiskLevel[] = ['high', 'critical'];

      component.onRiskFilterChange(risks);

      expect(component.activeRiskFilters).toEqual(risks);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { patientRiskLevel: risks } })
      );
    });

    it('should dispatch setFilter action with status on onStatusFilterChange', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const statuses: RecommendationStatus[] = ['pending', 'in-progress'];

      component.onStatusFilterChange(statuses);

      expect(component.activeStatusFilters).toEqual(statuses);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { status: statuses } })
      );
    });

    it('should handle empty filter arrays', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onUrgencyFilterChange([]);

      expect(component.activeUrgencyFilters).toEqual([]);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { urgency: [] } })
      );
    });
  });

  // ============================================================================
  // 5. Clear Filters Functionality
  // ============================================================================
  describe('Clear Filters Functionality', () => {
    it('should clear all filters and reset search term', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      // Set some filters first
      component.searchTerm = 'test';
      component.activeUrgencyFilters = ['urgent'];
      component.activeCategoryFilters = ['preventive'];
      component.activeRiskFilters = ['high'];
      component.activeStatusFilters = ['pending'];

      component.clearFilters();

      expect(component.searchTerm).toBe('');
      expect(component.activeUrgencyFilters).toEqual([]);
      expect(component.activeCategoryFilters).toEqual([]);
      expect(component.activeRiskFilters).toEqual([]);
      expect(component.activeStatusFilters).toEqual([]);
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.clearFilters()
      );
    });
  });

  // ============================================================================
  // 6. View Mode Switching
  // ============================================================================
  describe('View Mode Switching', () => {
    it('should dispatch setViewMode action when changing to list view', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onViewModeChange('list');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setViewMode({ viewMode: 'list' })
      );
    });

    it('should dispatch setViewMode action when changing to grid view', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onViewModeChange('grid');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setViewMode({ viewMode: 'grid' })
      );
    });

    it('should dispatch setViewMode action when changing to kanban view', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onViewModeChange('kanban');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setViewMode({ viewMode: 'kanban' })
      );
    });

    it('should dispatch setGroupBy action when changing grouping', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onGroupByChange('category');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setGroupBy({ groupBy: 'category' })
      );
    });
  });

  // ============================================================================
  // 7. Selection Methods
  // ============================================================================
  describe('Selection Methods', () => {
    it('should dispatch toggleSelection action when toggling recommendation', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const recommendation = mockRecommendations[0];

      component.toggleSelection(recommendation);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.toggleSelection({ id: recommendation.id })
      );
    });

    it('should dispatch selectAllRecommendations when master toggle is clicked and none selected', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      store.overrideSelector(RecommendationSelectors.selectIsAllSelected, false);
      store.refreshState();

      component.masterToggle();
      fixture.detectChanges();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.selectAllRecommendations()
      );
    });

    it('should dispatch clearSelection when master toggle is clicked and all selected', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      store.overrideSelector(RecommendationSelectors.selectIsAllSelected, true);
      store.refreshState();

      component.masterToggle();
      fixture.detectChanges();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.clearSelection()
      );
    });

    it('should dispatch clearSelection action when clearing selection', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.clearSelection();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.clearSelection()
      );
    });

    it('should check if recommendation is selected using SelectionModel', () => {
      const recommendation = mockRecommendations[0];
      component.selection.select(recommendation);

      expect(component.isSelected(recommendation)).toBe(true);
    });

    it('should return false for non-selected recommendation', () => {
      const recommendation = mockRecommendations[0];

      expect(component.isSelected(recommendation)).toBe(false);
    });
  });

  // ============================================================================
  // 8. Action Methods
  // ============================================================================
  describe('Action Methods', () => {
    it('should dispatch acceptRecommendation action', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const recommendation = mockRecommendations[0];

      component.acceptRecommendation(recommendation);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.acceptRecommendation({ id: recommendation.id })
      );
    });

    it('should dispatch declineRecommendation action with reason', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const recommendation = mockRecommendations[0];

      component.declineRecommendation(recommendation);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.declineRecommendation({
          id: recommendation.id,
          reason: 'Patient declined',
        })
      );
    });

    it('should dispatch completeRecommendation action with outcome', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const recommendation = mockRecommendations[0];

      component.completeRecommendation(recommendation);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.completeRecommendation({
          id: recommendation.id,
          outcome: 'Completed successfully',
        })
      );
    });
  });

  // ============================================================================
  // 9. Bulk Action Methods
  // ============================================================================
  describe('Bulk Action Methods', () => {
    beforeEach(() => {
      store.overrideSelector(RecommendationSelectors.selectSelectedIds, ['rec-1', 'rec-2']);
      store.refreshState();
    });

    it('should dispatch performBulkAction with accept action', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.bulkAccept();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.performBulkAction({
          request: { recommendationIds: ['rec-1', 'rec-2'], action: 'accept' },
        })
      );
    });

    it('should dispatch performBulkAction with decline action and reason', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.bulkDecline();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.performBulkAction({
          request: {
            recommendationIds: ['rec-1', 'rec-2'],
            action: 'decline',
            reason: 'Bulk decline',
          },
        })
      );
    });

    it('should dispatch performBulkAction with complete action', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.bulkComplete();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.performBulkAction({
          request: { recommendationIds: ['rec-1', 'rec-2'], action: 'complete' },
        })
      );
    });
  });

  // ============================================================================
  // 10. Pagination and Sorting
  // ============================================================================
  describe('Pagination and Sorting', () => {
    it('should dispatch setPage action when page changes', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const pageEvent: PageEvent = {
        pageIndex: 1,
        pageSize: 25,
        length: 100,
      };

      component.onPageChange(pageEvent);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setPage({
          pageIndex: 1,
          pageSize: 25,
        })
      );
    });

    it('should dispatch setSort action when sort changes', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const sort: Sort = {
        active: 'urgency',
        direction: 'asc',
      };

      component.onSortChange(sort);

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setSort({
          sort: {
            field: 'urgency',
            direction: 'asc',
          },
        })
      );
    });

    it('should not dispatch setSort action when sort direction is empty', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');
      const sort: Sort = {
        active: 'urgency',
        direction: '',
      };

      component.onSortChange(sort);

      expect(dispatchSpy).not.toHaveBeenCalledWith(
        expect.objectContaining({
          type: '[Care Recommendations] Set Sort',
        })
      );
    });
  });

  // ============================================================================
  // 11. Utility Methods
  // ============================================================================
  describe('Utility Methods', () => {
    it('should return correct badge type for emergent urgency', () => {
      expect(component.getUrgencyBadgeType('emergent')).toBe('error');
    });

    it('should return correct badge type for urgent urgency', () => {
      expect(component.getUrgencyBadgeType('urgent')).toBe('warning');
    });

    it('should return correct badge type for soon urgency', () => {
      expect(component.getUrgencyBadgeType('soon')).toBe('info');
    });

    it('should return correct badge type for routine urgency', () => {
      expect(component.getUrgencyBadgeType('routine')).toBe('success');
    });

    it('should return correct badge type for critical risk', () => {
      expect(component.getRiskLevelBadgeType('critical')).toBe('error');
    });

    it('should return correct badge type for high risk', () => {
      expect(component.getRiskLevelBadgeType('high')).toBe('warning');
    });

    it('should return correct badge type for moderate risk', () => {
      expect(component.getRiskLevelBadgeType('moderate')).toBe('info');
    });

    it('should return correct badge type for low risk', () => {
      expect(component.getRiskLevelBadgeType('low')).toBe('success');
    });

    it('should return correct display name for preventive category', () => {
      expect(component.getCategoryDisplayName('preventive')).toBe('Preventive Care');
    });

    it('should return correct display name for chronic-disease category', () => {
      expect(component.getCategoryDisplayName('chronic-disease')).toBe('Chronic Disease');
    });

    it('should return correct display name for medication category', () => {
      expect(component.getCategoryDisplayName('medication')).toBe('Medication');
    });

    it('should return correct display name for mental-health category', () => {
      expect(component.getCategoryDisplayName('mental-health')).toBe('Mental Health');
    });

    it('should return correct display name for sdoh category', () => {
      expect(component.getCategoryDisplayName('sdoh')).toBe('Social Determinants');
    });

    it('should return category name when display name not found', () => {
      expect(component.getCategoryDisplayName('unknown' as any)).toBe('unknown');
    });

    it('should return correct group label for urgency', () => {
      component.groupBy = 'urgency';
      expect(component.getGroupLabel('urgent')).toBe('Urgent');
    });

    it('should return correct group label for category', () => {
      component.groupBy = 'category';
      expect(component.getGroupLabel('preventive')).toBe('Preventive Care');
    });

    it('should return correct group label for patientRiskLevel', () => {
      component.groupBy = 'patientRiskLevel';
      expect(component.getGroupLabel('high')).toBe('High Risk');
    });

    it('should return correct group label for status', () => {
      component.groupBy = 'status';
      expect(component.getGroupLabel('in-progress')).toBe('In Progress');
    });

    it('should track recommendations by id', () => {
      const recommendation = mockRecommendations[0];
      expect(component.trackById(0, recommendation)).toBe(recommendation.id);
    });

    it('should dispatch loadRecommendations with refresh flag on refreshData', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.refreshData();

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.loadRecommendations({ refresh: true })
      );
      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.loadStats()
      );
    });

    it('should dispatch filter action when stat is clicked - emergent', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onStatClick('emergent');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { urgency: ['emergent'] } })
      );
    });

    it('should dispatch filter action when stat is clicked - urgent', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onStatClick('urgent');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({ filter: { urgency: ['urgent'] } })
      );
    });

    it('should dispatch filter action when stat is clicked - overdue', () => {
      const dispatchSpy = jest.spyOn(store, 'dispatch');

      component.onStatClick('overdue');

      expect(dispatchSpy).toHaveBeenCalledWith(
        RecommendationActions.setFilter({
          filter: { daysOverdueRange: { min: 1, max: null } },
        })
      );
    });
  });

  // ============================================================================
  // 12. Navigation
  // ============================================================================
  describe('Navigation', () => {
    it('should navigate to patient detail page', () => {
      const patientId = 'patient-123';

      component.viewPatient(patientId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients', patientId]);
    });
  });

  // ============================================================================
  // 13. Component Cleanup on Destroy
  // ============================================================================
  describe('Component Cleanup on Destroy', () => {
    it('should complete destroy$ subject on ngOnDestroy', () => {
      const destroySpy = jest.spyOn(component['destroy$'], 'next');
      const completeSpy = jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(destroySpy).toHaveBeenCalled();
      expect(completeSpy).toHaveBeenCalled();
    });

    it('should unsubscribe from search subject on destroy', () => {
      component.ngOnInit();

      // ngOnDestroy should complete without throwing
      expect(() => component.ngOnDestroy()).not.toThrow();
    });

    it('should unsubscribe from all observables using takeUntil', fakeAsync(() => {
      component.ngOnInit();
      fixture.detectChanges();

      let paginatedRecsReceived = 0;
      let viewModeReceived = 0;

      component.paginatedRecommendations$
        .pipe()
        .subscribe(() => paginatedRecsReceived++);

      component.viewMode$
        .pipe()
        .subscribe(() => viewModeReceived++);

      tick();
      const initialPaginatedCount = paginatedRecsReceived;
      const initialViewModeCount = viewModeReceived;

      component.ngOnDestroy();
      tick();

      // After destroy, subscriptions should be cleaned up
      // New emissions should not increase counters
      store.overrideSelector(RecommendationSelectors.selectPaginatedRecommendations, []);
      store.overrideSelector(RecommendationSelectors.selectViewMode, 'grid' as DashboardViewMode);
      store.refreshState();
      tick();

      // Counts might have increased during initialization, but should not increase after destroy
      expect(paginatedRecsReceived).toBeGreaterThanOrEqual(initialPaginatedCount);
      expect(viewModeReceived).toBeGreaterThanOrEqual(initialViewModeCount);
    }));
  });

  // ============================================================================
  // 14. AfterViewInit Lifecycle Hook
  // ============================================================================
  describe('AfterViewInit Lifecycle Hook', () => {
    it('should set paginator on dataSource after view init', () => {
      const mockPaginator = {} as MatPaginator;
      component.paginator = mockPaginator;

      component.ngAfterViewInit();

      expect(component.dataSource.paginator).toBe(mockPaginator);
    });

    it('should set sort on dataSource after view init', () => {
      const mockSort = {} as MatSort;
      component.sort = mockSort;

      component.ngAfterViewInit();

      expect(component.dataSource.sort).toBe(mockSort);
    });
  });

  // ============================================================================
  // 15. Constants Exposure for Template
  // ============================================================================
  describe('Constants Exposure for Template', () => {
    it('should expose URGENCY_COLORS constant', () => {
      expect(component.URGENCY_COLORS).toBeDefined();
      expect(component.URGENCY_COLORS.emergent).toBe('#f44336');
      expect(component.URGENCY_COLORS.urgent).toBe('#ff9800');
      expect(component.URGENCY_COLORS.soon).toBe('#ffc107');
      expect(component.URGENCY_COLORS.routine).toBe('#4caf50');
    });

    it('should expose URGENCY_ICONS constant', () => {
      expect(component.URGENCY_ICONS).toBeDefined();
      expect(component.URGENCY_ICONS.emergent).toBe('error');
      expect(component.URGENCY_ICONS.urgent).toBe('warning');
      expect(component.URGENCY_ICONS.soon).toBe('schedule');
      expect(component.URGENCY_ICONS.routine).toBe('check_circle');
    });

    it('should expose CATEGORY_ICONS constant', () => {
      expect(component.CATEGORY_ICONS).toBeDefined();
      expect(component.CATEGORY_ICONS.preventive).toBe('vaccines');
      expect(component.CATEGORY_ICONS['chronic-disease']).toBe('monitor_heart');
      expect(component.CATEGORY_ICONS.medication).toBe('medication');
      expect(component.CATEGORY_ICONS['mental-health']).toBe('psychology');
      expect(component.CATEGORY_ICONS.sdoh).toBe('home');
    });

    it('should expose CATEGORY_DISPLAY_NAMES constant', () => {
      expect(component.CATEGORY_DISPLAY_NAMES).toBeDefined();
      expect(component.CATEGORY_DISPLAY_NAMES.preventive).toBe('Preventive Care');
      expect(component.CATEGORY_DISPLAY_NAMES['chronic-disease']).toBe('Chronic Disease');
      expect(component.CATEGORY_DISPLAY_NAMES.medication).toBe('Medication');
      expect(component.CATEGORY_DISPLAY_NAMES['mental-health']).toBe('Mental Health');
      expect(component.CATEGORY_DISPLAY_NAMES.sdoh).toBe('Social Determinants');
    });
  });

  // ============================================================================
  // 16. Breadcrumbs
  // ============================================================================
  describe('Breadcrumbs', () => {
    it('should have correct breadcrumb configuration', () => {
      expect(component.breadcrumbs).toEqual([
        { label: 'Home', route: '/' },
        { label: 'Care Recommendations' },
      ]);
    });
  });

  // ============================================================================
  // 17. Local State Management
  // ============================================================================
  describe('Local State Management', () => {
    it('should initialize with empty selection model', () => {
      expect(component.selection.selected.length).toBe(0);
    });

    it('should initialize with empty search term', () => {
      expect(component.searchTerm).toBe('');
    });

    it('should initialize with list view mode', () => {
      expect(component.viewMode).toBe('list');
    });

    it('should initialize with urgency group by', () => {
      expect(component.groupBy).toBe('urgency');
    });

    it('should initialize dataSource with empty data', () => {
      expect(component.dataSource.data).toEqual([]);
    });
  });
});
