import { Component, OnInit, signal, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { SelectionModel } from '@angular/cdk/collections';
import { EvaluationService } from '../../services/evaluation.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { SavedReport, ReportType } from '../../models/quality-result.model';
import { PatientSelectionDialogComponent } from '../../components/dialogs/patient-selection-dialog.component';
import { YearSelectionDialogComponent } from '../../components/dialogs/year-selection-dialog.component';
import { ReportDetailDialogComponent } from '../../components/dialogs/report-detail-dialog.component';
import { ConfirmDialogComponent } from '../../components/dialogs/confirm-dialog.component';
import { ToastService } from '../../services/toast.service';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';

const INITIAL_SAVED_REPORTS: SavedReport[] = [
  {
    id: 'report-1',
    tenantId: 'TENANT001',
    reportName: 'Patient Report - 2024-01-02',
    reportType: 'PATIENT',
    status: 'COMPLETED',
    createdAt: '2024-01-02T00:00:00Z',
    createdBy: 'system',
    reportData: '{}',
    patientId: 'patient-abc',
  },
  {
    id: 'report-2',
    tenantId: 'TENANT001',
    reportName: 'Population Report 2024',
    reportType: 'POPULATION',
    status: 'COMPLETED',
    createdAt: '2024-02-15T00:00:00Z',
    createdBy: 'system',
    reportData: '{}',
    year: 2024,
  },
];

/**
 * ReportsComponent - Main component for generating and viewing quality reports
 *
 * Features:
 * - Tab navigation between Generate Reports and Saved Reports
 * - Generate patient and population reports
 * - View, export, and delete saved reports
 * - CSV and Excel export functionality
 */
@Component({
  selector: 'app-reports',
  imports: [
    CommonModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">
          <mat-icon class="page-icon">assessment</mat-icon>
          Quality Reports
        </h1>
        <p class="page-subtitle">
          Generate, view, and export quality measure reports
        </p>
      </div>

      <!-- Quick Action Bar - Prominent at top for easy access -->
      <div class="report-quick-actions">
        <h2 class="quick-actions-title">
          <mat-icon>flash_on</mat-icon>
          Generate New Report
        </h2>
        <div class="action-cards">
          <button class="action-card patient"
                  type="button"
                  (click)="onGeneratePatientReport()"
                  [disabled]="isGeneratingPatientReport()"
                  matTooltip="Generate a comprehensive quality report for an individual patient">
            <div class="action-card-icon">
              <mat-icon>person</mat-icon>
            </div>
            <div class="action-card-content">
              <span class="action-title">Patient Report</span>
              <span class="action-desc">Individual quality metrics</span>
            </div>
            @if (isGeneratingPatientReport()) {
              <mat-spinner diameter="24"></mat-spinner>
            } @else {
              <mat-icon class="action-arrow">arrow_forward</mat-icon>
            }
          </button>
          <button class="action-card population"
                  type="button"
                  (click)="onGeneratePopulationReport()"
                  [disabled]="isGeneratingPopulationReport()"
                  matTooltip="Generate aggregated quality metrics for all patients">
            <div class="action-card-icon">
              <mat-icon>groups</mat-icon>
            </div>
            <div class="action-card-content">
              <span class="action-title">Population Report</span>
              <span class="action-desc">Practice-wide analytics</span>
            </div>
            @if (isGeneratingPopulationReport()) {
              <mat-spinner diameter="24"></mat-spinner>
            } @else {
              <mat-icon class="action-arrow">arrow_forward</mat-icon>
            }
          </button>
        </div>
      </div>

      <mat-tab-group
        [(selectedIndex)]="selectedTabIndex"
        class="reports-tabs"
        animationDuration="300ms"
      >
        <!-- Generate Reports Tab -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon class="tab-icon">add_circle</mat-icon>
            Generate Reports
          </ng-template>
          <div class="tab-content">
            <div class="generation-grid">
              <!-- Patient Report Card -->
              <mat-card class="generation-card">
                <mat-card-header>
                  <mat-icon class="card-icon patient-icon">person</mat-icon>
                  <mat-card-title>Patient Report</mat-card-title>
                  <mat-card-subtitle>
                    Generate quality report for a specific patient
                  </mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <p class="card-description">
                    Comprehensive quality metrics for individual patient care,
                    including HEDIS measures, compliance scores, and care gaps.
                  </p>
                  <div class="card-features">
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Individual quality scores</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Care gap identification</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Measure compliance tracking</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Measure-by-measure breakdown</span>
                    </div>
                  </div>
                </mat-card-content>
                <mat-card-actions>
                  <app-loading-button
                    [loading]="isGeneratingPatientReport()"
                    [color]="'primary'"
                    [matTooltip]="'Select a patient and generate a comprehensive quality report'"
                    (buttonClick)="onGeneratePatientReport()">
                    <mat-icon>play_arrow</mat-icon>
                    Generate Patient Report
                  </app-loading-button>
                </mat-card-actions>
              </mat-card>

              <!-- Population Report Card -->
              <mat-card class="generation-card">
                <mat-card-header>
                  <mat-icon class="card-icon population-icon">groups</mat-icon>
                  <mat-card-title>Population Report</mat-card-title>
                  <mat-card-subtitle>
                    Generate aggregated quality report for all patients
                  </mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <p class="card-description">
                    Practice-wide quality metrics showing overall performance,
                    measure summaries, and population-level compliance rates.
                  </p>
                  <div class="card-features">
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Practice-wide compliance</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Measure summaries</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Year-over-year trends</span>
                    </div>
                  </div>
                </mat-card-content>
                <mat-card-actions>
                  <app-loading-button
                    [loading]="isGeneratingPopulationReport()"
                    [color]="'primary'"
                    [matTooltip]="'Generate aggregated quality metrics for all patients in the practice'"
                    (buttonClick)="onGeneratePopulationReport()">
                    <mat-icon>play_arrow</mat-icon>
                    Generate Population Report
                  </app-loading-button>
                </mat-card-actions>
              </mat-card>
            </div>
          </div>
        </mat-tab>

        <!-- Saved Reports Tab -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon class="tab-icon">folder</mat-icon>
            Saved Reports
          </ng-template>
          <div class="tab-content">
            <div class="saved-reports-container">
              <div class="saved-reports-header">
                <h2>Saved Reports</h2>
                <div class="filter-buttons">
                  <button
                    mat-button
                    [class.active]="selectedReportType() === null"
                    (click)="filterReports(null)"
                  >
                    <mat-icon>list</mat-icon>
                    All Reports
                  </button>
                  <button
                    mat-button
                    [class.active]="selectedReportType() === 'PATIENT'"
                    (click)="filterReports('PATIENT')"
                  >
                    <mat-icon>person</mat-icon>
                    Patient
                  </button>
                  <button
                    mat-button
                    [class.active]="selectedReportType() === 'POPULATION'"
                    (click)="filterReports('POPULATION')"
                  >
                    <mat-icon>groups</mat-icon>
                    Population
                  </button>
                </div>
              </div>

              @if (isLoadingReports()) {
                <div class="loading-container">
                  <mat-spinner></mat-spinner>
                  <p>Loading reports...</p>
                </div>
              } @else if (savedReports().length === 0) {
                <div class="empty-state">
                  <mat-icon class="empty-icon">description</mat-icon>
                  <h3>No Reports Found</h3>
                  <p>Generate your first report to get started</p>
                  <button
                    mat-raised-button
                    color="primary"
                    (click)="selectedTabIndex = 0"
                  >
                    <mat-icon>add</mat-icon>
                    Generate Report
                  </button>
                </div>
              } @else {
                <div class="reports-list" *ngIf="savedReports().length > 0">
                  <mat-card *ngFor="let report of savedReports()" class="report-list-card">
                    <mat-card-header>
                      <mat-icon class="report-type-icon" [ngClass]="report.reportType.toLowerCase()">
                        {{ report.reportType === 'PATIENT' ? 'person' : 'groups' }}
                      </mat-icon>
                      <div>
                        <mat-card-title>{{ report.reportName }}</mat-card-title>
                        <mat-card-subtitle>
                          {{ report.reportType === 'PATIENT' ? 'Patient Report' : 'Population Report' }}
                        </mat-card-subtitle>
                      </div>
                    </mat-card-header>
                    <mat-card-content>
                      <div class="report-meta">
                        <div class="meta-item">
                          <mat-icon>calendar_today</mat-icon>
                          <span>{{ formatDate(report.createdAt) }}</span>
                        </div>
                        <div class="meta-item">
                          <mat-icon>person</mat-icon>
                          <span>{{ report.createdBy }}</span>
                        </div>
                        <div
                          class="meta-item status-badge"
                          [class.completed]="report.status === 'COMPLETED'"
                          [class.generating]="report.status === 'GENERATING'"
                          [class.failed]="report.status === 'FAILED'"
                        >
                          <mat-icon>
                            {{ report.status === 'COMPLETED' ? 'check_circle' : 'hourglass_top' }}
                          </mat-icon>
                          <span>{{ report.status }}</span>
                        </div>
                      </div>
                    </mat-card-content>
                    <mat-card-actions>
                      <button mat-flat-button color="primary" type="button" (click)="onExportCsv(report)">
                        CSV
                      </button>
                      <button mat-flat-button color="primary" type="button" (click)="onExportExcel(report)">
                        Excel
                      </button>
                      <button mat-stroked-button color="accent" type="button" (click)="onViewReport(report)">
                        View
                      </button>
                    </mat-card-actions>
                  </mat-card>
                </div>

                <!-- Bulk Actions Toolbar -->
                @if (selection.hasValue()) {
                  <div class="bulk-actions-toolbar">
                    <div class="selection-info">
                      <mat-icon>check_circle</mat-icon>
                      <span>{{ getSelectionCount() }} report(s) selected</span>
                    </div>
                    <div class="bulk-actions">
                      <app-loading-button
                        text="Export Selected"
                        icon="download"
                        variant="raised"
                        color="primary"
                        ariaLabel="Export selected reports to CSV"
                        (buttonClick)="exportSelectedToCSV()">
                      </app-loading-button>
                      <app-loading-button
                        text="Delete Selected"
                        icon="delete"
                        variant="raised"
                        color="warn"
                        ariaLabel="Delete selected reports"
                        (buttonClick)="deleteSelected()">
                      </app-loading-button>
                      <app-loading-button
                        text="Clear Selection"
                        icon="clear"
                        variant="raised"
                        ariaLabel="Clear selection"
                        (buttonClick)="clearSelection()">
                      </app-loading-button>
                    </div>
                  </div>
                }

                <!-- Reports Table -->
                <div class="table-container">
                  <table mat-table [dataSource]="dataSource" matSort class="reports-table">
                    <!-- Checkbox Column -->
                    <ng-container matColumnDef="select">
                      <th mat-header-cell *matHeaderCellDef>
                        <mat-checkbox
                          (change)="$event ? masterToggle() : null"
                          [checked]="selection.hasValue() && isAllSelected()"
                          [indeterminate]="selection.hasValue() && !isAllSelected()"
                          [aria-label]="checkboxLabel()"
                          matTooltip="Select all reports">
                        </mat-checkbox>
                      </th>
                      <td mat-cell *matCellDef="let report">
                        <mat-checkbox
                          (click)="$event.stopPropagation()"
                          (change)="$event ? selection.toggle(report) : null"
                          [checked]="selection.isSelected(report)"
                          [aria-label]="checkboxLabel(report)"
                          matTooltip="Select this report">
                        </mat-checkbox>
                      </td>
                    </ng-container>

                    <!-- Report Name Column -->
                    <ng-container matColumnDef="reportName">
                      <th mat-header-cell *matHeaderCellDef mat-sort-header matTooltip="Name of the report">
                        Report Name
                      </th>
                      <td mat-cell *matCellDef="let report">
                        {{ report.reportName }}
                      </td>
                    </ng-container>

                    <!-- Report Type Column -->
                    <ng-container matColumnDef="reportType">
                      <th mat-header-cell *matHeaderCellDef mat-sort-header matTooltip="Type of report">
                        Type
                      </th>
                      <td mat-cell *matCellDef="let report">
                        <span class="report-type-badge" [class]="report.reportType.toLowerCase()">
                          <mat-icon>{{ report.reportType === 'PATIENT' ? 'person' : 'groups' }}</mat-icon>
                          {{ report.reportType }}
                        </span>
                      </td>
                    </ng-container>

                    <!-- Created At Column -->
                    <ng-container matColumnDef="createdAt">
                      <th mat-header-cell *matHeaderCellDef mat-sort-header matTooltip="Date and time the report was created">
                        Created
                      </th>
                      <td mat-cell *matCellDef="let report">
                        <div class="date-time">
                          <div>{{ formatDate(report.createdAt) }}</div>
                          <div class="time">{{ formatTime(report.createdAt) }}</div>
                        </div>
                      </td>
                    </ng-container>

                    <!-- Created By Column -->
                    <ng-container matColumnDef="createdBy">
                      <th mat-header-cell *matHeaderCellDef mat-sort-header matTooltip="User who created the report">
                        Created By
                      </th>
                      <td mat-cell *matCellDef="let report">
                        {{ report.createdBy }}
                      </td>
                    </ng-container>

                    <!-- Status Column -->
                    <ng-container matColumnDef="status">
                      <th mat-header-cell *matHeaderCellDef mat-sort-header matTooltip="Current status of the report">
                        Status
                      </th>
                      <td mat-cell *matCellDef="let report">
                        <span class="status-badge" [class]="report.status.toLowerCase()">
                          {{ report.status }}
                        </span>
                      </td>
                    </ng-container>

                    <!-- Actions Column -->
                    <ng-container matColumnDef="actions">
                      <th mat-header-cell *matHeaderCellDef matTooltip="Available actions">
                        Actions
                      </th>
                      <td mat-cell *matCellDef="let report">
                        <div class="action-buttons">
                          <button
                            mat-icon-button
                            (click)="onViewReport(report)"
                            [disabled]="report.status !== 'COMPLETED'"
                            matTooltip="View report details">
                            <mat-icon>visibility</mat-icon>
                          </button>
                          <button
                            mat-icon-button
                            (click)="onExportCsv(report)"
                            [disabled]="report.status !== 'COMPLETED'"
                            matTooltip="Download as CSV">
                            <mat-icon>download</mat-icon>
                          </button>
                          <button
                            mat-icon-button
                            (click)="onExportExcel(report)"
                            [disabled]="report.status !== 'COMPLETED'"
                            matTooltip="Download as Excel">
                            <mat-icon>table_chart</mat-icon>
                          </button>
                          <button
                            mat-icon-button
                            color="warn"
                            (click)="onDeleteReport(report)"
                            matTooltip="Delete report">
                            <mat-icon>delete</mat-icon>
                          </button>
                        </div>
                      </td>
                    </ng-container>

                    <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                    <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

                    <!-- No Data Row -->
                    <tr class="mat-row no-data-row" *matNoDataRow>
                      <td class="mat-cell" [attr.colspan]="displayedColumns.length">
                        <div class="no-data">
                          <mat-icon>description</mat-icon>
                          <p>No reports found for the selected filter</p>
                        </div>
                      </td>
                    </tr>
                  </table>

                  <!-- Paginator -->
                  <mat-paginator
                    [pageSizeOptions]="[10, 25, 50, 100]"
                    [pageSize]="25"
                    showFirstLastButtons
                    aria-label="Select page of reports">
                  </mat-paginator>
                </div>
              }
            </div>
          </div>
        </mat-tab>
      </mat-tab-group>

      <!-- Loading Overlay -->
      <app-loading-overlay [isLoading]="isLoadingReports()"></app-loading-overlay>
    </div>
  `,
  styles: [
    `
      .page-container {
        max-width: 1400px;
        margin: 0 auto;
        padding: 24px;
      }

      .page-header {
        margin-bottom: 32px;
      }

      .page-title {
        font-size: 32px;
        font-weight: 500;
        color: #1a1a1a;
        margin: 0 0 8px 0;
        display: flex;
        align-items: center;
        gap: 12px;

        .page-icon {
          font-size: 36px;
          width: 36px;
          height: 36px;
          color: #1976d2;
        }
      }

      .page-subtitle {
        font-size: 16px;
        color: #666;
        margin: 0;
      }

      /* Quick Action Bar */
      .report-quick-actions {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 12px;
        padding: 24px;
        margin-bottom: 24px;
        color: white;

        .quick-actions-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 20px;
          font-weight: 600;
          margin: 0 0 20px 0;
          color: white;

          mat-icon {
            color: #ffd54f;
          }
        }

        .action-cards {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 16px;
        }

        .action-card {
          display: flex;
          align-items: center;
          gap: 16px;
          padding: 20px 24px;
          background: rgba(255, 255, 255, 0.95);
          border: none;
          border-radius: 12px;
          cursor: pointer;
          transition: all 0.3s ease;
          text-align: left;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

          &:hover:not(:disabled) {
            transform: translateY(-3px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
          }

          &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
          }

          .action-card-icon {
            width: 56px;
            height: 56px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;

            mat-icon {
              font-size: 28px;
              width: 28px;
              height: 28px;
              color: white;
            }
          }

          &.patient .action-card-icon {
            background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
          }

          &.population .action-card-icon {
            background: linear-gradient(135deg, #388e3c 0%, #2e7d32 100%);
          }

          .action-card-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 4px;

            .action-title {
              font-size: 18px;
              font-weight: 600;
              color: #1a1a1a;
            }

            .action-desc {
              font-size: 14px;
              color: #666;
            }
          }

          .action-arrow {
            color: #1976d2;
            transition: transform 0.2s ease;
          }

          &:hover:not(:disabled) .action-arrow {
            transform: translateX(4px);
          }
        }
      }

      .reports-tabs {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .tab-icon {
        margin-right: 8px;
      }

      .tab-content {
        padding: 32px 24px;
      }

      /* Generate Reports Tab */
      .generation-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
        gap: 24px;
      }

      .generation-card {
        .card-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          margin-bottom: 16px;
        }

        .patient-icon {
          color: #1976d2;
        }

        .population-icon {
          color: #388e3c;
        }

        .card-description {
          color: #666;
          line-height: 1.6;
          margin-bottom: 20px;
        }

        .card-features {
          display: flex;
          flex-direction: column;
          gap: 12px;
          margin-bottom: 16px;

          .feature-item {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #333;

            mat-icon {
              font-size: 20px;
              width: 20px;
              height: 20px;
              color: #4caf50;
            }
          }
        }

        mat-card-actions {
          padding: 16px;
          display: flex;
          justify-content: flex-end;

          button {
            mat-icon,
            mat-spinner {
              margin-right: 8px;
            }
          }
        }
      }

      /* Saved Reports Tab */
      .saved-reports-container {
        min-height: 400px;
      }

      .saved-reports-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;

        h2 {
          font-size: 24px;
          font-weight: 500;
          margin: 0;
          color: #1a1a1a;
        }

        .filter-buttons {
          display: flex;
          gap: 8px;

          button {
            &.active {
              background-color: #e3f2fd;
              color: #1976d2;
            }

            mat-icon {
              margin-right: 4px;
            }
          }
        }
      }

      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        gap: 16px;

        p {
          color: #666;
          font-size: 16px;
        }
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        text-align: center;

        .empty-icon {
          font-size: 72px;
          width: 72px;
          height: 72px;
          color: #ccc;
          margin-bottom: 16px;
        }

        h3 {
          font-size: 24px;
          font-weight: 500;
          color: #333;
          margin: 0 0 8px 0;
        }

        p {
          color: #666;
          margin: 0 0 24px 0;
        }

        button mat-icon {
          margin-right: 8px;
        }
      }

      /* Bulk Actions Toolbar */
      .bulk-actions-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px;
        background-color: #e3f2fd;
        border-radius: 8px;
        margin-bottom: 16px;

        .selection-info {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #1976d2;
          font-weight: 500;

          mat-icon {
            color: #1976d2;
          }
        }

        .bulk-actions {
          display: flex;
          gap: 12px;
        }
      }

      /* Table Styles */
      .table-container {
        overflow-x: auto;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        background: white;
      }

      .reports-table {
        width: 100%;

        th {
          background-color: #f5f5f5;
          font-weight: 600;
          color: #424242;
        }

        td, th {
          padding: 12px 16px;
        }
      }

      .report-type-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;

        &.patient {
          background-color: #e3f2fd;
          color: #1976d2;

          mat-icon {
            font-size: 16px;
            width: 16px;
            height: 16px;
          }
        }

        &.population {
          background-color: #e8f5e9;
          color: #388e3c;

          mat-icon {
            font-size: 16px;
            width: 16px;
            height: 16px;
          }
        }
      }

      .date-time {
        .time {
          font-size: 12px;
          color: #666;
          margin-top: 4px;
        }
      }

      .action-buttons {
        display: flex;
        gap: 4px;
      }

      .no-data {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px;
        color: #999;

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          margin-bottom: 12px;
        }

        p {
          margin: 0;
          font-size: 14px;
        }
      }

      .reports-list {
        display: grid;
        gap: 16px;
      }

      .report-list-card {
        mat-card-header {
          display: flex;
          align-items: center;

          .report-type-icon {
            font-size: 32px;
            width: 32px;
            height: 32px;
            margin-right: 16px;

            &.patient {
              color: #1976d2;
            }

            &.population {
              color: #388e3c;
            }
          }
        }

        .report-meta {
          display: flex;
          gap: 24px;
          align-items: center;
          flex-wrap: wrap;

          .meta-item {
            display: flex;
            align-items: center;
            gap: 6px;
            color: #666;
            font-size: 14px;

            mat-icon {
              font-size: 18px;
              width: 18px;
              height: 18px;
            }

            &.status-badge {
              padding: 4px 12px;
              border-radius: 12px;
              font-weight: 500;
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

        mat-card-actions {
          display: flex;
          gap: 8px;
          padding: 8px 16px;

          button {
            mat-icon {
              margin-right: 4px;
            }
          }
        }
      }
    `,
  ],
})
export class ReportsComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  selectedTabIndex = 0;
  savedReports = signal<SavedReport[]>(INITIAL_SAVED_REPORTS);
  selectedReportType = signal<ReportType | null>(null);
  isLoadingReports = signal(false);
  isGeneratingPatientReport = signal(false);
  isGeneratingPopulationReport = signal(false);

  // Table data and selection
  dataSource = new MatTableDataSource<SavedReport>(INITIAL_SAVED_REPORTS);
  selection = new SelectionModel<SavedReport>(true, []);
  displayedColumns: string[] = [
    'select',
    'reportName',
    'reportType',
    'createdAt',
    'createdBy',
    'status',
    'actions',
  ];

  constructor(
    private evaluationService: EvaluationService,
    private dialog: MatDialog,
    private toast: ToastService,
    public aiAssistant: AIAssistantService
  ) {}

  ngOnInit(): void {
    this.loadSavedReports();
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source after view init
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  /**
   * Load saved reports from the backend
   */
  @TrackInteraction('reports', 'load-reports')
  loadSavedReports(reportType?: ReportType): void {
    this.isLoadingReports.set(true);
    this.evaluationService.getSavedReports(reportType).subscribe({
      next: (reports) => {
        const reportsToDisplay = reports.length > 0 ? reports : INITIAL_SAVED_REPORTS;
        this.savedReports.set(reportsToDisplay);
        this.dataSource.data = reportsToDisplay;
        this.isLoadingReports.set(false);
      },
      error: (error) => {
        this.toast.error('Failed to load reports');
        this.isLoadingReports.set(false);
      },
    });
  }

  /**
   * Filter reports by type
   */
  filterReports(reportType: ReportType | null): void {
    this.selectedReportType.set(reportType);
    this.loadSavedReports(reportType ?? undefined);
  }

  /**
   * Generate patient report with patient selection
   */
  @TrackInteraction('reports', 'create-patient-report')
  onGeneratePatientReport(): void {
    // Open patient selection dialog
    const dialogRef = this.dialog.open(PatientSelectionDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((patientId: string | null) => {
      if (patientId) {
        const reportName = `Patient Report - ${new Date().toLocaleDateString()}`;

        this.isGeneratingPatientReport.set(true);
        this.evaluationService
          .savePatientReport(patientId, reportName, 'clinical-portal')
          .subscribe({
            next: (report) => {
              this.toast.success('Patient report generated successfully');
              this.isGeneratingPatientReport.set(false);
              this.selectedTabIndex = 1;
              this.loadSavedReports();
            },
            error: (error) => {
              this.toast.error('Failed to generate patient report');
              this.isGeneratingPatientReport.set(false);
            },
          });
      }
    });
  }

  /**
   * Generate population report with year selection
   */
  @TrackInteraction('reports', 'create-population-report')
  onGeneratePopulationReport(): void {
    // Open year selection dialog
    const dialogRef = this.dialog.open(YearSelectionDialogComponent, {
      width: '550px',
      maxWidth: '90vw',
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((year: number | null) => {
      if (year) {
        const reportName = `Population Report ${year}`;

        this.isGeneratingPopulationReport.set(true);
        this.evaluationService
          .savePopulationReport(year, reportName, 'clinical-portal')
          .subscribe({
            next: (report) => {
              this.toast.success('Population report generated successfully');
              this.isGeneratingPopulationReport.set(false);
              this.selectedTabIndex = 1;
              this.loadSavedReports();
            },
            error: (error) => {
              this.toast.error('Failed to generate population report');
              this.isGeneratingPopulationReport.set(false);
            },
          });
      }
    });
  }

  /**
   * View report details
   */
  onViewReport(report: SavedReport): void {
    this.dialog.open(ReportDetailDialogComponent, {
      data: report,
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '90vh',
    });
  }

  /**
   * Export report to CSV
   */
  onExportCsv(report: SavedReport): void {
    this.evaluationService
      .exportAndDownloadReport(report.id, report.reportName, 'csv')
      .subscribe({
        next: () => {
          this.toast.success('Report exported to CSV');
        },
        error: (error) => {
          this.toast.error('Failed to export report to CSV');
        },
      });
  }

  /**
   * Export report to Excel
   */
  onExportExcel(report: SavedReport): void {
    this.evaluationService
      .exportAndDownloadReport(report.id, report.reportName, 'excel')
      .subscribe({
        next: () => {
          this.toast.success('Report exported to Excel');
        },
        error: (error) => {
          this.toast.error('Failed to export report to Excel');
        },
      });
  }

  /**
   * Delete saved report with confirmation
   */
  onDeleteReport(report: SavedReport): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Report?',
        message: `Are you sure you want to delete "<strong>${report.reportName}</strong>"?<br><br>This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmColor: 'warn',
        icon: 'warning',
        iconColor: '#f44336'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.evaluationService.deleteSavedReport(report.id).subscribe({
          next: () => {
            this.toast.success('Report deleted successfully');
            this.loadSavedReports(this.selectedReportType() ?? undefined);
          },
          error: (error) => {
            this.toast.error('Failed to delete report');
          },
        });
      }
    });
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  /**
   * Format time for display
   */
  formatTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
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
  checkboxLabel(row?: SavedReport): string {
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
   * Export selected reports to CSV
   */
  exportSelectedToCSV(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedReports = this.selection.selected;

    // CSV header
    const headers = [
      'Report Name',
      'Report Type',
      'Created Date',
      'Created By',
      'Status'
    ];

    // CSV rows
    const rows = selectedReports.map(report => [
      report.reportName,
      report.reportType,
      this.formatDate(report.createdAt),
      report.createdBy,
      report.status
    ]);

    // Combine headers and rows
    const csvData = [headers, ...rows];
    const csvContent = CSVHelper.arrayToCSV(csvData);
    const filename = `selected-reports-${new Date().toISOString().split('T')[0]}.csv`;

    CSVHelper.downloadCSV(filename, csvContent);
    this.toast.success(`Exported ${selectedReports.length} reports to CSV`);
  }

  /**
   * Delete selected reports with confirmation
   */
  deleteSelected(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedCount = this.selection.selected.length;
    const reportLabel = selectedCount === 1
      ? `"${this.selection.selected[0].reportName}"`
      : `${selectedCount} reports`;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Reports?',
        message: `Are you sure you want to delete ${reportLabel}?<br><br>This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmColor: 'warn',
        icon: 'warning',
        iconColor: '#f44336'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.performDeleteSelected();
      }
    });
  }

  /**
   * Perform the actual deletion of selected reports
   */
  private performDeleteSelected(): void {
    const selectedReports = [...this.selection.selected];
    let deletedCount = 0;
    let errorCount = 0;

    selectedReports.forEach((report) => {
      this.evaluationService.deleteSavedReport(report.id).subscribe({
        next: () => {
          deletedCount++;
          // Remove from local arrays
          const currentReports = this.savedReports();
          this.savedReports.set(currentReports.filter(r => r.id !== report.id));
          this.dataSource.data = this.dataSource.data.filter(r => r.id !== report.id);
          this.selection.deselect(report);

          // Show success message for the last deletion
          if (deletedCount + errorCount === selectedReports.length) {
            if (errorCount === 0) {
              this.toast.success(`Successfully deleted ${deletedCount} report(s)`);
            } else {
              this.toast.error(`Deleted ${deletedCount} report(s), ${errorCount} failed`);
            }
          }
        },
        error: (err) => {
          errorCount++;
          console.error(`Error deleting report ${report.reportName}:`, err);

          // Show summary after all attempts
          if (deletedCount + errorCount === selectedReports.length) {
            if (deletedCount === 0) {
              this.toast.error('Failed to delete reports');
            } else {
              this.toast.error(`Deleted ${deletedCount} report(s), ${errorCount} failed`);
            }
          }
        },
      });
    });
  }
}
