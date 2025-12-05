import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ResultsComponent } from './results.component';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';
import { PatientFactory } from '../../../testing/factories/patient.factory';

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

    await TestBed.configureTestingModule({
      imports: [ResultsComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: PatientService, useValue: mockPatientService },
      ],
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
  });

  describe('Status Badges and UI Helpers', () => {
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
});
