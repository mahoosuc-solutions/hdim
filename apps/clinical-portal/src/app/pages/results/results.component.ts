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
import { NgxChartsModule } from '@swimlane/ngx-charts';
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
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
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
    NgxChartsModule,
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

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  filterForm: FormGroup;
  results: QualityMeasureResult[] = [];
  filteredResults: QualityMeasureResult[] = [];
  dataSource = new MatTableDataSource<QualityMeasureResult>([]);
  selection = new SelectionModel<QualityMeasureResult>(true, []);
  loading = false;
  error: string | null = null;
  selectedResult: QualityMeasureResult | null = null;
  showDetailsPanel = false;
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

  // Display columns for table
  displayedColumns: string[] = [
    'select',
    'calculationDate',
    'patientId',
    'measureName',
    'measureCategory',
    'outcome',
    'complianceRate',
    'actions',
  ];

  // Measure categories for filter
  measureCategories = ['HEDIS', 'CMS', 'CUSTOM'];

  // Status options for filter
  statusOptions = [
    { value: 'compliant', label: 'Compliant' },
    { value: 'non-compliant', label: 'Non-Compliant' },
    { value: 'not-eligible', label: 'Not Eligible' },
  ];

  // Chart data for ngx-charts
  outcomeDistributionChartData: any[] = [];
  categoryComplianceChartData: any[] = [];
  complianceTrendChartData: any[] = [];
  measurePerformanceChartData: any[] = [];

  // Pie chart configuration
  pieChartView: [number, number] = [400, 300];
  pieChartShowLabels = true;
  pieChartIsDoughnut = true;
  pieChartLegend = true;
  pieChartColorScheme: any = {
    domain: ['#5AA454', '#E44D25', '#7aa3e5']
  };

  // Bar chart configuration
  barChartView: [number, number] = [500, 300];
  barChartShowXAxis = true;
  barChartShowYAxis = true;
  barChartGradient = false;
  barChartShowLegend = false;
  barChartShowXAxisLabel = true;
  barChartXAxisLabel = 'Category';
  barChartShowYAxisLabel = true;
  barChartYAxisLabel = 'Compliance Rate (%)';
  barChartColorScheme: any = {
    domain: ['#5AA454', '#A10A28', '#C7B42C']
  };

  // Line chart configuration (compliance trends)
  lineChartView: [number, number] = [800, 300];
  lineChartShowXAxis = true;
  lineChartShowYAxis = true;
  lineChartGradient = true;
  lineChartShowLegend = true;
  lineChartShowXAxisLabel = true;
  lineChartXAxisLabel = 'Week';
  lineChartShowYAxisLabel = true;
  lineChartYAxisLabel = 'Compliance Rate (%)';
  lineChartColorScheme: any = {
    domain: ['#1976d2', '#4caf50', '#ff9800']
  };
  lineChartCurve = 'curveMonotoneX';

  // Horizontal bar chart for measure performance
  horizontalBarView: [number, number] = [400, 350];
  horizontalBarColorScheme: any = {
    domain: ['#5AA454', '#E44D25', '#CFC0BB', '#7aa3e5', '#a8385d']
  };

  constructor(
    private fb: FormBuilder,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private qrdaExportService: QrdaExportService,
    private reportExportService: ReportExportService,
    public aiAssistant: AIAssistantService
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
          console.error('Error loading results:', error);
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((results) => {
        this.results = results;
        this.filteredResults = results;
        this.dataSource.data = results;
        this.totalResults = results.length;
        this.currentPage = 0;
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

    const filtered = this.results.filter((result) => {
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
    this.filteredResults = this.results;
    this.dataSource.data = this.results;
    this.dataSource.filter = '';
    this.dataSource.filterPredicate = () => true;
    this.currentPage = 0;
    this.totalResults = this.filteredResults.length;
  }

  /**
   * View detailed information for a result
   */
  viewResultDetails(result: QualityMeasureResult): void {
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
          console.error('Error loading patient:', err);
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

    this.outcomeDistributionChartData = [
      { name: 'Compliant', value: compliant },
      { name: 'Non-Compliant', value: nonCompliant },
      { name: 'Not Eligible', value: notEligible }
    ];

    // Update category compliance bar chart
    const categoryMap = this.groupByMeasureType();
    this.categoryComplianceChartData = Object.keys(categoryMap).map(category => {
      const categoryResults = categoryMap[category];
      const eligibleResults = categoryResults.filter(r => r.denominatorEligible);
      const compliantResults = eligibleResults.filter(r => r.numeratorCompliant);

      const complianceRate = eligibleResults.length > 0
        ? (compliantResults.length / eligibleResults.length) * 100
        : 0;

      return {
        name: category,
        value: Math.round(complianceRate * 100) / 100
      };
    });

    // Update compliance trend line chart (weekly data)
    this.complianceTrendChartData = this.calculateWeeklyTrends();

    // Update measure performance chart (top/bottom performers)
    this.measurePerformanceChartData = this.calculateMeasurePerformance();
  }

  /**
   * Calculate weekly compliance trends for line chart
   */
  private calculateWeeklyTrends(): any[] {
    if (this.filteredResults.length === 0) {
      return [];
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

    const series = sortedWeeks.map(([week, data]) => {
      const complianceRate = data.eligible > 0 ? (data.compliant / data.eligible) * 100 : 0;
      const weekDate = new Date(week);
      const weekLabel = `${weekDate.getMonth() + 1}/${weekDate.getDate()}`;
      return {
        name: weekLabel,
        value: Math.round(complianceRate * 10) / 10
      };
    });

    return [{ name: 'Compliance Rate', series }];
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
  private calculateMeasurePerformance(): any[] {
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

    return measureRates;
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
  checkboxLabel(row?: QualityMeasureResult): string {
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
          console.error('QRDA export failed:', err);
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
          console.error('Error polling QRDA job:', err);
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
          console.error('Failed to download QRDA export:', err);
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
          console.error('Failed to cancel QRDA export:', err);
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
}
