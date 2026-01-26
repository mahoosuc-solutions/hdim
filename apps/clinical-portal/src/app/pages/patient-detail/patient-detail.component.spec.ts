import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PatientDetailComponent } from './patient-detail.component';
import { PatientService } from '../../services/patient.service';
import { FhirClinicalService, PatientClinicalData } from '../../services/fhir-clinical.service';
import { EvaluationService } from '../../services/evaluation.service';
import { Patient } from '../../models/patient.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { LoggerService } from '../services/logger.service';
import { createMockLoggerService } from '../testing/mocks';

describe('PatientDetailComponent (TDD)', () => {
  let component: PatientDetailComponent;
  let fixture: ComponentFixture<PatientDetailComponent>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockFhirClinicalService: jest.Mocked<FhirClinicalService>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockRouter: jest.Mocked<Router>;
  let mockActivatedRoute: Partial<ActivatedRoute>;

  const mockPatient: Patient = {
    id: 'patient-123',
    identifier: [
      {
        use: 'official',
        type: {
          coding: [{ system: 'http://terminology.hl7.org/CodeSystem/v2-0203', code: 'MR' }],
          text: 'Medical Record Number',
        },
        system: 'http://hospital.example.org/patients',
        value: 'MRN12345',
      },
    ],
    name: [
      {
        use: 'official',
        family: 'Doe',
        given: ['John', 'Robert'],
        text: 'John Robert Doe',
      },
    ],
    gender: 'male',
    birthDate: '1980-05-15',
    active: true,
  };

  const mockClinicalData: PatientClinicalData = {
    observations: [
      {
        resourceType: 'Observation',
        id: 'obs-1',
        status: 'final',
        code: {
          coding: [{ system: 'http://loinc.org', code: '8480-6', display: 'Systolic BP' }],
          text: 'Systolic BP',
        },
        subject: { reference: 'Patient/patient-123' },
        effectiveDateTime: '2024-01-15',
        valueQuantity: { value: 120, unit: 'mmHg' },
      },
    ],
    conditions: [
      {
        resourceType: 'Condition',
        id: 'cond-1',
        clinicalStatus: {
          coding: [{ system: 'http://terminology.hl7.org/CodeSystem/condition-clinical', code: 'active', display: 'Active' }],
        },
        code: {
          coding: [{ system: 'http://snomed.info/sct', code: '38341003', display: 'Hypertension' }],
          text: 'Hypertension',
        },
        subject: { reference: 'Patient/patient-123' },
        onsetDateTime: '2020-03-10',
      },
    ],
    procedures: [
      {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [{ system: 'http://snomed.info/sct', code: '252160004', display: 'Office Visit' }],
          text: 'Office Visit',
        },
        subject: { reference: 'Patient/patient-123' },
        performedDateTime: '2024-01-15',
      },
    ],
  };

  const mockQualityResults: QualityMeasureResult[] = [
    {
      id: 'result-1',
      tenantId: 'TENANT001',
      measureId: 'HEDIS_CDC',
      measureName: 'Comprehensive Diabetes Care',
      measureCategory: 'HEDIS',
      measureYear: 2024,
      patientId: 'patient-123',
      numeratorCompliant: true,
      denominatorEligible: true,
      complianceRate: 95,
      score: 95,
      calculationDate: '2024-01-15T10:00:00Z',
      createdAt: '2024-01-15T10:00:00Z',
      createdBy: 'system',
      version: 1,
    },
    {
      id: 'result-2',
      tenantId: 'TENANT001',
      measureId: 'HEDIS_CBP',
      measureName: 'Controlling High Blood Pressure',
      measureCategory: 'HEDIS',
      measureYear: 2024,
      patientId: 'patient-123',
      numeratorCompliant: false,
      denominatorEligible: true,
      complianceRate: 45,
      score: 45,
      calculationDate: '2024-01-14T09:00:00Z',
      createdAt: '2024-01-14T09:00:00Z',
      createdBy: 'system',
      version: 1,
    },
  ];

  beforeEach(async () => {
    // Mock services
    mockPatientService = {
      getPatient: jest.fn(),
      formatPatientName: jest.fn(),
    } as unknown as jest.Mocked<PatientService>;

    mockFhirClinicalService = {
      getPatientClinicalData: jest.fn(),
      formatObservationValue: jest.fn(),
      getObservationCodeDisplay: jest.fn(),
      getConditionCodeDisplay: jest.fn(),
      getConditionStatus: jest.fn(),
      getProcedureCodeDisplay: jest.fn(),
    } as unknown as jest.Mocked<FhirClinicalService>;

    mockEvaluationService = {
      getPatientResults: jest.fn(),
    } as unknown as jest.Mocked<EvaluationService>;

    mockRouter = {
      navigate: jest.fn(),
    } as unknown as jest.Mocked<Router>;

    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: jest.fn((key: string) => (key === 'id' ? 'patient-123' : null)),
        },
      } as unknown as ActivatedRoute['snapshot'],
    };

    await TestBed.configureTestingModule({
      imports: [PatientDetailComponent, NoopAnimationsModule],
      providers: [{ provide: LoggerService, useValue: createMockLoggerService() },
        
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PatientService, useValue: mockPatientService },
        { provide: FhirClinicalService, useValue: mockFhirClinicalService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should extract patient ID from route on init', () => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockFhirClinicalService.getPatientClinicalData.mockReturnValue(of(mockClinicalData));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockQualityResults));

      component.ngOnInit();

      expect(component.patientId).toBe('patient-123');
    });

    it('should set error and stop loading if no patient ID provided', () => {
      const mockParamMap = mockActivatedRoute.snapshot?.paramMap as unknown as { get: jest.Mock };
      mockParamMap.get = jest.fn().mockReturnValue(null);

      component.ngOnInit();

      expect(component.error).toBe('No patient ID provided');
      expect(component.loading).toBe(false);
      expect(mockPatientService.getPatient).not.toHaveBeenCalled();
    });

    it('should load patient data on init when patient ID exists', () => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockFhirClinicalService.getPatientClinicalData.mockReturnValue(of(mockClinicalData));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockQualityResults));

      component.ngOnInit();

      expect(mockPatientService.getPatient).toHaveBeenCalledWith('patient-123');
    });
  });

  describe('Patient Data Loading', () => {
    beforeEach(() => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockFhirClinicalService.getPatientClinicalData.mockReturnValue(of(mockClinicalData));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockQualityResults));
    });

    it('should load patient demographics successfully', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        expect(component.patient).toEqual(mockPatient);
        done();
      }, 0);
    });

    it('should load clinical data after patient demographics', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        expect(mockFhirClinicalService.getPatientClinicalData).toHaveBeenCalledWith('patient-123');
        expect(component.clinicalData).toEqual(mockClinicalData);
        done();
      }, 0);
    });

    it('should load quality results after patient demographics', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        expect(mockEvaluationService.getPatientResults).toHaveBeenCalledWith('patient-123');
        expect(component.qualityResults).toEqual(mockQualityResults);
        done();
      }, 0);
    });

    it('should set loading to false after all data is loaded', (done) => {
      expect(component.loading).toBe(true);

      component.ngOnInit();

      setTimeout(() => {
        expect(component.loading).toBe(false);
        done();
      }, 0);
    });
  });

  describe('Error Handling', () => {
    it('should handle patient loading error', (done) => {
      mockPatientService.getPatient.mockReturnValue(throwError(() => new Error('API Error')));

      component.ngOnInit();

      setTimeout(() => {
        expect(component.error).toBe('Failed to load patient information');
        expect(component.loading).toBe(false);
        expect(component.patient).toBeNull();
        done();
      }, 0);
    });

    it('should continue loading even if clinical data fails', (done) => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockFhirClinicalService.getPatientClinicalData.mockReturnValue(throwError(() => new Error('Clinical API Error')));
      mockEvaluationService.getPatientResults.mockReturnValue(of(mockQualityResults));

      component.ngOnInit();

      setTimeout(() => {
        expect(component.patient).toEqual(mockPatient);
        expect(component.clinicalData).toBeNull();
        expect(component.loading).toBe(false);
        done();
      }, 0);
    });

    it('should continue loading even if quality results fail', (done) => {
      mockPatientService.getPatient.mockReturnValue(of(mockPatient));
      mockFhirClinicalService.getPatientClinicalData.mockReturnValue(of(mockClinicalData));
      mockEvaluationService.getPatientResults.mockReturnValue(throwError(() => new Error('Results API Error')));

      component.ngOnInit();

      setTimeout(() => {
        expect(component.patient).toEqual(mockPatient);
        expect(component.qualityResults).toEqual([]);
        expect(component.loading).toBe(false);
        done();
      }, 0);
    });
  });

  describe('Patient Name Formatting', () => {
    beforeEach(() => {
      component.patient = mockPatient;
    });

    it('should format patient name using service', () => {
      mockPatientService.formatPatientName.mockReturnValue('John Robert Doe');

      const name = component.getPatientName();

      expect(name).toBe('John Robert Doe');
      expect(mockPatientService.formatPatientName).toHaveBeenCalledWith(mockPatient);
    });

    it('should return "Unknown" if no patient loaded', () => {
      component.patient = null;

      const name = component.getPatientName();

      expect(name).toBe('Unknown');
    });
  });

  describe('Patient Age Calculation', () => {
    it('should calculate age correctly from birthDate', () => {
      const today = new Date();
      const birthDate = new Date(today.getFullYear() - 44, 4, 15); // 44 years old (May 15)
      component.patient = { ...mockPatient, birthDate: birthDate.toISOString().split('T')[0] };

      const age = component.getPatientAge();

      expect(age).toBe(44);
    });

    it('should handle birthday not yet passed this year', () => {
      const today = new Date();
      const nextMonth = today.getMonth() + 2; // Birth month ahead
      const birthYear = today.getFullYear() - 44;
      const birthDate = new Date(birthYear, nextMonth, 15);
      component.patient = { ...mockPatient, birthDate: birthDate.toISOString().split('T')[0] };

      const age = component.getPatientAge();

      expect(age).toBe(43); // One year less because birthday hasn't occurred yet
    });

    it('should return undefined if no birthDate', () => {
      component.patient = { ...mockPatient, birthDate: undefined };

      const age = component.getPatientAge();

      expect(age).toBeUndefined();
    });

    it('should return undefined if no patient loaded', () => {
      component.patient = null;

      const age = component.getPatientAge();

      expect(age).toBeUndefined();
    });
  });

  describe('Patient MRN Display', () => {
    beforeEach(() => {
      component.patient = mockPatient;
    });

    it('should extract MRN from patient identifiers', () => {
      const mrn = component.getPatientMRN();

      expect(mrn).toBe('MRN12345');
    });

    it('should extract MRN authority/system', () => {
      const authority = component.getPatientMRNAuthority();

      expect(authority).toBe('http://hospital.example.org/patients');
    });

    it('should format MRN authority as hostname', () => {
      const formatted = component.formatMRNAuthority('http://hospital.example.org/patients');

      expect(formatted).toBe('hospital.example.org');
    });

    it('should return authority as-is if not a valid URL', () => {
      const formatted = component.formatMRNAuthority('HOSPITAL_SYSTEM_001');

      expect(formatted).toBe('HOSPITAL_SYSTEM_001');
    });

    it('should return empty string for undefined authority', () => {
      const formatted = component.formatMRNAuthority(undefined);

      expect(formatted).toBe('');
    });

    it('should return undefined for MRN if no patient', () => {
      component.patient = null;

      const mrn = component.getPatientMRN();

      expect(mrn).toBeUndefined();
    });
  });

  describe('Clinical Data Formatting', () => {
    const mockObservation = mockClinicalData.observations[0];
    const mockCondition = mockClinicalData.conditions[0];
    const mockProcedure = mockClinicalData.procedures[0];

    it('should format observation value using service', () => {
      mockFhirClinicalService.formatObservationValue.mockReturnValue('120 mmHg');

      const value = component.formatObservationValue(mockObservation);

      expect(value).toBe('120 mmHg');
      expect(mockFhirClinicalService.formatObservationValue).toHaveBeenCalledWith(mockObservation);
    });

    it('should get observation code display using service', () => {
      mockFhirClinicalService.getObservationCodeDisplay.mockReturnValue('Systolic BP');

      const code = component.getObservationCode(mockObservation);

      expect(code).toBe('Systolic BP');
      expect(mockFhirClinicalService.getObservationCodeDisplay).toHaveBeenCalledWith(mockObservation);
    });

    it('should get condition code display using service', () => {
      mockFhirClinicalService.getConditionCodeDisplay.mockReturnValue('Hypertension');

      const code = component.getConditionCode(mockCondition);

      expect(code).toBe('Hypertension');
      expect(mockFhirClinicalService.getConditionCodeDisplay).toHaveBeenCalledWith(mockCondition);
    });

    it('should get condition status using service', () => {
      mockFhirClinicalService.getConditionStatus.mockReturnValue('Active');

      const status = component.getConditionStatus(mockCondition);

      expect(status).toBe('Active');
      expect(mockFhirClinicalService.getConditionStatus).toHaveBeenCalledWith(mockCondition);
    });

    it('should get procedure code display using service', () => {
      mockFhirClinicalService.getProcedureCodeDisplay.mockReturnValue('Office Visit');

      const code = component.getProcedureCode(mockProcedure);

      expect(code).toBe('Office Visit');
      expect(mockFhirClinicalService.getProcedureCodeDisplay).toHaveBeenCalledWith(mockProcedure);
    });
  });

  describe('Date Formatting', () => {
    it('should format date string to locale date', () => {
      const formatted = component.formatDate('2024-01-15');

      // Check that formatting works and returns a non-empty string
      // Locale-independent: just verify it's formatted and contains year
      expect(formatted).toBeTruthy();
      expect(formatted).not.toBe('N/A');
      expect(formatted.length).toBeGreaterThan(4); // More than just a year
      expect(formatted).toContain('2024'); // Year should be in output
    });

    it('should return "N/A" for undefined date', () => {
      const formatted = component.formatDate(undefined);

      expect(formatted).toBe('N/A');
    });

    it('should return "N/A" for empty string', () => {
      const formatted = component.formatDate('');

      expect(formatted).toBe('N/A');
    });
  });

  describe('Compliance Display Helpers', () => {
    it('should return "Compliant" label for compliant result', () => {
      const label = component.getComplianceLabel(true);

      expect(label).toBe('Compliant');
    });

    it('should return "Non-Compliant" label for non-compliant result', () => {
      const label = component.getComplianceLabel(false);

      expect(label).toBe('Non-Compliant');
    });

    it('should return "success" chip color for compliant result', () => {
      const color = component.getComplianceChipColor(true);

      expect(color).toBe('success');
    });

    it('should return "warn" chip color for non-compliant result', () => {
      const color = component.getComplianceChipColor(false);

      expect(color).toBe('warn');
    });
  });

  describe('Navigation', () => {
    beforeEach(() => {
      mockRouter.navigate.mockResolvedValue(true);
    });

    it('should navigate back to patients page', async () => {
      await component.goBack();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/patients']);
    });

    it('should set loading state during back navigation', () => {
      component.goBack();

      expect(component.backButtonLoading).toBe(true);
    });

    it('should reset loading state after successful back navigation', async () => {
      await component.goBack();

      expect(component.backButtonLoading).toBe(false);
    });

    it('should reset loading state after failed back navigation', async () => {
      mockRouter.navigate.mockRejectedValue(new Error('Navigation failed'));

      component.goBack();

      // Wait for promise to reject and catch handler to execute
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(component.backButtonLoading).toBe(false);
    });

    it('should navigate to results page with patient filter', async () => {
      component.patientId = 'patient-123';

      await component.navigateToResults();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/results'], {
        queryParams: { patient: 'patient-123' },
      });
    });

    it('should set loading state during results navigation', () => {
      component.navigateToResults();

      expect(component.viewResultsLoading).toBe(true);
    });

    it('should reset loading state after successful results navigation', async () => {
      await component.navigateToResults();

      expect(component.viewResultsLoading).toBe(false);
    });
  });

  describe('Care Gaps Identification', () => {
    beforeEach(() => {
      component.qualityResults = mockQualityResults;
    });

    it('should identify care gaps for non-evaluated measures', () => {
      const gaps = component.getCareGaps();

      expect(gaps.length).toBeGreaterThan(0);
      expect(gaps[0]).toHaveProperty('measure');
      expect(gaps[0]).toHaveProperty('reason');
    });

    it('should only include measures not in quality results', () => {
      const gaps = component.getCareGaps();
      const gapMeasures = gaps.map((g) => g.measure);

      expect(gapMeasures).not.toContain('HEDIS_CDC');
      expect(gapMeasures).not.toContain('HEDIS_CBP');
    });

    it('should return true if care gaps exist', () => {
      component.qualityResults = [mockQualityResults[0]]; // Only one measure completed

      const hasGaps = component.hasCareGaps();

      expect(hasGaps).toBe(true);
    });

    it('should return false if no care gaps', () => {
      component.qualityResults = [
        { id: '1', tenantId: 'T1', measureId: 'HEDIS_CDC', measureName: 'CDC', measureCategory: 'HEDIS', measureYear: 2024, patientId: 'p1', numeratorCompliant: true, denominatorEligible: true, complianceRate: 90, score: 90, calculationDate: '2024-01-01', createdAt: '2024-01-01', createdBy: 'system', version: 1 },
        { id: '2', tenantId: 'T1', measureId: 'HEDIS_CBP', measureName: 'CBP', measureCategory: 'HEDIS', measureYear: 2024, patientId: 'p1', numeratorCompliant: true, denominatorEligible: true, complianceRate: 85, score: 85, calculationDate: '2024-01-01', createdAt: '2024-01-01', createdBy: 'system', version: 1 },
        { id: '3', tenantId: 'T1', measureId: 'HEDIS_COL', measureName: 'COL', measureCategory: 'HEDIS', measureYear: 2024, patientId: 'p1', numeratorCompliant: true, denominatorEligible: true, complianceRate: 92, score: 92, calculationDate: '2024-01-01', createdAt: '2024-01-01', createdBy: 'system', version: 1 },
        { id: '4', tenantId: 'T1', measureId: 'HEDIS_BCS', measureName: 'BCS', measureCategory: 'HEDIS', measureYear: 2024, patientId: 'p1', numeratorCompliant: true, denominatorEligible: true, complianceRate: 88, score: 88, calculationDate: '2024-01-01', createdAt: '2024-01-01', createdBy: 'system', version: 1 },
        { id: '5', tenantId: 'T1', measureId: 'HEDIS_CIS', measureName: 'CIS', measureCategory: 'HEDIS', measureYear: 2024, patientId: 'p1', numeratorCompliant: true, denominatorEligible: true, complianceRate: 95, score: 95, calculationDate: '2024-01-01', createdAt: '2024-01-01', createdBy: 'system', version: 1 },
      ];

      const hasGaps = component.hasCareGaps();

      expect(hasGaps).toBe(false);
    });

    it('should set reason as "Not yet evaluated" for all gaps', () => {
      const gaps = component.getCareGaps();

      gaps.forEach((gap) => {
        expect(gap.reason).toBe('Not yet evaluated');
      });
    });
  });

  describe('Table Column Configuration', () => {
    it('should have correct observation table columns', () => {
      expect(component.observationsColumns).toEqual(['date', 'code', 'value']);
    });

    it('should have correct conditions table columns', () => {
      expect(component.conditionsColumns).toEqual(['status', 'code', 'onset']);
    });

    it('should have correct procedures table columns', () => {
      expect(component.proceduresColumns).toEqual(['status', 'code', 'performed']);
    });

    it('should have correct results table columns', () => {
      expect(component.resultsColumns).toEqual(['measure', 'compliant', 'date']);
    });
  });

  describe('Component State Management', () => {
    it('should initialize with loading state true', () => {
      expect(component.loading).toBe(true);
    });

    it('should initialize with no error', () => {
      expect(component.error).toBeNull();
    });

    it('should initialize with no patient data', () => {
      expect(component.patient).toBeNull();
      expect(component.clinicalData).toBeNull();
      expect(component.qualityResults).toEqual([]);
    });

    it('should initialize button loading states as false', () => {
      expect(component.backButtonLoading).toBe(false);
      expect(component.viewResultsLoading).toBe(false);
    });
  });
});
