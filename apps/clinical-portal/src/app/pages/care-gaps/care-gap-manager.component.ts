import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit, signal, computed } from '@angular/core';
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
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject, Observable, forkJoin } from 'rxjs';
import { debounceTime, takeUntil, finalize } from 'rxjs/operators';

import { PatientService } from '../../services/patient.service';
import { MeasureService } from '../../services/measure.service';
import { DialogService } from '../../services/dialog.service';
import { CareGapAlert, CareGapSummary, getCareGapIcon, getUrgencyColor, formatDaysOverdue } from '../../models/care-gap.model';
import { CareGapService, CareGapApiItem, GapPriority } from '../../services/care-gap.service';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { QuickActionDialogComponent, QuickActionType, QuickActionConfig, QuickActionResult } from './dialogs/quick-action-dialog.component';
import { getQuickActionsForGap, getPrimaryQuickAction, getSecondaryQuickActions, getClosureMetrics, CLOSURE_METRICS } from './quick-actions.config';
import { LoggerService } from '../../services/logger.service';
import { CareGapStatsDashboardComponent } from '../../components/care-gap-stats-dashboard/care-gap-stats-dashboard.component';

/**
 * Intervention Recommendation with ROI metrics
 */
interface InterventionRecommendation {
  id: string;
  name: string;
  description: string;
  icon: string;
  successRate: number;
  avgCost: number;
  avgTimeToClose: number; // days
  roi: number; // multiplier
  color: string;
}

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
    MatProgressBarModule,
    MatBadgeModule,
    MatDividerModule,
    MatCheckboxModule,
    MatChipsModule,
    MatMenuModule,
    MatTabsModule,
    MatDialogModule,
    MatSnackBarModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
    CareGapStatsDashboardComponent,
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
  showPatientPanel = signal(false);
  selectedPatient = signal<{id: string; name: string; mrn: string; gaps: CareGapAlert[]} | null>(null);
  viewMode = signal<'table' | 'by-measure'>('table');

  // Computed for template (avoid arrow functions in templates)
  selectedPatientHighUrgencyCount = computed(() => {
    const patient = this.selectedPatient();
    if (!patient) return 0;
    return patient.gaps.filter(g => g.urgency === 'high').length;
  });

  // Intervention recommendations with ROI metrics
  interventionRecommendations: InterventionRecommendation[] = [
    {
      id: 'letter',
      name: 'Member Outreach Letter',
      description: 'Personalized letter with care gap information and nearby providers',
      icon: 'mail',
      successRate: 32,
      avgCost: 12,
      avgTimeToClose: 45,
      roi: 8.2,
      color: '#2196f3',
    },
    {
      id: 'provider-alert',
      name: 'Provider Alert',
      description: 'Alert primary care provider for next patient visit',
      icon: 'local_hospital',
      successRate: 48,
      avgCost: 0,
      avgTimeToClose: 30,
      roi: 999, // Infinite ROI (no cost)
      color: '#4caf50',
    },
    {
      id: 'care-coordinator',
      name: 'Care Coordinator Call',
      description: 'Personal outreach from care management team',
      icon: 'phone',
      successRate: 67,
      avgCost: 45,
      avgTimeToClose: 14,
      roi: 5.8,
      color: '#ff9800',
    },
    {
      id: 'sms',
      name: 'SMS Reminder',
      description: 'Text message with appointment scheduling link',
      icon: 'sms',
      successRate: 28,
      avgCost: 2,
      avgTimeToClose: 21,
      roi: 12.5,
      color: '#9c27b0',
    },
  ];

  // Care gaps grouped by measure
  gapsByMeasure = computed(() => {
    const grouped = new Map<string, {measureCode: string; measureName: string; gaps: CareGapAlert[]; rate: number}>();
    for (const gap of this.careGaps) {
      const key = gap.measureName;
      if (!grouped.has(key)) {
        grouped.set(key, {
          measureCode: gap.measureName.split(' ')[0],
          measureName: gap.measureName,
          gaps: [],
          rate: 0,
        });
      }
      grouped.get(key)!.gaps.push(gap);
    }
    return Array.from(grouped.values());
  });

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
  private get logger() {
    return this.loggerService.withContext('CareGapManagerComponent');
  }

  constructor(
    private patientService: PatientService,
    private measureService: MeasureService,
    private careGapService: CareGapService,
    private dialogService: DialogService,
    private dialog: MatDialog,
    private router: Router,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private loggerService: LoggerService
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

    forkJoin({
      careGapPage: this.careGapService.getCareGapsPage({ size: 200 }),
      patients: this.patientService.getPatientsSummaryCached(),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ careGapPage, patients }) => {
          this.careGaps = this.mapCareGaps(careGapPage.content, patients);
          this.calculateSummary();
          this.applyFilters();
        },
        error: () => {
          this.careGaps = [];
          this.filteredGaps = [];
          this.dataSource.data = [];
          this.error = 'Unable to load care gaps. Please try again.';
        },
      });
  }

  /**
   * Generate mock care gaps for demonstration
   * In production, this would be replaced with actual service call
   */
  private generateMockCareGaps(): void {
    const mockGaps: CareGapAlert[] = [
      // BCS - Breast Cancer Screening
      {
        patientId: '1',
        patientName: 'Anderson, Sarah',
        mrn: 'MRN001',
        gapType: 'screening',
        gapDescription: 'Mammogram overdue - Last screening 26 months ago',
        daysOverdue: 60,
        urgency: 'high',
        measureName: 'BCS - Breast Cancer Screening',
        dueDate: '2025-10-15',
        lastContactDate: '2025-09-01',
      },
      {
        patientId: '2',
        patientName: 'Martinez, Elena',
        mrn: 'MRN002',
        gapType: 'screening',
        gapDescription: 'Mammogram needed - No screening on record',
        daysOverdue: 180,
        urgency: 'high',
        measureName: 'BCS - Breast Cancer Screening',
        dueDate: '2025-06-01',
        lastContactDate: '2025-08-15',
      },
      // COL - Colorectal Cancer Screening
      {
        patientId: '3',
        patientName: 'Johnson, Robert',
        mrn: 'MRN003',
        gapType: 'screening',
        gapDescription: 'Colonoscopy overdue - Last screening 11 years ago',
        daysOverdue: 365,
        urgency: 'high',
        measureName: 'COL - Colorectal Cancer Screening',
        dueDate: '2024-12-01',
        lastContactDate: '2025-10-20',
      },
      {
        patientId: '4',
        patientName: 'Williams, James',
        mrn: 'MRN004',
        gapType: 'lab',
        gapDescription: 'FIT test needed - Alternative to colonoscopy',
        daysOverdue: 45,
        urgency: 'medium',
        measureName: 'COL - Colorectal Cancer Screening',
        dueDate: '2025-11-16',
        lastContactDate: '2025-11-01',
      },
      // CDC - Comprehensive Diabetes Care
      {
        patientId: '5',
        patientName: 'Davis, Patricia',
        mrn: 'MRN005',
        gapType: 'lab',
        gapDescription: 'HbA1c test overdue - Last test 8 months ago',
        daysOverdue: 90,
        urgency: 'high',
        measureName: 'CDC - Comprehensive Diabetes Care',
        dueDate: '2025-10-01',
        lastContactDate: '2025-09-20',
      },
      {
        patientId: '6',
        patientName: 'Thompson, Michael',
        mrn: 'MRN006',
        gapType: 'screening',
        gapDescription: 'Diabetic retinal exam needed',
        daysOverdue: 120,
        urgency: 'high',
        measureName: 'CDC - Comprehensive Diabetes Care',
        dueDate: '2025-09-01',
        lastContactDate: '2025-08-15',
      },
      {
        patientId: '7',
        patientName: 'Garcia, Maria',
        mrn: 'MRN007',
        gapType: 'lab',
        gapDescription: 'Nephropathy screening needed - Annual urine test',
        daysOverdue: 30,
        urgency: 'medium',
        measureName: 'CDC - Comprehensive Diabetes Care',
        dueDate: '2025-12-01',
        lastContactDate: '2025-11-10',
      },
      // CBP - Controlling Blood Pressure
      {
        patientId: '8',
        patientName: 'Brown, William',
        mrn: 'MRN008',
        gapType: 'followup',
        gapDescription: 'BP follow-up needed - Last reading 158/94',
        daysOverdue: 14,
        urgency: 'high',
        measureName: 'CBP - Controlling Blood Pressure',
        dueDate: '2025-12-18',
        lastContactDate: '2025-12-04',
      },
      {
        patientId: '9',
        patientName: 'Lee, Jennifer',
        mrn: 'MRN009',
        gapType: 'medication',
        gapDescription: 'Antihypertensive medication adherence gap',
        daysOverdue: 21,
        urgency: 'medium',
        measureName: 'CBP - Controlling Blood Pressure',
        dueDate: '2025-12-10',
        lastContactDate: '2025-11-25',
      },
      // SPC - Statin Therapy
      {
        patientId: '10',
        patientName: 'Miller, David',
        mrn: 'MRN010',
        gapType: 'medication',
        gapDescription: 'Statin therapy not prescribed - ASCVD diagnosis',
        daysOverdue: 60,
        urgency: 'medium',
        measureName: 'SPC - Statin Therapy',
        dueDate: '2025-11-01',
        lastContactDate: '2025-10-15',
      },
      // AWC - Adolescent Well-Care
      {
        patientId: '11',
        patientName: 'Wilson, Emily',
        mrn: 'MRN011',
        gapType: 'screening',
        gapDescription: 'Annual wellness visit overdue',
        daysOverdue: 75,
        urgency: 'low',
        measureName: 'AWC - Adolescent Well-Care',
        dueDate: '2025-10-18',
        lastContactDate: '2025-09-01',
      },
      // Patient with multiple gaps
      {
        patientId: '5',
        patientName: 'Davis, Patricia',
        mrn: 'MRN005',
        gapType: 'screening',
        gapDescription: 'Diabetic foot exam needed',
        daysOverdue: 45,
        urgency: 'medium',
        measureName: 'CDC - Comprehensive Diabetes Care',
        dueDate: '2025-11-16',
        lastContactDate: '2025-09-20',
      },
      {
        patientId: '5',
        patientName: 'Davis, Patricia',
        mrn: 'MRN005',
        gapType: 'medication',
        gapDescription: 'Statin recommended for diabetes',
        daysOverdue: 30,
        urgency: 'low',
        measureName: 'SPC - Statin Therapy',
        dueDate: '2025-12-01',
        lastContactDate: '2025-09-20',
      },
    ];

    this.careGaps = mockGaps;
  }

  private mapCareGaps(
    gaps: CareGapApiItem[],
    patients: Array<{ id: string; fullName: string; mrn?: string }>
  ): CareGapAlert[] {
    const patientById = new Map(patients.map((patient) => [patient.id, patient]));
    const now = new Date();

    return gaps.map((gap) => {
      const patient = patientById.get(gap.patientId);
      const dueDate = this.parseGapDate(gap.dueDate);
      const daysOverdue = dueDate
        ? Math.ceil((now.getTime() - dueDate.getTime()) / 86400000)
        : 0;

      return {
        gapId: gap.id,
        patientId: gap.patientId,
        patientName: patient?.fullName || gap.patientId,
        mrn: patient?.mrn || 'N/A',
        gapType: this.mapGapType(gap.gapCategory),
        gapDescription: gap.gapDescription || gap.measureName || gap.measureId,
        daysOverdue,
        urgency: this.mapUrgency(gap.priority),
        measureName: gap.measureName || gap.measureId,
        dueDate: dueDate ? dueDate.toISOString() : undefined,
      };
    });
  }

  private mapGapType(category?: string): CareGapAlert['gapType'] {
    switch (category) {
      case 'PREVENTIVE':
        return 'screening';
      case 'MEDICATION':
        return 'medication';
      case 'FOLLOW_UP':
        return 'followup';
      case 'LAB':
        return 'lab';
      case 'ASSESSMENT':
        return 'assessment';
      default:
        return 'screening';
    }
  }

  private mapUrgency(priority?: string): CareGapAlert['urgency'] {
    switch (priority) {
      case 'HIGH':
      case GapPriority.HIGH:
        return 'high';
      case 'MEDIUM':
      case GapPriority.MEDIUM:
        return 'medium';
      default:
        return 'low';
    }
  }

  private parseGapDate(value?: number[] | string): Date | null {
    if (Array.isArray(value) && value.length >= 3) {
      return new Date(value[0], value[1] - 1, value[2]);
    }
    if (typeof value === 'string') {
      const parsed = new Date(value);
      return isNaN(parsed.getTime()) ? null : parsed;
    }
    return null;
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
   * Navigate to patient detail with care gap context (Issue #239)
   * Implements context-aware navigation from care gap dashboard
   */
  viewPatientDetail(gap: CareGapAlert): void {
    const gapId = gap.gapId || `gap-${gap.patientId}-${gap.measureName}`;

    // Navigate with context parameters (Issue #155 pattern)
    this.router.navigate(['/patients', gap.patientId], {
      queryParams: {
        tab: 'care-gaps',
        careGapId: gapId,
        highlight: 'true',
        source: 'care-gap-manager'
      }
    });

    this.logger.info('Navigating to patient detail with care gap context', {
      patientId: gap.patientId,
      careGapId: gapId,
      measureName: gap.measureName
    });
  }

  /**
   * Handle row click - navigate to patient detail (Issue #239)
   */
  onRowClick(gap: CareGapAlert, event: MouseEvent): void {
    // Don't navigate if user is clicking checkbox or action buttons
    // (those have stopPropagation on their click handlers)
    this.viewPatientDetail(gap);
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
      gapId: this.selectedGap.gapId || `gap-${this.selectedGap.patientId}`,
      patientId: this.selectedGap.patientId,
      ...this.interventionForm.value,
    };

    // In production, this would call the backend service
    this.logger.info('Submitting intervention', intervention);

    // Show success message
    this.logger.info('Intervention created for patient', this.selectedGap.patientName);
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
      gapId: this.selectedGap.gapId || `gap-${this.selectedGap.patientId}`,
      patientId: this.selectedGap.patientId,
      ...this.closureForm.value,
    };

    // In production, this would call the backend service
    this.logger.info('Submitting closure', closure);

    // Show success message
    this.logger.info('Care gap closed for patient', this.selectedGap.patientName);

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
      .pipe(takeUntil(this.destroy$))
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
    this.logger.info('Bulk closing gaps', selectedGaps);

    // Remove from local list
    this.careGaps = this.careGaps.filter((gap) => !selectedGaps.includes(gap));
    this.calculateSummary();
    this.applyFilters();
    this.selection.clear();

    // Show success message
    this.logger.info('Care gaps closed successfully', `${selectedGaps.length} gap(s)`);
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

  // ============================================================================
  // Patient Panel Methods
  // ============================================================================

  /**
   * Open patient detail panel showing all care gaps for a patient
   */
  openPatientPanel(gap: CareGapAlert): void {
    const patientGaps = this.careGaps.filter(g => g.patientId === gap.patientId);
    this.selectedPatient.set({
      id: gap.patientId,
      name: gap.patientName,
      mrn: gap.mrn,
      gaps: patientGaps,
    });
    this.showPatientPanel.set(true);
  }

  /**
   * Close the patient detail panel
   */
  closePatientPanel(): void {
    this.showPatientPanel.set(false);
    this.selectedPatient.set(null);
  }

  /**
   * Get the total patient count with care gaps
   */
  getUniquePatientCount(): number {
    const patientIds = new Set(this.careGaps.map(g => g.patientId));
    return patientIds.size;
  }

  // ============================================================================
  // Outreach Campaign Methods
  // ============================================================================

  /**
   * Navigate to outreach campaigns page with selected measure
   */
  createOutreachCampaign(measureName?: string): void {
    const params: any = {};
    if (measureName) {
      const measureCode = measureName.split(' ')[0];
      const measureGaps = this.careGaps.filter(g => g.measureName === measureName);
      params.measureCode = measureCode;
      params.careGapsCount = measureGaps.length;
    } else if (this.selection.hasValue()) {
      params.careGapsCount = this.selection.selected.length;
    } else {
      params.careGapsCount = this.filteredGaps.length;
    }
    this.router.navigate(['/outreach-campaigns'], { queryParams: params });
  }

  /**
   * Generate outreach for selected gaps
   */
  generateOutreachForSelected(): void {
    if (!this.selection.hasValue()) {
      this.snackBar.open('Please select care gaps first', 'Close', { duration: 3000 });
      return;
    }
    this.createOutreachCampaign();
  }

  // ============================================================================
  // View Mode Methods
  // ============================================================================

  /**
   * Toggle between table and by-measure view
   */
  toggleViewMode(): void {
    this.viewMode.set(this.viewMode() === 'table' ? 'by-measure' : 'table');
  }

  // ============================================================================
  // Intervention Recommendation Methods
  // ============================================================================

  /**
   * Get recommended interventions for a care gap
   */
  getRecommendedInterventions(gap: CareGapAlert): InterventionRecommendation[] {
    // In a real system, this would use ML to recommend based on patient characteristics
    // For demo, we return all interventions sorted by success rate
    return [...this.interventionRecommendations].sort((a, b) => b.successRate - a.successRate);
  }

  /**
   * Apply an intervention to a care gap
   */
  applyIntervention(gap: CareGapAlert, intervention: InterventionRecommendation): void {
    this.snackBar.open(
      `${intervention.name} initiated for ${gap.patientName}`,
      'Close',
      { duration: 3000 }
    );
  }

  /**
   * Format currency for display
   */
  formatCurrency(value: number): string {
    if (value === 0) return 'Free';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  /**
   * Format ROI for display
   */
  formatROI(value: number): string {
    if (value >= 999) return '∞';
    return `${value.toFixed(1)}x`;
  }

  // ============================================================================
  // Quick Action Methods (Issue #141)
  // ============================================================================

  /**
   * Get quick actions for a care gap row
   */
  getQuickActions(gap: CareGapAlert): QuickActionConfig[] {
    return getQuickActionsForGap(gap);
  }

  /**
   * Get the primary (first) quick action for a gap
   */
  getPrimaryAction(gap: CareGapAlert): QuickActionConfig {
    return getPrimaryQuickAction(gap);
  }

  /**
   * Get secondary quick actions (for dropdown)
   */
  getSecondaryActions(gap: CareGapAlert): QuickActionConfig[] {
    return getSecondaryQuickActions(gap);
  }

  /**
   * Open quick action dialog
   */
  openQuickActionDialog(gap: CareGapAlert, actionType: QuickActionType): void {
    const dialogRef = this.dialog.open(QuickActionDialogComponent, {
      width: '550px',
      maxWidth: '95vw',
      data: {
        gap,
        actionType,
      },
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((result: QuickActionResult | null) => {
      if (result?.success) {
        this.handleQuickActionResult(gap, result);
      }
    });
  }

  /**
   * Execute primary quick action for a gap
   */
  executePrimaryAction(gap: CareGapAlert): void {
    const primaryAction = this.getPrimaryAction(gap);
    this.openQuickActionDialog(gap, primaryAction.type);
  }

  /**
   * Execute quick close action
   */
  quickCloseGap(gap: CareGapAlert): void {
    this.openQuickActionDialog(gap, 'CLOSE_GAP');
  }

  /**
   * Handle quick action result
   */
  private handleQuickActionResult(gap: CareGapAlert, result: QuickActionResult): void {
    const actionName = this.getActionDisplayName(result.actionType);

    if (result.closureRequested) {
      // Remove gap from list (closed)
      this.careGaps = this.careGaps.filter(g => g !== gap);
      this.calculateSummary();
      this.applyFilters();

      this.snackBar.open(
        `${actionName} completed - Gap closed for ${gap.patientName}`,
        'View',
        { duration: 5000 }
      );

      // Track closure time metric
      this.trackClosureMetric(gap, result);
    } else {
      this.snackBar.open(
        `${actionName} initiated for ${gap.patientName}`,
        'Close',
        { duration: 4000 }
      );
    }
  }

  /**
   * Get display name for action type
   */
  private getActionDisplayName(actionType: QuickActionType): string {
    const names: Record<QuickActionType, string> = {
      ORDER_LAB: 'Lab order',
      SCHEDULE_VISIT: 'Visit scheduled',
      SEND_REMINDER: 'Reminder sent',
      REVIEW_MEDS: 'Medication review',
      REFILL_REQUEST: 'Refill request',
      ORDER_TEST: 'Test ordered',
      SCHEDULE_PROCEDURE: 'Procedure scheduled',
      SEND_EDUCATION: 'Education sent',
      SCHEDULE_SCREENING: 'Screening scheduled',
      REFER_SPECIALIST: 'Referral sent',
      CLOSE_GAP: 'Gap closure',
    };
    return names[actionType] || 'Action';
  }

  /**
   * Track closure time metrics for analytics
   */
  private trackClosureMetric(gap: CareGapAlert, result: QuickActionResult): void {
    // In production, this would send to analytics service
    this.logger.info('Closure metric tracked', {
      gapType: gap.gapType,
      measureName: gap.measureName,
      actionType: result.actionType,
      daysOverdue: gap.daysOverdue,
      closureTimestamp: result.timestamp,
    });
  }

  /**
   * Get closure metrics for display
   */
  getClosureMetricsForAction(actionType: QuickActionType) {
    return getClosureMetrics(actionType);
  }

  /**
   * Check if gap has high closure success rate via quick action
   */
  hasHighSuccessAction(gap: CareGapAlert): boolean {
    const primary = this.getPrimaryAction(gap);
    const metrics = getClosureMetrics(primary.type);
    return metrics ? metrics.successRate >= 70 : false;
  }
}
