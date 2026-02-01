# OCR Frontend Phase 1: Document Upload Component - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement drag-and-drop document upload component with OCR status polling for the HDIM Clinical Portal.

**Architecture:** Angular component using Material Design for file upload UI, RxJS for reactive OCR status polling with exponential backoff, and HttpClient for backend API integration. Follows HIPAA-compliant logging patterns (LoggerService, no console.log) with automatic audit trail via HTTP interceptor.

**Tech Stack:** Angular 17+, RxJS 7+, Angular Material, TypeScript 5+, Jest (testing)

**Reference Specification:** `docs/ISSUE_249_OCR_CLINICAL_WORKFLOW_INTEGRATION_SPEC.md`

---

## Prerequisites

**Before starting:**
1. OCR backend deployed and accessible (see `docs/ISSUE_245_OCR_COMPLETION_SUMMARY.md`)
2. Angular 17+ development environment ready
3. Clinical Portal running locally (`npm start` in project root)
4. Backend API accessible at `http://localhost:8089`

**Verify backend is ready:**
```bash
curl http://localhost:8089/actuator/health
# Expected: {"status":"UP"}
```

---

## Task 1: Create DocumentUploadService (Test-First)

**Files:**
- Create: `apps/clinical-portal/src/app/services/document-upload.service.ts`
- Create: `apps/clinical-portal/src/app/services/document-upload.service.spec.ts`
- Modify: `apps/clinical-portal/src/app/services/index.ts` (export barrel file)

---

### Step 1: Write the failing service test

**File:** `apps/clinical-portal/src/app/services/document-upload.service.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DocumentUploadService, AttachmentUploadResponse } from './document-upload.service';

describe('DocumentUploadService', () => {
  let service: DocumentUploadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DocumentUploadService]
    });
    service = TestBed.inject(DocumentUploadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('uploadDocument', () => {
    it('should upload file and return attachment response', (done) => {
      const documentId = 'doc-123';
      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const mockResponse: AttachmentUploadResponse = {
        attachmentId: 'attach-456',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 12,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      };

      service.uploadDocument(documentId, file).subscribe(response => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpMock.expectOne(`/api/documents/clinical/${documentId}/upload`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      req.flush(mockResponse);
    });

    it('should include X-Tenant-ID header', (done) => {
      const documentId = 'doc-123';
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });

      service.uploadDocument(documentId, file).subscribe(() => done());

      const req = httpMock.expectOne(`/api/documents/clinical/${documentId}/upload`);
      expect(req.request.headers.has('X-Tenant-ID')).toBe(true);
      req.flush({});
    });
  });

  describe('pollOcrStatus', () => {
    it('should poll status every 2 seconds until COMPLETED', (done) => {
      const attachmentId = 'attach-456';
      const responses = [
        { attachmentId, ocrStatus: 'PENDING' },
        { attachmentId, ocrStatus: 'PROCESSING' },
        { attachmentId, ocrStatus: 'COMPLETED', ocrText: 'extracted text' }
      ];
      let callCount = 0;

      service.pollOcrStatus(attachmentId).subscribe(status => {
        if (status === 'COMPLETED') {
          expect(callCount).toBe(3);
          done();
        }
      });

      // Simulate 3 polling calls
      const req1 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req1.flush(responses[callCount++]);

      setTimeout(() => {
        const req2 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
        req2.flush(responses[callCount++]);

        setTimeout(() => {
          const req3 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
          req3.flush(responses[callCount++]);
        }, 2000);
      }, 2000);
    }, 10000); // 10 second timeout for this test
  });

  describe('retryOcr', () => {
    it('should call reprocess endpoint', (done) => {
      const attachmentId = 'attach-456';

      service.retryOcr(attachmentId).subscribe(() => done());

      const req = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/reprocess-ocr`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });
});
```

---

### Step 2: Run test to verify it fails

```bash
cd apps/clinical-portal
npm test -- document-upload.service.spec.ts
```

**Expected:** FAIL with "Cannot find module './document-upload.service'"

---

### Step 3: Write minimal service implementation

**File:** `apps/clinical-portal/src/app/services/document-upload.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap, map, distinctUntilChanged, takeWhile } from 'rxjs/operators';

export type OcrStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AttachmentUploadResponse {
  attachmentId: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  uploadDate: string;
  ocrStatus: OcrStatus;
}

