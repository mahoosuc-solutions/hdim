import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DocumentUploadComponent, OcrCompletionEvent } from './document-upload.component';
import { DocumentUploadService, AttachmentUploadResponse, OcrStatusResponse, OcrStatus } from '../../services/document-upload.service';
import { AuthService } from '../../services/auth.service';
import { LoggerService } from '../../services/logger.service';

describe('DocumentUploadComponent - Integration Tests', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let httpMock: HttpTestingController;
  let authService: jest.Mocked<AuthService>;

  const mockDocumentId = 'doc-123';
  const mockPatientId = 'patient-456';
  const mockTenantId = 'tenant-789';
  const mockAttachmentId = 'attachment-abc';

  beforeEach(async () => {
    const authServiceMock = {
      getTenantId: jest.fn().mockReturnValue(mockTenantId)
    } as any;

    const loggerServiceMock = {
      withContext: jest.fn().mockReturnValue({
        info: jest.fn(),
        error: jest.fn(),
        debug: jest.fn(),
        warn: jest.fn()
      })
    } as any;

    await TestBed.configureTestingModule({
      imports: [
        DocumentUploadComponent
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DocumentUploadService,
        { provide: AuthService, useValue: authServiceMock },
        { provide: LoggerService, useValue: loggerServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;

    component.documentId = mockDocumentId;
    component.patientId = mockPatientId;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('End-to-End OCR Workflow', () => {
    it('should upload file and complete OCR end-to-end', fakeAsync(() => {
      // Arrange
      const mockFile = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const mockUploadResponse: AttachmentUploadResponse = {
        attachmentId: mockAttachmentId,
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 1024,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      };

      let uploadEmitted = false;
      let ocrCompleteEmitted = false;
      let emittedOcrEvent: OcrCompletionEvent | null = null;

      component.uploadSuccess.subscribe(() => {
        uploadEmitted = true;
      });

      component.ocrComplete.subscribe((event) => {
        ocrCompleteEmitted = true;
        emittedOcrEvent = event;
      });

      // Act - Upload file
      const event = {
        target: {
          files: [mockFile]
        }
      } as unknown as Event;
      component.onFileSelected(event);

      // Assert - Upload request
      const uploadReq = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/${mockDocumentId}/upload` &&
        req.method === 'POST' &&
        req.headers.get('X-Tenant-ID') === mockTenantId
      );
      expect(uploadReq.request.body.get('file')).toBe(mockFile);

      // Respond with upload success
      uploadReq.flush(mockUploadResponse);
      tick();

      // Verify uploadSuccess event emitted
      expect(uploadEmitted).toBe(true);
      expect(component.uploading).toBe(false);

      // Assert - First OCR status poll (immediate - timer(0, 2000))
      const statusReq1 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET' &&
        req.headers.get('X-Tenant-ID') === mockTenantId
      );
      statusReq1.flush({ attachmentId: mockAttachmentId, ocrStatus: 'PENDING' } as OcrStatusResponse);
      tick();

      expect(component.ocrStatus).toBe('PENDING');
      expect(ocrCompleteEmitted).toBe(false); // Not yet complete

      // Assert - Second OCR status poll (after 2 seconds)
      tick(2000);
      const statusReq2 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      statusReq2.flush({ attachmentId: mockAttachmentId, ocrStatus: 'PROCESSING' } as OcrStatusResponse);
      tick();

      expect(component.ocrStatus).toBe('PROCESSING');
      expect(ocrCompleteEmitted).toBe(false); // Still not complete

      // Assert - Third OCR status poll (after another 2 seconds)
      tick(2000);
      const statusReq3 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      statusReq3.flush({
        attachmentId: mockAttachmentId,
        ocrStatus: 'COMPLETED',
        ocrText: 'Extracted text from document',
        ocrProcessingDate: '2026-01-24T12:01:00Z'
      } as OcrStatusResponse);
      tick();

      // Verify OCR completion
      expect(component.ocrStatus).toBe('COMPLETED');
      expect(ocrCompleteEmitted).toBe(true);
      expect(emittedOcrEvent).toEqual({
        attachmentId: mockAttachmentId,
        ocrStatus: 'COMPLETED'
      });

      // Polling should stop after COMPLETED status
      tick(5000);
      httpMock.expectNone((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status`
      );

      flush();
    }));

    it('should handle OCR failure and retry', fakeAsync(() => {
      // Arrange
      const mockFile = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const mockUploadResponse: AttachmentUploadResponse = {
        attachmentId: mockAttachmentId,
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 1024,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      };

      let ocrCompleteCount = 0;
      let lastOcrStatus: OcrStatus | null = null;

      component.ocrComplete.subscribe((event) => {
        ocrCompleteCount++;
        lastOcrStatus = event.ocrStatus;
      });

      // Act - Upload file
      const event = {
        target: {
          files: [mockFile]
        }
      } as unknown as Event;
      component.onFileSelected(event);

      // Respond to upload request
      const uploadReq = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/${mockDocumentId}/upload` &&
        req.method === 'POST'
      );
      uploadReq.flush(mockUploadResponse);
      tick();

      // First status poll - PENDING
      const statusReq1 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      statusReq1.flush({ attachmentId: mockAttachmentId, ocrStatus: 'PENDING' } as OcrStatusResponse);
      tick();

      // Second status poll - FAILED
      tick(2000);
      const statusReq2 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      statusReq2.flush({
        attachmentId: mockAttachmentId,
        ocrStatus: 'FAILED',
        errorMessage: 'OCR processing failed due to low image quality'
      } as OcrStatusResponse);
      tick();

      // Verify FAILED status
      expect(component.ocrStatus).toBe('FAILED');
      expect(ocrCompleteCount).toBe(1);
      expect(lastOcrStatus).toBe('FAILED');

      // Polling should stop after FAILED status
      tick(5000);
      httpMock.expectNone((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status`
      );

      // Act - Retry OCR
      component.retryOcr(mockAttachmentId);

      // Assert - Reprocess request
      const retryReq = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/reprocess-ocr` &&
        req.method === 'POST' &&
        req.headers.get('X-Tenant-ID') === mockTenantId
      );
      retryReq.flush({});
      tick();

      // Assert - New status polling starts after retry
      const retryStatusReq1 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      retryStatusReq1.flush({ attachmentId: mockAttachmentId, ocrStatus: 'PROCESSING' } as OcrStatusResponse);
      tick();

      expect(component.ocrStatus).toBe('PROCESSING');

      // Final status poll - COMPLETED after retry
      tick(2000);
      const retryStatusReq2 = httpMock.expectOne((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status` &&
        req.method === 'GET'
      );
      retryStatusReq2.flush({
        attachmentId: mockAttachmentId,
        ocrStatus: 'COMPLETED',
        ocrText: 'Successfully extracted text after retry',
        ocrProcessingDate: '2026-01-24T12:02:00Z'
      } as OcrStatusResponse);
      tick();

      // Verify successful completion after retry
      expect(component.ocrStatus).toBe('COMPLETED');
      expect(ocrCompleteCount).toBe(2); // FAILED + COMPLETED
      expect(lastOcrStatus).toBe('COMPLETED');

      // Polling should stop after COMPLETED status
      tick(5000);
      httpMock.expectNone((req) =>
        req.url === `/api/documents/clinical/attachments/${mockAttachmentId}/ocr-status`
      );

      flush();
    }));
  });
});
