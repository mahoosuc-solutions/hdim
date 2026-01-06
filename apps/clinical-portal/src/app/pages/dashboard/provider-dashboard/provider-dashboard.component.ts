/**
 * Provider Dashboard Component
 *
 * Optimized for Provider (MD/DO/PA/NP) workflows:
 * - High-priority care gaps requiring clinical decisions
 * - Quality measure performance tracking
 * - Results review and clinical actions
 * - Patient panel overview
 */

import { Component, OnInit, OnDestroy } from '@angular/core';
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
import { Subject, takeUntil, forkJoin, of, from } from 'rxjs';
import { catchError, map, mergeMap, toArray } from 'rxjs/operators';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { CareGapService, CareGap, GapPriority } from '../../../services/care-gap.service';
import { EvaluationService } from '../../../services/evaluation.service';
import { PatientService } from '../../../services/patient.service';
import { Patient } from '../../../models/patient.model';
import { TrackInteraction } from '../../../utils/ai-tracking.decorator';

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

export interface PendingResult {
  id: string;
  patientName: string;
  patientMRN: string;
  resultType: string;
  date: string;
  abnormal: boolean;
  requiresReview: boolean;
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
    StatCardComponent,
    PageHeaderComponent,
    EmptyStateComponent
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

  constructor(
    private router: Router,
    private dialogService: DialogService,
    private notificationService: NotificationService,
    private careGapService: CareGapService,
    private evaluationService: EvaluationService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
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
        const patientLookups$ = uniquePatientIds.map(patientId =>
          this.patientService.getPatient(patientId).pipe(
            map(patient => ({ patientId, patient })),
            catchError(() => of({ patientId, patient: null as Patient | null }))
          )
        );

        return forkJoin(patientLookups$).pipe(
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
        console.warn('Failed to load care gaps from API, using fallback data', error);
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
        console.warn('Failed to load quality measures from API, using fallback data', error);
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
        console.warn('Failed to load pending results from API, using fallback data', error);
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

    return {
      id: result.id,
      patientName,
      patientMRN,
      resultType: result.measureId || 'Quality Measure Result',
      date: result.evaluationDate || new Date().toISOString().split('T')[0],
      abnormal: result.complianceRate < 70,
      requiresReview: true
    };
  }

  /**
   * Fallback pending results when API is unavailable
   */
  private getFallbackPendingResults(): PendingResult[] {
    return [
      {
        id: '1',
        patientName: 'Lopez, Carmen',
        patientMRN: 'MRN-401',
        resultType: 'Lab - Comprehensive Metabolic Panel',
        date: '2025-11-24',
        abnormal: true,
        requiresReview: true
      },
      {
        id: '2',
        patientName: 'Hernandez, Jose',
        patientMRN: 'MRN-402',
        resultType: 'Radiology - Chest X-Ray',
        date: '2025-11-24',
        abnormal: false,
        requiresReview: true
      },
      {
        id: '3',
        patientName: 'Gonzalez, Sofia',
        patientMRN: 'MRN-403',
        resultType: 'Lab - Lipid Panel',
        date: '2025-11-25',
        abnormal: true,
        requiresReview: true
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
   */
  addressCareGap(gap: HighPriorityCareGap): void {
    console.log('Addressing care gap:', gap.gapType);
    this.router.navigate(['/patients', gap.id], {
      queryParams: { action: 'clinical-review', gapId: gap.id }
    });
  }

  /**
   * Review lab/imaging results
   */
  reviewResult(result: PendingResult): void {
    console.log('Reviewing result:', result.resultType);
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
    // Mock data for demonstration - in production, this would call a service
    const dateStr = date.toISOString().split('T')[0];
    const today = new Date().toISOString().split('T')[0];

    if (dateStr === today) {
      return [
        {
          id: '1',
          patientName: 'Johnson, Sarah',
          patientMRN: 'MRN-501',
          startTime: '09:00',
          endTime: '09:30',
          date: dateStr,
          type: 'Follow-up',
          status: 'scheduled'
        },
        {
          id: '2',
          patientName: 'Williams, Michael',
          patientMRN: 'MRN-502',
          startTime: '10:00',
          endTime: '10:30',
          date: dateStr,
          type: 'Annual Physical',
          status: 'scheduled'
        },
        {
          id: '3',
          patientName: 'Brown, Jennifer',
          patientMRN: 'MRN-503',
          startTime: '11:00',
          endTime: '11:30',
          date: dateStr,
          type: 'Diabetes Management',
          status: 'scheduled'
        }
      ];
    }

    return [];
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

    return existingAppointments.some(existing => {
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
}