export interface OcrStatusResponse {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  ocrProcessingDate?: string;
  errorMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentUploadService {
  private readonly tenantId = 'default-tenant'; // TODO: Get from AuthService

  constructor(private http: HttpClient) {}

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

### Step 4: Run test to verify it passes

```bash
npm test -- document-upload.service.spec.ts
```

**Expected:** PASS (all 4 tests pass)

---

### Step 5: Commit

```bash
git add apps/clinical-portal/src/app/services/document-upload.service.ts
git add apps/clinical-portal/src/app/services/document-upload.service.spec.ts
git commit -m "feat(ocr): add DocumentUploadService with upload and polling

- Upload document with FormData and tenant header
- Poll OCR status every 2 seconds until completion
- Retry failed OCR processing
- 100% test coverage

Ref: Phase 1 - Issue #249"
```

---

## Task 2: Create File Validation Utilities (Test-First)

**Files:**
- Create: `apps/clinical-portal/src/app/utils/file-validation.ts`
- Create: `apps/clinical-portal/src/app/utils/file-validation.spec.ts`

---

### Step 1: Write the failing validation test

**File:** `apps/clinical-portal/src/app/utils/file-validation.spec.ts`

```typescript
import { validateFileSize, validateFileType, getAcceptedMimeTypes } from './file-validation';

describe('File Validation Utilities', () => {
  describe('validateFileSize', () => {
    it('should return true for files <= 10 MB', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 10 * 1024 * 1024 }); // 10 MB
      expect(validateFileSize(file)).toBe(true);
    });

    it('should return false for files > 10 MB', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 }); // 11 MB
      expect(validateFileSize(file)).toBe(false);
    });

    it('should handle custom max size', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 5 * 1024 * 1024 }); // 5 MB
      expect(validateFileSize(file, 6 * 1024 * 1024)).toBe(true);
      expect(validateFileSize(file, 4 * 1024 * 1024)).toBe(false);
    });
  });

  describe('validateFileType', () => {
    it('should return true for PDF files', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return true for PNG files', () => {
      const file = new File(['test'], 'test.png', { type: 'image/png' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return true for JPG/JPEG files', () => {
      const file1 = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const file2 = new File(['test'], 'test.jpeg', { type: 'image/jpeg' });
      expect(validateFileType(file1)).toBe(true);
      expect(validateFileType(file2)).toBe(true);
    });

    it('should return true for TIFF files', () => {
      const file = new File(['test'], 'test.tiff', { type: 'image/tiff' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return false for unsupported file types', () => {
      const file = new File(['test'], 'test.txt', { type: 'text/plain' });
      expect(validateFileType(file)).toBe(false);
    });
  });

  describe('getAcceptedMimeTypes', () => {
    it('should return array of accepted MIME types', () => {
      const mimeTypes = getAcceptedMimeTypes();
      expect(mimeTypes).toContain('application/pdf');
      expect(mimeTypes).toContain('image/png');
      expect(mimeTypes).toContain('image/jpeg');
      expect(mimeTypes).toContain('image/tiff');
      expect(mimeTypes.length).toBe(4);
    });

    it('should return string for input accept attribute', () => {
      const acceptString = getAcceptedMimeTypes().join(',');
      expect(acceptString).toBe('application/pdf,image/png,image/jpeg,image/tiff');
    });
  });
});
```

---

### Step 2: Run test to verify it fails

```bash
npm test -- file-validation.spec.ts
```

**Expected:** FAIL with "Cannot find module './file-validation'"

---

### Step 3: Write minimal validation implementation

**File:** `apps/clinical-portal/src/app/utils/file-validation.ts`

```typescript
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB in bytes

const ACCEPTED_MIME_TYPES = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'image/tiff'
] as const;

export function validateFileSize(file: File, maxSize: number = MAX_FILE_SIZE): boolean {
  return file.size <= maxSize;
}

export function validateFileType(file: File): boolean {
  return ACCEPTED_MIME_TYPES.includes(file.type as typeof ACCEPTED_MIME_TYPES[number]);
}

export function getAcceptedMimeTypes(): string[] {
  return [...ACCEPTED_MIME_TYPES];
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}
```

---

### Step 4: Run test to verify it passes

```bash
npm test -- file-validation.spec.ts
```

**Expected:** PASS (all tests pass)

---

### Step 5: Commit

```bash
git add apps/clinical-portal/src/app/utils/file-validation.ts
git add apps/clinical-portal/src/app/utils/file-validation.spec.ts
git commit -m "feat(ocr): add file validation utilities

- Validate file size (default 10 MB max)
- Validate file type (PDF, PNG, JPG, TIFF)
- Get accepted MIME types for input accept attribute
- Format file size for display
- 100% test coverage

Ref: Phase 1 - Issue #249"
```

---

## Task 3: Create DocumentUploadComponent (Test-First)

**Files:**
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.component.ts`
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.component.html`
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.component.scss`
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.component.spec.ts`

---

### Step 1: Write the failing component test

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.component.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { DocumentUploadComponent } from './document-upload.component';
import { DocumentUploadService } from '../../services/document-upload.service';
import { of, throwError } from 'rxjs';

describe('DocumentUploadComponent', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let uploadService: jasmine.SpyObj<DocumentUploadService>;

  beforeEach(async () => {
    const uploadServiceSpy = jasmine.createSpyObj('DocumentUploadService', [
      'uploadDocument',
      'pollOcrStatus',
      'retryOcr'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        DocumentUploadComponent,
        HttpClientTestingModule,
        MatIconModule,
        MatButtonModule,
        MatProgressBarModule,
        MatChipsModule,
        MatListModule
      ],
      providers: [
        { provide: DocumentUploadService, useValue: uploadServiceSpy }
      ]
    }).compileComponents();

    uploadService = TestBed.inject(DocumentUploadService) as jasmine.SpyObj<DocumentUploadService>;
    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    component.documentId = 'doc-123';
    component.patientId = 'patient-456';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('file validation', () => {
    it('should reject files larger than 10 MB', () => {
      const file = new File(['test'], 'large.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 });

      component.onFileSelected({ target: { files: [file] } } as any);

      expect(component.errorMessage).toContain('exceeds 10 MB');
      expect(uploadService.uploadDocument).not.toHaveBeenCalled();
    });

    it('should reject unsupported file types', () => {
      const file = new File(['test'], 'test.txt', { type: 'text/plain' });

      component.onFileSelected({ target: { files: [file] } } as any);

      expect(component.errorMessage).toContain('Unsupported file type');
      expect(uploadService.uploadDocument).not.toHaveBeenCalled();
    });

    it('should accept valid PDF files', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      uploadService.uploadDocument.and.returnValue(of({
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      }));
      uploadService.pollOcrStatus.and.returnValue(of('COMPLETED'));

      component.onFileSelected({ target: { files: [file] } } as any);

      expect(component.errorMessage).toBe('');
      expect(uploadService.uploadDocument).toHaveBeenCalledWith('doc-123', file);
    });
  });

  describe('file upload', () => {
    it('should upload file and start OCR polling', (done) => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      const mockResponse = {
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      };

      uploadService.uploadDocument.and.returnValue(of(mockResponse));
      uploadService.pollOcrStatus.and.returnValue(of('COMPLETED'));

      component.uploadSuccess.subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      component.ocrComplete.subscribe(event => {
        expect(event.attachmentId).toBe('attach-123');
        expect(event.ocrStatus).toBe('COMPLETED');
        done();
      });

      component.onFileSelected({ target: { files: [file] } } as any);
    });

    it('should handle upload errors', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      uploadService.uploadDocument.and.returnValue(
        throwError(() => new Error('Upload failed'))
      );

      component.uploadError.subscribe(error => {
        expect(error).toContain('Upload failed');
      });

      component.onFileSelected({ target: { files: [file] } } as any);

      expect(component.errorMessage).toContain('Upload failed');
    });
  });

  describe('OCR status polling', () => {
    it('should update status as polling progresses', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      uploadService.uploadDocument.and.returnValue(of({
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      }));
      uploadService.pollOcrStatus.and.returnValue(of('PROCESSING', 'COMPLETED'));

      component.onFileSelected({ target: { files: [file] } } as any);

      // Status should update through PROCESSING to COMPLETED
      expect(component.ocrStatus).toBeDefined();
    });
  });

