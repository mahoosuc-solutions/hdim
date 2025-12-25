import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SelectionModel } from '@angular/cdk/collections';
import { Observable, of } from 'rxjs';
import { map, startWith, catchError, takeUntil } from 'rxjs/operators';
import { injectDestroy } from '../../shared/utils';

import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { DialogService } from '../../services/dialog.service';
import { FilterPersistenceService } from '../../services/filter-persistence.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { MeasureFavoritesService } from '../../services/measure-favorites.service';
import { MeasureInfo, MeasureCategory } from '../../models/cql-library.model';
import { PatientSummary } from '../../models/patient.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { AppError, ErrorFactory } from '../../models/error.model';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';

@Component({
  selector: 'app-evaluations',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatTooltipModule,
    ErrorBannerComponent,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './evaluations.component.html',
  styleUrl: './evaluations.component.scss',
})
export class EvaluationsComponent implements OnInit, AfterViewInit {
  private destroy$ = injectDestroy();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  evaluationForm: FormGroup;

  // Available measures
  measures: MeasureInfo[] = [];
  allMeasures: MeasureInfo[] = []; // Store all measures for filtering
  loadingMeasures = false;
  measuresError: string | null = null;
  measuresErrorDetails: AppError | null = null;

  // Category filtering
  selectedCategory: string = '';
  measureSearchTerm: string = '';
  measureCategories: { value: string; label: string }[] = [
    { value: '', label: 'All Categories' },
    { value: 'PREVENTIVE', label: 'Preventive Care' },
    { value: 'CHRONIC_DISEASE', label: 'Chronic Disease' },
    { value: 'BEHAVIORAL_HEALTH', label: 'Behavioral Health' },
    { value: 'MEDICATION', label: 'Medication Management' },
    { value: 'WOMENS_HEALTH', label: "Women's Health" },
    { value: 'CHILD_ADOLESCENT', label: 'Child & Adolescent' },
    { value: 'SDOH', label: 'Social Determinants' },
    { value: 'UTILIZATION', label: 'Utilization' },
    { value: 'CARE_COORDINATION', label: 'Care Coordination' },
    { value: 'OVERUSE', label: 'Overuse/Appropriateness' },
    { value: 'CUSTOM', label: 'Custom Measures' },
  ];

  // Available patients
  patients: PatientSummary[] = [];
  filteredPatients: Observable<PatientSummary[]> = of([]);
  loadingPatients = false;
  patientsError: string | null = null;
  patientsErrorDetails: AppError | null = null;

  // Selected patient
  selectedPatient: PatientSummary | null = null;

  // Evaluation state
  submitting = false;
  evaluationResult: QualityMeasureResult | null = null;
  evaluationError: string | null = null;
  evaluationErrorDetails: AppError | null = null;

  // Bulk operation progress
  bulkOperationInProgress = false;
  bulkOperationProgress = 0;
  bulkOperationTotal = 0;

  // Table data and selection
  evaluations: QualityMeasureResult[] = [];
  dataSource = new MatTableDataSource<QualityMeasureResult>([]);
  selection = new SelectionModel<QualityMeasureResult>(true, []);
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

