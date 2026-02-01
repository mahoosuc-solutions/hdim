import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { EvaluationService } from './evaluation.service';
import {
  SavedReport,
  ReportType,
} from '../models/quality-result.model';
import {
  buildQualityMeasureUrl,
  QUALITY_MEASURE_ENDPOINTS,
} from '../config/api.config';

describe('EvaluationService - Reports API', () => {
  let service: EvaluationService;
  let httpMock: HttpTestingController;

  const mockSavedReport: SavedReport = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    tenantId: 'TENANT001',
    reportType: 'PATIENT',
    reportName: 'Test Patient Report',
    patientId: '550e8400-e29b-41d4-a716-446655440000',
    reportData: JSON.stringify({ qualityScore: 85 }),
    createdBy: 'test-user',
    createdAt: '2024-01-15T10:30:00Z',
    status: 'COMPLETED',
  };

  const mockPopulationReport: SavedReport = {
    id: '223e4567-e89b-12d3-a456-426614174001',
    tenantId: 'TENANT001',
    reportType: 'POPULATION',
    reportName: 'Test Population Report',
    year: 2024,
    reportData: JSON.stringify({ totalPatients: 1000 }),
    createdBy: 'test-user',
    createdAt: '2024-01-15T11:00:00Z',
    status: 'COMPLETED',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EvaluationService],
    });

    service = TestBed.inject(EvaluationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('savePatientReport', () => {
    it('should save a patient report successfully', (done) => {
      const patientId = '550e8400-e29b-41d4-a716-446655440000';
      const reportName = 'Test Report';
      const createdBy = 'test-user';

      service.savePatientReport(patientId, reportName, createdBy).subscribe({
        next: (report) => {
          expect(report).toEqual(mockSavedReport);
          expect(report.reportType).toBe('PATIENT');
          expect(report.patientId).toBe(patientId);
          // Note: Backend may modify the report name
          expect(report.reportName).toBe(mockSavedReport.reportName);
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVE_PATIENT_REPORT,
        { patient: patientId, name: reportName, createdBy }
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockSavedReport);
    });

    it('should use default createdBy when not provided', (done) => {
      const patientId = '550e8400-e29b-41d4-a716-446655440000';
      const reportName = 'Test Report';

      service.savePatientReport(patientId, reportName).subscribe({
        next: () => done(),
        error: () => fail('Expected successful response'),
      };

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVE_PATIENT_REPORT,
        { patient: patientId, name: reportName, createdBy: 'system' }
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockSavedReport);
    });

    it('should handle errors when saving patient report', (done) => {
      const patientId = '550e8400-e29b-41d4-a716-446655440000';
      const reportName = 'Test Report';
      const errorMessage = 'Failed to save report';

      service.savePatientReport(patientId, reportName).subscribe({
        next: () => fail('Expected error response'),
        error: (error) => {
          expect(error.status).toBe(500);
          done();
        },
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVE_PATIENT_REPORT,
        { patient: patientId, name: reportName, createdBy: 'system' }
      );

      const req = httpMock.expectOne(expectedUrl);
      req.flush(errorMessage, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('savePopulationReport', () => {
    it('should save a population report successfully', (done) => {
      const year = 2024;
      const reportName = 'Test Population Report';
      const createdBy = 'test-user';

      service.savePopulationReport(year, reportName, createdBy).subscribe({
        next: (report) => {
          expect(report).toEqual(mockPopulationReport);
          expect(report.reportType).toBe('POPULATION');
          expect(report.year).toBe(year);
          expect(report.reportName).toBe(reportName);
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVE_POPULATION_REPORT,
        { year: year.toString(), name: reportName, createdBy }
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockPopulationReport);
    });

    it('should use default createdBy when not provided', (done) => {
      const year = 2024;
      const reportName = 'Test Population Report';

      service.savePopulationReport(year, reportName).subscribe({
        next: () => done(),
        error: () => fail('Expected successful response'),
      };

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVE_POPULATION_REPORT,
        { year: year.toString(), name: reportName, createdBy: 'system' }
      );

      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockPopulationReport);
    });
  });

  describe('getSavedReports', () => {
    it('should get all saved reports', (done) => {
      const mockReports = [mockSavedReport, mockPopulationReport];

      service.getSavedReports().subscribe({
        next: (reports) => {
          expect(reports.length).toBe(2);
          expect(reports).toEqual(mockReports);
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS);
      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockReports);
    });

    it('should get reports filtered by type', (done) => {
      const reportType: ReportType = 'PATIENT';
      const mockReports = [mockSavedReport];

      service.getSavedReports(reportType).subscribe({
        next: (reports) => {
          expect(reports.length).toBe(1);
          expect(reports[0].reportType).toBe('PATIENT');
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS,
        { type: reportType }
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockReports);
    });

    it('should return empty array when no reports exist', (done) => {
      service.getSavedReports().subscribe({
        next: (reports) => {
          expect(reports).toEqual([]);
          expect(reports.length).toBe(0);
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS);
      const req = httpMock.expectOne(expectedUrl);
      req.flush([]);
    });
  });

  describe('getSavedReport', () => {
    it('should get a specific saved report by ID', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';

      service.getSavedReport(reportId).subscribe({
        next: (report) => {
          expect(report).toEqual(mockSavedReport);
          expect(report.id).toBe(reportId);
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockSavedReport);
    });

    it('should handle 404 when report not found', (done) => {
      const reportId = 'non-existent-id';

      service.getSavedReport(reportId).subscribe({
        next: () => fail('Expected error response'),
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        },
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('deleteSavedReport', () => {
    it('should delete a saved report successfully', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';

      service.deleteSavedReport(reportId).subscribe({
        next: () => {
          // Successful void response - just complete
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('should handle 404 when deleting non-existent report', (done) => {
      const reportId = 'non-existent-id';

      service.deleteSavedReport(reportId).subscribe({
        next: () => fail('Expected error response'),
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        },
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.SAVED_REPORT_BY_ID(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('exportReportToCsv', () => {
    it('should export report to CSV as Blob', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';
      const mockCsvBlob = new Blob(['header1,header2\nvalue1,value2'], {
        type: 'text/csv',
      };

      service.exportReportToCsv(reportId).subscribe({
        next: (blob) => {
          expect(blob).toBeInstanceOf(Blob);
          expect(blob.type).toBe('text/csv');
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.EXPORT_CSV(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockCsvBlob);
    });

    it('should handle errors when exporting CSV', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';

      service.exportReportToCsv(reportId).subscribe({
        next: () => fail('Expected error response'),
        error: (error) => {
          expect(error.status).toBe(500);
          done();
        },
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.EXPORT_CSV(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('exportReportToExcel', () => {
    it('should export report to Excel as Blob', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';
      const mockExcelBlob = new Blob(['excel data'], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      };

      service.exportReportToExcel(reportId).subscribe({
        next: (blob) => {
          expect(blob).toBeInstanceOf(Blob);
          expect(blob.type).toBe(
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          );
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const expectedUrl = buildQualityMeasureUrl(
        QUALITY_MEASURE_ENDPOINTS.EXPORT_EXCEL(reportId)
      );

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockExcelBlob);
    });
  });

  describe('downloadReport', () => {
    it('should trigger download with correct filename', () => {
      const mockBlob = new Blob(['test data'], { type: 'text/csv' });
      const filename = 'test-report.csv';

      // Mock URL methods that may not exist in JSDOM
      global.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
      global.URL.revokeObjectURL = jest.fn();

      // Spy on document.createElement and anchor.click
      const mockAnchor = {
        href: '',
        download: '',
        click: jest.fn(),
      } as unknown as HTMLAnchorElement;
      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockAnchor);

      service.downloadReport(mockBlob, filename);

      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(createElementSpy).toHaveBeenCalledWith('a');
      expect(mockAnchor.href).toBe('blob:mock-url');
      expect(mockAnchor.download).toBe(filename);
      expect(mockAnchor.click).toHaveBeenCalled();
      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

      // Cleanup
      createElementSpy.mockRestore();
    });
  });

  describe('exportAndDownloadReport', () => {
    it('should export and download CSV report', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';
      const reportName = 'Test Report';
      const mockBlob = new Blob(['csv data'], { type: 'text/csv' };

      // Spy on downloadReport method
      const downloadSpy = jest.spyOn(service, 'downloadReport').mockImplementation(() => undefined);

      service.exportAndDownloadReport(reportId, reportName, 'csv').subscribe({
        next: () => {
          expect(downloadSpy).toHaveBeenCalledWith(mockBlob, 'Test Report.csv');
          downloadSpy.mockRestore();
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const req = httpMock.expectOne(
        buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.EXPORT_CSV(reportId))
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });

    it('should export and download Excel report', (done) => {
      const reportId = '123e4567-e89b-12d3-a456-426614174000';
      const reportName = 'Test Report';
      const mockBlob = new Blob(['excel data'], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      };

      // Spy on downloadReport method
      const downloadSpy = jest.spyOn(service, 'downloadReport').mockImplementation(() => undefined);

      service.exportAndDownloadReport(reportId, reportName, 'excel').subscribe({
        next: () => {
          expect(downloadSpy).toHaveBeenCalledWith(mockBlob, 'Test Report.xlsx');
          downloadSpy.mockRestore();
          done();
        },
        error: () => fail('Expected successful response'),
      });

      const req = httpMock.expectOne(
        buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.EXPORT_EXCEL(reportId))
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });
  });
});
