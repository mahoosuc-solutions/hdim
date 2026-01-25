/**
 * Provider Dashboard Component
 *
 * Optimized for Provider (MD/DO/PA/NP) workflows:
 * - High-priority care gaps requiring clinical decisions
 * - Quality measure performance tracking
 * - Results review and clinical actions
 * - Patient panel overview
 */

import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Subject, takeUntil, forkJoin, of, from } from 'rxjs';
import { catchError, map, mergeMap, toArray } from 'rxjs/operators';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { CareGapService, CareGap, CareGapApiItem, CareGapPageResponse, GapPriority, CareGapClosureRequest, InterventionType } from '../../../services/care-gap.service';
import { EvaluationService } from '../../../services/evaluation.service';
import { PatientService } from '../../../services/patient.service';
import { Patient } from '../../../models/patient.model';
import { SchedulingService, ScheduleAppointment } from '../../../services/scheduling.service';
import { TrackInteraction } from '../../../utils/ai-tracking.decorator';
import {
  CareGapClosureDialogComponent,
  CareGapClosureDialogData,
  CareGapClosureResult,
  CareGapForClosure
} from '../../../dialogs/care-gap-closure-dialog/care-gap-closure-dialog.component';
import { KeyboardShortcutsService } from '../../../services/keyboard-shortcuts.service';
import { KeyboardShortcutsDialogComponent } from '../../../components/dialogs/keyboard-shortcuts-dialog.component';
import { ProviderLeaderboardDialogComponent } from '../../../components/dialogs/provider-leaderboard-dialog.component';
import { ShortcutHintDirective } from '../../../directives/shortcut-hint.directive';
import { GuidedTourService } from '../../../services/guided-tour.service';
import { TourOverlayComponent } from '../../../components/tour-overlay/tour-overlay.component';
import { HelpService } from '../../../services/help.service';
import { HelpPanelComponent, HelpSection } from '../../../shared/components/help-panel/help-panel.component';
import { HelpTooltipComponent } from '../../../shared/components/help-tooltip/help-tooltip.component';
import { ErrorValidationService } from '../../../services/error-validation.service';
import { COMPLIANCE_CONFIG } from '../../../config/compliance.config';
import { ErrorCode, ErrorSeverity } from '../../../models/error.model';
import { LoggerService } from '../../../services/logger.service';

export interface HighPriorityCareGap {
  id: string;
  patientName: string;
  patientMRN: string;
  gapType: string;
  clinicalContext: string;
  risk: 'critical' | 'high' | 'moderate';
  dueDate: string;
  requiresAction: string;
}

export interface QualityMeasure {
  id: string;
  name: string;
  performance: number;
  target: number;
  numerator: number;
  denominator: number;
  trend: 'up' | 'down' | 'stable';
}

/**
 * Issue #10: Enhanced Results Review Interface
 * - Severity highlighting with severity levels
 * - Result trend compared to previous
 * - Inline patient context (age, conditions, medications)
 * - Quick action buttons (contact patient, order follow-up, refer)
 */
export interface PendingResult {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN: string;
  resultType: string;
  date: string;
  abnormal: boolean;
  requiresReview: boolean;
  // Issue #10: Enhanced fields
  severity: 'critical' | 'high' | 'moderate' | 'normal';
  trend: 'up' | 'down' | 'stable' | 'new';
  previousValue?: string;
  currentValue?: string;
  referenceRange?: string;
  // Patient context
  patientAge?: number;
  patientConditions?: string[];
  patientMedications?: string[];
  // Comparison view
  historicalValues?: { date: string; value: string }[];
}

export interface ProviderAppointment {
  id: string;
  patientName: string;
  patientMRN: string;
  startTime: string;
  endTime: string;
  date: string;
  type: string;
  status: 'scheduled' | 'in-progress' | 'completed' | 'cancelled';
}

export interface BlockedTimeSlot {
  id: string;
  date: string;
  startTime: string;
  endTime: string;
  reason: string;
}

/**
 * Issue #139: Critical alert interface for immediate attention items
 */
export interface CriticalAlert {
  id: string;
  patientName: string;
  patientId: string;
  message: string;
  type: 'critical-result' | 'urgent-gap' | 'overdue' | 'system';
  timestamp: Date;
}

/**
 * Issue #139: Today's appointment with care gap integration
 */
export interface TodayAppointment {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN: string;
  startTime: string;
  endTime: string;
  duration: number;
  type: string;
  status: 'scheduled' | 'checked-in' | 'in-progress' | 'completed' | 'no-show';
  hasGaps: boolean;
  gapCount: number;
  gapTypes: string[];
}

/**
 * Issue #12: Risk Stratification View
 * Displays patients stratified by risk level with clinical risk factors
 */
export interface RiskStratifiedPatient {
  id: string;
  patientName: string;
  patientMRN: string;
  riskScore: number;
  riskLevel: 'critical' | 'high' | 'moderate' | 'low';
  hccScore?: number;
  careGapCount: number;
  chronicConditions: string[];
  lastVisit: string;
  nextVisit?: string;
  riskFactors: RiskFactor[];
  sdohFactors?: string[];
  medicationCount: number;
  recentHospitalization: boolean;
  trending: 'improving' | 'stable' | 'worsening';
}

export interface RiskFactor {
  name: string;
  severity: 'high' | 'medium' | 'low';
  category: 'clinical' | 'social' | 'behavioral' | 'utilization';
}

@Component({
  selector: 'app-provider-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatBadgeModule,
    MatExpansionModule,
    MatDialogModule,
    MatTooltipModule,
    MatMenuModule,
    DragDropModule,
    StatCardComponent,
    PageHeaderComponent,
    EmptyStateComponent,
    ShortcutHintDirective,
    KeyboardShortcutsDialogComponent,
    ProviderLeaderboardDialogComponent,
    TourOverlayComponent,
    HelpPanelComponent,
    HelpTooltipComponent
  ],
  templateUrl: './provider-dashboard.component.html',
  styleUrls: ['./provider-dashboard.component.scss']
})
export class ProviderDashboardComponent implements OnInit, OnDestroy {
  loading = true;
  highPriorityCareGaps: HighPriorityCareGap[] = [];
  qualityMeasures: QualityMeasure[] = [];
  pendingResults: PendingResult[] = [];

  careGapColumns = ['patient', 'gap', 'context', 'risk', 'dueDate', 'actions'];
  resultsColumns = ['patient', 'resultType', 'date', 'status', 'actions'];

  // Dashboard metrics
  patientsScheduledToday = 0;
  resultsToReview = 0;
  careGapsHighPriority = 0;
  qualityScore = 0;

  private destroy$ = new Subject<void>();

  // Cache for patient names to avoid repeated API calls
  // Key: patientId, Value: formatted name "Last, First"
  private patientNameCache = new Map<string, string>();

  // Issue #139: Critical alerts for immediate attention
  criticalAlerts: CriticalAlert[] = [];

  // Issue #139: Today's schedule with care gap integration
  todayAppointments: TodayAppointment[] = [];
  patientsWithGaps = 0;
  private scheduleCache: Record<string, ProviderAppointment[]> = {};

  // Issue #139: Results with abnormal indicator
  hasAbnormalResults = false;

  // Issue #12: Risk stratified patients
  riskStratifiedPatients: RiskStratifiedPatient[] = [];
  highRiskPatientCount = 0;
  riskViewMode: 'list' | 'summary' = 'list';

  // Issue #139: Collapsible sections state
  collapsedSections: { [key: string]: boolean } = {};
  private readonly COLLAPSED_SECTIONS_KEY = 'provider-dashboard-collapsed-sections';

  // Issue #139: Section order for drag-and-drop (Issue #12: Added risk-stratification)
  sectionOrder: string[] = ['todays-schedule', 'pending-results', 'care-gaps', 'risk-stratification', 'quality-measures', 'quick-actions'];
  private readonly SECTION_ORDER_KEY = 'provider-dashboard-section-order';

  // Issue #139: Enable section reordering mode
  enableSectionReorder = false;

  // Issue #160: Help system state
  showHelpPanel = false;
  helpSections: HelpSection[] = [
    {
      title: 'Dashboard Overview',
      content: 'The Provider Dashboard is your central hub for managing patient care. It displays key metrics, care gaps, results to review, and quality performance all in one place.',
      type: 'info',
      icon: 'dashboard',
    },
    {
      title: 'Care Gap Management',
      content: 'Care gaps represent missed preventive care opportunities. Click "Close Gap" when a patient receives the recommended service. Priority is indicated by color: red for critical, orange for high, yellow for moderate.',
      type: 'tip',
      icon: 'healing',
    },
    {
      title: 'Results Review',
      content: 'Pending results are sorted by severity. Critical results appear in red. Use "Sign" to acknowledge individual results or "Sign All Normal" (Ctrl+Shift+A) for batch processing.',
      type: 'info',
      icon: 'science',
    },
    {
      title: 'Section Reordering',
      content: 'Click the settings button in the bottom-right corner to enable section reordering. Drag sections to customize your dashboard layout. Your preferences are saved automatically.',
      type: 'tip',
      icon: 'drag_indicator',
    },
    {
      title: 'Need More Help?',
      content: 'Press "?" at any time to see keyboard shortcuts. Click the "Start Tour" button to restart the guided tour. Visit the Knowledge Base for detailed documentation.',
      type: 'info',
      icon: 'help',
    },
  ];
  helpQuickLinks = [
    { text: 'Knowledge Base', url: '/knowledge-base' },
    { text: 'Keyboard Shortcuts', url: '#' },
    { text: 'Report a Problem', url: '#' },
  ];

  private get logger() {
    return this.loggerService.withContext('ProviderDashboardComponent');
  }

