# Reports Integration Architecture

## Overview
This document outlines the architecture for implementing comprehensive Reports functionality with full backend and frontend integration for the HealthData-in-Motion platform.

## Current State

### Backend (Quality Measure Service)
✓ **Implemented:**
- `GET /quality-measure/report/patient` - Patient quality reports
- `GET /quality-measure/report/population` - Population reports
- `QualityReportService` with caching
- In-memory report generation

✗ **Missing:**
- Report persistence and history
- PDF/Excel/CSV export
- Scheduled/recurring reports
- Report templates
- Report metadata (title, description, creator)

### Frontend (Angular Clinical Portal)
✓ **Implemented:**
- `EvaluationService.getPatientReport()`
- `EvaluationService.getPopulationReport()`
- Data models (QualityReport, PopulationQualityReport)

✗ **Missing:**
- UI components (placeholder only)
- Report generation forms
- Report viewing/visualization
- Export functionality
- Report history/list view

---

## Architecture Design

### 1. Backend Enhancements

#### 1.1 Report Persistence Layer

**New Entity: `SavedReportEntity`**
```java
@Entity
@Table(name = "saved_reports")
public class SavedReportEntity {
    @Id
    @GeneratedValue
    private UUID id;

    private String tenantId;
    private String reportType; // PATIENT, POPULATION, CARE_GAP
    private String reportName;
    private String description;

    // Filters
    private String patientId; // For patient reports
    private Integer year; // For population reports
    private LocalDate startDate;
    private LocalDate endDate;

    // Report data (JSONB)
    @Column(columnDefinition = "jsonb")
    private String reportData;

    // Metadata
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime generatedAt;

    // Status
    private String status; // GENERATING, COMPLETED, FAILED
    private String errorMessage;
}
```

**New Repository: `SavedReportRepository`**
```java
public interface SavedReportRepository extends JpaRepository<SavedReportEntity, UUID> {
    List<SavedReportEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<SavedReportEntity> findByTenantIdAndReportType(String tenantId, String reportType);
    List<SavedReportEntity> findByTenantIdAndCreatedByOrderByCreatedAtDesc(String tenantId, String createdBy);
    Optional<SavedReportEntity> findByTenantIdAndId(String tenantId, UUID id);
}
```

#### 1.2 Export Service

**New Service: `ReportExportService`**
```java
@Service
public class ReportExportService {

    // PDF Export using iText or Apache PDFBox
    public byte[] exportToPdf(SavedReportEntity report);

    // Excel Export using Apache POI
    public byte[] exportToExcel(SavedReportEntity report);

    // CSV Export
    public byte[] exportToCsv(SavedReportEntity report);
}
```

**Dependencies to Add:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>8.0.2</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

#### 1.3 Enhanced Report Service

**Update `QualityReportService`**
```java
@Service
public class QualityReportService {

    // Existing methods...

    // NEW: Save report to database
    public SavedReportEntity savePatientReport(String tenantId, String patientId,
                                               String reportName, String createdBy) {
        QualityReport report = getPatientQualityReport(tenantId, patientId);
        // Convert to SavedReportEntity and persist
        return savedReportRepository.save(entity);
    }

    // NEW: Save population report
    public SavedReportEntity savePopulationReport(String tenantId, int year,
                                                  String reportName, String createdBy) {
        PopulationQualityReport report = getPopulationQualityReport(tenantId, year);
        // Convert to SavedReportEntity and persist
        return savedReportRepository.save(entity);
    }

    // NEW: Get saved reports
    public List<SavedReportEntity> getSavedReports(String tenantId) {
        return savedReportRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    // NEW: Get report by ID
    public SavedReportEntity getSavedReport(String tenantId, UUID reportId) {
        return savedReportRepository.findByTenantIdAndId(tenantId, reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }
}
```

#### 1.4 New Controller Endpoints

