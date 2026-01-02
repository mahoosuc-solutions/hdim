import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// Material Imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatTableModule,
  MatTableDataSource,
} from '@angular/material/table';
import {
  MatPaginatorModule,
  MatPaginator,
  PageEvent,
} from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { SelectionModel } from '@angular/cdk/collections';

// NgRx
import { Store } from '@ngrx/store';
import { Observable, Subject, combineLatest } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Shared Components
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';

// Local Components
import { RecommendationStatsPanelComponent } from './components/recommendation-stats-panel/recommendation-stats-panel.component';
import { BulkActionsToolbarComponent } from './components/bulk-actions-toolbar/bulk-actions-toolbar.component';

// Store
import { AppState } from '../../store';
import * as RecommendationActions from '../../store/actions/care-recommendation.actions';
import * as RecommendationSelectors from '../../store/selectors/care-recommendation.selectors';

// Models & Services
import { CareRecommendationService } from '../../services/care-recommendation.service';
import { ToastService } from '../../services/toast.service';
import {
  DashboardRecommendation,
  RecommendationDashboardStats,
  RecommendationFilterConfig,
  DashboardViewMode,
  GroupByField,
  RecommendationCategory,
  RecommendationUrgency,
  RecommendationStatus,
  URGENCY_COLORS,
  URGENCY_ICONS,
  CATEGORY_ICONS,
  CATEGORY_DISPLAY_NAMES,
  getUrgencyBadgeType,
  getRiskLevelBadgeType,
} from '../../models/care-recommendation.model';
import { RiskLevel } from '../../models/patient-health.model';