  constructor(
    private fb: FormBuilder,
    private measureService: MeasureService,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private dialogService: DialogService,
    private filterPersistence: FilterPersistenceService,
    public aiAssistant: AIAssistantService,
    public measureFavorites: MeasureFavoritesService
  ) {
    this.evaluationForm = this.fb.group({
      measureId: ['', Validators.required],
      patientSearch: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadActiveMeasures();
    this.loadPatients();
    this.setupPatientAutocomplete();
    this.loadEvaluations();
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source after view init
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  /**
   * Load available HEDIS measures from backend registry
   * Uses the new /evaluate/measures endpoint for discovery
   */
  loadActiveMeasures(): void {
    this.loadingMeasures = true;
    this.measuresError = null;
    this.measuresErrorDetails = null;

    // Use getAllAvailableMeasures() which fetches from the HEDIS registry
    this.measureService.getAllAvailableMeasures().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.measuresErrorDetails = ErrorFactory.createCqlEngineError('load measures', error);
        this.measuresError = this.measuresErrorDetails.userMessage;
        console.error('[ERR-5002] Error loading HEDIS measures:', error);
        // Fallback to active library measures if HEDIS registry fails
        return this.measureService.getActiveMeasuresInfo().pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe((measures) => {
      this.allMeasures = measures;
      this.measures = measures;
      this.loadingMeasures = false;
    });
  }

  /**
   * Filter measures by category
   */
  onCategoryChange(category: string): void {
    this.selectedCategory = category;
    this.filterMeasures();
  }

  /**
   * Filter measures by search term
   */
  onMeasureSearchChange(searchTerm: string): void {
    this.measureSearchTerm = searchTerm;
    this.filterMeasures();
  }

  /**
   * Clear measure search
   */
  clearMeasureSearch(): void {
    this.measureSearchTerm = '';
    this.filterMeasures();
  }

  /**
   * Apply all measure filters (category + search)
   */
  private filterMeasures(): void {
    let filtered = [...this.allMeasures];

    // Apply category filter
    if (this.selectedCategory) {
      filtered = filtered.filter(m => m.category === this.selectedCategory);
    }

    // Apply search filter
    if (this.measureSearchTerm) {
      const searchLower = this.measureSearchTerm.toLowerCase();
      filtered = filtered.filter(m =>
        m.name.toLowerCase().includes(searchLower) ||
        m.displayName.toLowerCase().includes(searchLower) ||
        (m.description && m.description.toLowerCase().includes(searchLower))
      );
    }

    this.measures = filtered;

    // Clear measure selection if current selection is not in filtered list
    const currentMeasureId = this.evaluationForm.get('measureId')?.value;
    if (currentMeasureId && !this.measures.some(m => m.name === currentMeasureId)) {
      this.evaluationForm.patchValue({ measureId: '' });
    }
  }

  /**
   * Get count of measures in a category (respects search filter)
   */
  getCategoryCount(category: string): number {
    if (!this.measureSearchTerm) {
      if (!category) return this.allMeasures.length;
      return this.allMeasures.filter(m => m.category === category).length;
    }
    // When there's a search term, show counts for matching measures only
    const searchLower = this.measureSearchTerm.toLowerCase();
    const matchingMeasures = this.allMeasures.filter(m =>
      m.name.toLowerCase().includes(searchLower) ||
      m.displayName.toLowerCase().includes(searchLower) ||
      (m.description && m.description.toLowerCase().includes(searchLower))
    );
    if (!category) return matchingMeasures.length;
    return matchingMeasures.filter(m => m.category === category).length;
  }

  /**
   * Get total filtered count for display
   */
  getFilteredMeasureCount(): number {
    return this.measures.length;
  }

  /**
   * Clear all measure filters (search and category)
   */
  clearAllMeasureFilters(): void {
    this.measureSearchTerm = '';
    this.selectedCategory = '';
    this.filterMeasures();
  }

  /**
   * Load patients from FHIR server
   */
  loadPatients(): void {
    this.loadingPatients = true;
    this.patientsError = null;
    this.patientsErrorDetails = null;

    this.patientService.getPatientsSummary().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.patientsErrorDetails = ErrorFactory.createFhirServiceError('load patients', error);
        this.patientsError = this.patientsErrorDetails.userMessage;
        console.error('[ERR-5001] Error loading patients:', error);
        return of([]);
      })
    ).subscribe((patients) => {
      this.patients = patients;
      this.loadingPatients = false;
    });
  }

  /**
   * Setup autocomplete for patient search
   */
  setupPatientAutocomplete(): void {
    this.filteredPatients = this.evaluationForm.get('patientSearch')!.valueChanges.pipe(
      startWith(''),
      map((value) => {
        const searchValue = typeof value === 'string' ? value : value?.fullName || '';
        return this._filterPatients(searchValue);
      })
    );
  }

  /**
   * Filter patients based on search string
   */
  private _filterPatients(value: string): PatientSummary[] {
    if (!value) {
      return this.patients.slice(0, 10); // Show first 10 by default
    }

    const filterValue = value.toLowerCase();
    return this.patients.filter((patient) =>
      patient.fullName.toLowerCase().includes(filterValue) ||
      patient.mrn?.toLowerCase().includes(filterValue)
    ).slice(0, 10); // Limit results to 10
  }

  /**
   * Display function for patient autocomplete
   */
  displayPatient(patient: PatientSummary | null): string {
    if (!patient) return '';
    return patient.mrn ? `${patient.fullName} (MRN: ${patient.mrn})` : patient.fullName;
  }

  /**
   * Handle patient selection from autocomplete
   */
  onPatientSelected(patient: PatientSummary): void {
    this.selectedPatient = patient;
  }

  /**
   * Submit evaluation
   */
  @TrackInteraction('evaluations', 'submit-evaluation')
  submitEvaluation(): void {
    if (this.evaluationForm.invalid || !this.selectedPatient) {
      return;
    }

    this.submitting = true;
    this.evaluationResult = null;
    this.evaluationError = null;

    const measureId = this.evaluationForm.value.measureId;
    const patientId = this.selectedPatient.id;

    // Record measure usage for recent tracking
    const selectedMeasure = this.allMeasures.find(m => m.name === measureId);
    if (selectedMeasure) {
      this.measureFavorites.recordUsage(selectedMeasure);
    }

    // Use Quality Measure Service to calculate measure
    this.evaluationService.calculateQualityMeasure(patientId, measureId).pipe(
      takeUntil(this.destroy$),
      catchError((error: any) => {
        this.evaluationErrorDetails = ErrorFactory.createEvaluationError(patientId, measureId, error);
        this.evaluationError = this.evaluationErrorDetails.userMessage;
        console.error(`[${this.evaluationErrorDetails.code}] Evaluation error:`, error);
        this.submitting = false;
        return of(null);
      })
    ).subscribe((result) => {
      if (result) {
        this.evaluationResult = result;
      }
      this.submitting = false;
    });
  }

