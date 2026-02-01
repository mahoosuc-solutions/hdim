import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { CareGapTrendComponent } from '../../shared/components/care-gap-trend/care-gap-trend.component';
import { CareGapService, CareGapTrendPoint, CareGapUpdate } from '../../services/care-gap.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Care Gap Stats Response from API
 */
interface CareGapStats {
  totalGaps: number;
  highPriorityCount: number;
  mediumPriorityCount: number;
  lowPriorityCount: number;
  overdueCount: number;
  closedThisMonth: number;
  byType: Record<string, number>;
}

/**
 * Care Gap Statistics Dashboard Component
 *
 * Displays KPI statistics cards and trend visualization for care gaps.
 * Features:
 * - 4 stat cards: Total Gaps, High Priority, Overdue, Closed This Month
 * - 30-day trend chart for gap tracking
 * - Real-time updates via gapUpdates$ observable
 * - Click navigation to filtered care gap views
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - No PHI displayed in aggregate statistics
 *
 * Accessibility:
 * - ARIA labels on stat cards
 * - Keyboard navigation support
 * - Screen reader friendly
 *
 * Sprint 2 - Issue #240: Care Gap Statistics Cards
 */
@Component({
  selector: 'app-care-gap-stats-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    StatCardComponent,
    CareGapTrendComponent,
  ],
  templateUrl: './care-gap-stats-dashboard.component.html',
  styleUrls: ['./care-gap-stats-dashboard.component.scss'],
})
export class CareGapStatsDashboardComponent implements OnInit, OnDestroy {
  // Signals for reactive state
  totalGaps = signal(0);
  highPriorityCount = signal(0);
  overdueCount = signal(0);
  closedThisMonth = signal(0);

  // Computed signals for display values
  totalGapsDisplay = computed(() => this.totalGaps().toLocaleString());
  highPriorityDisplay = computed(() => this.highPriorityCount().toLocaleString());
  overdueDisplay = computed(() => this.overdueCount().toLocaleString());
  closedDisplay = computed(() => this.closedThisMonth().toLocaleString());

  // Trend data
  trendData = signal<CareGapTrendPoint[]>([]);

  // Loading state
  loading = signal(true);
  loadError = signal<string | null>(null);

  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private careGapService: CareGapService,
    private router: Router,
    private logger: LoggerService
  ) {
  }

  ngOnInit(): void {
    this.loadStatistics();
    this.loadTrendData();
    this.subscribeToGapUpdates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load care gap statistics from backend
   * Note: Using demo data for now - will connect to real API when available
   */
  private loadStatistics(): void {
    this.loading.set(true);
    this.loadError.set(null);

    // TODO: Replace with actual API call when getCareGapStats() is available for tenant-level stats
    // For now, using mock data based on the service's response structure
    setTimeout(() => {
      // Simulate API response
      const stats: CareGapStats = {
        totalGaps: 142,
        highPriorityCount: 28,
        mediumPriorityCount: 57,
        lowPriorityCount: 57,
        overdueCount: 15,
        closedThisMonth: 23,
        byType: {
          screening: 42,
          medication: 35,
          followup: 28,
          lab: 21,
          assessment: 16,
        },
      };

      this.totalGaps.set(stats.totalGaps);
      this.highPriorityCount.set(stats.highPriorityCount);
      this.overdueCount.set(stats.overdueCount);
      this.closedThisMonth.set(stats.closedThisMonth);

      this.loading.set(false);
      this.logger.info('Care gap statistics loaded', { totalGaps: stats.totalGaps });
    }, 300);
  }

  /**
   * Load 30-day trend data
   */
  private loadTrendData(): void {
    this.careGapService
      .getCareGapTrends(30)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (trends) => {
          this.trendData.set(trends);
          this.logger.info('Care gap trends loaded', { days: 30, points: trends.length });
        },
        error: (error) => {
          this.logger.error('Failed to load care gap trends', error);
          // Set empty array on error - component will show "No data" state
          this.trendData.set([]);
        },
      });
  }

  /**
   * Subscribe to real-time gap updates
   * Increment/decrement counters when gaps are created or closed
   */
  private subscribeToGapUpdates(): void {
    this.careGapService.gapUpdates$
      .pipe(takeUntil(this.destroy$))
      .subscribe((update: CareGapUpdate | null) => {
        if (!update) return;

        switch (update.type) {
          case 'detected':
            // New gap detected
            this.totalGaps.update((val) => val + 1);
            this.logger.info('Gap detected - incrementing total', { gapId: update.gapId });
            break;

          case 'closed':
            // Gap closed
            this.totalGaps.update((val) => Math.max(0, val - 1));
            this.closedThisMonth.update((val) => val + 1);
            this.logger.info('Gap closed - decrementing total', { gapId: update.gapId });
            break;

          case 'bulk-closed':
            // Multiple gaps closed
            const closedCount = update.successCount || 0;
            this.totalGaps.update((val) => Math.max(0, val - closedCount));
            this.closedThisMonth.update((val) => val + closedCount);
            this.logger.info('Bulk gaps closed', { count: closedCount });
            break;

          case 'batch-detected':
            // Batch of gaps detected
            const detectedCount = update.successCount || 0;
            this.totalGaps.update((val) => val + detectedCount);
            this.logger.info('Batch gaps detected', { count: detectedCount });
            break;
        }

        // Reload trend data after significant changes
        if (
          update.type === 'bulk-closed' ||
          update.type === 'batch-detected'
        ) {
          this.loadTrendData();
        }
      });
  }

  /**
   * Navigate to care gaps filtered by high priority
   */
  viewHighPriority(): void {
    this.logger.info('Navigating to high priority care gaps');
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        priority: 'HIGH',
        source: 'stats-dashboard',
      },
    });
  }

  /**
   * Navigate to overdue care gaps
   */
  viewOverdue(): void {
    this.logger.info('Navigating to overdue care gaps');
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        status: 'OVERDUE',
        source: 'stats-dashboard',
      },
    });
  }

  /**
   * Navigate to closed care gaps for this month
   */
  viewClosedThisMonth(): void {
    this.logger.info('Navigating to closed care gaps');
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        status: 'CLOSED',
        startDate: firstDay.toISOString().split('T')[0],
        source: 'stats-dashboard',
      },
    });
  }

  /**
   * Navigate to all care gaps
   */
  viewAllGaps(): void {
    this.logger.info('Navigating to all care gaps');
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        source: 'stats-dashboard',
      },
    });
  }
}
