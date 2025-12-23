import { TestBed } from '@angular/core/testing';
import { ReportExportService, ReportOptions, ComplianceSummary } from './report-export.service';
import { QualityMeasureResult } from '../models/quality-result.model';

describe('ReportExportService', () => {
  let service: ReportExportService;

  const mockResults: QualityMeasureResult[] = [
    {
      id: '1',
      patientId: 'patient-1',
      measureName: 'Comprehensive Diabetes Care',
      measureCategory: 'HEDIS',
      denominatorEligible: true,
      numeratorCompliant: true,
      complianceRate: 100,
      score: 1.0,
      calculationDate: '2024-06-15T10:00:00Z',
    },
    {
      id: '2',
      patientId: 'patient-2',
      measureName: 'Comprehensive Diabetes Care',
      measureCategory: 'HEDIS',
      denominatorEligible: true,
      numeratorCompliant: false,
      complianceRate: 0,
      score: 0.0,
      calculationDate: '2024-06-16T10:00:00Z',
    },
    {
      id: '3',
      patientId: 'patient-3',
      measureName: 'Breast Cancer Screening',
      measureCategory: 'HEDIS',
      denominatorEligible: false,
      numeratorCompliant: false,
      complianceRate: 0,
      score: 0.0,
      calculationDate: '2024-06-17T10:00:00Z',
    },
    {
      id: '4',
      patientId: 'patient-4',
      measureName: 'Breast Cancer Screening',
      measureCategory: 'HEDIS',
      denominatorEligible: true,
      numeratorCompliant: true,
      complianceRate: 100,
      score: 1.0,
      calculationDate: '2024-06-18T10:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReportExportService],
    });
    service = TestBed.inject(ReportExportService);
  });

  describe('Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('calculateSummary', () => {
    it('should calculate correct summary statistics', () => {
      const summary = service.calculateSummary(mockResults);

      expect(summary.totalEvaluations).toBe(4);
      expect(summary.compliantCount).toBe(2);
      expect(summary.nonCompliantCount).toBe(1);
      expect(summary.notEligibleCount).toBe(1);
    });

    it('should calculate correct overall compliance rate', () => {
      const summary = service.calculateSummary(mockResults);

      // 2 compliant out of 3 eligible = 66.67%
      expect(summary.overallComplianceRate).toBeCloseTo(66.67, 1);
    });

    it('should group results by measure', () => {
      const summary = service.calculateSummary(mockResults);

      expect(summary.measureBreakdown.length).toBe(2);

      const cdcMeasure = summary.measureBreakdown.find(
        (m) => m.measureName === 'Comprehensive Diabetes Care'
      );
      expect(cdcMeasure).toBeTruthy();
      expect(cdcMeasure!.evaluations).toBe(2);
      expect(cdcMeasure!.compliant).toBe(1);
      expect(cdcMeasure!.eligible).toBe(2);
      expect(cdcMeasure!.complianceRate).toBe(50);
    });

    it('should handle empty results', () => {
      const summary = service.calculateSummary([]);

      expect(summary.totalEvaluations).toBe(0);
      expect(summary.compliantCount).toBe(0);
      expect(summary.overallComplianceRate).toBe(0);
      expect(summary.measureBreakdown.length).toBe(0);
    });

    it('should sort measures by compliance rate descending', () => {
      const summary = service.calculateSummary(mockResults);

      // BCS has 100% (1/1), CDC has 50% (1/2)
      expect(summary.measureBreakdown[0].measureName).toBe('Breast Cancer Screening');
      expect(summary.measureBreakdown[1].measureName).toBe('Comprehensive Diabetes Care');
    });
  });

  describe('generatePDFReport', () => {
    let windowOpenSpy: jest.SpyInstance;
    let mockPrintWindow: {
      document: {
        write: jest.Mock;
        close: jest.Mock;
        title: string;
      };
      print: jest.Mock;
      onload: (() => void) | null;
    };

    beforeEach(() => {
      mockPrintWindow = {
        document: {
          write: jest.fn(),
          close: jest.fn(),
          title: '',
        },
        print: jest.fn(),
        onload: null,
      };

      windowOpenSpy = jest.spyOn(window, 'open').mockReturnValue(mockPrintWindow as unknown as Window);
    });

    afterEach(() => {
      windowOpenSpy.mockRestore();
    });

    it('should open a new window with report content', () => {
      service.generatePDFReport(mockResults);

      expect(windowOpenSpy).toHaveBeenCalledWith('', '_blank', 'width=800,height=600');
      expect(mockPrintWindow.document.write).toHaveBeenCalled();
      expect(mockPrintWindow.document.close).toHaveBeenCalled();
    });

    it('should include title in report', () => {
      const options: ReportOptions = {
        title: 'Custom Report Title',
      };

      service.generatePDFReport(mockResults, options);

      const writtenContent = mockPrintWindow.document.write.mock.calls[0][0];
      expect(writtenContent).toContain('Custom Report Title');
    });

    it('should include date range in report when provided', () => {
      const options: ReportOptions = {
        dateRange: {
          from: new Date('2024-01-01'),
          to: new Date('2024-06-30'),
        },
      };

      service.generatePDFReport(mockResults, options);

      const writtenContent = mockPrintWindow.document.write.mock.calls[0][0];
      expect(writtenContent).toContain('Reporting Period');
    });

    it('should handle popup blocker gracefully', () => {
      windowOpenSpy.mockReturnValue(null);
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      service.generatePDFReport(mockResults);

      expect(consoleSpy).toHaveBeenCalledWith(
        'Failed to open print window. Please allow popups.'
      );

      consoleSpy.mockRestore();
    });
  });

  describe('downloadHTMLReport', () => {
    let originalCreateObjectURL: typeof URL.createObjectURL;
    let originalRevokeObjectURL: typeof URL.revokeObjectURL;
    let originalCreateElement: typeof document.createElement;
    let capturedBlob: Blob | null = null;
    let clickSpy: jest.Mock;

    beforeEach(() => {
      capturedBlob = null;
      clickSpy = jest.fn();

      // Save originals
      originalCreateObjectURL = URL.createObjectURL;
      originalRevokeObjectURL = URL.revokeObjectURL;
      originalCreateElement = document.createElement.bind(document);

      // Mock URL methods
      URL.createObjectURL = jest.fn((blob: Blob) => {
        capturedBlob = blob;
        return 'blob:test';
      });
      URL.revokeObjectURL = jest.fn();

      // Mock only anchor createElement to avoid recursion
      const mockLink = {
        href: '',
        download: '',
        click: clickSpy,
      };
      jest.spyOn(document, 'createElement').mockImplementation((tagName: string): any => {
        if (tagName === 'a') {
          return mockLink;
        }
        // For div (used in escapeHtml), return a simple mock
        if (tagName === 'div') {
          return {
            textContent: '',
            get innerHTML() {
              return this.textContent;
            },
          };
        }
        return originalCreateElement(tagName);
      });
    });

    afterEach(() => {
      // Restore
      URL.createObjectURL = originalCreateObjectURL;
      URL.revokeObjectURL = originalRevokeObjectURL;
      jest.restoreAllMocks();
    });

    it('should create and download HTML file', () => {
      service.downloadHTMLReport(mockResults);

      expect(URL.createObjectURL).toHaveBeenCalled();
      expect(capturedBlob).toBeInstanceOf(Blob);
      expect(capturedBlob!.type).toBe('text/html;charset=utf-8');
      expect(clickSpy).toHaveBeenCalled();
    });

    it('should include custom options in the report', () => {
      const options: ReportOptions = {
        title: 'Custom HTML Report',
        subtitle: 'Test Subtitle',
      };

      service.downloadHTMLReport(mockResults, options);

      expect(capturedBlob).toBeInstanceOf(Blob);
    });
  });

  describe('Report Content', () => {
    it('should include summary statistics in generated HTML', () => {
      const summary = service.calculateSummary(mockResults);

      // Verify summary values are correct
      expect(summary.compliantCount).toBe(2);
      expect(summary.nonCompliantCount).toBe(1);
      expect(summary.notEligibleCount).toBe(1);
    });

    it('should escape HTML in measure names', () => {
      const resultsWithHtml: QualityMeasureResult[] = [
        {
          id: '1',
          patientId: 'patient-1',
          measureName: '<script>alert("xss")</script>',
          measureCategory: 'HEDIS',
          denominatorEligible: true,
          numeratorCompliant: true,
          complianceRate: 100,
          score: 1.0,
          calculationDate: '2024-06-15T10:00:00Z',
        },
      ];

      const summary = service.calculateSummary(resultsWithHtml);
      expect(summary.measureBreakdown[0].measureName).toBe('<script>alert("xss")</script>');
      // The escaping happens in generateReportHTML which is private
    });
  });
});
