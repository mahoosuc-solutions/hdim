import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EvaluationService } from './evaluation.service';
import { AuditService } from './audit.service';
import { buildQualityMeasureUrl, QUALITY_MEASURE_ENDPOINTS } from '../config/api.config';
import { LocalMeasureResult, QualityMeasureResult } from '../models/quality-result.model';

describe('EvaluationService', () => {
  let service: EvaluationService;
  let httpMock: HttpTestingController;
  let auditServiceMock: jasmine.SpyObj<AuditService>;

  beforeEach(() => {
    auditServiceMock = jasmine.createSpyObj<AuditService>('AuditService', [
      'logEvaluation',
      'logBatchEvaluationStart',
      'logBatchEvaluationComplete',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        EvaluationService,
        { provide: AuditService, useValue: auditServiceMock },
      ],
    });

    service = TestBed.inject(EvaluationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call calculate-local endpoint for local evaluations', () => {
    const patientId = 'patient-123';
    const measureId = 'CDC';
    const mockResponse: LocalMeasureResult = {
      measureId,
      measureName: 'CDC',
      patientId,
      eligible: true,
      denominatorMembership: true,
      denominatorExclusion: false,
      subMeasures: {},
      careGaps: [],
      recommendations: [],
      calculatedAt: '2026-02-24T00:00:00Z',
    };

    service.calculateLocalMeasure(patientId, measureId).subscribe((result) => {
      expect(result.measureId).toBe(measureId);
      expect(result.patientId).toBe(patientId);
    });

    const req = httpMock.expectOne(
      buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE_LOCAL, {
        patient: patientId,
        measure: measureId,
      })
    );

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockResponse);
  });

  it('should call calculate endpoint for quality measure calculation', () => {
    const patientId = 'patient-abc';
    const measureId = 'BCS';

    const mockResponse: QualityMeasureResult = {
      id: 'result-1',
      tenantId: 'acme-health',
      patientId,
      measureId,
      measureName: 'Breast Cancer Screening',
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
    };

    service.calculateQualityMeasure(patientId, measureId).subscribe((result) => {
      expect(result.id).toBe('result-1');
      expect(result.measureId).toBe(measureId);
    });

    const req = httpMock.expectOne(
      buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE, {
        patient: patientId,
        measure: measureId,
        createdBy: 'system',
      })
    );

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockResponse);
  });

  it('should return empty results array when /results fails', () => {
    service.getPatientResults('patient-failure').subscribe((results) => {
      expect(results).toEqual([]);
    });

    const req = httpMock.expectOne(
      buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.RESULTS_BY_PATIENT, {
        patient: 'patient-failure',
      })
    );

    expect(req.request.method).toBe('GET');
    req.flush('error', { status: 500, statusText: 'Server Error' });
  });
});
