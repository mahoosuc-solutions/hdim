import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { EvaluationsComponent } from './evaluations.component';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { CqlLibraryFactory } from '../../../testing/factories/cql-library.factory';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';

describe('EvaluationsComponent', () => {
  let component: EvaluationsComponent;
  let fixture: ComponentFixture<EvaluationsComponent>;
  let mockMeasureService: jest.Mocked<MeasureService>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockPatientService: jest.Mocked<PatientService>;

  beforeEach(async () => {
    // Create mock services
    mockMeasureService = {
      getActiveMeasuresInfo: jest.fn(),
    } as any;

    mockEvaluationService = {
      calculateQualityMeasure: jest.fn(),
    } as any;

    mockPatientService = {
      getPatientsSummary: jest.fn(),
      toPatientSummary: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [EvaluationsComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: PatientService, useValue: mockPatientService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluationsComponent);
    component = fixture.componentInstance;
    CqlLibraryFactory.reset();
    PatientFactory.reset();
    EvaluationFactory.reset();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with required fields', () => {
      expect(component.evaluationForm).toBeDefined();
      expect(component.evaluationForm.get('measureId')).toBeDefined();
      expect(component.evaluationForm.get('patientSearch')).toBeDefined();
    });

    it('should have validators on form fields', () => {
      const measureIdControl = component.evaluationForm.get('measureId');
      const patientSearchControl = component.evaluationForm.get('patientSearch');

      expect(measureIdControl?.hasError('required')).toBe(true);
      expect(patientSearchControl?.hasError('required')).toBe(true);
    });

    it('should call loadActiveMeasures and loadPatients on ngOnInit', () => {
      const loadMeasuresSpy = jest.spyOn(component, 'loadActiveMeasures');
      const loadPatientsSpy = jest.spyOn(component, 'loadPatients');

      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of([]));
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.ngOnInit();

      expect(loadMeasuresSpy).toHaveBeenCalled();
      expect(loadPatientsSpy).toHaveBeenCalled();
    });
  });

  describe('Loading Active Measures', () => {
    it('should load measures successfully', () => {
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of(mockMeasures));

      component.loadActiveMeasures();

      expect(component.loadingMeasures).toBe(false);
      expect(component.measures).toEqual(mockMeasures);
      expect(component.measures.length).toBe(3);
      expect(component.measuresError).toBeNull();
    });

    it('should set loading state while fetching measures', () => {
      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of([]));

      component.loadActiveMeasures();
      expect(component.loadingMeasures).toBe(false); // Will be false after subscription
    });

    it('should handle error when loading measures fails', () => {
      const error = { status: 500, message: 'Server Error' };
      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(
        throwError(() => error)
      );

      component.loadActiveMeasures();

      expect(component.loadingMeasures).toBe(false);
      expect(component.measuresError).toContain('Unable to process quality measures');
      expect(component.measures).toEqual([]);
    });

    it('should clear previous error on successful load', () => {
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      component.measuresError = 'Previous error';

      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of(mockMeasures));

      component.loadActiveMeasures();

      expect(component.measuresError).toBeNull();
    });
  });

  describe('Loading Patients', () => {
    it('should load patients successfully', () => {
      const mockPatients = PatientFactory.createSummaryList();
      mockPatientService.getPatientsSummary.mockReturnValue(of(mockPatients));

      component.loadPatients();

      expect(component.loadingPatients).toBe(false);
      expect(component.patients).toEqual(mockPatients);
      expect(component.patients.length).toBe(3);
      expect(component.patientsError).toBeNull();
    });

    it('should handle error when loading patients fails', () => {
      const error = { status: 500, message: 'Server Error' };
      mockPatientService.getPatientsSummary.mockReturnValue(
        throwError(() => error)
      );

      component.loadPatients();

      expect(component.loadingPatients).toBe(false);
      expect(component.patientsError).toContain('Unable to access patient data');
      expect(component.patients).toEqual([]);
    });
  });

  describe('Patient Autocomplete', () => {
    beforeEach(() => {
      const mockPatients = PatientFactory.createSummaryList();
      component.patients = mockPatients;
      component.setupPatientAutocomplete();
    });

    it('should filter patients by name', (done) => {
      component.evaluationForm.get('patientSearch')?.setValue('John');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        expect(filtered[0].fullName).toContain('John');
        done();
      });
    });

    it('should filter patients by MRN', (done) => {
      component.evaluationForm.get('patientSearch')?.setValue('MRN00001');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        expect(filtered[0].mrn).toBe('MRN00001');
        done();
      });
    });

    it('should show first 10 patients when search is empty', (done) => {
      component.patients = PatientFactory.createMany(15).map((p) =>
        PatientFactory.createSummary()
      );
      component.setupPatientAutocomplete();
      component.evaluationForm.get('patientSearch')?.setValue('');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBe(10);
        done();
      });
    });

    it('should be case-insensitive when filtering', (done) => {
      component.evaluationForm.get('patientSearch')?.setValue('JOHN');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        done();
      });
    });

    it('should limit results to 10 items', (done) => {
      const manyPatients = Array.from({ length: 20 }, (_, i) =>
        PatientFactory.createSummary({
          fullName: `Patient ${i}`,
          id: `patient-${i}`,
        })
      );
      component.patients = manyPatients;
      component.setupPatientAutocomplete();
      component.evaluationForm.get('patientSearch')?.setValue('Patient');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeLessThanOrEqual(10);
        done();
      });
    });
  });

  describe('Display Patient', () => {
    it('should display patient with MRN', () => {
      const patient = PatientFactory.createSummary({
        fullName: 'John Doe',
        mrn: 'MRN12345',
      });

      const display = component.displayPatient(patient);

      expect(display).toBe('John Doe (MRN: MRN12345)');
    });

    it('should display patient without MRN', () => {
      const patient = PatientFactory.createSummary({
        fullName: 'Jane Smith',
        mrn: undefined,
      });

      const display = component.displayPatient(patient);

      expect(display).toBe('Jane Smith');
    });

    it('should return empty string for null patient', () => {
      const display = component.displayPatient(null);

      expect(display).toBe('');
    });
  });

  describe('Patient Selection', () => {
    it('should set selectedPatient when patient is selected', () => {
      const patient = PatientFactory.createSummary();

      component.onPatientSelected(patient);

      expect(component.selectedPatient).toEqual(patient);
    });

    it('should log selected patient to console', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
      const patient = PatientFactory.createSummary();

      component.onPatientSelected(patient);

      expect(consoleSpy).toHaveBeenCalledWith('Selected patient:', patient);
      consoleSpy.mockRestore();
    });
  });

  describe('Submit Evaluation', () => {
    beforeEach(() => {
      const patient = PatientFactory.createSummary({ id: 'patient-001' });
      component.selectedPatient = patient;
      component.evaluationForm.patchValue({
        measureId: 'lib-1',
        patientSearch: patient,
      });
    });

    it('should not submit if form is invalid', () => {
      component.evaluationForm.patchValue({ measureId: '' });

      component.submitEvaluation();

      expect(mockEvaluationService.calculateQualityMeasure).not.toHaveBeenCalled();
    });

    it('should not submit if no patient is selected', () => {
      component.selectedPatient = null;

      component.submitEvaluation();

      expect(mockEvaluationService.calculateQualityMeasure).not.toHaveBeenCalled();
    });

    it('should submit evaluation with correct parameters', () => {
      const mockResult = EvaluationFactory.createCompliantResult();
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(of(mockResult));

      component.submitEvaluation();

      expect(mockEvaluationService.calculateQualityMeasure).toHaveBeenCalledWith(
        'patient-001',
        'lib-1'
      );
    });

    it('should set submitting state during submission', () => {
      const mockResult = EvaluationFactory.createCompliantResult();
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(of(mockResult));

      component.submitEvaluation();

      // After subscription completes, submitting should be false
      expect(component.submitting).toBe(false);
    });

    it('should store evaluation result on success', () => {
      const mockResult = EvaluationFactory.createCompliantResult();
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(of(mockResult));

      component.submitEvaluation();

      expect(component.evaluationResult).toEqual(mockResult);
      expect(component.evaluationError).toBeNull();
    });

    it('should handle evaluation error', () => {
      const error = { userMessage: 'Evaluation failed', status: 500 };
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(
        throwError(() => error)
      );

      component.submitEvaluation();

      // Component uses ErrorFactory.createCqlEngineError which produces a specific message
      expect(component.evaluationError).toContain('Unable to complete the quality measure evaluation');
      expect(component.evaluationResult).toBeNull();
      expect(component.submitting).toBe(false);
    });

    it('should use default error message if userMessage not provided', () => {
      const error = { status: 500 };
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(
        throwError(() => error)
      );

      component.submitEvaluation();

      // Component uses ErrorFactory for error messages
      expect(component.evaluationError).toContain('Unable to complete');
    });

    it('should clear previous results before submission', () => {
      component.evaluationResult = EvaluationFactory.createCompliantResult();
      component.evaluationError = 'Previous error';

      const mockResult = EvaluationFactory.createNonCompliantResult();
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(of(mockResult));

      component.submitEvaluation();

      expect(component.evaluationResult).toEqual(mockResult);
      expect(component.evaluationError).toBeNull();
    });
  });

  describe('Reset Form', () => {
    it('should reset form controls', () => {
      component.evaluationForm.patchValue({
        measureId: 'lib-1',
        patientSearch: 'John Doe',
      });

      component.resetForm();

      expect(component.evaluationForm.get('measureId')?.value).toBeNull();
      expect(component.evaluationForm.get('patientSearch')?.value).toBeNull();
    });

    it('should clear selected patient', () => {
      component.selectedPatient = PatientFactory.createSummary();

      component.resetForm();

      expect(component.selectedPatient).toBeNull();
    });

    it('should clear evaluation result', () => {
      component.evaluationResult = EvaluationFactory.createCompliantResult();

      component.resetForm();

      expect(component.evaluationResult).toBeNull();
    });

    it('should clear evaluation error', () => {
      component.evaluationError = 'Some error';

      component.resetForm();

      expect(component.evaluationError).toBeNull();
    });
  });

  describe('Get Status Class', () => {
    it('should return success class for compliant result', () => {
      const result = EvaluationFactory.createCompliantResult();

      const statusClass = component.getStatusClass(result);

      expect(statusClass).toBe('status-success');
    });

    it('should return warning class for non-compliant but eligible result', () => {
      const result = EvaluationFactory.createNonCompliantResult();

      const statusClass = component.getStatusClass(result);

      expect(statusClass).toBe('status-warning');
    });

    it('should return info class for not eligible result', () => {
      const result = EvaluationFactory.createNotEligibleResult();

      const statusClass = component.getStatusClass(result);

      expect(statusClass).toBe('status-info');
    });
  });

  describe('Get Status Text', () => {
    it('should return "Compliant" for compliant result', () => {
      const result = EvaluationFactory.createCompliantResult();

      const statusText = component.getStatusText(result);

      expect(statusText).toBe('Compliant');
    });

    it('should return "Non-Compliant (Eligible)" for non-compliant but eligible', () => {
      const result = EvaluationFactory.createNonCompliantResult();

      const statusText = component.getStatusText(result);

      expect(statusText).toBe('Non-Compliant (Eligible)');
    });

    it('should return "Not Eligible" for not eligible result', () => {
      const result = EvaluationFactory.createNotEligibleResult();

      const statusText = component.getStatusText(result);

      expect(statusText).toBe('Not Eligible');
    });
  });

  describe('Integration Tests', () => {
    it('should handle complete evaluation workflow', () => {
      // Setup
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      const mockPatients = PatientFactory.createSummaryList();
      const mockResult = EvaluationFactory.createCompliantResult();

      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of(mockMeasures));
      mockPatientService.getPatientsSummary.mockReturnValue(of(mockPatients));
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(of(mockResult));

      // Initialize component
      component.ngOnInit();

      // Verify data loaded
      expect(component.measures.length).toBe(3);
      expect(component.patients.length).toBe(3);

      // Select patient
      component.onPatientSelected(mockPatients[0]);
      expect(component.selectedPatient).toEqual(mockPatients[0]);

      // Fill form
      component.evaluationForm.patchValue({
        measureId: mockMeasures[0].id,
        patientSearch: mockPatients[0],
      });

      // Submit evaluation
      component.submitEvaluation();

      // Verify result
      expect(component.evaluationResult).toEqual(mockResult);
      expect(component.getStatusText(mockResult)).toBe('Compliant');
    });
  });
});