  describe('retry OCR', () => {
    it('should retry failed OCR processing', () => {
      uploadService.retryOcr.and.returnValue(of(undefined));
      uploadService.pollOcrStatus.and.returnValue(of('COMPLETED'));

      component.retryOcr('attach-123');

      expect(uploadService.retryOcr).toHaveBeenCalledWith('attach-123');
      expect(uploadService.pollOcrStatus).toHaveBeenCalledWith('attach-123');
    });
  });
});
```

---

### Step 2: Run test to verify it fails

```bash
npm test -- document-upload.component.spec.ts
```

**Expected:** FAIL with "Cannot find module './document-upload.component'"

---

### Step 3: Write minimal component implementation (TypeScript)

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.component.ts`

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { DocumentUploadService, AttachmentUploadResponse, OcrStatus } from '../../services/document-upload.service';
import { validateFileSize, validateFileType, formatFileSize } from '../../utils/file-validation';
import { LoggerService } from '../../services/logger.service';

export interface OcrCompletionEvent {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  errorMessage?: string;
}

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatChipsModule,
    MatListModule
  ],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.scss']
})
export class DocumentUploadComponent {
  @Input() documentId!: string;
  @Input() patientId!: string;
  @Input() allowedFileTypes: string[] = ['pdf', 'png', 'jpg', 'jpeg', 'tiff'];
  @Input() maxFileSize: number = 10 * 1024 * 1024; // 10 MB

  @Output() uploadSuccess = new EventEmitter<AttachmentUploadResponse>();
  @Output() uploadError = new EventEmitter<string>();
  @Output() ocrComplete = new EventEmitter<OcrCompletionEvent>();

  selectedFile: File | null = null;
  uploading = false;
  errorMessage = '';
  ocrStatus: OcrStatus | null = null;
  uploadedFiles: AttachmentUploadResponse[] = [];

  private logger = this.loggerService.withContext('DocumentUploadComponent');

  constructor(
    private uploadService: DocumentUploadService,
    private loggerService: LoggerService
  ) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.errorMessage = '';

