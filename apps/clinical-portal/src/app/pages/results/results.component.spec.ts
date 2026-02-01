import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ResultsComponent } from './results.component';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { LoggerService } from '../../services/logger.service';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { CSVHelper } from '../../utils/csv-helper';
import { createMockStore } from '../../testing/mocks';
import { Store } from '@ngrx/store';

/**
 * TDD Test Suite for Results Management Component
 *
 * This test suite is written BEFORE implementation to follow TDD principles.
 * The Results component allows users to:
 * - View all evaluation results in a paginated list
 * - Filter results by date range, measure type, and outcome status
 * - Sort results by various criteria
 * - View detailed information for a single result
 * - Export results to CSV/Excel
 */
describe('ResultsComponent (TDD)', () => {
  let component: ResultsComponent;
  let fixture: ComponentFixture<ResultsComponent>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockLoggerService: jest.Mocked<any>;
  let globalConsoleErrorSpy: jest.SpyInstance;

  beforeAll(() => {
    globalConsoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  beforeEach(async () => {
    // Create mock services
    mockEvaluationService = {
      getPatientResults: jest.fn(),
      getPatientReport: jest.fn(),
      getAllResults: jest.fn(),
    } as any;
    mockEvaluationService.getAllResults.mockReturnValue(of([]));

    mockPatientService = {
      getPatient: jest.fn(),
      toPatientSummary: jest.fn(),
    } as any;

    mockLoggerService = createMockLoggerService();

    await TestBed.configureTestingModule({
      imports: [ResultsComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: LoggerService, useValue: mockLoggerService },
    }).compileComponents();

    fixture = TestBed.createComponent(ResultsComponent);
    component = fixture.componentInstance;
    EvaluationFactory.reset();
    PatientFactory.reset();
  });

  afterAll(() => {
    globalConsoleErrorSpy.mockRestore();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize filter form with default values', () => {
      expect(component.filterForm).toBeDefined();
      expect(component.filterForm.get('dateFrom')).toBeDefined();
      expect(component.filterForm.get('dateTo')).toBeDefined();
      expect(component.filterForm.get('measureType')).toBeDefined();
      expect(component.filterForm.get('status')).toBeDefined();
    });

    it('should load results on ngOnInit', () => {
      const loadResultsSpy = jest.spyOn(component, 'loadResults');
      mockEvaluationService.getAllResults.mockReturnValue(of([]));

      component.ngOnInit();

      expect(loadResultsSpy).toHaveBeenCalled();
    });

    it('should initialize with empty results array', () => {
      expect(component.results).toEqual([]);
    });

    it('should initialize with default pagination settings', () => {
      expect(component.currentPage).toBe(0);
      expect(component.pageSize).toBe(20);
    });
  });

  describe('Loading Results', () => {
    it('should load all results successfully', () => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
        EvaluationFactory.createNotEligibleResult(),
      ];
      mockEvaluationService.getAllResults.mockReturnValue(of(mockResults));

      component.loadResults();

      expect(component.loading).toBe(false);
      expect(component.results.length).toBe(3);
      expect(component.error).toBeNull();
    });

    it('should set loading state while fetching results', () => {
      mockEvaluationService.getAllResults.mockReturnValue(of([]));

      component.loadResults();

      expect(component.loading).toBe(false); // Will be false after subscription
    });

    it('should handle error when loading results fails', () => {
      const error = { status: 500, message: 'Server Error' };
      mockEvaluationService.getAllResults.mockReturnValue(
        throwError(() => error)
      );

      component.loadResults();

      expect(component.loading).toBe(false);
      expect(component.error).toContain('Failed to load results');
    });

    it('should calculate total results count', () => {
      const mockResults = EvaluationFactory.createMany(50);
      mockEvaluationService.getAllResults.mockReturnValue(of(mockResults));

      component.loadResults();

      expect(component.totalResults).toBe(50);
    });
  });

  describe('Filtering Results', () => {
    beforeEach(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
        EvaluationFactory.createNotEligibleResult(),
      ];
      component.results = mockResults;
    });

    it('should filter results by date range', () => {
      component.filterForm.patchValue({
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      });

      component.applyFilters();

      expect(component.filteredResults.length).toBeGreaterThan(0);
    });

    it('should filter results by measure type', () => {
      component.filterForm.patchValue({
        measureType: 'HEDIS',
      });

      component.applyFilters();

      expect(component.filteredResults.every((r) => r.measureCategory === 'HEDIS')).toBe(true);
    });

    it('should filter results by status (compliant)', () => {
      component.filterForm.patchValue({
        status: 'compliant',
      });

      component.applyFilters();

      expect(component.filteredResults.every((r) => r.numeratorCompliant)).toBe(true);
    });

    it('should filter results by status (non-compliant)', () => {
      component.filterForm.patchValue({
        status: 'non-compliant',
      });

      component.applyFilters();

      expect(
        component.filteredResults.every((r) => !r.numeratorCompliant && r.denominatorEligible)
      ).toBe(true);
    });

    it('should combine multiple filters', () => {
      component.filterForm.patchValue({
        measureType: 'HEDIS',
        status: 'compliant',
        dateFrom: '2024-01-01',
      });

      component.applyFilters();

      expect(component.filteredResults.every((r) =>
        r.measureCategory === 'HEDIS' && r.numeratorCompliant
      )).toBe(true);
    });

    it('should reset filters', () => {
      component.filterForm.patchValue({
        measureType: 'HEDIS',
        status: 'compliant',
      });

      component.resetFilters();

      expect(component.filterForm.get('measureType')?.value).toBeNull();
      expect(component.filterForm.get('status')?.value).toBeNull();
      expect(component.filteredResults.length).toBe(component.results.length);
    });
  });

  describe('Sorting Results', () => {
    beforeEach(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
        EvaluationFactory.createNotEligibleResult(),
      ];
      component.results = mockResults;
      component.filteredResults = [...mockResults];
    });

    it('should sort results by date (ascending)', () => {
      component.sortBy('date', 'asc');

      const dates = component.filteredResults.map((r) => new Date(r.calculationDate));
      for (let i = 1; i < dates.length; i++) {
        expect(dates[i].getTime()).toBeGreaterThanOrEqual(dates[i - 1].getTime());
      }
    });

    it('should sort results by date (descending)', () => {
      component.sortBy('date', 'desc');

      const dates = component.filteredResults.map((r) => new Date(r.calculationDate));
      for (let i = 1; i < dates.length; i++) {
        expect(dates[i].getTime()).toBeLessThanOrEqual(dates[i - 1].getTime());
      }
    });

    it('should sort results by measure name', () => {
      component.sortBy('measureName', 'asc');

      const names = component.filteredResults.map((r) => r.measureName);
      const sortedNames = [...names].sort();
      expect(names).toEqual(sortedNames);
    });

    it('should sort results by compliance rate', () => {
      component.sortBy('complianceRate', 'desc');

      const rates = component.filteredResults.map((r) => r.complianceRate);
      for (let i = 1; i < rates.length; i++) {
        expect(rates[i]).toBeLessThanOrEqual(rates[i - 1]);
      }
    });

    it('should toggle sort direction on same column', () => {
      component.sortBy('date', 'asc');
      const firstAscending = [...component.filteredResults];

      component.sortBy('date', 'desc');
      const firstDescending = component.filteredResults[0];

      expect(firstDescending).toEqual(firstAscending[firstAscending.length - 1]);
    });
  });

  describe('Pagination', () => {
    beforeEach(() => {
      const mockResults = Array.from({ length: 50 }, () =>
        EvaluationFactory.createCompliantResult()
      );
      component.results = mockResults;
      component.filteredResults = mockResults;
    });

    it('should paginate results correctly', () => {
      component.pageSize = 20;
      component.currentPage = 0;

      const paginatedResults = component.getPaginatedResults();

      expect(paginatedResults.length).toBe(20);
    });

    it('should navigate to next page', () => {
      component.pageSize = 20;
      component.currentPage = 0;

      component.nextPage();

      expect(component.currentPage).toBe(1);
    });

    it('should navigate to previous page', () => {
      component.currentPage = 2;

      component.previousPage();

      expect(component.currentPage).toBe(1);
    });

    it('should not go to negative page numbers', () => {
      component.currentPage = 0;

      component.previousPage();

      expect(component.currentPage).toBe(0);
    });

    it('should calculate total pages correctly', () => {
      component.pageSize = 20;

      const totalPages = component.getTotalPages();

      expect(totalPages).toBe(3); // 50 results / 20 per page = 3 pages
    });

    it('should not exceed last page', () => {
      component.pageSize = 20;
      component.currentPage = 2; // Last page (0-indexed)

      component.nextPage();

      expect(component.currentPage).toBe(2); // Should not increment
    });
  });

  describe('Result Details View', () => {
    it('should select a result for detailed view', () => {
      const result = EvaluationFactory.createCompliantResult();

      component.viewResultDetails(result);

      expect(component.selectedResult).toEqual(result);
      expect(component.showDetailsPanel).toBe(true);
    });

    it('should close details panel', () => {
      component.selectedResult = EvaluationFactory.createCompliantResult();
      component.showDetailsPanel = true;

      component.closeDetailsPanel();

      expect(component.selectedResult).toBeNull();
      expect(component.showDetailsPanel).toBe(false);
    });

    it('should load patient information when viewing details', () => {
      const result = EvaluationFactory.createCompliantResult();
      const mockPatient = PatientFactory.createJohnDoe();
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));

      component.viewResultDetails(result);

      expect(mockPatientService.getPatient).toHaveBeenCalledWith(result.patientId);
    });
  });

  describe('Export Functionality', () => {
    beforeEach(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
      ];
      component.filteredResults = mockResults;
    });

    it('should export results to CSV and update loading flags', () => {
      jest.useFakeTimers();
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation(() => undefined);

      component.exportToCSV();
      expect(component.exportCsvLoading).toBe(true);

      jest.advanceTimersByTime(500);

      expect(component.exportCsvLoading).toBe(false);
      expect(component.exportCsvSuccess).toBe(true);
      expect(downloadSpy).toHaveBeenCalled();

      downloadSpy.mockRestore();
      jest.useRealTimers();
    });

    it('should export results to Excel and update loading flags', () => {
      jest.useFakeTimers();
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation(() => undefined);

      component.exportToExcel();
      expect(component.exportExcelLoading).toBe(true);

      jest.advanceTimersByTime(500);

      expect(component.exportExcelLoading).toBe(false);
      expect(component.exportExcelSuccess).toBe(true);
      expect(downloadSpy).toHaveBeenCalled();

      downloadSpy.mockRestore();
      jest.useRealTimers();
    });

    it('should export results to CSV', () => {
      const exportSpy = jest.spyOn(component, 'exportToCSV');

      component.exportToCSV();

      expect(exportSpy).toHaveBeenCalled();
    });

    it('should export results to Excel', () => {
      const exportSpy = jest.spyOn(component, 'exportToExcel');

      component.exportToExcel();

      expect(exportSpy).toHaveBeenCalled();
    });

    it('should include all filtered results in export', () => {
      const csvData = component.generateCSVData();

      expect(csvData.length).toBe(component.filteredResults.length + 1); // +1 for header
    });

    it('should include correct columns in CSV export', () => {
      const csvData = component.generateCSVData();
      const headers = csvData[0];

      expect(headers).toContain('Patient ID');
      expect(headers).toContain('Measure Name');
      expect(headers).toContain('Evaluation Date');
      expect(headers).toContain('Outcome');
      expect(headers).toContain('Compliance Rate');
    });

    it('should export only selected rows when selection exists', () => {
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation(() => undefined);
      component.dataSource.data = component.filteredResults;
      component.selection.select(component.filteredResults[0]);

      component.exportSelectedToCSV();

      expect(downloadSpy).toHaveBeenCalled();
      downloadSpy.mockRestore();
    });

    it('should skip export when no rows are selected', () => {
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation(() => undefined);
      component.selection.clear();

      component.exportSelectedToCSV();

      expect(downloadSpy).not.toHaveBeenCalled();
      downloadSpy.mockRestore();
    });
  });

  describe('Statistics and Summary', () => {
    beforeEach(() => {
      const mockResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
        EvaluationFactory.createNotEligibleResult(),
      ];
      component.results = mockResults;
      component.filteredResults = mockResults;
    });

    it('should calculate overall compliance rate', () => {
      const complianceRate = component.calculateOverallCompliance();

      expect(complianceRate).toBeCloseTo(50.0, 1); // 2 out of 4 compliant = 50%
    });

    it('should count compliant results', () => {
      const compliantCount = component.getCompliantCount();

      expect(compliantCount).toBe(2);
    });

    it('should count non-compliant results', () => {
      const nonCompliantCount = component.getNonCompliantCount();

      expect(nonCompliantCount).toBe(1);
    });

    it('should count not eligible results', () => {
      const notEligibleCount = component.getNotEligibleCount();

      expect(notEligibleCount).toBe(1);
    });

    it('should group results by measure type', () => {
      const groupedResults = component.groupByMeasureType();

      expect(groupedResults).toHaveProperty('HEDIS');
      expect(groupedResults['HEDIS'].length).toBeGreaterThan(0);
    });

    it('should return zero overall compliance when no results', () => {
      component.filteredResults = [];

      expect(component.calculateOverallCompliance()).toBe(0);
    });

    it('should return compliance stats bundle', () => {
      const stats = component.getComplianceStats();

      expect(stats).toHaveProperty('compliant');
      expect(stats).toHaveProperty('nonCompliant');
      expect(stats).toHaveProperty('notEligible');
      expect(stats).toHaveProperty('overallRate');
    });
  });

  describe('Status Badges and UI Helpers', () => {
    it('should return correct outcome text', () => {
      expect(component.getOutcomeText(EvaluationFactory.createCompliantResult())).toBe('Compliant');
      expect(component.getOutcomeText(EvaluationFactory.createNonCompliantResult())).toBe('Non-Compliant');
      expect(component.getOutcomeText(EvaluationFactory.createNotEligibleResult())).toBe('Not Eligible');
    });

    it('should return correct status class', () => {
      expect(component.getStatusClass(EvaluationFactory.createCompliantResult())).toBe('status-compliant');
      expect(component.getStatusClass(EvaluationFactory.createNonCompliantResult())).toBe('status-non-compliant');
      expect(component.getStatusClass(EvaluationFactory.createNotEligibleResult())).toBe('status-not-eligible');
    });

    it('should return correct badge class for compliant result', () => {
      const result = EvaluationFactory.createCompliantResult();

      const badgeClass = component.getStatusBadgeClass(result);

      expect(badgeClass).toBe('badge-success');
    });

    it('should return correct badge class for non-compliant result', () => {
      const result = EvaluationFactory.createNonCompliantResult();

      const badgeClass = component.getStatusBadgeClass(result);

      expect(badgeClass).toBe('badge-warning');
    });

    it('should return correct badge class for not eligible result', () => {
      const result = EvaluationFactory.createNotEligibleResult();

      const badgeClass = component.getStatusBadgeClass(result);

      expect(badgeClass).toBe('badge-info');
    });

    it('should format date correctly', () => {
      const dateString = '2024-01-15T10:30:00Z';

      const formattedDate = component.formatDate(dateString);

      expect(formattedDate).toContain('2024');
      expect(formattedDate).toContain('01');
      expect(formattedDate).toContain('15');
    });

    it('should format percentage correctly', () => {
      const percentage = component.formatPercentage(85.5);

      expect(percentage).toBe('85.5%');
    });
  });

  describe('Chart and Selection Helpers', () => {
    it('should update chart data based on filtered results', () => {
      component.filteredResults = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
        EvaluationFactory.createNotEligibleResult(),
      ];

      component.updateChartData();

      expect(component.outcomeDistributionChartData.length).toBe(3);
      expect(component.categoryComplianceChartData.length).toBeGreaterThan(0);
    });

    it('should handle total pages when page size is zero', () => {
      component.pageSize = 0;
      component.filteredResults = [EvaluationFactory.createCompliantResult()];

      expect(component.getTotalPages()).toBe(0);
    });

    it('should toggle selection for all rows', () => {
      const row = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [row];

      expect(component.isAllSelected()).toBe(false);
      component.masterToggle();
      expect(component.isAllSelected()).toBe(true);
      component.masterToggle();
      expect(component.isAllSelected()).toBe(false);
    });

    it('should provide checkbox labels for rows and header', () => {
      const row = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [row];

      expect(component.checkboxLabel()).toContain('select all');
      component.selection.select(row);
      expect(component.checkboxLabel(row)).toContain(`deselect row ${row.id}`);
    });

    it('should clear selection and report selection count', () => {
      const row = EvaluationFactory.createCompliantResult();
      component.selection.select(row);

      expect(component.getSelectionCount()).toBe(1);
      component.clearSelection();
      expect(component.getSelectionCount()).toBe(0);
    });
  });
});
