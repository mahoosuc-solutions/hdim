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
import { QualityMeasureResult, LocalMeasureResult } from '../../models/quality-result.model';
import { EvaluationDefaultPreset } from '../../models/evaluation.model';
import { ToastService } from '../../services/toast.service';
import { AppError, ErrorFactory } from '../../models/error.model';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';
import { LoggerService } from '../../services/logger.service';
import { EvaluationDataFlowComponent, DataFlowStep } from '../../components/evaluation-data-flow/evaluation-data-flow.component';
import { EvaluationDataFlowService } from '../../services/evaluation-data-flow.service';

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
    EvaluationDataFlowComponent,
  ],
  templateUrl: './evaluations.component.html',
  styleUrl: './evaluations.component.scss',
})
export class EvaluationsComponent implements OnInit, AfterViewInit {
  private destroy$ = injectDestroy();
  private get logger() {
    return this.loggerService.withContext('EvaluationsComponent');
  }

  private defaultPresetApplied = false;

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
  localEvaluationResult: LocalMeasureResult | null = null;
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

  // Data flow visualization
  showDataFlow = false;
  currentEvaluationId?: string;
  dataFlowSteps: DataFlowStep[] = [];

  defaultPreset: EvaluationDefaultPreset | null = null;
  lastPresetRefreshAt: Date | null = null;

