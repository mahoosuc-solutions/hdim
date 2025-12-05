import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil, finalize } from 'rxjs/operators';

import { PatientService } from '../../services/patient.service';
import { EvaluationService } from '../../services/evaluation.service';
import { DialogService } from '../../services/dialog.service';
import { FilterPersistenceService } from '../../services/filter-persistence.service';
import { PatientDeduplicationService } from '../../services/patient-deduplication.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { PatientSummary, Patient, Address } from '../../models/patient.model';
import { PatientSummaryWithLinks, DeduplicationStatistics } from '../../models/patient-link.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { AppError, ErrorFactory } from '../../models/error.model';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';

interface PatientStatistics {
  totalPatients: number;
  activePatients: number;
  inactivePatients: number;
  averageAge: number;
  genderDistribution: {
    male: number;
    female: number;
    other: number;
  };
}

@Component({
  selector: 'app-patients',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatCardModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatDividerModule,
    MatCheckboxModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './patients.component.html',
  styleUrl: './patients.component.scss',
})
export class PatientsComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Data properties
  patients: PatientSummary[] = [];
  patientsWithLinks: PatientSummaryWithLinks[] = [];
  filteredPatients: PatientSummaryWithLinks[] = [];
  dataSource = new MatTableDataSource<PatientSummaryWithLinks>([]);
  selection = new SelectionModel<PatientSummaryWithLinks>(true, []);
  selectedPatient: PatientSummaryWithLinks | null = null;
  patientDetails: Patient | null = null;
  patientEvaluations: QualityMeasureResult[] = [];

  // MPI properties
  showMasterRecordsOnly = false;
  deduplicationStats: DeduplicationStatistics | null = null;
  detectingDuplicates = false;
  duplicateDetectionResult: string | null = null;

  // UI state
  loading = false;
  retryLoading = false;
  retrySuccess = false;
  error: string | null = null;
  errorDetails: AppError | null = null;
  showDetails = false;
  searchTerm = '';
  totalPatients = 0;
  pageSize = 10;
  currentPage = 0;
  sortColumn: string = 'fullName';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Bulk operation progress
  bulkOperationInProgress = false;
  bulkOperationProgress = 0;
  bulkOperationTotal = 0;
  bulkOperationMessage = '';

  // Filter form
  filterForm: FormGroup;

  // Table columns
  displayedColumns: string[] = [
    'select',
    'mpiStatus',
    'mrn',
    'fullName',
    'dateOfBirth',
    'age',
    'gender',
    'status',
    'actions',
  ];

  // Statistics
  statistics: PatientStatistics = {
    totalPatients: 0,
    activePatients: 0,
    inactivePatients: 0,
    averageAge: 0,
    genderDistribution: {
      male: 0,
      female: 0,
      other: 0,
    },
  };

  // Search debounce
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private patientService: PatientService,
    private evaluationService: EvaluationService,
    private dialogService: DialogService,
    private filterPersistence: FilterPersistenceService,
    private deduplicationService: PatientDeduplicationService,
    private router: Router,
    private fb: FormBuilder,
    public aiAssistant: AIAssistantService
  ) {
    this.filterForm = this.fb.group({
      gender: [null],
      status: [null],
      ageFrom: [null],
      ageTo: [null],
    });
  }

  ngOnInit(): void {
    // Load persisted filters
    this.loadPersistedFilters();
    this.loadPatients();
    this.setupSearchDebounce();

    // Save filters on change
    this.filterForm.valueChanges.pipe(
      debounceTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.saveFilters();
    });
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all patients from the service
   */
  @TrackInteraction('patients', 'load-patients')
  loadPatients(): void {
    this.loading = true;
    this.retrySuccess = false;
    this.error = null;
    this.errorDetails = null;

    this.patientService
      .getPatientsSummary()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.retryLoading = false;
        })
      )
      .subscribe({
        next: (patients) => {
          const sortedPatients = [...patients].sort((a, b) =>
            (a.fullName || '').localeCompare(b.fullName || '')
          );
          this.patients = sortedPatients;

          // Enhance with MPI information (manual detection via button click)
          this.patientsWithLinks = this.deduplicationService.enhancePatientList(sortedPatients);
          this.applyMasterRecordsFilter();
          this.updateDeduplicationStatistics();

          this.calculateStatistics();
          this.retrySuccess = true;
        },
        error: (err) => {
          this.errorDetails = ErrorFactory.createFhirServiceError('load patients', err);
          this.error = this.errorDetails.userMessage;
          console.error('[ERR-5001] Error loading patients:', err);
        },
      });
  }

  /**
   * Setup search debounce
   * OPTIMIZED: Reduced to 0ms for instant client-side filtering
   */
  private setupSearchDebounce(): void {
    this.searchSubject
      .pipe(debounceTime(0), takeUntil(this.destroy$))
      .subscribe(() => {
        this.filterPatients();
      });
  }

  /**
   * Handle search input change
   * OPTIMIZED: Instant filtering for better UX
   */
  onSearchChange(): void {
    this.searchSubject.next(this.searchTerm);
  }

  /**
   * Filter patients based on search term and filter form
   * OPTIMIZED: Client-side filtering with fuzzy search for instant results
   */
  filterPatients(): void {
    // Ensure the latest patient list is used for filtering
    this.dataSource.data = this.filteredPatients;

    const gender = this.filterForm.get('gender')?.value;
    const status = this.filterForm.get('status')?.value;
    const ageFrom = this.filterForm.get('ageFrom')?.value;
    const ageTo = this.filterForm.get('ageTo')?.value;

    // Set custom filter predicate with fuzzy matching
    this.dataSource.filterPredicate = (patient: PatientSummary) => {
      // Search by name, MRN, or date of birth (with fuzzy matching)
      if (this.searchTerm) {
        const searchLower = this.searchTerm.toLowerCase();

        // Exact match (fastest)
        const matchesName = patient.fullName.toLowerCase().includes(searchLower);
        const matchesMRN = patient.mrn && patient.mrn.toLowerCase().includes(searchLower);
        const matchesDOB = patient.dateOfBirth && patient.dateOfBirth.includes(this.searchTerm);

        if (matchesName || matchesMRN || matchesDOB) {
          return this.applyOtherFilters(patient, gender, status, ageFrom, ageTo);
        }

        // Fuzzy match (for misspellings like "Jon Doe" → "John Doe")
        const matchesFuzzy = this.fuzzyMatch(patient.fullName, this.searchTerm);
        if (matchesFuzzy) {
          return this.applyOtherFilters(patient, gender, status, ageFrom, ageTo);
        }

        return false;
      }

      // If no search term, just apply other filters
      return this.applyOtherFilters(patient, gender, status, ageFrom, ageTo);
    };

    // Trigger filtering
    this.dataSource.filter = Math.random().toString();
    this.filteredPatients = this.dataSource.filteredData;
    this.sortPatients();
    this.currentPage = 0;
  }

  /**
   * Apply non-search filters (gender, status, age)
   */
  private applyOtherFilters(
    patient: PatientSummary,
    gender: string | null,
    status: string | null,
    ageFrom: number | null,
    ageTo: number | null
  ): boolean {
    // Filter by gender
    if (gender && patient.gender !== gender) {
      return false;
    }

    // Filter by status
    if (status && patient.status !== status) {
      return false;
    }

    // Filter by age range
    if (ageFrom !== null && ageFrom !== undefined && (patient.age || 0) < ageFrom) {
      return false;
    }
    if (ageTo !== null && ageTo !== undefined && (patient.age || 0) > ageTo) {
      return false;
    }

    return true;
  }

  /**
   * Fuzzy matching algorithm for patient names
   * Handles typos like "Jon Doe" matching "John Doe"
   */
  private fuzzyMatch(text: string, query: string): boolean {
    if (!text || !query) return false;

    // Normalize strings (remove non-alphanumeric, lowercase)
    const textNorm = text.toLowerCase().replace(/[^a-z0-9]/g, '');
    const queryNorm = query.toLowerCase().replace(/[^a-z0-9]/g, '');

    // Simple contains check on normalized strings
    if (textNorm.includes(queryNorm)) {
      return true;
    }

    // Calculate Levenshtein distance for close matches
    // Only check if query is relatively long (>= 4 chars) to avoid false positives
    if (query.length >= 4) {
      const words = text.toLowerCase().split(/\s+/);
      for (const word of words) {
        const distance = this.levenshteinDistance(word, query.toLowerCase());
        // Allow 1-2 character difference for fuzzy match
        const maxDistance = Math.max(1, Math.floor(query.length * 0.25));
        if (distance <= maxDistance) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Calculate Levenshtein distance between two strings
   * Used for fuzzy matching to handle typos
   */
  private levenshteinDistance(str1: string, str2: string): number {
    const len1 = str1.length;
    const len2 = str2.length;
    const matrix: number[][] = [];

    // Initialize matrix
    for (let i = 0; i <= len1; i++) {
      matrix[i] = [i];
    }
    for (let j = 0; j <= len2; j++) {
      matrix[0][j] = j;
    }

    // Fill matrix
    for (let i = 1; i <= len1; i++) {
      for (let j = 1; j <= len2; j++) {
        if (str1[i - 1] === str2[j - 1]) {
          matrix[i][j] = matrix[i - 1][j - 1];
        } else {
          matrix[i][j] = Math.min(
            matrix[i - 1][j - 1] + 1, // substitution
            matrix[i][j - 1] + 1,     // insertion
            matrix[i - 1][j] + 1      // deletion
          );
        }
      }
    }

    return matrix[len1][len2];
  }

  /**
   * Return patients for the current page
   */
  getPaginatedPatients(): PatientSummary[] {
    const startIndex = this.currentPage * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    return this.filteredPatients.slice(startIndex, endIndex);
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
    if (this.pageSize === 0) return 0;
    return Math.ceil(this.filteredPatients.length / this.pageSize);
  }

  setPageSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  /**
   * Sort patient list based on the selected column/direction
   */
  sortPatients(): void {
    const sorted = [...this.filteredPatients].sort((a, b) => {
      let comparison = 0;

      if (this.sortColumn === 'age') {
        comparison = (a.age || 0) - (b.age || 0);
      } else if (this.sortColumn === 'dateOfBirth') {
        const dateA = new Date(a.dateOfBirth || '').getTime();
        const dateB = new Date(b.dateOfBirth || '').getTime();
        comparison = dateA - dateB;
      } else {
        comparison = (a.fullName || '').localeCompare(b.fullName || '');
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });

    this.filteredPatients = sorted;
    this.dataSource.data = sorted;
  }

  toggleSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.sortPatients();
  }

  isSortedBy(column: string): boolean {
    return this.sortColumn === column;
  }

  /**
   * Reset all filters
   */
  resetFilters(): void {
    this.searchTerm = '';
    this.filterForm.reset({
      gender: null,
      status: null,
      ageFrom: null,
      ageTo: null,
    });
    this.dataSource.filter = '';
    this.dataSource.filterPredicate = () => true;
    this.filteredPatients = this.patientsWithLinks;
    this.dataSource.data = this.filteredPatients;
    this.filterPersistence.clearFilters('patients');
  }

  /**
   * Save current filters to localStorage
   */
  private saveFilters(): void {
    const filters = {
      searchTerm: this.searchTerm,
      gender: this.filterForm.get('gender')?.value,
      status: this.filterForm.get('status')?.value,
      ageFrom: this.filterForm.get('ageFrom')?.value,
      ageTo: this.filterForm.get('ageTo')?.value,
      showMasterRecordsOnly: this.showMasterRecordsOnly,
    };
    this.filterPersistence.saveFilters('patients', filters);
  }

  /**
   * Load persisted filters from localStorage
   */
  private loadPersistedFilters(): void {
    const filters = this.filterPersistence.loadFilters('patients');
    if (filters) {
      this.searchTerm = filters['searchTerm'] || '';
      this.showMasterRecordsOnly = filters['showMasterRecordsOnly'] || false;
      this.filterForm.patchValue({
        gender: filters['gender'],
        status: filters['status'],
        ageFrom: filters['ageFrom'],
        ageTo: filters['ageTo'],
      }, { emitEvent: false });
    }
  }

  /**
   * Select a patient to view details
   */
  /**
   * Navigate to patient detail page with full FHIR data
   */
  viewPatientDetail(patient: PatientSummaryWithLinks): void {
    this.router.navigate(['/patients', patient.id]);
  }

  selectPatient(patient: PatientSummaryWithLinks): void {
    this.selectedPatient = patient;
    this.showDetails = true;
    this.loadPatientDetails(patient.id);
    this.loadPatientEvaluations(patient.id);
  }

  /**
   * Load full patient details from FHIR server
   */
  private loadPatientDetails(patientId: string): void {
    const patient$ = this.patientService.getPatient?.(patientId);
    if (!patient$ || typeof (patient$ as any).pipe !== 'function') {
      this.patientDetails = null;
      return;
    }

    patient$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (patient) => {
          this.patientDetails = patient;
        },
        error: (err) => {
          console.error('Error loading patient details:', err);
        },
      });
  }

  /**
   * Load patient evaluation history
   */
  private loadPatientEvaluations(patientId: string): void {
    const results$ = this.evaluationService.getPatientResults?.(patientId);
    if (!results$ || typeof (results$ as any).pipe !== 'function') {
      this.patientEvaluations = [];
      return;
    }

    results$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (evaluations: QualityMeasureResult[]) => {
          // Sort by date (newest first)
          this.patientEvaluations = evaluations.sort((a, b) => {
            const dateA = new Date(
              a.calculationDate || (a as any).evaluationDate || ''
            ).getTime();
            const dateB = new Date(
              b.calculationDate || (b as any).evaluationDate || ''
            ).getTime();
            return dateB - dateA;
          });
        },
        error: (err) => {
          console.error('Error loading patient evaluations:', err);
          this.patientEvaluations = [];
        },
      });
  }

  /**
   * Close patient details panel
   */
  closeDetails(): void {
    this.selectedPatient = null;
    this.showDetails = false;
    this.patientDetails = null;
    this.patientEvaluations = [];
  }

  /**
   * Calculate patient compliance rate
   */
  calculatePatientComplianceRate(): number {
    if (this.patientEvaluations.length === 0) {
      return 0;
    }

    const compliantCount = this.patientEvaluations.filter(
      (e) => e.numeratorCompliant
    ).length;
    return (compliantCount / this.patientEvaluations.length) * 100;
  }

  /**
   * Navigate to evaluation details
   */
  viewEvaluationDetails(evaluationId: string): void {
    this.router.navigate(['/results', evaluationId]);
  }


  /**
   * Navigate to new evaluation with patient pre-selected
   */
  newEvaluationForPatient(patient: PatientSummary): void {
    this.router.navigate(['/evaluations'], {
      queryParams: { patientId: patient.id },
    });
  }

  /**
   * View results filtered by patient
   */
  viewPatientResults(patient: PatientSummary): void {
    this.router.navigate(['/results'], {
      queryParams: { patientId: patient.id },
    });
  }

  /**
   * Navigate to patient detail page
   * Called when user clicks on a patient row
   */
  navigateToPatientDetail(patient: PatientSummary): void {
    // Navigate to patient detail page
    this.router.navigate(['/patient-detail', patient.id]);
  }

  /**
   * Check if can evaluate patient
   */
  canEvaluatePatient(): boolean {
    return true;
  }

  /**
   * Calculate statistics
   */
  calculateStatistics(): void {
    const total = this.patients.length;
    const active = this.patients.filter((p) => p.status === 'Active').length;
    const inactive = total - active;

    const ageSum = this.patients.reduce((sum, p) => sum + (p.age || 0), 0);
    const avgAge = total > 0 ? ageSum / total : 0;

    const male = this.patients.filter((p) => p.gender === 'male').length;
    const female = this.patients.filter((p) => p.gender === 'female').length;
    const other = total - male - female;

    this.statistics = {
      totalPatients: total,
      activePatients: active,
      inactivePatients: inactive,
      averageAge: avgAge,
      genderDistribution: {
        male,
        female,
        other,
      },
    };
    this.totalPatients = total;
  }

  /**
   * Retry loading data
   */
  retryLoad(): void {
    this.retryLoading = true;
    this.loadPatients();
  }

  /**
   * Check if list is empty
   */
  isEmpty(): boolean {
    return this.patients.length === 0;
  }

  /**
   * Get empty state message
   */
  getEmptyMessage(): string {
    return 'No patients found. Please add patients to get started.';
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Format date for display
   */
  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  }

  /**
   * Calculate age from birthdate
   */
  calculateAge(birthDate: string): number {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }

    return age;
  }

  /**
   * Get status badge class
   */
  getStatusBadgeClass(status: string): string {
    return status === 'Active' ? 'badge-success' : 'badge-secondary';
  }

  /**
   * Format phone number
   */
  formatPhoneNumber(phone: string): string {
    if (!phone) return '';

    // Remove all non-numeric characters
    const cleaned = phone.replace(/\D/g, '');

    // Format as (XXX) XXX-XXXX
    if (cleaned.length === 10) {
      return `(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}`;
    }

    return phone;
  }

  /**
   * Format address
   */
  formatAddress(address: Address): string {
    const parts: string[] = [];

    if (address.line && address.line.length > 0) {
      parts.push(...address.line);
    }

    const cityStateZip: string[] = [];
    if (address.city) cityStateZip.push(address.city);
    if (address.state) cityStateZip.push(address.state);
    if (address.postalCode) cityStateZip.push(address.postalCode);

    if (cityStateZip.length > 0) {
      parts.push(cityStateZip.join(', '));
    }

    return parts.join(', ');
  }

  /**
   * Get outcome text for evaluation
   */
  getOutcomeText(evaluation: QualityMeasureResult): string {
    if (evaluation.numeratorCompliant) {
      return 'Compliant';
    } else if (evaluation.denominatorEligible) {
      return 'Non-Compliant';
    } else {
      return 'Not Eligible';
    }
  }

  /**
   * Get outcome badge class for evaluation
   */
  getOutcomeBadgeClass(evaluation: QualityMeasureResult): string {
    if (evaluation.numeratorCompliant) {
      return 'badge-success';
    } else if (evaluation.denominatorEligible) {
      return 'badge-warning';
    } else {
      return 'badge-info';
    }
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
    this.isAllSelected()
      ? this.selection.clear()
      : this.dataSource.data.forEach((row) => this.selection.select(row));
  }

  /**
   * The label for the checkbox on the passed row
   */
  checkboxLabel(row?: PatientSummaryWithLinks): string {
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

    const selectedPatients = this.selection.selected;

    // CSV header
    const headers = [
      'MRN',
      'Full Name',
      'Date of Birth',
      'Age',
      'Gender',
      'Status',
    ];

    // CSV rows
    const rows = selectedPatients.map((patient) => [
      patient.mrn || 'N/A',
      patient.fullName,
      this.formatDate(patient.dateOfBirth),
      patient.age?.toString() || 'N/A',
      patient.gender || 'N/A',
      patient.status,
    ]);

    // Combine headers and rows
    const csvData = [headers, ...rows];
    const csvContent = CSVHelper.arrayToCSV(csvData);
    const filename = `selected-patients-${new Date().toISOString().split('T')[0]}.csv`;

    CSVHelper.downloadCSV(filename, csvContent);
  }

  /**
   * Delete selected patients with confirmation
   */
  deleteSelected(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedCount = this.selection.selected.length;
    const patientLabel =
      selectedCount === 1
        ? `"${this.selection.selected[0].fullName}"`
        : `${selectedCount} patients`;

    this.dialogService
      .confirm(
        'Delete Patients',
        `Are you sure you want to delete ${patientLabel}?<br><br>This action cannot be undone.`,
        'Delete',
        'Cancel',
        'warn'
      )
      .subscribe((confirmed) => {
        if (confirmed) {
          this.performDeleteSelected();
        }
      });
  }

  /**
   * Perform the actual deletion of selected patients
   */
  private performDeleteSelected(): void {
    const selectedPatients = [...this.selection.selected];
    let deletedCount = 0;
    let errorCount = 0;

    // Initialize progress tracking
    this.bulkOperationInProgress = true;
    this.bulkOperationProgress = 0;
    this.bulkOperationTotal = selectedPatients.length;
    this.bulkOperationMessage = 'Deleting patients...';

    // Delete each patient
    selectedPatients.forEach((patient) => {
      const deleteObservable = this.patientService.deletePatient?.(patient.id);

      if (deleteObservable && typeof (deleteObservable as any).pipe === 'function') {
        deleteObservable
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              deletedCount++;
              this.bulkOperationProgress++;
              // Remove from local arrays
              this.patients = this.patients.filter((p) => p.id !== patient.id);
              this.filteredPatients = this.filteredPatients.filter(
                (p) => p.id !== patient.id
              );
              this.dataSource.data = this.dataSource.data.filter(
                (p) => p.id !== patient.id
              );
              this.selection.deselect(patient);
              this.calculateStatistics();

              // Show success message for the last deletion
              if (deletedCount + errorCount === selectedPatients.length) {
                this.bulkOperationInProgress = false;
                this.showDeletionSummary(deletedCount, errorCount);
              }
            },
            error: (err: unknown) => {
              errorCount++;
              this.bulkOperationProgress++;
              const appError = ErrorFactory.createDataDeleteError('patient', patient.id, err);
              console.error(`[${appError.code}] Error deleting patient ${patient.fullName}:`, err);

              // Show summary after all attempts
              if (deletedCount + errorCount === selectedPatients.length) {
                this.bulkOperationInProgress = false;
                this.showDeletionSummary(deletedCount, errorCount);
              }
            },
          });
      } else {
        // If delete method doesn't exist, just remove from local state
        this.patients = this.patients.filter((p) => p.id !== patient.id);
        this.filteredPatients = this.filteredPatients.filter(
          (p) => p.id !== patient.id
        );
        this.dataSource.data = this.dataSource.data.filter(
          (p) => p.id !== patient.id
        );
        this.selection.deselect(patient);
        deletedCount++;
      }
    });

    // If no delete method exists, show summary immediately
    if (!this.patientService.deletePatient) {
      this.calculateStatistics();
      this.showDeletionSummary(deletedCount, errorCount);
    }

    // Clear selection
    this.selection.clear();
  }

  /**
   * Show deletion summary message
   */
  private showDeletionSummary(deletedCount: number, errorCount: number): void {
    if (errorCount === 0) {
      console.log(
        `Successfully deleted ${deletedCount} patient${deletedCount !== 1 ? 's' : ''}`
      );
    } else if (deletedCount === 0) {
      this.errorDetails = ErrorFactory.createDataDeleteError('patients', 'bulk', {
        attempted: deletedCount + errorCount,
        failed: errorCount
      });
      this.error = `Unable to delete selected patients. They may be in use or you may not have permission. Error code: ${this.errorDetails.code}`;
    } else {
      console.log(
        `Deleted ${deletedCount} patient${deletedCount !== 1 ? 's' : ''}, but ${errorCount} failed. Some patients may be in use or you may not have permission.`
      );
    }
  }

  /**
   * Toggle master records only filter
   */
  toggleMasterRecordsOnly(): void {
    this.applyMasterRecordsFilter();
  }

  /**
   * Apply master records filter
   */
  private applyMasterRecordsFilter(): void {
    if (this.showMasterRecordsOnly) {
      this.filteredPatients = this.deduplicationService.filterMasterRecordsOnly(this.patientsWithLinks);
    } else {
      this.filteredPatients = this.patientsWithLinks;
    }
    this.dataSource.data = this.filteredPatients;
    this.filterPatients();  // Reapply other filters
  }

  /**
   * Update deduplication statistics
   */
  private updateDeduplicationStatistics(): void {
    this.deduplicationService.getStatistics(this.patientsWithLinks).subscribe({
      next: (stats) => {
        this.deduplicationStats = stats;
      },
      error: (err) => {
        console.error('Error calculating deduplication statistics:', err);
      }
    });
  }

  /**
   * Auto-detect and link duplicate patient records
   */
  autoDetectDuplicates(): void {
    this.detectingDuplicates = true;
    this.duplicateDetectionResult = null;

    console.log(`Starting duplicate detection for ${this.patients.length} patients...`);

    this.deduplicationService.autoDetectAndLinkDuplicates(this.patients).subscribe({
      next: (result) => {
        this.detectingDuplicates = false;

        console.log('Detection result:', result);

        // Refresh the enhanced patient list
        this.patientsWithLinks = this.deduplicationService.enhancePatientList(this.patients);

        // Apply master records filter if enabled
        this.applyMasterRecordsFilter();

        // Update statistics
        this.updateDeduplicationStatistics();

        console.log('Updated stats:', this.deduplicationStats);
        console.log('Patients with links:', this.patientsWithLinks.filter(p => p.isMaster || p.masterPatientId).length);

        // Show result message
        if (result.duplicatesLinked > 0) {
          this.duplicateDetectionResult = `✓ Success! Linked ${result.duplicatesLinked} duplicate(s) to ${result.mastersCreated} master record(s). Look for green "Master" and orange "Duplicate" badges below.`;
        } else {
          this.duplicateDetectionResult = 'No high-confidence duplicates detected (85%+ match score required)';
        }

        // Clear message after 10 seconds (longer so user can read it)
        setTimeout(() => {
          this.duplicateDetectionResult = null;
        }, 10000);
      },
      error: (err) => {
        this.detectingDuplicates = false;
        this.duplicateDetectionResult = 'Error detecting duplicates';
        console.error('Error detecting duplicates:', err);

        setTimeout(() => {
          this.duplicateDetectionResult = null;
        }, 10000);
      }
    });
  }

  /**
   * Clear all duplicate links (for testing/reset)
   */
  clearDuplicateLinks(): void {
    this.deduplicationService.clearAllLinks();

    // Refresh the enhanced patient list
    this.patientsWithLinks = this.deduplicationService.enhancePatientList(this.patients);

    // Apply master records filter if enabled
    this.applyMasterRecordsFilter();

    // Update statistics
    this.updateDeduplicationStatistics();

    this.duplicateDetectionResult = 'All duplicate links cleared';
    setTimeout(() => {
      this.duplicateDetectionResult = null;
    }, 3000);
  }
}
