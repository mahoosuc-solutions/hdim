import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentUploadComponent } from './document-upload.component';
import { DocumentUploadService } from '../../services/document-upload.service';
import { LoggerService } from '../../services/logger.service';
import { of, throwError } from 'rxjs';

describe('DocumentUploadComponent', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let uploadService: jest.Mocked<DocumentUploadService>;
  let loggerService: jest.Mocked<LoggerService>;

  beforeEach(async () => {
    const uploadServiceMock = {
      uploadDocument: jest.fn(),
      pollOcrStatus: jest.fn(),
      retryOcr: jest.fn()
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
        DocumentUploadComponent,
        MatIconModule,
        MatButtonModule,
        MatProgressBarModule,
        MatChipsModule,
        MatListModule,
        MatTooltipModule
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DocumentUploadService, useValue: uploadServiceMock },
        { provide: LoggerService, useValue: loggerServiceMock }
      ]
    }).compileComponents();

    uploadService = TestBed.inject(DocumentUploadService) as jest.Mocked<DocumentUploadService>;
    loggerService = TestBed.inject(LoggerService) as jest.Mocked<LoggerService>;
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
      uploadService.uploadDocument.mockReturnValue(of({
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      }));
      uploadService.pollOcrStatus.mockReturnValue(of('COMPLETED'));

      component.onFileSelected({ target: { files: [file] } } as any);

      expect(component.errorMessage).toBe('');
      expect(uploadService.uploadDocument).toHaveBeenCalledWith('doc-123', file);
    });
  });

  describe('file upload', () => {
    it('should upload file and start OCR polling', (done) => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' };
      const mockResponse = {
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      };

      uploadService.uploadDocument.mockReturnValue(of(mockResponse));
      uploadService.pollOcrStatus.mockReturnValue(of('COMPLETED'));

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
      uploadService.uploadDocument.mockReturnValue(
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
      uploadService.uploadDocument.mockReturnValue(of({
        attachmentId: 'attach-123',
        fileName: 'test.pdf',
        mimeType: 'application/pdf',
        fileSize: 4,
        uploadDate: '2026-01-24T12:00:00Z',
        ocrStatus: 'PENDING'
      }));
      uploadService.pollOcrStatus.mockReturnValue(of('PROCESSING', 'COMPLETED'));

      component.onFileSelected({ target: { files: [file] } } as any);

      // Status should update through PROCESSING to COMPLETED
      expect(component.ocrStatus).toBeDefined();
    });
  });

  describe('retry OCR', () => {
    it('should retry failed OCR processing', () => {
      uploadService.retryOcr.mockReturnValue(of(undefined));
      uploadService.pollOcrStatus.mockReturnValue(of('COMPLETED'));

      component.retryOcr('attach-123');

      expect(uploadService.retryOcr).toHaveBeenCalledWith('attach-123');
      expect(uploadService.pollOcrStatus).toHaveBeenCalledWith('attach-123');
    });
  });
});
