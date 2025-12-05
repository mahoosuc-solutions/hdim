import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
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
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject, Observable } from 'rxjs';
import { debounceTime, takeUntil, finalize } from 'rxjs/operators';

import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { DialogService } from '../../services/dialog.service';
import { CareGapAlert, CareGapSummary, getCareGapIcon, getUrgencyColor, formatDaysOverdue } from '../../models/care-gap.model';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';

/**
 * Care Gap Intervention Request
 */
interface CareGapIntervention {
  gapId: string;
  patientId: string;
  interventionType: 'call' | 'email' | 'appointment' | 'referral' | 'note';
  description: string;
  scheduledDate?: string;
  assignedTo?: string;
  notes?: string;
}

/**
 * Care Gap Closure Request
 */
interface CareGapClosure {
  gapId: string;
  patientId: string;
  closureReason: 'completed' | 'not-applicable' | 'patient-declined' | 'other';
  closureDate: string;
  notes?: string;
  documentReference?: string;
}

/**
 * Care Gap Filter Options
 */
interface CareGapFilter {
  urgency: string | null;
  gapType: string | null;
  daysOverdueMin: number | null;
  daysOverdueMax: number | null;
  patientSearch: string;
}

@Component({
  selector: 'app-care-gap-manager',
  standalone: true,
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
    MatChipsModule,
    MatMenuModule,
    MatDialogModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './care-gap-manager.component.html',
  styleUrl: './care-gap-manager.component.scss',
})
export class CareGapManagerComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Data properties
  careGaps: CareGapAlert[] = [];
  filteredGaps: CareGapAlert[] = [];
  dataSource = new MatTableDataSource<CareGapAlert>([]);
  selection = new SelectionModel<CareGapAlert>(true, []);
  summary: CareGapSummary = {
    totalGaps: 0,
    highUrgencyCount: 0,
    mediumUrgencyCount: 0,
    lowUrgencyCount: 0,
    byType: {
      screening: 0,
      medication: 0,
      followup: 0,
      lab: 0,
      assessment: 0,
    },
    topAlerts: [],
  };

  // UI state
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedGap: CareGapAlert | null = null;
  showInterventionForm = false;
  showClosureForm = false;

  // Filter form
  filterForm: FormGroup;

  // Intervention form
  interventionForm: FormGroup;

  // Closure form
  closureForm: FormGroup;

  // Table columns
  displayedColumns: string[] = [
    'select',
    'urgency',
    'patientName',
    'mrn',
    'gapType',
    'gapDescription',
    'daysOverdue',
    'measureName',
    'actions',
  ];

  // Urgency options
  urgencyOptions = [
    { value: 'high', label: 'High', color: 'warn' },
    { value: 'medium', label: 'Medium', color: 'accent' },
    { value: 'low', label: 'Low', color: 'primary' },
  ];

  // Gap type options
  gapTypeOptions = [
    { value: 'screening', label: 'Screening', icon: 'health_and_safety' },
    { value: 'medication', label: 'Medication', icon: 'medication' },
    { value: 'followup', label: 'Follow-up', icon: 'event_repeat' },
    { value: 'lab', label: 'Lab', icon: 'biotech' },
    { value: 'assessment', label: 'Assessment', icon: 'psychology' },
  ];

  // Intervention type options
  interventionTypeOptions = [
    { value: 'call', label: 'Phone Call', icon: 'call' },
    { value: 'email', label: 'Email', icon: 'email' },
    { value: 'appointment', label: 'Schedule Appointment', icon: 'event' },
    { value: 'referral', label: 'Referral', icon: 'send' },
    { value: 'note', label: 'Add Note', icon: 'note_add' },
  ];

  // Closure reason options
  closureReasonOptions = [
    { value: 'completed', label: 'Care Gap Completed' },
    { value: 'not-applicable', label: 'Not Applicable' },
    { value: 'patient-declined', label: 'Patient Declined' },
    { value: 'other', label: 'Other' },
  ];

  // Search debounce
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private patientService: PatientService,
    private measureService: MeasureService,
    private dialogService: DialogService,
    private dialog: MatDialog,
    private router: Router,
    private fb: FormBuilder
  ) {
    // Initialize filter form
    this.filterForm = this.fb.group({
      urgency: [null],
      gapType: [null],
      daysOverdueMin: [null],
      daysOverdueMax: [null],
    });

    // Initialize intervention form
    this.interventionForm = this.fb.group({
      interventionType: ['', Validators.required],
      description: ['', Validators.required],
      scheduledDate: [''],
      assignedTo: [''],
      notes: [''],
    });

    // Initialize closure form
    this.closureForm = this.fb.group({
      closureReason: ['', Validators.required],
      closureDate: [new Date().toISOString().split('T')[0], Validators.required],
      notes: [''],
      documentReference: [''],
    });
  }

  ngOnInit(): void {
    this.loadCareGaps();
    this.setupSearchDebounce();
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // Set custom sort accessor for urgency (high > medium > low)
    this.dataSource.sortingDataAccessor = (item: CareGapAlert, property: string) => {
      switch (property) {
        case 'urgency':
          return item.urgency === 'high' ? 3 : item.urgency === 'medium' ? 2 : 1;
        case 'daysOverdue':
          return item.daysOverdue;
        case 'patientName':
          return item.patientName.toLowerCase();
        case 'gapType':
          return item.gapType;
        default:
          return (item as any)[property];
      }
    };
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all care gaps from the service
   */
  loadCareGaps(): void {
    this.loading = true;
    this.error = null;

    // In a real implementation, this would call the backend service
    // For now, we'll generate mock data
    this.generateMockCareGaps();

    this.loading = false;
    this.calculateSummary();
    this.applyFilters();
  }

  /**
   * Generate mock care gaps for demonstration
   * In production, this would be replaced with actual service call
   */
  private generateMockCareGaps(): void {
    const mockGaps: CareGapAlert[] = [
      {
        patientId: '1',
        patientName: 'Smith, John',
        mrn: 'MRN001',
        gapType: 'screening',
        gapDescription: 'Annual Wellness Visit Overdue',
        daysOverdue: 45,
        urgency: 'high',
        measureName: 'HEDIS AWV',
        dueDate: '2024-10-15',
        lastContactDate: '2024-09-01',
      },
      {
        patientId: '2',
        patientName: 'Johnson, Mary',
        mrn: 'MRN002',
        gapType: 'lab',
        gapDescription: 'HbA1c Test Overdue',
        daysOverdue: 90,
        urgency: 'high',
        measureName: 'HEDIS CDC',
        dueDate: '2024-09-01',
        lastContactDate: '2024-08-15',
      },
      {
        patientId: '3',
        patientName: 'Williams, Robert',
        mrn: 'MRN003',
        gapType: 'medication',
        gapDescription: 'Statin Therapy Not Prescribed',
        daysOverdue: 30,
        urgency: 'medium',
        measureName: 'HEDIS SPC',
        dueDate: '2024-11-01',
        lastContactDate: '2024-10-20',
      },
      {
        patientId: '4',
        patientName: 'Brown, Patricia',
        mrn: 'MRN004',
        gapType: 'followup',
        gapDescription: 'Post-Discharge Follow-up Missing',
        daysOverdue: 15,
        urgency: 'medium',
        measureName: 'HEDIS FUH',
        dueDate: '2024-11-16',
        lastContactDate: '2024-11-01',
      },
      {
        patientId: '5',
        patientName: 'Davis, Michael',
        mrn: 'MRN005',
        gapType: 'assessment',
        gapDescription: 'Depression Screening Overdue',
        daysOverdue: 60,
        urgency: 'low',
        measureName: 'HEDIS DEM',
        dueDate: '2024-10-01',
        lastContactDate: '2024-09-20',
      },
    ];

    this.careGaps = mockGaps;
  }

  /**
   * Calculate summary statistics
   */
  private calculateSummary(): void {
    const highUrgency = this.careGaps.filter((g) => g.urgency === 'high').length;
    const mediumUrgency = this.careGaps.filter((g) => g.urgency === 'medium').length;
    const lowUrgency = this.careGaps.filter((g) => g.urgency === 'low').length;

    this.summary = {
      totalGaps: this.careGaps.length,
      highUrgencyCount: highUrgency,
      mediumUrgencyCount: mediumUrgency,
      lowUrgencyCount: lowUrgency,
      byType: {
        screening: this.careGaps.filter((g) => g.gapType === 'screening').length,
        medication: this.careGaps.filter((g) => g.gapType === 'medication').length,
        followup: this.careGaps.filter((g) => g.gapType === 'followup').length,
        lab: this.careGaps.filter((g) => g.gapType === 'lab').length,
        assessment: this.careGaps.filter((g) => g.gapType === 'assessment').length,
      },
      topAlerts: this.careGaps
        .filter((g) => g.urgency === 'high')
        .sort((a, b) => b.daysOverdue - a.daysOverdue)
        .slice(0, 5),
    };
  }

  /**
   * Setup search debounce
   */
  private setupSearchDebounce(): void {
    this.searchSubject
      .pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyFilters();
      });
  }

  /**
   * Handle search input change
   */
  onSearchChange(): void {
    this.searchSubject.next(this.searchTerm);
  }

  /**
   * Apply all filters
   */
  applyFilters(): void {
    let filtered = [...this.careGaps];

    // Search filter
    if (this.searchTerm) {
      const searchLower = this.searchTerm.toLowerCase();
      filtered = filtered.filter(
        (gap) =>
          gap.patientName.toLowerCase().includes(searchLower) ||
          gap.mrn.toLowerCase().includes(searchLower) ||
          gap.gapDescription.toLowerCase().includes(searchLower)
      );
    }

    // Urgency filter
    const urgency = this.filterForm.get('urgency')?.value;
    if (urgency) {
      filtered = filtered.filter((gap) => gap.urgency === urgency);
    }

    // Gap type filter
    const gapType = this.filterForm.get('gapType')?.value;
    if (gapType) {
      filtered = filtered.filter((gap) => gap.gapType === gapType);
    }

    // Days overdue filter
    const daysMin = this.filterForm.get('daysOverdueMin')?.value;
    const daysMax = this.filterForm.get('daysOverdueMax')?.value;
    if (daysMin !== null && daysMin !== undefined) {
      filtered = filtered.filter((gap) => gap.daysOverdue >= daysMin);
    }
    if (daysMax !== null && daysMax !== undefined) {
      filtered = filtered.filter((gap) => gap.daysOverdue <= daysMax);
    }

    this.filteredGaps = filtered;
    this.dataSource.data = filtered;
  }

  /**
   * Reset all filters
   */
  resetFilters(): void {
    this.searchTerm = '';
    this.filterForm.reset({
      urgency: null,
      gapType: null,
      daysOverdueMin: null,
      daysOverdueMax: null,
    });
    this.applyFilters();
  }

  /**
   * Navigate to patient detail
   */
  viewPatientDetail(gap: CareGapAlert): void {
    this.router.navigate(['/patients', gap.patientId]);
  }

  /**
   * Open intervention form for a care gap
   */
  openInterventionForm(gap: CareGapAlert): void {
    this.selectedGap = gap;
    this.showInterventionForm = true;
    this.interventionForm.reset();
  }

  /**
   * Submit intervention
   */
  submitIntervention(): void {
    if (this.interventionForm.invalid || !this.selectedGap) {
      return;
    }

    const intervention: CareGapIntervention = {
      gapId: `gap-${this.selectedGap.patientId}`,
      patientId: this.selectedGap.patientId,
      ...this.interventionForm.value,
    };

    // In production, this would call the backend service
    console.log('Submitting intervention:', intervention);

    // Show success message
    console.log(`Intervention created for ${this.selectedGap.patientName}`);
    this.closeInterventionForm();
  }

  /**
   * Close intervention form
   */
  closeInterventionForm(): void {
    this.showInterventionForm = false;
    this.selectedGap = null;
    this.interventionForm.reset();
  }

  /**
   * Open closure form for a care gap
   */
  openClosureForm(gap: CareGapAlert): void {
    this.selectedGap = gap;
    this.showClosureForm = true;
    this.closureForm.reset({
      closureReason: '',
      closureDate: new Date().toISOString().split('T')[0],
      notes: '',
      documentReference: '',
    });
  }

  /**
   * Submit care gap closure
   */
  submitClosure(): void {
    if (this.closureForm.invalid || !this.selectedGap) {
      return;
    }

    const closure: CareGapClosure = {
      gapId: `gap-${this.selectedGap.patientId}`,
      patientId: this.selectedGap.patientId,
      ...this.closureForm.value,
    };

    // In production, this would call the backend service
    console.log('Submitting closure:', closure);

    // Show success message
    console.log(`Care gap closed for ${this.selectedGap.patientName}`);

    // Remove from local list
    this.careGaps = this.careGaps.filter((g) => g !== this.selectedGap);
    this.calculateSummary();
    this.applyFilters();
    this.closeClosureForm();
  }

  /**
   * Close closure form
   */
  closeClosureForm(): void {
    this.showClosureForm = false;
    this.selectedGap = null;
    this.closureForm.reset();
  }

  /**
   * Bulk close selected care gaps
   */
  bulkCloseGaps(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedCount = this.selection.selected.length;
    this.dialogService
      .confirm(
        'Close Care Gaps',
        `Are you sure you want to close ${selectedCount} care gap(s)?`,
        'Close',
        'Cancel',
        'primary'
      )
      .subscribe((confirmed) => {
        if (confirmed) {
          this.performBulkClose();
        }
      });
  }

  /**
   * Perform bulk closure
   */
  private performBulkClose(): void {
    const selectedGaps = [...this.selection.selected];

    // In production, this would call the backend service
    console.log('Bulk closing gaps:', selectedGaps);

    // Remove from local list
    this.careGaps = this.careGaps.filter((gap) => !selectedGaps.includes(gap));
    this.calculateSummary();
    this.applyFilters();
    this.selection.clear();

    // Show success message
    console.log(`${selectedGaps.length} care gap(s) closed successfully`);
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Get icon for care gap type
   */
  getGapIcon(gapType: CareGapAlert['gapType']): string {
    return getCareGapIcon(gapType);
  }

  /**
   * Get color for urgency level
   */
  getUrgencyColor(urgency: CareGapAlert['urgency']): 'warn' | 'accent' | 'primary' {
    return getUrgencyColor(urgency);
  }

  /**
   * Format days overdue
   */
  formatDaysOverdue(days: number): string {
    return formatDaysOverdue(days);
  }

  /**
   * Get urgency badge class
   */
  getUrgencyBadgeClass(urgency: CareGapAlert['urgency']): string {
    const classMap = {
      high: 'urgency-high',
      medium: 'urgency-medium',
      low: 'urgency-low',
    };
    return classMap[urgency] || 'urgency-low';
  }

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

  // ============================================================================
  // Selection Methods
  // ============================================================================

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
  checkboxLabel(row?: CareGapAlert): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.patientId}`;
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
}
