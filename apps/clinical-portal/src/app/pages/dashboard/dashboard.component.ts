import { Component, OnInit, Type } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { NgxChartsModule, Color, ScaleType } from '@swimlane/ngx-charts';
import { forkJoin, catchError, of, finalize, takeUntil } from 'rxjs';
import { injectDestroy } from '../../shared/utils';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { CqlEvaluation } from '../../models/evaluation.model';
import { PatientSummary } from '../../models/patient.model';
import { MeasureInfo } from '../../models/cql-library.model';
import { CareGapAlert, CareGapSummary, getCareGapIcon, getUrgencyColor, formatDaysOverdue } from '../../models/care-gap.model';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { StatCardComponent, StatCardAction } from '../../shared/components/stat-card/stat-card.component';
import { BatchCalculationComponent } from '../../shared/components/batch-calculation/batch-calculation.component';
import { SystemActivitySectionComponent } from '../../shared/components/system-activity-section/system-activity-section.component';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';
import { UserRoleService, UserRole } from '../../shared/services/user-role.service';
import { MeasureFavoritesService, FavoriteMeasure, RecentMeasure } from '../../services/measure-favorites.service';

/**
 * Dashboard Statistics Interface
 */
export interface DashboardStatistics {
  totalEvaluations: number;
  totalPatients: number;
  overallCompliance: number;
  recentEvaluations: number;
  complianceChange: number;
}

/**
 * Recent Activity Interface
 */
export interface RecentActivity {
  id: string;
  date: string;
  patientId: string;
  patientName: string;
  patientMrn?: string;
  patientMrnAuthority?: string;
  measureName: string;
  outcome: 'compliant' | 'non-compliant' | 'not-eligible';
}

/**
 * Measure Performance Interface
 */
export interface MeasurePerformance {
  measureId: string;
  measureName: string;
  category: string;
  evaluationCount: number;
  complianceRate: number;
  trend: 'up' | 'down' | 'stable';
}

/**
 * Quick Action Interface
 */
export interface QuickAction {
  label: string;
  icon: string;
  route: string;
  color: 'primary' | 'accent' | 'warn';
  ariaLabel: string;
  tooltip?: string;
  loading?: boolean;
  success?: boolean;
}

/**
 * Compliance Trend Data Point
 */
export interface ComplianceTrendPoint {
  period: string;
  complianceRate: number;
  count: number;
}

/**
 * NgxCharts Data Point
 */
export interface ChartDataPoint {
  name: string;
  value: number;
}

/**
 * NgxCharts Series Data (for line/area charts)
 */
export interface ChartSeriesData {
  name: string;
  series: ChartDataPoint[];
}

/**
 * NgxCharts Color Scheme
 * Uses the Color type from @swimlane/ngx-charts
 */
export type ChartColorScheme = Color;

