import { Component, Inject, OnInit } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { CommonModule } from '@angular/common';
import { LoggerService } from '../../services/logger.service';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { LoggerService } from '../../services/logger.service';
import { MatButtonModule } from '@angular/material/button';
import { LoggerService } from '../../services/logger.service';
import { MatIconModule } from '@angular/material/icon';
import { LoggerService } from '../../services/logger.service';
import { MatTabsModule } from '@angular/material/tabs';
import { LoggerService } from '../../services/logger.service';
import { MatCardModule } from '@angular/material/card';
import { LoggerService } from '../../services/logger.service';
import { MatDividerModule } from '@angular/material/divider';
import { LoggerService } from '../../services/logger.service';
import { SavedReport } from '../../models/quality-result.model';
import { LoggerService } from '../../services/logger.service';
import { EvaluationService } from '../../services/evaluation.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Report Detail Dialog Component
 *
 * Displays the full details of a saved quality report including metadata
 * and parsed report data.
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(ReportDetailDialogComponent, {
 *   data: report,  // SavedReport object
 *   width: '900px'
 * });
 * ```
 */
@Component({
  selector: 'app-report-detail-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatCardModule,
    MatDividerModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title>
          <mat-icon [class]="'report-type-icon ' + report.reportType.toLowerCase()">
            {{ report.reportType === 'PATIENT' ? 'person' : 'groups' }}
          </mat-icon>
          {{ report.reportName }}
        </h2>
        <button mat-icon-button (click)="onClose()" class="close-button">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content>
        <mat-tab-group animationDuration="300ms">
          <!-- Overview Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">info</mat-icon>
              Overview
            </ng-template>
            <div class="tab-content">
              <!-- Metadata Card -->
              <mat-card class="metadata-card">
                <mat-card-header>
                  <mat-card-title>Report Information</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="metadata-grid">
                    <div class="metadata-item">
                      <mat-icon>badge</mat-icon>
                      <div class="metadata-content">
                        <label>Report ID</label>
                        <span class="value">{{ report.id }}</span>
                      </div>
                    </div>

                    <div class="metadata-item">
                      <mat-icon>category</mat-icon>
                      <div class="metadata-content">
                        <label>Report Type</label>
                        <span class="value">{{ report.reportType }}</span>
                      </div>
                    </div>

                    <div class="metadata-item">
                      <mat-icon>event</mat-icon>
                      <div class="metadata-content">
                        <label>Created</label>
                        <span class="value">{{ formatDateTime(report.createdAt) }}</span>
                      </div>
                    </div>

                    <div class="metadata-item">
                      <mat-icon>person</mat-icon>
                      <div class="metadata-content">
                        <label>Created By</label>
                        <span class="value">{{ report.createdBy }}</span>
                      </div>
                    </div>

                    <div class="metadata-item">
                      <mat-icon>check_circle</mat-icon>
                      <div class="metadata-content">
                        <label>Status</label>
                        <span class="value status-badge" [class]="report.status.toLowerCase()">
                          {{ report.status }}
                        </span>
                      </div>
                    </div>

                    @if (report.patientId) {
                      <div class="metadata-item">
                        <mat-icon>account_circle</mat-icon>
                        <div class="metadata-content">
                          <label>Patient ID</label>
                          <span class="value">{{ report.patientId }}</span>
                        </div>
                      </div>
                    }

                    @if (report.year) {
                      <div class="metadata-item">
                        <mat-icon>calendar_today</mat-icon>
                        <div class="metadata-content">
                          <label>Reporting Year</label>
                          <span class="value">{{ report.year }}</span>
                        </div>
                      </div>
                    }

                    <div class="metadata-item">
                      <mat-icon>business</mat-icon>
                      <div class="metadata-content">
                        <label>Tenant ID</label>
                        <span class="value">{{ report.tenantId }}</span>
                      </div>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Summary Card -->
              @if (parsedData) {
                <mat-card class="summary-card">
                  <mat-card-header>
                    <mat-card-title>Quality Summary</mat-card-title>
                  </mat-card-header>
                  <mat-card-content>
                    @if (report.reportType === 'PATIENT') {
                      <div class="summary-grid">
                        <div class="summary-item">
                          <div class="summary-value">{{ parsedData.qualityScore || 0 }}%</div>
                          <div class="summary-label">Quality Score</div>
                        </div>
                        <div class="summary-item">
                          <div class="summary-value">{{ parsedData.totalMeasures || 0 }}</div>
                          <div class="summary-label">Total Measures</div>
                        </div>
                        <div class="summary-item">
                          <div class="summary-value">{{ parsedData.compliantMeasures || 0 }}</div>
                          <div class="summary-label">Compliant</div>
                        </div>
                        @if (parsedData.gapsInCare) {
                          <div class="summary-item">
                            <div class="summary-value warning">{{ parsedData.gapsInCare.length || 0 }}</div>
                            <div class="summary-label">Care Gaps</div>
                          </div>
                        }
                      </div>
                    } @else if (report.reportType === 'POPULATION') {
                      <div class="summary-grid">
                        <div class="summary-item">
                          <div class="summary-value">{{ parsedData.totalPatients || 0 }}</div>
                          <div class="summary-label">Total Patients</div>
                        </div>
                        <div class="summary-item">
                          <div class="summary-value">{{ parsedData.overallCompliance || 0 }}%</div>
                          <div class="summary-label">Overall Compliance</div>
                        </div>
                        @if (parsedData.measureSummaries) {
                          <div class="summary-item">
                            <div class="summary-value">{{ parsedData.measureSummaries.length || 0 }}</div>
                            <div class="summary-label">Measures Tracked</div>
                          </div>
                        }
                      </div>
                    }
                  </mat-card-content>
                </mat-card>
              }
            </div>
          </mat-tab>

          <!-- Data Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">description</mat-icon>
              Report Data
            </ng-template>
            <div class="tab-content">
              @if (parsedData) {
                <!-- Patient Report Data -->
                @if (report.reportType === 'PATIENT') {
                  <!-- Measure Results -->
                  @if (parsedData.measureResults && parsedData.measureResults.length > 0) {
                    <mat-card class="data-card">
                      <mat-card-header>
                        <mat-card-title>Measure Results</mat-card-title>
                        <mat-card-subtitle>
                          {{ parsedData.measureResults.length }} measures evaluated
                        </mat-card-subtitle>
                      </mat-card-header>
                      <mat-card-content>
                        <div class="measures-list">
                          @for (measure of parsedData.measureResults; track measure.id || $index) {
                            <div class="measure-item">
                              <div class="measure-header">
                                <div class="measure-title">
                                  <mat-icon [class]="measure.numeratorCompliant ? 'compliant-icon' : 'non-compliant-icon'">
                                    {{ measure.numeratorCompliant ? 'check_circle' : 'cancel' }}
                                  </mat-icon>
                                  <span class="measure-name">{{ measure.measureName || measure.measureId }}</span>
                                </div>
                                <span class="measure-score">{{ measure.score }}%</span>
                              </div>
                              <div class="measure-details">
                                <span class="detail-badge">{{ measure.measureCategory }}</span>
                                <span class="detail-text">
                                  {{ measure.numeratorCompliant ? 'Compliant' : 'Non-compliant' }}
                                </span>
                              </div>
                            </div>
                          }
                        </div>
                      </mat-card-content>
                    </mat-card>
                  }

                  <!-- Care Gaps -->
                  @if (parsedData.gapsInCare && parsedData.gapsInCare.length > 0) {
                    <mat-card class="data-card care-gaps-card">
                      <mat-card-header>
                        <mat-card-title>
                          <mat-icon class="warning-icon">warning</mat-icon>
                          Care Gaps
                        </mat-card-title>
                        <mat-card-subtitle>
                          {{ parsedData.gapsInCare.length }} identified gaps
                        </mat-card-subtitle>
                      </mat-card-header>
                      <mat-card-content>
                        <div class="gaps-list">
                          @for (gap of parsedData.gapsInCare; track gap.measureId || $index) {
                            <div class="gap-item">
                              <div class="gap-header">
                                <mat-icon>health_and_safety</mat-icon>
                                <span class="gap-measure">{{ gap.measureName }}</span>
                              </div>
                              <div class="gap-description">{{ gap.description }}</div>
                              @if (gap.recommendation) {
                                <div class="gap-recommendation">
                                  <mat-icon class="rec-icon">lightbulb</mat-icon>
                                  <span>{{ gap.recommendation }}</span>
                                </div>
                              }
                            </div>
                          }
                        </div>
                      </mat-card-content>
                    </mat-card>
                  }
                }

                <!-- Population Report Data -->
                @if (report.reportType === 'POPULATION') {
                  @if (parsedData.measureSummaries && parsedData.measureSummaries.length > 0) {
                    <mat-card class="data-card">
                      <mat-card-header>
                        <mat-card-title>Measure Summaries</mat-card-title>
                        <mat-card-subtitle>
                          Population-wide quality metrics
                        </mat-card-subtitle>
                      </mat-card-header>
                      <mat-card-content>
                        <div class="population-measures">
                          @for (measure of parsedData.measureSummaries; track measure.measureId || $index) {
                            <div class="population-measure-item">
                              <div class="measure-info">
                                <div class="measure-name">{{ measure.measureName }}</div>
                                <div class="measure-stats">
                                  <span class="stat-item">
                                    <mat-icon>people</mat-icon>
                                    {{ measure.totalEligible }} eligible
                                  </span>
                                  <span class="stat-item">
                                    <mat-icon>check</mat-icon>
                                    {{ measure.totalCompliant }} compliant
                                  </span>
                                </div>
                              </div>
                              <div class="compliance-badge" [class.high-compliance]="measure.complianceRate >= 80">
                                {{ measure.complianceRate }}%
                              </div>
                            </div>
                          }
                        </div>
                      </mat-card-content>
                    </mat-card>
                  }
                }
              } @else {
                <div class="no-data">
                  <mat-icon>inbox</mat-icon>
                  <p>No report data available</p>
                </div>
              }
            </div>
          </mat-tab>

          <!-- Raw Data Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon">code</mat-icon>
              Raw JSON
            </ng-template>
            <div class="tab-content">
              <mat-card class="json-card">
                <mat-card-content>
                  <pre class="json-viewer">{{ report.reportData }}</pre>
                </mat-card-content>
              </mat-card>
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onExportCsv()">
          <mat-icon>download</mat-icon>
          Export CSV
        </button>
        <button mat-button (click)="onExportExcel()">
          <mat-icon>download</mat-icon>
          Export Excel
        </button>
        <button mat-raised-button color="primary" (click)="onClose()">
          <mat-icon>close</mat-icon>
          Close
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 700px;
        max-width: 900px;
      }

      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 24px 24px 16px;
        border-bottom: 1px solid #e0e0e0;

        h2[mat-dialog-title] {
          display: flex;
          align-items: center;
          gap: 12px;
          margin: 0;
          font-size: 24px;
          font-weight: 500;
          color: #1a1a1a;
          flex: 1;

          .report-type-icon {
            font-size: 32px;
            width: 32px;
            height: 32px;

            &.patient {
              color: #1976d2;
            }

            &.population {
              color: #388e3c;
            }
          }
        }

        .close-button {
          mat-icon {
            color: #666;
          }
        }
      }

      mat-dialog-content {
        padding: 0;
        max-height: 70vh;
        overflow: hidden;

        ::ng-deep .mat-mdc-tab-body-wrapper {
          max-height: calc(70vh - 48px);
          overflow-y: auto;
        }
      }

      .tab-icon {
        margin-right: 8px;
      }

      .tab-content {
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      /* Metadata Card */
      .metadata-card {
        .metadata-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
          gap: 20px;
          margin-top: 16px;
        }

        .metadata-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;

          mat-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;
            color: #1976d2;
            margin-top: 2px;
          }

          .metadata-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 4px;

            label {
              font-size: 12px;
              color: #666;
              font-weight: 500;
              text-transform: uppercase;
              letter-spacing: 0.5px;
            }

            .value {
              font-size: 14px;
              color: #1a1a1a;
              word-break: break-word;

              &.status-badge {
                display: inline-block;
                padding: 4px 12px;
                border-radius: 12px;
                font-weight: 600;
                font-size: 12px;
                text-transform: uppercase;

                &.completed {
                  background-color: #e8f5e9;
                  color: #2e7d32;
                }

                &.generating {
                  background-color: #fff3e0;
                  color: #e65100;
                }

                &.failed {
                  background-color: #ffebee;
                  color: #c62828;
                }
              }
            }
          }
        }
      }

      /* Summary Card */
      .summary-card {
        .summary-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
          gap: 20px;
          margin-top: 16px;
        }

        .summary-item {
          text-align: center;
          padding: 16px;
          background-color: #f5f5f5;
          border-radius: 8px;

          .summary-value {
            font-size: 32px;
            font-weight: 600;
            color: #1976d2;
            margin-bottom: 8px;

            &.warning {
              color: #f57c00;
            }
          }

          .summary-label {
            font-size: 13px;
            color: #666;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.5px;
          }
        }
      }

      /* Data Cards */
      .data-card {
        mat-card-header {
          margin-bottom: 16px;
        }
      }

      /* Measures List */
      .measures-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .measure-item {
          padding: 16px;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          background-color: #fafafa;

          .measure-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;

            .measure-title {
              display: flex;
              align-items: center;
              gap: 8px;
              flex: 1;

              mat-icon {
                font-size: 20px;
                width: 20px;
                height: 20px;

                &.compliant-icon {
                  color: #4caf50;
                }

                &.non-compliant-icon {
                  color: #f44336;
                }
              }

              .measure-name {
                font-weight: 500;
                color: #1a1a1a;
              }
            }

            .measure-score {
              font-size: 18px;
              font-weight: 600;
              color: #1976d2;
            }
          }

          .measure-details {
            display: flex;
            gap: 12px;
            align-items: center;

            .detail-badge {
              background-color: #e3f2fd;
              color: #1976d2;
              padding: 2px 8px;
              border-radius: 12px;
              font-size: 11px;
              font-weight: 600;
              text-transform: uppercase;
            }

            .detail-text {
              font-size: 13px;
              color: #666;
            }
          }
        }
      }

      /* Care Gaps */
      .care-gaps-card {
        mat-card-title {
          display: flex;
          align-items: center;
          gap: 8px;

          .warning-icon {
            color: #f57c00;
          }
        }

        .gaps-list {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .gap-item {
            padding: 16px;
            border-left: 4px solid #f57c00;
            background-color: #fff3e0;
            border-radius: 4px;

            .gap-header {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 8px;

              mat-icon {
                font-size: 20px;
                width: 20px;
                height: 20px;
                color: #f57c00;
              }

              .gap-measure {
                font-weight: 600;
                color: #1a1a1a;
              }
            }

            .gap-description {
              font-size: 14px;
              color: #555;
              margin-bottom: 8px;
              line-height: 1.5;
            }

            .gap-recommendation {
              display: flex;
              align-items: flex-start;
              gap: 8px;
              padding: 8px;
              background-color: #fff;
              border-radius: 4px;
              font-size: 13px;
              color: #333;

              .rec-icon {
                font-size: 18px;
                width: 18px;
                height: 18px;
                color: #ffa726;
                margin-top: 2px;
              }
            }
          }
        }
      }

      /* Population Measures */
      .population-measures {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .population-measure-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          background-color: #fafafa;

          .measure-info {
            flex: 1;

            .measure-name {
              font-weight: 500;
              color: #1a1a1a;
              margin-bottom: 8px;
            }

            .measure-stats {
              display: flex;
              gap: 16px;

              .stat-item {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 13px;
                color: #666;

                mat-icon {
                  font-size: 16px;
                  width: 16px;
                  height: 16px;
                }
              }
            }
          }

          .compliance-badge {
            font-size: 20px;
            font-weight: 600;
            color: #f57c00;
            padding: 8px 16px;
            background-color: #fff3e0;
            border-radius: 8px;

            &.high-compliance {
              color: #2e7d32;
              background-color: #e8f5e9;
            }
          }
        }
      }

      /* JSON Viewer */
      .json-card {
        .json-viewer {
          background-color: #2e3440;
          color: #d8dee9;
          padding: 20px;
          border-radius: 4px;
          font-family: 'Courier New', Courier, monospace;
          font-size: 13px;
          line-height: 1.6;
          overflow-x: auto;
          white-space: pre-wrap;
          word-wrap: break-word;
        }
      }

      /* No Data */
      .no-data {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        text-align: center;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #ccc;
          margin-bottom: 16px;
        }

        p {
          color: #666;
          margin: 0;
        }
      }

      /* Dialog Actions */
      mat-dialog-actions {
        padding: 16px 24px;
        border-top: 1px solid #e0e0e0;

        button {
          mat-icon {
            margin-right: 8px;
          }
        }
      }
    `,
  ],
})
export class ReportDetailDialogComponent implements OnInit {
  parsedData: any = null;