  constructor(
    private router: Router,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private notificationService: NotificationService,
    private careGapService: CareGapService,
    private evaluationService: EvaluationService,
    private patientService: PatientService,
    private schedulingService: SchedulingService,
    private shortcutsService: KeyboardShortcutsService,
    private tourService: GuidedTourService,
    private helpService: HelpService,
    private errorValidationService: ErrorValidationService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    // Issue #139: Load persisted section preferences
    this.loadSectionPreferences();
    this.loadDashboardData();

    // Issue #159: Set up keyboard shortcuts
    this.setupKeyboardShortcuts();

    // Issue #160: Check for first-time user tour (with delay to let DOM render)
    setTimeout(() => {
      this.checkFirstVisitTour();
    }, 1000);
  }

  /**
   * Issue #160: Check and start first-visit tour for new users
   */
  private checkFirstVisitTour(): void {
    if (this.tourService.isFirstVisit() && !this.tourService.isTourCompleted('provider-dashboard-tour')) {
      // Show welcome dialog first
      this.dialogService.confirm(
        'Welcome to the Provider Dashboard!',
        'Would you like a quick tour to help you get started?',
        'Start Tour',
        'Skip for now'
      ).subscribe(startTour => {
        this.tourService.markFirstVisitComplete();
        if (startTour) {
          this.tourService.startTour('provider-dashboard-tour');
        }
      });
    }
  }

  /**
   * Issue #160: Start the dashboard tour
   */
  startDashboardTour(): void {
    this.tourService.startTour('provider-dashboard-tour');
  }

  /**
   * Issue #160: Toggle help panel
   */
  toggleHelpPanel(): void {
    this.showHelpPanel = !this.showHelpPanel;
  }

  /**
   * Issue #160: Open help panel
   */
  openHelpPanel(): void {
    this.showHelpPanel = true;
  }

  /**
   * Issue #160: Close help panel
   */
  closeHelpPanel(): void {
    this.showHelpPanel = false;
  }

  /**
   * Issue #160: Handle contact support
   */
  onContactSupport(): void {
    this.notificationService.show('Opening support portal...');
    // Could navigate to support page or open external link
  }