**Update `QualityMeasureController`**
```java
// Save patient report
@PostMapping("/report/patient/save")
public ResponseEntity<SavedReportEntity> savePatientReport(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestParam("patient") String patientId,
    @RequestParam("name") String reportName,
    @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
);

// Save population report
@PostMapping("/report/population/save")
public ResponseEntity<SavedReportEntity> savePopulationReport(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestParam(value = "year") Integer year,
    @RequestParam("name") String reportName,
    @RequestParam(value = "createdBy", defaultValue = "system") String createdBy
);

// Get saved reports list
@GetMapping("/reports")
public ResponseEntity<List<SavedReportEntity>> getSavedReports(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestParam(value = "type", required = false) String reportType
);

// Get saved report by ID
@GetMapping("/reports/{reportId}")
public ResponseEntity<SavedReportEntity> getSavedReport(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @PathVariable UUID reportId
);

// Export report to PDF
@GetMapping("/reports/{reportId}/export/pdf")
public ResponseEntity<byte[]> exportReportToPdf(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @PathVariable UUID reportId
);

// Export report to Excel
@GetMapping("/reports/{reportId}/export/excel")
public ResponseEntity<byte[]> exportReportToExcel(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @PathVariable UUID reportId
);

// Export report to CSV
@GetMapping("/reports/{reportId}/export/csv")
public ResponseEntity<byte[]> exportReportToCsv(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @PathVariable UUID reportId
);

// Delete report
@DeleteMapping("/reports/{reportId}")
public ResponseEntity<Void> deleteReport(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @PathVariable UUID reportId
);
```

#### 1.5 Report Scheduling (Future Enhancement)

**New Service: `ReportSchedulingService`**
```java
@Service
public class ReportSchedulingService {

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void generateScheduledReports() {
        // Query scheduled_report_configs table
        // Generate reports based on schedule
        // Email/notify users
    }
}
```

---

### 2. Frontend Implementation

#### 2.1 Enhanced Models

**Update `quality-result.model.ts`**
```typescript
// Saved Report (matches backend SavedReportEntity)
export interface SavedReport {
  id: string;
  tenantId: string;
  reportType: ReportType;
  reportName: string;
  description?: string;
  patientId?: string;
  year?: number;
  startDate?: string;
  endDate?: string;
  reportData: any; // QualityReport or PopulationQualityReport
  createdBy: string;
  createdAt: string;
  generatedAt: string;
  status: ReportStatus;
  errorMessage?: string;
}

export type ReportType = 'PATIENT' | 'POPULATION' | 'CARE_GAP';
export type ReportStatus = 'GENERATING' | 'COMPLETED' | 'FAILED';

// Report Generation Request
export interface GenerateReportRequest {
  reportType: ReportType;
  reportName: string;
  description?: string;
  patientId?: string;
  year?: number;
  startDate?: string;
  endDate?: string;
}
```

#### 2.2 Enhanced Service

**Update `evaluation.service.ts`**
```typescript
// Save patient report
savePatientReport(patientId: string, reportName: string, createdBy: string = 'system'): Observable<SavedReport> {
  const url = buildQualityMeasureUrl('/report/patient/save', { patient: patientId, name: reportName, createdBy });
  return this.http.post<SavedReport>(url, {});
}

// Save population report
savePopulationReport(year: number, reportName: string, createdBy: string = 'system'): Observable<SavedReport> {
  const url = buildQualityMeasureUrl('/report/population/save', { year: year.toString(), name: reportName, createdBy });
  return this.http.post<SavedReport>(url, {});
}

// Get saved reports
getSavedReports(reportType?: ReportType): Observable<SavedReport[]> {
  const url = buildQualityMeasureUrl('/reports', reportType ? { type: reportType } : {});
  return this.http.get<SavedReport[]>(url);
}

// Get saved report by ID
getSavedReportById(reportId: string): Observable<SavedReport> {
  const url = buildQualityMeasureUrl(`/reports/${reportId}`);
  return this.http.get<SavedReport>(url);
}

// Export report to PDF
exportReportToPdf(reportId: string): Observable<Blob> {
  const url = buildQualityMeasureUrl(`/reports/${reportId}/export/pdf`);
  return this.http.get(url, { responseType: 'blob' });
}

// Export report to Excel
exportReportToExcel(reportId: string): Observable<Blob> {
  const url = buildQualityMeasureUrl(`/reports/${reportId}/export/excel`);
  return this.http.get(url, { responseType: 'blob' });
}

// Export report to CSV
exportReportToCsv(reportId: string): Observable<Blob> {
  const url = buildQualityMeasureUrl(`/reports/${reportId}/export/csv`);
  return this.http.get(url, { responseType: 'blob' });
}

// Delete report
deleteReport(reportId: string): Observable<void> {
  const url = buildQualityMeasureUrl(`/reports/${reportId}`);
  return this.http.delete<void>(url);
}
```

