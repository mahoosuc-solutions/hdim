import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { QualityMeasuresComponent } from './quality-measures.component';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { LoggerService } from '../../services/logger.service';
import { PatientService } from '../../services/patient.service';

describe('QualityMeasuresComponent', () => {
  let component: QualityMeasuresComponent;
  let fixture: ComponentFixture<QualityMeasuresComponent>;
  let httpMock: HttpTestingController;
  let measureServiceSpy: any;
  let evaluationServiceSpy: any;
  let patientServiceSpy: any;
  let loggerContextSpy: any;
  let loggerServiceSpy: any;

  beforeEach(async () => {
    measureServiceSpy = {
      getLocalMeasuresAsInfo: jest.fn(),
    };
    evaluationServiceSpy = {
      getAllResults: jest.fn(),
      getDefaultEvaluationPreset: jest.fn(),
      calculateLocalMeasure: jest.fn(),
    };
    patientServiceSpy = {
      getPatientsSummaryCached: jest.fn(),
    };
    loggerContextSpy = {
      error: jest.fn(),
      warn: jest.fn(),
      info: jest.fn(),
    };
    loggerServiceSpy = {
      withContext: jest.fn().mockReturnValue(loggerContextSpy),
    };

    measureServiceSpy.getLocalMeasuresAsInfo.mockReturnValue(of([
      {
        id: 'CDC',
        name: 'CDC',
        version: '1.0.0',
        description: 'Comprehensive Diabetes Care',
        category: 'CHRONIC_DISEASE',
        displayName: 'CDC - Comprehensive Diabetes Care (v1.0.0)',
      },
    ]));

    evaluationServiceSpy.getAllResults.mockReturnValue(of([
      {
        id: 'result-1',
        tenantId: 'acme-health',
        patientId: 'patient-1',
        measureId: 'CDC',
        measureName: 'Comprehensive Diabetes Care',
        measureCategory: 'HEDIS',
        measureYear: 2026,
        numeratorCompliant: false,
        denominatorEligible: true,
        complianceRate: 0,
        score: 0,
        calculationDate: '2026-02-24T00:00:00Z',
        createdAt: '2026-02-24T00:00:00Z',
        createdBy: 'system',
        version: 1,
      },
    ]));

    evaluationServiceSpy.getDefaultEvaluationPreset.mockReturnValue(of({ patientId: 'preset-patient' } as any));
    patientServiceSpy.getPatientsSummaryCached.mockReturnValue(of([
      {
        id: 'preset-patient',
        fullName: 'Jane Doe',
        mrn: 'MRN-001',
        status: 'Active',
      },
      {
        id: 'patient-2',
        fullName: 'John Smith',
        mrn: 'MRN-002',
        status: 'Active',
      },
    ]));

    await TestBed.configureTestingModule({
      imports: [QualityMeasuresComponent, HttpClientTestingModule],
      providers: [
        { provide: MeasureService, useValue: measureServiceSpy },
        { provide: EvaluationService, useValue: evaluationServiceSpy },
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        { provide: MatDialog, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityMeasuresComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushFhirPatientCount(): void {
    const req = httpMock.expectOne('/fhir/Patient?_count=1');
    req.flush({
      total: 12,
      entry: [{ resource: { id: 'fhir-patient-1' } }],
    });
  }

  it('loads measures and maps backend results on init', () => {
    fixture.detectChanges();
    flushFhirPatientCount();

    expect(component.measures().length).toBe(1);
    expect(component.measures()[0].code).toBe('CDC');
    expect(component.measures()[0].careGaps).toBe(1);
    expect(component.totalPatients()).toBe(12);
    expect(component.evaluationPatientId()).toBe('preset-patient');
    expect(component.patientSearchTerm()).toContain('Jane Doe');
  });

  it('runs evaluation via EvaluationService.calculateLocalMeasure', async () => {
    evaluationServiceSpy.calculateLocalMeasure.mockReturnValue(
      of({
        measureId: 'CDC',
        measureName: 'Comprehensive Diabetes Care',
        patientId: 'preset-patient',
        eligible: true,
        denominatorMembership: true,
        denominatorExclusion: false,
        subMeasures: {},
        careGaps: [],
        recommendations: [],
        calculatedAt: '2026-02-24T00:00:00Z',
      })
    );

    fixture.detectChanges();
    flushFhirPatientCount();

    component.selectMeasure(component.measures()[0]);
    await component.runEvaluation();

    expect(evaluationServiceSpy.calculateLocalMeasure).toHaveBeenCalledWith('preset-patient', 'CDC');
    expect(component.evaluationResult()?.measureCode).toBe('CDC');
    expect(component.evaluationResult()?.careGapsCount).toBe(0);
  });

  it('selects patient context from autocomplete option', () => {
    fixture.detectChanges();
    flushFhirPatientCount();

    const patient = component.patients()[1];
    component.selectEvaluationPatient(patient);

    expect(component.evaluationPatientId()).toBe('patient-2');
    expect(component.patientSearchTerm()).toContain('John Smith');
  });
});