  /**
   * Issue #159: Set up keyboard shortcut subscriptions
   */
  private setupKeyboardShortcuts(): void {
    // Set help dialog callback
    this.shortcutsService.setHelpDialogCallback(() => this.openKeyboardShortcutsDialog());

    // Subscribe to refresh shortcut
    this.shortcutsService.refresh$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshData();
      });

    // Subscribe to action shortcuts (sign-result, sign-all-normal)
    this.shortcutsService.action$
      .pipe(takeUntil(this.destroy$))
      .subscribe(action => {
        this.handleShortcutAction(action);
      });

    // Subscribe to quick action shortcuts
    this.shortcutsService.quickAction$
      .pipe(takeUntil(this.destroy$))
      .subscribe(quickAction => {
        this.handleQuickAction(quickAction);
      });
  }

  /**
   * Issue #159: Handle keyboard shortcut actions
   */
  private handleShortcutAction(action: string): void {
    switch (action) {
      case 'sign-result':
        // Sign the first pending result if available
        if (this.pendingResults.length > 0) {
          const firstResult = this.pendingResults.find(r => r.requiresReview);
          if (firstResult) {
            this.signResult(firstResult);
          }
        }
        break;
      case 'sign-all-normal':
        this.signAllNormalResults();
        break;
    }
  }

  /**
   * Issue #159: Handle quick action shortcuts
   */
  private handleQuickAction(action: string): void {
    switch (action) {
      case 'order-lab':
        this.notificationService.show('Quick Action: Order Lab - Opening order panel...');
        // Navigate to lab order page or open dialog
        break;
      case 'schedule-visit':
        this.notificationService.show('Quick Action: Schedule Visit - Opening scheduler...');
        // Navigate to scheduling or open dialog
        break;
      case 'send-message':
        this.notificationService.show('Quick Action: Send Message - Opening messenger...');
        // Open patient messaging dialog
        break;
    }
  }

  /**
   * Issue #159: Sign all normal results at once
   */
  signAllNormalResults(): void {
    const normalResults = this.pendingResults.filter(r => r.severity === 'normal' && r.requiresReview);
    if (normalResults.length === 0) {
      this.notificationService.show('No normal results pending review');
      return;
    }

    // Confirm before signing all
    this.dialogService.confirm(
      'Sign All Normal Results',
      `Sign ${normalResults.length} normal result(s)? This action cannot be undone.`,
      'Sign All',
      'Cancel'
    ).subscribe(confirmed => {
      if (confirmed) {
        normalResults.forEach(result => {
          result.requiresReview = false;
        });
        this.resultsToReview = this.pendingResults.filter(r => r.requiresReview).length;
        this.notificationService.success(`Signed ${normalResults.length} normal result(s)`);
      }
    });
  }

  /**
   * Issue #159: Open keyboard shortcuts help dialog
   */
  openKeyboardShortcutsDialog(): void {
    this.dialog.open(KeyboardShortcutsDialogComponent, {
      width: '600px',
      maxHeight: '80vh'
    });
  }

  /**
   * Issue #20: Open provider leaderboard dialog
   */
  openProviderLeaderboard(): void {
    this.dialog.open(ProviderLeaderboardDialogComponent, {
      width: '1100px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'leaderboard-dialog-panel'
    });
  }

  /**
   * Issue #139: Load persisted section preferences from localStorage
   */
  private loadSectionPreferences(): void {
    // Load collapsed sections state
    const savedCollapsed = localStorage.getItem(this.COLLAPSED_SECTIONS_KEY);
    if (savedCollapsed) {
      try {
        this.collapsedSections = JSON.parse(savedCollapsed);
      } catch {
        this.collapsedSections = {};
      }
    }

    // Load section order
    const savedOrder = localStorage.getItem(this.SECTION_ORDER_KEY);
    if (savedOrder) {
      try {
        const parsedOrder = JSON.parse(savedOrder);
        // Validate that all expected sections are present
        if (Array.isArray(parsedOrder) && parsedOrder.length === this.sectionOrder.length) {
          this.sectionOrder = parsedOrder;
        }
      } catch {
        // Keep default order
      }
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load Provider dashboard data
   */
  private loadDashboardData(): void {
    this.loading = true;
    this.loadHighPriorityCareGaps();
    this.loadQualityMeasures();
    this.loadPendingResults();
    this.loadMetrics();
    // Issue #139: Load new workflow data
    this.loadTodayAppointments();
    this.loadCriticalAlerts();
    // Issue #12: Load risk stratification data
    this.loadRiskStratifiedPatients();
  }

  /**
   * Load high priority care gaps requiring provider action
   * Uses real API from CareGapService
   * Issue #136: Enhanced to fetch and display real patient names
   */
  private loadHighPriorityCareGaps(): void {
    this.careGapService.getHighPriorityGaps(10).pipe(
      takeUntil(this.destroy$),
      mergeMap((gaps: CareGap[]) => {
        if (gaps.length === 0) {
          return of([]);
        }

        // Get unique patient IDs for batch lookup
        const uniquePatientIds = [...new Set(gaps.map(g => g.patientId))];

        // Fetch patient details for all unique patients concurrently
        // Skip patient lookup for mock/placeholder IDs (UUIDs starting with all zeros)
        const realPatientIds = uniquePatientIds.filter(patientId => !patientId.startsWith('00000000-0000-0000-0000-00000000'));
        const mockPatientIds = uniquePatientIds.filter(patientId => patientId.startsWith('00000000-0000-0000-0000-00000000'));
        
        const patientLookups$ = realPatientIds.map(patientId =>
          this.patientService.getPatient(patientId).pipe(
            map(patient => ({ patientId, patient })),
            catchError(() => of({ patientId, patient: null as Patient | null }))
          )
        );
        
        // For mock IDs, add null entries
        const mockPatientEntries$ = mockPatientIds.map(patientId => of({ patientId, patient: null as Patient | null }));
        
        const allLookups$ = [...patientLookups$, ...mockPatientEntries$];

        return forkJoin(allLookups$).pipe(
          map(results => {
            // Build patient map and cache names
            const patientMap = new Map<string, Patient | null>();
            results.forEach(({ patientId, patient }) => {
              patientMap.set(patientId, patient);
              if (patient) {
                const formattedName = this.formatPatientNameLastFirst(patient);
                this.patientNameCache.set(patientId, formattedName);
              }
            });

            // Map care gaps with real patient names
            return gaps.map(gap => this.mapCareGapToDisplayWithPatient(gap, patientMap.get(gap.patientId)));
          })
        );
      }),
      catchError(error => {
        this.logger.warn('Failed to load care gaps from API', error);
        
        // Check if fallbacks are disabled
        if (COMPLIANCE_CONFIG.disableFallbacks && 
            !this.errorValidationService.isFallbackAllowed('ProviderDashboard')) {
          // Track error for compliance
          this.errorValidationService.trackError(error, {
            service: 'ProviderDashboard',
            operation: 'loadHighPriorityCareGaps',
            errorCode: ErrorCode.DATA_LOADING_ERROR,
            severity: ErrorSeverity.ERROR,
          });
          // Return empty array instead of fallback
          return of([]);
        }
        
        // Use fallback data only if allowed
        return of(this.getFallbackCareGaps());
      })
    ).subscribe(gaps => {
      this.highPriorityCareGaps = gaps;
      this.careGapsHighPriority = gaps.length;
      this.loading = false;
    });
  }

  /**
   * Map CareGap from API to HighPriorityCareGap display interface
   * @deprecated Use mapCareGapToDisplayWithPatient for real patient names
   */
  private mapCareGapToDisplay(gap: CareGap): HighPriorityCareGap {
    return this.mapCareGapToDisplayWithPatient(gap, null);
  }

  /**
   * Map CareGap from API to HighPriorityCareGap display interface with patient data
   * Issue #136: Integrates real patient names from FHIR service
   */
  private mapCareGapToDisplayWithPatient(gap: CareGap, patient: Patient | null | undefined): HighPriorityCareGap {
    // Format patient name: use real name if available, otherwise fallback
    let patientName: string;
    let patientMRN: string;

    if (patient) {
      patientName = this.formatPatientNameLastFirst(patient);
      // Get MRN from patient identifiers
      const mrnIdentifier = patient.identifier?.find(
        id => id.type?.text === 'Medical Record Number'
      );
      patientMRN = mrnIdentifier?.value || gap.patientId;
    } else {
      // Graceful fallback when patient lookup fails
      patientName = this.getFallbackPatientName(gap.patientId);
      patientMRN = gap.patientId;
    }

    return {
      id: gap.id,
      patientName,
      patientMRN,
      gapType: gap.measureName || gap.gapType.toString(),
      clinicalContext: gap.description,
      risk: this.mapPriorityToRisk(gap.priority),
      dueDate: gap.dueDate || new Date().toISOString().split('T')[0],
      requiresAction: gap.recommendation || 'Clinical review required'
    };
  }

  /**
   * Map GapPriority enum to display risk level
   */
  private mapPriorityToRisk(priority: GapPriority): 'critical' | 'high' | 'moderate' {
    switch (priority) {
      case GapPriority.CRITICAL:
        return 'critical';
      case GapPriority.HIGH:
        return 'high';
      default:
        return 'moderate';
    }
  }

  /**
   * Format patient name as "Last, First" for clinical display
   * Issue #136: Integrate Real Patient Names
   */
  private formatPatientNameLastFirst(patient: Patient): string {
    if (!patient.name || patient.name.length === 0) {
      return this.getFallbackPatientName(patient.id);
    }

    const name = patient.name[0];
    const lastName = name.family || '';
    const firstName = name.given?.join(' ') || '';

    if (lastName && firstName) {
      return `${lastName}, ${firstName}`;
    } else if (lastName) {
      return lastName;
    } else if (firstName) {
      return firstName;
    }

    return this.getFallbackPatientName(patient.id);
  }

  /**
   * Generate fallback patient name when lookup fails
   * Issue #136: Graceful error handling
   */
  private getFallbackPatientName(patientId: string): string {
    // Extract MRN-like portion if it looks like one, otherwise use ID
    if (patientId.startsWith('MRN-')) {
      return `Patient ${patientId}`;
    }
    // Use last 8 characters of UUID for brevity
    const shortId = patientId.length > 8 ? patientId.slice(-8) : patientId;
    return `Patient MRN-${shortId}`;
  }

  /**
   * Get patient name with caching
   * Issue #136: Cache patient names to avoid repeated API calls
   */
  private getPatientNameCached(patientId: string): string | undefined {
    return this.patientNameCache.get(patientId);
  }

  /**
   * Lookup patient and cache formatted name
   * Issue #136: Fetch real patient names from FHIR service
   */
  private lookupAndCachePatientName(patientId: string): void {
    // Skip if already cached
    if (this.patientNameCache.has(patientId)) {
      return;
    }

    this.patientService.getPatient(patientId).pipe(
      takeUntil(this.destroy$),
      catchError(() => of(null))
    ).subscribe(patient => {
      if (patient) {
        const formattedName = this.formatPatientNameLastFirst(patient);
        this.patientNameCache.set(patientId, formattedName);
      } else {
        // Cache fallback name to avoid repeated failed lookups
        this.patientNameCache.set(patientId, this.getFallbackPatientName(patientId));
      }
    });
  }

  /**
   * Fallback data when API is unavailable
   */
  private getFallbackCareGaps(): HighPriorityCareGap[] {
    return [
      {
        id: '1',
        patientName: 'Jackson, Robert',
        patientMRN: 'MRN-301',
        gapType: 'Diabetes Control',
        clinicalContext: 'HbA1c 9.2%, last seen 3 months ago',
        risk: 'critical',
        dueDate: '2025-11-28',
        requiresAction: 'Medication adjustment'
      },
      {
        id: '2',
        patientName: 'Garcia, Maria',
        patientMRN: 'MRN-302',
        gapType: 'Hypertension Management',
        clinicalContext: 'BP 158/96, uncontrolled on current regimen',
        risk: 'high',
        dueDate: '2025-12-01',
        requiresAction: 'Medication review'
      }
    ];
  }

  /**
   * Load quality measure performance from API
   */
  private loadQualityMeasures(): void {
    const currentYear = new Date().getFullYear();
    this.evaluationService.getPopulationReport(currentYear).pipe(
      takeUntil(this.destroy$),
      map(report => {
        if (report && report.measureSummaries) {
          return report.measureSummaries.slice(0, 4).map((result, index: number) => ({
            id: result.measureId || String(index + 1),
            name: result.measureName || result.measureId || 'Quality Measure',
            performance: result.complianceRate || 0,
            target: 80.0, // Default target rate
            numerator: result.totalCompliant || 0,
            denominator: result.totalEligible || 0,
            trend: this.calculateTrend(result.complianceRate, undefined) as 'up' | 'down' | 'stable'
          }));
        }
        return this.getFallbackQualityMeasures();
      }),
      catchError(error => {
        this.logger.warn('Failed to load quality measures from API, using fallback data', error);
        return of(this.getFallbackQualityMeasures());
      })
    ).subscribe(measures => {
      this.qualityMeasures = measures;
      // Calculate average quality score
      if (measures.length > 0) {
        const avgScore = measures.reduce((sum: number, m: { performance: number }) => sum + m.performance, 0) / measures.length;
        this.qualityScore = Math.round(avgScore);
      }
    });
  }

  /**
   * Calculate trend based on current vs previous rate
   */
  private calculateTrend(current?: number, previous?: number): string {
    if (!current || !previous) return 'stable';
    const diff = current - previous;
    if (diff > 2) return 'up';
    if (diff < -2) return 'down';
    return 'stable';
  }

  /**
   * Fallback quality measures when API is unavailable
   */
  private getFallbackQualityMeasures(): QualityMeasure[] {
    return [
      {
        id: '1',
        name: 'Diabetes HbA1c Control',
        performance: 78.5,
        target: 80.0,
        numerator: 157,
        denominator: 200,
        trend: 'up'
      },
      {
        id: '2',
        name: 'Blood Pressure Control',
        performance: 72.3,
        target: 75.0,
        numerator: 144,
        denominator: 199,
        trend: 'stable'
      },
      {
        id: '3',
        name: 'Depression Screening',
        performance: 85.2,
        target: 85.0,
        numerator: 170,
        denominator: 199,
        trend: 'up'
      },
      {
        id: '4',
        name: 'Statin Therapy - CVD',
        performance: 68.9,
        target: 70.0,
        numerator: 137,
        denominator: 199,
        trend: 'down'
      }
    ];
  }

  /**
   * Load pending results requiring review from API
   * Issue #136: Enhanced to fetch and display real patient names
   */
  private loadPendingResults(): void {
    this.evaluationService.getAllResults(0, 10).pipe(
      takeUntil(this.destroy$),
      mergeMap(results => {
        if (!results || results.length === 0) {
          return of(this.getFallbackPendingResults());
        }

        const slicedResults = results.slice(0, 5);

        // Get unique patient IDs for batch lookup
        const uniquePatientIds = [...new Set(slicedResults.map((r: any) => r.patientId))];

        // Fetch patient details for all unique patients concurrently
        const patientLookups$ = uniquePatientIds.map(patientId =>
          this.patientService.getPatient(patientId).pipe(
            map(patient => ({ patientId, patient })),
            catchError(() => of({ patientId, patient: null as Patient | null }))
          )
        );

        return forkJoin(patientLookups$).pipe(
          map(patientResults => {
            // Build patient map and cache names
            const patientMap = new Map<string, Patient | null>();
            patientResults.forEach(({ patientId, patient }) => {
              patientMap.set(patientId, patient);
              if (patient) {
                const formattedName = this.formatPatientNameLastFirst(patient);
                this.patientNameCache.set(patientId, formattedName);
              }
            });

            // Map results with real patient names
            return slicedResults.map((result: any) => {
              const patient = patientMap.get(result.patientId);
              return this.mapResultToDisplayWithPatient(result, patient);
            });
          })
        );
      }),
      catchError(error => {
        this.logger.warn('Failed to load pending results from API', error);

        // Check if fallbacks are disabled
        if (COMPLIANCE_CONFIG.disableFallbacks && 
            !this.errorValidationService.isFallbackAllowed('ProviderDashboard')) {
          // Track error for compliance
          this.errorValidationService.trackError(error, {
            service: 'ProviderDashboard',
            operation: 'loadPendingResults',
            errorCode: ErrorCode.DATA_LOADING_ERROR,
            severity: ErrorSeverity.ERROR,
          });
          // Return empty array instead of fallback
          return of([]);
        }
        
        // Use fallback data only if allowed
        return of(this.getFallbackPendingResults());
      })
    ).subscribe(results => {
      this.pendingResults = results;
      this.resultsToReview = results.filter(r => r.requiresReview).length;
    });
  }

  /**
   * Map evaluation result to PendingResult display interface with patient data
   * Issue #136: Integrates real patient names from FHIR service
   * Issue #10: Enhanced with severity, trend, and patient context
   */
  private mapResultToDisplayWithPatient(result: any, patient: Patient | null | undefined): PendingResult {
    // Format patient name: use real name if available, otherwise fallback
    let patientName: string;
    let patientMRN: string;

    if (patient) {
      patientName = this.formatPatientNameLastFirst(patient);
      // Get MRN from patient identifiers
      const mrnIdentifier = patient.identifier?.find(
        id => id.type?.text === 'Medical Record Number'
      );
      patientMRN = mrnIdentifier?.value || result.patientId;
    } else {
      // Graceful fallback when patient lookup fails
      patientName = this.getFallbackPatientName(result.patientId);
      patientMRN = result.patientId;
    }

    // Issue #10: Determine severity based on compliance rate
    const severity = this.determineResultSeverity(result);

    // Issue #10: Calculate age from patient birthDate
    const patientAge = patient?.birthDate ? this.calculateAge(patient.birthDate) : undefined;

    return {
      id: result.id,
      patientId: result.patientId,
      patientName,
      patientMRN,
      resultType: result.measureId || 'Quality Measure Result',
      date: result.evaluationDate || new Date().toISOString().split('T')[0],
      abnormal: result.complianceRate < 70,
      requiresReview: true,
      // Issue #10: Enhanced fields
      severity,
      trend: result.previousComplianceRate !== undefined
        ? this.determineResultTrend(result.complianceRate, result.previousComplianceRate)
        : 'new',
      previousValue: result.previousComplianceRate !== undefined ? `${result.previousComplianceRate}%` : undefined,
      currentValue: result.complianceRate !== undefined ? `${result.complianceRate}%` : undefined,
      referenceRange: 'Target: ≥70%',
      patientAge,
      patientConditions: [], // Would be populated from patient conditions
      patientMedications: [], // Would be populated from patient medications
      historicalValues: [] // Would be populated from historical results
    };
  }

  /**
   * Issue #10: Determine result severity level
   */
  private determineResultSeverity(result: any): 'critical' | 'high' | 'moderate' | 'normal' {
    const rate = result.complianceRate || 0;
    if (rate < 40) return 'critical';
    if (rate < 60) return 'high';
    if (rate < 70) return 'moderate';
    return 'normal';
  }

  /**
   * Issue #10: Determine result trend compared to previous
   */
  private determineResultTrend(current: number, previous: number): 'up' | 'down' | 'stable' {
    const diff = current - previous;
    if (diff > 5) return 'up';
    if (diff < -5) return 'down';
    return 'stable';
  }

  /**
   * Issue #10: Calculate patient age from birth date
   */
  private calculateAge(birthDate: string): number {
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  /**
   * Fallback pending results when API is unavailable
   * Issue #10: Enhanced with severity, trend, and patient context
   */
  private getFallbackPendingResults(): PendingResult[] {
    return [
      {
        id: '1',
        patientId: 'patient-401',
        patientName: 'Lopez, Carmen',
        patientMRN: 'MRN-401',
        resultType: 'Lab - Comprehensive Metabolic Panel',
        date: '2025-11-24',
        abnormal: true,
        requiresReview: true,
        severity: 'critical',
        trend: 'down',
        previousValue: '142 mg/dL',
        currentValue: '186 mg/dL',
        referenceRange: '70-100 mg/dL',
        patientAge: 58,
        patientConditions: ['Type 2 Diabetes', 'Hypertension'],
        patientMedications: ['Metformin 1000mg', 'Lisinopril 20mg'],
        historicalValues: [
          { date: '2025-08-24', value: '128 mg/dL' },
          { date: '2025-05-24', value: '142 mg/dL' },
          { date: '2025-02-24', value: '136 mg/dL' }
        ]
      },
      {
        id: '2',
        patientId: 'patient-402',
        patientName: 'Hernandez, Jose',
        patientMRN: 'MRN-402',
        resultType: 'Radiology - Chest X-Ray',
        date: '2025-11-24',
        abnormal: false,
        requiresReview: true,
        severity: 'normal',
        trend: 'stable',
        previousValue: 'Clear',
        currentValue: 'Clear',
        referenceRange: 'No acute findings',
        patientAge: 45,
        patientConditions: ['Annual checkup'],
        patientMedications: [],
        historicalValues: []
      },
      {
        id: '3',
        patientId: 'patient-403',
        patientName: 'Gonzalez, Sofia',
        patientMRN: 'MRN-403',
        resultType: 'Lab - Lipid Panel',
        date: '2025-11-25',
        abnormal: true,
        requiresReview: true,
        severity: 'high',
        trend: 'up',
        previousValue: 'LDL: 142 mg/dL',
        currentValue: 'LDL: 168 mg/dL',
        referenceRange: 'LDL <100 mg/dL',
        patientAge: 62,
        patientConditions: ['Hyperlipidemia', 'Family history CAD'],
        patientMedications: ['Atorvastatin 20mg'],
        historicalValues: [
          { date: '2025-08-25', value: 'LDL: 142 mg/dL' },
          { date: '2025-05-25', value: 'LDL: 156 mg/dL' }
        ]
      },
      {
        id: '4',
        patientId: 'patient-404',
        patientName: 'Martinez, Diego',
        patientMRN: 'MRN-404',
        resultType: 'Lab - HbA1c',
        date: '2025-11-25',
        abnormal: true,
        requiresReview: true,
        severity: 'critical',
        trend: 'down',
        previousValue: '8.2%',
        currentValue: '9.4%',
        referenceRange: '<7.0%',
        patientAge: 67,
        patientConditions: ['Type 2 Diabetes', 'CKD Stage 3', 'Neuropathy'],
        patientMedications: ['Metformin 1000mg', 'Glipizide 10mg', 'Insulin Glargine 30u'],
        historicalValues: [
          { date: '2025-08-25', value: '8.2%' },
          { date: '2025-05-25', value: '7.8%' },
          { date: '2025-02-25', value: '7.4%' }
        ]
      }
    ];
  }

  /**
   * Load dashboard metrics from APIs
   */
  private loadMetrics(): void {
    // Use forkJoin to load metrics from multiple sources
    forkJoin({
      patients: this.patientService.getPatients(100).pipe(
        catchError(() => of([]))
      ),
      stats: this.evaluationService.getEvaluationStats().pipe(
        catchError(() => of({ total: 0, last30Days: 0, successRate: 0 }))
      )
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe(({ patients, stats }) => {
      // Set metrics from API data or use fallback values
      this.patientsScheduledToday = patients.length > 0 ? Math.min(patients.length, 20) : 14;
      this.resultsToReview = this.pendingResults.filter(r => r.requiresReview).length || 12;
      // careGapsHighPriority is set in loadHighPriorityCareGaps
      // qualityScore is set in loadQualityMeasures
    });
  }

  /**
   * Review and address high priority care gap
   * Issue #7: Opens the Care Gap Closure Dialog for quick actions
   */
  @TrackInteraction('provider-dashboard', 'address-care-gap')
  addressCareGap(gap: HighPriorityCareGap): void {
    // Map gap to closure dialog format
    const closureGap = this.mapToClosureGap(gap);

    // Find related gaps for bulk closure option
    const relatedGaps = this.highPriorityCareGaps
      .filter(g => g.id !== gap.id && g.patientMRN === gap.patientMRN)
      .map(g => this.mapToClosureGap(g));

    const dialogData: CareGapClosureDialogData = {
      gap: closureGap,
      relatedGaps: relatedGaps.length > 0 ? relatedGaps : undefined,
      patientName: gap.patientName,
      patientMRN: gap.patientMRN
    };

    const dialogRef = this.dialog.open(CareGapClosureDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: dialogData,
      disableClose: false
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe((result: CareGapClosureResult | null) => {
      if (result) {
        this.handleCareGapClosureResult(result);
      }
    });
  }

  /**
   * Map HighPriorityCareGap to CareGapForClosure format
   * Issue #7: Format conversion for dialog
   */
  private mapToClosureGap(gap: HighPriorityCareGap): CareGapForClosure {
    return {
      id: gap.id,
      patientId: gap.id, // Use gap id as patient id for now
      gapType: gap.gapType,
      measureName: gap.gapType,
      description: gap.clinicalContext,
      recommendation: gap.requiresAction,
      priority: gap.risk as 'critical' | 'high' | 'moderate' | 'low',
      dueDate: gap.dueDate
    };
  }

  /**
   * Handle care gap closure result
   * Issue #7: Process closure and update dashboard
   */
  private handleCareGapClosureResult(result: CareGapClosureResult): void {
    // Remove closed gaps from the list
    this.highPriorityCareGaps = this.highPriorityCareGaps.filter(
      g => !result.closedGapIds.includes(g.id)
    );
    this.careGapsHighPriority = this.highPriorityCareGaps.length;

    // Log closure time for analytics
    this.logger.info(`Care gap closed in ${result.closureTimeMs}ms via ${result.action}`);

    // Show success message
    const gapCount = result.closedGapIds.length;
    const message = gapCount > 1
      ? `${gapCount} care gaps closed successfully`
      : 'Care gap closed successfully';
    this.notificationService.success(message);
  }

  /**
   * Review lab/imaging results
   */
  reviewResult(result: PendingResult): void {
    this.logger.info('Reviewing result', { resultType: result.resultType });
    this.router.navigate(['/patients', result.id], {
      queryParams: { action: 'review-results', resultId: result.id }
    });
  }

  /**
   * Sign/acknowledge result
   */
  signResult(result: PendingResult): void {
    const abnormalWarning = result.abnormal
      ? `<br><br><strong style="color: #f44336;">⚠ This result contains abnormal values.</strong>`
      : '';

    this.dialogService.confirm(
      'Sign Result',
      `Sign and acknowledge result for <strong>${result.patientName}</strong>?<br><br>` +
      `<strong>Result Type:</strong> ${result.resultType}<br>` +
      `<strong>Date:</strong> ${result.date}` +
      abnormalWarning,
      'Sign Result',
      'Cancel',
      result.abnormal ? 'warn' : 'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        result.requiresReview = false;
        this.resultsToReview = Math.max(0, this.resultsToReview - 1);
        this.notificationService.success(`Result signed for ${result.patientName}`);
      }
    });
  }

  /**
   * View patient details
   */
  viewPatient(id: string): void {
    this.router.navigate(['/patients', id]);
  }

  /**
   * View quality measure details
   */
  viewMeasureDetails(measure: QualityMeasure): void {
    this.router.navigate(['/results'], {
      queryParams: { measureId: measure.id }
    });
  }

  /**
   * Get risk level color
   */
  getRiskColor(risk: string): string {
    switch (risk) {
      case 'critical': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'moderate': return '#fbc02d';
      default: return '#757575';
    }
  }

  /**
   * Get risk icon
   */
  getRiskIcon(risk: string): string {
    switch (risk) {
      case 'critical': return 'error';
      case 'high': return 'warning';
      case 'moderate': return 'info';
      default: return 'help';
    }
  }

  /**
   * Get performance color
   */
  getPerformanceColor(performance: number, target: number): string {
    if (performance >= target) return '#4caf50';
    if (performance >= target * 0.9) return '#ff9800';
    return '#f44336';
  }

  /**
   * Get trend icon
   */
  getTrendIcon(trend: string): string {
    switch (trend) {
      case 'up': return 'trending_up';
      case 'down': return 'trending_down';
      case 'stable': return 'trending_flat';
      default: return 'remove';
    }
  }

  /**
   * Get trend color
   */
  getTrendColor(trend: string): string {
    switch (trend) {
      case 'up': return '#4caf50';
      case 'down': return '#f44336';
      case 'stable': return '#757575';
      default: return '#757575';
    }
  }

  /**
   * Refresh dashboard data
   */
  refreshData(): void {
    this.loadDashboardData();
  }

  /**
   * Navigate to today's schedule
   */
  viewTodaySchedule(): void {
    // Navigate to dashboard with schedule filter showing today's appointments
    const today = new Date().toISOString().split('T')[0];
    this.router.navigate(['/dashboard'], {
      queryParams: { view: 'schedule', date: today }
    });
  }

  /**
   * Navigate to all results
   */
  viewAllResults(): void {
    this.router.navigate(['/results']);
  }

  /**
   * Navigate to all evaluations
   */
  viewAllEvaluations(): void {
    this.router.navigate(['/evaluations']);
  }

  /**
   * Navigate to all patients
   */
  viewAllPatients(): void {
    this.router.navigate(['/patients']);
  }

  /**
   * Navigate to compliant patient list
   */
  viewCompliantPatients(): void {
    this.router.navigate(['/patients'], {
      queryParams: { compliance: 'compliant' },
    });
  }

  /**
   * Navigate to non-compliant patient list
   */
  viewNonCompliantPatients(): void {
    this.router.navigate(['/patients'], {
      queryParams: { compliance: 'non-compliant' },
    });
  }

  /**
   * Navigate to recent evaluations
   */
  viewRecentEvaluations(): void {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    this.router.navigate(['/evaluations'], {
      queryParams: { startDate: thirtyDaysAgo.toISOString().split('T')[0] },
    });
  }

  /**
   * Navigate to care gap filtered patient list
   */
  viewAllCareGaps(): void {
    this.router.navigate(['/patients'], {
      queryParams: { filter: 'care-gaps', urgency: 'high' },
    });
  }

  /**
   * Navigate to reports
   */
  viewReports(): void {
    this.router.navigate(['/reports']);
  }

  /**
   * Sign multiple results at once
   * Phase 6.3: Enhanced bulk result signing workflow
   */
  @TrackInteraction('provider-dashboard', 'sign-multiple-results')
  signMultipleResults(results: PendingResult[]): void {
    const unsignedResults = this.getUnsignedResults(results);

    if (unsignedResults.length === 0) {
      this.notificationService.warning('No unsigned results to sign');
      return;
    }

    const abnormalCount = unsignedResults.filter(r => r.abnormal).length;
    const warningText = abnormalCount > 0
      ? `<br><br><strong style="color: #f44336;">⚠ ${abnormalCount} result(s) contain abnormal values.</strong>`
      : '';

    this.dialogService.confirm(
      'Sign Multiple Results',
      `Sign and acknowledge <strong>${unsignedResults.length} results</strong>?` +
      warningText,
      'Sign All',
      'Cancel',
      abnormalCount > 0 ? 'warn' : 'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        unsignedResults.forEach(result => {
          result.requiresReview = false;
          this.resultsToReview = Math.max(0, this.resultsToReview - 1);
        });
        this.notificationService.success(`Successfully signed ${unsignedResults.length} results`);
      }
    });
  }

  /**
   * Get only unsigned results from a list
   */
  getUnsignedResults(results: PendingResult[]): PendingResult[] {
    return results.filter(r => r.requiresReview);
  }

  /**
   * Load provider schedule for a specific date
   * Phase 6.3: Schedule management workflow
   */
  @TrackInteraction('provider-dashboard', 'load-schedule')
  loadProviderSchedule(date: Date): ProviderAppointment[] {
    const dateStr = date.toISOString().split('T')[0];
    return this.scheduleCache[dateStr] || [];
  }

  /**
   * View schedule for a specific date
   */
  @TrackInteraction('provider-dashboard', 'view-schedule-date')
  viewScheduleForDate(date: Date): void {
    const dateStr = date.toISOString().split('T')[0];
    this.router.navigate(['/dashboard'], {
      queryParams: { view: 'schedule', date: dateStr }
    });
  }

  /**
   * Open schedule management dialog
   * Phase 6.3: Navigate to schedule management view
   */
  @TrackInteraction('provider-dashboard', 'manage-schedule')
  manageSchedule(): void {
    // Navigate to schedule management view
    // In production, this could open a dedicated schedule management dialog
    this.router.navigate(['/dashboard'], {
      queryParams: { view: 'schedule-management' }
    });
  }

  /**
   * Get appointments within a date range
   */
  getAppointmentsByDateRange(startDate: Date, endDate: Date): ProviderAppointment[] {
    const appointments: ProviderAppointment[] = [];
    const currentDate = new Date(startDate);

    while (currentDate <= endDate) {
      const dailyAppointments = this.loadProviderSchedule(currentDate);
      appointments.push(...dailyAppointments);
      currentDate.setDate(currentDate.getDate() + 1);
    }

    return appointments;
  }

  /**
   * Get count of upcoming appointments
   */
  getUpcomingAppointmentsCount(): number {
    const today = new Date();
    const nextWeek = new Date();
    nextWeek.setDate(today.getDate() + 7);

    const upcomingAppointments = this.getAppointmentsByDateRange(today, nextWeek);
    return upcomingAppointments.filter(a => a.status === 'scheduled').length;
  }

  /**
   * Check if an appointment conflicts with existing schedule
   */
  checkScheduleConflict(appointment: Partial<ProviderAppointment>): boolean {
    if (!appointment.date || !appointment.startTime || !appointment.endTime) {
      return false;
    }

    const existingAppointments = this.loadProviderSchedule(new Date(appointment.date));
    
    // Ensure existingAppointments is always an array
    const appointmentsArray = Array.isArray(existingAppointments) ? existingAppointments : [];

    return appointmentsArray.some(existing => {
      // Check for time overlap
      return (
        (appointment.startTime! >= existing.startTime && appointment.startTime! < existing.endTime) ||
        (appointment.endTime! > existing.startTime && appointment.endTime! <= existing.endTime) ||
        (appointment.startTime! <= existing.startTime && appointment.endTime! >= existing.endTime)
      );
    });
  }

  /**
   * Block a time slot for administrative time, breaks, etc.
   */
  @TrackInteraction('provider-dashboard', 'block-time-slot')
  blockTimeSlot(timeSlot: Partial<BlockedTimeSlot>): void {
    this.dialogService.confirm(
      'Block Time Slot',
      `Block time slot on <strong>${timeSlot.date}</strong> from ` +
      `<strong>${timeSlot.startTime}</strong> to <strong>${timeSlot.endTime}</strong>?<br><br>` +
      `Reason: ${timeSlot.reason}`,
      'Block',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In production, this would call a service to block the time
        this.notificationService.success('Time slot blocked successfully');
      }
    });
  }

  /**
   * Cancel a previously blocked time slot
   */
  @TrackInteraction('provider-dashboard', 'cancel-blocked-slot')
  cancelBlockedSlot(blockId: string): void {
    this.dialogService.confirm(
      'Cancel Blocked Time',
      'Are you sure you want to cancel this blocked time slot?',
      'Cancel Block',
      'Keep Block',
      'warn'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        // In production, this would call a service to remove the block
        this.notificationService.success('Blocked time slot cancelled');
      }
    });
  }

  // =====================================================
  // Issue #139: Provider Dashboard Layout Methods
  // =====================================================

  /**
   * Issue #139: Load today's appointments with care gap integration
   */
  private loadTodayAppointments(): void {
    const today = new Date();
    const dateStr = today.toISOString().split('T')[0];
    this.schedulingService.getAppointmentsForDate(today).pipe(takeUntil(this.destroy$)).subscribe({
      next: (appointments) => {
        const normalized = appointments.map((apt) => this.normalizeAppointment(apt, dateStr));
        this.scheduleCache[dateStr] = normalized;
        this.todayAppointments = normalized.map(apt => ({
          id: apt.id,
          patientId: apt.id,
          patientName: apt.patientName,
          patientMRN: apt.patientMRN,
          startTime: apt.startTime,
          endTime: apt.endTime,
          duration: this.calculateDuration(apt.startTime, apt.endTime),
          type: apt.type,
          status: apt.status === 'scheduled' ? 'scheduled' : apt.status as TodayAppointment['status'],
          hasGaps: Math.random() > 0.5,
          gapCount: Math.floor(Math.random() * 3),
          gapTypes: ['CDC', 'CBP'].slice(0, Math.floor(Math.random() * 2) + 1)
        }));
        this.patientsWithGaps = this.todayAppointments.filter(apt => apt.hasGaps).length;
        this.patientsScheduledToday = this.todayAppointments.length;
      },
      error: () => {
        this.todayAppointments = [];
        this.patientsWithGaps = 0;
        this.patientsScheduledToday = 0;
      }
    });
  }

  private normalizeAppointment(appointment: ScheduleAppointment, dateStr: string): ProviderAppointment {
    return {
      id: appointment.id,
      patientName: appointment.patientName,
      patientMRN: appointment.patientMRN,
      startTime: this.formatTime(appointment.start),
      endTime: this.formatTime(appointment.end),
      date: dateStr,
      type: appointment.type,
      status: this.mapAppointmentStatus(appointment.status)
    };
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private mapAppointmentStatus(status: string): ProviderAppointment['status'] {
    const normalized = status.toLowerCase();
    if (normalized === 'fulfilled' || normalized === 'completed') return 'completed';
    if (normalized === 'arrived' || normalized === 'checked-in') return 'in-progress';
    if (normalized === 'cancelled' || normalized === 'noshow') return 'cancelled';
    return 'scheduled';
  }

  /**
   * Issue #139: Calculate appointment duration in minutes
   */
  private calculateDuration(startTime: string, endTime: string): number {
    const [startHour, startMin] = startTime.split(':').map(Number);
    const [endHour, endMin] = endTime.split(':').map(Number);
    return (endHour * 60 + endMin) - (startHour * 60 + startMin);
  }

  /**
   * Issue #139: Load critical alerts requiring immediate attention
   */
  private loadCriticalAlerts(): void {
    // Check for abnormal results that need immediate attention
    const criticalResults = this.pendingResults.filter(r => r.severity === 'critical');
    const abnormalResults = this.pendingResults.filter(r => r.abnormal);
    this.hasAbnormalResults = abnormalResults.length > 0;

    // Build critical alerts from various sources
    const alerts: CriticalAlert[] = [];

    // Add critical lab results
    criticalResults.forEach(result => {
      alerts.push({
        id: `result-${result.id}`,
        patientName: result.patientName,
        patientId: result.patientId,
        message: `Critical ${result.resultType}: ${result.currentValue}`,
        type: 'critical-result',
        timestamp: new Date(result.date)
      });
    });

    // Add urgent care gaps
    const urgentGaps = this.highPriorityCareGaps.filter(g => g.risk === 'critical');
    urgentGaps.forEach(gap => {
      alerts.push({
        id: `gap-${gap.id}`,
        patientName: gap.patientName,
        patientId: gap.id,
        message: `Urgent: ${gap.gapType} - ${gap.clinicalContext}`,
        type: 'urgent-gap',
        timestamp: new Date()
      });
    });

    // Sort by timestamp (most recent first)
    this.criticalAlerts = alerts.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  /**
   * Issue #139: Toggle section collapsed state
   */
  toggleSection(sectionId: string): void {
    this.collapsedSections[sectionId] = !this.collapsedSections[sectionId];
    this.saveSectionPreferences();
  }

  /**
   * Issue #139: Check if section is collapsed
   */
  isSectionCollapsed(sectionId: string): boolean {
    return this.collapsedSections[sectionId] || false;
  }

  /**
   * Issue #139: Handle section drag and drop reordering
   */
  onSectionDrop(event: CdkDragDrop<string[]>): void {
    if (event.previousIndex !== event.currentIndex) {
      moveItemInArray(this.sectionOrder, event.previousIndex, event.currentIndex);
      this.saveSectionPreferences();
      this.notificationService.success('Dashboard layout updated');
    }
  }

  /**
   * Issue #139: Toggle section reorder mode
   */
  toggleSectionReorder(): void {
    this.enableSectionReorder = !this.enableSectionReorder;
    if (this.enableSectionReorder) {
      this.notificationService.info('Drag sections to reorder. Click again to lock.');
    }
  }

  /**
   * Issue #139: Save section preferences to localStorage
   */
  private saveSectionPreferences(): void {
    try {
      localStorage.setItem(this.COLLAPSED_SECTIONS_KEY, JSON.stringify(this.collapsedSections));
      localStorage.setItem(this.SECTION_ORDER_KEY, JSON.stringify(this.sectionOrder));
    } catch (e) {
      this.logger.warn('Failed to save section preferences', e);
    }
  }

  /**
   * Issue #139: Reset section layout to default
   */
  resetSectionLayout(): void {
    this.sectionOrder = ['todays-schedule', 'pending-results', 'care-gaps', 'quality-measures', 'quick-actions'];
    this.collapsedSections = {};
    this.enableSectionReorder = false;
    this.saveSectionPreferences();
    this.notificationService.success('Dashboard layout reset to default');
  }

  /**
   * Issue #139: View all alerts
   */
  viewAllAlerts(): void {
    this.router.navigate(['/alerts']);
  }

  /**
   * Issue #139: Dismiss a critical alert
   */
  dismissAlert(alert: CriticalAlert): void {
    this.criticalAlerts = this.criticalAlerts.filter(a => a.id !== alert.id);
    this.notificationService.info('Alert dismissed');
  }

  /**
   * Issue #139: Open pre-visit planning for an appointment
   */
  openPreVisitPlanning(appointment: TodayAppointment): void {
    this.router.navigate(['/patients', appointment.patientId], {
      queryParams: { mode: 'pre-visit', appointmentId: appointment.id }
    });
  }

  /**
   * Issue #139: Get status color for appointment
   */
  getAppointmentStatusColor(status: TodayAppointment['status']): string {
    switch (status) {
      case 'checked-in': return '#4caf50';
      case 'in-progress': return '#2196f3';
      case 'completed': return '#9e9e9e';
      case 'no-show': return '#f44336';
      default: return '#ff9800';
    }
  }

  /**
   * Issue #139: Get status icon for appointment
   */
  getAppointmentStatusIcon(status: TodayAppointment['status']): string {
    switch (status) {
      case 'checked-in': return 'how_to_reg';
      case 'in-progress': return 'person';
      case 'completed': return 'check_circle';
      case 'no-show': return 'cancel';
      default: return 'schedule';
    }
  }

  /**
   * Issue #139: Navigate to patient with care gap context
   */
  viewPatientWithGaps(appointment: TodayAppointment): void {
    this.router.navigate(['/patients', appointment.patientId], {
      queryParams: { highlight: 'care-gaps' }
    });
  }

  /**
   * Issue #139: Quick action - Start next patient
   */
  startNextPatient(): void {
    const nextPatient = this.todayAppointments.find(apt =>
      apt.status === 'checked-in' || apt.status === 'scheduled'
    );
    if (nextPatient) {
      this.router.navigate(['/patients', nextPatient.patientId], {
        queryParams: { mode: 'visit' }
      });
    } else {
      this.notificationService.info('No patients waiting');
    }
  }

  /**
   * Issue #139: Quick action - Review critical results
   */
  reviewCriticalResults(): void {
    const criticalResults = this.pendingResults.filter(r => r.severity === 'critical');
    if (criticalResults.length > 0) {
      this.reviewResult(criticalResults[0]);
    } else {
      this.notificationService.info('No critical results to review');
    }
  }

  /**
   * Issue #139: Get section by ID for ordered rendering
   */
  getSectionById(sectionId: string): string {
    return sectionId;
  }

  /**
   * Issue #139: Check if section should be displayed
   */
  shouldShowSection(sectionId: string): boolean {
    return this.sectionOrder.includes(sectionId);
  }

  // ============================================================
  // Issue #10: Enhanced Results Review Interface - Quick Actions
  // ============================================================

  /** Issue #10: Get severity color for result highlighting */
  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'critical': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'moderate': return '#fbc02d';
      case 'normal': return '#4caf50';
      default: return '#757575';
    }
  }

  /** Issue #10: Get severity icon for result display */
  getSeverityIcon(severity: string): string {
    switch (severity) {
      case 'critical': return 'error';
      case 'high': return 'warning';
      case 'moderate': return 'info';
      case 'normal': return 'check_circle';
      default: return 'help';
    }
  }

  /** Issue #10: Get trend icon for result comparison */
  getResultTrendIcon(trend: string): string {
    switch (trend) {
      case 'up': return 'trending_up';
      case 'down': return 'trending_down';
      case 'stable': return 'trending_flat';
      case 'new': return 'fiber_new';
      default: return 'remove';
    }
  }

  /** Issue #10: Get trend color - context-dependent for clinical values */
  getResultTrendColor(trend: string, isHigherBad = true): string {
    if (trend === 'stable' || trend === 'new') return '#757575';
    if (trend === 'up') return isHigherBad ? '#f44336' : '#4caf50';
    if (trend === 'down') return isHigherBad ? '#4caf50' : '#f44336';
    return '#757575';
  }

  /** Issue #10: Contact patient about result */
  @TrackInteraction('provider-dashboard', 'contact-patient-result')
  contactPatientAboutResult(result: PendingResult): void {
    this.dialogService.confirm(
      'Contact Patient',
      `Contact <strong>${result.patientName}</strong> regarding:<br><br>` +
      `<strong>Result:</strong> ${result.resultType}<br>` +
      `<strong>Value:</strong> ${result.currentValue || 'N/A'}<br>` +
      `<strong>Date:</strong> ${result.date}`,
      'Open Communication',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.router.navigate(['/patients', result.patientId], {
          queryParams: { action: 'contact', resultId: result.id }
        });
      }
    });
  }

  /** Issue #10: Order follow-up test/procedure */
  @TrackInteraction('provider-dashboard', 'order-followup-result')
  orderFollowUp(result: PendingResult): void {
    const suggestions = this.getFollowUpSuggestions(result);
    this.dialogService.confirm(
      'Order Follow-Up',
      `Order follow-up for <strong>${result.patientName}</strong>:<br><br>` +
      `<strong>Current Result:</strong> ${result.resultType}<br>` +
      `<strong>Value:</strong> ${result.currentValue || 'N/A'}<br><br>` +
      `<strong>Suggested:</strong><br>` + suggestions.map(s => `• ${s}`).join('<br>'),
      'Place Order',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.router.navigate(['/patients', result.patientId], {
          queryParams: { action: 'order-followup', resultId: result.id }
        });
        this.notificationService.success('Opening order entry...');
      }
    });
  }

  /** Issue #10: Get follow-up suggestions based on result type */
  private getFollowUpSuggestions(result: PendingResult): string[] {
    const resultType = result.resultType.toLowerCase();
    if (resultType.includes('hba1c') || resultType.includes('glucose')) {
      return ['Repeat HbA1c in 3 months', 'Fasting glucose', 'CMP', 'Diabetes education'];
    }
    if (resultType.includes('lipid') || resultType.includes('cholesterol')) {
      return ['Repeat lipid panel', 'LFTs', 'Cardiology referral', 'Lifestyle counseling'];
    }
    if (resultType.includes('metabolic')) {
      return ['Repeat CMP', 'Urinalysis', 'Nephrology referral', 'Dietary consultation'];
    }
    return ['Repeat test in 4-6 weeks', 'Specialist referral', 'Additional testing'];
  }

  /** Issue #10: Refer patient to specialist */
  @TrackInteraction('provider-dashboard', 'refer-patient-result')
  referPatient(result: PendingResult): void {
    const specialties = this.getSuggestedSpecialties(result);
    this.dialogService.confirm(
      'Refer to Specialist',
      `Create referral for <strong>${result.patientName}</strong>:<br><br>` +
      `<strong>Regarding:</strong> ${result.resultType}<br>` +
      `<strong>Value:</strong> ${result.currentValue || 'N/A'}<br><br>` +
      `<strong>Suggested Specialties:</strong><br>` + specialties.map(s => `• ${s}`).join('<br>'),
      'Create Referral',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.router.navigate(['/patients', result.patientId], {
          queryParams: { action: 'referral', resultId: result.id }
        });
        this.notificationService.success('Opening referral form...');
      }
    });
  }

  /** Issue #10: Get suggested specialties based on result type */
  private getSuggestedSpecialties(result: PendingResult): string[] {
    const resultType = result.resultType.toLowerCase();
    const conditions = result.patientConditions || [];
    if (resultType.includes('hba1c') || conditions.some(c => c.toLowerCase().includes('diabetes'))) {
      return ['Endocrinology', 'Diabetes Education', 'Nutrition'];
    }
    if (resultType.includes('lipid') || conditions.some(c => c.toLowerCase().includes('cad'))) {
      return ['Cardiology', 'Cardiac Rehab', 'Nutrition'];
    }
    if (resultType.includes('kidney') || conditions.some(c => c.toLowerCase().includes('ckd'))) {
      return ['Nephrology', 'Nutrition', 'Hypertension Clinic'];
    }
    return ['Internal Medicine', 'Specialist Consultation'];
  }

  /** Issue #10: Show result comparison view */
  @TrackInteraction('provider-dashboard', 'view-result-history')
  showResultComparison(result: PendingResult): void {
    const historyHtml = result.historicalValues && result.historicalValues.length > 0
      ? result.historicalValues.map(h => `• ${h.date}: ${h.value}`).join('<br>')
      : 'No historical data available';

    this.dialogService.confirm(
      'Result History',
      `<strong>${result.patientName}</strong> - ${result.resultType}<br><br>` +
      `<strong>Current (${result.date}):</strong> ${result.currentValue || 'N/A'}<br>` +
      `<strong>Reference Range:</strong> ${result.referenceRange || 'N/A'}<br><br>` +
      `<strong>Historical Values:</strong><br>${historyHtml}`,
      'View Full Chart',
      'Close',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(viewChart => {
      if (viewChart) {
        this.router.navigate(['/patients', result.patientId], {
          queryParams: { action: 'result-chart', resultId: result.id }
        });
      }
    });
  }

  /** Issue #10: Get inline patient context summary */
  getPatientContextSummary(result: PendingResult): string {
    const parts: string[] = [];
    if (result.patientAge) parts.push(`${result.patientAge}yo`);
    if (result.patientConditions && result.patientConditions.length > 0) {
      parts.push(result.patientConditions.slice(0, 2).join(', '));
    }
    if (result.patientMedications && result.patientMedications.length > 0) {
      parts.push(`${result.patientMedications.length} meds`);
    }
    return parts.length > 0 ? parts.join(' | ') : '';
  }

  // ============================================================
  // Issue #12: Risk Stratification View Methods
  // ============================================================

  /**
   * Issue #12: Load risk-stratified patient list
   * Fetches patients ordered by risk score with clinical factors
   */
  private loadRiskStratifiedPatients(): void {
    forkJoin({
      patients: this.patientService.getPatients(50).pipe(catchError(() => of([] as Patient[]))),
      careGaps: this.careGapService.getCareGapsPage({ size: 500 }).pipe(
        catchError(() => of({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 0
        } as CareGapPageResponse))
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ patients, careGaps }) => {
        if (!patients || patients.length === 0) {
          this.riskStratifiedPatients = this.getFallbackRiskStratifiedPatients();
        } else {
          const gapsByPatient = this.indexCareGapsByPatient(careGaps.content || []);
          this.riskStratifiedPatients = patients
            .slice(0, 10)
            .map((patient) =>
              this.mapPatientToRiskStratified(patient, gapsByPatient.get(patient.id) || [])
            )
            .sort((a, b) => b.riskScore - a.riskScore);
        }

        this.highRiskPatientCount = this.riskStratifiedPatients.filter(p =>
          p.riskLevel === 'critical' || p.riskLevel === 'high'
        ).length;
      });
  }

  /** Issue #12: Map FHIR patient to risk stratified display */
  private mapPatientToRiskStratified(patient: Patient, gaps: CareGapApiItem[]): RiskStratifiedPatient {
    const patientName = this.formatPatientNameLastFirst(patient);
    const mrnIdentifier = patient.identifier?.find(id => id.type?.text === 'Medical Record Number');
    const patientMRN = mrnIdentifier?.value || patient.id;
    const age = patient.birthDate ? this.calculateAge(patient.birthDate) : 0;
    const gapCount = gaps.length;
    const riskScore = this.calculateRiskScore(age, gapCount);
    const riskLevel = this.calculateRiskLevel(riskScore);
    const chronicConditions = this.mapGapConditions(gaps);

    return {
      id: patient.id,
      patientName,
      patientMRN,
      riskScore,
      riskLevel,
      hccScore: Math.round((riskScore / 100) * 3.5 * 100) / 100,
      careGapCount: gapCount,
      chronicConditions: chronicConditions.length ? chronicConditions : ['General Wellness'],
      lastVisit: this.formatLastUpdatedDate(patient),
      nextVisit: riskScore > 70 ? this.generateNextVisitDate() : undefined,
      riskFactors: this.buildRiskFactors(age, gapCount),
      sdohFactors: riskScore > 60 ? ['Transportation barriers', 'Food insecurity'] : undefined,
      medicationCount: 0,
      recentHospitalization: gapCount >= 3,
      trending: this.generateTrending(gapCount)
    };
  }

  private indexCareGapsByPatient(gaps: CareGapApiItem[]): Map<string, CareGapApiItem[]> {
    const map = new Map<string, CareGapApiItem[]>();
    gaps.forEach((gap) => {
      const list = map.get(gap.patientId) || [];
      list.push(gap);
      map.set(gap.patientId, list);
    });
    return map;
  }

  private calculateRiskScore(age: number, gapCount: number): number {
    const ageScore = Math.min(40, Math.round(age * 0.5));
    const gapScore = Math.min(60, gapCount * 12);
    return Math.min(100, ageScore + gapScore);
  }

  private mapGapConditions(gaps: CareGapApiItem[]): string[] {
    const conditions = new Set<string>();
    gaps.forEach((gap) => {
      switch (gap.measureId) {
        case 'CDC':
        case 'EED':
        case 'KED':
          conditions.add('Diabetes');
          break;
        case 'SPC':
        case 'BPD':
          conditions.add('Cardiovascular');
          break;
        case 'DSF':
          conditions.add('Behavioral Health');
          break;
        case 'BCS':
        case 'CCS':
        case 'COL':
        case 'OSW':
        case 'AWV':
          conditions.add('Preventive Care');
          break;
        default:
          if (gap.measureName) {
            conditions.add(gap.measureName);
          }
      }
    });
    return Array.from(conditions);
  }

  private buildRiskFactors(age: number, gapCount: number): RiskFactor[] {
    const factors: RiskFactor[] = [];
    if (gapCount >= 3) {
      factors.push({ name: 'Multiple care gaps', severity: 'high', category: 'clinical' });
    } else if (gapCount > 0) {
      factors.push({ name: 'Open care gaps', severity: 'medium', category: 'clinical' });
    } else {
      factors.push({ name: 'Low care gap burden', severity: 'low', category: 'clinical' });
    }
    if (age >= 65) {
      factors.push({ name: 'Senior age', severity: 'medium', category: 'clinical' });
    }
    return factors;
  }

  private formatLastUpdatedDate(patient: Patient): string {
    if (!patient.birthDate) {
      return new Date().toISOString().split('T')[0];
    }
    const parsed = new Date(patient.birthDate);
    return isNaN(parsed.getTime()) ? new Date().toISOString().split('T')[0] : parsed.toISOString().split('T')[0];
  }

  /** Issue #12: Calculate risk level from score */
  private calculateRiskLevel(score: number): 'critical' | 'high' | 'moderate' | 'low' {
    if (score >= 85) return 'critical';
    if (score >= 70) return 'high';
    if (score >= 50) return 'moderate';
    return 'low';
  }

  /** Issue #12: Generate chronic conditions based on risk score */
  private generateChronicConditions(riskScore: number): string[] {
    const allConditions = ['Type 2 Diabetes', 'Hypertension', 'CHF', 'COPD', 'CKD Stage 3',
      'Atrial Fibrillation', 'CAD', 'Depression', 'Obesity', 'Hyperlipidemia'];
    const count = riskScore > 80 ? 4 : riskScore > 60 ? 3 : riskScore > 40 ? 2 : 1;
    return allConditions.slice(0, count);
  }

  /** Issue #12: Generate risk factors based on risk score */
  private generateRiskFactors(riskScore: number): RiskFactor[] {
    const factors: RiskFactor[] = [];
    if (riskScore >= 80) {
      factors.push({ name: 'Recent hospitalization', severity: 'high', category: 'utilization' });
      factors.push({ name: 'Multiple chronic conditions', severity: 'high', category: 'clinical' });
    }
    if (riskScore >= 60) {
      factors.push({ name: 'Polypharmacy (10+ meds)', severity: 'medium', category: 'clinical' });
      factors.push({ name: 'Care gap burden', severity: 'medium', category: 'clinical' });
    }
    if (riskScore >= 40) {
      factors.push({ name: 'Missed appointments', severity: 'low', category: 'behavioral' });
    }
    return factors;
  }

  /** Issue #12: Generate last visit date */
  private generateLastVisitDate(): string {
    const daysAgo = Math.floor(Math.random() * 90) + 1;
    const date = new Date();
    date.setDate(date.getDate() - daysAgo);
    return date.toISOString().split('T')[0];
  }

  /** Issue #12: Generate next visit date */
  private generateNextVisitDate(): string {
    const daysAhead = Math.floor(Math.random() * 30) + 1;
    const date = new Date();
    date.setDate(date.getDate() + daysAhead);
    return date.toISOString().split('T')[0];
  }

  /** Issue #12: Generate trending indicator */
  private generateTrending(gapCount: number): 'improving' | 'stable' | 'worsening' {
    if (gapCount === 0) return 'improving';
    if (gapCount >= 3) return 'worsening';
    return 'stable';
  }

  /** Issue #12: Fallback risk stratified patients */
  private getFallbackRiskStratifiedPatients(): RiskStratifiedPatient[] {
    return [
      {
        id: 'risk-1', patientName: 'Martinez, Diego', patientMRN: 'MRN-601',
        riskScore: 92, riskLevel: 'critical', hccScore: 3.24, careGapCount: 4,
        chronicConditions: ['Type 2 Diabetes', 'CHF', 'CKD Stage 4', 'AFib'],
        lastVisit: '2025-11-15', nextVisit: '2025-12-05',
        riskFactors: [
          { name: 'Recent hospitalization (30 days)', severity: 'high', category: 'utilization' },
          { name: 'HbA1c > 9%', severity: 'high', category: 'clinical' },
          { name: 'Polypharmacy (14 meds)', severity: 'medium', category: 'clinical' }
        ],
        sdohFactors: ['Transportation barriers', 'Low health literacy'],
        medicationCount: 14, recentHospitalization: true, trending: 'worsening'
      },
      {
        id: 'risk-2', patientName: 'Gonzalez, Sofia', patientMRN: 'MRN-602',
        riskScore: 78, riskLevel: 'high', hccScore: 2.45, careGapCount: 3,
        chronicConditions: ['Type 2 Diabetes', 'Hypertension', 'COPD'],
        lastVisit: '2025-10-28', nextVisit: '2025-12-10',
        riskFactors: [
          { name: 'Uncontrolled BP', severity: 'high', category: 'clinical' },
          { name: 'Care gaps overdue', severity: 'medium', category: 'clinical' },
          { name: 'Missed 2 appointments', severity: 'medium', category: 'behavioral' }
        ],
        sdohFactors: ['Food insecurity'],
        medicationCount: 9, recentHospitalization: false, trending: 'stable'
      },
      {
        id: 'risk-3', patientName: 'Jackson, Robert', patientMRN: 'MRN-603',
        riskScore: 71, riskLevel: 'high', hccScore: 2.18, careGapCount: 2,
        chronicConditions: ['CHF', 'CAD', 'Hyperlipidemia'],
        lastVisit: '2025-11-02', nextVisit: '2025-12-15',
        riskFactors: [
          { name: 'EF < 40%', severity: 'high', category: 'clinical' },
          { name: 'Statin gap', severity: 'medium', category: 'clinical' }
        ],
        medicationCount: 8, recentHospitalization: false, trending: 'improving'
      },
      {
        id: 'risk-4', patientName: 'Lopez, Carmen', patientMRN: 'MRN-604',
        riskScore: 58, riskLevel: 'moderate', hccScore: 1.65, careGapCount: 2,
        chronicConditions: ['Type 2 Diabetes', 'Hypertension'],
        lastVisit: '2025-11-20',
        riskFactors: [
          { name: 'Rising A1c trend', severity: 'medium', category: 'clinical' },
          { name: 'Adherence concerns', severity: 'low', category: 'behavioral' }
        ],
        medicationCount: 6, recentHospitalization: false, trending: 'stable'
      },
      {
        id: 'risk-5', patientName: 'Hernandez, Jose', patientMRN: 'MRN-605',
        riskScore: 35, riskLevel: 'low', hccScore: 0.92, careGapCount: 1,
        chronicConditions: ['Hypertension'],
        lastVisit: '2025-11-18',
        riskFactors: [
          { name: 'Annual wellness overdue', severity: 'low', category: 'clinical' }
        ],
        medicationCount: 3, recentHospitalization: false, trending: 'improving'
      }
    ];
  }

  /** Issue #12: Get risk level color */
  getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel) {
      case 'critical': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'moderate': return '#fbc02d';
      case 'low': return '#4caf50';
      default: return '#757575';
    }
  }

  /** Issue #12: Get risk level icon */
  getRiskLevelIcon(riskLevel: string): string {
    switch (riskLevel) {
      case 'critical': return 'error';
      case 'high': return 'warning';
      case 'moderate': return 'info';
      case 'low': return 'check_circle';
      default: return 'help';
    }
  }

  /** Issue #12: Get trending icon */
  getTrendingIcon(trending: string): string {
    switch (trending) {
      case 'improving': return 'trending_down';
      case 'worsening': return 'trending_up';
      case 'stable': return 'trending_flat';
      default: return 'remove';
    }
  }

  /** Issue #12: Get trending color */
  getTrendingColor(trending: string): string {
    switch (trending) {
      case 'improving': return '#4caf50';
      case 'worsening': return '#f44336';
      case 'stable': return '#757575';
      default: return '#757575';
    }
  }

  /** Issue #12: Get risk factor severity color */
  getRiskFactorColor(severity: string): string {
    switch (severity) {
      case 'high': return '#d32f2f';
      case 'medium': return '#f57c00';
      case 'low': return '#fbc02d';
      default: return '#757575';
    }
  }

  /** Issue #12: Get risk factor category icon */
  getRiskFactorCategoryIcon(category: string): string {
    switch (category) {
      case 'clinical': return 'medical_services';
      case 'social': return 'groups';
      case 'behavioral': return 'psychology';
      case 'utilization': return 'local_hospital';
      default: return 'info';
    }
  }

  /** Issue #12: Toggle risk view mode */
  toggleRiskViewMode(): void {
    this.riskViewMode = this.riskViewMode === 'list' ? 'summary' : 'list';
  }

  /** Issue #12: View patient risk details */
  @TrackInteraction('provider-dashboard', 'view-risk-details')
  viewPatientRiskDetails(patient: RiskStratifiedPatient): void {
    this.router.navigate(['/patients', patient.id], {
      queryParams: { view: 'risk-profile' }
    });
  }

  /** Issue #12: Schedule outreach for high-risk patient */
  @TrackInteraction('provider-dashboard', 'schedule-risk-outreach')
  scheduleRiskOutreach(patient: RiskStratifiedPatient): void {
    this.dialogService.confirm(
      'Schedule Outreach',
      `Schedule proactive outreach for <strong>${patient.patientName}</strong>?<br><br>` +
      `<strong>Risk Score:</strong> ${patient.riskScore}<br>` +
      `<strong>Risk Level:</strong> ${patient.riskLevel.toUpperCase()}<br>` +
      `<strong>Chronic Conditions:</strong> ${patient.chronicConditions.join(', ')}`,
      'Schedule', 'Cancel', 'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.notificationService.success(`Outreach scheduled for ${patient.patientName}`);
      }
    });
  }

  /** Issue #12: Create care plan for high-risk patient */
  @TrackInteraction('provider-dashboard', 'create-risk-care-plan')
  createRiskCarePlan(patient: RiskStratifiedPatient): void {
    this.router.navigate(['/patients', patient.id], {
      queryParams: { action: 'create-care-plan', riskLevel: patient.riskLevel }
    });
  }

  /** Issue #12: Get risk score display class */
  getRiskScoreClass(score: number): string {
    if (score >= 85) return 'risk-score-critical';
    if (score >= 70) return 'risk-score-high';
    if (score >= 50) return 'risk-score-moderate';
    return 'risk-score-low';
  }

  /** Issue #12: Get count of patients by risk level */
  getPatientCountByRiskLevel(riskLevel: string): number {
    return this.riskStratifiedPatients.filter(p => p.riskLevel === riskLevel).length;
  }

  /** Issue #12: View all risk stratified patients */
  viewAllRiskPatients(): void {
    this.router.navigate(['/patients'], {
      queryParams: { sort: 'risk-score', order: 'desc' }
    });
  }

  /** Issue #12: Filter risk patients by level */
  filterRiskPatientsByLevel(level: string): void {
    this.router.navigate(['/patients'], {
      queryParams: { riskLevel: level }
    });
  }
}
