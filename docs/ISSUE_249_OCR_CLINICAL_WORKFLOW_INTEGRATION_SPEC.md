# Issue #249 - OCR Clinical Workflow Integration: Technical Specification

**Issue:** Integrate OCR document processing with Clinical Portal workflows
**Status:** 📋 **SPECIFICATION** (Ready for Implementation)
**Date:** January 24, 2026
**Depends On:** Issue #245 (OCR Backend - ✅ COMPLETE)
**Task:** #9 - Integrate OCR with clinical workflows

---

## Executive Summary

Create user-facing components in the Clinical Portal to leverage the completed OCR backend infrastructure (Issue #245). This integration will enable clinicians to upload clinical documents (PDFs, images), monitor OCR processing status, and search extracted text to quickly find patient information.

**Backend Infrastructure Status (✅ Complete):**
- ✅ OCR Service with Tesseract integration
- ✅ Async processing with status tracking
- ✅ PostgreSQL full-text search with GIN indexing
- ✅ REST API endpoints (upload, status, reprocess, search)
- ✅ Multi-tenant isolation
- ✅ Comprehensive integration tests (87.5% pass rate)

**Frontend Work Required (📋 Pending):**
- 📋 Document upload UI component
- 📋 OCR status monitoring interface
- 📋 Full-text search interface for OCR documents
- 📋 Integration with Patient Detail page
- 📋 Document management workflows

---

## Use Cases

### Use Case 1: Upload Clinical Document for Patient

**Actor:** Clinician (ADMIN or EVALUATOR role)
**Precondition:** User is viewing a patient's detail page
**Trigger:** User clicks "Upload Document" button

**Main Flow:**
1. User clicks "Upload Document" button
2. System displays file picker dialog
3. User selects PDF or image file (PNG, JPG, JPEG, TIFF)
4. System validates file size (≤ 10 MB) and type
5. System uploads file to backend via `/api/documents/clinical/{documentId}/upload`
6. Backend triggers async OCR processing
7. System displays upload success message with "Processing OCR..." status
8. System automatically polls OCR status every 2 seconds
9. When OCR completes, system displays "OCR Complete" with extracted text preview

**Alternative Flow 1 - File Too Large:**
- 4a. File size > 10 MB
- 4b. System displays error: "File size exceeds 10 MB limit"
- 4c. User must compress file or use a different file

**Alternative Flow 2 - Unsupported File Type:**
- 4a. File type not in [PDF, PNG, JPG, JPEG, TIFF]
- 4b. System displays error: "Unsupported file type. Please upload PDF or image files."

**Alternative Flow 3 - OCR Processing Failure:**
- 8a. OCR status changes to "FAILED"
- 8b. System displays error message with "Retry OCR" button
- 8c. User clicks "Retry OCR"
- 8d. System calls `/api/documents/clinical/attachments/{id}/reprocess-ocr`
- 8e. OCR processing restarted

---

### Use Case 2: Search OCR-Extracted Text

**Actor:** Clinician
**Precondition:** Documents with completed OCR exist
**Trigger:** User navigates to "Document Search" page or uses global search

**Main Flow:**
1. User enters search query (e.g., "diabetes HbA1c")
2. System calls `/api/documents/clinical/search-ocr?query=diabetes%20HbA1c&page=0&size=20`
3. Backend performs PostgreSQL full-text search with relevance ranking
4. System displays search results with:
   - Document filename
   - Patient name (if associated)
   - Upload date
   - OCR text excerpt with search term highlighted
   - Relevance score (optional)
5. User clicks on search result
6. System navigates to patient detail page and scrolls to document

**Alternative Flow - No Results Found:**
- 3a. No documents match search query
- 3b. System displays: "No documents found containing 'diabetes HbA1c'"
- 3c. System suggests: "Try different search terms or upload more documents"

---

### Use Case 3: View OCR Processing Status

**Actor:** Clinician
**Precondition:** User has uploaded documents
**Trigger:** User views patient's documents tab

**Main Flow:**
1. User clicks "Documents" tab on patient detail page
2. System displays list of all documents for patient
3. For each document, system shows:
   - Filename
   - File type (PDF, PNG, etc.)
   - Upload date
   - OCR status badge:
     - 🟡 PENDING - "OCR queued for processing"
     - 🔵 PROCESSING - "Extracting text..." with spinner
     - 🟢 COMPLETED - "OCR complete" with text preview
     - 🔴 FAILED - "OCR failed" with retry button
4. User can click on document to view details and extracted text

---

### Use Case 4: Retry Failed OCR Processing

**Actor:** Clinician
**Precondition:** Document OCR status is "FAILED"
**Trigger:** User clicks "Retry OCR" button

**Main Flow:**
1. User clicks "Retry OCR" button on failed document
2. System confirms action: "Retry OCR processing for this document?"
3. User confirms
4. System calls `/api/documents/clinical/attachments/{id}/reprocess-ocr`
5. Backend resets OCR status to "PENDING" and re-queues for processing
6. System displays "OCR retry initiated" message
7. System polls OCR status until completion

---

## Technical Requirements

### 1. Document Upload Component

**Component Name:** `DocumentUploadComponent`
**Location:** `src/app/components/document-upload/`

**Inputs:**
- `@Input() documentId: string` - Clinical document ID to associate upload
- `@Input() patientId: string` - Patient ID for context
- `@Input() allowedFileTypes: string[]` - Default: ['pdf', 'png', 'jpg', 'jpeg', 'tiff']
- `@Input() maxFileSize: number` - Default: 10485760 (10 MB)

**Outputs:**
- `@Output() uploadSuccess: EventEmitter<AttachmentUploadResponse>`
- `@Output() uploadError: EventEmitter<string>`
- `@Output() ocrComplete: EventEmitter<OcrCompletionEvent>`

**Key Features:**
- Drag-and-drop file upload
- File type and size validation
- Progress bar during upload
- OCR status polling (2-second interval)
- Accessible with ARIA attributes
- HIPAA-compliant logging (no PHI in console)

**Template Structure:**
```html
<div class="upload-container" [class.drag-over]="isDragOver">
  <!-- Drag-and-drop zone -->
  <div class="drop-zone"
       (drop)="onFileDrop($event)"
       (dragover)="onDragOver($event)"
       (dragleave)="onDragLeave($event)"
       role="region"
       aria-label="Document upload area">
    <mat-icon>cloud_upload</mat-icon>
    <p>Drag and drop files here, or click to browse</p>
    <input type="file"
           #fileInput
           [accept]="acceptedMimeTypes"
           (change)="onFileSelected($event)"
           aria-label="Choose file to upload">
    <button mat-raised-button color="primary" (click)="fileInput.click()">
      Choose File
    </button>
  </div>

  <!-- Upload progress -->
  <div *ngIf="uploading" class="upload-progress">
    <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    <span>Uploading {{ selectedFile?.name }}...</span>
  </div>

  <!-- OCR status -->
  <div *ngIf="ocrStatus" class="ocr-status">
    <mat-chip [color]="getOcrStatusColor(ocrStatus)">
      {{ getOcrStatusLabel(ocrStatus) }}
    </mat-chip>
    <mat-spinner *ngIf="ocrStatus === 'PROCESSING'" diameter="20"></mat-spinner>
  </div>

  <!-- Uploaded files list -->
  <mat-list class="uploaded-files">
    <mat-list-item *ngFor="let file of uploadedFiles">
      <mat-icon matListItemIcon>{{ getFileIcon(file.mimeType) }}</mat-icon>
      <div matListItemTitle>{{ file.fileName }}</div>
      <div matListItemLine>
        Uploaded: {{ file.uploadDate | date }}
        <span class="ocr-status-badge" [class]="file.ocrStatus">
          {{ file.ocrStatus }}
        </span>
      </div>
      <button mat-icon-button
              *ngIf="file.ocrStatus === 'FAILED'"
              (click)="retryOcr(file.id)"
              aria-label="Retry OCR for {{ file.fileName }}">
        <mat-icon>refresh</mat-icon>
      </button>
    </mat-list-item>
  </mat-list>
</div>
```

**Service Methods:**
```typescript
export class DocumentUploadService {
  uploadDocument(
    documentId: string,
    file: File
  ): Observable<AttachmentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AttachmentUploadResponse>(
      `/api/documents/clinical/${documentId}/upload`,
      formData,
      { headers: { 'X-Tenant-ID': this.tenantId } }
    );
  }

  pollOcrStatus(attachmentId: string): Observable<OcrStatus> {
    return interval(2000).pipe(
      switchMap(() =>
        this.http.get<OcrStatusResponse>(
          `/api/documents/clinical/attachments/${attachmentId}/ocr-status`,
          { headers: { 'X-Tenant-ID': this.tenantId } }
        )
      ),
      map(response => response.ocrStatus),
      distinctUntilChanged(),
      takeWhile(status => status === 'PENDING' || status === 'PROCESSING', true)
    );
  }

  retryOcr(attachmentId: string): Observable<void> {
    return this.http.post<void>(
      `/api/documents/clinical/attachments/${attachmentId}/reprocess-ocr`,
      {},
      { headers: { 'X-Tenant-ID': this.tenantId } }
    );
  }
}
```

---

### 2. OCR Search Component

**Component Name:** `OcrSearchComponent`
**Location:** `src/app/components/ocr-search/`

**Inputs:**
- `@Input() placeholder: string` - Default: "Search clinical documents..."
- `@Input() resultsPerPage: number` - Default: 20

**Outputs:**
- `@Output() resultSelected: EventEmitter<OcrSearchResult>`
- `@Output() searchPerformed: EventEmitter<string>`

**Key Features:**
- Real-time search with debounce (500ms)
- Pagination support
- Search term highlighting in excerpts
- Relevance score display
- Empty state handling
- Accessible with screen reader support

**Template Structure:**
```html
<div class="ocr-search-container">
  <!-- Search input -->
  <mat-form-field class="search-field" appearance="outline">
    <mat-label>Search clinical documents</mat-label>
    <input matInput
           [(ngModel)]="searchQuery"
           (ngModelChange)="onSearchChange()"
           placeholder="e.g., diabetes HbA1c"
           aria-label="Search clinical documents by content"
           role="searchbox">
    <mat-icon matPrefix>search</mat-icon>
    <button mat-icon-button matSuffix
            *ngIf="searchQuery"
            (click)="clearSearch()"
            aria-label="Clear search">
      <mat-icon>clear</mat-icon>
    </button>
  </mat-form-field>

  <!-- Loading state -->
  <app-loading-overlay
    [isLoading]="searching"
    message="Searching documents..."
    [spinnerSize]="32">
  </app-loading-overlay>

  <!-- Search results -->
  <div *ngIf="!searching && searchResults.length > 0" class="search-results">
    <div class="results-header">
      <span>{{ totalResults }} result(s) found</span>
    </div>

    <mat-list>
      <mat-list-item *ngFor="let result of searchResults"
                     (click)="selectResult(result)"
                     class="search-result-item"
                     role="button"
                     tabindex="0"
                     [attr.aria-label]="'View document ' + result.fileName">
        <mat-icon matListItemIcon>{{ getFileIcon(result.mimeType) }}</mat-icon>
        <div matListItemTitle>
          {{ result.fileName }}
          <mat-chip class="relevance-chip" *ngIf="result.relevanceScore">
            Relevance: {{ result.relevanceScore | number:'1.0-0' }}%
          </mat-chip>
        </div>
        <div matListItemLine class="patient-info">
          Patient: {{ result.patientName }} (MRN: {{ result.patientMrn }})
        </div>
        <div matListItemLine class="ocr-excerpt" [innerHTML]="highlightSearchTerms(result.ocrExcerpt)">
        </div>
        <div matListItemLine class="upload-date">
          Uploaded: {{ result.uploadDate | date }}
        </div>
      </mat-list-item>
    </mat-list>

    <!-- Pagination -->
    <mat-paginator
      [length]="totalResults"
      [pageSize]="resultsPerPage"
      [pageSizeOptions]="[10, 20, 50, 100]"
      (page)="onPageChange($event)"
      aria-label="Select page of search results">
    </mat-paginator>
  </div>

  <!-- Empty state -->
  <div *ngIf="!searching && searchQuery && searchResults.length === 0" class="empty-state">
    <mat-icon>search_off</mat-icon>
    <h3>No documents found</h3>
    <p>No documents contain "{{ searchQuery }}"</p>
    <p class="suggestion">Try different search terms or upload more documents</p>
  </div>
</div>
```

**Service Methods:**
```typescript
export class OcrSearchService {
  searchDocuments(
    query: string,
    page: number = 0,
    size: number = 20
  ): Observable<OcrSearchResponse> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<OcrSearchResponse>(
      '/api/documents/clinical/search-ocr',
      {
        params,
        headers: { 'X-Tenant-ID': this.tenantId }
      }
    );
  }

  highlightSearchTerms(text: string, searchQuery: string): string {
    if (!searchQuery) return text;

    const terms = searchQuery.split(' ').filter(t => t.length > 2);
    let highlighted = text;

    terms.forEach(term => {
      const regex = new RegExp(`(${term})`, 'gi');
      highlighted = highlighted.replace(regex, '<mark>$1</mark>');
    });

    return highlighted;
  }
}
```

---

### 3. Patient Detail Page Integration

**Modification:** Add "Documents" tab to Patient Detail page
**Location:** `src/app/pages/patient-detail/patient-detail.component.ts`

**New Tab Structure:**
```html
<mat-tab-group [(selectedIndex)]="selectedTabIndex">
  <mat-tab label="Demographics">
    <!-- Existing demographics content -->
  </mat-tab>

  <mat-tab label="Clinical Summary">
    <!-- Existing clinical summary content -->
  </mat-tab>

  <mat-tab label="Evaluations">
    <!-- Existing evaluations content -->
  </mat-tab>

  <!-- NEW: Documents Tab -->
  <mat-tab label="Documents">
    <div class="documents-tab-content">
      <!-- Document upload component -->
      <app-document-upload
        [documentId]="documentId"
        [patientId]="patient.id"
        (uploadSuccess)="onDocumentUploaded($event)"
        (ocrComplete)="onOcrComplete($event)">
      </app-document-upload>

      <mat-divider></mat-divider>

      <!-- OCR search within patient's documents -->
      <app-ocr-search
        [placeholder]="'Search ' + patient.fullName + '\'s documents...'"
        (resultSelected)="onSearchResultSelected($event)">
      </app-ocr-search>
    </div>
  </mat-tab>
</mat-tab-group>
```

**Component Logic:**
```typescript
export class PatientDetailComponent implements OnInit {
  documentId: string; // Clinical document ID for this patient
  patient: Patient;

  ngOnInit(): void {
    this.loadPatient();
    this.createOrGetClinicalDocument();
  }

  private createOrGetClinicalDocument(): void {
    // Create or retrieve clinical document for this patient
    this.clinicalDocumentService.getOrCreateForPatient(this.patient.id)
      .subscribe(document => {
        this.documentId = document.id;
      });
  }

  onDocumentUploaded(response: AttachmentUploadResponse): void {
    this.logger.info('Document uploaded', response.attachmentId);
    this.auditService.logDocumentUpload({
      patientId: this.patient.id,
      attachmentId: response.attachmentId,
      fileName: response.fileName,
    });
    // Optionally refresh document list
  }

  onOcrComplete(event: OcrCompletionEvent): void {
    this.logger.info('OCR processing complete', event.attachmentId);
    // Show success message
    this.snackBar.open('OCR processing complete', 'Close', { duration: 3000 });
  }

  onSearchResultSelected(result: OcrSearchResult): void {
    // Scroll to document in list or open document viewer
    this.viewDocument(result.attachmentId);
  }

  private viewDocument(attachmentId: string): void {
    // Open document viewer dialog or navigate to document detail
    this.dialog.open(DocumentViewerDialog, {
      data: { attachmentId },
      width: '80vw',
      height: '90vh',
    });
  }
}
```

---

### 4. Document Viewer Dialog (Optional Enhancement)

**Component Name:** `DocumentViewerDialog`
**Location:** `src/app/dialogs/document-viewer/`

**Purpose:** Display uploaded document with extracted OCR text side-by-side

**Template Structure:**
```html
<h2 mat-dialog-title>
  {{ document.fileName }}
  <button mat-icon-button mat-dialog-close aria-label="Close document viewer">
    <mat-icon>close</mat-icon>
  </button>
</h2>

<mat-dialog-content>
  <div class="viewer-container">
    <!-- Left: Document preview -->
    <div class="document-preview">
      <img *ngIf="isPdfOrImage(document.mimeType)"
           [src]="document.fileUrl"
           alt="Document preview">
      <iframe *ngIf="document.mimeType === 'application/pdf'"
              [src]="document.fileUrl | safe"
              width="100%"
              height="100%">
      </iframe>
    </div>

    <!-- Right: OCR text -->
    <div class="ocr-text-panel">
      <h3>Extracted Text</h3>
      <div *ngIf="document.ocrStatus === 'COMPLETED'" class="ocr-text">
        {{ document.ocrText }}
      </div>
      <div *ngIf="document.ocrStatus === 'PROCESSING'" class="ocr-processing">
        <mat-spinner diameter="32"></mat-spinner>
        <p>Processing OCR...</p>
      </div>
      <div *ngIf="document.ocrStatus === 'FAILED'" class="ocr-failed">
        <mat-icon color="warn">error</mat-icon>
        <p>OCR processing failed</p>
        <button mat-raised-button color="primary" (click)="retryOcr()">
          Retry OCR
        </button>
      </div>
    </div>
  </div>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button mat-dialog-close>Close</button>
  <button mat-raised-button color="primary" (click)="downloadDocument()">
    <mat-icon>download</mat-icon>
    Download
  </button>
</mat-dialog-actions>
```

---

## Data Models (TypeScript Interfaces)

```typescript
// Attachment upload response
export interface AttachmentUploadResponse {
  attachmentId: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  uploadDate: string;
  ocrStatus: OcrStatus;
}

// OCR status
export type OcrStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

// OCR status response
export interface OcrStatusResponse {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  ocrProcessingDate?: string;
  errorMessage?: string;
}

// OCR search response
export interface OcrSearchResponse {
  content: OcrSearchResult[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  empty: boolean;
}

// OCR search result
export interface OcrSearchResult {
  attachmentId: string;
  fileName: string;
  mimeType: string;
  uploadDate: string;
  ocrText: string;
  ocrExcerpt: string;
  relevanceScore?: number;
  patientId: string;
  patientName: string;
  patientMrn: string;
}

// OCR completion event
export interface OcrCompletionEvent {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  errorMessage?: string;
}

// Clinical document
export interface ClinicalDocument {
  id: string;
  patientId: string;
  documentType: string;
  createdDate: string;
  attachments: DocumentAttachment[];
}

// Document attachment
export interface DocumentAttachment {
  id: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  filePath: string;
  uploadDate: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  ocrProcessingDate?: string;
}
```

---

## Acceptance Criteria

### Functional Requirements

- [ ] **UC1: Document Upload**
  - [ ] User can upload PDF and image files via drag-and-drop or file picker
  - [ ] System validates file size (≤ 10 MB) and type
  - [ ] Upload progress indicator displayed
  - [ ] OCR processing status automatically polled every 2 seconds
  - [ ] Success/error messages displayed appropriately

- [ ] **UC2: OCR Status Monitoring**
  - [ ] All uploaded documents show OCR status badge (PENDING, PROCESSING, COMPLETED, FAILED)
  - [ ] Status updates in real-time while user is viewing
  - [ ] Failed OCR shows "Retry" button
  - [ ] Completed OCR shows text preview

- [ ] **UC3: Full-Text Search**
  - [ ] User can search OCR-extracted text across all documents
  - [ ] Search supports multi-word queries
  - [ ] Results show search term highlighting
  - [ ] Results paginated (20 per page)
  - [ ] Results sorted by relevance score

- [ ] **UC4: Patient Detail Integration**
  - [ ] "Documents" tab added to patient detail page
  - [ ] Tab shows document upload component and search
  - [ ] Search scoped to current patient's documents
  - [ ] Clicking search result scrolls to document or opens viewer

### Non-Functional Requirements

- [ ] **Accessibility**
  - [ ] All components WCAG 2.1 Level AA compliant
  - [ ] ARIA labels on all interactive elements
  - [ ] Keyboard navigation support
  - [ ] Screen reader compatible

- [ ] **Performance**
  - [ ] File upload completes within 5 seconds for 5 MB file
  - [ ] OCR status polling does not degrade UI performance
  - [ ] Search results return within 500ms for 1000 documents

- [ ] **HIPAA Compliance**
  - [ ] No PHI logged to console (use LoggerService)
  - [ ] All API calls automatically audited (HTTP interceptor)
  - [ ] Multi-tenant isolation enforced (X-Tenant-ID header)

- [ ] **Error Handling**
  - [ ] User-friendly error messages for all failure scenarios
  - [ ] Network errors handled gracefully with retry options
  - [ ] OCR failures allow retry without re-uploading file

---

## Implementation Phases

### Phase 1: Core Upload Functionality (8-12 hours)

**Deliverables:**
1. `DocumentUploadComponent` with drag-and-drop
2. `DocumentUploadService` with upload and status polling
3. File validation (size, type)
4. OCR status display (PENDING, PROCESSING, COMPLETED, FAILED)
5. Retry OCR functionality

**Acceptance Criteria:**
- User can upload PDF/image files
- OCR status updates automatically
- Failed OCR can be retried

---

### Phase 2: Search Functionality (6-8 hours)

**Deliverables:**
1. `OcrSearchComponent` with real-time search
2. `OcrSearchService` with pagination
3. Search term highlighting
4. Relevance score display
5. Empty state handling

**Acceptance Criteria:**
- User can search OCR text
- Results show highlighted search terms
- Pagination works correctly

---

### Phase 3: Patient Detail Integration (4-6 hours)

**Deliverables:**
1. "Documents" tab on patient detail page
2. Integration of upload and search components
3. Clinical document creation/retrieval
4. Audit logging for document actions

**Acceptance Criteria:**
- Documents tab visible on patient detail
- Upload and search work within patient context
- All actions audited

---

### Phase 4: Document Viewer (Optional - 6-8 hours)

**Deliverables:**
1. `DocumentViewerDialog` component
2. Side-by-side document and OCR text display
3. Download functionality
4. Full-screen view support

**Acceptance Criteria:**
- User can view document and extracted text
- Download button works
- Dialog is keyboard accessible

---

## Testing Strategy

### Unit Tests

```typescript
describe('DocumentUploadComponent', () => {
  it('should validate file size (max 10 MB)', () => {
    const file = new File(['content'], 'large.pdf', { type: 'application/pdf' });
    Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 }); // 11 MB
    expect(component.validateFileSize(file)).toBe(false);
  });

  it('should validate file type (PDF, PNG, JPG, JPEG, TIFF)', () => {
    expect(component.validateFileType('application/pdf')).toBe(true);
    expect(component.validateFileType('image/png')).toBe(true);
    expect(component.validateFileType('text/plain')).toBe(false);
  });

  it('should poll OCR status every 2 seconds', fakeAsync(() => {
    const spy = spyOn(service, 'pollOcrStatus').and.returnValue(of('COMPLETED'));
    component.startOcrStatusPolling('attachment-123');
    tick(2000);
    expect(spy).toHaveBeenCalled();
  }));
});

describe('OcrSearchComponent', () => {
  it('should debounce search input (500ms)', fakeAsync(() => {
    const spy = spyOn(service, 'searchDocuments').and.returnValue(of(mockResults));
    component.searchQuery = 'diabetes';
    component.onSearchChange();
    tick(400); // Before debounce
    expect(spy).not.toHaveBeenCalled();
    tick(100); // After debounce
    expect(spy).toHaveBeenCalled();
  }));

  it('should highlight search terms in excerpts', () => {
    const excerpt = 'Patient has diabetes type 2';
    const highlighted = component.highlightSearchTerms(excerpt, 'diabetes');
    expect(highlighted).toContain('<mark>diabetes</mark>');
  });
});
```

### Integration Tests

```typescript
describe('OCR Workflow Integration', () => {
  it('should upload file and complete OCR end-to-end', async () => {
    // 1. Upload file
    const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
    const uploadResponse = await uploadService.uploadDocument('doc-123', file).toPromise();
    expect(uploadResponse.attachmentId).toBeTruthy();

    // 2. Poll OCR status until complete
    await new Promise((resolve) => {
      uploadService.pollOcrStatus(uploadResponse.attachmentId).subscribe({
        next: (status) => {
          if (status === 'COMPLETED') resolve(true);
        },
      });
    });

    // 3. Search for OCR text
    const searchResults = await searchService.searchDocuments('test content').toPromise();
    expect(searchResults.content.length).toBeGreaterThan(0);
    expect(searchResults.content[0].attachmentId).toBe(uploadResponse.attachmentId);
  });
});
```

### E2E Tests (Playwright/Cypress)

```typescript
test('Upload document and verify OCR completion', async ({ page }) => {
  // Navigate to patient detail
  await page.goto('/patients/patient-123');

  // Click Documents tab
  await page.click('text=Documents');

  // Upload file
  const fileInput = await page.locator('input[type="file"]');
  await fileInput.setInputFiles('test-files/diabetes-report.pdf');

  // Wait for upload success
  await page.waitForSelector('text=Upload successful');

  // Wait for OCR completion (max 30 seconds)
  await page.waitForSelector('text=OCR complete', { timeout: 30000 });

  // Verify extracted text preview
  const preview = await page.locator('.ocr-text-preview');
  await expect(preview).toContainText('HbA1c');
});

test('Search OCR documents and view result', async ({ page }) => {
  await page.goto('/patients/patient-123');
  await page.click('text=Documents');

  // Enter search query
  await page.fill('[aria-label="Search clinical documents by content"]', 'diabetes HbA1c');

  // Wait for search results
  await page.waitForSelector('.search-result-item');

  // Click first result
  await page.click('.search-result-item:first-child');

  // Verify document viewer opens
  await expect(page.locator('[role="dialog"]')).toBeVisible();
});
```

---

## HIPAA Compliance Checklist

- [ ] **Logging Compliance**
  - [ ] All components use LoggerService (no console.log)
  - [ ] PHI filtering enabled in production
  - [ ] Audit logging via HTTP interceptor

- [ ] **Multi-Tenant Isolation**
  - [ ] All API calls include X-Tenant-ID header
  - [ ] Search results scoped to tenant
  - [ ] Document access enforced at backend

- [ ] **Session Timeout**
  - [ ] Document upload respects 15-minute idle timeout
  - [ ] OCR status polling stops on session timeout
  - [ ] User warned before timeout during active uploads

- [ ] **Audit Trail**
  - [ ] Document upload logged with patient ID, file name, timestamp
  - [ ] OCR retry logged with attachment ID, reason
  - [ ] Search queries logged with query text, result count
  - [ ] Document view logged with attachment ID, patient ID

---

## Performance Considerations

### OCR Status Polling

**Strategy:** Exponential backoff to reduce server load

```typescript
pollOcrStatus(attachmentId: string): Observable<OcrStatus> {
  let retryCount = 0;
  return interval(0).pipe(
    delay(() => timer(Math.min(2000 * Math.pow(1.5, retryCount++), 10000))),
    switchMap(() => this.getOcrStatus(attachmentId)),
    map(response => response.ocrStatus),
    distinctUntilChanged(),
    takeWhile(status => status === 'PENDING' || status === 'PROCESSING', true)
  );
}
```

**Rationale:**
- Start with 2-second interval
- Increase to 3s, 4.5s, 6.75s, 10s (max)
- Reduces backend load for long-running OCR jobs

---

### Search Debouncing

**Strategy:** 500ms debounce to avoid excessive API calls

```typescript
searchQuery$ = new Subject<string>();

ngOnInit(): void {
  this.searchQuery$.pipe(
    debounceTime(500),
    distinctUntilChanged(),
    switchMap(query => this.searchService.searchDocuments(query))
  ).subscribe(results => {
    this.searchResults = results.content;
  });
}
```

---

### File Upload Progress

**Strategy:** Track upload progress for user feedback

```typescript
uploadDocument(file: File): Observable<HttpEvent<AttachmentUploadResponse>> {
  const formData = new FormData();
  formData.append('file', file);

  return this.http.post<AttachmentUploadResponse>(
    `/api/documents/clinical/${this.documentId}/upload`,
    formData,
    {
      reportProgress: true,
      observe: 'events',
      headers: { 'X-Tenant-ID': this.tenantId }
    }
  );
}

// Component
uploadFile(file: File): void {
  this.uploadService.uploadDocument(file).subscribe({
    next: (event: HttpEvent<any>) => {
      if (event.type === HttpEventType.UploadProgress) {
        this.uploadProgress = Math.round((event.loaded / event.total!) * 100);
      } else if (event instanceof HttpResponse) {
        this.onUploadSuccess(event.body);
      }
    },
    error: (err) => this.onUploadError(err)
  });
}
```

---

## Security Considerations

### File Type Validation

**Client-side:** Validate MIME type before upload
**Server-side:** Re-validate MIME type and file content (defense in depth)

```typescript
// Client-side validation
validateFileType(file: File): boolean {
  const allowedTypes = [
    'application/pdf',
    'image/png',
    'image/jpeg',
    'image/jpg',
    'image/tiff'
  ];
  return allowedTypes.includes(file.type);
}

// Server-side: Already implemented in ClinicalDocumentService
// - MIME type verification
// - Magic number validation
// - File extension check
```

---

### XSS Prevention

**Issue:** Displaying OCR text in HTML can introduce XSS vulnerabilities

**Mitigation:**
```typescript
// Use Angular's DomSanitizer for OCR text display
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

highlightSearchTerms(text: string, query: string): SafeHtml {
  const escaped = this.escapeHtml(text);
  const terms = query.split(' ').filter(t => t.length > 2);
  let highlighted = escaped;

  terms.forEach(term => {
    const escapedTerm = this.escapeHtml(term);
    const regex = new RegExp(`(${escapedTerm})`, 'gi');
    highlighted = highlighted.replace(regex, '<mark>$1</mark>');
  });

  return this.sanitizer.sanitize(SecurityContext.HTML, highlighted) || '';
}

private escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
```

---

## Future Enhancements

### Phase 5 (Future - Not in Scope)

1. **Real-time OCR Progress Updates (WebSocket)**
   - Replace polling with WebSocket for live OCR progress
   - Display page-by-page extraction for multi-page PDFs

2. **Advanced Search Features**
   - Fuzzy matching for OCR text with typos
   - Synonym expansion (e.g., "DM" → "Diabetes Mellitus")
   - Date range filtering
   - Document type filtering

3. **Document Classification**
   - ML-based automatic document type detection (Lab Report, Referral, etc.)
   - Auto-tagging based on OCR content

4. **Batch Upload**
   - Upload multiple files simultaneously
   - Bulk OCR processing queue

5. **OCR Quality Scoring**
   - Confidence score per page
   - Auto-retry low-quality OCR

6. **Export Features**
   - Export search results to CSV
   - Export OCR text to structured format (FHIR DocumentReference)

---

## Conclusion

This specification provides a comprehensive blueprint for integrating the completed OCR backend infrastructure (Issue #245) with the Clinical Portal frontend. The implementation is divided into 4 phases, with Phases 1-3 being essential for a production-ready OCR workflow.

**Recommended Approach:**
1. **Start with Phase 1** (Core Upload Functionality) - 8-12 hours
2. **Follow with Phase 2** (Search Functionality) - 6-8 hours
3. **Complete Phase 3** (Patient Detail Integration) - 4-6 hours
4. **Optional: Phase 4** (Document Viewer) - 6-8 hours

**Total Estimated Effort:** 18-26 hours (core), 24-34 hours (with document viewer)

**Status:** ✅ **READY FOR IMPLEMENTATION**

---

_Document Created:_ January 24, 2026
_Author:_ Claude Code
_Task:_ #9 - Integrate OCR with clinical workflows
_Depends On:_ Issue #245 (OCR Backend) - ✅ COMPLETE