#### 2.3 New Component: Report Generation Form

**File: `reports.component.ts`**
```typescript
@Component({
  selector: 'app-reports',
  template: `
    <div class="page-container">
      <h1 class="page-title">Reports</h1>

      <!-- Tab Navigation -->
      <mat-tab-group>
        <!-- Generate Report Tab -->
        <mat-tab label="Generate Report">
          <app-report-generation-form
            (onReportGenerated)="handleReportGenerated($event)">
          </app-report-generation-form>
        </mat-tab>

        <!-- Saved Reports Tab -->
        <mat-tab label="Saved Reports">
          <app-saved-reports-list
            [reports]="savedReports"
            (onViewReport)="viewReport($event)"
            (onExportReport)="exportReport($event)"
            (onDeleteReport)="deleteReport($event)">
          </app-saved-reports-list>
        </mat-tab>
      </mat-tab-group>
    </div>
  `
})
export class ReportsComponent implements OnInit {
  savedReports: SavedReport[] = [];

  ngOnInit() {
    this.loadSavedReports();
  }

  loadSavedReports() {
    this.evaluationService.getSavedReports().subscribe(reports => {
      this.savedReports = reports;
    });
  }

  handleReportGenerated(report: SavedReport) {
    this.savedReports.unshift(report);
  }

  viewReport(report: SavedReport) {
    this.router.navigate(['/reports', report.id]);
  }

  exportReport(data: { reportId: string, format: 'pdf' | 'excel' | 'csv' }) {
    // Handle export
  }

  deleteReport(reportId: string) {
    // Handle delete
  }
}
```

#### 2.4 New Component: Report Generation Form

