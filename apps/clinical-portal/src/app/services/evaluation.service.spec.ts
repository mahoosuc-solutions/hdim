import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { EvaluationService } from './evaluation.service';
import { EvaluationFactory } from '../../testing/factories/evaluation.factory';
import {
  buildCqlEngineUrl,
  buildQualityMeasureUrl,
  CQL_ENGINE_ENDPOINTS,
  QUALITY_MEASURE_ENDPOINTS,
} from '../config/api.config';

describe('EvaluationService', () => {
  let service: EvaluationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EvaluationService],
    });

    service = TestBed.inject(EvaluationService);
    httpMock = TestBed.inject(HttpTestingController);
    EvaluationFactory.reset();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('CQL Engine Evaluation Endpoints', () => {
    describe('submitEvaluation', () => {
      it('should submit a single patient evaluation', () => {
        const mockEvaluation = EvaluationFactory.createSuccessfulEvaluation();
        const libraryId = 'lib-1';
        const patientId = 'patient-001';
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          libraryId,
          patientId,
        });

        service.submitEvaluation(libraryId, patientId).subscribe((evaluation) => {
          expect(evaluation).toEqual(mockEvaluation);
          expect(evaluation.status).toBe('SUCCESS');
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({});
        req.flush(mockEvaluation);
      });

      it('should submit evaluation with context data', () => {
        const mockEvaluation = EvaluationFactory.createSuccessfulEvaluation();
        const contextData = { measurementPeriod: '2024' };
        const libraryId = 'lib-1';
        const patientId = 'patient-001';

        service
          .submitEvaluation(libraryId, patientId, contextData)
          .subscribe((evaluation) => {
            expect(evaluation).toEqual(mockEvaluation);
          });

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
            libraryId,
            patientId,
          })
        );
        expect(req.request.body).toEqual(contextData);
        req.flush(mockEvaluation);
      });

      it('should handle failed evaluation', () => {
        const failedEvaluation = EvaluationFactory.createFailedEvaluation();
        const libraryId = 'lib-1';
        const patientId = 'patient-001';

        service.submitEvaluation(libraryId, patientId).subscribe((evaluation) => {
          expect(evaluation.status).toBe('FAILED');
          expect(evaluation.errorMessage).toBeTruthy();
        });

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
            libraryId,
            patientId,
          })
        );
        req.flush(failedEvaluation);
      });
    });

    describe('batchEvaluate', () => {
      it('should batch evaluate multiple patients', () => {
        const batchResponse = EvaluationFactory.createBatchResponse(5);
        const libraryId = 'lib-1';
        const patientIds = ['p1', 'p2', 'p3', 'p4', 'p5'];
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH, {
          libraryId,
        });

        service.batchEvaluate(libraryId, patientIds).subscribe((response) => {
          expect(response.totalPatients).toBe(5);
          expect(response.successfulEvaluations).toBe(5);
          expect(response.evaluations.length).toBe(5);
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(patientIds);
        req.flush(batchResponse);
      });

      it('should handle partial batch failures', () => {
        const batchResponse = {
          totalPatients: 3,
          successfulEvaluations: 2,
          failedEvaluations: 1,
          evaluations: [
            EvaluationFactory.createEvaluationResponse({ status: 'SUCCESS' }),
            EvaluationFactory.createEvaluationResponse({ status: 'SUCCESS' }),
            EvaluationFactory.createEvaluationResponse({
              status: 'FAILED',
              error: 'Patient data incomplete',
            }),
          ],
        };

        service.batchEvaluate('lib-1', ['p1', 'p2', 'p3']).subscribe((response) => {
          expect(response.successfulEvaluations).toBe(2);
          expect(response.failedEvaluations).toBe(1);
        });

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH, {
            libraryId: 'lib-1',
          })
        );
        req.flush(batchResponse);
      });
    });

    describe('getPatientEvaluations', () => {
      it('should fetch all evaluations for a patient', () => {
        const evaluations = [
          EvaluationFactory.createCqlEvaluation({ patientId: 'patient-001' }),
          EvaluationFactory.createCqlEvaluation({ patientId: 'patient-001' }),
        ];
        const patientId = 'patient-001';
        const expectedUrl = buildCqlEngineUrl(
          CQL_ENGINE_ENDPOINTS.EVALUATIONS_BY_PATIENT(patientId)
        );

        service.getPatientEvaluations(patientId).subscribe((results) => {
          expect(results.length).toBe(2);
          expect(results.every((e) => e.patientId === patientId)).toBe(true);
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(evaluations);
      });
    });

    describe('getLibraryEvaluations', () => {
      it('should fetch all evaluations for a library', () => {
        const evaluations = [
          EvaluationFactory.createCqlEvaluation({ library: { id: 'lib-1', name: 'HEDIS-CDC', version: '1.0.0' } }),
          EvaluationFactory.createCqlEvaluation({ library: { id: 'lib-1', name: 'HEDIS-CDC', version: '1.0.0' } }),
        ];
        const libraryId = 'lib-1';
        const expectedUrl = buildCqlEngineUrl(
          CQL_ENGINE_ENDPOINTS.EVALUATIONS_BY_LIBRARY(libraryId)
        );

        service.getLibraryEvaluations(libraryId).subscribe((results) => {
          expect(results.length).toBe(2);
          expect(results.every((e) => e.library?.id === libraryId)).toBe(true);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      });
    });

    describe('getAllEvaluations', () => {
      it('should fetch all evaluations with default pagination', () => {
        const evaluations = Array.from({ length: 20 }, () =>
          EvaluationFactory.createCqlEvaluation()
        );
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '0',
          size: '1000',
        });

        service.getAllEvaluations().subscribe((results) => {
          expect(results.length).toBe(20);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      });

      it('should fetch evaluations with custom pagination', () => {
        const evaluations = Array.from({ length: 10 }, () =>
          EvaluationFactory.createCqlEvaluation()
        );
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '2',
          size: '10',
        });

        service.getAllEvaluations(2, 10).subscribe((results) => {
          expect(results.length).toBe(10);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      });
    });

    describe('evaluateSimple', () => {
      it('should evaluate with simple endpoint', () => {
        const result = { InDenominator: true, InNumerator: true };
        const libraryName = 'HEDIS-CDC';
        const patientId = 'patient-001';
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATE_SIMPLE, {
          library: libraryName,
          patient: patientId,
        });

        service.evaluateSimple(libraryName, patientId).subscribe((response) => {
          expect(response).toEqual(result);
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        req.flush(result);
      });

      it('should pass parameters to simple evaluation', () => {
        const params = { measurementYear: 2024 };
        const libraryName = 'HEDIS-CDC';
        const patientId = 'patient-001';

        service.evaluateSimple(libraryName, patientId, params).subscribe();

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATE_SIMPLE, {
            library: libraryName,
            patient: patientId,
          })
        );
        expect(req.request.body).toEqual(params);
        req.flush({});
      });
    });
  });

  describe('Quality Measure Service Endpoints', () => {
    describe('calculateQualityMeasure', () => {
      it('should calculate quality measure for a patient', () => {
        const result = EvaluationFactory.createCompliantResult();
        const patientId = 'patient-001';
        const measureId = 'lib-1';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE, {
          patient: patientId,
          measure: measureId,
          createdBy: 'system',
        });

        service.calculateQualityMeasure(patientId, measureId).subscribe((response) => {
          expect(response).toEqual(result);
          expect(response.numeratorCompliant).toBe(true);
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        req.flush(result);
      });

      it('should use custom createdBy parameter', () => {
        const result = EvaluationFactory.createCompliantResult();
        const patientId = 'patient-001';
        const measureId = 'lib-1';
        const createdBy = 'user-123';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE, {
          patient: patientId,
          measure: measureId,
          createdBy,
        });

        service
          .calculateQualityMeasure(patientId, measureId, createdBy)
          .subscribe((response) => {
            expect(response.createdBy).toBe('system'); // Backend returns its own value
          });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(result);
      });

      it('should handle non-compliant result', () => {
        const result = EvaluationFactory.createNonCompliantResult();

        service.calculateQualityMeasure('patient-002', 'lib-1').subscribe((response) => {
          expect(response.numeratorCompliant).toBe(false);
          expect(response.denominatorEligible).toBe(true);
        });

        const req = httpMock.expectOne((request) =>
          request.url.includes(QUALITY_MEASURE_ENDPOINTS.CALCULATE)
        );
        req.flush(result);
      });

      it('should handle not eligible result', () => {
        const result = EvaluationFactory.createNotEligibleResult();

        service.calculateQualityMeasure('patient-003', 'lib-1').subscribe((response) => {
          expect(response.denominatorEligible).toBe(false);
          expect(response.numeratorCompliant).toBe(false);
        });

        const req = httpMock.expectOne((request) =>
          request.url.includes(QUALITY_MEASURE_ENDPOINTS.CALCULATE)
        );
        req.flush(result);
      });
    });

    describe('getPatientResults', () => {
      it('should fetch all quality measure results for a patient', () => {
        const results = [
          EvaluationFactory.createCompliantResult(),
          EvaluationFactory.createNonCompliantResult(),
        ];
        const patientId = 'patient-001';
        const expectedUrl = buildQualityMeasureUrl(
          QUALITY_MEASURE_ENDPOINTS.RESULTS_BY_PATIENT,
          { patient: patientId }
        );

        service.getPatientResults(patientId).subscribe((response) => {
          expect(response.length).toBe(2);
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(results);
      });
    });

    describe('getQualityScore', () => {
      it('should fetch quality score for a patient', () => {
        const score = EvaluationFactory.createQualityScore();
        const patientId = 'patient-001';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.QUALITY_SCORE, {
          patient: patientId,
        });

        service.getQualityScore(patientId).subscribe((response) => {
          expect(response).toEqual(score);
          expect(response.scorePercentage).toBe(80.0);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(score);
      });
    });

    describe('getPatientReport', () => {
      it('should fetch patient quality report', () => {
        const report = EvaluationFactory.createPatientReport();
        const patientId = 'patient-001';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_REPORT, {
          patient: patientId,
        });

        service.getPatientReport(patientId).subscribe((response) => {
          expect(response).toEqual(report);
          expect(response.patientId).toBe('patient-001');
          expect(response.measureResults.length).toBeGreaterThan(0);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(report);
      });
    });

    describe('getPopulationReport', () => {
      it('should fetch population quality report', () => {
        const report = EvaluationFactory.createPopulationReport();
        const year = 2024;
        const expectedUrl = buildQualityMeasureUrl(
          QUALITY_MEASURE_ENDPOINTS.POPULATION_REPORT,
          { year: year.toString() }
        );

        service.getPopulationReport(year).subscribe((response) => {
          expect(response).toEqual(report);
          expect(response.year).toBe(2024);
          expect(response.measureSummaries.length).toBeGreaterThan(0);
        });

        const req = httpMock.expectOne(expectedUrl);
        req.flush(report);
      });
    });

    describe('checkHealth', () => {
      it('should check Quality Measure Service health', () => {
        const healthResponse = { status: 'UP', timestamp: '2024-01-15T10:00:00Z' };
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.HEALTH);

        service.checkHealth().subscribe((response) => {
          expect(response.status).toBe('UP');
        });

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(healthResponse);
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', () => {
      service.submitEvaluation('lib-1', 'patient-001').subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error).toBeTruthy();
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(CQL_ENGINE_ENDPOINTS.EVALUATIONS)
      );
      req.error(new ProgressEvent('Network error'));
    });

    it('should handle 500 errors', () => {
      service.calculateQualityMeasure('patient-001', 'lib-1').subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(QUALITY_MEASURE_ENDPOINTS.CALCULATE)
      );
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });
  });
});
