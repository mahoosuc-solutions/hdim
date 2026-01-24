import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule, MatTabGroup } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntil } from 'rxjs';
import { injectDestroy } from '../../shared/utils';
import { PatientService } from '../../services/patient.service';
import { FhirClinicalService, PatientClinicalData, Observation, Condition, Procedure } from '../../services/fhir-clinical.service';
import { EvaluationService } from '../../services/evaluation.service';
import { ContextNavigationService, NavigationContext, parseNavigationContext } from '../../services/context-navigation.service';
import { Patient } from '../../models/patient.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { PatientHealthOverviewComponent } from '../patient-health-overview/patient-health-overview.component';
import { PatientDemographicsCardComponent } from '../../components/patient-demographics-card/patient-demographics-card.component';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDividerModule,
    MatTooltipModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
    PatientHealthOverviewComponent,
    PatientDemographicsCardComponent,
  ],
  templateUrl: './patient-detail.component.html',
  styleUrls: ['./patient-detail.component.scss'],
})
export class PatientDetailComponent implements OnInit, AfterViewInit {
  private destroy$ = injectDestroy();

  @ViewChild('tabGroup') tabGroup!: MatTabGroup;

  patientId: string | null = null;
  patient: Patient | null = null;
  clinicalData: PatientClinicalData | null = null;
  qualityResults: QualityMeasureResult[] = [];

  loading = true;
  error: string | null = null;

  // Button loading states
  backButtonLoading = false;
  viewResultsLoading = false;

  // Display columns for tables
  observationsColumns: string[] = ['date', 'code', 'value'];
  conditionsColumns: string[] = ['status', 'code', 'onset'];
  proceduresColumns: string[] = ['status', 'code', 'performed'];
  resultsColumns: string[] = ['measure', 'compliant', 'date'];

  // Context-aware navigation (Issue #155)
  navigationContext: NavigationContext | null = null;
  highlightedCareGapId: string | null = null;
  highlightedResultId: string | null = null;
  selectedTabIndex = 0;

