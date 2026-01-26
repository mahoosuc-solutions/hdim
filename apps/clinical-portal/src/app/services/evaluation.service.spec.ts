import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { of } from 'rxjs';
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
    }, 30000);
  }, 30000);

  describe('CQL Engine Evaluation Endpoints', () => {
    describe('submitEvaluation', () => {
      it('should submit a single patient evaluation', () => {
        const mockEvaluation = EvaluationFactory.createSuccessfulEvaluation();
        const libraryId = 'lib-1';
        const patientId = 'patient-001';
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          libraryId,
          patientId,
        }, 30000);

        service.submitEvaluation(libraryId, patientId).subscribe((evaluation) => {
          expect(evaluation).toEqual(mockEvaluation);
          expect(evaluation.status).toBe('SUCCESS');
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({}, 30000);
        req.flush(mockEvaluation);
      }, 30000);

      it('should submit evaluation with context data', () => {
        const mockEvaluation = EvaluationFactory.createSuccessfulEvaluation();
        const contextData = { measurementPeriod: '2024' };
        const libraryId = 'lib-1';
        const patientId = 'patient-001';

        service
          .submitEvaluation(libraryId, patientId, contextData)
          .subscribe((evaluation) => {
            expect(evaluation).toEqual(mockEvaluation);
          }, 30000);

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
            libraryId,
            patientId,
          })
        );
        expect(req.request.body).toEqual(contextData);
        req.flush(mockEvaluation);
      }, 30000);

      it('should handle failed evaluation', () => {
        const failedEvaluation = EvaluationFactory.createFailedEvaluation();
        const libraryId = 'lib-1';
        const patientId = 'patient-001';

        service.submitEvaluation(libraryId, patientId).subscribe((evaluation) => {
          expect(evaluation.status).toBe('FAILED');
          expect(evaluation.errorMessage).toBeTruthy();
        }, 30000);

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
            libraryId,
            patientId,
          })
        );
        req.flush(failedEvaluation);
      }, 30000);
    }, 30000);

    describe('batchEvaluate', () => {
      it('should batch evaluate multiple patients', () => {
        const batchResponse = EvaluationFactory.createBatchResponse(5);
        const libraryId = 'lib-1';
        const patientIds = ['p1', 'p2', 'p3', 'p4', 'p5'];
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH, {
          libraryId,
        }, 30000);

        service.batchEvaluate(libraryId, patientIds).subscribe((response) => {
          expect(response.totalPatients).toBe(5);
          expect(response.successfulEvaluations).toBe(5);
          expect(response.evaluations.length).toBe(5);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(patientIds);
        req.flush(batchResponse);
      }, 30000);

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
        }, 30000);

        const req = httpMock.expectOne(
          buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH, {
            libraryId: 'lib-1',
          })
        );
        req.flush(batchResponse);
      }, 30000);
    }, 30000);

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
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(evaluations);
      }, 30000);
    }, 30000);

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
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      }, 30000);
    }, 30000);

    describe('getAllEvaluations', () => {
      it('should fetch all evaluations with default pagination', () => {
        const evaluations = Array.from({ length: 20 }, () =>
          EvaluationFactory.createCqlEvaluation()
        );
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '0',
          size: '1000',
        }, 30000);

        service.getAllEvaluations().subscribe((results) => {
          expect(results.length).toBe(20);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      }, 30000);

      it('should map Spring Data REST responses with content', () => {
        const evaluations = [
          EvaluationFactory.createCqlEvaluation(),
          EvaluationFactory.createCqlEvaluation(),
        ];
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '0',
          size: '1000',
        }, 30000);

        service.getAllEvaluations().subscribe((results) => {
          expect(results).toEqual(evaluations);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush({ content: evaluations }, 30000);
      }, 30000);

      it('should return empty array for unexpected response', () => {
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '0',
          size: '1000',
        }, 30000);

        service.getAllEvaluations().subscribe((results) => {
          expect(results).toEqual([]);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush({ message: 'unexpected' }, 30000);
      }, 30000);

      it('should fetch evaluations with custom pagination', () => {
        const evaluations = Array.from({ length: 10 }, () =>
          EvaluationFactory.createCqlEvaluation()
        );
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '2',
          size: '10',
        }, 30000);

        service.getAllEvaluations(2, 10).subscribe((results) => {
          expect(results.length).toBe(10);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);
      }, 30000);
    }, 30000);

    describe('evaluateSimple', () => {
      it('should evaluate with simple endpoint', () => {
        const result = { InDenominator: true, InNumerator: true };
        const libraryName = 'HEDIS-CDC';
        const patientId = 'patient-001';
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATE_SIMPLE, {
          library: libraryName,
          patient: patientId,
        }, 30000);

        service.evaluateSimple(libraryName, patientId).subscribe((response) => {
          expect(response).toEqual(result);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        req.flush(result);
      }, 30000);

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
        req.flush({}, 30000);
      }, 30000);
    }, 30000);

    describe('cached evaluations', () => {
      it('should cache evaluations within TTL', () => {
        const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(1000);
        const evaluations = [EvaluationFactory.createCqlEvaluation()];
        const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.EVALUATIONS, {
          page: '0',
          size: '1000',
        }, 30000);

        service.getAllEvaluationsCached().subscribe((results) => {
          expect(results.length).toBe(1);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(evaluations);

        service.getAllEvaluationsCached().subscribe((results) => {
          expect(results.length).toBe(1);
        }, 30000);

        httpMock.expectNone(expectedUrl);
        nowSpy.mockRestore();
      }, 30000);

      it('should filter recent evaluations and limit results', () => {
        const recent = new Date();
        const old = new Date();
        old.setDate(old.getDate() - 40);

        const evaluations = [
          { ...EvaluationFactory.createCqlEvaluation(), createdAt: recent.toISOString() },
          { ...EvaluationFactory.createCqlEvaluation(), createdAt: old.toISOString() },
        ];

        const cacheSpy = jest
          .spyOn(service, 'getAllEvaluationsCached')
          .mockReturnValue(of(evaluations));

        service.getRecentEvaluations(30, 1).subscribe((results) => {
          expect(results.length).toBe(1);
          expect(new Date(results[0].createdAt).getTime()).toBeGreaterThanOrEqual(
            old.getTime()
          );
        }, 30000);

        cacheSpy.mockRestore();
      }, 30000);

      it('should calculate evaluation stats with empty data', () => {
        const cacheSpy = jest.spyOn(service, 'getAllEvaluationsCached').mockReturnValue(of([]));

        service.getEvaluationStats().subscribe((stats) => {
          expect(stats.total).toBe(0);
          expect(stats.last30Days).toBe(0);
          expect(stats.successRate).toBe(0);
        }, 30000);

        cacheSpy.mockRestore();
      }, 30000);
    }, 30000);
  }, 30000);

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
        }, 30000);

        service.calculateQualityMeasure(patientId, measureId).subscribe((response) => {
          expect(response).toEqual(result);
          expect(response.numeratorCompliant).toBe(true);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('POST');
        req.flush(result);
      }, 30000);

      it('should use custom createdBy parameter', () => {
        const result = EvaluationFactory.createCompliantResult();
        const patientId = 'patient-001';
        const measureId = 'lib-1';
        const createdBy = 'user-123';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CALCULATE, {
          patient: patientId,
          measure: measureId,
          createdBy,
        }, 30000);

        service
          .calculateQualityMeasure(patientId, measureId, createdBy)
          .subscribe((response) => {
            expect(response.createdBy).toBe('system'); // Backend returns its own value
          }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(result);
      }, 30000);

      it('should handle non-compliant result', () => {
        const result = EvaluationFactory.createNonCompliantResult();

        service.calculateQualityMeasure('patient-002', 'lib-1').subscribe((response) => {
          expect(response.numeratorCompliant).toBe(false);
          expect(response.denominatorEligible).toBe(true);
        }, 30000);

        const req = httpMock.expectOne((request) =>
          request.url.includes(QUALITY_MEASURE_ENDPOINTS.CALCULATE)
        );
        req.flush(result);
      }, 30000);

      it('should handle not eligible result', () => {
        const result = EvaluationFactory.createNotEligibleResult();

        service.calculateQualityMeasure('patient-003', 'lib-1').subscribe((response) => {
          expect(response.denominatorEligible).toBe(false);
          expect(response.numeratorCompliant).toBe(false);
        }, 30000);

        const req = httpMock.expectOne((request) =>
          request.url.includes(QUALITY_MEASURE_ENDPOINTS.CALCULATE)
        );
        req.flush(result);
      }, 30000);
    }, 30000);

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
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(results);
      }, 30000);

      it('should fetch all results without patient filter', () => {
        const expectedUrl = buildQualityMeasureUrl(
          QUALITY_MEASURE_ENDPOINTS.RESULTS_BY_PATIENT,
          { page: '1', size: '25' }
        );

        service.getPatientResults(null, 1, 25).subscribe();

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush([]);
      }, 30000);
    }, 30000);

    describe('getQualityScore', () => {
      it('should fetch quality score for a patient', () => {
        const score = EvaluationFactory.createQualityScore();
        const patientId = 'patient-001';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.QUALITY_SCORE, {
          patient: patientId,
        }, 30000);

        service.getQualityScore(patientId).subscribe((response) => {
          expect(response).toEqual(score);
          expect(response.scorePercentage).toBe(80.0);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(score);
      }, 30000);
    }, 30000);

    describe('getPatientReport', () => {
      it('should fetch patient quality report', () => {
        const report = EvaluationFactory.createPatientReport();
        const patientId = 'patient-001';
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_REPORT, {
          patient: patientId,
        }, 30000);

        service.getPatientReport(patientId).subscribe((response) => {
          expect(response).toEqual(report);
          expect(response.patientId).toBe('patient-001');
          expect(response.measureResults.length).toBeGreaterThan(0);
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(report);
      }, 30000);
    }, 30000);

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
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        req.flush(report);
      }, 30000);
    }, 30000);

    describe('checkHealth', () => {
      it('should check Quality Measure Service health', () => {
        const healthResponse = { status: 'UP', timestamp: '2024-01-15T10:00:00Z' };
        const expectedUrl = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.HEALTH);

        service.checkHealth().subscribe((response) => {
          expect(response.status).toBe('UP');
        }, 30000);

        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush(healthResponse);
      }, 30000);
    }, 30000);
  }, 30000);

  describe('Saved Reports', () => {
    it('should fetch saved reports with and without type', () => {
      service.getSavedReports().subscribe();
      service.getSavedReports('PATIENT' as any).subscribe();

      const allReq = httpMock.expectOne(
        buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS, undefined)
      );
      allReq.flush([]);

      const typeReq = httpMock.expectOne(
        buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SAVED_REPORTS, { type: 'PATIENT' })
      );
      typeReq.flush([]);
    }, 30000);

    it('should export and download report as CSV', (done) => {
      const blob = new Blob(['csv']);
      const exportSpy = jest.spyOn(service, 'exportReportToCsv').mockReturnValue(of(blob));
      const downloadSpy = jest.spyOn(service, 'downloadReport').mockImplementation(() => undefined);

      service.exportAndDownloadReport('report-1', 'report-name', 'csv').subscribe({
        next: () => {
          expect(downloadSpy).toHaveBeenCalledWith(blob, 'report-name.csv');
          exportSpy.mockRestore();
          downloadSpy.mockRestore();
          done();
        },
        error: done.fail,
      }, 30000);
    });

    it('should export and download report as Excel', (done) => {
      const blob = new Blob(['xlsx']);
      const exportSpy = jest.spyOn(service, 'exportReportToExcel').mockReturnValue(of(blob));
      const downloadSpy = jest.spyOn(service, 'downloadReport').mockImplementation(() => undefined);

      service.exportAndDownloadReport('report-2', 'report-name', 'xlsx').subscribe({
        next: () => {
          expect(downloadSpy).toHaveBeenCalledWith(blob, 'report-name.xlsx');
          exportSpy.mockRestore();
          downloadSpy.mockRestore();
          done();
        },
        error: done.fail,
      }, 30000);
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
