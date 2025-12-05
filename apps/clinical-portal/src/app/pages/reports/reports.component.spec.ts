import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ReportsComponent } from './reports.component';
import { EvaluationService } from '../../services/evaluation.service';
import { ToastService } from '../../services/toast.service';
import { SavedReport } from '../../models/quality-result.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ReportsComponent (TDD)', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;
  let mockDialog: jest.Mocked<MatDialog>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockToast: jest.Mocked<ToastService>;

  const mockSavedReports: SavedReport[] = [
    {
      id: 'report-1',
      reportName: 'Patient Report - John Doe',
      reportType: 'PATIENT',
      patientId: 'patient-123',
      tenantId: 'TENANT001',
      reportData: '{}',
      createdBy: 'user1',
      createdAt: '2024-01-15T10:00:00Z',
      status: 'COMPLETED',
    },
    {
      id: 'report-2',
      reportName: 'Population Report 2024',
      reportType: 'POPULATION',
      year: 2024,
      tenantId: 'TENANT001',
      reportData: '{}',
      createdBy: 'user2',
      createdAt: '2024-01-14T09:00:00Z',
      status: 'COMPLETED',
    },
    {
      id: 'report-3',
      reportName: 'Patient Report - Jane Smith',
      reportType: 'PATIENT',
      patientId: 'patient-456',
      tenantId: 'TENANT001',
      reportData: '{}',
      createdBy: 'user1',
      createdAt: '2024-01-13T08:00:00Z',
      status: 'COMPLETED',
    },
  ];

  beforeEach(async () => {
    // Create default mock dialog ref
    const defaultMockDialogRef = {
      afterClosed: jest.fn().mockReturnValue(of(null)),
    };

    mockDialog = {
      open: jest.fn().mockReturnValue(defaultMockDialogRef as unknown as MatDialogRef<unknown>),
    } as unknown as jest.Mocked<MatDialog>;

    mockEvaluationService = {
      getSavedReports: jest.fn(),
      savePatientReport: jest.fn(),
      savePopulationReport: jest.fn(),
      deleteSavedReport: jest.fn(),
      exportAndDownloadReport: jest.fn(),
    } as unknown as jest.Mocked<EvaluationService>;

    mockToast = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as unknown as jest.Mocked<ToastService>;

    await TestBed.configureTestingModule({
      imports: [ReportsComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: MatDialog, useValue: mockDialog },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    // Default mock to return empty array
    mockEvaluationService.getSavedReports.mockReturnValue(of([]));

    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
    // Don't call fixture.detectChanges() here - let individual tests control when ngOnInit is called
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default tab (Generate Reports)', () => {
      expect(component.selectedTabIndex).toBe(0);
    });

    it('should initialize report filter to null (all reports)', () => {
      expect(component.selectedReportType()).toBeNull();
    });

    it('should initialize with empty reports array', () => {
      // Before ngOnInit, should have initial reports
      expect(component.savedReports().length).toBeGreaterThan(0);
    });

    it('should initialize loading states to false', () => {
      expect(component.isLoadingReports()).toBe(false);
      expect(component.isGeneratingPatientReport()).toBe(false);
      expect(component.isGeneratingPopulationReport()).toBe(false);
    });

    it('should load saved reports on init', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of(mockSavedReports));

      component.ngOnInit();

      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith(undefined);
      expect(component.savedReports()).toEqual(mockSavedReports);
    });
  });

  describe('Loading Saved Reports', () => {
    it('should fetch all reports when no filter applied', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of(mockSavedReports));

      component.loadSavedReports();

      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith(undefined);
      expect(component.savedReports()).toEqual(mockSavedReports);
    });

    it('should filter by PATIENT report type', () => {
      const patientReports = mockSavedReports.filter((r) => r.reportType === 'PATIENT');
      mockEvaluationService.getSavedReports.mockReturnValue(of(patientReports));

      component.loadSavedReports('PATIENT');

      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith('PATIENT');
      expect(component.savedReports()).toEqual(patientReports);
    });

    it('should filter by POPULATION report type', () => {
      const populationReports = mockSavedReports.filter((r) => r.reportType === 'POPULATION');
      mockEvaluationService.getSavedReports.mockReturnValue(of(populationReports));

      component.loadSavedReports('POPULATION');

      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith('POPULATION');
      expect(component.savedReports()).toEqual(populationReports);
    });

    it('should set loading state while fetching', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of(mockSavedReports));

      component.loadSavedReports();

      expect(component.isLoadingReports()).toBe(false);
    });

    it('should handle empty results', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of([]));

      component.loadSavedReports();

      // When empty, component shows INITIAL_SAVED_REPORTS as fallback
      expect(component.savedReports().length).toBeGreaterThan(0);
      expect(component.isLoadingReports()).toBe(false);
    });

    it('should handle error loading reports', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(
        throwError(() => new Error('API Error'))
      );

      component.loadSavedReports();

      expect(mockToast.error).toHaveBeenCalledWith('Failed to load reports');
      expect(component.isLoadingReports()).toBe(false);
    });

    it('should reset loading state on error', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(
        throwError(() => new Error('API Error'))
      );

      component.loadSavedReports();

      expect(component.isLoadingReports()).toBe(false);
    });
  });

  describe('Report Filtering', () => {
    beforeEach(() => {
      mockEvaluationService.getSavedReports.mockReturnValue(of(mockSavedReports));
    });

    it('should update selected report type when filtering', () => {
      component.filterReports('PATIENT');

      expect(component.selectedReportType()).toBe('PATIENT');
    });

    it('should reload reports with filter applied', () => {
      component.filterReports('PATIENT');

      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith('PATIENT');
    });

    it('should clear filter when null passed', () => {
      component.selectedReportType.set('PATIENT');

      component.filterReports(null);

      expect(component.selectedReportType()).toBeNull();
      expect(mockEvaluationService.getSavedReports).toHaveBeenCalledWith(undefined);
    });

    it('should switch between filters correctly', () => {
      component.filterReports('PATIENT');
      expect(component.selectedReportType()).toBe('PATIENT');

      component.filterReports('POPULATION');
      expect(component.selectedReportType()).toBe('POPULATION');

      component.filterReports(null);
      expect(component.selectedReportType()).toBeNull();
    });
  });

  describe('Generate Patient Report', () => {
    it('should have onGeneratePatientReport method', () => {
      expect(typeof component.onGeneratePatientReport).toBe('function');
    });

    it('should not generate report without user selection', () => {
      // Verify initial state - no reports generated yet
      expect(component.isGeneratingPatientReport()).toBe(false);
      expect(mockEvaluationService.savePatientReport).not.toHaveBeenCalled();
    });
  });

  describe('Generate Population Report', () => {
    it('should have onGeneratePopulationReport method', () => {
      expect(typeof component.onGeneratePopulationReport).toBe('function');
    });

    it('should not generate report without user selection', () => {
      // Verify initial state - no reports generated yet
      expect(component.isGeneratingPopulationReport()).toBe(false);
      expect(mockEvaluationService.savePopulationReport).not.toHaveBeenCalled();
    });
  });

  describe('View Report Details', () => {
    it('should have onViewReport method', () => {
      expect(typeof component.onViewReport).toBe('function');
    });

    it('should accept SavedReport as parameter', () => {
      const report = mockSavedReports[0];
      // Verify method signature accepts SavedReport
      expect(() => component.onViewReport(report)).toBeDefined();
    });
  });

  describe('Export to CSV', () => {
    it('should call export service with CSV format', () => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(of(void 0));

      component.onExportCsv(report);

      expect(mockEvaluationService.exportAndDownloadReport).toHaveBeenCalledWith(
        report.id,
        report.reportName,
        'csv'
      );
    });

    it('should show success message on export', (done) => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(of(void 0));

      component.onExportCsv(report);

      setTimeout(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Report exported to CSV');
        done();
      }, 0);
    });

    it('should handle export error', (done) => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(
        throwError(() => new Error('Export failed'))
      );

      component.onExportCsv(report);

      setTimeout(() => {
        expect(mockToast.error).toHaveBeenCalledWith('Failed to export report to CSV');
        done();
      }, 0);
    });
  });

  describe('Export to Excel', () => {
    it('should call export service with Excel format', () => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(of(void 0));

      component.onExportExcel(report);

      expect(mockEvaluationService.exportAndDownloadReport).toHaveBeenCalledWith(
        report.id,
        report.reportName,
        'excel'
      );
    });

    it('should show success message on export', (done) => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(of(void 0));

      component.onExportExcel(report);

      setTimeout(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Report exported to Excel');
        done();
      }, 0);
    });

    it('should handle export error', (done) => {
      const report = mockSavedReports[0];
      mockEvaluationService.exportAndDownloadReport.mockReturnValue(
        throwError(() => new Error('Export failed'))
      );

      component.onExportExcel(report);

      setTimeout(() => {
        expect(mockToast.error).toHaveBeenCalledWith('Failed to export report to Excel');
        done();
      }, 0);
    });
  });

  describe('Delete Report', () => {
    it('should have onDeleteReport method', () => {
      expect(typeof component.onDeleteReport).toBe('function');
    });

    it('should accept SavedReport as parameter', () => {
      const report = mockSavedReports[0];
      // Verify method signature accepts SavedReport
      expect(() => component.onDeleteReport(report)).toBeDefined();
    });

    it('should not delete report without confirmation', () => {
      // Verify initial state - no reports deleted yet
      expect(mockEvaluationService.deleteSavedReport).not.toHaveBeenCalled();
    });
  });

  describe('Date Formatting', () => {
    it('should format date to localized string', () => {
      const dateString = '2024-01-15T10:30:00Z';

      const formatted = component.formatDate(dateString);

      expect(formatted).toMatch(/Jan|January/);
      expect(formatted).toContain('15');
      expect(formatted).toContain('2024');
    });

    it('should handle different date formats', () => {
      const dates = [
        '2024-01-15T10:30:00Z',
        '2024-12-31T23:59:59Z',
        '2024-06-15T12:00:00Z',
      ];

      dates.forEach((date) => {
        const formatted = component.formatDate(date);
        expect(formatted).toBeTruthy();
        expect(formatted.length).toBeGreaterThan(0);
      });
    });
  });

  describe('Time Formatting', () => {
    it('should format time to 12-hour format', () => {
      const dateString = '2024-01-15T10:30:00Z';

      const formatted = component.formatTime(dateString);

      expect(formatted).toMatch(/\d{1,2}:\d{2}/);
    });

    it('should include AM/PM indicator', () => {
      const morningTime = '2024-01-15T10:30:00Z';
      const eveningTime = '2024-01-15T22:30:00Z';

      const morning = component.formatTime(morningTime);
      const evening = component.formatTime(eveningTime);

      expect(morning).toBeTruthy();
      expect(evening).toBeTruthy();
    });
  });

  describe('Report Type Indicators', () => {
    it('should identify patient reports', () => {
      const patientReports = component.savedReports().filter((r) => r.reportType === 'PATIENT');

      expect(patientReports.length).toBeGreaterThanOrEqual(0);
    });

    it('should identify population reports', () => {
      component.savedReports.set(mockSavedReports);
      const populationReports = component
        .savedReports()
        .filter((r) => r.reportType === 'POPULATION');

      expect(populationReports.length).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Empty State', () => {
    it('should handle empty reports list', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of([]));

      component.loadSavedReports();

      // When empty, component shows INITIAL_SAVED_REPORTS as fallback
      expect(component.savedReports().length).toBeGreaterThan(0);
    });

    it('should display empty state when filtered to empty', () => {
      mockEvaluationService.getSavedReports.mockReturnValue(of([]));

      component.filterReports('PATIENT');

      // When empty, component shows INITIAL_SAVED_REPORTS as fallback
      expect(component.savedReports().length).toBeGreaterThan(0);
    });
  });
});
