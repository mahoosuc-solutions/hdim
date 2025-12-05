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
import { SelectionModel } from '@angular/cdk/collections';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
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

  constructor(
    private fb: FormBuilder,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
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
      patientObservable.subscribe(
        (patient) => {
          if (this.patientService.toPatientSummary) {
            const patientSummary = this.patientService.toPatientSummary(patient);
            // Store patient summary or use as needed
          }
        },
        (error) => {
          console.error('Error loading patient:', error);
        }
      );
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
}
