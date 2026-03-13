import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, takeUntil, finalize, forkJoin, catchError, of } from 'rxjs';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { LoggerService } from '../../services/logger.service';
import { StarRatingsService } from './star-ratings.service';
import {
  StarRatingResponse,
  StarRatingTrendResponse,
  StarMeasureSummary,
  SimulatedGapClosure,
  StarRatingSimulationRequest,
  getStarColor,
  getStarTier,
} from './star-ratings.model';

@Component({
  selector: 'app-star-ratings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatTableModule,
    MatSortModule,
    MatSelectModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    BaseChartDirective,
    StatCardComponent,
    PageHeaderComponent,
    DecimalPipe,
    DatePipe,
    PercentPipe,
  ],
  templateUrl: './star-ratings.component.html',
  styleUrl: './star-ratings.component.scss',
})
export class StarRatingsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger;

  // State
  loading = true;
  error: string | null = null;
  currentRating: StarRatingResponse | null = null;
  trendData: StarRatingTrendResponse | null = null;

  // Trend config
  trendWeeks = 12;
  trendGranularity: 'WEEKLY' | 'MONTHLY' = 'WEEKLY';

  // Measure table
  displayedColumns = ['measureCode', 'measureName', 'domain', 'performanceRate', 'stars', 'gapCount'];
  sortedMeasures: StarMeasureSummary[] = [];
  domainFilter = 'ALL';

  // Simulation
  simulationClosures: SimulatedGapClosure[] = [{ gapCode: '', closures: 1 }];
  simulatedRating: StarRatingResponse | null = null;
  simulating = false;

  // Trend chart (Chart.js)
  trendChartData: ChartData<'line'> = { labels: [], datasets: [] };
  trendChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.parsed.y.toFixed(2)} ★`,
        },
      },
    },
    scales: {
      y: {
        min: 1,
        max: 5,
        ticks: { stepSize: 0.5 },
        title: { display: true, text: 'Star Rating' },
      },
      x: {
        title: { display: true, text: 'Date' },
      },
    },
  };

  constructor(
    private starRatingsService: StarRatingsService,
    private router: Router,
    loggerService: LoggerService,
  ) {
    this.logger = loggerService.withContext('StarRatingsComponent');
  }

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Data Loading ──────────────────────────────────────────────

  loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      current: this.starRatingsService.getCurrentRating().pipe(
        catchError((err) => {
          this.logger.error('Failed to load current rating', err);
          return of(null);
        })
      ),
      trend: this.starRatingsService.getTrend(this.trendWeeks, this.trendGranularity).pipe(
        catchError((err) => {
          this.logger.error('Failed to load trend data', err);
          return of(null);
        })
      ),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loading = false))
      )
      .subscribe(({ current, trend }) => {
        if (current) {
          this.currentRating = current;
          this.sortedMeasures = [...current.measures];
        } else {
          this.error = 'Unable to load star rating data. The care-gap-event-service may not be running.';
        }
        if (trend) {
          this.trendData = trend;
          this.buildTrendChart(trend);
        }
      });
  }

  refreshTrend(): void {
    this.starRatingsService
      .getTrend(this.trendWeeks, this.trendGranularity)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (trend) => {
          this.trendData = trend;
          this.buildTrendChart(trend);
        },
        error: (err) => this.logger.error('Failed to refresh trend', err),
      });
  }

  // ── Trend Chart ───────────────────────────────────────────────

  private buildTrendChart(trend: StarRatingTrendResponse): void {
    const labels = trend.points.map((p) => p.snapshotDate);
    const data = trend.points.map((p) => p.overallRating);

    this.trendChartData = {
      labels,
      datasets: [
        {
          label: 'Overall Rating',
          data,
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          borderWidth: 2,
          tension: 0.3,
          fill: true,
          pointRadius: 4,
          pointBackgroundColor: data.map((v) => getStarColor(v)),
        },
        {
          label: 'Quality Bonus Threshold',
          data: data.map(() => 4.0),
          borderColor: '#388E3C',
          borderWidth: 1,
          borderDash: [8, 4],
          pointRadius: 0,
          fill: false,
        },
      ],
    };
  }

  // ── Measure Table ─────────────────────────────────────────────

  get filteredMeasures(): StarMeasureSummary[] {
    if (this.domainFilter === 'ALL') return this.sortedMeasures;
    return this.sortedMeasures.filter((m) => m.domain === this.domainFilter);
  }

  get uniqueDomains(): string[] {
    if (!this.currentRating) return [];
    const domains = new Set(this.currentRating.measures.map((m) => m.domain));
    return Array.from(domains).sort();
  }

  sortMeasures(sort: Sort): void {
    if (!this.currentRating) return;
    const data = [...this.currentRating.measures];

    if (!sort.active || sort.direction === '') {
      this.sortedMeasures = data;
      return;
    }

    this.sortedMeasures = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'measureCode': return compare(a.measureCode, b.measureCode, isAsc);
        case 'measureName': return compare(a.measureName, b.measureName, isAsc);
        case 'domain': return compare(a.domain, b.domain, isAsc);
        case 'performanceRate': return compare(a.performanceRate, b.performanceRate, isAsc);
        case 'stars': return compare(a.stars, b.stars, isAsc);
        case 'gapCount': return compare(a.denominator - a.numerator, b.denominator - b.numerator, isAsc);
        default: return 0;
      }
    });
  }

  getStarArray(rating: number): boolean[] {
    return Array(5).fill(false).map((_, i) => i < rating);
  }

  getMeasureGapCount(m: StarMeasureSummary): number {
    return m.denominator - m.numerator;
  }

  // ── Simulation ────────────────────────────────────────────────

  addSimulationRow(): void {
    this.simulationClosures.push({ gapCode: '', closures: 1 });
  }

  removeSimulationRow(index: number): void {
    if (this.simulationClosures.length > 1) {
      this.simulationClosures.splice(index, 1);
    }
  }

  get availableMeasureCodes(): string[] {
    return this.currentRating?.measures.map((m) => m.measureCode) ?? [];
  }

  runSimulation(): void {
    const validClosures = this.simulationClosures.filter(
      (c) => c.gapCode && c.closures > 0
    );
    if (validClosures.length === 0) return;

    this.simulating = true;
    this.simulatedRating = null;

    const request: StarRatingSimulationRequest = { closures: validClosures };

    this.starRatingsService
      .simulate(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.simulating = false))
      )
      .subscribe({
        next: (result) => {
          this.simulatedRating = result;
          this.logger.info('Simulation complete', {
            current: this.currentRating?.overallRating,
            simulated: result.overallRating,
          });
        },
        error: (err) => this.logger.error('Simulation failed', err),
      });
  }

  get simulationDelta(): number | null {
    if (!this.currentRating || !this.simulatedRating) return null;
    return this.simulatedRating.overallRating - this.currentRating.overallRating;
  }

  // ── Navigation ────────────────────────────────────────────────

  navigateToCareGaps(measureCode?: string): void {
    this.router.navigate(['/care-gaps'], {
      queryParams: measureCode ? { filter: measureCode } : undefined,
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  getStarColor(rating: number): string {
    return getStarColor(rating);
  }

  getStarTier(rating: number): string {
    return getStarTier(rating);
  }

  exportMeasureCsv(): void {
    if (!this.currentRating) return;
    const headers = 'Measure Code,Measure Name,Domain,Numerator,Denominator,Performance Rate,Stars,Open Gaps\n';
    const rows = this.filteredMeasures
      .map(
        (m) =>
          `${m.measureCode},${m.measureName},${m.domain},${m.numerator},${m.denominator},${(m.performanceRate * 100).toFixed(1)}%,${m.stars},${m.denominator - m.numerator}`
      )
      .join('\n');

    const blob = new Blob([headers + rows], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `star-ratings-measures-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}

function compare(a: string | number, b: string | number, isAsc: boolean): number {
  return (a < b ? -1 : a > b ? 1 : 0) * (isAsc ? 1 : -1);
}