  constructor(
    private loggerService: LoggerService,
    @Inject(MAT_DIALOG_DATA) public report: SavedReport,
    private dialogRef: MatDialogRef<ReportDetailDialogComponent>,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
    this.parseReportData();
  }

  /**
   * Parse JSON report data
   */
  private parseReportData(): void {
    try {
      if (this.report.reportData) {
        this.parsedData = JSON.parse(this.report.reportData);
      }
    } catch (error) {
      this.logger.error('Error parsing report data:', { error });
      this.parsedData = null;
    }
  }

  /**
   * Format date and time
   */
  formatDateTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Export to CSV
   */
  onExportCsv(): void {
    this.evaluationService
      .exportAndDownloadReport(this.report.id, this.report.reportName, 'csv')
      .subscribe({
        next: () => {
          this.logger.info('CSV export successful');
        },
        error: (error) => {
          this.logger.error('Error exporting to CSV:', { error });
        },
      });
  }

  /**
   * Export to Excel
   */
  onExportExcel(): void {
    this.evaluationService
      .exportAndDownloadReport(this.report.id, this.report.reportName, 'excel')
      .subscribe({
        next: () => {
          this.logger.info('Excel export successful');
        },
        error: (error) => {
          this.logger.error('Error exporting to Excel:', { error });
        },
      });
  }

  /**
   * Close dialog
   */
  onClose(): void {
    this.dialogRef.close();
  }
}
