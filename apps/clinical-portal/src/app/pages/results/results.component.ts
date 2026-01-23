import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { SelectionModel } from '@angular/cdk/collections';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { catchError, finalize, takeUntil } from 'rxjs/operators';
import { of } from 'rxjs';
import { injectDestroy } from '../../shared/utils';

import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { QrdaExportService, QrdaExportJob } from '../../services/qrda-export.service';
import { ReportExportService, ReportOptions } from '../../services/report-export.service';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { Patient } from '../../models/patient.model';
import {
  EnhancedResult,
  ResultSeverity,
  ResultActionConfig,
  ResultActionType,
  enhanceResults,
  sortBySeverity,
  getSeverityCounts,
  getSeverityClass,
  getSeverityBadgeClass,
  getSeverityText,
  getTrendClass,
  getQuickActionsForResult,
  getPrimaryResultAction,
  getSecondaryResultActions,
} from './result-enhancement.config';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoggerService } from '../../services/logger.service';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';

@Component({
  selector: 'app-results',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatMenuModule,
    MatDividerModule,
    BaseChartDirective,
    LoadingButtonComponent,
    LoadingOverlayComponent,
    StatCardComponent,
    ErrorBannerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './results.component.html',
  styleUrl: './results.component.scss',
})
export class ResultsComponent implements OnInit, AfterViewInit {
  private destroy$ = injectDestroy();
  private get logger() {
    return this.loggerService.withContext('ResultsComponent');
  }

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  filterForm: FormGroup;
  results: QualityMeasureResult[] = [];
  enhancedResults: EnhancedResult[] = [];
  filteredResults: EnhancedResult[] = [];
  dataSource = new MatTableDataSource<EnhancedResult>([]);
  selection = new SelectionModel<EnhancedResult>(true, []);
  loading = false;
  error: string | null = null;
  selectedResult: EnhancedResult | null = null;
  showDetailsPanel = false;

  // Severity filter state
  severityFilter: ResultSeverity | null = null;
  severityCounts: Record<ResultSeverity, number> = { critical: 0, high: 0, moderate: 0, normal: 0 };

  // Comparison view state
  showComparisonView = false;
  currentPage = 0;
  pageSize = 20;
  totalResults = 0;

  // Button loading states
  applyFiltersLoading = false;
  applyFiltersSuccess = false;
  exportCsvLoading = false;
  exportCsvSuccess = false;
  exportExcelLoading = false;
  exportExcelSuccess = false;
  exportPdfLoading = false;
  exportPdfSuccess = false;
  exportQrdaLoading = false;
  exportQrdaSuccess = false;

  // QRDA Export state
  qrdaExportJob: QrdaExportJob | null = null;
  qrdaExportError: string | null = null;

  // Display columns for table - enhanced with severity and trend
  displayedColumns: string[] = [
    'select',
    'severity',
    'calculationDate',
    'patientId',
    'patientContext',
    'measureName',
    'measureCategory',
    'outcome',
    'trend',
    'complianceRate',
    'quickActions',
  ];

  // Measure categories for filter
  measureCategories = ['HEDIS', 'CMS', 'CUSTOM'];

  // Status options for filter
  statusOptions = [
    { value: 'compliant', label: 'Compliant' },
    { value: 'non-compliant', label: 'Non-Compliant' },
    { value: 'not-eligible', label: 'Not Eligible' },
  ];

