import { Component, OnInit, OnDestroy, signal, ViewChild, AfterViewInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
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
import { ReportTemplatesService, ReportTemplate, ReportTemplateCategory } from '../../services/report-templates.service';
import { QrdaExportService, QrdaExportJob, QrdaJobType, QrdaExportRequest } from '../../services/qrda-export.service';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { SavedReport, ReportType } from '../../models/quality-result.model';
import { PatientSelectionDialogComponent } from '../../components/dialogs/patient-selection-dialog.component';
import { YearSelectionDialogComponent } from '../../components/dialogs/year-selection-dialog.component';
import { ReportDetailDialogComponent } from '../../components/dialogs/report-detail-dialog.component';
import { ConfirmDialogComponent } from '../../components/dialogs/confirm-dialog.component';
import { PeriodComparisonDialogComponent, PeriodComparisonSelection } from '../../components/dialogs/period-comparison-dialog.component';
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
    FormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatRadioModule,
    MatSelectModule,
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
          <button class="action-card comparative"
                  type="button"
                  (click)="onGenerateComparativeReport()"
                  [disabled]="isGeneratingComparativeReport()"
                  matTooltip="Compare compliance across time periods">
            <div class="action-card-icon">
              <mat-icon>compare_arrows</mat-icon>
            </div>
            <div class="action-card-content">
              <span class="action-title">Comparative Report</span>
              <span class="action-desc">Period-over-period trends</span>
            </div>
            @if (isGeneratingComparativeReport()) {
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

              <!-- Comparative Report Card -->
              <mat-card class="generation-card">
                <mat-card-header>
                  <mat-icon class="card-icon comparative-icon">compare_arrows</mat-icon>
                  <mat-card-title>Comparative Report</mat-card-title>
                  <mat-card-subtitle>
                    Compare compliance across time periods
                  </mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <p class="card-description">
                    Analyze how quality metrics have changed over time by comparing
                    different time periods - year-over-year, quarter-over-quarter, or month-over-month.
                  </p>
                  <div class="card-features">
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Period-over-period comparison</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Identify improving measures</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Highlight declining metrics</span>
                    </div>
                    <div class="feature-item">
                      <mat-icon>check_circle</mat-icon>
                      <span>Trend analysis and insights</span>
                    </div>
                  </div>
                </mat-card-content>
                <mat-card-actions>
                  <app-loading-button
                    [loading]="isGeneratingComparativeReport()"
                    [color]="'primary'"
                    [matTooltip]="'Compare compliance rates across different time periods'"
                    (buttonClick)="onGenerateComparativeReport()">
                    <mat-icon>play_arrow</mat-icon>
                    Generate Comparative Report
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

        <!-- Report Templates Tab -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon class="tab-icon">library_books</mat-icon>
            Report Templates
          </ng-template>
          <div class="tab-content">
            <div class="templates-container">
              <!-- Templates Header -->
              <div class="templates-header">
                <div class="templates-title-section">
                  <h2>Pre-Built Report Templates</h2>
                  <p class="templates-subtitle">Quick-start with CMS/HEDIS compliant report templates</p>
                </div>
                <div class="templates-search">
                  <mat-form-field appearance="outline" class="search-field">
                    <mat-label>Search templates</mat-label>
                    <input
                      matInput
                      [(ngModel)]="templateSearchTerm"
                      (ngModelChange)="filterTemplates()"
                      placeholder="Search by name or tag..."
                    />
                    <mat-icon matPrefix>search</mat-icon>
                    @if (templateSearchTerm) {
                      <button mat-icon-button matSuffix (click)="templateSearchTerm = ''; filterTemplates()">
                        <mat-icon>close</mat-icon>
                      </button>
                    }
                  </mat-form-field>
                </div>
              </div>

              <!-- Category Chips -->
              <div class="category-chips">
                <button
                  mat-stroked-button
                  [class.active]="!selectedTemplateCategory"
                  (click)="selectTemplateCategory(null)"
                >
                  <mat-icon>apps</mat-icon>
                  All Templates
                </button>
                @for (cat of templateCategories; track cat.value) {
                  <button
                    mat-stroked-button
                    [class.active]="selectedTemplateCategory === cat.value"
                    (click)="selectTemplateCategory(cat.value)"
                  >
                    <mat-icon>{{ cat.icon }}</mat-icon>
                    {{ cat.label }}
                  </button>
                }
              </div>

              <!-- Favorites Section -->
              @if (favoriteTemplates.length > 0) {
                <div class="templates-section">
                  <h3 class="section-title">
                    <mat-icon>star</mat-icon>
                    Favorite Templates
                  </h3>
                  <div class="templates-grid">
                    @for (template of favoriteTemplates; track template.id) {
                      <mat-card class="template-card" [class]="template.category.toLowerCase()">
                        <mat-card-header>
                          <mat-icon mat-card-avatar class="template-icon">{{ template.icon }}</mat-icon>
                          <mat-card-title>{{ template.name }}</mat-card-title>
                          <mat-card-subtitle>{{ template.category }} | {{ template.type }}</mat-card-subtitle>
                          <button
                            mat-icon-button
                            class="favorite-btn favorited"
                            (click)="toggleTemplateFavorite(template.id, $event)"
                            matTooltip="Remove from favorites"
                          >
                            <mat-icon>star</mat-icon>
                          </button>
                        </mat-card-header>
                        <mat-card-content>
                          <p class="template-description">{{ template.description }}</p>
                          <div class="template-tags">
                            @for (tag of template.tags.slice(0, 3); track tag) {
                              <span class="tag">{{ tag }}</span>
                            }
                          </div>
                        </mat-card-content>
                        <mat-card-actions>
                          <button mat-raised-button color="primary" (click)="useTemplate(template)">
                            <mat-icon>play_arrow</mat-icon>
                            Use Template
                          </button>
                        </mat-card-actions>
                      </mat-card>
                    }
                  </div>
                </div>
              }

              <!-- All Templates Section -->
              <div class="templates-section">
                <h3 class="section-title">
                  <mat-icon>library_books</mat-icon>
                  {{ selectedTemplateCategory ? getCategoryLabel(selectedTemplateCategory) : 'All Templates' }}
                  <span class="count">({{ filteredTemplates.length }})</span>
                </h3>
                <div class="templates-grid">
                  @for (template of filteredTemplates; track template.id) {
                    <mat-card class="template-card" [class]="template.category.toLowerCase()">
                      <mat-card-header>
                        <mat-icon mat-card-avatar class="template-icon">{{ template.icon }}</mat-icon>
                        <mat-card-title>{{ template.name }}</mat-card-title>
                        <mat-card-subtitle>{{ template.category }} | {{ template.type }}</mat-card-subtitle>
                        <button
                          mat-icon-button
                          class="favorite-btn"
                          [class.favorited]="template.isFavorite"
                          (click)="toggleTemplateFavorite(template.id, $event)"
                          [matTooltip]="template.isFavorite ? 'Remove from favorites' : 'Add to favorites'"
                        >
                          <mat-icon>{{ template.isFavorite ? 'star' : 'star_border' }}</mat-icon>
                        </button>
                      </mat-card-header>
                      <mat-card-content>
                        <p class="template-description">{{ template.description }}</p>
                        <div class="template-meta">
                          <span class="meta-item" *ngIf="template.measures.length > 0">
                            <mat-icon>checklist</mat-icon>
                            {{ template.measures.length }} measures
                          </span>
                          <span class="meta-item" *ngIf="template.usageCount > 0">
                            <mat-icon>trending_up</mat-icon>
                            Used {{ template.usageCount }} times
                          </span>
                        </div>
                        <div class="template-tags">
                          @for (tag of template.tags.slice(0, 4); track tag) {
                            <span class="tag">{{ tag }}</span>
                          }
                        </div>
                      </mat-card-content>
                      <mat-card-actions>
                        <button mat-raised-button color="primary" (click)="useTemplate(template)">
                          <mat-icon>play_arrow</mat-icon>
                          Use Template
                        </button>
                        <button mat-stroked-button (click)="previewTemplate(template)">
                          <mat-icon>visibility</mat-icon>
                          Preview
                        </button>
                      </mat-card-actions>
                    </mat-card>
                  }
                </div>

                @if (filteredTemplates.length === 0) {
                  <div class="empty-state">
                    <mat-icon class="empty-icon">search_off</mat-icon>
                    <h3>No Templates Found</h3>
                    <p>Try adjusting your search or category filter</p>
                    <button mat-raised-button color="primary" (click)="clearTemplateFilters()">
                      <mat-icon>clear</mat-icon>
                      Clear Filters
                    </button>
                  </div>
                }
              </div>
            </div>
          </div>
        </mat-tab>

        <!-- QRDA Export Tab -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon class="tab-icon">cloud_upload</mat-icon>
            QRDA Export
          </ng-template>
          <div class="tab-content">
            <div class="qrda-container">
              <!-- QRDA Header -->
              <div class="qrda-header">
                <div class="qrda-title-section">
                  <h2>CMS Quality Reporting (QRDA)</h2>
                  <p class="qrda-subtitle">Generate QRDA documents for CMS submission</p>
                </div>
              </div>

              <!-- Category Selection Cards -->
              <div class="qrda-category-cards">
                <mat-card
                  class="qrda-category-card"
                  [class.selected]="selectedQrdaCategory() === 'QRDA_I'"
                  (click)="selectQrdaCategory('QRDA_I')"
                >
                  <mat-card-header>
                    <mat-icon mat-card-avatar class="category-icon cat-i">person</mat-icon>
                    <mat-card-title>QRDA Category I</mat-card-title>
                    <mat-card-subtitle>Patient-Level Export</mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <p class="category-description">
                      Individual patient quality measure results for CMS submission.
                      Creates one XML document per patient with detailed measure data.
                    </p>
                    <div class="category-features">
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>Individual patient data</span>
                      </div>
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>PHI included (secure)</span>
                      </div>
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>CMS certified format</span>
                      </div>
                    </div>
                  </mat-card-content>
                </mat-card>

                <mat-card
                  class="qrda-category-card"
                  [class.selected]="selectedQrdaCategory() === 'QRDA_III'"
                  (click)="selectQrdaCategory('QRDA_III')"
                >
                  <mat-card-header>
                    <mat-icon mat-card-avatar class="category-icon cat-iii">groups</mat-icon>
                    <mat-card-title>QRDA Category III</mat-card-title>
                    <mat-card-subtitle>Aggregate Population Export</mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <p class="category-description">
                      Aggregated population-level quality measure results.
                      Single XML document with summary statistics for all patients.
                    </p>
                    <div class="category-features">
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>Population statistics</span>
                      </div>
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>No PHI (de-identified)</span>
                      </div>
                      <div class="feature-item">
                        <mat-icon>check_circle</mat-icon>
                        <span>CMS certified format</span>
                      </div>
                    </div>
                  </mat-card-content>
                </mat-card>
              </div>

              @if (selectedQrdaCategory()) {
                <!-- Export Configuration -->
                <mat-card class="qrda-config-card">
                  <mat-card-header>
                    <mat-icon mat-card-avatar class="config-icon">settings</mat-icon>
                    <mat-card-title>Export Configuration</mat-card-title>
                    <mat-card-subtitle>Configure your {{ selectedQrdaCategory() === 'QRDA_I' ? 'Category I' : 'Category III' }} export</mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <div class="config-form">
                      <!-- Measure Selection -->
                      <div class="form-section">
                        <h4>Quality Measures</h4>
                        <mat-form-field appearance="outline" class="full-width">
                          <mat-label>Select Measures</mat-label>
                          <mat-select multiple [(ngModel)]="selectedQrdaMeasures">
                            @for (measure of availableQrdaMeasures; track measure.id) {
                              <mat-option [value]="measure.id">
                                {{ measure.code }} - {{ measure.name }}
                              </mat-option>
                            }
                          </mat-select>
                          <mat-hint>Select one or more quality measures to include</mat-hint>
                        </mat-form-field>
                      </div>

                      <!-- Reporting Period -->
                      <div class="form-section">
                        <h4>Reporting Period</h4>
                        <div class="date-range">
                          <mat-form-field appearance="outline">
                            <mat-label>Start Date</mat-label>
                            <input matInput [matDatepicker]="startPicker" [(ngModel)]="qrdaPeriodStart">
                            <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
                            <mat-datepicker #startPicker></mat-datepicker>
                          </mat-form-field>
                          <mat-form-field appearance="outline">
                            <mat-label>End Date</mat-label>
                            <input matInput [matDatepicker]="endPicker" [(ngModel)]="qrdaPeriodEnd">
                            <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
                            <mat-datepicker #endPicker></mat-datepicker>
                          </mat-form-field>
                        </div>
                        <div class="period-presets">
                          <button mat-stroked-button type="button" (click)="setQrdaPeriod('ytd')">Year to Date</button>
                          <button mat-stroked-button type="button" (click)="setQrdaPeriod('2025')">CY 2025</button>
                          <button mat-stroked-button type="button" (click)="setQrdaPeriod('2024')">CY 2024</button>
                          <button mat-stroked-button type="button" (click)="setQrdaPeriod('q4-2025')">Q4 2025</button>
                        </div>
                      </div>

                      @if (selectedQrdaCategory() === 'QRDA_I') {
                        <!-- Patient Selection (Category I only) -->
                        <div class="form-section">
                          <h4>Patient Selection</h4>
                          <mat-radio-group [(ngModel)]="qrdaPatientSelection" class="patient-selection-group">
                            <mat-radio-button value="all">All eligible patients</mat-radio-button>
                            <mat-radio-button value="selected">Selected patients only</mat-radio-button>
                          </mat-radio-group>
                          @if (qrdaPatientSelection === 'selected') {
                            <button mat-stroked-button color="primary" type="button" (click)="selectPatientsForQrda()">
                              <mat-icon>person_add</mat-icon>
                              Select Patients ({{ selectedQrdaPatients.length }} selected)
                            </button>
                          }
                        </div>
                      }

                      <!-- Validation Options -->
                      <div class="form-section">
                        <h4>Export Options</h4>
                        <div class="checkbox-options">
                          <mat-checkbox [(ngModel)]="qrdaValidateDocuments">
                            Validate documents before export
                          </mat-checkbox>
                          <mat-checkbox [(ngModel)]="qrdaIncludeSupplemental">
                            Include supplemental data elements
                          </mat-checkbox>
                        </div>
                      </div>
                    </div>
                  </mat-card-content>
                  <mat-card-actions>
                    <button mat-raised-button color="primary" type="button"
                            [disabled]="!canGenerateQrda() || isGeneratingQrda()"
                            (click)="generateQrdaExport()">
                      @if (isGeneratingQrda()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>cloud_upload</mat-icon>
                      }
                      Generate {{ selectedQrdaCategory() === 'QRDA_I' ? 'Category I' : 'Category III' }} Export
                    </button>
                  </mat-card-actions>
                </mat-card>
              }

              <!-- Active Export Jobs -->
              @if (activeQrdaJobs().length > 0) {
                <div class="qrda-jobs-section">
                  <h3>
                    <mat-icon>work_history</mat-icon>
                    Export Jobs
                  </h3>
                  <div class="qrda-jobs-grid">
                    @for (job of activeQrdaJobs(); track job.id) {
                      <mat-card class="qrda-job-card" [class]="job.status.toLowerCase()">
                        <mat-card-header>
                          <mat-icon mat-card-avatar [class]="job.jobType === 'QRDA_I' ? 'cat-i' : 'cat-iii'">
                            {{ job.jobType === 'QRDA_I' ? 'person' : 'groups' }}
                          </mat-icon>
                          <mat-card-title>{{ job.jobType === 'QRDA_I' ? 'Category I' : 'Category III' }} Export</mat-card-title>
                          <mat-card-subtitle>{{ formatDate(job.createdAt) }}</mat-card-subtitle>
                        </mat-card-header>
                        <mat-card-content>
                          <div class="job-status">
                            <span class="status-badge" [class]="job.status.toLowerCase()">
                              @if (job.status === 'RUNNING') {
                                <mat-spinner diameter="14"></mat-spinner>
                              } @else if (job.status === 'COMPLETED') {
                                <mat-icon>check_circle</mat-icon>
                              } @else if (job.status === 'FAILED') {
                                <mat-icon>error</mat-icon>
                              } @else if (job.status === 'PENDING') {
                                <mat-icon>schedule</mat-icon>
                              } @else {
                                <mat-icon>cancel</mat-icon>
                              }
                              {{ job.status }}
                            </span>
                          </div>
                          @if (job.status === 'RUNNING') {
                            <mat-progress-bar mode="indeterminate"></mat-progress-bar>
                          }
                          <div class="job-details">
                            <div class="detail-item">
                              <span class="label">Measures:</span>
                              <span class="value">{{ job.measureIds.length }}</span>
                            </div>
                            <div class="detail-item">
                              <span class="label">Period:</span>
                              <span class="value">{{ job.periodStart }} - {{ job.periodEnd }}</span>
                            </div>
                            @if (job.documentCount) {
                              <div class="detail-item">
                                <span class="label">Documents:</span>
                                <span class="value">{{ job.documentCount }}</span>
                              </div>
                            }
                            @if (job.patientCount) {
                              <div class="detail-item">
                                <span class="label">Patients:</span>
                                <span class="value">{{ job.patientCount }}</span>
                              </div>
                            }
                          </div>
                          @if (job.errorMessage) {
                            <div class="job-error">
                              <mat-icon>error_outline</mat-icon>
                              {{ job.errorMessage }}
                            </div>
                          }
                        </mat-card-content>
                        <mat-card-actions>
                          @if (job.status === 'COMPLETED') {
                            <button mat-raised-button color="primary" type="button" (click)="downloadQrdaExport(job)">
                              <mat-icon>download</mat-icon>
                              Download
                            </button>
                          }
                          @if (job.status === 'PENDING' || job.status === 'RUNNING') {
                            <button mat-stroked-button color="warn" type="button" (click)="cancelQrdaJob(job)">
                              <mat-icon>cancel</mat-icon>
                              Cancel
                            </button>
                          }
                        </mat-card-actions>
                      </mat-card>
                    }
                  </div>
                </div>
              }

              <!-- CMS Submission Info -->
              <mat-card class="qrda-info-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar class="info-icon">info</mat-icon>
                  <mat-card-title>About QRDA Exports</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="info-grid">
                    <div class="info-item">
                      <h4>QRDA Category I</h4>
                      <p>Patient-level quality measure reports containing individual clinical data.
                         Used for Medicare/Medicaid EHR Incentive Programs and other patient-specific reporting.</p>
                    </div>
                    <div class="info-item">
                      <h4>QRDA Category III</h4>
                      <p>Aggregate population-level reports with summary statistics.
                         Used for CMS quality reporting programs including MIPS and hospital quality measures.</p>
                    </div>
                    <div class="info-item">
                      <h4>Validation</h4>
                      <p>Documents are validated against CMS schematrons to ensure compliance
                         before submission to the CMS portal.</p>
                    </div>
                    <div class="info-item">
                      <h4>Audit Logging</h4>
                      <p>All QRDA exports are logged for HIPAA compliance, including document generation
                         and download activities.</p>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>
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

          &.comparative .action-card-icon {
            background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%);
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

        .comparative-icon {
          color: #9c27b0;
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

      /* Templates Tab Styles */
      .templates-container {
        padding: 0;
      }

      .templates-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 24px;
        flex-wrap: wrap;
        gap: 16px;

        .templates-title-section {
          h2 {
            font-size: 24px;
            font-weight: 500;
            margin: 0 0 4px 0;
            color: #1a1a1a;
          }

          .templates-subtitle {
            font-size: 14px;
            color: #666;
            margin: 0;
          }
        }

        .templates-search {
          .search-field {
            width: 300px;
            margin: 0;

            ::ng-deep .mat-mdc-form-field-subscript-wrapper {
              display: none;
            }
          }
        }
      }

      .category-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 24px;

        button {
          border-radius: 20px;
          padding: 0 16px;
          height: 36px;
          font-size: 13px;
          transition: all 0.2s ease;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
            margin-right: 6px;
          }

          &.active {
            background-color: #1976d2;
            color: white;
            border-color: #1976d2;
          }

          &:hover:not(.active) {
            background-color: #e3f2fd;
          }
        }
      }

      .templates-section {
        margin-bottom: 32px;

        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 18px;
          font-weight: 500;
          color: #333;
          margin: 0 0 16px 0;

          mat-icon {
            color: #1976d2;
          }

          .count {
            font-weight: 400;
            color: #666;
            font-size: 14px;
          }
        }
      }

      .templates-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 20px;
      }

      .template-card {
        position: relative;
        transition: all 0.3s ease;
        border-left: 4px solid transparent;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
        }

        &.cms {
          border-left-color: #1976d2;
        }

        &.hedis {
          border-left-color: #388e3c;
        }

        &.preventive {
          border-left-color: #00bcd4;
        }

        &.chronic_disease {
          border-left-color: #ff9800;
        }

        &.behavioral_health {
          border-left-color: #9c27b0;
        }

        &.medication {
          border-left-color: #e91e63;
        }

        &.custom {
          border-left-color: #607d8b;
        }

        mat-card-header {
          position: relative;

          .template-icon {
            background-color: #f5f5f5;
            color: #1976d2;
            width: 48px;
            height: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            border-radius: 8px;
          }

          .favorite-btn {
            position: absolute;
            top: 0;
            right: 0;

            mat-icon {
              color: #bdbdbd;
              transition: color 0.2s ease;
            }

            &:hover mat-icon {
              color: #ffc107;
            }

            &.favorited mat-icon {
              color: #ffc107;
            }
          }
        }

        .template-description {
          font-size: 14px;
          color: #666;
          line-height: 1.5;
          margin: 0 0 12px 0;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .template-meta {
          display: flex;
          gap: 16px;
          margin-bottom: 12px;

          .meta-item {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: #888;

            mat-icon {
              font-size: 16px;
              width: 16px;
              height: 16px;
            }
          }
        }

        .template-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;

          .tag {
            display: inline-block;
            padding: 2px 10px;
            background-color: #f0f0f0;
            border-radius: 12px;
            font-size: 11px;
            color: #666;
          }
        }

        mat-card-actions {
          display: flex;
          gap: 8px;
          padding: 12px 16px;
          border-top: 1px solid #eee;

          button {
            mat-icon {
              margin-right: 4px;
            }
          }
        }
      }

      /* QRDA Export Tab Styles */
      .qrda-container {
        padding: 0;
      }

      .qrda-header {
        margin-bottom: 24px;

        h2 {
          font-size: 24px;
          font-weight: 500;
          margin: 0 0 4px 0;
          color: #1a1a1a;
        }

        .qrda-subtitle {
          font-size: 14px;
          color: #666;
          margin: 0;
        }
      }

      .qrda-category-cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
        gap: 24px;
        margin-bottom: 24px;
      }

      .qrda-category-card {
        cursor: pointer;
        transition: all 0.3s ease;
        border: 2px solid transparent;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
        }

        &.selected {
          border-color: #1976d2;
          box-shadow: 0 4px 12px rgba(25, 118, 210, 0.25);
        }

        .category-icon {
          width: 56px;
          height: 56px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 12px;
          font-size: 28px;

          &.cat-i {
            background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
            color: white;
          }

          &.cat-iii {
            background: linear-gradient(135deg, #388e3c 0%, #2e7d32 100%);
            color: white;
          }
        }

        .category-description {
          color: #666;
          line-height: 1.6;
          margin: 0 0 16px 0;
        }

        .category-features {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .feature-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: #333;

            mat-icon {
              font-size: 18px;
              width: 18px;
              height: 18px;
              color: #4caf50;
            }
          }
        }
      }

      .qrda-config-card {
        margin-bottom: 24px;

        .config-icon {
          background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%);
          color: white;
          width: 48px;
          height: 48px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
        }

        .config-form {
          .form-section {
            margin-bottom: 24px;

            h4 {
              font-size: 16px;
              font-weight: 500;
              margin: 0 0 12px 0;
              color: #333;
            }

            .full-width {
              width: 100%;
            }
          }

          .date-range {
            display: flex;
            gap: 16px;
            margin-bottom: 12px;

            mat-form-field {
              flex: 1;
            }
          }

          .period-presets {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;

            button {
              font-size: 12px;
            }
          }

          .patient-selection-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
            margin-bottom: 12px;
          }

          .checkbox-options {
            display: flex;
            flex-direction: column;
            gap: 8px;
          }
        }

        mat-card-actions {
          padding: 16px;

          button {
            mat-icon, mat-spinner {
              margin-right: 8px;
            }
          }
        }
      }

      .qrda-jobs-section {
        margin-bottom: 24px;

        h3 {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 18px;
          font-weight: 500;
          margin: 0 0 16px 0;
          color: #333;

          mat-icon {
            color: #1976d2;
          }
        }
      }

      .qrda-jobs-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 16px;
      }

      .qrda-job-card {
        border-left: 4px solid transparent;

        &.completed {
          border-left-color: #4caf50;
        }

        &.running, &.pending {
          border-left-color: #ff9800;
        }

        &.failed {
          border-left-color: #f44336;
        }

        &.cancelled {
          border-left-color: #9e9e9e;
        }

        .job-status {
          margin-bottom: 12px;

          .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
            text-transform: uppercase;

            &.completed {
              background-color: #e8f5e9;
              color: #2e7d32;
            }

            &.running, &.pending {
              background-color: #fff3e0;
              color: #e65100;
            }

            &.failed {
              background-color: #ffebee;
              color: #c62828;
            }

            &.cancelled {
              background-color: #f5f5f5;
              color: #616161;
            }

            mat-icon {
              font-size: 14px;
              width: 14px;
              height: 14px;
            }
          }
        }

        mat-progress-bar {
          margin-bottom: 12px;
        }

        .job-details {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 8px;

          .detail-item {
            font-size: 13px;

            .label {
              color: #666;
            }

            .value {
              font-weight: 500;
              color: #333;
              margin-left: 4px;
            }
          }
        }

        .job-error {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          margin-top: 12px;
          padding: 8px;
          background-color: #ffebee;
          border-radius: 4px;
          color: #c62828;
          font-size: 13px;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
            flex-shrink: 0;
          }
        }

        mat-card-actions {
          padding: 12px 16px;
          display: flex;
          gap: 8px;

          button mat-icon {
            margin-right: 4px;
          }
        }
      }

      .qrda-info-card {
        background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);

        .info-icon {
          background: linear-gradient(135deg, #607d8b 0%, #455a64 100%);
          color: white;
          width: 48px;
          height: 48px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
        }

        .info-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
          gap: 24px;

          .info-item {
            h4 {
              font-size: 15px;
              font-weight: 600;
              margin: 0 0 8px 0;
              color: #333;
            }

            p {
              font-size: 13px;
              color: #666;
              line-height: 1.6;
              margin: 0;
            }
          }
        }
      }

      .cat-i {
        color: #1976d2;
      }

      .cat-iii {
        color: #388e3c;
      }
    `,
  ],
})
export class ReportsComponent implements OnInit, OnDestroy, AfterViewInit {
  private destroy$ = new Subject<void>();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  selectedTabIndex = 0;
  savedReports = signal<SavedReport[]>(INITIAL_SAVED_REPORTS);
  selectedReportType = signal<ReportType | null>(null);
  isLoadingReports = signal(false);
  isGeneratingPatientReport = signal(false);
  isGeneratingPopulationReport = signal(false);
  isGeneratingComparativeReport = signal(false);

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

  // Template properties
  templateSearchTerm = '';
  selectedTemplateCategory: ReportTemplateCategory | null = null;
  filteredTemplates: ReportTemplate[] = [];
  favoriteTemplates: ReportTemplate[] = [];
  templateCategories: { value: ReportTemplateCategory; label: string; icon: string }[] = [];

  // QRDA Export properties
  selectedQrdaCategory = signal<QrdaJobType | null>(null);
  selectedQrdaMeasures: string[] = [];
  qrdaPeriodStart: Date | null = new Date(2025, 0, 1);
  qrdaPeriodEnd: Date | null = new Date(2025, 11, 31);
  qrdaPatientSelection: 'all' | 'selected' = 'all';
  selectedQrdaPatients: string[] = [];
  qrdaValidateDocuments = true;
  qrdaIncludeSupplemental = true;
  isGeneratingQrda = signal(false);
  activeQrdaJobs = signal<QrdaExportJob[]>([]);

  // Available measures for QRDA export
  availableQrdaMeasures = [
    { id: 'BCS', code: 'BCS', name: 'Breast Cancer Screening' },
    { id: 'COL', code: 'COL', name: 'Colorectal Cancer Screening' },
    { id: 'CBP', code: 'CBP', name: 'Controlling High Blood Pressure' },
    { id: 'CDC-HBA1C', code: 'CDC', name: 'Comprehensive Diabetes Care - HbA1c' },
    { id: 'EED', code: 'EED', name: 'Eye Exam for Diabetics' },
    { id: 'SPC', code: 'SPC', name: 'Statin Therapy for Cardiovascular Disease' },
    { id: 'AWC', code: 'AWC', name: 'Adolescent Well-Care Visits' },
    { id: 'CIS', code: 'CIS', name: 'Childhood Immunization Status' },
    { id: 'WCC', code: 'WCC', name: 'Weight Assessment and Counseling' },
    { id: 'PPC', code: 'PPC', name: 'Prenatal and Postpartum Care' },
  ];

  constructor(
    private evaluationService: EvaluationService,
    private dialog: MatDialog,
    private toast: ToastService,
    private reportTemplatesService: ReportTemplatesService,
    private qrdaExportService: QrdaExportService,
    public aiAssistant: AIAssistantService
  ) {}

  ngOnInit(): void {
    this.loadSavedReports();
    this.initializeTemplates();
  }

  /**
   * Initialize report templates
   */
  private initializeTemplates(): void {
    this.templateCategories = this.reportTemplatesService.getCategories();
    this.filterTemplates();
    this.updateFavoriteTemplates();
  }

  ngAfterViewInit(): void {
    // Connect paginator and sort to data source after view init
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load saved reports from the backend
   */
  @TrackInteraction('reports', 'load-reports')
  loadSavedReports(reportType?: ReportType): void {
    this.isLoadingReports.set(true);
    this.evaluationService.getSavedReports(reportType).pipe(takeUntil(this.destroy$)).subscribe({
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

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((patientId: string | null) => {
      if (patientId) {
        const reportName = `Patient Report - ${new Date().toLocaleDateString()}`;

        this.isGeneratingPatientReport.set(true);
        this.evaluationService
          .savePatientReport(patientId, reportName, 'clinical-portal')
          .pipe(takeUntil(this.destroy$))
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

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((year: number | null) => {
      if (year) {
        const reportName = `Population Report ${year}`;

        this.isGeneratingPopulationReport.set(true);
        this.evaluationService
          .savePopulationReport(year, reportName, 'clinical-portal')
          .pipe(takeUntil(this.destroy$))
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
   * Generate comparative report for period-over-period analysis
   */
  @TrackInteraction('reports', 'create-comparative-report')
  onGenerateComparativeReport(): void {
    // Open period comparison dialog
    const dialogRef = this.dialog.open(PeriodComparisonDialogComponent, {
      width: '650px',
      maxWidth: '90vw',
      disableClose: false,
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((selection: PeriodComparisonSelection | null) => {
      if (selection) {
        const reportName = `Comparative Report: ${selection.period1.label} vs ${selection.period2.label}`;

        this.isGeneratingComparativeReport.set(true);

        // For now, generate a population report for the comparison period
        // In a full implementation, this would call a dedicated comparative report endpoint
        this.evaluationService
          .savePopulationReport(
            selection.period2.startDate.getFullYear(),
            reportName,
            'clinical-portal'
          )
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (report) => {
              this.toast.success('Comparative report generated successfully');
              this.isGeneratingComparativeReport.set(false);
              this.selectedTabIndex = 1;
              this.loadSavedReports();
            },
            error: (error) => {
              this.toast.error('Failed to generate comparative report');
              this.isGeneratingComparativeReport.set(false);
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
      .pipe(takeUntil(this.destroy$))
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
      .pipe(takeUntil(this.destroy$))
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

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.evaluationService.deleteSavedReport(report.id).pipe(takeUntil(this.destroy$)).subscribe({
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

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((confirmed: boolean) => {
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
      this.evaluationService.deleteSavedReport(report.id).pipe(takeUntil(this.destroy$)).subscribe({
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

  // ===== Template Methods =====

  /**
   * Filter templates by search term and category
   */
  filterTemplates(): void {
    let templates = this.reportTemplatesService.getTemplates();

    // Filter by category
    if (this.selectedTemplateCategory) {
      templates = templates.filter(t => t.category === this.selectedTemplateCategory);
    }

    // Filter by search term
    if (this.templateSearchTerm.trim()) {
      templates = this.reportTemplatesService.searchTemplates(this.templateSearchTerm);
      if (this.selectedTemplateCategory) {
        templates = templates.filter(t => t.category === this.selectedTemplateCategory);
      }
    }

    this.filteredTemplates = templates;
  }

  /**
   * Update favorite templates list
   */
  private updateFavoriteTemplates(): void {
    this.favoriteTemplates = this.reportTemplatesService.getFavoriteTemplates();
  }

  /**
   * Select a template category
   */
  selectTemplateCategory(category: ReportTemplateCategory | null): void {
    this.selectedTemplateCategory = category;
    this.filterTemplates();
  }

  /**
   * Toggle template favorite status
   */
  toggleTemplateFavorite(templateId: string, event: Event): void {
    event.stopPropagation();
    this.reportTemplatesService.toggleFavorite(templateId);
    this.updateFavoriteTemplates();
    this.filterTemplates();
  }

  /**
   * Use a template to generate a report
   */
  useTemplate(template: ReportTemplate): void {
    this.reportTemplatesService.recordUsage(template.id);

    if (template.type === 'PATIENT') {
      this.onGeneratePatientReport();
    } else if (template.type === 'POPULATION') {
      this.onGeneratePopulationReport();
    } else if (template.type === 'COMPARATIVE') {
      this.onGenerateComparativeReport();
    }

    this.toast.info(`Using template: ${template.name}`);
  }

  /**
   * Preview a template configuration
   */
  previewTemplate(template: ReportTemplate): void {
    const config = template.configuration;
    const features = [];

    if (config.includeCharts) features.push('Charts');
    if (config.includeDetails) features.push('Details');
    if (config.includeTrends) features.push('Trends');
    if (config.includeCareGaps) features.push('Care Gaps');
    if (config.includeRecommendations) features.push('Recommendations');

    const message = `
      <strong>${template.name}</strong><br><br>
      <strong>Type:</strong> ${template.type}<br>
      <strong>Category:</strong> ${template.category}<br>
      <strong>Measures:</strong> ${template.measures.length > 0 ? template.measures.length : 'All applicable'}<br>
      <strong>Features:</strong> ${features.join(', ')}<br>
      <strong>Group By:</strong> ${config.groupBy}<br>
      <strong>Sort By:</strong> ${config.sortBy}
    `;

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Template Preview',
        message: message,
        confirmText: 'Use Template',
        cancelText: 'Close',
        confirmColor: 'primary',
        icon: template.icon,
        iconColor: '#1976d2'
      }
    }).afterClosed().pipe(takeUntil(this.destroy$)).subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.useTemplate(template);
      }
    });
  }

  /**
   * Clear all template filters
   */
  clearTemplateFilters(): void {
    this.templateSearchTerm = '';
    this.selectedTemplateCategory = null;
    this.filterTemplates();
  }

  /**
   * Get category label
   */
  getCategoryLabel(category: ReportTemplateCategory): string {
    const cat = this.templateCategories.find(c => c.value === category);
    return cat ? cat.label : category;
  }

  // ===== QRDA Export Methods =====

  /**
   * Select QRDA category
   */
  selectQrdaCategory(category: QrdaJobType): void {
    this.selectedQrdaCategory.set(category);
  }

  /**
   * Set QRDA period based on preset
   */
  setQrdaPeriod(preset: string): void {
    const now = new Date();
    switch (preset) {
      case 'ytd':
        this.qrdaPeriodStart = new Date(now.getFullYear(), 0, 1);
        this.qrdaPeriodEnd = now;
        break;
      case '2025':
        this.qrdaPeriodStart = new Date(2025, 0, 1);
        this.qrdaPeriodEnd = new Date(2025, 11, 31);
        break;
      case '2024':
        this.qrdaPeriodStart = new Date(2024, 0, 1);
        this.qrdaPeriodEnd = new Date(2024, 11, 31);
        break;
      case 'q4-2025':
        this.qrdaPeriodStart = new Date(2025, 9, 1);
        this.qrdaPeriodEnd = new Date(2025, 11, 31);
        break;
    }
  }

  /**
   * Select patients for Category I export
   */
  selectPatientsForQrda(): void {
    const dialogRef = this.dialog.open(PatientSelectionDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      data: { multiSelect: true },
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((patientIds: string[] | null) => {
      if (patientIds && patientIds.length > 0) {
        this.selectedQrdaPatients = patientIds;
        this.toast.info(`${patientIds.length} patient(s) selected`);
      }
    });
  }

  /**
   * Check if QRDA export can be generated
   */
  canGenerateQrda(): boolean {
    return (
      this.selectedQrdaCategory() !== null &&
      this.selectedQrdaMeasures.length > 0 &&
      this.qrdaPeriodStart !== null &&
      this.qrdaPeriodEnd !== null
    );
  }

  /**
   * Format date to ISO string for API
   */
  private formatDateForApi(date: Date | null): string {
    if (!date) return '';
    return date.toISOString().split('T')[0];
  }

  /**
   * Generate QRDA export
   */
  generateQrdaExport(): void {
    if (!this.canGenerateQrda()) {
      this.toast.error('Please complete all required fields');
      return;
    }

    const category = this.selectedQrdaCategory();
    if (!category) return;

    this.isGeneratingQrda.set(true);

    const request: QrdaExportRequest = {
      jobType: category,
      measureIds: this.selectedQrdaMeasures,
      periodStart: this.formatDateForApi(this.qrdaPeriodStart),
      periodEnd: this.formatDateForApi(this.qrdaPeriodEnd),
      validateDocuments: this.qrdaValidateDocuments,
      includeSupplementalData: this.qrdaIncludeSupplemental,
    };

    // Add patient IDs for Category I if specific patients selected
    if (category === 'QRDA_I' && this.qrdaPatientSelection === 'selected' && this.selectedQrdaPatients.length > 0) {
      request.patientIds = this.selectedQrdaPatients;
    }

    const exportObservable = category === 'QRDA_I'
      ? this.qrdaExportService.generateCategoryI(request)
      : this.qrdaExportService.generateCategoryIII(request);

    exportObservable.pipe(takeUntil(this.destroy$)).subscribe({
      next: (job) => {
        this.isGeneratingQrda.set(false);
        this.toast.success(`${category === 'QRDA_I' ? 'Category I' : 'Category III'} export started`);
        this.activeQrdaJobs.update(jobs => [job, ...jobs]);

        // Start polling for job completion
        this.pollQrdaJob(job.id);
      },
      error: (error) => {
        this.isGeneratingQrda.set(false);
        this.toast.error(`Failed to start ${category === 'QRDA_I' ? 'Category I' : 'Category III'} export`);
      },
    });
  }

  /**
   * Poll QRDA job status
   */
  private pollQrdaJob(jobId: string): void {
    this.qrdaExportService.pollJobUntilComplete(jobId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (job) => {
          // Update job in active jobs list
          this.activeQrdaJobs.update(jobs =>
            jobs.map(j => j.id === job.id ? job : j)
          );

          // Show completion message
          if (job.status === 'COMPLETED') {
            this.toast.success(`QRDA export completed - ${job.documentCount || 1} document(s) ready`);
          } else if (job.status === 'FAILED') {
            this.toast.error(`QRDA export failed: ${job.errorMessage || 'Unknown error'}`);
          }
        },
        error: (error) => {
          this.toast.error('Failed to get export status');
        },
      });
  }

  /**
   * Download QRDA export
   */
  downloadQrdaExport(job: QrdaExportJob): void {
    this.qrdaExportService.downloadDocument(job.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const filename = this.qrdaExportService.getExportFilename(job);
          this.qrdaExportService.triggerDownload(blob, filename);
          this.toast.success('Download started');
        },
        error: (error) => {
          this.toast.error('Failed to download export');
        },
      });
  }

  /**
   * Cancel QRDA job
   */
  cancelQrdaJob(job: QrdaExportJob): void {
    this.qrdaExportService.cancelJob(job.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cancelledJob) => {
          this.activeQrdaJobs.update(jobs =>
            jobs.map(j => j.id === cancelledJob.id ? cancelledJob : j)
          );
          this.toast.info('Export cancelled');
        },
        error: (error) => {
          this.toast.error('Failed to cancel export');
        },
      });
  }
}
