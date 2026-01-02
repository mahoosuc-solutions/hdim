import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BatchEvaluationDialogComponent } from './batch-evaluation-dialog.component';
import { MeasureService } from '../../services/measure.service';
import { PatientService } from '../../services/patient.service';
import { EvaluationService } from '../../services/evaluation.service';
import { CareGapService, CareGap, CareGapStatus, CareGapType, GapPriority } from '../../services/care-gap.service';
import { ScheduledEvaluationService } from '../../services/scheduled-evaluation.service';
import { ToastService } from '../../services/toast.service';
import { MeasureInfo } from '../../models/cql-library.model';
import { PatientSummary } from '../../models/patient.model';

describe('BatchEvaluationDialogComponent', () => {
  let component: BatchEvaluationDialogComponent;
  let fixture: ComponentFixture<BatchEvaluationDialogComponent>;
  let dialogRefSpy: { close: jest.Mock };
  let mockMeasureService: jest.Mocked<Partial<MeasureService>>;
  let mockPatientService: jest.Mocked<Partial<PatientService>>;
  let mockEvaluationService: jest.Mocked<Partial<EvaluationService>>;
  let mockCareGapService: jest.Mocked<Partial<CareGapService>>;
  let mockScheduledEvaluationService: jest.Mocked<Partial<ScheduledEvaluationService>>;
  let mockToastService: jest.Mocked<Partial<ToastService>>;
  let mockRouter: { navigate: jest.Mock };

  const mockMeasures: MeasureInfo[] = [
    { id: '1', name: 'CDC', displayName: 'Diabetes Care', version: '2024', category: 'CHRONIC_DISEASE' },
    { id: '2', name: 'BCS', displayName: 'Breast Cancer Screening', version: '2024', category: 'PREVENTIVE' },
    { id: '3', name: 'CBP', displayName: 'Blood Pressure Control', version: '2024', category: 'CHRONIC_DISEASE' },
  ];

  const mockPatients: PatientSummary[] = [
    { id: 'p1', fullName: 'John Doe', firstName: 'John', lastName: 'Doe', mrn: 'MRN001', age: 45, gender: 'male', status: 'Active' },
    { id: 'p2', fullName: 'Jane Smith', firstName: 'Jane', lastName: 'Smith', mrn: 'MRN002', age: 52, gender: 'female', status: 'Active' },
    { id: 'p3', fullName: 'Bob Wilson', firstName: 'Bob', lastName: 'Wilson', mrn: 'MRN003', age: 38, gender: 'male', status: 'Active' },
  ];

  const mockCareGaps: CareGap[] = [
    {
      id: 'gap1',
      patientId: 'p1',
      measureId: 'CDC',
      measureName: 'Diabetes Care',
      gapType: CareGapType.CHRONIC_DISEASE_MANAGEMENT,
      status: CareGapStatus.OPEN,
      priority: GapPriority.HIGH,
      priorityScore: 85,
      description: 'HbA1c control gap',
      recommendation: 'Schedule HbA1c test',
      detectedDate: new Date().toISOString(),
    },
  ];

  beforeEach(async () => {
    dialogRefSpy = { close: jest.fn() };
    mockRouter = { navigate: jest.fn() };
    mockMeasureService = {
      getAllAvailableMeasures: jest.fn().mockReturnValue(of(mockMeasures)),
    };
    mockPatientService = {
      getPatientsSummary: jest.fn().mockReturnValue(of(mockPatients)),
    };
    mockEvaluationService = {
      calculateQualityMeasure: jest.fn().mockReturnValue(of({
        id: 'result-1',
        patientId: 'p1',
        measureId: 'CDC',
        measureName: 'Diabetes Care',
        measureCategory: 'CHRONIC_DISEASE',
        denominatorEligible: true,
        numeratorCompliant: true,
        score: 1.0,
        complianceRate: 100,
        calculationDate: new Date().toISOString(),
      })),
    };
    mockCareGapService = {
      detectGapsBatch: jest.fn().mockReturnValue(of({
        batchId: 'batch-1',
        totalPatients: 1,
        successCount: 1,
        failureCount: 0,
        patientGaps: [{ patientId: 'p1', gaps: mockCareGaps }],
        errors: [],
      })),
      getPatientCareGaps: jest.fn().mockReturnValue(of(mockCareGaps)),
    };
    mockScheduledEvaluationService = {
      getSchedules: jest.fn().mockReturnValue(of([])),
    };
    mockToastService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [BatchEvaluationDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: null },
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: CareGapService, useValue: mockCareGapService },
        { provide: ScheduledEvaluationService, useValue: mockScheduledEvaluationService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
        provideNativeDateAdapter(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchEvaluationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load measures from service', () => {
    expect(mockMeasureService.getAllAvailableMeasures).toHaveBeenCalled();
    expect(component.availableMeasures.length).toBe(3);
  });

  it('should load patients from service', () => {
    expect(mockPatientService.getPatientsSummary).toHaveBeenCalled();
    expect(component.dataSource.data.length).toBe(3);
  });

  it('should filter measures by category', () => {
    const chronicMeasures = component.getMeasuresByCategory('CHRONIC_DISEASE');
    expect(chronicMeasures.length).toBe(2);
    expect(chronicMeasures.every(m => m.category === 'CHRONIC_DISEASE')).toBe(true);
  });

  it('should select and deselect all patients', () => {
    component.selectAll();
    expect(component.selection.selected.length).toBe(component.dataSource.data.length);

    component.deselectAll();
    expect(component.selection.selected.length).toBe(0);
  });

  it('should toggle master selection', () => {
    component.masterToggle();
    expect(component.isAllSelected()).toBe(true);

    component.masterToggle();
    expect(component.selection.selected.length).toBe(0);
  });

  it('should not allow run without patient selection', () => {
    component.deselectAll();
    component.selectedMeasures = ['CDC'];
    expect(component.canRun()).toBe(false);
  });

  it('should not allow run without measure selection', () => {
    component.selectAll();
    component.selectedMeasures = [];
    expect(component.canRun()).toBe(false);
  });

  it('should allow run with both patient and measure selections', () => {
    component.selection.select(mockPatients[0]);
    component.selectedMeasures = ['CDC'];
    expect(component.canRun()).toBe(true);
  });

  it('should calculate total evaluations correctly', () => {
    component.selection.select(mockPatients[0]);
    component.selection.select(mockPatients[1]);
    component.selectedMeasures = ['CDC', 'BCS'];
    expect(component.getTotalEvaluations()).toBe(4);
  });

  it('should provide appropriate run tooltip', () => {
    component.deselectAll();
    component.selectedMeasures = [];
    expect(component.getRunTooltip()).toBe('Select at least one patient');

    component.selection.select(mockPatients[0]);
    expect(component.getRunTooltip()).toBe('Select at least one measure');

    component.selectedMeasures = ['CDC'];
    expect(component.getRunTooltip()).toContain('Run 1 evaluations');
  });

  it('should calculate success rate correctly', () => {
    component.successCount.set(8);
    component.errorCount.set(2);
    expect(component.getSuccessRate()).toBe(80);
  });

  it('should handle zero evaluations for success rate', () => {
    component.successCount.set(0);
    component.errorCount.set(0);
    expect(component.getSuccessRate()).toBe(0);
  });

  it('closes with result when complete', () => {
    component.isComplete.set(true);
    component.successCount.set(3);
    component.errorCount.set(1);
    component.results = [];
    component.errors = [];

    component.onCancel();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      successCount: 3,
      errorCount: 1,
      results: [],
      errors: [],
      careGapsDetected: 0,
      careGaps: [],
    });
  });

  it('closes with null when cancelled before completion', () => {
    component.isComplete.set(false);
    component.onCancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });

  it('should handle view results action', () => {
    component.isComplete.set(true);
    component.successCount.set(5);
    component.errorCount.set(0);
    component.results = [];
    component.errors = [];

    component.onViewResults();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      successCount: 5,
      errorCount: 0,
      results: [],
      errors: [],
      careGapsDetected: 0,
      careGaps: [],
    });
  });

  it('should apply pre-selected data from dialog input', async () => {
    // Reset and create with pre-selected data
    const preSelectedPatients = [mockPatients[0]];
    const preSelectedMeasures = ['CDC'];

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [BatchEvaluationDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { preSelectedPatients, preSelectedMeasures } },
        { provide: MeasureService, useValue: mockMeasureService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: EvaluationService, useValue: mockEvaluationService },
        { provide: CareGapService, useValue: mockCareGapService },
        { provide: ScheduledEvaluationService, useValue: mockScheduledEvaluationService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
        provideNativeDateAdapter(),
      ],
    }).compileComponents();

    const newFixture = TestBed.createComponent(BatchEvaluationDialogComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.selection.selected.length).toBe(1);
    expect(newComponent.selectedMeasures).toEqual(['CDC']);
  });

  it('should handle evaluation errors gracefully', (done) => {
    mockEvaluationService.calculateQualityMeasure = jest.fn()
      .mockReturnValue(throwError(() => new Error('Evaluation failed')));

    component.selection.select(mockPatients[0]);
    component.selectedMeasures = ['CDC'];

    component.onRun();

    // Wait for async processing
    setTimeout(() => {
      expect(component.errorCount()).toBe(1);
      expect(component.errors.length).toBe(1);
      expect(component.errors[0].error).toBe('Evaluation failed');
      done();
    }, 200);
  });

  // Care Gap Integration Tests
  describe('Care Gap Integration', () => {
    it('should have care gap signals initialized', () => {
      expect(component.detectingCareGaps()).toBe(false);
      expect(component.careGapCheckComplete()).toBe(false);
      expect(component.careGapsDetected()).toBe(0);
    });

    it('should include care gap data in result on close', () => {
      component.isComplete.set(true);
      component.successCount.set(5);
      component.errorCount.set(0);
      component.careGapsDetected.set(2);
      component.detectedCareGaps = mockCareGaps;
      component.results = [];
      component.errors = [];

      component.onCancel();

      expect(dialogRefSpy.close).toHaveBeenCalledWith(
        expect.objectContaining({
          careGapsDetected: 2,
          careGaps: mockCareGaps,
        })
      );
    });

    it('should include care gap data in view results', () => {
      component.isComplete.set(true);
      component.successCount.set(3);
      component.errorCount.set(0);
      component.careGapsDetected.set(1);
      component.detectedCareGaps = mockCareGaps;
      component.results = [];
      component.errors = [];

      component.onViewResults();

      expect(dialogRefSpy.close).toHaveBeenCalledWith(
        expect.objectContaining({
          successCount: 3,
          errorCount: 0,
          careGapsDetected: 1,
          careGaps: mockCareGaps,
        })
      );
    });

    it('should navigate to care gaps page when viewing care gaps', () => {
      component.isComplete.set(true);
      component.careGapsDetected.set(1);
      component.detectedCareGaps = mockCareGaps;
      component.results = [];
      component.errors = [];

      component.onViewCareGaps();

      expect(dialogRefSpy.close).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/care-gaps']);
    });
  });
});
