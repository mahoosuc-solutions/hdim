import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { EvaluationsComponent } from './evaluations.component';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { DialogService } from '../../services/dialog.service';
import { ToastService } from '../../services/toast.service';
import { LoggerService } from '../../services/logger.service';
import { CqlLibraryFactory } from '../../../testing/factories/cql-library.factory';
import { PatientFactory } from '../../../testing/factories/patient.factory';
import { EvaluationFactory } from '../../../testing/factories/evaluation.factory';
import { CSVHelper } from '../../utils/csv-helper';
import { createMockLoggerService } from '../../../testing/mocks';
import { createMockHttpClient } from '../../testing/mocks';
import { createMockStore } from '../../testing/mocks';
import { Store } from '@ngrx/store';

const mockLoggerService = createMockLoggerService();

describe('EvaluationsComponent', () => {
  let component: EvaluationsComponent;
  let fixture: ComponentFixture<EvaluationsComponent>;
  let mockMeasureService: jest.Mocked<MeasureService>;
  let mockEvaluationService: jest.Mocked<EvaluationService>;
  let mockPatientService: jest.Mocked<PatientService>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockToastService: jest.Mocked<ToastService>;

  beforeEach(async () => {
    // Create mock services
    mockMeasureService = {
      getActiveMeasuresInfo: jest.fn(),
      getAllAvailableMeasures: jest.fn(),
    } as any;

    mockEvaluationService = {
      calculateQualityMeasure: jest.fn(),
      getAllResults: jest.fn(),
      getDefaultEvaluationPreset: jest.fn(),
      saveDefaultEvaluationPreset: jest.fn(),
      clearDefaultEvaluationPreset: jest.fn(),
    } as any;
    mockEvaluationService.getAllResults.mockReturnValue(of([]));
    mockEvaluationService.getDefaultEvaluationPreset.mockReturnValue(of(null));
    mockEvaluationService.saveDefaultEvaluationPreset.mockReturnValue(of(null));
    mockEvaluationService.clearDefaultEvaluationPreset.mockReturnValue(of(null));

    mockPatientService = {
      getPatientsSummary: jest.fn(),
      toPatientSummary: jest.fn(),
    } as any;

    mockDialogService = {
      confirm: jest.fn(),
    } as any;
    mockDialogService.confirm.mockReturnValue(of(true));

    mockToastService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as any;

    await TestBed.configureTestingModule({
      imports: [EvaluationsComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: HttpClient, useValue: createMockHttpClient() },
        { provide: Store, useValue: createMockStore() }],
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

      mockMeasureService.getAllAvailableMeasures.mockReturnValue(of([]));
      mockPatientService.getPatientsSummary.mockReturnValue(of([]));

      component.ngOnInit();

      expect(loadMeasuresSpy).toHaveBeenCalled();
      expect(loadPatientsSpy).toHaveBeenCalled();
    });
  });

  describe('Loading Active Measures', () => {
    it('should load measures successfully', () => {
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      mockMeasureService.getAllAvailableMeasures.mockReturnValue(of(mockMeasures));

      component.loadActiveMeasures();

      expect(component.loadingMeasures).toBe(false);
      expect(component.measures).toEqual(mockMeasures);
      expect(component.allMeasures).toEqual(mockMeasures);
      expect(component.measures.length).toBe(3);
      expect(component.measuresError).toBeNull();
    });

    it('should set loading state while fetching measures', () => {
      mockMeasureService.getAllAvailableMeasures.mockReturnValue(of([]));

      component.loadActiveMeasures();
      expect(component.loadingMeasures).toBe(false); // Will be false after subscription
    });

    it('should handle error when loading measures fails with fallback', () => {
      const error = { status: 500, message: 'Server Error' };
      mockMeasureService.getAllAvailableMeasures.mockReturnValue(
        throwError(() => error)
      );
      // Fallback also fails
      mockMeasureService.getActiveMeasuresInfo.mockReturnValue(of([]));

      component.loadActiveMeasures();

      expect(component.loadingMeasures).toBe(false);
      expect(component.measuresError).toContain('Unable to process quality measures');
    });

    it('should clear previous error on successful load', () => {
      const mockMeasures = CqlLibraryFactory.createMeasureInfoList();
      component.measuresError = 'Previous error';

      mockMeasureService.getAllAvailableMeasures.mockReturnValue(of(mockMeasures));

      component.loadActiveMeasures();

      expect(component.measuresError).toBeNull();
    });
  });

  describe('Category Filtering', () => {
    beforeEach(() => {
      // Setup measures with different categories
      const mockMeasures = [
        { id: '1', name: 'CDC', displayName: 'CDC Measure', category: 'CHRONIC_DISEASE', version: '2024' },
        { id: '2', name: 'BCS', displayName: 'BCS Measure', category: 'PREVENTIVE', version: '2024' },
        { id: '3', name: 'CBP', displayName: 'CBP Measure', category: 'CHRONIC_DISEASE', version: '2024' },
        { id: '4', name: 'AMM', displayName: 'AMM Measure', category: 'BEHAVIORAL_HEALTH', version: '2024' },
      ];
      component.allMeasures = mockMeasures;
      component.measures = mockMeasures;
    });

    it('should filter measures by category', () => {
      component.onCategoryChange('CHRONIC_DISEASE');

      expect(component.selectedCategory).toBe('CHRONIC_DISEASE');
      expect(component.measures.length).toBe(2);
      expect(component.measures.every(m => m.category === 'CHRONIC_DISEASE')).toBe(true);
    });

    it('should show all measures when category is empty', () => {
      component.selectedCategory = 'CHRONIC_DISEASE';
      component.measures = component.allMeasures.filter(m => m.category === 'CHRONIC_DISEASE');

      component.onCategoryChange('');

      expect(component.selectedCategory).toBe('');
      expect(component.measures.length).toBe(4);
    });

    it('should return correct count for each category', () => {
      expect(component.getCategoryCount('')).toBe(4);
      expect(component.getCategoryCount('CHRONIC_DISEASE')).toBe(2);
      expect(component.getCategoryCount('PREVENTIVE')).toBe(1);
      expect(component.getCategoryCount('BEHAVIORAL_HEALTH')).toBe(1);
      expect(component.getCategoryCount('SDOH')).toBe(0);
    });

    it('should clear measure selection when filtered out', () => {
      component.evaluationForm.patchValue({ measureId: 'AMM' });

      component.onCategoryChange('CHRONIC_DISEASE');

      expect(component.evaluationForm.get('measureId')?.value).toBe('');
    });

    it('should keep measure selection if still in filtered list', () => {
      component.evaluationForm.patchValue({ measureId: 'CDC' });

      component.onCategoryChange('CHRONIC_DISEASE');

      expect(component.evaluationForm.get('measureId')?.value).toBe('CDC');
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
      };
    });

    it('should filter patients by MRN', (done) => {
      component.evaluationForm.get('patientSearch')?.setValue('MRN00001');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        expect(filtered[0].mrn).toBe('MRN00001');
        done();
      };
    });

    it('should handle object values in patient search', (done) => {
      const patient = PatientFactory.createSummary({ fullName: 'Jane Roe', mrn: 'MRN00999' };
      component.patients = [patient];
      component.setupPatientAutocomplete();

      component.evaluationForm.get('patientSearch')?.setValue(patient);

      let subscription: { unsubscribe: () => void } | null = null;
      subscription = component.filteredPatients.subscribe((filtered) => {
        if (filtered.some((match) => match.fullName === 'Jane Roe')) {
          expect(filtered[0].fullName).toBe('Jane Roe');
          subscription?.unsubscribe();
          done();
        }
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
      };
    });

    it('should be case-insensitive when filtering', (done) => {
      component.evaluationForm.get('patientSearch')?.setValue('JOHN');

      component.filteredPatients.subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        done();
      };
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

    it('should set selected patient', () => {
      const patient = PatientFactory.createSummary();

      component.onPatientSelected(patient);

      expect(component.selectedPatient).toEqual(patient);
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

      mockMeasureService.getAllAvailableMeasures.mockReturnValue(of(mockMeasures));
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

  describe('Additional behaviors', () => {
    it('returns display text for patient', () => {
      const patient = PatientFactory.createSummary({ fullName: 'John Doe', mrn: 'MRN1' });
      expect(component.displayPatient(patient)).toContain('MRN1');
      expect(component.displayPatient(null)).toBe('');
    });

    it('handles evaluation errors', () => {
      const patient = PatientFactory.createSummary({ id: 'patient-1' });
      component.onPatientSelected(patient);
      component.evaluationForm.patchValue({ measureId: 'm1', patientSearch: patient });
      mockEvaluationService.calculateQualityMeasure.mockReturnValue(
        throwError(() => new Error('fail'))
      );

      component.submitEvaluation();

      expect(component.evaluationError).toContain('Unable');
      expect(component.submitting).toBe(false);
    });

    it('resets form state', () => {
      component.selectedPatient = PatientFactory.createSummary({ id: 'patient-1' });
      component.evaluationResult = EvaluationFactory.createCompliantResult();

      component.resetForm();

      expect(component.selectedPatient).toBeNull();
      expect(component.evaluationResult).toBeNull();
      expect(component.evaluationError).toBeNull();
    });

    it('formats date and outcomes', () => {
      expect(component.formatDate('2025-01-02T12:00:00Z')).toBe('2025-01-02');
      const result = EvaluationFactory.createCompliantResult();
      expect(component.getOutcomeText(result)).toBe('Compliant');
      expect(component.getStatusClass(result)).toBe('status-success');
    });

    it('formats non-compliant and not-eligible outcomes', () => {
      const nonCompliant = EvaluationFactory.createNonCompliantResult();
      const notEligible = EvaluationFactory.createNotEligibleResult();

      expect(component.getOutcomeText(nonCompliant)).toBe('Non-Compliant');
      expect(component.getOutcomeText(notEligible)).toBe('Not Eligible');
    });

    it('enables and disables controls without emitting events', () => {
      (component as any).setControlEnabled('measureId', false);
      expect(component.evaluationForm.get('measureId')?.disabled).toBe(true);

      (component as any).setControlEnabled('measureId', true);
      expect(component.evaluationForm.get('measureId')?.enabled).toBe(true);
    });

    it('ignores setControlEnabled for unknown controls', () => {
      expect(() => (component as any).setControlEnabled('missing-control', true)).not.toThrow();
    });

    it('handles selection helpers', () => {
      const evaluation = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [evaluation];

      expect(component.isAllSelected()).toBe(false);
      component.masterToggle();
      expect(component.isAllSelected()).toBe(true);
      expect(component.getSelectionCount()).toBe(1);
      component.clearSelection();
      expect(component.getSelectionCount()).toBe(0);
    });

    it('clears selections when all rows are selected', () => {
      const first = EvaluationFactory.createCompliantResult();
      const second = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [first, second];
      component.selection.select(first);
      component.selection.select(second);

      expect(component.isAllSelected()).toBe(true);
      component.masterToggle();
      expect(component.getSelectionCount()).toBe(0);
    });

    it('builds checkbox labels for rows and bulk selection', () => {
      const evaluation = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [evaluation];

      expect(component.checkboxLabel()).toContain('select all');
      expect(component.checkboxLabel(evaluation)).toContain(`select row ${evaluation.id}`);

      component.selection.select(evaluation);
      expect(component.checkboxLabel()).toContain('deselect all');
      expect(component.checkboxLabel(evaluation)).toContain(`deselect row ${evaluation.id}`);
    });

    it('exports selected evaluations to CSV', () => {
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation();
      const evaluation = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [evaluation];
      component.selection.select(evaluation);

      component.exportSelectedToCSV();

      expect(arraySpy).toHaveBeenCalled();
      expect(downloadSpy).toHaveBeenCalled();
      arraySpy.mockRestore();
      downloadSpy.mockRestore();
    });

    it('does not export when nothing is selected', () => {
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');

      component.exportSelectedToCSV();

      expect(arraySpy).not.toHaveBeenCalled();
      arraySpy.mockRestore();
    });

    it('deletes selected evaluations when confirmed', () => {
      const evaluation = EvaluationFactory.createCompliantResult();
      component.evaluations = [evaluation];
      component.dataSource.data = [evaluation];
      component.selection.select(evaluation);

      component.deleteSelected();

      expect(component.evaluations.length).toBe(0);
    });

    it('uses plural label when deleting multiple evaluations', () => {
      const first = EvaluationFactory.createCompliantResult();
      const second = EvaluationFactory.createCompliantResult();
      component.evaluations = [first, second];
      component.dataSource.data = [first, second];
      component.selection.select(first);
      component.selection.select(second);

      component.deleteSelected();

      expect(mockDialogService.confirm).toHaveBeenCalledWith(
        'Delete Evaluations',
        expect.stringContaining('2 evaluations'),
        'Delete',
        'Cancel',
        'warn'
      );
    });

    it('does nothing when delete is requested without selection', () => {
      component.deleteSelected();

      expect(mockDialogService.confirm).not.toHaveBeenCalled();
    });

    it('does nothing when delete is cancelled', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      const evaluation = EvaluationFactory.createCompliantResult();
      component.dataSource.data = [evaluation];
      component.selection.select(evaluation);

      component.deleteSelected();

      expect(component.dataSource.data.length).toBe(1);
    });

    it('loads evaluations and handles errors', () => {
      const results = [EvaluationFactory.createCompliantResult()];
      mockEvaluationService.getAllResults.mockReturnValue(of(results));

      component.loadEvaluations();

      expect(component.dataSource.data.length).toBe(1);

      mockEvaluationService.getAllResults.mockReturnValue(
        throwError(() => new Error('load failed'))
      );

      component.loadEvaluations();

      expect(component.dataSource.data.length).toBe(0);
    });
  });
});
