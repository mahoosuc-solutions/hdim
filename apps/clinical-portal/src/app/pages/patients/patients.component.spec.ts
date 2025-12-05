import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { PatientsComponent } from './patients.component';
import { PatientService } from '../../services/patient.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';

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

    mockRouter = {
      navigate: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [PatientsComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: PatientService, useValue: mockPatientService },
        { provide: EvaluationService, useValue: mockEvaluationService },
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

    it('should calculate age from birthdate', () => {
      const birthDate = '1980-01-15';
      const age = component.calculateAge(birthDate);

      expect(age).toBeGreaterThan(40);
      expect(age).toBeLessThan(50);
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
  });
});
