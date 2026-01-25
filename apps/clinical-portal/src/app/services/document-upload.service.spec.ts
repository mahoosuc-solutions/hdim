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

      // interval(2000) waits 2 seconds before first emission
      setTimeout(() => {
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
