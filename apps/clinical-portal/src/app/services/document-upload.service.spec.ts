import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DocumentUploadService, AttachmentUploadResponse } from './document-upload.service';
import { AuthService } from './auth.service';

describe('DocumentUploadService', () => {
  let service: DocumentUploadService;
  let httpMock: HttpTestingController;
  let mockAuthService: jest.Mocked<AuthService>;

  beforeEach(() => {
    mockAuthService = {
      getTenantId: jest.fn().mockReturnValue('test-tenant-123')
    } as any;

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DocumentUploadService,
        { provide: AuthService, useValue: mockAuthService }
      ]
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

    it('should include X-Tenant-ID header from AuthService', (done) => {
      const documentId = 'doc-123';
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });

      service.uploadDocument(documentId, file).subscribe(() => done());

      const req = httpMock.expectOne(`/api/documents/clinical/${documentId}/upload`);
      expect(req.request.headers.has('X-Tenant-ID')).toBe(true);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('test-tenant-123');
      req.flush({});
    });
  });

  describe('pollOcrStatus', () => {
    it('should poll status immediately then every 2 seconds until COMPLETED', fakeAsync(() => {
      const attachmentId = 'attach-456';
      const responses = [
        { attachmentId, ocrStatus: 'PENDING' as const },
        { attachmentId, ocrStatus: 'PROCESSING' as const },
        { attachmentId, ocrStatus: 'COMPLETED' as const, ocrText: 'extracted text' }
      ];
      const emittedStatuses: string[] = [];

      service.pollOcrStatus(attachmentId).subscribe(status => {
        emittedStatuses.push(status);
      });

      // timer(0, 2000) emits immediately, then every 2 seconds
      // First emission (immediate at t=0)
      tick(0);
      const req1 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req1.flush(responses[0]);

      // Second emission (at t=2000ms)
      tick(2000);
      const req2 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req2.flush(responses[1]);

      // Third emission (at t=4000ms)
      tick(2000);
      const req3 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req3.flush(responses[2]);

      flush();
      expect(emittedStatuses).toEqual(['PENDING', 'PROCESSING', 'COMPLETED']);
    }));

    it('should stop polling when status is FAILED', fakeAsync(() => {
      const attachmentId = 'attach-789';
      const responses = [
        { attachmentId, ocrStatus: 'PENDING' as const },
        { attachmentId, ocrStatus: 'PROCESSING' as const },
        { attachmentId, ocrStatus: 'FAILED' as const, errorMessage: 'OCR processing failed' }
      ];
      const emittedStatuses: string[] = [];

      service.pollOcrStatus(attachmentId).subscribe({
        next: (status) => {
          emittedStatuses.push(status);
        }
      });

      // First emission (immediate at t=0)
      tick(0);
      const req1 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req1.flush(responses[0]);

      // Second emission (at t=2000ms)
      tick(2000);
      const req2 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req2.flush(responses[1]);

      // Third emission (at t=4000ms) - should stop after FAILED
      tick(2000);
      const req3 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      req3.flush(responses[2]);

      flush();
      expect(emittedStatuses).toEqual(['PENDING', 'PROCESSING', 'FAILED']);

      // Verify no more requests after FAILED status
      httpMock.expectNone(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
    }));

    it('should retry failed HTTP requests during polling', fakeAsync(() => {
      const attachmentId = 'attach-999';
      let attemptCount = 0;
      const emittedStatuses: string[] = [];

      service.pollOcrStatus(attachmentId).subscribe({
        next: (status) => {
          emittedStatuses.push(status);
        }
      });

      // First polling interval (t=0) - initial request fails
      tick(0);
      const req1 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      attemptCount++;
      req1.flush('Network error', { status: 500, statusText: 'Internal Server Error' });

      // First retry (after 1 second delay from retry config)
      tick(1000);
      const req2 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      attemptCount++;
      req2.flush('Network error', { status: 500, statusText: 'Internal Server Error' });

      // Second retry (after another 1 second)
      tick(1000);
      const req3 = httpMock.expectOne(`/api/documents/clinical/attachments/${attachmentId}/ocr-status`);
      attemptCount++;
      req3.flush({ attachmentId, ocrStatus: 'COMPLETED' });

      flush();
      expect(attemptCount).toBe(3); // 1 initial + 2 retries
      expect(emittedStatuses).toEqual(['COMPLETED']);
    }));
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