  constructor(
    private fb: FormBuilder,
    private measureService: MeasureService,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private dialogService: DialogService,
    private filterPersistence: FilterPersistenceService,
    public aiAssistant: AIAssistantService,
    public measureFavorites: MeasureFavoritesService,
    private dataFlowService: EvaluationDataFlowService,
    private toastService: ToastService,
    private loggerService: LoggerService
  ) {
    this.evaluationForm = this.fb.group({
      measureId: ['', Validators.required],
      patientSearch: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadDefaultPreset();
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
   * Primary: Uses /quality-measure/measures/local for locally-calculated measures
   * Fallback: Uses /evaluate/measures for HEDIS registry
   */
  loadActiveMeasures(): void {
    this.loadingMeasures = true;
    this.measuresError = null;
    this.measuresErrorDetails = null;

    // Primary: Load locally-calculable measures from quality-measure-service
    this.measureService.getLocalMeasuresAsInfo().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.logger.warn('[WARN] Local measures unavailable, falling back to HEDIS registry', error);
        // Fallback to HEDIS registry measures
        return this.measureService.getAllAvailableMeasures().pipe(
          catchError((hedisError) => {
            this.measuresErrorDetails = ErrorFactory.createCqlEngineError('load measures', hedisError);
            this.measuresError = this.measuresErrorDetails.userMessage;
            this.logger.error('[ERR-5002] Error loading measures', hedisError);
            return of([]);
          })
        );
      })
    ).subscribe((measures) => {
      this.allMeasures = measures;
      this.measures = measures;
      this.loadingMeasures = false;
      this.logger.info(`[INFO] Loaded ${measures.length} measures`, measures.map(m => m.name).join(', '));
      this.applyDefaultPresetIfReady();
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
        this.logger.error('[ERR-5001] Error loading patients', error);
        return of([]);
      })
    ).subscribe((patients) => {
      this.patients = patients;
      this.loadingPatients = false;
      this.applyDefaultPresetIfReady();
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

  private loadDefaultPreset(): void {
    this.fetchDefaultPreset(false);
  }

  private applyDefaultPresetIfReady(): void {
    if (this.defaultPresetApplied || !this.defaultPreset) {
      return;
    }
    if (this.loadingMeasures || this.loadingPatients) {
      return;
    }
    const presetMeasure = this.allMeasures.find(m => m.name === this.defaultPreset?.measureId);
    const presetPatient = this.patients.find(p => p.id === this.defaultPreset?.patientId);
    if (presetMeasure) {
      this.evaluationForm.patchValue({ measureId: presetMeasure.name });
    }
    if (presetPatient) {
      this.selectedPatient = presetPatient;
      this.evaluationForm.patchValue({ patientSearch: presetPatient });
    }
    this.defaultPresetApplied = true;
  }

  saveDefaultPreset(): void {
    if (this.evaluationForm.invalid || !this.selectedPatient) {
      return;
    }
    const measureId = this.evaluationForm.value.measureId as string;
    const patientId = this.selectedPatient.id;
    this.evaluationService.saveDefaultEvaluationPreset({
      measureId,
      patientId,
      useCqlEngine: false,
    }).pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.logger.warn('[WARN] Unable to save default evaluation preset', error);
        return of(null);
      })
    ).subscribe((preset) => {
      if (!preset) {
        return;
      }
      this.defaultPreset = preset;
      this.defaultPresetApplied = true;
    });
  }

  clearDefaultPreset(): void {
    this.evaluationService.clearDefaultEvaluationPreset().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.logger.warn('[WARN] Unable to clear default evaluation preset', error);
        return of(null);
      })
    ).subscribe(() => {
      this.defaultPreset = null;
      this.defaultPresetApplied = false;
      this.toastService.info('Default preset cleared');
    });
  }

  reloadDefaultPreset(): void {
    this.fetchDefaultPreset(true);
  }

  runDefaultPreset(): void {
    const defaultPreset = this.defaultPreset;
    if (!defaultPreset) {
      return;
    }
    const presetMeasure = this.allMeasures.find(m => m.name === defaultPreset.measureId);
    const presetPatient = this.patients.find(p => p.id === defaultPreset.patientId);
    if (!presetMeasure || !presetPatient) {
      this.evaluationError = 'Default preset could not be matched to current data. Please save it again.';
      this.evaluationErrorDetails = null;
      return;
    }
    this.evaluationForm.patchValue({ measureId: presetMeasure.name, patientSearch: presetPatient });
    this.selectedPatient = presetPatient;
    this.submitEvaluation(Boolean(defaultPreset.useCqlEngine));
  }

  getDefaultPresetSummary(): string {
    const defaultPreset = this.defaultPreset;
    if (!defaultPreset) return '';
    const presetMeasure = this.allMeasures.find(m => m.name === defaultPreset.measureId);
    const presetPatient = this.patients.find(p => p.id === defaultPreset.patientId);
    const measureLabel = presetMeasure?.displayName || defaultPreset.measureId;
    const patientLabel = presetPatient?.fullName || defaultPreset.patientId;
    return `${measureLabel} · ${patientLabel}`;
  }

  getDefaultPresetStatus(): string {
    if (this.loadingMeasures || this.loadingPatients) {
      return 'Checking...';
    }
    const defaultPreset = this.defaultPreset;
    if (!defaultPreset) {
      return 'Not configured';
    }
    const presetMeasure = this.allMeasures.find(m => m.name === defaultPreset.measureId);
    const presetPatient = this.patients.find(p => p.id === defaultPreset.patientId);
    if (!presetMeasure || !presetPatient) {
      return 'Needs attention';
    }
    return 'Ready';
  }

  getDefaultPresetStatusClass(): string {
    const status = this.getDefaultPresetStatus();
    if (status === 'Ready') {
      return 'status-ready';
    }
    if (status === 'Needs attention') {
      return 'status-warning';
    }
    if (status === 'Checking...') {
      return 'status-loading';
    }
    return 'status-missing';
  }

  getDefaultPresetStatusDetail(): string {
    if (this.loadingMeasures || this.loadingPatients) {
      return 'Loading measures and patients to verify the default preset.';
    }
    const defaultPreset = this.defaultPreset;
    if (!defaultPreset) {
      return 'Save a default preset to enable one-click evaluations.';
    }
    const presetMeasure = this.allMeasures.find(m => m.name === defaultPreset.measureId);
    const presetPatient = this.patients.find(p => p.id === defaultPreset.patientId);
    if (!presetMeasure || !presetPatient) {
      return 'The saved preset no longer matches current measures or patients. Save a new preset.';
    }
    return 'Preset measure and patient are available.';
  }

  private fetchDefaultPreset(showToast: boolean): void {
    this.defaultPresetApplied = false;
    this.evaluationService.getDefaultEvaluationPreset().pipe(
      takeUntil(this.destroy$),
      catchError((error) => {
        this.logger.warn('[WARN] Unable to load default evaluation preset', error);
        if (showToast) {
          this.toastService.error('Failed to reload default preset');
        }
        return of(null);
      })
    ).subscribe((preset) => {
      this.defaultPreset = preset;
      this.lastPresetRefreshAt = new Date();
      this.applyDefaultPresetIfReady();
      if (showToast) {
        if (preset) {
          this.toastService.success('Default preset refreshed');
        } else {
          this.toastService.warning('No default preset found');
        }
      }
    });
  }

  /**
   * Submit evaluation with data flow visualization
   * Option 1: Use CQL evaluation endpoint (shows data flow)
   * Option 2: Use local calculation endpoint (faster, no data flow)
   */
  @TrackInteraction('evaluations', 'submit-evaluation')
  submitEvaluation(useCqlEngine: boolean = false): void {
    if (this.evaluationForm.invalid || !this.selectedPatient) {
      return;
    }

    this.submitting = true;
    this.evaluationResult = null;
    this.localEvaluationResult = null;
    this.evaluationError = null;

    const measureId = this.evaluationForm.value.measureId;
    const patientId = this.selectedPatient.id;

    // Record measure usage for recent tracking
    const selectedMeasure = this.allMeasures.find(m => m.name === measureId);
    if (selectedMeasure) {
      this.measureFavorites.recordUsage(selectedMeasure);
    }

    if (useCqlEngine) {
      // Use CQL Engine endpoint (enables data flow tracking)
      this.submitCqlEvaluation(measureId, patientId);
    } else {
      // Use local calculation endpoint (bypasses CQL Engine, uses Java MeasureRegistry)
      this.submitLocalEvaluation(measureId, patientId);
    }
  }

  /**
   * Submit evaluation using CQL Engine (with data flow tracking)
   */
  private submitCqlEvaluation(measureId: string, patientId: string): void {
    // Find library ID for the measure
    const selectedMeasure = this.allMeasures.find(m => m.name === measureId);
    if (!selectedMeasure || !selectedMeasure.id) {
      this.evaluationError = 'Measure library not found';
      this.submitting = false;
      return;
    }

    // Show data flow visualization
    this.showDataFlow = true;
    this.dataFlowSteps = [];
    this.currentEvaluationId = undefined;

    // Connect to WebSocket for real-time updates
    // Note: We'll get the evaluation ID after creation
    this.evaluationService.submitEvaluation(selectedMeasure.id, patientId).pipe(
      takeUntil(this.destroy$),
      catchError((error: any) => {
        this.evaluationErrorDetails = ErrorFactory.createEvaluationError(patientId, measureId, error);
        this.evaluationError = this.evaluationErrorDetails.userMessage;
        this.logger.error(`[${this.evaluationErrorDetails.code}] CQL evaluation error`, error);
        this.submitting = false;
        this.showDataFlow = false;
        return of(null);
      })
    ).subscribe((evaluation) => {
      if (evaluation) {
        this.currentEvaluationId = evaluation.id;
        this.evaluationResult = this.mapEvaluationToResult(evaluation);

        // Connect WebSocket for this evaluation
        this.dataFlowService.connect(evaluation.id).pipe(
          takeUntil(this.destroy$)
        ).subscribe((step) => {
          this.dataFlowSteps.push(step);
        });

        this.logger.info('[INFO] CQL evaluation result', evaluation);
      }
      this.submitting = false;
    });
  }

  /**
   * Submit evaluation using local calculation (no data flow tracking)
   */
  private submitLocalEvaluation(measureId: string, patientId: string): void {
    this.showDataFlow = false;
    
    this.evaluationService.calculateLocalMeasure(patientId, measureId).pipe(
      takeUntil(this.destroy$),
      catchError((error: any) => {
        this.evaluationErrorDetails = ErrorFactory.createEvaluationError(patientId, measureId, error);
        this.evaluationError = this.evaluationErrorDetails.userMessage;
        this.logger.error(`[${this.evaluationErrorDetails.code}] Local evaluation error`, error);
        this.submitting = false;
        return of(null);
      })
    ).subscribe((result) => {
      if (result) {
        this.localEvaluationResult = result;
        this.logger.info('[INFO] Local evaluation result', result);
      }
      this.submitting = false;
    });
  }

  /**
   * Map CQL evaluation to quality measure result format
   */
  private mapEvaluationToResult(evaluation: any): QualityMeasureResult {
    // Parse evaluation result JSON
    let resultData: any = {};
    try {
      if (evaluation.evaluationResult) {
        // evaluationResult might be a string or already an object
        if (typeof evaluation.evaluationResult === 'string') {
          resultData = JSON.parse(evaluation.evaluationResult);
        } else {
          resultData = evaluation.evaluationResult;
        }
      }
    } catch (e) {
      this.logger.warn('Failed to parse evaluation result', e);
    }

    const libraryName = evaluation.library?.name || evaluation.library?.libraryName || '';

    const now = new Date().toISOString();
    return {
      id: evaluation.id,
      tenantId: evaluation.tenantId || '',
      patientId: evaluation.patientId,
      measureId: libraryName,
      measureName: libraryName,
      measureCategory: 'CUSTOM' as const,
      measureYear: new Date().getFullYear(),
      calculationDate: evaluation.evaluationDate || now,
      numeratorCompliant: resultData.inNumerator || false,
      denominatorEligible: resultData.inDenominator || false,
      complianceRate: resultData.complianceRate || 0,
      score: resultData.complianceRate || 0,
      createdAt: evaluation.evaluationDate || now,
      createdBy: evaluation.createdBy || '',
      version: 1,
    } as QualityMeasureResult;
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
    this.localEvaluationResult = null;
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
        this.logger.error('Error loading evaluations', error);
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

  /**
   * Get sub-measure keys for local evaluation result
   */
  getSubMeasureKeys(): string[] {
    if (this.localEvaluationResult?.subMeasures) {
      return Object.keys(this.localEvaluationResult.subMeasures);
    }
    return [];
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