**File: `report-generation-form.component.ts`**
```typescript
@Component({
  selector: 'app-report-generation-form',
  template: `
    <mat-card>
      <mat-card-content>
        <form [formGroup]="reportForm" (ngSubmit)="onSubmit()">

          <!-- Report Type -->
          <mat-form-field>
            <mat-label>Report Type</mat-label>
            <mat-select formControlName="reportType" (selectionChange)="onReportTypeChange()">
              <mat-option value="PATIENT">Patient Quality Report</mat-option>
              <mat-option value="POPULATION">Population Quality Report</mat-option>
              <mat-option value="CARE_GAP">Care Gap Report</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Patient Selection (for PATIENT reports) -->
          <mat-form-field *ngIf="reportForm.value.reportType === 'PATIENT'">
            <mat-label>Patient</mat-label>
            <input matInput formControlName="patientId" [matAutocomplete]="auto">
            <mat-autocomplete #auto="matAutocomplete">
              <mat-option *ngFor="let patient of filteredPatients | async" [value]="patient.id">
                {{ patient.fullName }} (MRN: {{ patient.mrn }})
              </mat-option>
            </mat-autocomplete>
          </mat-form-field>

          <!-- Year Selection (for POPULATION reports) -->
          <mat-form-field *ngIf="reportForm.value.reportType === 'POPULATION'">
            <mat-label>Year</mat-label>
            <mat-select formControlName="year">
              <mat-option *ngFor="let y of availableYears" [value]="y">{{ y }}</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Report Name -->
          <mat-form-field>
            <mat-label>Report Name</mat-label>
            <input matInput formControlName="reportName" placeholder="My Quality Report">
          </mat-form-field>

          <!-- Description -->
          <mat-form-field>
            <mat-label>Description (optional)</mat-label>
            <textarea matInput formControlName="description" rows="3"></textarea>
          </mat-form-field>

          <!-- Actions -->
          <div class="form-actions">
            <button mat-raised-button color="primary" type="submit" [disabled]="!reportForm.valid || isGenerating">
              <mat-icon>assessment</mat-icon>
              Generate Report
            </button>
          </div>

          <!-- Progress -->
          <mat-progress-bar *ngIf="isGenerating" mode="indeterminate"></mat-progress-bar>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class ReportGenerationFormComponent {
  reportForm: FormGroup;
  isGenerating = false;
  @Output() onReportGenerated = new EventEmitter<SavedReport>();

  onSubmit() {
    if (this.reportForm.valid) {
      this.isGenerating = true;
      const formValue = this.reportForm.value;

      if (formValue.reportType === 'PATIENT') {
        this.evaluationService.savePatientReport(
          formValue.patientId,
          formValue.reportName,
          'current-user'
        ).subscribe(report => {
          this.isGenerating = false;
          this.onReportGenerated.emit(report);
        });
      } else if (formValue.reportType === 'POPULATION') {
        this.evaluationService.savePopulationReport(
          formValue.year,
          formValue.reportName,
          'current-user'
        ).subscribe(report => {
          this.isGenerating = false;
          this.onReportGenerated.emit(report);
        });
      }
    }
  }
}
```

#### 2.5 New Component: Saved Reports List

**File: `saved-reports-list.component.ts`**
```typescript
@Component({
  selector: 'app-saved-reports-list',
  template: `
    <mat-table [dataSource]="dataSource" matSort>

      <ng-container matColumnDef="reportName">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Report Name</mat-header-cell>
        <mat-cell *matCellDef="let report">{{ report.reportName }}</mat-cell>
      </ng-container>

      <ng-container matColumnDef="reportType">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Type</mat-header-cell>
        <mat-cell *matCellDef="let report">
          <mat-chip>{{ report.reportType }}</mat-chip>
        </mat-cell>
      </ng-container>

      <ng-container matColumnDef="createdAt">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Created</mat-header-cell>
        <mat-cell *matCellDef="let report">{{ report.createdAt | date:'short' }}</mat-cell>
      </ng-container>

      <ng-container matColumnDef="status">
        <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
        <mat-cell *matCellDef="let report">
          <mat-chip [color]="getStatusColor(report.status)">
            {{ report.status }}
          </mat-chip>
        </mat-cell>
      </ng-container>

      <ng-container matColumnDef="actions">
        <mat-header-cell *matHeaderCellDef>Actions</mat-header-cell>
        <mat-cell *matCellDef="let report">
          <button mat-icon-button [matMenuTriggerFor]="menu">
            <mat-icon>more_vert</mat-icon>
          </button>
          <mat-menu #menu="matMenu">
            <button mat-menu-item (click)="onViewReport.emit(report)">
              <mat-icon>visibility</mat-icon>
              View
            </button>
            <button mat-menu-item (click)="onExportReport.emit({reportId: report.id, format: 'pdf'})">
              <mat-icon>picture_as_pdf</mat-icon>
              Export PDF
            </button>
            <button mat-menu-item (click)="onExportReport.emit({reportId: report.id, format: 'excel'})">
              <mat-icon>grid_on</mat-icon>
              Export Excel
            </button>
            <button mat-menu-item (click)="onDeleteReport.emit(report.id)">
              <mat-icon>delete</mat-icon>
              Delete
            </button>
          </mat-menu>
        </mat-cell>
      </ng-container>

      <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
      <mat-row *matRowDef="let row; columns: displayedColumns;"></mat-row>
    </mat-table>
  `
})
export class SavedReportsListComponent {
  @Input() reports: SavedReport[] = [];
  @Output() onViewReport = new EventEmitter<SavedReport>();
  @Output() onExportReport = new EventEmitter<{reportId: string, format: string}>();
  @Output() onDeleteReport = new EventEmitter<string>();