  // Tab name to index mapping
  private tabIndexMap: Record<string, number> = {
    'overview': 0,
    'clinical': 1,
    'care-gaps': 2,
    'results': 3
  };

  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private fhirClinicalService: FhirClinicalService,
    private evaluationService: EvaluationService,
    private contextNavService: ContextNavigationService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('PatientDetailComponent');
  }

  ngOnInit(): void {
    this.patientId = this.route.snapshot.paramMap.get('id');

    if (!this.patientId) {
      this.error = 'No patient ID provided';
      this.loading = false;
      return;
    }

    // Handle context-aware navigation (Issue #155)
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.navigationContext = parseNavigationContext(params);
      this.processNavigationContext();
    });

    this.loadPatientData();
  }

  ngAfterViewInit(): void {
    // Handle highlighting after view is ready
    if (this.navigationContext?.highlight) {
      setTimeout(() => this.scrollToHighlightedItem(), 500);
    }
  }

  /**
   * Process navigation context from query params (Issue #155)
   */
  private processNavigationContext(): void {
    if (!this.navigationContext) return;

    // Set the appropriate tab based on context
    if (this.navigationContext.tab) {
      const tabIndex = this.tabIndexMap[this.navigationContext.tab];
      if (tabIndex !== undefined) {
        this.selectedTabIndex = tabIndex;
      }
    }

    // Handle care gap highlighting
    if (this.navigationContext.careGapId) {
      this.highlightedCareGapId = this.navigationContext.careGapId;
      if (!this.navigationContext.tab) {
        this.selectedTabIndex = this.tabIndexMap['care-gaps'];
      }
    }

    // Handle result highlighting
    if (this.navigationContext.resultId) {
      this.highlightedResultId = this.navigationContext.resultId;
      if (!this.navigationContext.tab) {
        this.selectedTabIndex = this.tabIndexMap['results'];
      }
    }

    // Handle close-gap action
    if (this.navigationContext.action === 'close-gap' && this.navigationContext.careGapId) {
      this.initiateGapClosure(this.navigationContext.careGapId);
    }
  }

  /**
   * Scroll to highlighted item after view init
   */
  private scrollToHighlightedItem(): void {
    const elementId = this.highlightedCareGapId
      ? `care-gap-${this.highlightedCareGapId}`
      : this.highlightedResultId
        ? `result-${this.highlightedResultId}`
        : null;

    if (elementId) {
      const element = document.getElementById(elementId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        element.classList.add('highlighted');
        setTimeout(() => element.classList.remove('highlighted'), 3000);
      }
    }
  }

  /**
   * Initiate care gap closure workflow
   */
  private initiateGapClosure(careGapId: string): void {
    // This would open a dialog or panel for gap closure
    // Implementation depends on the care gap service integration
    this.logger.info('Initiating gap closure', careGapId);
  }

  /**
   * Check if a care gap row should be highlighted
   */
  isHighlightedCareGap(careGapId: string): boolean {
    return this.highlightedCareGapId === careGapId;
  }

  /**
   * Check if a result row should be highlighted
   */
  isHighlightedResult(resultId: string): boolean {
    return this.highlightedResultId === resultId;
  }

  private loadPatientData(): void {
    if (!this.patientId) return;

    this.loading = true;
    this.error = null;

    // Load patient demographics
    this.patientService.getPatient(this.patientId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (patient: Patient) => {
        this.patient = patient;
        this.loadClinicalData();
        this.loadQualityResults();
      },
      error: (err: any) => {
        this.logger.error('Error loading patient', err);
        this.error = 'Failed to load patient information';
        this.loading = false;
      },
    });
  }

  private loadClinicalData(): void {
    if (!this.patientId) return;

    this.fhirClinicalService.getPatientClinicalData(this.patientId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data: PatientClinicalData) => {
        this.clinicalData = data;
        this.loading = false;
      },
      error: (err: any) => {
        this.logger.error('Error loading clinical data', err);
        this.loading = false;
      },
    });
  }

  private loadQualityResults(): void {
    if (!this.patientId) return;

    this.evaluationService.getPatientResults(this.patientId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (results: QualityMeasureResult[]) => {
        this.qualityResults = results;
      },
      error: (err: any) => {
        this.logger.error('Error loading quality results', err);
      },
    });
  }

  // Helper methods for displaying data

  getPatientName(): string {
    if (!this.patient) return 'Unknown';
    return this.patientService.formatPatientName(this.patient);
  }

  getPatientAge(): number | undefined {
    if (!this.patient?.birthDate) return undefined;
    const today = new Date();
    const birth = new Date(this.patient.birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  getPatientMRN(): string | undefined {
    if (!this.patient) return undefined;
    return this.patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    )?.value;
  }

  getPatientMRNAuthority(): string | undefined {
    if (!this.patient) return undefined;
    return this.patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    )?.system;
  }

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

  formatObservationValue(obs: Observation): string {
    return this.fhirClinicalService.formatObservationValue(obs);
  }

  getObservationCode(obs: Observation): string {
    return this.fhirClinicalService.getObservationCodeDisplay(obs);
  }

  getConditionCode(condition: Condition): string {
    return this.fhirClinicalService.getConditionCodeDisplay(condition);
  }

  getConditionStatus(condition: Condition): string {
    return this.fhirClinicalService.getConditionStatus(condition);
  }

  getProcedureCode(procedure: Procedure): string {
    return this.fhirClinicalService.getProcedureCodeDisplay(procedure);
  }

  formatDate(dateString?: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  getComplianceLabel(compliant: boolean): string {
    return compliant ? 'Compliant' : 'Non-Compliant';
  }

  getComplianceChipColor(compliant: boolean): string {
    return compliant ? 'success' : 'warn';
  }

  goBack(): void {
    this.backButtonLoading = true;
    // Use context-aware navigation to return to origin (Issue #155)
    if (this.navigationContext?.returnUrl) {
      this.contextNavService.navigateBack('/patients');
    } else {
      this.router.navigate(['/patients']).then(() => {
        this.backButtonLoading = false;
      }).catch(() => {
        this.backButtonLoading = false;
      });
    }
  }

  navigateToResults(): void {
    this.viewResultsLoading = true;
    this.router.navigate(['/results'], {
      queryParams: { patient: this.patientId },
    }).then(() => {
      this.viewResultsLoading = false;
    }).catch(() => {
      this.viewResultsLoading = false;
    });
  }

  // Care gap identification (placeholder for future implementation)
  getCareGaps(): Array<{ measure: string; reason: string }> {
    const allMeasures = ['HEDIS_CDC', 'HEDIS_CBP', 'HEDIS_COL', 'HEDIS_BCS', 'HEDIS_CIS'];
    const completedMeasures = this.qualityResults.map((r) => r.measureId);

    return allMeasures
      .filter((m) => !completedMeasures.includes(m))
      .map((m) => ({
        measure: m,
        reason: 'Not yet evaluated',
      }));
  }

  hasCareGaps(): boolean {
    return this.getCareGaps().length > 0;
  }
}