/**
 * Dashboard Component
 * Provides overview statistics, recent activity, and quick actions
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatGridListModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatButtonToggleModule,
    NgxChartsModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
    StatCardComponent,
    BatchCalculationComponent,
    SystemActivitySectionComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  // Subscription cleanup
  private destroy$ = injectDestroy();

  // Role management
  currentRole: UserRole = UserRole.ADMIN;
  UserRole = UserRole; // Expose enum for template use

  // Dynamic component loading
  roleComponent: Type<unknown> | null = null;
  loadingRoleComponent = false;
  // Data properties
  statistics: DashboardStatistics = {
    totalEvaluations: 0,
    totalPatients: 0,
    overallCompliance: 0,
    recentEvaluations: 0,
    complianceChange: 0,
  };

  recentActivity: RecentActivity[] = [];
  measurePerformance: MeasurePerformance[] = [];
  complianceTrends: ComplianceTrendPoint[] = [];
  careGapSummary: CareGapSummary | null = null;
  urgentCareGaps: CareGapAlert[] = [];

  // Chart data for ngx-charts
  complianceTrendChartData: ChartSeriesData[] = [];
  measurePerformanceChartData: ChartDataPoint[] = [];

  // Chart configuration
  // Line chart config
  lineChartView: [number, number] = [700, 300];
  lineChartShowXAxis = true;
  lineChartShowYAxis = true;
  lineChartGradient = false;
  lineChartShowLegend = false;
  lineChartShowXAxisLabel = true;
  lineChartXAxisLabel = 'Period';
  lineChartShowYAxisLabel = true;
  lineChartYAxisLabel = 'Compliance Rate (%)';
  lineChartColorScheme: ChartColorScheme = {
    name: 'Compliance',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#5AA454', '#E44D25', '#CFC0BB', '#7aa3e5', '#a8385d', '#aae3f5']
  };

  // Bar chart config for measure performance
  barChartView: [number, number] = [700, 400];
  barChartShowXAxis = true;
  barChartShowYAxis = true;
  barChartGradient = false;
  barChartShowLegend = false;
  barChartShowXAxisLabel = true;
  barChartXAxisLabel = 'Measure';
  barChartShowYAxisLabel = true;
  barChartYAxisLabel = 'Compliance Rate (%)';
  barChartColorScheme: ChartColorScheme = {
    name: 'Performance',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };

  // UI State
  loading = false;
  refreshSuccess = false;
  error: string | null = null;
  lastUpdated: Date | null = null;

  // Trend settings
  trendPeriod: 'daily' | 'weekly' | 'monthly' = 'weekly';
  trendDirection: 'up' | 'down' | 'stable' = 'stable';
  dateRange = {
    start: new Date(new Date().setMonth(new Date().getMonth() - 3)),
    end: new Date(),
  };

  // Expose Math for template use
  Math = Math;

  // Expose care gap helper functions for template use
  getCareGapIcon = getCareGapIcon;
  getUrgencyColor = getUrgencyColor;
  formatDaysOverdue = formatDaysOverdue;

  // Quick Actions
  quickActions: QuickAction[] = [
    {
      label: 'New Evaluation',
      icon: 'add_circle',
      route: '/evaluations',
      color: 'primary',
      ariaLabel: 'Create a new quality measure evaluation',
      loading: false,
      success: false,
    },
    {
      label: 'View All Results',
      icon: 'assessment',
      route: '/results',
      color: 'accent',
      ariaLabel: 'View all quality measure results',
      loading: false,
      success: false,
    },
    {
      label: 'View Reports',
      icon: 'description',
      route: '/reports',
      color: 'warn',
      ariaLabel: 'View saved quality measure reports',
      loading: false,
      success: false,
    },
  ];

  // Raw data
  private allEvaluations: CqlEvaluation[] = [];
  private allPatients: PatientSummary[] = [];
  private allMeasures: MeasureInfo[] = [];

  constructor(
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private measureService: MeasureService,
    private router: Router,
    public aiAssistant: AIAssistantService,
    private userRoleService: UserRoleService,
    public measureFavorites: MeasureFavoritesService
  ) {}

  ngOnInit(): void {
    // Load current role from service
    this.currentRole = this.userRoleService.getCurrentRole();

    // Subscribe to role changes
    this.userRoleService.currentRole$
      .pipe(takeUntil(this.destroy$))
      .subscribe((role) => {
        this.currentRole = role;
        this.loadRoleComponent(role);
      });

    this.loadDashboardData();
    this.loadRoleComponent(this.currentRole);
  }

  /**
   * Switch dashboard view by role
   */
  switchRole(role: UserRole): void {
    this.userRoleService.setRole(role);
    this.currentRole = role;
  }

  /**
   * Dynamically load role-specific dashboard component
   */
  private async loadRoleComponent(role: UserRole): Promise<void> {
    // Only load for non-admin roles (admin uses default dashboard)
    if (role === UserRole.ADMIN) {
      this.roleComponent = null;
      return;
    }

    this.loadingRoleComponent = true;

    try {
      switch (role) {
        case UserRole.MEDICAL_ASSISTANT:
          const maModule = await import('./ma-dashboard/ma-dashboard.component');
          this.roleComponent = maModule.MADashboardComponent;
          break;
        case UserRole.REGISTERED_NURSE:
          const rnModule = await import('./rn-dashboard/rn-dashboard.component');
          this.roleComponent = rnModule.RNDashboardComponent;
          break;
        case UserRole.PROVIDER:
          const providerModule = await import('./provider-dashboard/provider-dashboard.component');
          this.roleComponent = providerModule.ProviderDashboardComponent;
          break;
        default:
          this.roleComponent = null;
      }
    } catch (error) {
      console.error('Error loading role component:', error);
      this.roleComponent = null;
    } finally {
      this.loadingRoleComponent = false;
    }
  }

  // Helper functions to safely extract properties from CqlEvaluation
  private isInDenominator(evaluation: CqlEvaluation): boolean {
    return evaluation.evaluationResult?.['InDenominator'] === true;
  }

  private isInNumerator(evaluation: CqlEvaluation): boolean {
    return evaluation.evaluationResult?.['InNumerator'] === true;
  }

  private getLibraryName(evaluation: CqlEvaluation): string {
    return evaluation.library?.name || 'Unknown Measure';
  }

  /**
   * Load all dashboard data using cached services and priority-based loading
   * Phase 1: Load critical above-fold data (patients, recent evaluations)
   * Phase 2: Load secondary data for charts and analytics
   */
  @TrackInteraction('dashboard', 'load-data')
  loadDashboardData(): void {
    this.loading = true;
    this.refreshSuccess = false;
    this.error = null;

    // Priority 1: Load critical data using cached services
    // This shows stats and care gaps quickly
    forkJoin({
      patients: this.patientService.getPatientsSummaryCached(),
      measures: this.measureService.getActiveMeasuresInfoCached(),
    }).pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        console.error('Error loading critical dashboard data:', error);
        this.error = 'Failed to load dashboard data. Please try again.';
        return of(null);
      })
    ).subscribe({
      next: (criticalData) => {
        if (criticalData) {
          this.allPatients = criticalData.patients;
          this.allMeasures = criticalData.measures;

          // Show patient count immediately
          this.statistics.totalPatients = this.allPatients.length;

          // Priority 2: Load evaluations (heavier data) in parallel
          // Using cached version for better performance
          this.loadEvaluationsData();
        }
      }
    });
  }

  /**
   * Load evaluations data (secondary priority)
   * Uses cached data for better performance
   */
  private loadEvaluationsData(): void {
    this.evaluationService.getAllEvaluationsCached().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        console.error('Error loading evaluations:', error);
        // Continue with empty evaluations rather than failing completely
        return of([]);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (evaluations) => {
        this.allEvaluations = evaluations;

        // Calculate all dashboard metrics
        this.calculateStatistics();
        this.generateRecentActivity();
        this.calculateMeasurePerformance();
        this.calculateComplianceTrends();
        this.calculateCareGaps();

        this.lastUpdated = new Date();
        this.refreshSuccess = true;
      }
    });
  }

  /**
   * Force refresh dashboard data (bypasses cache)
   */
  forceRefresh(): void {
    // Invalidate all caches before reloading
    this.patientService.invalidateCache();
    this.evaluationService.invalidateAllCaches();
    this.measureService.invalidateCache();

    this.loadDashboardData();
  }

  /**
   * Calculate dashboard statistics
   */
  private calculateStatistics(): void {
    const totalEvaluations = this.allEvaluations.length;
    const totalPatients = this.allPatients.length;

    // Calculate overall compliance
    const eligibleEvaluations = this.allEvaluations.filter(e => this.isInDenominator(e));
    const compliantEvaluations = eligibleEvaluations.filter(e => this.isInNumerator(e));

    const overallCompliance =
      eligibleEvaluations.length > 0
        ? (compliantEvaluations.length / eligibleEvaluations.length) * 100
        : 0;

    // Calculate recent evaluations (last 30 days)
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const recentEvaluations = this.allEvaluations.filter((e) => {
      const evalDate = new Date(e.evaluationDate);
      return evalDate >= thirtyDaysAgo;
    }).length;

    // Calculate compliance change (comparing last 30 days to previous 30 days)
    const sixtyDaysAgo = new Date();
    sixtyDaysAgo.setDate(sixtyDaysAgo.getDate() - 60);

    const previousPeriodEvals = this.allEvaluations.filter((e) => {
      const evalDate = new Date(e.evaluationDate);
      return evalDate >= sixtyDaysAgo && evalDate < thirtyDaysAgo;
    });

    const previousCompliance =
      previousPeriodEvals.length > 0
        ? (previousPeriodEvals.filter((e) => this.isInNumerator(e) && this.isInDenominator(e)).length /
            previousPeriodEvals.filter((e) => this.isInDenominator(e)).length) *
          100
        : 0;

    const currentPeriodEvals = this.allEvaluations.filter((e) => {
      const evalDate = new Date(e.evaluationDate);
      return evalDate >= thirtyDaysAgo;
    });

    const currentCompliance =
      currentPeriodEvals.length > 0
        ? (currentPeriodEvals.filter((e) => this.isInNumerator(e) && this.isInDenominator(e)).length /
            currentPeriodEvals.filter((e) => this.isInDenominator(e)).length) *
          100
        : 0;

    const complianceChange = currentCompliance - previousCompliance;

    // Update trend direction
    if (complianceChange > 2) {
      this.trendDirection = 'up';
    } else if (complianceChange < -2) {
      this.trendDirection = 'down';
    } else {
      this.trendDirection = 'stable';
    }

    this.statistics = {
      totalEvaluations,
      totalPatients,
      overallCompliance: Math.round(overallCompliance * 100) / 100,
      recentEvaluations,
      complianceChange: Math.round(complianceChange * 100) / 100,
    };
  }

  /**
   * Generate recent activity list
   */
  private generateRecentActivity(): void {
    // Sort evaluations by date descending
    const sortedEvaluations = [...this.allEvaluations].sort((a, b) => {
      return new Date(b.evaluationDate).getTime() - new Date(a.evaluationDate).getTime();
    });

    // Take last 10
    this.recentActivity = sortedEvaluations.slice(0, 10).map((evaluation) => {
      const patient = this.allPatients.find((p) => p.id === evaluation.patientId);

      let outcome: 'compliant' | 'non-compliant' | 'not-eligible';
      if (!this.isInDenominator(evaluation)) {
        outcome = 'not-eligible';
      } else if (this.isInNumerator(evaluation)) {
        outcome = 'compliant';
      } else {
        outcome = 'non-compliant';
      }

      return {
        id: evaluation.id,
        date: evaluation.evaluationDate,
        patientId: evaluation.patientId,
        patientName: patient?.fullName || 'Unknown Patient',
        patientMrn: patient?.mrn,
        patientMrnAuthority: patient?.mrnAssigningAuthority,
        measureName: this.getLibraryName(evaluation),
        outcome,
      };
    });
  }

  /**
   * Calculate measure performance with trend analysis
   */
  private calculateMeasurePerformance(): void {
    const measureMap = new Map<string, {
      measureId: string;
      measureName: string;
      category: string;
      evaluations: CqlEvaluation[];
    }>();

    // Group evaluations by library (measure)
    this.allEvaluations.forEach((evaluation) => {
      const libraryId = evaluation.library?.id || 'unknown';
      const libraryName = evaluation.library?.name || 'Unknown Library';

      if (!measureMap.has(libraryId)) {
        measureMap.set(libraryId, {
          measureId: libraryId,
          measureName: libraryName,
          category: 'CQL', // Default category
          evaluations: [],
        });
      }
      measureMap.get(libraryId)?.evaluations.push(evaluation);
    });

    // Calculate performance for each measure with trend
    this.measurePerformance = Array.from(measureMap.values()).map((measure) => {
      const eligibleEvals = measure.evaluations.filter((e) => e.evaluationResult?.['InDenominator'] === true);
      const compliantEvals = eligibleEvals.filter((e) => e.evaluationResult?.['InNumerator'] === true);

      const complianceRate =
        eligibleEvals.length > 0
          ? (compliantEvals.length / eligibleEvals.length) * 100
          : 0;

      // Calculate trend by comparing recent vs older evaluations
      const trend = this.calculateMeasureTrend(measure.evaluations);

      return {
        measureId: measure.measureId,
        measureName: measure.measureName,
        category: measure.category,
        evaluationCount: measure.evaluations.length,
        complianceRate: Math.round(complianceRate * 100) / 100,
        trend,
      };
    });

    // Sort by compliance rate descending
    this.measurePerformance.sort((a, b) => b.complianceRate - a.complianceRate);

    // Transform data for ngx-charts bar chart (top 10 measures)
    this.measurePerformanceChartData = this.measurePerformance
      .slice(0, 10)
      .map(measure => ({
        name: measure.measureName.length > 30
          ? measure.measureName.substring(0, 27) + '...'
          : measure.measureName,
        value: measure.complianceRate
      }));
  }

  /**
   * Calculate compliance trends over time
   */
  private calculateComplianceTrends(): void {
    if (this.allEvaluations.length === 0) {
      this.complianceTrends = [];
      return;
    }

    // Group evaluations by time period
    const trendMap = new Map<string, CqlEvaluation[]>();

    this.allEvaluations.forEach((evaluation) => {
      const evalDate = new Date(evaluation.evaluationDate);

      // Skip invalid dates
      if (isNaN(evalDate.getTime())) {
        return;
      }

      let periodKey: string;

      if (this.trendPeriod === 'daily') {
        periodKey = evalDate.toISOString().split('T')[0];
      } else if (this.trendPeriod === 'weekly') {
        const weekStart = new Date(evalDate);
        weekStart.setDate(evalDate.getDate() - evalDate.getDay());
        periodKey = weekStart.toISOString().split('T')[0];
      } else {
        // monthly
        periodKey = `${evalDate.getFullYear()}-${String(evalDate.getMonth() + 1).padStart(2, '0')}`;
      }

      if (!trendMap.has(periodKey)) {
        trendMap.set(periodKey, []);
      }
      trendMap.get(periodKey)?.push(evaluation);
    });

    // Calculate compliance rate for each period
    this.complianceTrends = Array.from(trendMap.entries())
      .map(([period, evaluations]) => {
        const eligibleEvals = evaluations.filter((e) => e.evaluationResult?.['InDenominator'] === true);
        const compliantEvals = eligibleEvals.filter((e) => e.evaluationResult?.['InNumerator'] === true);

        const complianceRate =
          eligibleEvals.length > 0
            ? (compliantEvals.length / eligibleEvals.length) * 100
            : 0;

        return {
          period,
          complianceRate: Math.round(complianceRate * 100) / 100,
          count: evaluations.length,
        };
      })
      .sort((a, b) => a.period.localeCompare(b.period));

    // Transform data for ngx-charts line chart
    this.complianceTrendChartData = [
      {
        name: 'Compliance Rate',
        series: this.complianceTrends.map(trend => ({
          name: trend.period,
          value: trend.complianceRate
        }))
      }
    ];
  }

  /**
   * Calculate average compliance across all measures
   */
  calculateAverageCompliance(): number {
    if (this.measurePerformance.length === 0) {
      return 0;
    }

    const sum = this.measurePerformance.reduce(
      (acc, measure) => acc + measure.complianceRate,
      0
    );

    return Math.round((sum / this.measurePerformance.length) * 100) / 100;
  }

  /**
   * Calculate trend for a specific measure by comparing recent vs older evaluations
   * @param evaluations - All evaluations for a measure
   * @returns Trend direction: 'up', 'down', or 'stable'
   */
  private calculateMeasureTrend(evaluations: CqlEvaluation[]): 'up' | 'down' | 'stable' {
    if (evaluations.length < 4) {
      // Not enough data points to determine trend
      return 'stable';
    }

    // Sort evaluations by date
    const sortedEvals = [...evaluations]
      .filter(e => e.evaluationDate)
      .sort((a, b) => new Date(a.evaluationDate).getTime() - new Date(b.evaluationDate).getTime());

    if (sortedEvals.length < 4) {
      return 'stable';
    }

    // Split into two halves: older half and recent half
    const midpoint = Math.floor(sortedEvals.length / 2);
    const olderHalf = sortedEvals.slice(0, midpoint);
    const recentHalf = sortedEvals.slice(midpoint);

    // Calculate compliance rate for each half
    const olderCompliance = this.calculateComplianceForEvaluations(olderHalf);
    const recentCompliance = this.calculateComplianceForEvaluations(recentHalf);

    // Determine trend based on percentage point difference
    const difference = recentCompliance - olderCompliance;
    const threshold = 3; // 3 percentage points threshold for significance

    if (difference >= threshold) {
      return 'up';
    } else if (difference <= -threshold) {
      return 'down';
    }
    return 'stable';
  }

  /**
   * Helper to calculate compliance rate for a set of evaluations
   */
  private calculateComplianceForEvaluations(evaluations: CqlEvaluation[]): number {
    const eligible = evaluations.filter(e => e.evaluationResult?.['InDenominator'] === true);
    const compliant = eligible.filter(e => e.evaluationResult?.['InNumerator'] === true);
    return eligible.length > 0 ? (compliant.length / eligible.length) * 100 : 0;
  }

  /**
   * Refresh dashboard data
   */
  refreshData(): void {
    this.loadDashboardData();
  }

  /**
   * Navigate to a specific route
   */
  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  /**
   * Handle quick action navigation with loading states
   */
  onQuickAction(action: QuickAction): void {
    action.loading = true;
    action.success = false;

    this.router.navigate([action.route]).then(() => {
      action.loading = false;
      action.success = true;
    }).catch(() => {
      action.loading = false;
    });
  }

  /**
   * View result details
   */
  viewResultDetails(resultId: string): void {
    this.router.navigate(['/results', resultId]);
  }

  /**
   * Set trend period
   */
  setTrendPeriod(period: 'daily' | 'weekly' | 'monthly'): void {
    this.trendPeriod = period;
    this.calculateComplianceTrends();
  }

  /**
   * Set date range for trends
   */
  setDateRange(start: Date, end: Date): void {
    this.dateRange = { start, end };
    this.calculateComplianceTrends();
  }

  /**
   * Check if dashboard is empty
   */
  isEmpty(): boolean {
    return (
      this.statistics.totalEvaluations === 0 &&
      this.statistics.totalPatients === 0
    );
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  }

  /**
   * Format percentage for display
   */
  formatPercentage(value: number): string {
    return `${Math.round(value * 10) / 10}%`;
  }

  /**
   * Get badge class for outcome
   */
  getOutcomeBadgeClass(outcome: 'compliant' | 'non-compliant' | 'not-eligible'): string {
    switch (outcome) {
      case 'compliant':
        return 'badge-success';
      case 'non-compliant':
        return 'badge-warning';
      case 'not-eligible':
        return 'badge-info';
      default:
        return 'badge-default';
    }
  }

  /**
   * Get status class (for backward compatibility)
   */
  getStatusClass(result: string): string {
    return result === 'Compliant' ? 'status-success' : 'status-error';
  }

  /**
   * Format MRN assigning authority for display
   */
  formatMRNAuthority(authority?: string): string {
    if (!authority) return '';
    // Extract domain from URL (e.g., "http://hospital.example.org/patients" -> "hospital.example.org")
    try {
      const url = new URL(authority);
      return url.hostname;
    } catch {
      // If not a valid URL, return as-is
      return authority;
    }
  }

  /**
   * Calculate care gaps from evaluations
   * Identifies patients with non-compliant evaluations that need attention
   */
  private calculateCareGaps(): void {
    const careGaps: CareGapAlert[] = [];

    // Find non-compliant evaluations (in denominator but not in numerator)
    const nonCompliantEvals = this.allEvaluations.filter(
      (e) => this.isInDenominator(e) && !this.isInNumerator(e)
    );

    // Create care gap alerts for each non-compliant evaluation
    nonCompliantEvals.forEach((evaluation) => {
      const patient = this.allPatients.find((p) => p.id === evaluation.patientId);
      if (!patient) return;

      const evalDate = new Date(evaluation.evaluationDate);
      const today = new Date();
      const daysOverdue = Math.floor(
        (today.getTime() - evalDate.getTime()) / (1000 * 60 * 60 * 24)
      );

      // Determine urgency based on days overdue
      let urgency: 'high' | 'medium' | 'low' = 'low';
      if (daysOverdue > 90) urgency = 'high';
      else if (daysOverdue > 30) urgency = 'medium';

      // Determine gap type from measure name
      const measureName = this.getLibraryName(evaluation);
      let gapType: CareGapAlert['gapType'] = 'followup';
      if (measureName.toLowerCase().includes('screening')) gapType = 'screening';
      else if (measureName.toLowerCase().includes('medication') || measureName.toLowerCase().includes('statin')) gapType = 'medication';
      else if (measureName.toLowerCase().includes('lab') || measureName.toLowerCase().includes('hba1c')) gapType = 'lab';
      else if (measureName.toLowerCase().includes('assessment') || measureName.toLowerCase().includes('depression')) gapType = 'assessment';

      careGaps.push({
        patientId: patient.id,
        patientName: patient.fullName,
        mrn: patient.mrn || 'N/A',
        gapType,
        gapDescription: `${measureName} - Not compliant`,
        daysOverdue,
        urgency,
        measureName,
        dueDate: evaluation.evaluationDate,
      });
    });

    // Sort by urgency (high first) then by days overdue
    careGaps.sort((a, b) => {
      const urgencyOrder = { high: 3, medium: 2, low: 1 };
      if (urgencyOrder[a.urgency] !== urgencyOrder[b.urgency]) {
        return urgencyOrder[b.urgency] - urgencyOrder[a.urgency];
      }
      return b.daysOverdue - a.daysOverdue;
    });

    // Take top 10 for urgent care gaps (increased from 5 for better visibility)
    this.urgentCareGaps = careGaps.slice(0, 10);

    // Calculate summary
    this.careGapSummary = {
      totalGaps: careGaps.length,
      highUrgencyCount: careGaps.filter((g) => g.urgency === 'high').length,
      mediumUrgencyCount: careGaps.filter((g) => g.urgency === 'medium').length,
      lowUrgencyCount: careGaps.filter((g) => g.urgency === 'low').length,
      byType: {
        screening: careGaps.filter((g) => g.gapType === 'screening').length,
        medication: careGaps.filter((g) => g.gapType === 'medication').length,
        followup: careGaps.filter((g) => g.gapType === 'followup').length,
        lab: careGaps.filter((g) => g.gapType === 'lab').length,
        assessment: careGaps.filter((g) => g.gapType === 'assessment').length,
      },
      topAlerts: this.urgentCareGaps,
    };
  }

  /**
   * Navigate to patient list with care gap filter
   */
  viewAllCareGaps(): void {
    this.router.navigate(['/patients'], {
      queryParams: { filter: 'care-gaps', urgency: 'high' },
    });
  }

  /**
   * Navigate to specific patient with care gap
   */
  viewPatientWithCareGap(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }

  /**
   * Schedule appointment for a patient with care gap
   * Navigates to scheduling page (placeholder - would integrate with scheduling system)
   */
  scheduleAppointment(patientId: string): void {
    // For now, navigate to patient detail with scheduling intent
    this.router.navigate(['/patients', patientId], {
      queryParams: { action: 'schedule' },
    });
  }

  /**
   * Send reminder to patient about care gap
   * Navigates to patient detail with reminder intent (placeholder - would integrate with messaging system)
   */
  sendReminder(patientId: string): void {
    // For now, navigate to patient detail with reminder intent
    this.router.navigate(['/patients', patientId], {
      queryParams: { action: 'remind' },
    });
  }

  /**
   * Navigate to all evaluations
   */
  viewAllEvaluations(): void {
    this.router.navigate(['/evaluations']);
  }

  /**
   * Navigate to evaluations by status
   */
  viewEvaluationsByStatus(status: string): void {
    this.router.navigate(['/evaluations'], {
      queryParams: { status },
    });
  }

  /**
   * Navigate to all patients
   */
  viewAllPatients(): void {
    this.router.navigate(['/patients']);
  }

  /**
   * Navigate to patients by status
   */
  viewPatientsByStatus(status: string): void {
    this.router.navigate(['/patients'], {
      queryParams: { status },
    });
  }

  /**
   * Navigate to compliant patients
   */
  viewCompliantPatients(): void {
    this.router.navigate(['/patients'], {
      queryParams: { compliance: 'compliant' },
    });
  }

  /**
   * Navigate to non-compliant patients
   */
  viewNonCompliantPatients(): void {
    this.router.navigate(['/patients'], {
      queryParams: { compliance: 'non-compliant' },
    });
  }

  /**
   * Navigate to recent evaluations (last 30 days)
   */
  viewRecentEvaluations(): void {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    this.router.navigate(['/evaluations'], {
      queryParams: { startDate: thirtyDaysAgo.toISOString().split('T')[0] },
    });
  }

  /**
   * View compliance breakdown
   */
  viewComplianceBreakdown(): void {
    this.router.navigate(['/reports'], {
      queryParams: { reportType: 'compliance' },
    });
  }

  // ==================== Favorites & Recent Measures ====================

  /**
   * Navigate to evaluations page with a specific measure pre-selected
   */
  evaluateMeasure(measureId: string): void {
    this.router.navigate(['/evaluations'], {
      queryParams: { measure: measureId },
    });
  }

  /**
   * Remove a measure from favorites
   */
  removeFavorite(measureId: string, event: Event): void {
    event.stopPropagation();
    this.measureFavorites.removeFavorite(measureId);
  }

  /**
   * Clear all recent measures
   */
  clearRecentMeasures(): void {
    this.measureFavorites.clearRecent();
  }

  /**
   * Get display name for a measure, fallback to ID if not found
   */
  getMeasureDisplayName(measureId: string): string {
    const measure = this.allMeasures.find(m => m.id === measureId);
    return measure?.displayName || measureId;
  }

  /**
   * Get category for a measure
   */
  getMeasureCategory(measureId: string): string {
    const measure = this.allMeasures.find(m => m.id === measureId);
    return measure?.category || 'Quality Measure';
  }

  /**
   * Format relative time for display
   */
  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  }

  /**
   * Calculate usage percentage for the bar chart in most-used measures
   */
  getUsagePercentage(usageCount: number): number {
    const mostUsed = this.measureFavorites.getMostUsed(1);
    if (!mostUsed || mostUsed.length === 0 || !mostUsed[0].usageCount) {
      return 100;
    }
    return (usageCount / mostUsed[0].usageCount) * 100;
  }
}
