import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { PatientsComponent } from './patients.component';
import { PatientService } from '../../services/patient.service';
import { EvaluationService } from '../../services/evaluation.service';
import { DialogService } from '../../services/dialog.service';
import { FilterPersistenceService } from '../../services/filter-persistence.service';
import { PatientDeduplicationService } from '../../services/patient-deduplication.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';
import { CSVHelper } from '../../utils/csv-helper';

/**
 * TDD Test Suite for Patients Component
 *
 * This test suite is written BEFORE implementation to follow TDD principles.
 * The Patients component provides:
 * - Patient list display with comprehensive information
 * - Search and filtering capabilities
 * - Patient details view with evaluation history
 * - Pagination and sorting
 * - Patient statistics and analytics
 * - Navigation to evaluations and results
 */
describe('PatientsComponent (TDD)', () => {
  let component: PatientsComponent;
  let fixture: ComponentFixture<PatientsComponent>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockFilterPersistence: jest.Mocked<FilterPersistenceService>;
  let mockDeduplicationService: jest.Mocked<PatientDeduplicationService>;
  let mockAIAssistantService: jest.Mocked<AIAssistantService>;
  let mockRouter: jest.Mocked<Router>;
  let globalConsoleErrorSpy: jest.SpyInstance;

  beforeAll(() => {
    globalConsoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  beforeEach(async () => {
    // Create mock services
    mockPatientService = {
      getPatientsSummary: jest.fn(),
      getPatient: jest.fn(),
    } as any;
    mockPatientService.getPatientsSummary.mockReturnValue(of([]));
    mockPatientService.getPatient.mockReturnValue(
      of(PatientFactory.create({ id: 'patient-001' }))
    );

    mockEvaluationService = {
      getPatientEvaluations: jest.fn(),
      getPatientResults: jest.fn(),
    } as any;
    mockEvaluationService.getPatientEvaluations.mockReturnValue(of([]));
    mockEvaluationService.getPatientResults.mockReturnValue(of([]));

    mockDialogService = {
      confirm: jest.fn(),
    } as any;
    mockDialogService.confirm.mockReturnValue(of(true));

    mockFilterPersistence = {
      loadFilters: jest.fn(),
      saveFilters: jest.fn(),
      clearFilters: jest.fn(),
    } as any;
    mockFilterPersistence.loadFilters.mockReturnValue(null);

    mockDeduplicationService = {
      enhancePatientList: jest.fn((patients) => patients),
      filterMasterRecordsOnly: jest.fn((patients) => patients.filter((p) => p.isMaster)),
      getStatistics: jest.fn(() => of({ masters: 0, duplicates: 0, links: 0 })),
      autoDetectAndLinkDuplicates: jest.fn(() => of({ duplicatesLinked: 0, mastersCreated: 0 })),
      clearAllLinks: jest.fn(),
    } as any;

    mockAIAssistantService = {} as any;

    mockRouter = {
      navigate: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [PatientsComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: PatientService, useValue: mockPatientService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: FilterPersistenceService, useValue: mockFilterPersistence },
        { provide: PatientDeduplicationService, useValue: mockDeduplicationService },
        { provide: AIAssistantService, useValue: mockAIAssistantService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientsComponent);
    component = fixture.componentInstance;
    PatientFactory.reset();
    EvaluationFactory.reset();
  });

  afterAll(() => {
    globalConsoleErrorSpy.mockRestore();
  });

  // ============================================================================
  // 1. Component Initialization (5 tests)
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should load patients on ngOnInit', () => {
      const loadPatientsSpy = jest.spyOn(component, 'loadPatients');
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.ngOnInit();

      expect(loadPatientsSpy).toHaveBeenCalled();
    });

    it('should initialize with empty patient list', () => {
      expect(component.patients).toEqual([]);
      expect(component.filteredPatients).toEqual([]);
    });

    it('should set loading state while fetching data', () => {
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.loadPatients();

      expect(component.loading).toBe(false); // Will be false after subscription
    });

    it('should handle initialization errors', () => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummary.mockReturnValue(
        throwError(() => error)
      );

      component.loadPatients();

      expect(component.loading).toBe(false);
      expect(component.error).toContain('Unable to access patient data');
    });
  });

  // ============================================================================
  // 2. Patient List Display (6 tests)
  // ============================================================================
  describe('Patient List Display', () => {
    beforeEach(() => {
      const mockPatients = PatientFactory.createSummaryList();
      mockPatientService.getPatientsSummary.mockReturnValue(of(mockPatients));
    });

    it('should display all patients in a table', () => {
      component.loadPatients();

      expect(component.patients.length).toBe(3);
      expect(component.filteredPatients.length).toBe(3);
    });

    it('should show patient MRN, name, DOB, age, gender, status', () => {
      component.loadPatients();

      const patient = component.patients[0];
      expect(patient).toHaveProperty('mrn');
      expect(patient).toHaveProperty('fullName');
      expect(patient).toHaveProperty('dateOfBirth');
      expect(patient).toHaveProperty('age');
      expect(patient).toHaveProperty('gender');
      expect(patient).toHaveProperty('status');
    });

    it('should display correct column headers', () => {
      expect(component.displayedColumns).toContain('mrn');
      expect(component.displayedColumns).toContain('fullName');
      expect(component.displayedColumns).toContain('dateOfBirth');
      expect(component.displayedColumns).toContain('age');
      expect(component.displayedColumns).toContain('gender');
      expect(component.displayedColumns).toContain('status');
      expect(component.displayedColumns).toContain('actions');
    });

    it('should handle empty patient list', () => {
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.loadPatients();

      expect(component.patients).toEqual([]);
      expect(component.filteredPatients).toEqual([]);
    });

    it('should show patient count', () => {
      component.loadPatients();

      expect(component.totalPatients).toBe(3);
    });

    it('should sort patients by name by default', () => {
      component.loadPatients();

      const firstPatient = component.filteredPatients[0];
      const secondPatient = component.filteredPatients[1];
      expect(firstPatient.fullName <= secondPatient.fullName).toBe(true);
    });
  });

  // ============================================================================
  // 3. Patient Search & Filtering (8 tests)
  // ============================================================================
  describe('Patient Search & Filtering', () => {
    beforeEach(() => {
      const mockPatients = [
        PatientFactory.createSummary({
          id: 'patient-001',
          fullName: 'John Doe',
          mrn: 'MRN12345',
          age: 45,
          gender: 'male',
          status: 'Active',
        }),
        PatientFactory.createSummary({
          id: 'patient-002',
          fullName: 'Jane Smith',
          mrn: 'MRN67890',
          age: 30,
          gender: 'female',
          status: 'Active',
        }),
        PatientFactory.createSummary({
          id: 'patient-003',
          fullName: 'Robert Johnson',
          mrn: 'MRN11111',
          age: 65,
          gender: 'male',
          status: 'Inactive',
        }),
      ];
      component.patients = mockPatients;
      component.filteredPatients = mockPatients;
    });

    it('should filter patients by name', () => {
      component.searchTerm = 'John';
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(2); // John Doe and Robert Johnson
      expect(component.filteredPatients.some(p => p.fullName.includes('John'))).toBe(true);
    });

    it('should filter patients by MRN', () => {
      component.searchTerm = 'MRN12345';
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].mrn).toBe('MRN12345');
    });

    it('should filter patients by age range', () => {
      component.filterForm.patchValue({
        ageFrom: 40,
        ageTo: 70,
      });
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(2); // John (45) and Robert (65)
      expect(component.filteredPatients.every(p => (p.age || 0) >= 40 && (p.age || 0) <= 70)).toBe(true);
    });

    it('should exclude patients above ageTo', () => {
      component.filterForm.patchValue({
        ageTo: 40,
      });
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].age).toBe(30);
    });

    it('should filter patients by gender', () => {
      component.filterForm.patchValue({
        gender: 'female',
      });
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].gender).toBe('female');
    });

    it('should filter patients by status (active/inactive)', () => {
      component.filterForm.patchValue({
        status: 'Inactive',
      });
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].status).toBe('Inactive');
    });

    it('should filter patients by date of birth search', () => {
      component.searchTerm = '1980-01';
      component.patients = [
        PatientFactory.createSummary({
          id: 'patient-001',
          fullName: 'DOB Match',
          dateOfBirth: '1980-01-15',
        }),
      ];
      component.filteredPatients = component.patients;

      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].fullName).toBe('DOB Match');
    });

    it('should return no matches for unmatched search term', () => {
      component.searchTerm = 'No Match';
      component.filterPatients();

      expect(component.filteredPatients.length).toBe(0);
    });

    it('should filter out patients below ageFrom when age is missing/zero', () => {
      component.filterForm.patchValue({ ageFrom: 1 });
      component.patients = [
        {
          id: 'patient-001',
          fullName: 'No Age',
          mrn: 'MRN000',
          age: 0,
          gender: 'male',
          status: 'Active',
          dateOfBirth: '2000-01-01',
        } as any,
      ];
      component.filteredPatients = component.patients;

      component.filterPatients();

      expect(component.filteredPatients.length).toBe(0);
    });

    it('should handle case-insensitive search', () => {
      component.searchTerm = 'JOHN';
      component.filterPatients();

      expect(component.filteredPatients.length).toBeGreaterThan(0);
      expect(component.filteredPatients.some(p => p.fullName.toLowerCase().includes('john'))).toBe(true);
    });

    it('should trigger search immediately (debounceTime is 0 for instant UX)', fakeAsync(() => {
      component.ngOnInit(); // Initialize to set up search subscription
      const filterSpy = jest.spyOn(component, 'filterPatients');

      component.searchTerm = 'J';
      component.onSearchChange();
      tick(); // With debounceTime(0), filter should trigger immediately

      expect(filterSpy).toHaveBeenCalled();
    }));

    it('should reset filters', () => {
      // Set up initial patients data
      const mockPatients = PatientFactory.createSummaryList();
      // Component uses patientsWithLinks internally (enhanced with MPI info)
      const patientsWithLinks = mockPatients.map(p => ({ ...p, isMaster: false, linkedPatients: [] }));
      component.patients = mockPatients;
      component.patientsWithLinks = patientsWithLinks;
      component.filteredPatients = [patientsWithLinks[0]]; // Start with filtered subset

      component.filterForm.patchValue({
        gender: 'female',
        status: 'Inactive',
        ageFrom: 30,
        ageTo: 50,
      });
      component.searchTerm = 'test';

      component.resetFilters();

      expect(component.filterForm.get('gender')?.value).toBeNull();
      expect(component.filterForm.get('status')?.value).toBeNull();
      expect(component.filterForm.get('ageFrom')?.value).toBeNull();
      expect(component.filterForm.get('ageTo')?.value).toBeNull();
      expect(component.searchTerm).toBe('');
      // After reset, filteredPatients should be restored to full patientsWithLinks list
      expect(component.filteredPatients.length).toBe(component.patientsWithLinks.length);
    });
  });

  // ============================================================================
  // 4. Patient Details View (7 tests)
  // ============================================================================
  describe('Patient Details View', () => {
    const mockPatient = PatientFactory.create({
      id: 'patient-001',
    });

    it('should open details panel when patient clicked', () => {
      const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientEvaluations.mockReturnValue(of([]));

      component.selectPatient(patientSummary);

      expect(component.selectedPatient).toEqual(patientSummary);
      expect(component.showDetails).toBe(true);
    });

    it('should load patient details from FHIR server', () => {
      const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientEvaluations.mockReturnValue(of([]));

      component.selectPatient(patientSummary);

      expect(mockPatientService.getPatient).toHaveBeenCalledWith('patient-001');
      expect(component.patientDetails).toEqual(mockPatient);
    });

    it('should display patient demographics', () => {
      const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientEvaluations.mockReturnValue(of([]));

      component.selectPatient(patientSummary);

      expect(component.patientDetails).toBeDefined();
      expect(component.patientDetails?.birthDate).toBeDefined();
      expect(component.patientDetails?.gender).toBeDefined();
    });

    it('should display patient identifiers', () => {
      const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientEvaluations.mockReturnValue(of([]));

      component.selectPatient(patientSummary);

      expect(component.patientDetails?.identifier).toBeDefined();
      expect(component.patientDetails?.identifier?.length).toBeGreaterThan(0);
    });

    it('should show patient contact information', () => {
      const patientWithContact = {
        ...PatientFactory.create({ id: 'patient-001' }),
        telecom: [
          { system: 'phone', value: '555-1234', use: 'home' },
          { system: 'email', value: 'patient@example.com', use: 'home' },
        ],
      };
      component.patientDetails = patientWithContact;

      expect(component.patientDetails?.telecom).toBeDefined();
      expect(component.patientDetails?.telecom?.length).toBeGreaterThan(0);
    });

    it('should display patient addresses', () => {
      const patientWithAddress = {
        ...PatientFactory.create({ id: 'patient-001' }),
        address: [
          {
            use: 'home',
            line: ['123 Main St'],
            city: 'Springfield',
            state: 'IL',
            postalCode: '62701',
          },
        ],
      };
      component.patientDetails = patientWithAddress;

      expect(component.patientDetails?.address).toBeDefined();
      expect(component.patientDetails?.address?.length).toBeGreaterThan(0);
    });

    it('should close details panel', () => {
      component.selectedPatient = PatientFactory.createSummary();
      component.showDetails = true;

      component.closeDetails();

      expect(component.selectedPatient).toBeNull();
      expect(component.showDetails).toBe(false);
      expect(component.patientDetails).toBeNull();
    });
  });

  // ============================================================================
  // 5. Patient Evaluation History (6 tests)
  // ============================================================================
  describe('Patient Evaluation History', () => {
    const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });
    const mockPatient = PatientFactory.create({ id: 'patient-001' });

    it('should load evaluation history for selected patient', () => {
      const mockEvaluations = EvaluationFactory.createMany(5);
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockEvaluations));

      component.selectPatient(patientSummary);

      expect(mockEvaluationService.getPatientResults).toHaveBeenCalledWith('patient-001');
      expect(component.patientEvaluations.length).toBe(5);
    });

    it('should display evaluations sorted by date (newest first)', () => {
      // Use QualityMeasureResult objects (not CqlEvaluation) since patientEvaluations is QualityMeasureResult[]
      const oldEvaluation = {
        ...EvaluationFactory.createCompliantResult(),
        calculationDate: '2024-01-01T00:00:00Z',
      };
      const newEvaluation = {
        ...EvaluationFactory.createCompliantResult(),
        calculationDate: '2024-12-01T00:00:00Z',
      };
      component.patientEvaluations = [oldEvaluation, newEvaluation].sort((a, b) => {
        const dateA = new Date(a.calculationDate).getTime();
        const dateB = new Date(b.calculationDate).getTime();
        return dateB - dateA;
      });

      const firstDate = new Date(component.patientEvaluations[0].calculationDate || '');
      const secondDate = new Date(component.patientEvaluations[1].calculationDate || '');
      expect(firstDate.getTime()).toBeGreaterThanOrEqual(secondDate.getTime());
    });

    it('should show evaluation measure, date, and outcome', () => {
      const mockEvaluation = EvaluationFactory.createCompliantResult();
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientResults.mockReturnValue(of([mockEvaluation]));

      component.selectPatient(patientSummary);

      const evaluation = component.patientEvaluations[0];
      expect(evaluation).toHaveProperty('measureName');
      expect(evaluation).toHaveProperty('calculationDate');
      expect(evaluation).toHaveProperty('numeratorCompliant');
      expect(evaluation).toHaveProperty('denominatorEligible');
    });

    it('should calculate compliance rate for patient', () => {
      const mockEvaluations = [
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createCompliantResult(),
        EvaluationFactory.createNonCompliantResult(),
      ];
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockEvaluations));

      component.selectPatient(patientSummary);

      const complianceRate = component.calculatePatientComplianceRate();
      expect(complianceRate).toBeCloseTo(66.67, 1); // 2 out of 3
    });

    it('should handle patient with no evaluations', () => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockEvaluationService.getPatientResults.mockReturnValue(of([]));

      component.selectPatient(patientSummary);

      expect(component.patientEvaluations).toEqual([]);
      expect(component.calculatePatientComplianceRate()).toBe(0);
    });

    it('should navigate to evaluation details', () => {
      const evaluationId = 'eval-001';

      component.viewEvaluationDetails(evaluationId);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results', evaluationId]);
    });
  });

  // ============================================================================
  // 6. Pagination (5 tests)
  // ============================================================================
  describe('Pagination', () => {
    beforeEach(() => {
      const mockPatients = PatientFactory.createMany(50).map(() =>
        PatientFactory.createSummary()
      );
      component.patients = mockPatients;
      component.filteredPatients = mockPatients;
    });

    it('should paginate patient list', () => {
      component.pageSize = 10;
      component.currentPage = 0;

      const paginatedPatients = component.getPaginatedPatients();

      expect(paginatedPatients.length).toBe(10);
    });

    it('should navigate to next page', () => {
      component.pageSize = 10;
      component.currentPage = 0;

      component.nextPage();

      expect(component.currentPage).toBe(1);
    });

    it('should navigate to previous page', () => {
      component.pageSize = 10;
      component.currentPage = 2;

      component.previousPage();

      expect(component.currentPage).toBe(1);
    });

    it('should show correct page numbers', () => {
      component.pageSize = 10;

      const totalPages = component.getTotalPages();

      expect(totalPages).toBe(5); // 50 patients / 10 per page
    });

    it('should return zero pages when page size is zero', () => {
      component.pageSize = 0;
      component.filteredPatients = PatientFactory.createMany(5).map(() =>
        PatientFactory.createSummary()
      );

      expect(component.getTotalPages()).toBe(0);
    });

    it('should update page size', () => {
      component.pageSize = 10;
      component.currentPage = 2;

      component.setPageSize(20);

      expect(component.pageSize).toBe(20);
      expect(component.currentPage).toBe(0); // Reset to first page
    });
  });

  // ============================================================================
  // 7. Sorting (5 tests)
  // ============================================================================
  describe('Sorting', () => {
    beforeEach(() => {
      const mockPatients = [
        PatientFactory.createSummary({ fullName: 'Charlie Brown', age: 50, dateOfBirth: '1974-01-01' }),
        PatientFactory.createSummary({ fullName: 'Alice Johnson', age: 30, dateOfBirth: '1994-06-15' }),
        PatientFactory.createSummary({ fullName: 'Bob Smith', age: 65, dateOfBirth: '1959-12-25' }),
      ];
      component.patients = mockPatients;
      component.filteredPatients = mockPatients;
    });

    it('should sort by name (ascending/descending)', () => {
      component.sortColumn = 'fullName';
      component.sortDirection = 'asc';
      component.sortPatients();

      expect(component.filteredPatients[0].fullName).toBe('Alice Johnson');
      expect(component.filteredPatients[2].fullName).toBe('Charlie Brown');

      component.sortDirection = 'desc';
      component.sortPatients();

      expect(component.filteredPatients[0].fullName).toBe('Charlie Brown');
      expect(component.filteredPatients[2].fullName).toBe('Alice Johnson');
    });

    it('should sort by age (ascending/descending)', () => {
      component.sortColumn = 'age';
      component.sortDirection = 'asc';
      component.sortPatients();

      expect(component.filteredPatients[0].age).toBe(30);
      expect(component.filteredPatients[2].age).toBe(65);

      component.sortDirection = 'desc';
      component.sortPatients();

      expect(component.filteredPatients[0].age).toBe(65);
      expect(component.filteredPatients[2].age).toBe(30);
    });

    it('should sort by DOB (ascending/descending)', () => {
      component.sortColumn = 'dateOfBirth';
      component.sortDirection = 'asc';
      component.sortPatients();

      expect(component.filteredPatients[0].dateOfBirth).toBe('1959-12-25');
      expect(component.filteredPatients[2].dateOfBirth).toBe('1994-06-15');

      component.sortDirection = 'desc';
      component.sortPatients();

      expect(component.filteredPatients[0].dateOfBirth).toBe('1994-06-15');
      expect(component.filteredPatients[2].dateOfBirth).toBe('1959-12-25');
    });

    it('should toggle sort direction', () => {
      component.sortColumn = 'fullName';
      component.sortDirection = 'asc';

      component.toggleSort('fullName');

      expect(component.sortDirection).toBe('desc');

      component.toggleSort('fullName');

      expect(component.sortDirection).toBe('asc');
    });

    it('should indicate current sort column', () => {
      component.sortColumn = 'age';

      expect(component.isSortedBy('age')).toBe(true);
      expect(component.isSortedBy('fullName')).toBe(false);
    });
  });

  // ============================================================================
  // 8. Actions & Navigation (4 tests)
  // ============================================================================
  describe('Actions & Navigation', () => {
    const patientSummary = PatientFactory.createSummary({ id: 'patient-001' });

    it('should have "New Evaluation" button for patient', () => {
      expect(component.canEvaluatePatient).toBeDefined();
      expect(component.canEvaluatePatient()).toBe(true);
    });

    it('should navigate to evaluation page with patient pre-selected', () => {
      component.newEvaluationForPatient(patientSummary);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/evaluations'], {
        queryParams: { patientId: 'patient-001' },
      });
    });

    it('should have "View Results" button', () => {
      expect(component.viewPatientResults).toBeDefined();
    });

    it('should navigate to results filtered by patient', () => {
      component.viewPatientResults(patientSummary);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results'], {
        queryParams: { patientId: 'patient-001' },
      });
    });
  });

  // ============================================================================
  // 9. Patient Statistics (4 tests)
  // ============================================================================
  describe('Patient Statistics', () => {
    beforeEach(() => {
      const mockPatients = [
        PatientFactory.createSummary({ age: 30, gender: 'male', status: 'Active' }),
        PatientFactory.createSummary({ age: 50, gender: 'female', status: 'Active' }),
        PatientFactory.createSummary({ age: 65, gender: 'male', status: 'Inactive' }),
        PatientFactory.createSummary({ age: 45, gender: 'other', status: 'Active' }),
      ];
      component.patients = mockPatients;
      component.calculateStatistics();
    });

    it('should calculate average patient age', () => {
      expect(component.statistics.averageAge).toBeCloseTo(47.5, 1); // (30+50+65+45)/4
    });

    it('should count active vs inactive patients', () => {
      expect(component.statistics.activePatients).toBe(3);
      expect(component.statistics.inactivePatients).toBe(1);
    });

    it('should show gender distribution', () => {
      expect(component.statistics.genderDistribution.male).toBe(2);
      expect(component.statistics.genderDistribution.female).toBe(1);
      expect(component.statistics.genderDistribution.other).toBe(1);
    });

    it('should display total patient count', () => {
      expect(component.statistics.totalPatients).toBe(4);
    });

    it('should handle empty statistics safely', () => {
      component.patients = [];
      component.calculateStatistics();

      expect(component.statistics.totalPatients).toBe(0);
      expect(component.statistics.averageAge).toBe(0);
      expect(component.statistics.activePatients).toBe(0);
      expect(component.statistics.inactivePatients).toBe(0);
    });
  });

  // ============================================================================
  // 10. Error Handling (3 tests)
  // ============================================================================
  describe('Error Handling', () => {
    let consoleErrorSpy: jest.SpyInstance;

    beforeEach(() => {
      consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
      consoleErrorSpy.mockRestore();
    });

    it('should display error message on load failure', () => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummary.mockReturnValue(
        throwError(() => error)
      );

      component.loadPatients();

      expect(component.error).toContain('Unable to access patient data');
      expect(component.loading).toBe(false);
    });

    it('should retry on error', () => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummary.mockReturnValue(
        throwError(() => error)
      );

      component.loadPatients();
      expect(component.error).toBeTruthy();

      // Retry
      const mockPatients = PatientFactory.createSummaryList();
      mockPatientService.getPatientsSummary.mockReturnValue(of(mockPatients));
      component.retryLoad();

      expect(component.error).toBeNull();
      expect(component.patients.length).toBe(3);
    });

    it('should show empty state with helpful message', () => {
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.loadPatients();

      expect(component.isEmpty()).toBe(true);
      expect(component.getEmptyMessage()).toContain('No patients found');
    });
  });

  // ============================================================================
  // Additional Helper Tests
  // ============================================================================
  describe('Helper Methods', () => {
    it('should format date correctly', () => {
      const dateString = '1980-01-15';
      const formatted = component.formatDate(dateString);

      // Format is MM/DD/YYYY so it should contain these parts
      expect(formatted).toMatch(/\d{2}\/\d{2}\/1980/);
      expect(formatted).toContain('1980');
    });

    it('should return N/A for empty date', () => {
      expect(component.formatDate(undefined)).toBe('N/A');
    });

    it('should calculate age from birthdate', () => {
      const birthDate = '1980-01-15';
      const age = component.calculateAge(birthDate);

      expect(age).toBeGreaterThan(40);
      expect(age).toBeLessThan(50);
    });

    it('should adjust age when birthday has not occurred yet this year', () => {
      jest.useFakeTimers();
      jest.setSystemTime(new Date('2024-01-10T00:00:00Z'));

      const age = component.calculateAge('2000-12-20');
      expect(age).toBe(23);

      jest.useRealTimers();
    });

    it('should get status badge class for active', () => {
      const badgeClass = component.getStatusBadgeClass('Active');

      expect(badgeClass).toBe('badge-success');
    });

    it('should get status badge class for inactive', () => {
      const badgeClass = component.getStatusBadgeClass('Inactive');

      expect(badgeClass).toBe('badge-secondary');
    });

    it('should format phone number', () => {
      const phone = '5551234567';
      const formatted = component.formatPhoneNumber(phone);

      expect(formatted).toContain('555');
    });

    it('should return empty string for missing phone and preserve non-10 digits', () => {
      expect(component.formatPhoneNumber('')).toBe('');
      expect(component.formatPhoneNumber('555123')).toBe('555123');
    });

    it('should format address', () => {
      const address = {
        line: ['123 Main St'],
        city: 'Springfield',
        state: 'IL',
        postalCode: '62701',
      };
      const formatted = component.formatAddress(address);

      expect(formatted).toContain('123 Main St');
      expect(formatted).toContain('Springfield');
      expect(formatted).toContain('IL');
      expect(formatted).toContain('62701');
    });

    it('should get outcome text for evaluation', () => {
      const compliantResult = EvaluationFactory.createCompliantResult();
      const outcomeText = component.getOutcomeText(compliantResult);

      expect(outcomeText).toBe('Compliant');
    });

    it('should get outcome badge class for evaluation', () => {
      const compliantResult = EvaluationFactory.createCompliantResult();
      const badgeClass = component.getOutcomeBadgeClass(compliantResult);

      expect(badgeClass).toBe('badge-success');
    });

    it('should return non-compliant outcome text and badge class', () => {
      const nonCompliant = {
        ...EvaluationFactory.createNonCompliantResult(),
        denominatorEligible: true,
        numeratorCompliant: false,
      };

      expect(component.getOutcomeText(nonCompliant)).toBe('Non-Compliant');
      expect(component.getOutcomeBadgeClass(nonCompliant)).toBe('badge-warning');
    });

    it('should return not eligible outcome text and badge class', () => {
      const notEligible = {
        ...EvaluationFactory.createNonCompliantResult(),
        denominatorEligible: false,
        numeratorCompliant: false,
      };

      expect(component.getOutcomeText(notEligible)).toBe('Not Eligible');
      expect(component.getOutcomeBadgeClass(notEligible)).toBe('badge-info');
    });
  });

  describe('Filtering, sorting, and pagination', () => {
    it('filters patients by fuzzy name match', () => {
      const patients = [
        PatientFactory.createSummary({
          id: 'patient-1',
          fullName: 'John Doe',
          mrn: 'MRN1',
          age: 40,
          gender: 'male',
          status: 'Active',
        }),
      ] as any;

      component.patientsWithLinks = patients;
      component.filteredPatients = patients;
      component.searchTerm = 'Jahn';

      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
    });

    it('matches fuzzy search on normalized text', () => {
      expect((component as any).fuzzyMatch('John-Doe', 'johndoe')).toBe(true);
    });

    it('returns false for short fuzzy queries or empty text', () => {
      expect((component as any).fuzzyMatch('', 'Jon')).toBe(false);
      expect((component as any).fuzzyMatch('John Doe', '')).toBe(false);
      expect((component as any).fuzzyMatch('John Doe', 'xy')).toBe(false);
    });

    it('filters patients by gender and age range', () => {
      const patients = [
        PatientFactory.createSummary({
          id: 'patient-1',
          fullName: 'Jane Doe',
          mrn: 'MRN1',
          age: 30,
          gender: 'female',
          status: 'Active',
        }),
        PatientFactory.createSummary({
          id: 'patient-2',
          fullName: 'John Doe',
          mrn: 'MRN2',
          age: 60,
          gender: 'male',
          status: 'Active',
        }),
      ] as any;

      component.patientsWithLinks = patients;
      component.filteredPatients = patients;
      component.searchTerm = '';
      component.filterForm.patchValue({ gender: 'female', ageFrom: 20, ageTo: 40 });

      component.filterPatients();

      expect(component.filteredPatients.length).toBe(1);
      expect(component.filteredPatients[0].gender).toBe('female');
    });

    it('toggles sort direction and sorts by age', () => {
      component.filteredPatients = [
        PatientFactory.createSummary({ id: 'p1', age: 50 }),
        PatientFactory.createSummary({ id: 'p2', age: 30 }),
      ] as any;

      component.toggleSort('age');
      expect(component.sortColumn).toBe('age');
      expect(component.filteredPatients[0].age).toBe(30);

      component.toggleSort('age');
      expect(component.sortDirection).toBe('desc');
      expect(component.filteredPatients[0].age).toBe(50);
    });

    it('paginates patients', () => {
      component.filteredPatients = PatientFactory.createSummaryList() as any;
      component.pageSize = 2;
      component.currentPage = 0;

      expect(component.getPaginatedPatients().length).toBe(2);
      component.nextPage();
      expect(component.currentPage).toBe(1);
      component.previousPage();
      expect(component.currentPage).toBe(0);
    });
  });

  describe('Selection and exports', () => {
    it('exports selected patients to CSV', () => {
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation();
      const patient = PatientFactory.createSummary({ id: 'patient-1' }) as any;

      component.dataSource.data = [patient];
      component.selection.select(patient);

      component.exportSelectedToCSV();

      expect(arraySpy).toHaveBeenCalled();
      expect(downloadSpy).toHaveBeenCalled();
      arraySpy.mockRestore();
      downloadSpy.mockRestore();
    });

    it('does not export when nothing selected', () => {
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');
      component.exportSelectedToCSV();
      expect(arraySpy).not.toHaveBeenCalled();
      arraySpy.mockRestore();
    });

    it('toggles master selection and checkbox labels', () => {
      const patients = [
        PatientFactory.createSummary({ id: 'patient-1' }),
        PatientFactory.createSummary({ id: 'patient-2' }),
      ] as any;
      component.dataSource.data = patients;

      component.masterToggle();
      expect(component.isAllSelected()).toBe(true);
      expect(component.checkboxLabel()).toBe('deselect all');
      expect(component.checkboxLabel(patients[0])).toBe('deselect row patient-1');

      component.masterToggle();
      expect(component.selection.selected.length).toBe(0);
      expect(component.checkboxLabel()).toBe('select all');
    });
  });

  describe('Delete flows', () => {
    it('does not delete when confirmation is cancelled', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      const patient = PatientFactory.createSummary({ id: 'patient-1' }) as any;
      component.dataSource.data = [patient];
      component.selection.select(patient);

      component.deleteSelected();

      expect(mockPatientService.deletePatient).toBeUndefined();
    });

    it('performs delete when service is available', () => {
      const deleteSpy = jest.fn().mockReturnValue(of({}));
      (mockPatientService as any).deletePatient = deleteSpy;
      const patient = PatientFactory.createSummary({ id: 'patient-1' }) as any;

      component.patients = [patient];
      component.filteredPatients = [patient];
      component.dataSource.data = [patient];
      component.selection.select(patient);

      component.deleteSelected();

      expect(deleteSpy).toHaveBeenCalledWith('patient-1');
      expect(component.patients.length).toBe(0);
    });

    it('sets error message when all deletes fail', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      const deleteSpy = jest.fn().mockReturnValue(throwError(() => new Error('fail')));
      (mockPatientService as any).deletePatient = deleteSpy;
      const patient = PatientFactory.createSummary({ id: 'patient-1' }) as any;

      component.patients = [patient];
      component.filteredPatients = [patient];
      component.dataSource.data = [patient];
      component.selection.select(patient);

      component.deleteSelected();

      expect(component.error).toContain('Unable to delete selected patients');
      expect(component.bulkOperationInProgress).toBe(false);
    });
  });

  describe('Details loading and helpers', () => {
    it('handles missing patient details service', () => {
      (mockPatientService as any).getPatient = undefined;
      (component as any).loadPatientDetails('patient-1');
      expect(component.patientDetails).toBeNull();
    });

    it('handles non-observable patient details response', () => {
      (mockPatientService as any).getPatient = jest.fn().mockReturnValue({});
      (component as any).loadPatientDetails('patient-1');
      expect(component.patientDetails).toBeNull();
    });

    it('handles missing evaluation results service', () => {
      (mockEvaluationService as any).getPatientResults = undefined;
      (component as any).loadPatientEvaluations('patient-1');
      expect(component.patientEvaluations).toEqual([]);
    });

    it('handles evaluation results errors', () => {
      (mockEvaluationService as any).getPatientResults = jest.fn().mockReturnValue(
        throwError(() => new Error('boom'))
      );
      (component as any).loadPatientEvaluations('patient-1');
      expect(component.patientEvaluations).toEqual([]);
    });

    it('sorts evaluations using evaluationDate fallback', () => {
      const evaluations = [
        {
          ...EvaluationFactory.createCompliantResult(),
          calculationDate: undefined,
          evaluationDate: '2023-01-01T00:00:00Z',
        },
        {
          ...EvaluationFactory.createCompliantResult(),
          calculationDate: undefined,
          evaluationDate: '2024-01-01T00:00:00Z',
        },
      ];
      (mockEvaluationService as any).getPatientResults = jest.fn().mockReturnValue(of(evaluations));
      (component as any).loadPatientEvaluations('patient-1');

      expect(component.patientEvaluations[0].evaluationDate).toBe('2024-01-01T00:00:00Z');
      expect(component.patientEvaluations[1].evaluationDate).toBe('2023-01-01T00:00:00Z');
    });

    it('formats MRN authority', () => {
      expect(component.formatMRNAuthority('http://hospital.example.org/patients')).toBe('hospital.example.org');
      expect(component.formatMRNAuthority('not-a-url')).toBe('not-a-url');
    });
  });

  describe('Filters and deduplication actions', () => {
    it('resets filters and clears persistence', () => {
      component.searchTerm = 'abc';
      component.filterForm.patchValue({ gender: 'male' });
      component.patientsWithLinks = PatientFactory.createSummaryList() as any;

      component.resetFilters();

      expect(mockFilterPersistence.clearFilters).toHaveBeenCalledWith('patients');
      expect(component.searchTerm).toBe('');
    });

    it('loads persisted filters and master-record flag', () => {
      mockFilterPersistence.loadFilters.mockReturnValue({
        searchTerm: 'Jane',
        gender: 'female',
        status: 'Active',
        ageFrom: 20,
        ageTo: 50,
        showMasterRecordsOnly: true,
      });

      component.ngOnInit();

      expect(component.searchTerm).toBe('Jane');
      expect(component.showMasterRecordsOnly).toBe(true);
      expect(component.filterForm.get('gender')?.value).toBe('female');
      expect(component.filterForm.get('status')?.value).toBe('Active');
      expect(component.filterForm.get('ageFrom')?.value).toBe(20);
      expect(component.filterForm.get('ageTo')?.value).toBe(50);
    });

    it('applies master records filter when enabled', () => {
      const patients = [
        { ...PatientFactory.createSummary({ id: 'patient-1' }), isMaster: true },
        { ...PatientFactory.createSummary({ id: 'patient-2' }), isMaster: false },
      ] as any;
      component.patientsWithLinks = patients;
      component.filteredPatients = patients;
      component.showMasterRecordsOnly = true;

      component.toggleMasterRecordsOnly();

      expect(mockDeduplicationService.filterMasterRecordsOnly).toHaveBeenCalledWith(patients);
      expect(component.filteredPatients.every((patient) => patient.isMaster)).toBe(true);
    });

    it('auto-detects duplicates and clears message', () => {
      jest.useFakeTimers();
      mockDeduplicationService.autoDetectAndLinkDuplicates.mockReturnValue(
        of({ duplicatesLinked: 2, mastersCreated: 1 })
      );
      component.patients = PatientFactory.createSummaryList() as any;

      component.autoDetectDuplicates();

      expect(component.duplicateDetectionResult).toContain('Success');
      jest.advanceTimersByTime(10000);
      expect(component.duplicateDetectionResult).toBeNull();
      jest.useRealTimers();
    });

    it('handles duplicate detection errors and clears message', () => {
      jest.useFakeTimers();
      mockDeduplicationService.autoDetectAndLinkDuplicates.mockReturnValue(
        throwError(() => new Error('boom'))
      );
      component.patients = PatientFactory.createSummaryList() as any;

      component.autoDetectDuplicates();

      expect(component.detectingDuplicates).toBe(false);
      expect(component.duplicateDetectionResult).toBe('Error detecting duplicates');
      jest.advanceTimersByTime(10000);
      expect(component.duplicateDetectionResult).toBeNull();
      jest.useRealTimers();
    });

    it('clears duplicate links and hides message', () => {
      jest.useFakeTimers();
      component.patients = PatientFactory.createSummaryList() as any;

      component.clearDuplicateLinks();

      expect(mockDeduplicationService.clearAllLinks).toHaveBeenCalled();
      expect(component.duplicateDetectionResult).toBe('All duplicate links cleared');
      jest.advanceTimersByTime(3000);
      expect(component.duplicateDetectionResult).toBeNull();
      jest.useRealTimers();
    });
  });
});
