import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { QualityMeasureDetailComponent } from './quality-measure-detail.component';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { LoggerService } from '../../services/logger.service';

describe('QualityMeasureDetailComponent', () => {
  let component: QualityMeasureDetailComponent;
  let fixture: ComponentFixture<QualityMeasureDetailComponent>;
  let httpMock: HttpTestingController;
  let measureServiceSpy: any;
  let evaluationServiceSpy: any;
  let loggerSpy: any;

  beforeEach(async () => {
    measureServiceSpy = {
      getLocalMeasuresAsInfo: jest.fn(),
    };
    evaluationServiceSpy = {
      getAllResults: jest.fn(),
    };
    loggerSpy = {
      info: jest.fn(),
      error: jest.fn(),
      warn: jest.fn(),
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
        calculationDate: '2026-02-10T00:00:00Z',
        createdAt: '2026-02-10T00:00:00Z',
        createdBy: 'system',
        version: 1,
      },
      {
        id: 'result-2',
        tenantId: 'acme-health',
        patientId: 'patient-2',
        measureId: 'CDC',
        measureName: 'Comprehensive Diabetes Care',
        measureCategory: 'HEDIS',
        measureYear: 2026,
        numeratorCompliant: true,
        denominatorEligible: true,
        complianceRate: 100,
        score: 100,
        calculationDate: '2026-02-24T00:00:00Z',
        createdAt: '2026-02-24T00:00:00Z',
        createdBy: 'system',
        version: 1,
      },
    ]));

    await TestBed.configureTestingModule({
      imports: [QualityMeasureDetailComponent, HttpClientTestingModule],
      providers: [
        { provide: MeasureService, useValue: measureServiceSpy },
        { provide: EvaluationService, useValue: evaluationServiceSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: 'CDC' }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityMeasureDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads measure detail, patient results, and care gaps from backend services', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('/care-gap/api/v1/care-gaps');
    req.flush({
      content: [
        {
          id: 'gap-1',
          patientId: 'patient-1',
          measureId: 'CDC',
          gapDescription: 'Missing A1c follow-up',
          priority: 'high',
          gapStatus: 'open',
          dueDate: '2026-01-01',
        },
      ],
    });

    expect(component.loading).toBe(false);
    expect(component.error).toBeNull();
    expect(component.measure?.code).toBe('CDC');
    expect(component.patientResults.length).toBe(2);
    expect(component.careGaps.length).toBe(1);
    expect(component.historicalTrends.length).toBeGreaterThan(0);
  });

  it('shows not found error when requested measure does not exist', () => {
    measureServiceSpy.getLocalMeasuresAsInfo.mockReturnValue(of([]));

    fixture = TestBed.createComponent(QualityMeasureDetailComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();

    const req = httpMock.expectOne('/care-gap/api/v1/care-gaps');
    req.flush({ content: [] });

    expect(component.loading).toBe(false);
    expect(component.error).toContain('not found');
  });
});