@Component({
  selector: 'app-care-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    // Material
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatMenuModule,
    MatChipsModule,
    MatSelectModule,
    MatInputModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatButtonToggleModule,
    MatDividerModule,
    MatBadgeModule,
    // Shared Components
    PageHeaderComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    LoadingOverlayComponent,
    // Local Components
    RecommendationStatsPanelComponent,
    BulkActionsToolbarComponent,
  ],
  templateUrl: './care-recommendations.component.html',
  styleUrl: './care-recommendations.component.scss',
})
export class CareRecommendationsComponent
  implements OnInit, OnDestroy, AfterViewInit
{
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Observable streams from store
  recommendations$: Observable<DashboardRecommendation[]>;
  filteredRecommendations$: Observable<DashboardRecommendation[]>;
  paginatedRecommendations$: Observable<DashboardRecommendation[]>;
  groupedRecommendations$: Observable<Map<string, DashboardRecommendation[]>>;
  stats$: Observable<RecommendationDashboardStats | null>;
  loading$: Observable<boolean>;
  selectedIds$: Observable<string[]>;
  selectedCount$: Observable<number>;
  isAllSelected$: Observable<boolean>;
  isIndeterminate$: Observable<boolean>;
  viewMode$: Observable<DashboardViewMode>;
  hasActiveFilters$: Observable<boolean>;
  error$: Observable<string | null>;

  // Local state
  dataSource = new MatTableDataSource<DashboardRecommendation>([]);
  selection = new SelectionModel<DashboardRecommendation>(true, []);
  searchTerm = '';
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // View configuration
  viewMode: DashboardViewMode = 'list';
  groupBy: GroupByField = 'urgency';

  // Filter options
  urgencyOptions: RecommendationUrgency[] = [
    'emergent',
    'urgent',
    'soon',
    'routine',
  ];
  categoryOptions: RecommendationCategory[] = [
    'preventive',
    'chronic-disease',
    'medication',
    'mental-health',
    'sdoh',
  ];
  riskLevelOptions: RiskLevel[] = ['critical', 'high', 'moderate', 'low'];
  statusOptions: RecommendationStatus[] = [
    'pending',
    'in-progress',
    'completed',
    'declined',
  ];

  // Active filters
  activeUrgencyFilters: RecommendationUrgency[] = [];
  activeCategoryFilters: RecommendationCategory[] = [];
  activeRiskFilters: RiskLevel[] = [];
  activeStatusFilters: RecommendationStatus[] = [];

  // Table columns
  displayedColumns: string[] = [
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
  ];

  // Breadcrumbs
  breadcrumbs = [
    { label: 'Home', route: '/' },
    { label: 'Care Recommendations' },
  ];

  // Constants for template
  readonly URGENCY_COLORS = URGENCY_COLORS;
  readonly URGENCY_ICONS = URGENCY_ICONS;
  readonly CATEGORY_ICONS = CATEGORY_ICONS;
  readonly CATEGORY_DISPLAY_NAMES = CATEGORY_DISPLAY_NAMES;

  constructor(
    private store: Store<AppState>,
    private recommendationService: CareRecommendationService,
    private toastService: ToastService,
    private dialog: MatDialog,
    private router: Router
  ) {
    // Initialize observables from store
    this.recommendations$ = this.store.select(
      RecommendationSelectors.selectAllRecommendations
    );
    this.filteredRecommendations$ = this.store.select(
      RecommendationSelectors.selectFilteredRecommendations
    );
    this.paginatedRecommendations$ = this.store.select(
      RecommendationSelectors.selectPaginatedRecommendations
    );
    this.groupedRecommendations$ = this.store.select(
      RecommendationSelectors.selectGroupedRecommendations
    );
    this.stats$ = this.store.select(RecommendationSelectors.selectStats);
    this.loading$ = this.store.select(RecommendationSelectors.selectLoading);
    this.selectedIds$ = this.store.select(
      RecommendationSelectors.selectSelectedIds
    );
    this.selectedCount$ = this.store.select(
      RecommendationSelectors.selectSelectedCount
    );
    this.isAllSelected$ = this.store.select(
      RecommendationSelectors.selectIsAllSelected
    );
    this.isIndeterminate$ = this.store.select(
      RecommendationSelectors.selectIsIndeterminate
    );
    this.viewMode$ = this.store.select(RecommendationSelectors.selectViewMode);
    this.hasActiveFilters$ = this.store.select(
      RecommendationSelectors.selectHasActiveFilters
    );
    this.error$ = this.store.select(RecommendationSelectors.selectError);
  }

  ngOnInit(): void {
    // Load initial data
    this.store.dispatch(RecommendationActions.loadRecommendations({}));
    this.store.dispatch(RecommendationActions.loadStats());

    // Setup search debounce
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        this.store.dispatch(
          RecommendationActions.setSearchTerm({ searchTerm: term })
        );
      });

    // Subscribe to paginated recommendations for table
    this.paginatedRecommendations$
      .pipe(takeUntil(this.destroy$))
      .subscribe((recommendations) => {
        this.dataSource.data = recommendations;
      });

    // Subscribe to view mode changes
    this.viewMode$.pipe(takeUntil(this.destroy$)).subscribe((mode) => {
      this.viewMode = mode;
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ============================================
  // Search & Filter Methods
  // ============================================

  onSearchChange(term: string): void {
    this.searchSubject.next(term);
  }

  onUrgencyFilterChange(urgencies: RecommendationUrgency[]): void {
    this.activeUrgencyFilters = urgencies;
    this.store.dispatch(
      RecommendationActions.setFilter({ filter: { urgency: urgencies } })
    );
  }

  onCategoryFilterChange(categories: RecommendationCategory[]): void {
    this.activeCategoryFilters = categories;
    this.store.dispatch(
      RecommendationActions.setFilter({ filter: { category: categories } })
    );
  }

  onRiskFilterChange(risks: RiskLevel[]): void {
    this.activeRiskFilters = risks;
    this.store.dispatch(
      RecommendationActions.setFilter({ filter: { patientRiskLevel: risks } })
    );
  }

  onStatusFilterChange(statuses: RecommendationStatus[]): void {
    this.activeStatusFilters = statuses;
    this.store.dispatch(
      RecommendationActions.setFilter({ filter: { status: statuses } })
    );
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.activeUrgencyFilters = [];
    this.activeCategoryFilters = [];
    this.activeRiskFilters = [];
    this.activeStatusFilters = [];
    this.store.dispatch(RecommendationActions.clearFilters());
  }

  // ============================================
  // View Mode Methods
  // ============================================

  onViewModeChange(mode: DashboardViewMode): void {
    this.store.dispatch(RecommendationActions.setViewMode({ viewMode: mode }));
  }

  onGroupByChange(groupBy: GroupByField): void {
    this.store.dispatch(RecommendationActions.setGroupBy({ groupBy }));
  }

  // ============================================
  // Selection Methods
  // ============================================

  isSelected(recommendation: DashboardRecommendation): boolean {
    return this.selection.isSelected(recommendation);
  }

  toggleSelection(recommendation: DashboardRecommendation): void {
    this.store.dispatch(
      RecommendationActions.toggleSelection({ id: recommendation.id })
    );
  }

  masterToggle(): void {
    this.store
      .select(RecommendationSelectors.selectIsAllSelected)
      .pipe(takeUntil(this.destroy$))
      .subscribe((isAllSelected) => {
        if (isAllSelected) {
          this.store.dispatch(RecommendationActions.clearSelection());
        } else {
          this.store.dispatch(RecommendationActions.selectAllRecommendations());
        }
      })
      .unsubscribe();
  }

  clearSelection(): void {
    this.store.dispatch(RecommendationActions.clearSelection());
  }

  // ============================================
  // Action Methods
  // ============================================

  acceptRecommendation(recommendation: DashboardRecommendation): void {
    this.store.dispatch(
      RecommendationActions.acceptRecommendation({ id: recommendation.id })
    );
  }

  declineRecommendation(recommendation: DashboardRecommendation): void {
    // In a real implementation, you'd open a dialog to get the reason
    const reason = 'Patient declined'; // Placeholder
    this.store.dispatch(
      RecommendationActions.declineRecommendation({
        id: recommendation.id,
        reason,
      })
    );
  }

  completeRecommendation(recommendation: DashboardRecommendation): void {
    // In a real implementation, you'd open a dialog to get the outcome
    const outcome = 'Completed successfully'; // Placeholder
    this.store.dispatch(
      RecommendationActions.completeRecommendation({
        id: recommendation.id,
        outcome,
      })
    );
  }

  viewPatient(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }

  // ============================================
  // Bulk Action Methods
  // ============================================

  bulkAccept(): void {
    this.store.select(RecommendationSelectors.selectSelectedIds).subscribe(
      (ids) => {
        this.store.dispatch(
          RecommendationActions.performBulkAction({
            request: { recommendationIds: ids, action: 'accept' },
          })
        );
      }
    ).unsubscribe();
  }

  bulkDecline(): void {
    this.store.select(RecommendationSelectors.selectSelectedIds).subscribe(
      (ids) => {
        this.store.dispatch(
          RecommendationActions.performBulkAction({
            request: {
              recommendationIds: ids,
              action: 'decline',
              reason: 'Bulk decline',
            },
          })
        );
      }
    ).unsubscribe();
  }

  bulkComplete(): void {
    this.store.select(RecommendationSelectors.selectSelectedIds).subscribe(
      (ids) => {
        this.store.dispatch(
          RecommendationActions.performBulkAction({
            request: { recommendationIds: ids, action: 'complete' },
          })
        );
      }
    ).unsubscribe();
  }

  // ============================================
  // Pagination Methods
  // ============================================

  onPageChange(event: PageEvent): void {
    this.store.dispatch(
      RecommendationActions.setPage({
        pageIndex: event.pageIndex,
        pageSize: event.pageSize,
      })
    );
  }

  // ============================================
  // Sort Methods
  // ============================================

  onSortChange(sort: Sort): void {
    if (sort.direction) {
      this.store.dispatch(
        RecommendationActions.setSort({
          sort: {
            field: sort.active as any,
            direction: sort.direction,
          },
        })
      );
    }
  }

  // ============================================
  // Utility Methods
  // ============================================

  refreshData(): void {
    this.store.dispatch(RecommendationActions.loadRecommendations({ refresh: true }));
    this.store.dispatch(RecommendationActions.loadStats());
  }

  getUrgencyBadgeType(urgency: RecommendationUrgency): 'error' | 'warning' | 'info' | 'success' {
    return getUrgencyBadgeType(urgency);
  }

  getRiskLevelBadgeType(risk: RiskLevel): 'error' | 'warning' | 'info' | 'success' {
    return getRiskLevelBadgeType(risk);
  }

  getCategoryDisplayName(category: RecommendationCategory): string {
    return CATEGORY_DISPLAY_NAMES[category] || category;
  }

  getCategoryIcon(category: RecommendationCategory): string {
    return CATEGORY_ICONS[category] || 'help';
  }

  getGroupLabel(key: string): string {
    // Convert key to display label
    switch (this.groupBy) {
      case 'urgency':
        return key.charAt(0).toUpperCase() + key.slice(1);
      case 'category':
        return CATEGORY_DISPLAY_NAMES[key as RecommendationCategory] || key;
      case 'patientRiskLevel':
        return key.charAt(0).toUpperCase() + key.slice(1) + ' Risk';
      case 'status':
        return key
          .split('-')
          .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
          .join(' ');
      default:
        return key;
    }
  }

  onStatClick(stat: string): void {
    // Filter by the clicked statistic
    switch (stat) {
      case 'emergent':
      case 'urgent':
        this.onUrgencyFilterChange([stat as RecommendationUrgency]);
        break;
      case 'overdue':
        // Filter for overdue items
        this.store.dispatch(
          RecommendationActions.setFilter({
            filter: { daysOverdueRange: { min: 1, max: null } },
          })
        );
        break;
    }
  }

  trackById(index: number, item: DashboardRecommendation): string {
    return item.id;
  }
}
