import { Component, OnInit, OnDestroy, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, catchError, finalize, map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { PatientSummary } from '../../models/patient.model';
import { MeasureInfo } from '../../models/cql-library.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { MeasureService } from '../../services/measure.service';
import { PatientService } from '../../services/patient.service';
import { EvaluationService } from '../../services/evaluation.service';
import { CareGapService, CareGap } from '../../services/care-gap.service';
import { ScheduledEvaluationService } from '../../services/scheduled-evaluation.service';
import { ToastService } from '../../services/toast.service';
import { LoggerService } from '../../services/logger.service';

export interface BatchEvaluationResult {
  successCount: number;
  errorCount: number;
  results: QualityMeasureResult[];
  errors: { patientId: string; measureId: string; error: string }[];
  careGapsDetected?: number;
  careGaps?: CareGap[];
}

export interface BatchEvaluationDialogData {
  preSelectedPatients?: PatientSummary[];
  preSelectedMeasures?: string[];
}

@Component({
  selector: 'app-batch-evaluation-dialog',
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatInputModule,
    MatChipsModule,
    MatTooltipModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="batch-evaluation-dialog">
      <h2 mat-dialog-title>
        <mat-icon>analytics</mat-icon>
        Batch Quality Measure Evaluation
      </h2>

      <mat-dialog-content>
        @if (!isRunning() && !isComplete()) {
          <!-- Configuration Phase -->
          <div class="config-section">
            <!-- Measure Selection -->
            <div class="section-header">
              <h3>
                <mat-icon>assessment</mat-icon>
                Select Measures
              </h3>
              <span class="selection-info">{{ selectedMeasures.length }} measure(s) selected</span>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Quality Measures</mat-label>
              <mat-select [(value)]="selectedMeasures" multiple>
                @for (category of measureCategories; track category.value) {
                  <mat-optgroup [label]="category.label">
                    @for (measure of getMeasuresByCategory(category.value); track measure.id) {
                      <mat-option [value]="measure.name">
                        {{ measure.displayName }}
                      </mat-option>
                    }
                  </mat-optgroup>
                }
              </mat-select>
              <mat-icon matPrefix>medical_services</mat-icon>
              <mat-hint>Select one or more HEDIS measures to evaluate</mat-hint>
            </mat-form-field>

            <!-- Patient Selection -->
            <div class="section-header">
              <h3>
                <mat-icon>people</mat-icon>
                Select Patients
              </h3>
              <div class="table-actions">
                <button mat-stroked-button (click)="selectAll()" [disabled]="loadingPatients">
                  <mat-icon>select_all</mat-icon>
                  Select All
                </button>
                <button mat-stroked-button (click)="deselectAll()" [disabled]="loadingPatients">
                  <mat-icon>deselect</mat-icon>
                  Clear
                </button>
                <span class="selection-count">{{ selection.selected.length }} of {{ dataSource.data.length }} selected</span>
              </div>
            </div>

            @if (loadingPatients) {
              <div class="loading-patients">
                <mat-progress-bar mode="indeterminate"></mat-progress-bar>
                <p>Loading patients...</p>
              </div>
            } @else {
              <div class="patients-table-container">
                <table mat-table [dataSource]="dataSource" class="patients-table">
                  <ng-container matColumnDef="select">
                    <th mat-header-cell *matHeaderCellDef>
                      <mat-checkbox
                        (change)="$event ? masterToggle() : null"
                        [checked]="selection.hasValue() && isAllSelected()"
                        [indeterminate]="selection.hasValue() && !isAllSelected()">
                      </mat-checkbox>
                    </th>
                    <td mat-cell *matCellDef="let row">
                      <mat-checkbox
                        (click)="$event.stopPropagation()"
                        (change)="$event ? selection.toggle(row) : null"
                        [checked]="selection.isSelected(row)">
                      </mat-checkbox>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="name">
                    <th mat-header-cell *matHeaderCellDef>Patient Name</th>
                    <td mat-cell *matCellDef="let patient">{{ patient.fullName }}</td>
                  </ng-container>

                  <ng-container matColumnDef="mrn">
                    <th mat-header-cell *matHeaderCellDef>MRN</th>
                    <td mat-cell *matCellDef="let patient">{{ patient.mrn || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="age">
                    <th mat-header-cell *matHeaderCellDef>Age</th>
                    <td mat-cell *matCellDef="let patient">{{ patient.age || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="gender">
                    <th mat-header-cell *matHeaderCellDef>Gender</th>
                    <td mat-cell *matCellDef="let patient">{{ patient.gender }}</td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns; sticky: true"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"
                      (click)="selection.toggle(row)"
                      class="patient-row"></tr>
                </table>
              </div>
            }

            <!-- Evaluation Summary -->
            <div class="evaluation-summary">
              <mat-icon>info</mat-icon>
              <span>
                This will run <strong>{{ getTotalEvaluations() }}</strong> evaluations
                ({{ selection.selected.length }} patients × {{ selectedMeasures.length }} measures)
              </span>
            </div>
          </div>
        } @else if (isRunning()) {
          <!-- Running Phase -->
          <div class="progress-section">
            <mat-icon class="progress-icon spinning">sync</mat-icon>
            <h3>Evaluating Quality Measures...</h3>
            <p class="progress-detail">
              Processing {{ currentPatient() }} - {{ currentMeasure() }}
            </p>
            <p class="progress-count">
              {{ currentIndex() }} of {{ getTotalEvaluations() }} evaluations
            </p>
            <mat-progress-bar mode="determinate" [value]="progress()"></mat-progress-bar>
            <div class="progress-stats">
              <span class="stat success">
                <mat-icon>check_circle</mat-icon>
                {{ successCount() }} passed
              </span>
              <span class="stat error">
                <mat-icon>error</mat-icon>
                {{ errorCount() }} failed
              </span>
            </div>
          </div>
        } @else {
          <!-- Complete Phase -->
          <div class="results-section">
            <mat-icon class="success-icon">task_alt</mat-icon>
            <h3>Batch Evaluation Complete</h3>
            <div class="results-summary">
              <div class="result-card success">
                <mat-icon>check_circle</mat-icon>
                <div class="result-value">{{ successCount() }}</div>
                <div class="result-label">Successful</div>
              </div>
              <div class="result-card error">
                <mat-icon>error</mat-icon>
                <div class="result-value">{{ errorCount() }}</div>
                <div class="result-label">Failed</div>
              </div>
              <div class="result-card info">
                <mat-icon>percent</mat-icon>
                <div class="result-value">{{ getSuccessRate() | number:'1.0-0' }}%</div>
                <div class="result-label">Success Rate</div>
              </div>
            </div>

            <!-- Care Gap Detection Section -->
            @if (detectingCareGaps()) {
              <div class="care-gap-detecting">
                <mat-progress-bar mode="indeterminate"></mat-progress-bar>
                <p>Detecting care gaps from evaluation results...</p>
              </div>
            } @else if (careGapsDetected() > 0) {
              <div class="care-gap-alert" (click)="onViewCareGaps()">
                <mat-icon>warning_amber</mat-icon>
                <div class="care-gap-info">
                  <strong>{{ careGapsDetected() }} Care Gaps Identified</strong>
                  <span>Based on measure evaluation results. Click to review and manage.</span>
                </div>
                <mat-icon class="arrow">chevron_right</mat-icon>
              </div>
            } @else if (careGapCheckComplete()) {
              <div class="care-gap-success">
                <mat-icon>verified</mat-icon>
                <span>No new care gaps detected for evaluated patients</span>
              </div>
            }

            @if (errorCount() > 0) {
              <div class="error-details">
                <h4>Failed Evaluations</h4>
                <div class="error-list">
                  @for (error of errors.slice(0, 5); track $index) {
                    <div class="error-item">
                      <mat-icon>warning</mat-icon>
                      <span>{{ error.patientId }} - {{ error.measureId }}: {{ error.error }}</span>
                    </div>
                  }
                  @if (errors.length > 5) {
                    <div class="error-more">...and {{ errors.length - 5 }} more errors</div>
                  }
                </div>
              </div>
            }
          </div>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        @if (!isRunning()) {
          <button mat-button (click)="onCancel()">
            <mat-icon>close</mat-icon>
            {{ isComplete() ? 'Close' : 'Cancel' }}
          </button>
          @if (!isComplete()) {
            <button mat-stroked-button
              [disabled]="!canRun()"
              (click)="onSchedule()"
              matTooltip="Schedule this evaluation to run automatically">
              <mat-icon>schedule</mat-icon>
              Schedule
            </button>
            <app-loading-button
              [disabled]="!canRun()"
              (buttonClick)="onRun()"
              color="primary"
              icon="play_arrow"
              [matTooltip]="getRunTooltip()">
              Run Now
            </app-loading-button>
          } @else {
            <button mat-raised-button color="primary" (click)="onViewResults()">
              <mat-icon>visibility</mat-icon>
              View Results
            </button>
          }
        }
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .batch-evaluation-dialog { min-width: 800px; max-width: 900px; }
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 12px; margin: 0; padding: 16px 24px; background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%); color: white; margin: -24px -24px 24px -24px; }
    h2[mat-dialog-title] mat-icon { font-size: 28px; width: 28px; height: 28px; }
    .config-section { padding: 0; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; margin-top: 24px; }
    .section-header:first-child { margin-top: 0; }
    .section-header h3 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 16px; font-weight: 500; color: #333; }
    .section-header h3 mat-icon { color: #1976d2; }
    .selection-info { font-size: 13px; color: #666; }
    .table-actions { display: flex; gap: 8px; align-items: center; }
    .selection-count { margin-left: 8px; color: #666; font-size: 13px; }
    .loading-patients { text-align: center; padding: 32px; color: #666; }
    .patients-table-container { max-height: 280px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 8px; }
    .patients-table { width: 100%; }
    .patient-row { cursor: pointer; }
    .patient-row:hover { background: #f5f5f5; }
    .full-width { width: 100%; }
    .evaluation-summary { display: flex; align-items: center; gap: 8px; padding: 16px; background: #e3f2fd; border-radius: 8px; margin-top: 24px; color: #1565c0; }
    .evaluation-summary mat-icon { color: #1976d2; }
    .progress-section { text-align: center; padding: 48px 32px; }
    .progress-icon { font-size: 64px; width: 64px; height: 64px; color: #1976d2; }
    .progress-icon.spinning { animation: spin 1.5s linear infinite; }
    @keyframes spin { 100% { transform: rotate(360deg); } }
    .progress-detail { color: #666; margin: 8px 0; }
    .progress-count { font-weight: 500; margin-bottom: 16px; }
    .progress-stats { display: flex; justify-content: center; gap: 32px; margin-top: 16px; }
    .progress-stats .stat { display: flex; align-items: center; gap: 4px; font-size: 14px; }
    .progress-stats .stat.success { color: #4caf50; }
    .progress-stats .stat.error { color: #f44336; }
    .results-section { text-align: center; padding: 32px; }
    .success-icon { font-size: 72px; width: 72px; height: 72px; color: #4caf50; }
    .results-summary { display: flex; justify-content: center; gap: 24px; margin-top: 24px; }
    .result-card { padding: 20px 32px; border-radius: 12px; min-width: 120px; }
    .result-card mat-icon { font-size: 32px; width: 32px; height: 32px; }
    .result-card.success { background: #e8f5e9; color: #2e7d32; }
    .result-card.error { background: #ffebee; color: #c62828; }
    .result-card.info { background: #e3f2fd; color: #1565c0; }
    .result-value { font-size: 28px; font-weight: 600; margin: 8px 0 4px; }
    .result-label { font-size: 13px; opacity: 0.8; }
    .error-details { margin-top: 24px; text-align: left; background: #fff3e0; padding: 16px; border-radius: 8px; }
    .error-details h4 { margin: 0 0 12px; color: #e65100; font-size: 14px; }
    .error-list { max-height: 120px; overflow-y: auto; }
    .error-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; color: #bf360c; }
    .error-item mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .error-more { font-style: italic; color: #e65100; margin-top: 8px; }
    .care-gap-detecting { text-align: center; padding: 16px; margin-top: 20px; background: #fff3e0; border-radius: 8px; color: #e65100; }
    .care-gap-detecting p { margin: 12px 0 0; font-size: 14px; }
    .care-gap-alert { display: flex; align-items: center; gap: 16px; padding: 16px; margin-top: 20px; background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%); border: 1px solid #ffc107; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .care-gap-alert:hover { background: linear-gradient(135deg, #ffecb3 0%, #ffe082 100%); transform: translateX(4px); }
    .care-gap-alert > mat-icon:first-child { font-size: 32px; width: 32px; height: 32px; color: #f57c00; }
    .care-gap-info { flex: 1; }
    .care-gap-info strong { display: block; color: #e65100; font-size: 15px; }
    .care-gap-info span { color: #f57c00; font-size: 13px; }
    .care-gap-alert .arrow { color: #f57c00; }
    .care-gap-success { display: flex; align-items: center; gap: 12px; padding: 12px 16px; margin-top: 20px; background: #e8f5e9; border-radius: 8px; color: #2e7d32; font-size: 14px; }
    .care-gap-success mat-icon { color: #4caf50; }
  `],
})
export class BatchEvaluationDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  displayedColumns = ['select', 'name', 'mrn', 'age', 'gender'];
  dataSource = new MatTableDataSource<PatientSummary>([]);
  selection = new SelectionModel<PatientSummary>(true, []);

  // Available data
  availableMeasures: MeasureInfo[] = [];
  loadingPatients = false;
  loadingMeasures = false;

  // Selected values
  selectedMeasures: string[] = [];

  // Category groupings for measure dropdown
  measureCategories = [
    { value: 'PREVENTIVE', label: 'Preventive Care' },
    { value: 'CHRONIC_DISEASE', label: 'Chronic Disease' },
    { value: 'BEHAVIORAL_HEALTH', label: 'Behavioral Health' },
    { value: 'MEDICATION', label: 'Medication Management' },
    { value: 'WOMENS_HEALTH', label: "Women's Health" },
    { value: 'CHILD_ADOLESCENT', label: 'Child & Adolescent' },
    { value: 'CARE_COORDINATION', label: 'Care Coordination' },
    { value: 'OVERUSE', label: 'Overuse/Appropriateness' },
  ];

  // Progress tracking
  isRunning = signal(false);
  isComplete = signal(false);
  progress = signal(0);
  currentIndex = signal(0);
  currentPatient = signal('');
  currentMeasure = signal('');
  successCount = signal(0);
  errorCount = signal(0);

  // Care gap detection
  detectingCareGaps = signal(false);
  careGapCheckComplete = signal(false);
  careGapsDetected = signal(0);
  detectedCareGaps: CareGap[] = [];

  // Results
  results: QualityMeasureResult[] = [];
  errors: { patientId: string; measureId: string; error: string }[] = [];

  private get logger() {
    return this.loggerService.withContext('BatchEvaluationDialogComponent');
  }

  constructor(
    private dialogRef: MatDialogRef<BatchEvaluationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BatchEvaluationDialogData | null,
    private measureService: MeasureService,
    private patientService: PatientService,
    private evaluationService: EvaluationService,
    private careGapService: CareGapService,
    private scheduledEvaluationService: ScheduledEvaluationService,
    private toastService: ToastService,
    private router: Router,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadMeasures();
    this.loadPatients();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadMeasures(): void {
    this.loadingMeasures = true;
    this.measureService.getAllAvailableMeasures()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (measures) => {
          this.availableMeasures = measures;
          this.loadingMeasures = false;

          // Apply pre-selected measures if provided
          if (this.data?.preSelectedMeasures) {
            this.selectedMeasures = this.data.preSelectedMeasures;
          }
        },
        error: (err) => {
          this.logger.error('Failed to load measures', err);
          this.loadingMeasures = false;
        }
      });
  }

  private loadPatients(): void {
    this.loadingPatients = true;
    this.patientService.getPatientsSummary()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (patients) => {
          this.dataSource.data = patients;
          this.loadingPatients = false;

          // Apply pre-selected patients if provided
          if (this.data?.preSelectedPatients) {
            this.data.preSelectedPatients.forEach(p => {
              const found = patients.find(patient => patient.id === p.id);
              if (found) this.selection.select(found);
            });
          }
        },
        error: (err) => {
          this.logger.error('Failed to load patients', err);
          this.loadingPatients = false;
        }
      });
  }

  getMeasuresByCategory(category: string): MeasureInfo[] {
    return this.availableMeasures.filter(m => m.category === category);
  }

  isAllSelected(): boolean {
    return this.selection.selected.length === this.dataSource.data.length;
  }

  masterToggle(): void {
    this.isAllSelected()
      ? this.selection.clear()
      : this.dataSource.data.forEach(row => this.selection.select(row));
  }

  selectAll(): void {
    this.dataSource.data.forEach(row => this.selection.select(row));
  }

  deselectAll(): void {
    this.selection.clear();
  }

  getTotalEvaluations(): number {
    return this.selection.selected.length * this.selectedMeasures.length;
  }

  canRun(): boolean {
    return this.selection.selected.length > 0 && this.selectedMeasures.length > 0;
  }

  getRunTooltip(): string {
    if (this.selection.selected.length === 0) {
      return 'Select at least one patient';
    }
    if (this.selectedMeasures.length === 0) {
      return 'Select at least one measure';
    }
    return `Run ${this.getTotalEvaluations()} evaluations`;
  }

  getSuccessRate(): number {
    const total = this.successCount() + this.errorCount();
    return total > 0 ? (this.successCount() / total) * 100 : 0;
  }

  onRun(): void {
    this.isRunning.set(true);
    this.results = [];
    this.errors = [];
    this.successCount.set(0);
    this.errorCount.set(0);

    const patients = this.selection.selected;
    const measures = this.selectedMeasures;
    const totalEvals = patients.length * measures.length;
    let completed = 0;

    // Process evaluations sequentially to avoid overwhelming the backend
    this.processNextEvaluation(patients, measures, 0, 0, totalEvals);
  }

  /**
   * Schedule this batch evaluation to run automatically
   */
  onSchedule(): void {
    if (!this.canRun()) return;

    const measureNames = this.selectedMeasures.join(', ');
    const patientCount = this.selection.selected.length;
    const scheduleName = `Batch: ${measureNames.substring(0, 50)}${measureNames.length > 50 ? '...' : ''}`;

    // Create a daily schedule for these measures
    this.scheduledEvaluationService
      .createSchedule(scheduleName, this.selectedMeasures, 'daily', {
        description: `Evaluate ${this.selectedMeasures.length} measure(s) for ${patientCount} patient(s)`,
        patientFilter: {
          type: 'custom',
          patientIds: this.selection.selected.map((p) => p.id),
        },
      })
      .subscribe({
        next: (schedule) => {
          this.toastService.success(
            `Scheduled "${schedule.name}" to run ${this.scheduledEvaluationService.getDescription(schedule)}`
          );
          this.dialogRef.close({ scheduled: true, schedule });
        },
        error: (err) => {
          this.logger.error('Failed to create schedule', err);
          this.toastService.error('Failed to create schedule');
        },
      });
  }

  private processNextEvaluation(
    patients: PatientSummary[],
    measures: string[],
    patientIndex: number,
    measureIndex: number,
    total: number
  ): void {
    if (patientIndex >= patients.length) {
      // All evaluations done - now detect care gaps
      this.isRunning.set(false);
      this.isComplete.set(true);
      this.detectCareGapsForEvaluatedPatients(patients);
      return;
    }

    const patient = patients[patientIndex];
    const measureId = measures[measureIndex];
    const completed = patientIndex * measures.length + measureIndex + 1;

    this.currentIndex.set(completed);
    this.currentPatient.set(patient.fullName);
    this.currentMeasure.set(measureId);
    this.progress.set((completed / total) * 100);

    this.evaluationService.calculateQualityMeasure(patient.id, measureId)
      .pipe(
        takeUntil(this.destroy$),
        catchError((err) => {
          this.errors.push({
            patientId: patient.fullName,
            measureId: measureId,
            error: err.message || 'Evaluation failed'
          });
          this.errorCount.update(c => c + 1);
          return of(null);
        })
      )
      .subscribe((result) => {
        if (result) {
          this.results.push(result);
          this.successCount.update(c => c + 1);
        }

        // Move to next evaluation
        let nextMeasureIndex = measureIndex + 1;
        let nextPatientIndex = patientIndex;

        if (nextMeasureIndex >= measures.length) {
          nextMeasureIndex = 0;
          nextPatientIndex++;
        }

        // Small delay to prevent UI freezing
        setTimeout(() => {
          this.processNextEvaluation(patients, measures, nextPatientIndex, nextMeasureIndex, total);
        }, 50);
      });
  }

  onCancel(): void {
    if (this.isComplete()) {
      const result: BatchEvaluationResult = {
        successCount: this.successCount(),
        errorCount: this.errorCount(),
        results: this.results,
        errors: this.errors,
        careGapsDetected: this.careGapsDetected(),
        careGaps: this.detectedCareGaps,
      };
      this.dialogRef.close(result);
    } else {
      this.dialogRef.close(null);
    }
  }

  onViewResults(): void {
    const result: BatchEvaluationResult = {
      successCount: this.successCount(),
      errorCount: this.errorCount(),
      results: this.results,
      errors: this.errors,
      careGapsDetected: this.careGapsDetected(),
      careGaps: this.detectedCareGaps,
    };
    this.dialogRef.close(result);
  }

  /**
   * Detect care gaps for all evaluated patients
   * The backend automatically creates care gaps via Kafka when measures are calculated,
   * but we can also fetch them here to show the user immediately
   */
  private detectCareGapsForEvaluatedPatients(patients: PatientSummary[]): void {
    if (patients.length === 0) {
      this.careGapCheckComplete.set(true);
      return;
    }

    this.detectingCareGaps.set(true);
    const patientIds = patients.map(p => p.id);

    this.careGapService.detectGapsBatch(patientIds)
      .pipe(
        takeUntil(this.destroy$),
        catchError((err) => {
          this.logger.error('Failed to detect care gaps', err);
          // On error, try fetching existing gaps instead
          return this.fetchExistingCareGaps(patientIds);
        })
      )
      .subscribe({
        next: (result) => {
          // Flatten all patient gaps into a single array
          this.detectedCareGaps = result.patientGaps?.flatMap(pg => pg.gaps) || [];
          this.careGapsDetected.set(this.detectedCareGaps.length);
          this.detectingCareGaps.set(false);
          this.careGapCheckComplete.set(true);
        },
        error: () => {
          this.detectingCareGaps.set(false);
          this.careGapCheckComplete.set(true);
        }
      });
  }

  /**
   * Fallback: fetch existing care gaps if batch detection fails
   */
  private fetchExistingCareGaps(patientIds: string[]): import('rxjs').Observable<{ patientGaps: { patientId: string; gaps: CareGap[] }[] }> {
    // Fetch care gaps for each patient individually and combine results
    const requests = patientIds.map(id =>
      this.careGapService.getPatientCareGaps(id, true).pipe(
        catchError(() => of([] as CareGap[])),
        map(gaps => ({ patientId: id, gaps: gaps.filter(g => g.status === 'OPEN') }))
      )
    );

    return forkJoin(requests).pipe(
      map(results => ({ patientGaps: results }))
    );
  }

  /**
   * Navigate to care gap manager with the detected gaps
   */
  onViewCareGaps(): void {
    // Close dialog with results
    const result: BatchEvaluationResult = {
      successCount: this.successCount(),
      errorCount: this.errorCount(),
      results: this.results,
      errors: this.errors,
      careGapsDetected: this.careGapsDetected(),
      careGaps: this.detectedCareGaps,
    };
    this.dialogRef.close(result);

    // Navigate to care gap manager
    this.router.navigate(['/care-gaps']);
  }
}