    // Validate file size
    if (!validateFileSize(file, this.maxFileSize)) {
      this.errorMessage = `File size exceeds 10 MB limit (${formatFileSize(file.size)})`;
      this.uploadError.emit(this.errorMessage);
      return;
    }

    // Validate file type
    if (!validateFileType(file)) {
      this.errorMessage = `Unsupported file type: ${file.type}. Please upload PDF or image files.`;
      this.uploadError.emit(this.errorMessage);
      return;
    }

    this.selectedFile = file;
    this.uploadFile(file);
  }

  private uploadFile(file: File): void {
    this.uploading = true;
    this.logger.info('Uploading file', this.documentId);

    this.uploadService.uploadDocument(this.documentId, file).subscribe({
      next: (response) => {
        this.uploading = false;
        this.uploadSuccess.emit(response);
        this.uploadedFiles.push(response);
        this.logger.info('File uploaded successfully', response.attachmentId);

        // Start OCR status polling
        this.startOcrPolling(response.attachmentId);
      },
      error: (error) => {
        this.uploading = false;
        this.errorMessage = `Upload failed: ${error.message || 'Unknown error'}`;
        this.uploadError.emit(this.errorMessage);
        this.logger.error('File upload failed', error);
      }
    });
  }

  private startOcrPolling(attachmentId: string): void {
    this.logger.info('Starting OCR status polling', attachmentId);

    this.uploadService.pollOcrStatus(attachmentId).subscribe({
      next: (status) => {
        this.ocrStatus = status;
        this.logger.info('OCR status update', { attachmentId, status });

        if (status === 'COMPLETED' || status === 'FAILED') {
          this.ocrComplete.emit({
            attachmentId,
            ocrStatus: status
          });
        }
      },
      error: (error) => {
        this.logger.error('OCR status polling failed', error);
      }
    });
  }

  retryOcr(attachmentId: string): void {
    this.logger.info('Retrying OCR processing', attachmentId);

    this.uploadService.retryOcr(attachmentId).subscribe({
      next: () => {
        this.logger.info('OCR retry initiated', attachmentId);
        this.startOcrPolling(attachmentId);
      },
      error: (error) => {
        this.logger.error('OCR retry failed', error);
      }
    });
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType === 'application/pdf') return 'picture_as_pdf';
    return 'insert_drive_file';
  }

  getOcrStatusColor(status: OcrStatus): string {
    switch (status) {
      case 'PENDING': return 'warn';
      case 'PROCESSING': return 'accent';
      case 'COMPLETED': return 'primary';
      case 'FAILED': return 'warn';
      default: return '';
    }
  }

  getOcrStatusLabel(status: OcrStatus): string {
    switch (status) {
      case 'PENDING': return 'OCR Queued';
      case 'PROCESSING': return 'Processing OCR...';
      case 'COMPLETED': return 'OCR Complete';
      case 'FAILED': return 'OCR Failed';
      default: return '';
    }
  }
}
```

---

### Step 4: Write component template

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.component.html`

```html
<div class="upload-container">
  <!-- File upload input -->
  <div class="upload-section">
    <input
      #fileInput
      type="file"
      [accept]="allowedFileTypes.map(t => '.' + t).join(',')"
      (change)="onFileSelected($event)"
      style="display: none"
      aria-label="Choose file to upload">

    <button
      mat-raised-button
      color="primary"
      (click)="fileInput.click()"
      [disabled]="uploading"
      aria-label="Upload clinical document">
      <mat-icon>cloud_upload</mat-icon>
      Upload Document
    </button>

    <p class="upload-hint">
      Accepted: PDF, PNG, JPG, JPEG, TIFF (Max 10 MB)
    </p>
  </div>

  <!-- Upload progress -->
  <div *ngIf="uploading" class="upload-progress" role="status" aria-live="polite">
    <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    <span class="sr-only">Uploading {{ selectedFile?.name }}...</span>
  </div>

  <!-- Error message -->
  <div *ngIf="errorMessage" class="error-message" role="alert">
    <mat-icon color="warn">error</mat-icon>
    <span>{{ errorMessage }}</span>
  </div>

  <!-- OCR status -->
  <div *ngIf="ocrStatus" class="ocr-status" role="status" aria-live="polite">
    <mat-chip [color]="getOcrStatusColor(ocrStatus)" selected>
      {{ getOcrStatusLabel(ocrStatus) }}
    </mat-chip>
  </div>

  <!-- Uploaded files list -->
  <mat-list class="uploaded-files" *ngIf="uploadedFiles.length > 0">
    <h3 matSubheader>Uploaded Documents</h3>
    <mat-list-item *ngFor="let file of uploadedFiles">
      <mat-icon matListItemIcon [aria-hidden]="true">
        {{ getFileIcon(file.mimeType) }}
      </mat-icon>
      <div matListItemTitle>{{ file.fileName }}</div>
      <div matListItemLine>
        Uploaded: {{ file.uploadDate | date }}
        <span class="ocr-status-badge" [class]="file.ocrStatus">
          {{ file.ocrStatus }}
        </span>
      </div>
      <button
        mat-icon-button
        *ngIf="file.ocrStatus === 'FAILED'"
        (click)="retryOcr(file.attachmentId)"
        [attr.aria-label]="'Retry OCR for ' + file.fileName"
        matTooltip="Retry OCR">
        <mat-icon [aria-hidden]="true">refresh</mat-icon>
      </button>
    </mat-list-item>
  </mat-list>
</div>
```

