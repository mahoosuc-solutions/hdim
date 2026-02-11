import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';

import { Subject, Observable } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Phase2ExecutionService } from '../../../services/phase2-execution.service';
import { LoggerService } from '../../../services/logger.service';

/**
 * Phase 2 Dashboard Summary Component
 *
 * Displays overall execution status, completion percentage, and high-level metrics
 * grouped by category and week. Also displays financial ROI metrics including
 * quality bonus captured, care gaps closed, and measure-specific ROI breakdown.
 */
@Component({
  selector: 'app-phase2-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatGridListModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatButtonModule,
  ],
  templateUrl: './phase2-dashboard.component.html',
  styleUrls: ['./phase2-dashboard.component.scss'],
})
export class Phase2DashboardComponent implements OnInit, OnDestroy {
  @Input() dashboard: any;
  @Output() categoryClick = new EventEmitter<string>();
  @Output() weekClick = new EventEmitter<number>();

  private destroy$ = new Subject<void>();
  private logger: any;

  // Financial Metrics
  financialMetrics: any;
  totalBonusCaptured = 0;
  totalGapsClosed = 0;
  averageROI = 0;
  measureROIData: any[] = [];

  // Table columns
  displayedColumns: string[] = ['measure', 'captured', 'gaps', 'tasks'];

  categories = [
    { id: 'PRODUCT', label: 'Product & Engineering', icon: 'code' },
    { id: 'SALES', label: 'Sales & Business Dev', icon: 'trending_up' },
    { id: 'MARKETING', label: 'Marketing & Thought Leadership', icon: 'campaign' },
    { id: 'LEADERSHIP', label: 'Executive & Strategy', icon: 'person' },
  ];

  weeks = [
    { id: 1, label: 'Week 1-2: Positioning Refinement' },
    { id: 2, label: 'Week 3-4: Pilot Acquisition' },
  ];

  constructor(
    private phase2Service: Phase2ExecutionService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('Phase2DashboardComponent');
  }

  /**
   * Initialize component and load financial data
   */
  ngOnInit(): void {
    this.logger.info('Initializing Phase 2 Dashboard with financial metrics');
    this.loadFinancialDashboard();
    this.loadMeasureROI();
  }

  /**
   * Clean up subscriptions
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load financial dashboard metrics
   */
  private loadFinancialDashboard(): void {
    this.phase2Service
      .getFinancialDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.financialMetrics = data;
          this.totalBonusCaptured = data.totalBonusCaptured || 0;
          this.totalGapsClosed = data.totalGapsClosed || 0;
          this.averageROI = data.averageROI || 0;
          this.logger.info('Financial dashboard loaded', {
            captured: this.totalBonusCaptured,
            gaps: this.totalGapsClosed,
            roi: this.averageROI,
          });
        },
        error: (err) => {
          this.logger.error('Failed to load financial dashboard', err);
          this.totalBonusCaptured = 0;
          this.totalGapsClosed = 0;
          this.averageROI = 0;
        },
      });
  }

  /**
   * Load measure-specific ROI data
   */
  private loadMeasureROI(): void {
    this.phase2Service
      .getMeasureROI()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.measureROIData = data || [];
          this.logger.info('Measure ROI data loaded', { count: data.length });
        },
        error: (err) => {
          this.logger.error('Failed to load measure ROI', err);
          this.measureROIData = [];
        },
      });
  }

  /**
   * Format currency values with USD symbol
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  /**
   * Format percentage values with 2 decimal places
   */
  formatPercentage(value: number): string {
    return `${value.toFixed(2)}%`;
  }

  /**
   * Get published case studies
   */
  getCaseStudies(published: boolean = true): Observable<any[]> {
    return this.phase2Service.getCaseStudies(published);
  }

  /**
   * Publish a case study
   */
  publishCaseStudy(caseStudyId: string): Observable<any> {
    return this.phase2Service.publishCaseStudy(caseStudyId);
  }

  /**
   * Handle category click
   */
  onCategoryClick(categoryId: string): void {
    this.categoryClick.emit(categoryId);
  }

  /**
   * Handle week click
   */
  onWeekClick(week: number): void {
    this.weekClick.emit(week);
  }

  /**
   * Get category task count
   */
  getCategoryTaskCount(categoryId: string): number {
    return this.dashboard?.tasksByCategory?.[categoryId] ?? 0;
  }

  /**
   * Get week task count
   */
  getWeekTaskCount(week: number): number {
    return this.dashboard?.tasksByWeek?.[week] ?? 0;
  }

  /**
   * Calculate progress color
   */
  getProgressColor(percentage: number): string {
    if (percentage >= 75) return '#4CAF50'; // Green
    if (percentage >= 50) return '#2196F3'; // Blue
    if (percentage >= 25) return '#FF9800'; // Orange
    return '#F44336'; // Red
  }

  /**
   * Format status display
   */
  formatStatus(count: number, label: string): string {
    return `${count} ${label}`;
  }
}