  // Chart data for ng2-charts
  outcomeDistributionChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [],
  };
  categoryComplianceChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [],
  };
  complianceTrendChartData: ChartData<'line'> = {
    labels: [],
    datasets: [],
  };
  measurePerformanceChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [],
  };

  outcomeDistributionChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    plugins: { legend: { position: 'bottom' } },
  };

  categoryComplianceChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: {
      x: { title: { display: true, text: 'Category' } },
      y: {
        title: { display: true, text: 'Compliance Rate (%)' },
        min: 0,
        max: 100,
      },
    },
  };

  complianceTrendChartOptions: ChartOptions<'line'> = {
    responsive: true,
    plugins: { legend: { display: true } },
    scales: {
      x: { title: { display: true, text: 'Week' } },
      y: {
        title: { display: true, text: 'Compliance Rate (%)' },
        min: 0,
        max: 100,
      },
    },
  };

  measurePerformanceChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: {
      x: { title: { display: true, text: 'Compliance Rate (%)' }, min: 0, max: 100 },
      y: { title: { display: false, text: '' } },
    },
  };

  constructor(
    private fb: FormBuilder,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private qrdaExportService: QrdaExportService,
    private reportExportService: ReportExportService,
    public aiAssistant: AIAssistantService,
    private loggerService: LoggerService
  ) {
    this.filterForm = this.fb.group({
      dateFrom: [null],
      dateTo: [null],
      measureType: [null],
      status: [null],
    });
  }

  ngOnInit(): void {
    this.loadResults();
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source after view init
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  /**
   * Load all results from the evaluation service
   */
  @TrackInteraction('results', 'load-results')
  loadResults(): void {
    this.loading = true;
    this.error = null;

    // Get all results across all patients for the tenant
    this.evaluationService
      .getAllResults(0, 1000) // Load more results for client-side pagination
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.error = 'Failed to load results. Please try again.';
          this.logger.error('Error loading results', error);
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((results) => {
        this.results = results;
        // Enhance results with severity, trend, and patient context
        this.enhancedResults = enhanceResults(results);
        // Sort by severity (critical first)
        this.enhancedResults = sortBySeverity(this.enhancedResults);
        this.filteredResults = this.enhancedResults;
        this.dataSource.data = this.enhancedResults;
        this.totalResults = this.enhancedResults.length;
        this.currentPage = 0;
        // Update severity counts
        this.severityCounts = getSeverityCounts(this.enhancedResults);
        this.updateChartData();
      });
  }

  /**
   * Apply filters to results
   */
  applyFilters(): void {
    this.applyFiltersLoading = true;
    this.applyFiltersSuccess = false;

    const { dateFrom, dateTo, measureType, status } = this.filterForm.value;

    const filtered = this.enhancedResults.filter((result) => {
      if (dateFrom) {
        const resultDate = new Date(result.calculationDate);
        const fromDate = new Date(dateFrom);
        if (resultDate < fromDate) {
          return false;
        }
      }

      if (dateTo) {
        const resultDate = new Date(result.calculationDate);
        const toDate = new Date(dateTo);
        if (resultDate > toDate) {
          return false;
        }
      }

      if (measureType && result.measureCategory !== measureType) {
        return false;
      }

      if (status) {
        if (status === 'compliant' && !result.numeratorCompliant) {
          return false;
        }
        if (
          status === 'non-compliant' &&
          (!result.denominatorEligible || result.numeratorCompliant)
        ) {
          return false;
        }
        if (status === 'not-eligible' && result.denominatorEligible) {
          return false;
        }
      }

      return true;
    });

    this.filteredResults = filtered;
    this.dataSource.data = filtered;
    this.currentPage = 0;
    this.applyFiltersLoading = false;
    this.applyFiltersSuccess = true;
    this.totalResults = this.filteredResults.length;
    this.updateChartData();
  }

  /**
   * Sort results by column and direction
   */
  sortBy(
    column: 'date' | 'measureName' | 'complianceRate',
    direction: 'asc' | 'desc'
  ): void {
    const sorted = [...this.filteredResults].sort((a, b) => {
      let comparison = 0;

      if (column === 'date') {
        const dateA = new Date(a.calculationDate).getTime();
        const dateB = new Date(b.calculationDate).getTime();
        comparison = dateA - dateB;
      } else if (column === 'measureName') {
        comparison = (a.measureName || '').localeCompare(b.measureName || '');
      } else {
        comparison = (a.complianceRate || 0) - (b.complianceRate || 0);
      }

      if (comparison === 0) {
        comparison = (a.id || '').localeCompare(b.id || '');
      }

      return direction === 'asc' ? comparison : -comparison;
    });

    this.filteredResults = sorted;
    this.dataSource.data = sorted;
  }

  /**
   * Pagination helpers
   */
  getPaginatedResults(): QualityMeasureResult[] {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredResults.slice(start, end);
  }

  nextPage(): void {
    if (this.currentPage < this.getTotalPages() - 1) {
      this.currentPage += 1;
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage -= 1;
    }
  }

  getTotalPages(): number {
    if (this.pageSize === 0) {
      return 0;
    }
    return Math.ceil(this.filteredResults.length / this.pageSize);
  }

  /**
   * Reset all filters
   */
  resetFilters(): void {
    this.filterForm.reset({
      dateFrom: null,
      dateTo: null,
      measureType: null,
      status: null,
    });
    this.filteredResults = this.enhancedResults;
    this.dataSource.data = this.enhancedResults;
    this.dataSource.filter = '';
    this.dataSource.filterPredicate = () => true;
    this.currentPage = 0;
    this.totalResults = this.filteredResults.length;
  }

  /**
   * View detailed information for a result
   */
  viewResultDetails(result: EnhancedResult): void {
    this.selectedResult = result;
    this.showDetailsPanel = true;

    // Load patient information
    const patientObservable = this.patientService.getPatient(result.patientId);
    if (patientObservable) {
      patientObservable.pipe(takeUntil(this.destroy$)).subscribe({
        next: (patient) => {
          if (this.patientService.toPatientSummary) {
            const patientSummary = this.patientService.toPatientSummary(patient);
            // Store patient summary or use as needed
          }
        },
        error: (err) => {
          this.logger.error('Error loading patient', err);
        }
      });
    }
  }

  /**
   * Close the details panel
   */
  closeDetailsPanel(): void {
    this.selectedResult = null;
    this.showDetailsPanel = false;
  }

  /**
   * Export results to CSV
   */
  @TrackInteraction('results', 'export-results')
  exportToCSV(): void {
    this.exportCsvLoading = true;
    this.exportCsvSuccess = false;

    // Simulate processing time for UX feedback
    setTimeout(() => {
      const csvData = this.generateCSVData();
      const csvContent = CSVHelper.arrayToCSV(csvData);
      const filename = `quality-results-${new Date().toISOString().split('T')[0]}.csv`;

      CSVHelper.downloadCSV(filename, csvContent);

      this.exportCsvLoading = false;
      this.exportCsvSuccess = true;
    }, 500);
  }

  /**
   * Export results to Excel (placeholder - uses CSV for now)
   */
  exportToExcel(): void {
    this.exportExcelLoading = true;
    this.exportExcelSuccess = false;

    // Simulate processing time for UX feedback
    setTimeout(() => {
      // For now, use CSV export. Can be enhanced with a library like xlsx
      const csvData = this.generateCSVData();
      const csvContent = CSVHelper.arrayToCSV(csvData);
      const filename = `quality-results-${new Date().toISOString().split('T')[0]}.xlsx`;

      CSVHelper.downloadCSV(filename, csvContent);

      this.exportExcelLoading = false;
      this.exportExcelSuccess = true;
    }, 500);
  }

  /**
   * Generate CSV data from filtered results
   */
  generateCSVData(): string[][] {
    const headers = [
      'Patient ID',
      'Measure Name',
      'Evaluation Date',
      'Outcome',
      'Compliance Rate',
    ];

    const rows = this.filteredResults.map((result) => [
      result.patientId,
      result.measureName,
      this.formatDate(result.calculationDate),
      this.getOutcomeText(result),
      `${result.complianceRate}%`,
    ]);

    return [headers, ...rows];
  }

  /**
   * Get outcome text for a result
   */
  getOutcomeText(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) {
      return 'Compliant';
    } else if (result.denominatorEligible) {
      return 'Non-Compliant';
    } else {
      return 'Not Eligible';
    }
  }

  /**
   * Get status CSS class for a result
   */
  getStatusClass(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) {
      return 'status-compliant';
    } else if (result.denominatorEligible) {
      return 'status-non-compliant';
    } else {
      return 'status-not-eligible';
    }
  }

  /**
   * Get badge class for status display
   */
  getStatusBadgeClass(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) {
      return 'badge-success';
    } else if (result.denominatorEligible) {
      return 'badge-warning';
    } else {
      return 'badge-info';
    }
  }

  /**
   * Format date string for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Format percentage for display
   */
  formatPercentage(value: number): string {
    return `${value}%`;
  }

  /**
   * Get compliance statistics
   */
  getComplianceStats(): {
    compliant: number;
    nonCompliant: number;
    notEligible: number;
    overallRate: number;
  } {
    const compliant = this.getCompliantCount();
    const nonCompliant = this.getNonCompliantCount();
    const notEligible = this.getNotEligibleCount();
    const overallRate = this.calculateOverallCompliance();

    return {
      compliant,
      nonCompliant,
      notEligible,
      overallRate,
    };
  }

  /**
   * Calculate overall compliance rate
   */
  calculateOverallCompliance(): number {
    const filteredData = this.filteredResults;
    if (filteredData.length === 0) {
      return 0;
    }
    const compliantCount = this.getCompliantCount();
    return (compliantCount / filteredData.length) * 100;
  }

  /**
   * Get count of compliant results
   */
  getCompliantCount(): number {
    return this.filteredResults.filter((r) => r.numeratorCompliant).length;
  }

  /**
   * Get count of non-compliant results
   */
  getNonCompliantCount(): number {
    return this.filteredResults.filter(
      (r) => !r.numeratorCompliant && r.denominatorEligible
    ).length;
  }

  /**
   * Get count of not eligible results
   */
  getNotEligibleCount(): number {
    return this.filteredResults.filter((r) => !r.denominatorEligible).length;
  }

  /**
   * Group results by measure type
   */
  groupByMeasureType(): Record<string, QualityMeasureResult[]> {
    return this.filteredResults.reduce(
      (acc, result) => {
        const category = result.measureCategory;
        if (!acc[category]) {
          acc[category] = [];
        }
        acc[category].push(result);
        return acc;
      },
      {} as Record<string, QualityMeasureResult[]>
    );
  }

  /**
   * Update chart data based on filtered results
   */
  updateChartData(): void {
    // Update outcome distribution pie chart
    const compliant = this.getCompliantCount();
    const nonCompliant = this.getNonCompliantCount();
    const notEligible = this.getNotEligibleCount();

    this.outcomeDistributionChartData = {
      labels: ['Compliant', 'Non-Compliant', 'Not Eligible'],
      datasets: [
        {
          data: [compliant, nonCompliant, notEligible],
          backgroundColor: ['#5AA454', '#E44D25', '#7aa3e5'],
        },
      ],
    };

    // Update category compliance bar chart
    const categoryMap = this.groupByMeasureType();
    const categoryLabels = Object.keys(categoryMap);
    const categoryRates = categoryLabels.map(category => {
      const categoryResults = categoryMap[category];
      const eligibleResults = categoryResults.filter(r => r.denominatorEligible);
      const compliantResults = eligibleResults.filter(r => r.numeratorCompliant);

      const complianceRate = eligibleResults.length > 0
        ? (compliantResults.length / eligibleResults.length) * 100
        : 0;

      return Math.round(complianceRate * 100) / 100;
    });
    this.categoryComplianceChartData = {
      labels: categoryLabels,
      datasets: [
        {
          label: 'Compliance Rate (%)',
          data: categoryRates,
          backgroundColor: ['#5AA454', '#A10A28', '#C7B42C'],
        },
      ],
    };

    // Update compliance trend line chart (weekly data)
    const trend = this.calculateWeeklyTrends();
    this.complianceTrendChartData = {
      labels: trend.labels,
      datasets: [
        {
          label: 'Compliance Rate',
          data: trend.data,
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.2)',
          tension: 0.3,
          fill: true,
        },
      ],
    };

    // Update measure performance chart (top/bottom performers)
    const performance = this.calculateMeasurePerformance();
    this.measurePerformanceChartData = {
      labels: performance.labels,
      datasets: [
        {
          label: 'Compliance Rate (%)',
          data: performance.data,
          backgroundColor: '#5AA454',
        },
      ],
    };
  }

  /**
   * Calculate weekly compliance trends for line chart
   */
  private calculateWeeklyTrends(): { labels: string[]; data: number[] } {
    if (this.filteredResults.length === 0) {
      return { labels: [], data: [] };
    }

    // Group results by week
    const weeklyData = new Map<string, { compliant: number; eligible: number }>();

    this.filteredResults.forEach(result => {
      const date = new Date(result.calculationDate);
      const weekStart = this.getWeekStart(date);
      const weekKey = weekStart.toISOString().split('T')[0];

      if (!weeklyData.has(weekKey)) {
        weeklyData.set(weekKey, { compliant: 0, eligible: 0 });
      }

      const data = weeklyData.get(weekKey)!;
      if (result.denominatorEligible) {
        data.eligible++;
        if (result.numeratorCompliant) {
          data.compliant++;
        }
      }
    });

    // Sort by date and calculate compliance rates
    const sortedWeeks = Array.from(weeklyData.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-12); // Last 12 weeks

    const labels = sortedWeeks.map(([week, data]) => {
      const complianceRate = data.eligible > 0 ? (data.compliant / data.eligible) * 100 : 0;
      const weekDate = new Date(week);
      const weekLabel = `${weekDate.getMonth() + 1}/${weekDate.getDate()}`;
      return {
        label: weekLabel,
        value: Math.round(complianceRate * 10) / 10
      };
    });

    return {
      labels: labels.map(item => item.label),
      data: labels.map(item => item.value),
    };
  }

  /**
   * Get the start of the week (Monday) for a given date
   */
  private getWeekStart(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  /**
   * Calculate measure performance for horizontal bar chart
   */
  private calculateMeasurePerformance(): { labels: string[]; data: number[] } {
    // Group by measure name
    const measureMap = new Map<string, { compliant: number; eligible: number }>();

    this.filteredResults.forEach(result => {
      const measureName = result.measureName || 'Unknown';
      if (!measureMap.has(measureName)) {
        measureMap.set(measureName, { compliant: 0, eligible: 0 });
      }

      const data = measureMap.get(measureName)!;
      if (result.denominatorEligible) {
        data.eligible++;
        if (result.numeratorCompliant) {
          data.compliant++;
        }
      }
    });

    // Calculate compliance rates and sort
    const measureRates = Array.from(measureMap.entries())
      .map(([name, data]) => ({
        name: name.length > 25 ? name.substring(0, 22) + '...' : name,
        value: data.eligible > 0 ? Math.round((data.compliant / data.eligible) * 1000) / 10 : 0,
        extra: { fullName: name, eligible: data.eligible }
      }))
      .filter(m => m.extra.eligible >= 3) // Only show measures with enough data
      .sort((a, b) => b.value - a.value)
      .slice(0, 10); // Top 10 measures

    return {
      labels: measureRates.map(rate => rate.name),
      data: measureRates.map(rate => rate.value),
    };
  }

  // ===== Row Selection Methods =====

  /**
   * Whether the number of selected elements matches the total number of rows
   */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /**
   * Selects all rows if they are not all selected; otherwise clear selection
   */
  masterToggle(): void {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.data.forEach(row => this.selection.select(row));
  }

  /**
   * The label for the checkbox on the passed row
   */
  checkboxLabel(row?: EnhancedResult): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.id}`;
  }

  /**
   * Get the count of selected rows
   */
  getSelectionCount(): number {
    return this.selection.selected.length;
  }

  /**
   * Clear all selected rows
   */
  clearSelection(): void {
    this.selection.clear();
  }

  /**
   * Export selected rows to CSV
   */
  exportSelectedToCSV(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedResults = this.selection.selected;

    // CSV header
    const headers = [
      'Evaluation Date',
      'Patient ID',
      'Measure Name',
      'Measure Category',
      'Numerator Compliant',
      'Denominator Eligible',
      'Compliance Rate',
      'Score'
    ];

    // CSV rows
    const rows = selectedResults.map(result => [
      this.formatDate(result.calculationDate),
      result.patientId,
      result.measureName,
      result.measureCategory,
      result.numeratorCompliant ? 'Yes' : 'No',
      result.denominatorEligible ? 'Yes' : 'No',
      this.formatPercentage(result.complianceRate),
      result.score.toString()
    ]);

    // Combine headers and rows
    const csvData = [headers, ...rows];
    const csvContent = CSVHelper.arrayToCSV(csvData);
    const filename = `selected-results-${new Date().toISOString().split('T')[0]}.csv`;

    CSVHelper.downloadCSV(filename, csvContent);
  }

  // ===== PDF Export Methods =====

  /**
   * Export results to PDF report.
   * Uses browser's print-to-PDF functionality.
   */
  @TrackInteraction('results', 'export-pdf')
  exportToPDF(): void {
    this.exportPdfLoading = true;
    this.exportPdfSuccess = false;

    const dateRange = this.getFilterDateRange();
    const options: ReportOptions = {
      title: 'Quality Measure Compliance Report',
      subtitle: 'Healthcare Data in Motion Platform',
      dateRange,
      includeDetails: true,
      groupByMeasure: true,
    };

    // Small delay for UX feedback
    setTimeout(() => {
      this.reportExportService.generatePDFReport(this.filteredResults, options);
      this.exportPdfLoading = false;
      this.exportPdfSuccess = true;
    }, 300);
  }

  /**
   * Export results to downloadable HTML report.
   */
  exportToHTML(): void {
    const dateRange = this.getFilterDateRange();
    const options: ReportOptions = {
      title: 'Quality Measure Compliance Report',
      subtitle: 'Healthcare Data in Motion Platform',
      dateRange,
      includeDetails: true,
    };

    this.reportExportService.downloadHTMLReport(this.filteredResults, options);
  }

  /**
   * Get date range from filter form for report.
   */
  private getFilterDateRange(): { from: Date; to: Date } | undefined {
    const { dateFrom, dateTo } = this.filterForm.value;
    if (dateFrom && dateTo) {
      return { from: new Date(dateFrom), to: new Date(dateTo) };
    }
    return undefined;
  }

  // ===== QRDA Export Methods =====

  /**
   * Export results to QRDA Category III format.
   * This is the CMS standard for aggregate quality measure reporting.
   */
  @TrackInteraction('results', 'export-qrda')
  exportToQrdaIII(): void {
    this.exportQrdaLoading = true;
    this.exportQrdaSuccess = false;
    this.qrdaExportError = null;
    this.qrdaExportJob = null;

    // Get unique measure IDs from filtered results
    const measureIds = [...new Set(this.filteredResults.map(r => r.measureName).filter(Boolean))] as string[];

    if (measureIds.length === 0) {
      this.qrdaExportError = 'No measures found in the current results to export.';
      this.exportQrdaLoading = false;
      return;
    }

    // Calculate date range from results
    const dates = this.filteredResults
      .map(r => new Date(r.calculationDate))
      .filter(d => !isNaN(d.getTime()));

    const periodStart = dates.length > 0
      ? new Date(Math.min(...dates.map(d => d.getTime()))).toISOString().split('T')[0]
      : new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0];

    const periodEnd = dates.length > 0
      ? new Date(Math.max(...dates.map(d => d.getTime()))).toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0];

    const request = {
      jobType: 'QRDA_III' as const,
      measureIds,
      periodStart,
      periodEnd,
      validateDocuments: true,
      includeSupplementalData: true,
    };

    this.qrdaExportService.generateCategoryIII(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job) => {
          this.qrdaExportJob = job;
          // Poll for completion
          this.pollQrdaExportJob(job.id);
        },
        error: (err) => {
          this.logger.error('QRDA export failed', err);
          this.qrdaExportError = err?.error?.message || 'Failed to initiate QRDA export. Please try again.';
          this.exportQrdaLoading = false;
        }
      });
  }

  /**
   * Poll QRDA export job status until completion.
   */
  private pollQrdaExportJob(jobId: string): void {
    this.qrdaExportService.pollJobUntilComplete(jobId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job) => {
          this.qrdaExportJob = job;

          if (job.status === 'COMPLETED') {
            this.exportQrdaLoading = false;
            this.exportQrdaSuccess = true;
            // Auto-download the file
            this.downloadQrdaExport(jobId);
          } else if (job.status === 'FAILED' || job.status === 'CANCELLED') {
            this.exportQrdaLoading = false;
            this.qrdaExportError = job.errorMessage || 'QRDA export failed.';
          }
        },
        error: (err) => {
          this.logger.error('Error polling QRDA job', err);
          this.exportQrdaLoading = false;
          this.qrdaExportError = 'Failed to check export status. Please try again.';
        }
      });
  }

  /**
   * Download the completed QRDA export.
   */
  downloadQrdaExport(jobId?: string): void {
    const id = jobId || this.qrdaExportJob?.id;
    if (!id) {
      return;
    }

    this.qrdaExportService.downloadDocument(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const filename = this.qrdaExportJob
            ? this.qrdaExportService.getExportFilename(this.qrdaExportJob)
            : `qrda-export-${new Date().toISOString().split('T')[0]}.xml`;
          this.qrdaExportService.triggerDownload(blob, filename);
        },
        error: (err) => {
          this.logger.error('Failed to download QRDA export', err);
          this.qrdaExportError = 'Failed to download the export file.';
        }
      });
  }

  /**
   * Cancel a running QRDA export job.
   */
  cancelQrdaExport(): void {
    if (!this.qrdaExportJob?.id) {
      return;
    }

    this.qrdaExportService.cancelJob(this.qrdaExportJob.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job) => {
          this.qrdaExportJob = job;
          this.exportQrdaLoading = false;
        },
        error: (err) => {
          this.logger.error('Failed to cancel QRDA export', err);
        }
      });
  }

  /**
   * Clear QRDA export state.
   */
  clearQrdaExportState(): void {
    this.qrdaExportJob = null;
    this.qrdaExportError = null;
    this.exportQrdaSuccess = false;
  }

  // ===== Enhanced Results Methods (Issue #145) =====

  /**
   * Get severity class for a result row
   */
  getResultSeverityClass(result: EnhancedResult): string {
    return getSeverityClass(result.severity);
  }

  /**
   * Get severity badge class
   */
  getResultSeverityBadgeClass(result: EnhancedResult): string {
    return getSeverityBadgeClass(result.severity);
  }

  /**
   * Get severity display text
   */
  getResultSeverityText(result: EnhancedResult): string {
    return getSeverityText(result.severity);
  }

  /**
   * Get trend class for styling
   */
  getResultTrendClass(result: EnhancedResult): string {
    return result.trend ? getTrendClass(result.trend) : '';
  }

  /**
   * Get trend display text
   */
  getTrendText(result: EnhancedResult): string {
    if (!result.trend) return '';
    switch (result.trend) {
      case 'improving':
        return 'Improving';
      case 'worsening':
        return 'Worsening';
      case 'stable':
        return 'Stable';
    }
  }

  /**
   * Filter results by severity level
   */
  filterBySeverity(severity: ResultSeverity | null): void {
    this.severityFilter = severity;
    if (severity === null) {
      this.filteredResults = this.enhancedResults;
    } else {
      this.filteredResults = this.enhancedResults.filter(r => r.severity === severity);
    }
    this.dataSource.data = this.filteredResults;
    this.totalResults = this.filteredResults.length;
    this.updateChartData();
  }

  /**
   * Toggle comparison view
   */
  toggleComparisonView(): void {
    this.showComparisonView = !this.showComparisonView;
  }

  /**
   * Get quick actions for a result
   */
  getResultQuickActions(result: EnhancedResult): ResultActionConfig[] {
    return getQuickActionsForResult(result);
  }

  /**
   * Get primary action for result
   */
  getResultPrimaryAction(result: EnhancedResult): ResultActionConfig {
    return getPrimaryResultAction(result);
  }

  /**
   * Get secondary actions for result
   */
  getResultSecondaryActions(result: EnhancedResult): ResultActionConfig[] {
    return getSecondaryResultActions(result);
  }

  /**
   * Execute a quick action on a result
   */
  @TrackInteraction('results', 'quick-action')
  executeResultAction(result: EnhancedResult, actionType: ResultActionType): void {
    this.logger.info(`Executing action ${actionType} on result ${result.id}`);

    // In a real implementation, this would call appropriate services
    switch (actionType) {
      case 'CONTACT_PATIENT':
        // Open contact dialog or initiate call
        this.aiAssistant.addAssistantMessage(
          `Initiating contact for patient ${result.patientId} regarding ${result.measureName} result.`
        );
        break;
      case 'ORDER_FOLLOWUP':
        // Open order dialog
        this.aiAssistant.addAssistantMessage(
          `Preparing follow-up order for ${result.measureName}.`
        );
        break;
      case 'REFER_SPECIALIST':
        // Open referral dialog
        this.aiAssistant.addAssistantMessage(
          `Preparing specialist referral for patient ${result.patientId}.`
        );
        break;
      case 'SCHEDULE_VISIT':
        // Open scheduling dialog
        this.aiAssistant.addAssistantMessage(
          `Opening scheduler for patient ${result.patientId}.`
        );
        break;
      case 'SIGN_RESULT':
        // Sign the result (would call bulk signing API)
        this.aiAssistant.addAssistantMessage(
          `Result ${result.id} has been digitally signed.`
        );
        break;
    }
  }

  /**
   * Execute primary action for a result
   */
  executePrimaryResultAction(result: EnhancedResult): void {
    const action = this.getResultPrimaryAction(result);
    this.executeResultAction(result, action.type);
  }

  /**
   * Get patient context summary for display
   */
  getPatientContextSummary(result: EnhancedResult): string {
    if (!result.patientContext) return '';
    const ctx = result.patientContext;
    return `${ctx.age}y ${ctx.gender}`;
  }

  /**
   * Get patient conditions list
   */
  getPatientConditions(result: EnhancedResult): string[] {
    return result.patientContext?.conditions || [];
  }

  /**
   * Get patient medications list
   */
  getPatientMedications(result: EnhancedResult): string[] {
    return result.patientContext?.medications || [];
  }

  /**
   * Get patient risk level
   */
  getPatientRiskLevel(result: EnhancedResult): string {
    return result.patientContext?.riskLevel || 'unknown';
  }

  /**
   * Get risk level badge class
   */
  getRiskLevelBadgeClass(result: EnhancedResult): string {
    const risk = result.patientContext?.riskLevel;
    switch (risk) {
      case 'high':
        return 'risk-badge-high';
      case 'moderate':
        return 'risk-badge-moderate';
      case 'low':
        return 'risk-badge-low';
      default:
        return 'risk-badge-unknown';
    }
  }

  /**
   * Format previous date for comparison
   */
  formatPreviousDate(result: EnhancedResult): string {
    if (!result.previousDate) return 'N/A';
    return this.formatDate(result.previousDate);
  }

  /**
   * Get comparison difference
   */
  getComparisonDiff(result: EnhancedResult): number {
    if (result.previousValue === undefined) return 0;
    return Math.round((result.complianceRate - result.previousValue) * 10) / 10;
  }

  /**
   * Format comparison difference with sign
   */
  formatComparisonDiff(result: EnhancedResult): string {
    const diff = this.getComparisonDiff(result);
    if (diff > 0) return `+${diff}%`;
    if (diff < 0) return `${diff}%`;
    return '0%';
  }

  /**
   * View result details with enhanced information
   */
  viewEnhancedResultDetails(result: EnhancedResult): void {
    this.selectedResult = result;
    this.showDetailsPanel = true;

    // Load patient information
    const patientObservable = this.patientService.getPatient(result.patientId);
    if (patientObservable) {
      patientObservable.pipe(takeUntil(this.destroy$)).subscribe({
        next: (patient) => {
          if (this.patientService.toPatientSummary) {
            const patientSummary = this.patientService.toPatientSummary(patient);
            // Update patient context with real data if available
          }
        },
        error: (err) => {
          this.logger.error('Error loading patient', err);
        }
      });
    }
  }
}