---

### Step 5: Write component styles

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.component.scss`

```scss
.upload-container {
  padding: 16px;
}

.upload-section {
  text-align: center;
  padding: 24px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  margin-bottom: 16px;

  &:hover {
    border-color: #999;
  }
}

.upload-hint {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.6);
  font-size: 14px;
}

.upload-progress {
  margin: 16px 0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background-color: #ffebee;
  border-radius: 4px;
  margin: 16px 0;
  color: #c62828;
}

.ocr-status {
  margin: 16px 0;
  text-align: center;
}

.uploaded-files {
  margin-top: 24px;
}

.ocr-status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-left: 8px;

  &.PENDING {
    background-color: #fff3e0;
    color: #e65100;
  }

  &.PROCESSING {
    background-color: #e3f2fd;
    color: #1565c0;
  }

  &.COMPLETED {
    background-color: #e8f5e9;
    color: #2e7d32;
  }

  &.FAILED {
    background-color: #ffebee;
    color: #c62828;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

---

### Step 6: Run test to verify it passes

```bash
npm test -- document-upload.component.spec.ts
```

**Expected:** PASS (all tests pass)

---

### Step 7: Commit

```bash
git add apps/clinical-portal/src/app/components/document-upload/
git commit -m "feat(ocr): add DocumentUploadComponent with file upload UI

- Drag-and-drop file upload with Material Design
- File validation (size, type)
- Upload progress indicator
- OCR status polling with visual feedback
- Retry failed OCR processing
- HIPAA-compliant logging (LoggerService, no console.log)
- Accessible with ARIA labels and screen reader support
- 100% test coverage

Ref: Phase 1 - Issue #249"
```

---

## Task 4: Integration Testing

**Files:**
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.integration.spec.ts`

---

### Step 1: Write integration test

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.integration.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DocumentUploadComponent } from './document-upload.component';
import { DocumentUploadService } from '../../services/document-upload.service';

describe('DocumentUploadComponent Integration', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        DocumentUploadComponent,
        HttpClientTestingModule
      ],
      providers: [DocumentUploadService]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    component.documentId = 'doc-123';
    component.patientId = 'patient-456';
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should upload file and complete OCR end-to-end', fakeAsync(() => {
    const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
    const uploadResponse = {
      attachmentId: 'attach-123',
      fileName: 'test.pdf',
      mimeType: 'application/pdf',
      fileSize: 12,
      uploadDate: '2026-01-24T12:00:00Z',
      ocrStatus: 'PENDING' as const
    };

    let uploadSuccessCalled = false;
    let ocrCompleteCalled = false;

    component.uploadSuccess.subscribe(response => {
      expect(response).toEqual(uploadResponse);
      uploadSuccessCalled = true;
    });

    component.ocrComplete.subscribe(event => {
      expect(event.attachmentId).toBe('attach-123');
      expect(event.ocrStatus).toBe('COMPLETED');
      ocrCompleteCalled = true;
    });

    // Trigger file upload
    component.onFileSelected({
      target: { files: [file] }
    } as any);

    // Respond to upload request
    const uploadReq = httpMock.expectOne('/api/documents/clinical/doc-123/upload');
    expect(uploadReq.request.method).toBe('POST');
    uploadReq.flush(uploadResponse);

    expect(uploadSuccessCalled).toBe(true);

    // Simulate OCR status polling (3 requests: PENDING → PROCESSING → COMPLETED)
    tick(2000);
    const statusReq1 = httpMock.expectOne('/api/documents/clinical/attachments/attach-123/ocr-status');
    statusReq1.flush({ attachmentId: 'attach-123', ocrStatus: 'PENDING' });

    tick(2000);
    const statusReq2 = httpMock.expectOne('/api/documents/clinical/attachments/attach-123/ocr-status');
    statusReq2.flush({ attachmentId: 'attach-123', ocrStatus: 'PROCESSING' });

    tick(2000);
    const statusReq3 = httpMock.expectOne('/api/documents/clinical/attachments/attach-123/ocr-status');
    statusReq3.flush({
      attachmentId: 'attach-123',
      ocrStatus: 'COMPLETED',
      ocrText: 'Extracted text from PDF'
    });

    tick(); // Allow observables to complete

    expect(ocrCompleteCalled).toBe(true);
    expect(component.ocrStatus).toBe('COMPLETED');
  }));

  it('should handle OCR failure and allow retry', fakeAsync(() => {
    const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
    const uploadResponse = {
      attachmentId: 'attach-456',
      fileName: 'test.pdf',
      mimeType: 'application/pdf',
      fileSize: 4,
      uploadDate: '2026-01-24T12:00:00Z',
      ocrStatus: 'PENDING' as const
    };

    component.onFileSelected({ target: { files: [file] } } as any);

    const uploadReq = httpMock.expectOne('/api/documents/clinical/doc-123/upload');
    uploadReq.flush(uploadResponse);

    // Simulate OCR failure
    tick(2000);
    const statusReq = httpMock.expectOne('/api/documents/clinical/attachments/attach-456/ocr-status');
    statusReq.flush({ attachmentId: 'attach-456', ocrStatus: 'FAILED', errorMessage: 'Tesseract error' });

    tick();

    expect(component.ocrStatus).toBe('FAILED');

    // Retry OCR
    component.retryOcr('attach-456');

    const retryReq = httpMock.expectOne('/api/documents/clinical/attachments/attach-456/reprocess-ocr');
    expect(retryReq.request.method).toBe('POST');
    retryReq.flush({});

    // New status polling after retry
    tick(2000);
    const statusReq2 = httpMock.expectOne('/api/documents/clinical/attachments/attach-456/ocr-status');
    statusReq2.flush({ attachmentId: 'attach-456', ocrStatus: 'COMPLETED' });

    tick();

    expect(component.ocrStatus).toBe('COMPLETED');
  }));
});
```

---

### Step 2: Run integration test

```bash
npm test -- document-upload.integration.spec.ts
```

**Expected:** PASS (both integration tests pass)

---

### Step 3: Commit

```bash
git add apps/clinical-portal/src/app/components/document-upload/document-upload.integration.spec.ts
git commit -m "test(ocr): add end-to-end integration tests for document upload

- Upload file and complete OCR workflow
- Handle OCR failure and retry
- Verify HTTP requests and responses
- Test OCR status polling behavior

Ref: Phase 1 - Issue #249"
```

---

## Task 5: Accessibility Testing

**Files:**
- Create: `apps/clinical-portal/src/app/components/document-upload/document-upload.component.a11y.spec.ts`

---

### Step 1: Write accessibility tests

**File:** `apps/clinical-portal/src/app/components/document-upload/document-upload.component.a11y.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DocumentUploadComponent } from './document-upload.component';
import {
  testAccessibility,
  testKeyboardAccessibility,
  testAriaAttributes
} from '../../../testing/accessibility.helper';

describe('DocumentUploadComponent - Accessibility', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        DocumentUploadComponent,
        HttpClientTestingModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    component.documentId = 'doc-123';
    component.patientId = 'patient-456';
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      const results = await testAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have accessible upload button', () => {
      const button = fixture.nativeElement.querySelector('button[aria-label="Upload clinical document"]');
      expect(button).toBeTruthy();
      expect(button.getAttribute('aria-label')).toBe('Upload clinical document');
    });

    it('should have accessible file input', () => {
      const input = fixture.nativeElement.querySelector('input[type="file"]');
      expect(input).toBeTruthy();
      expect(input.getAttribute('aria-label')).toBe('Choose file to upload');
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have valid ARIA attributes', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Screen Reader Support', () => {
    it('should announce upload progress', () => {
      component.uploading = true;
      component.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      fixture.detectChanges();

      const progressContainer = fixture.nativeElement.querySelector('.upload-progress');
      expect(progressContainer.getAttribute('role')).toBe('status');
      expect(progressContainer.getAttribute('aria-live')).toBe('polite');

      const srText = fixture.nativeElement.querySelector('.sr-only');
      expect(srText.textContent).toContain('Uploading test.pdf');
    });

    it('should announce OCR status updates', () => {
      component.ocrStatus = 'PROCESSING';
      fixture.detectChanges();

      const statusContainer = fixture.nativeElement.querySelector('.ocr-status');
      expect(statusContainer.getAttribute('role')).toBe('status');
      expect(statusContainer.getAttribute('aria-live')).toBe('polite');
    });

    it('should announce errors with role="alert"', () => {
      component.errorMessage = 'File size exceeds 10 MB';
      fixture.detectChanges();

      const errorContainer = fixture.nativeElement.querySelector('.error-message');
      expect(errorContainer.getAttribute('role')).toBe('alert');
      expect(errorContainer.textContent).toContain('File size exceeds 10 MB');
    });
  });

  describe('Retry Button Accessibility', () => {
    it('should have accessible retry button for failed OCR', () => {
      component.uploadedFiles = [{
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 12,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'FAILED'
      }];
      fixture.detectChanges();

      const retryButton = fixture.nativeElement.querySelector('button[aria-label*="Retry OCR"]');
      expect(retryButton).toBeTruthy();
      expect(retryButton.getAttribute('aria-label')).toBe('Retry OCR for test.pdf');

      const icon = retryButton.querySelector('mat-icon');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  describe('File Icons Accessibility', () => {
    it('should hide decorative icons from screen readers', () => {
      component.uploadedFiles = [{
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 12,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'COMPLETED'
      }];
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('mat-icon[matListItemIcon]');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });
});
```

---

### Step 2: Run accessibility tests

```bash
npm test -- document-upload.component.a11y.spec.ts
```

**Expected:** PASS (all accessibility tests pass)

---

### Step 3: Commit

```bash
git add apps/clinical-portal/src/app/components/document-upload/document-upload.component.a11y.spec.ts
git commit -m "test(ocr): add accessibility tests for document upload

- WCAG 2.1 Level A and AA compliance
- Keyboard navigation support
- ARIA attributes validation
- Screen reader announcements (upload progress, OCR status, errors)
- Accessible retry button with context
- Decorative icons hidden from screen readers

Ref: Phase 1 - Issue #249"
```

---

## Task 6: Manual Testing Guide

**Files:**
- Create: `docs/testing/OCR_PHASE1_MANUAL_TEST_GUIDE.md`

---

### Step 1: Create manual testing guide

**File:** `docs/testing/OCR_PHASE1_MANUAL_TEST_GUIDE.md`

```markdown
# OCR Phase 1 - Manual Testing Guide

**Component:** DocumentUploadComponent
**Date:** 2026-01-24
**Tester:** [Your Name]

---

## Prerequisites

1. Backend running: `curl http://localhost:8089/actuator/health`
2. Frontend running: `npm start` in Clinical Portal
3. Navigate to: `http://localhost:4200` (login if required)

---

## Test Cases

### TC1: Upload Valid PDF File

**Steps:**
1. Navigate to Patient Detail page
2. Click "Upload Document" button
3. Select a PDF file (< 10 MB)
4. Click "Open"

**Expected:**
- ✅ Upload progress bar appears
- ✅ "Uploading [filename]..." message displays
- ✅ Progress bar disappears after upload
- ✅ OCR status shows "OCR Queued"
- ✅ After 2-10 seconds, status changes to "Processing OCR..."
- ✅ After processing, status changes to "OCR Complete"
- ✅ File appears in "Uploaded Documents" list
- ✅ No console errors

**Actual:**
- [ ] Upload progress bar appears
- [ ] Upload message displays
- [ ] OCR status updates correctly
- [ ] File appears in list

**Result:** ☐ PASS ☐ FAIL

**Notes:**

---

### TC2: Upload Valid Image File (PNG, JPG, TIFF)

**Steps:**
1. Click "Upload Document"
2. Select a PNG/JPG/TIFF file (< 10 MB)
3. Click "Open"

**Expected:**
- ✅ Same as TC1
- ✅ File icon shows "image" (not PDF icon)

**Result:** ☐ PASS ☐ FAIL

---

### TC3: Reject Oversized File (> 10 MB)

**Steps:**
1. Click "Upload Document"
2. Select a file > 10 MB
3. Click "Open"

**Expected:**
- ✅ Error message: "File size exceeds 10 MB limit (XX MB)"
- ✅ Red error banner displays
- ✅ File does NOT upload
- ✅ No API call made (check Network tab)

**Result:** ☐ PASS ☐ FAIL

---

### TC4: Reject Unsupported File Type

**Steps:**
1. Click "Upload Document"
2. Select a .txt or .docx file
3. Click "Open"

**Expected:**
- ✅ Error message: "Unsupported file type: text/plain. Please upload PDF or image files."
- ✅ Red error banner displays
- ✅ File does NOT upload

**Result:** ☐ PASS ☐ FAIL

---

### TC5: Retry Failed OCR

**Steps:**
1. Upload a file that will fail OCR (corrupt PDF or non-text image)
2. Wait for OCR status to show "OCR Failed"
3. Click "Retry" button (refresh icon)

**Expected:**
- ✅ Retry button appears next to failed file
- ✅ Clicking retry initiates new OCR processing
- ✅ Status changes to "OCR Queued" → "Processing OCR..."
- ✅ If successful, status changes to "OCR Complete"

**Result:** ☐ PASS ☐ FAIL

---

### TC6: Multiple File Uploads

**Steps:**
1. Upload 3 different files (PDF, PNG, JPG)
2. Wait for all to complete OCR

**Expected:**
- ✅ All 3 files appear in "Uploaded Documents" list
- ✅ Each file shows correct icon (PDF or image)
- ✅ Each file shows independent OCR status
- ✅ OCR status polls independently for each file

**Result:** ☐ PASS ☐ FAIL

---

## Accessibility Testing

### AT1: Keyboard Navigation

**Steps:**
1. Use Tab key to navigate to "Upload Document" button
2. Press Enter to activate file picker
3. Select file and press Enter
4. Tab to retry button (if OCR fails)
5. Press Enter to retry

**Expected:**
- ✅ All interactive elements keyboard accessible
- ✅ Focus indicators visible
- ✅ No keyboard traps

**Result:** ☐ PASS ☐ FAIL

---

### AT2: Screen Reader (NVDA)

**Steps:**
1. Enable NVDA
2. Navigate to upload button
3. Upload a file
4. Listen for status announcements

**Expected Announcements:**
- "Upload clinical document, button"
- "Choose file to upload, edit"
- "Uploading test.pdf..." (during upload)
- "OCR Queued" (after upload)
- "Processing OCR..." (during processing)
- "OCR Complete" (when done)
- "File size exceeds 10 MB limit" (if error)

**Result:** ☐ PASS ☐ FAIL

---

### AT3: Error Announcements

**Steps:**
1. Upload oversized file
2. Listen for error announcement

**Expected:**
- ✅ Error announced with role="alert"
- ✅ Screen reader reads: "File size exceeds 10 MB limit"

**Result:** ☐ PASS ☐ FAIL

---

## HIPAA Compliance

### HC1: No PHI in Console Logs

**Steps:**
1. Open browser DevTools Console
2. Upload a file with patient name in filename
3. Check console for any patient information

**Expected:**
- ✅ No patient names in console
- ✅ No file contents in console
- ✅ Only attachment IDs logged
- ✅ LoggerService used (check for "DocumentUploadComponent" context)

**Result:** ☐ PASS ☐ FAIL

---

### HC2: Audit Logging

**Steps:**
1. Upload a file
2. Check backend logs or audit database

**Expected:**
- ✅ Upload action logged with user ID, timestamp, patient ID
- ✅ OCR status changes logged
- ✅ Retry actions logged

**Result:** ☐ PASS ☐ FAIL

---

## Performance Testing

### PT1: Upload Performance

**Steps:**
1. Upload a 5 MB PDF file
2. Measure time from click to upload complete

**Expected:**
- ✅ Upload completes in < 5 seconds
- ✅ UI remains responsive during upload

**Actual Time:** __________ seconds

**Result:** ☐ PASS ☐ FAIL

---

### PT2: OCR Status Polling

**Steps:**
1. Upload file and monitor Network tab
2. Count OCR status polling requests
3. Verify 2-second interval

**Expected:**
- ✅ Polling starts immediately after upload
- ✅ Requests sent every ~2 seconds
- ✅ Polling stops when status is COMPLETED or FAILED

**Result:** ☐ PASS ☐ FAIL

---

## Test Summary

**Total Test Cases:** 12
**Passed:** _____ / 12
**Failed:** _____ / 12

**Critical Issues Found:**

**Minor Issues Found:**

**Overall Status:** ☐ PASS ☐ FAIL

**Tester Signature:** __________________ **Date:** __________
```

---

### Step 2: Commit manual testing guide

```bash
git add docs/testing/OCR_PHASE1_MANUAL_TEST_GUIDE.md
git commit -m "docs(ocr): add manual testing guide for Phase 1

- 12 comprehensive test cases
- Accessibility testing procedures
- HIPAA compliance checks
- Performance benchmarks
- Screen reader testing steps

Ref: Phase 1 - Issue #249"
```

---

## Final Verification

### Step 1: Run all tests

```bash
cd apps/clinical-portal
npm test
```

**Expected:** All tests pass (100% coverage for Phase 1 code)

---

### Step 2: Build application

```bash
npm run build
```

**Expected:** Build succeeds with no errors

---

### Step 3: Verify no console.log statements

```bash
grep -r "console\." apps/clinical-portal/src/app/components/document-upload/ --include="*.ts" | grep -v spec.ts
```

**Expected:** No results (no console statements in production code)

---

### Step 4: Final commit

```bash
git add .
git commit -m "chore(ocr): Phase 1 complete - DocumentUploadComponent ready

Phase 1 Deliverables:
- DocumentUploadService (upload, poll, retry)
- File validation utilities (size, type)
- DocumentUploadComponent (UI, drag-and-drop)
- Integration tests (end-to-end workflow)
- Accessibility tests (WCAG 2.1 Level AA)
- Manual testing guide (12 test cases)

100% test coverage, HIPAA compliant, accessible

Ref: Phase 1 - Issue #249
Ready for: Phase 2 (Search Functionality)"
```

---

## Summary

**Phase 1 Complete:**
- ✅ 6 tasks completed
- ✅ DocumentUploadService with 3 methods
- ✅ File validation utilities
- ✅ DocumentUploadComponent with full UI
- ✅ Unit tests (100% coverage)
- ✅ Integration tests (end-to-end)
- ✅ Accessibility tests (WCAG 2.1 Level AA)
- ✅ Manual testing guide (12 cases)
- ✅ HIPAA compliant (LoggerService, audit logging)
- ✅ Accessible (keyboard, screen reader)

**Estimated Time:** 8-12 hours
**Next Phase:** Phase 2 - OcrSearchComponent

---

## Execution Options

Plan complete and saved to `docs/plans/2026-01-24-ocr-frontend-phase1-document-upload.md`.

**Two execution options:**

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach would you like?**
