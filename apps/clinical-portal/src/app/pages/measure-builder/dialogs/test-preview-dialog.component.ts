import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Subscription } from 'rxjs';
import { timeout, catchError } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import { CustomMeasureService, TestPatientResult } from '../../../services/custom-measure.service';
import { LoggerService } from '../../../services/logger.service';

// Evaluation timeout in milliseconds (2 minutes)
const EVALUATION_TIMEOUT_MS = 120000;

export interface TestPreviewDialogData {
  measureId: string;
  measureName: string;
}

interface TestResult {
  patientId: string;
  patientName: string;
  mrn: string;
  outcome: 'pass' | 'fail' | 'not-eligible' | 'error';
  inPopulation: boolean;
  inDenominator: boolean;
  inNumerator: boolean;
  details: string[];
  executionTimeMs?: number;
}

@Component({
  selector: 'app-test-preview-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="test-dialog">
      <!-- Header -->
      <h2 mat-dialog-title>
        <mat-icon color="accent">science</mat-icon>
        Test Measure Preview
      </h2>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <div class="measure-info">
          <h3>{{ data.measureName }}</h3>
          <p>Testing against sample patient cohort</p>
        </div>

        <!-- Loading State -->
        @if (loading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Running measure evaluation...</p>
          </div>
        }

        <!-- Error Message -->
        @if (errorMessage) {
          <div class="error-banner">
            <mat-icon>info</mat-icon>
            <span>{{ errorMessage }}</span>
          </div>
        }

        <!-- Results Summary -->
        @if (!loading && testResults.length > 0) {
          <div class="results-summary">
            <div class="summary-card pass">
              <mat-icon>check_circle</mat-icon>
              <div>
                <div class="value">{{ getPassCount() }}</div>
                <div class="label">Passed</div>
              </div>
            </div>
            <div class="summary-card fail">
              <mat-icon>cancel</mat-icon>
              <div>
                <div class="value">{{ getFailCount() }}</div>
                <div class="label">Failed</div>
              </div>
            </div>
            <div class="summary-card not-eligible">
              <mat-icon>info</mat-icon>
              <div>
                <div class="value">{{ getNotEligibleCount() }}</div>
                <div class="label">Not Eligible</div>
              </div>
            </div>
            <div class="summary-card total">
              <mat-icon>people</mat-icon>
              <div>
                <div class="value">{{ testResults.length }}</div>
                <div class="label">Total Patients</div>
              </div>
            </div>
          </div>

          <!-- Detailed Results -->
          <div class="detailed-results">
            <h4>Patient Results</h4>
            <mat-accordion class="results-accordion">
              @for (result of testResults; track result.patientId) {
                <mat-expansion-panel>
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <div class="patient-header">
                        <mat-icon [class]="'outcome-icon ' + result.outcome">
                          {{ getOutcomeIcon(result.outcome) }}
                        </mat-icon>
                        <div class="patient-info">
                          <span class="patient-name">{{ result.patientName }}</span>
                          <span class="patient-mrn">MRN: {{ result.mrn }}</span>
                        </div>
                        <mat-chip [class]="'outcome-chip ' + result.outcome">
                          {{ result.outcome | uppercase }}
                        </mat-chip>
                      </div>
                    </mat-panel-title>
                  </mat-expansion-panel-header>

                  <div class="patient-details">
                    <!-- Population Criteria -->
                    <div class="criteria-section">
                      <h5>Population Criteria</h5>
                      <div class="criteria-item">
                        <mat-icon [class.met]="result.inPopulation">
                          {{ result.inPopulation ? 'check_circle' : 'cancel' }}
                        </mat-icon>
                        <span>Initial Population</span>
                      </div>
                      <div class="criteria-item">
                        <mat-icon [class.met]="result.inDenominator">
                          {{ result.inDenominator ? 'check_circle' : 'cancel' }}
                        </mat-icon>
                        <span>Denominator</span>
                      </div>
                      <div class="criteria-item">
                        <mat-icon [class.met]="result.inNumerator">
                          {{ result.inNumerator ? 'check_circle' : 'cancel' }}
                        </mat-icon>
                        <span>Numerator</span>
                      </div>
                    </div>

                    <!-- Evaluation Details -->
                    <div class="details-section">
                      <h5>Evaluation Details</h5>
                      <ul>
                        @for (detail of result.details; track detail) {
                          <li>{{ detail }}</li>
                        }
                      </ul>
                    </div>
                  </div>
                </mat-expansion-panel>
              }
            </mat-accordion>
          </div>
        }

        <!-- No Results -->
        @if (!loading && testResults.length === 0) {
          <div class="no-results">
            <mat-icon>info</mat-icon>
            <p>No test results available. Click "Run Test" to evaluate this measure.</p>
          </div>
        }
      </mat-dialog-content>

      <!-- Actions -->
      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close>
          <mat-icon>close</mat-icon>
          Close
        </button>
        <app-loading-button
          text="Run Test"
          icon="play_arrow"
          color="accent"
          variant="raised"
          [loading]="loading"
          loadingText="Testing..."
          ariaLabel="Run measure test"
          (buttonClick)="runTest()">
        </app-loading-button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .test-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }
    }

    .dialog-content {
      flex: 1;
      min-height: 0;
      padding: 24px;
      overflow-y: auto;
    }

    .measure-info {
      margin-bottom: 24px;

      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 500;
      }

      p {
        margin: 0;
        color: #666;
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;

      mat-spinner {
        margin-bottom: 16px;
      }

      p {
        color: #666;
      }
    }

    .results-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 16px;
      margin-bottom: 24px;

      .summary-card {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        border-radius: 8px;
        background-color: #f5f5f5;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        .value {
          font-size: 24px;
          font-weight: 600;
        }

        .label {
          font-size: 12px;
          color: #666;
          text-transform: uppercase;
        }

        &.pass {
          background-color: #e8f5e9;
          mat-icon { color: #2e7d32; }
          .value { color: #2e7d32; }
        }

        &.fail {
          background-color: #ffebee;
          mat-icon { color: #c62828; }
          .value { color: #c62828; }
        }

        &.not-eligible {
          background-color: #e3f2fd;
          mat-icon { color: #1565c0; }
          .value { color: #1565c0; }
        }

        &.total {
          background-color: #f3e5f5;
          mat-icon { color: #6a1b9a; }
          .value { color: #6a1b9a; }
        }
      }
    }

    .detailed-results {
      h4 {
        margin: 0 0 16px 0;
        font-size: 16px;
        font-weight: 600;
      }

      .results-accordion {
        .patient-header {
          display: flex;
          align-items: center;
          gap: 12px;
          width: 100%;

          .outcome-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;

            &.pass { color: #2e7d32; }
            &.fail { color: #c62828; }
            &.not-eligible { color: #1565c0; }
          }

          .patient-info {
            display: flex;
            flex-direction: column;
            flex: 1;

            .patient-name {
              font-weight: 500;
            }

            .patient-mrn {
              font-size: 12px;
              color: #666;
            }
          }

          .outcome-chip {
            font-size: 11px;
            min-height: 24px;

            &.pass { background-color: #e8f5e9; color: #2e7d32; }
            &.fail { background-color: #ffebee; color: #c62828; }
            &.not-eligible { background-color: #e3f2fd; color: #1565c0; }
          }
        }

        .patient-details {
          padding: 16px;

          .criteria-section,
          .details-section {
            margin-bottom: 16px;

            h5 {
              margin: 0 0 12px 0;
              font-size: 14px;
              font-weight: 600;
              color: #555;
            }
          }

          .criteria-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 0;

            mat-icon {
              font-size: 20px;
              width: 20px;
              height: 20px;
              color: #999;

              &.met {
                color: #2e7d32;
              }
            }
          }

          .details-section ul {
            margin: 0;
            padding-left: 20px;
            color: #666;

            li {
              margin-bottom: 8px;
            }
          }
        }
      }
    }

    .no-results {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #999;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        margin-bottom: 16px;
      }

      p {
        text-align: center;
        color: #666;
      }
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background-color: #fff3e0;
      border: 1px solid #ffb74d;
      border-radius: 4px;
      margin-bottom: 16px;
      color: #e65100;
      font-size: 13px;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      gap: 8px;
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      padding: 0;
    }
  `],
})
export class TestPreviewDialogComponent implements OnInit, OnDestroy {
  loading = false;
  testResults: TestResult[] = [];
  errorMessage: string | null = null;
  totalExecutionTimeMs = 0;
  private testSubscription?: Subscription;
  private logger = this.loggerService.withContext('TestPreviewDialogComponent');

  constructor(
    private dialogRef: MatDialogRef<TestPreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TestPreviewDialogData,
    private customMeasureService: CustomMeasureService,
    private loggerService: LoggerService
  ) {}

  ngOnDestroy(): void {
    // Cancel any pending test execution when dialog closes
    if (this.testSubscription) {
      this.testSubscription.unsubscribe();
    }
  }

  ngOnInit(): void {
    // Auto-run test on dialog open
    this.runTest();
  }

  /**
   * Run measure test against sample patients via backend API
   * Includes timeout protection to prevent hanging requests
   */
  runTest(): void {
    // Cancel any previous pending request
    if (this.testSubscription) {
      this.testSubscription.unsubscribe();
    }

    this.loading = true;
    this.errorMessage = null;

    this.testSubscription = this.customMeasureService.testMeasure(this.data.measureId).pipe(
      timeout(EVALUATION_TIMEOUT_MS),
      catchError((err) => {
        // Handle timeout specifically
        if (err.name === 'TimeoutError') {
          this.logger.warn('Test evaluation timed out after seconds', EVALUATION_TIMEOUT_MS / 1000);
          this.errorMessage = `Evaluation timed out after ${EVALUATION_TIMEOUT_MS / 1000} seconds. Using sample data.`;
        } else {
          this.logger.warn('Test API unavailable, using sample data', err);
          this.errorMessage = 'Using sample data (backend test endpoint not available)';
        }
        this.testResults = this.generateSampleResults();
        this.loading = false;
        // Return empty to complete the stream
        throw err;
      })
    ).subscribe({
      next: (result) => {
        // Map API results to component format
        this.testResults = result.results.map((r: TestPatientResult) => ({
          patientId: r.patientId,
          patientName: r.patientName,
          mrn: r.mrn,
          outcome: r.outcome,
          inPopulation: r.inPopulation,
          inDenominator: r.inDenominator,
          inNumerator: r.inNumerator,
          details: r.details,
          executionTimeMs: r.executionTimeMs,
        }));
        this.totalExecutionTimeMs = result.results.reduce(
          (sum: number, r: TestPatientResult) => sum + (r.executionTimeMs || 0), 0
        );
        this.loading = false;
      },
      error: () => {
        // Error already handled in catchError, just ensure loading is false
        this.loading = false;
      },
    });
  }

  /**
   * Generate sample test results (fallback when backend unavailable)
   */
  private generateSampleResults(): TestResult[] {
    return [
      {
        patientId: 'p1',
        patientName: 'John Smith',
        mrn: 'MRN-001',
        outcome: 'pass',
        inPopulation: true,
        inDenominator: true,
        inNumerator: true,
        details: [
          'Patient is in initial population (age 18-75)',
          'Patient meets denominator criteria (has diabetes diagnosis)',
          'Patient meets numerator criteria (HbA1c test in measurement period)',
        ],
      },
      {
        patientId: 'p2',
        patientName: 'Jane Doe',
        mrn: 'MRN-002',
        outcome: 'fail',
        inPopulation: true,
        inDenominator: true,
        inNumerator: false,
        details: [
          'Patient is in initial population (age 18-75)',
          'Patient meets denominator criteria (has diabetes diagnosis)',
          'Patient does NOT meet numerator criteria (no HbA1c test in measurement period)',
        ],
      },
      {
        patientId: 'p3',
        patientName: 'Robert Johnson',
        mrn: 'MRN-003',
        outcome: 'not-eligible',
        inPopulation: false,
        inDenominator: false,
        inNumerator: false,
        details: [
          'Patient is NOT in initial population (age 82, outside range)',
          'Excluded from measure evaluation',
        ],
      },
      {
        patientId: 'p4',
        patientName: 'Emily Williams',
        mrn: 'MRN-004',
        outcome: 'pass',
        inPopulation: true,
        inDenominator: true,
        inNumerator: true,
        details: [
          'Patient is in initial population (age 18-75)',
          'Patient meets denominator criteria (has diabetes diagnosis)',
          'Patient meets numerator criteria (HbA1c test in measurement period)',
        ],
      },
      {
        patientId: 'p5',
        patientName: 'Michael Brown',
        mrn: 'MRN-005',
        outcome: 'fail',
        inPopulation: true,
        inDenominator: true,
        inNumerator: false,
        details: [
          'Patient is in initial population (age 18-75)',
          'Patient meets denominator criteria (has diabetes diagnosis)',
          'Patient does NOT meet numerator criteria (HbA1c test outside measurement period)',
        ],
      },
    ];
  }

  /**
   * Get outcome icon
   */
  getOutcomeIcon(outcome: string): string {
    switch (outcome) {
      case 'pass': return 'check_circle';
      case 'fail': return 'cancel';
      case 'not-eligible': return 'info';
      default: return 'help';
    }
  }

  /**
   * Get count of passed results
   */
  getPassCount(): number {
    return this.testResults.filter(r => r.outcome === 'pass').length;
  }

  /**
   * Get count of failed results
   */
  getFailCount(): number {
    return this.testResults.filter(r => r.outcome === 'fail').length;
  }

  /**
   * Get count of not eligible results
   */
  getNotEligibleCount(): number {
    return this.testResults.filter(r => r.outcome === 'not-eligible').length;
  }
}
