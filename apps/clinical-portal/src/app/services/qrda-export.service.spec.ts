import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { QrdaExportService, QrdaExportJob, QrdaExportRequest } from './qrda-export.service';
import { environment } from '../../environments/environment';

describe('QrdaExportService', () => {
  let service: QrdaExportService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/qrda`;

  const mockJob: QrdaExportJob = {
    id: 'job-123',
    tenantId: 'tenant-1',
    jobType: 'QRDA_III',
    status: 'PENDING',
    measureIds: ['CDC', 'BCS'],
    periodStart: '2024-01-01',
    periodEnd: '2024-12-31',
    requestedBy: 'test-user',
    createdAt: '2024-01-15T10:00:00Z',
  };

  const mockRequest: QrdaExportRequest = {
    jobType: 'QRDA_III',
    measureIds: ['CDC', 'BCS'],
    periodStart: '2024-01-01',
    periodEnd: '2024-12-31',
    validateDocuments: true,
    includeSupplementalData: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        QrdaExportService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(QrdaExportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('generateCategoryI', () => {
    it('should send POST request to generate QRDA-I', () => {
      service.generateCategoryI(mockRequest).subscribe((job) => {
        expect(job).toEqual(mockJob);
      });

      const req = httpMock.expectOne(`${baseUrl}/category-i/generate`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.jobType).toBe('QRDA_I');
      req.flush(mockJob);
    });
  });

  describe('generateCategoryIII', () => {
    it('should send POST request to generate QRDA-III', () => {
      service.generateCategoryIII(mockRequest).subscribe((job) => {
        expect(job).toEqual(mockJob);
      });

      const req = httpMock.expectOne(`${baseUrl}/category-iii/generate`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.jobType).toBe('QRDA_III');
      req.flush(mockJob);
    });
  });

  describe('getJobStatus', () => {
    it('should get job status by ID', () => {
      service.getJobStatus('job-123').subscribe((job) => {
        expect(job).toEqual(mockJob);
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockJob);
    });
  });

  describe('listJobs', () => {
    it('should list jobs with pagination', () => {
      const mockPage = {
        content: [mockJob],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      };

      service.listJobs(0, 20).subscribe((page) => {
        expect(page.content.length).toBe(1);
        expect(page.totalElements).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs?page=0&size=20`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('should filter jobs by type and status', () => {
      const mockPage = {
        content: [mockJob],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      };

      service.listJobs(0, 20, 'QRDA_III', 'COMPLETED').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/jobs?page=0&size=20&jobType=QRDA_III&status=COMPLETED`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });
  });

  describe('downloadDocument', () => {
    it('should download document as blob', () => {
      const mockBlob = new Blob(['test content'], { type: 'application/xml' });

      service.downloadDocument('job-123').subscribe((blob) => {
        expect(blob).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-123/download`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });
  });

  describe('cancelJob', () => {
    it('should cancel a running job', () => {
      const cancelledJob = { ...mockJob, status: 'CANCELLED' as const };

      service.cancelJob('job-123').subscribe((job) => {
        expect(job.status).toBe('CANCELLED');
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-123/cancel`);
      expect(req.request.method).toBe('POST');
      req.flush(cancelledJob);
    });
  });

  describe('triggerDownload', () => {
    it('should create download link and trigger download', () => {
      const mockBlob = new Blob(['test'], { type: 'text/plain' });
      const clickSpy = jest.fn();

      // Mock URL methods
      const originalCreateObjectURL = URL.createObjectURL;
      const originalRevokeObjectURL = URL.revokeObjectURL;
      URL.createObjectURL = jest.fn().mockReturnValue('blob:test');
      URL.revokeObjectURL = jest.fn();

      // Mock createElement
      const originalCreateElement = document.createElement.bind(document);
      jest.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
        if (tagName === 'a') {
          return { href: '', download: '', click: clickSpy } as unknown as HTMLAnchorElement;
        }
        return originalCreateElement(tagName);
      });

      service.triggerDownload(mockBlob, 'test.xml');

      expect(URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(clickSpy).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');

      // Restore
      URL.createObjectURL = originalCreateObjectURL;
      URL.revokeObjectURL = originalRevokeObjectURL;
    });
  });

  describe('getExportFilename', () => {
    it('should return zip filename for QRDA-I', () => {
      const job = { ...mockJob, jobType: 'QRDA_I' as const };
      const filename = service.getExportFilename(job);
      expect(filename).toMatch(/^qrda-i-export-\d{4}-\d{2}-\d{2}\.zip$/);
    });

    it('should return xml filename for QRDA-III', () => {
      const job = { ...mockJob, jobType: 'QRDA_III' as const };
      const filename = service.getExportFilename(job);
      expect(filename).toMatch(/^qrda-iii-export-\d{4}-\d{2}-\d{2}\.xml$/);
    });
  });
});