  /**
   * Toggle favorite status for a measure
   */
  toggleFavorite(measure: MeasureInfo, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.measureFavorites.toggleFavorite(measure);
  }

  /**
   * Check if a measure is favorited
   */
  isFavorite(measureId: string): boolean {
    return this.measureFavorites.isFavorite(measureId);
  }

  /**
   * Get favorite measures from the full measures list
   */
  getFavoriteMeasures(): MeasureInfo[] {
    const favoriteIds = this.measureFavorites.getFavoriteIds();
    return this.allMeasures.filter(m => favoriteIds.includes(m.id));
  }

  /**
   * Get recent measures from the full measures list
   */
  getRecentMeasures(): MeasureInfo[] {
    const recentIds = this.measureFavorites.getRecentIds();
    return recentIds
      .map(id => this.allMeasures.find(m => m.id === id))
      .filter((m): m is MeasureInfo => m !== undefined)
      .slice(0, 5);
  }

  /**
   * Quick select a measure (from favorites or recent)
   */
  quickSelectMeasure(measure: MeasureInfo): void {
    this.evaluationForm.patchValue({ measureId: measure.name });
  }

  /**
   * Reset form
   */
  resetForm(): void {
    this.evaluationForm.reset();
    this.selectedPatient = null;
    this.evaluationResult = null;
    this.evaluationError = null;
  }

  /**
   * Enable/disable a form control without firing valueChanges
   */
  private setControlEnabled(controlName: string, enabled: boolean): void {
    const control = this.evaluationForm.get(controlName);
    if (!control) return;

    if (enabled) {
      control.enable({ emitEvent: false });
    } else {
      control.disable({ emitEvent: false });
    }
  }

  /**
   * Get status class for result
   */
  getStatusClass(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) {
      return 'status-success';
    } else if (result.denominatorEligible) {
      return 'status-warning';
    } else {
      return 'status-info';
    }
  }

  /**
   * Get status text for result
   */
  getStatusText(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) {
      return 'Compliant';
    } else if (result.denominatorEligible) {
      return 'Non-Compliant (Eligible)';
    } else {
      return 'Not Eligible';
    }
  }

  /**
   * Load all evaluations for the current tenant
   */
  @TrackInteraction('evaluations', 'load-evaluations')
  loadEvaluations(): void {
    this.evaluationService.getAllResults(0, 1000).pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        console.error('Error loading evaluations:', error);
        return of([]);
      })
    ).subscribe((results) => {
      this.evaluations = results;
      this.dataSource.data = results;
    });
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Get outcome text for display
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

    const selectedEvaluations = this.selection.selected;

    // CSV header
    const headers = [
      'Evaluation Date',
      'Patient ID',
      'Measure Name',
      'Measure Category',
      'Outcome',
      'Compliance Rate',
      'Score'
    ];

    // CSV rows
    const rows = selectedEvaluations.map(evaluation => [
      this.formatDate(evaluation.calculationDate),
      evaluation.patientId,
      evaluation.measureName,
      evaluation.measureCategory,
      this.getOutcomeText(evaluation),
      `${evaluation.complianceRate}%`,
      evaluation.score.toString()
    ]);

    // Combine headers and rows
    const csvData = [headers, ...rows];
    const csvContent = CSVHelper.arrayToCSV(csvData);
    const filename = `selected-evaluations-${new Date().toISOString().split('T')[0]}.csv`;

    CSVHelper.downloadCSV(filename, csvContent);
  }

  /**
   * Delete selected evaluations with confirmation
   */
  deleteSelected(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedCount = this.selection.selected.length;
    const evaluationLabel = selectedCount === 1
      ? 'this evaluation'
      : `${selectedCount} evaluations`;

    this.dialogService.confirm(
      'Delete Evaluations',
      `Are you sure you want to delete ${evaluationLabel}?<br><br>This action cannot be undone.`,
      'Delete',
      'Cancel',
      'warn'
    ).pipe(takeUntil(this.destroy$)).subscribe((confirmed) => {
      if (confirmed) {
        this.performDeleteSelected();
      }
    });
  }

  /**
   * Perform the actual deletion of selected evaluations
   */
  private performDeleteSelected(): void {
    const selectedEvaluations = [...this.selection.selected];
    let deletedCount = 0;

    // Initialize progress tracking
    this.bulkOperationInProgress = true;
    this.bulkOperationProgress = 0;
    this.bulkOperationTotal = selectedEvaluations.length;

    selectedEvaluations.forEach((evaluation) => {
      // Remove from local arrays
      this.evaluations = this.evaluations.filter(e => e.id !== evaluation.id);
      this.dataSource.data = this.dataSource.data.filter(e => e.id !== evaluation.id);
      this.selection.deselect(evaluation);
      deletedCount++;
      this.bulkOperationProgress++;
    });

    this.bulkOperationInProgress = false;
  }
}