  displayedColumns = ['reportName', 'reportType', 'createdAt', 'status', 'actions'];
  dataSource: MatTableDataSource<SavedReport>;
}
```

#### 2.6 New Component: Report Detail Viewer

**File: `report-detail.component.ts`**
```typescript
@Component({
  selector: 'app-report-detail',
  template: `
    <div class="report-container" *ngIf="report">
      <!-- Report Header -->
      <div class="report-header">
        <h1>{{ report.reportName }}</h1>
        <p class="report-meta">
          Generated {{ report.generatedAt | date:'medium' }} by {{ report.createdBy }}
        </p>
      </div>

      <!-- Patient Report -->
      <div *ngIf="report.reportType === 'PATIENT'">
        <app-patient-report-viewer [reportData]="report.reportData">
        </app-patient-report-viewer>
      </div>

      <!-- Population Report -->
      <div *ngIf="report.reportType === 'POPULATION'">
        <app-population-report-viewer [reportData]="report.reportData">
        </app-population-report-viewer>
      </div>

      <!-- Actions -->
      <div class="report-actions">
        <button mat-raised-button (click)="exportPdf()">
          <mat-icon>picture_as_pdf</mat-icon>
          Export PDF
        </button>
        <button mat-raised-button (click)="exportExcel()">
          <mat-icon>grid_on</mat-icon>
          Export Excel
        </button>
      </div>
    </div>
  `
})
export class ReportDetailComponent implements OnInit {
  report: SavedReport | null = null;

  ngOnInit() {
    const reportId = this.route.snapshot.params['id'];
    this.evaluationService.getSavedReportById(reportId).subscribe(report => {
      this.report = report;
    });
  }
}
```

---

## Database Migration

### Migration: Create `saved_reports` table

```sql
CREATE TABLE saved_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    description TEXT,

    patient_id UUID,
    year INTEGER,
    start_date DATE,
    end_date DATE,

    report_data JSONB NOT NULL,

    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    error_message TEXT,

    CONSTRAINT fk_saved_reports_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_saved_reports_tenant ON saved_reports(tenant_id);
CREATE INDEX idx_saved_reports_type ON saved_reports(report_type);
CREATE INDEX idx_saved_reports_created_at ON saved_reports(created_at);
CREATE INDEX idx_saved_reports_patient ON saved_reports(patient_id) WHERE patient_id IS NOT NULL;
```

---

## Implementation Phases

### Phase 1: Backend Foundation (2-3 hours)
1. Create `SavedReportEntity` and `SavedReportRepository`
2. Database migration for `saved_reports` table
3. Update `QualityReportService` with save/retrieve methods
4. Add new controller endpoints

### Phase 2: Export Service (2-3 hours)
1. Add PDF export dependencies
2. Implement `ReportExportService` for PDF
3. Implement Excel export
4. Implement CSV export
5. Add export endpoints to controller

### Phase 3: Frontend Models & Service (1-2 hours)
1. Update `quality-result.model.ts` with new interfaces
2. Update `evaluation.service.ts` with new methods
3. Add export service methods

### Phase 4: Frontend UI Components (3-4 hours)
1. Create `ReportGenerationFormComponent`
2. Create `SavedReportsListComponent`
3. Create `ReportDetailComponent`
4. Create `PatientReportViewerComponent`
5. Create `PopulationReportViewerComponent`
6. Update main `ReportsComponent`

### Phase 5: Integration & Testing (2-3 hours)
1. Integration testing
2. E2E testing
3. Export functionality testing
4. Error handling and edge cases

### Phase 6: Polish & Documentation (1-2 hours)
1. Add loading states
2. Add error messages
3. Add success notifications
4. Update API documentation
5. Update user documentation

---

## Total Estimated Time: 11-17 hours

## Success Criteria
- ✅ Reports can be generated and saved to database
- ✅ Reports can be viewed in the UI with charts
- ✅ Reports can be exported to PDF, Excel, CSV
- ✅ Report history is maintained
- ✅ Care gaps are displayed in patient reports
- ✅ Population reports show aggregated statistics
- ✅ All functionality is multi-tenant aware
- ✅ Role-based access control is enforced
- ✅ Comprehensive error handling
