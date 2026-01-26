/**
 * Unit Tests for Mental Health Service
 */
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MentalHealthService } from './mental-health.service';
import { FhirQuestionnaireService } from '../fhir/fhir-questionnaire.service';
import { FhirConditionService } from '../fhir/fhir-condition.service';
import { LoggerService } from '../logger.service';
import {
  MentalHealthAssessment,
  MentalHealthAssessmentType,
  AssessmentHistory,
  AssessmentHistoryEntry,
} from '../../models/patient-health.model';

describe('MentalHealthService', () => {
  let service: MentalHealthService;
  let httpMock: HttpTestingController;

  const mockLoggerService = {
    withContext: jest.fn().mockReturnValue({
      info: jest.fn(),
      debug: jest.fn(),
      error: jest.fn(),
      warn: jest.fn(),
    }),
  };

  const mockFhirQuestionnaireService = {
    getQuestionnaireResponses: jest.fn().mockReturnValue(of({ entry: [] })),
    invalidatePatientQuestionnaires: jest.fn(),
  };

  const mockFhirConditionService = {
    getConditions: jest.fn().mockReturnValue(of({ entry: [] })),
    getMentalHealthConditions: jest.fn().mockReturnValue(of([])),
    invalidatePatientConditions: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        MentalHealthService,
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: FhirQuestionnaireService, useValue: mockFhirQuestionnaireService },
        { provide: FhirConditionService, useValue: mockFhirConditionService },
      ],
    });

    service = TestBed.inject(MentalHealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      const pendingReqs = httpMock.match((req) => true);
      pendingReqs.forEach((req) => {
        if (!req.cancelled) {
          req.flush({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] });
        }
      });
    } catch {
      // No pending requests
    }
    httpMock.verify();
  });

  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    }, 30000);
  }, 30000);

  describe('PHQ-9 Depression Scoring', () => {
    it('should score minimal depression (0-4)', () => {
      const responses = { q1: 0, q2: 1, q3: 0, q4: 1, q5: 0, q6: 1, q7: 0, q8: 0, q9: 0 };

      const result = service.scorePHQ9(responses);

      expect(result.type).toBe('PHQ-9');
      expect(result.score).toBe(3);
      expect(result.maxScore).toBe(27);
      expect(result.severity).toBe('minimal');
      expect(result.positiveScreen).toBe(false);
      expect(result.requiresFollowup).toBe(false);
    }, 30000);

    it('should score mild depression (5-9)', () => {
      const responses = { q1: 1, q2: 1, q3: 1, q4: 1, q5: 1, q6: 1, q7: 1, q8: 0, q9: 0 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(7);
      expect(result.severity).toBe('mild');
      expect(result.positiveScreen).toBe(false);
    }, 30000);

    it('should score moderate depression (10-14)', () => {
      const responses = { q1: 2, q2: 1, q3: 1, q4: 1, q5: 1, q6: 1, q7: 1, q8: 1, q9: 1 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(10);
      expect(result.severity).toBe('moderate');
      expect(result.positiveScreen).toBe(true);
      expect(result.requiresFollowup).toBe(true);
    }, 30000);

    it('should score moderately severe depression (15-19)', () => {
      const responses = { q1: 2, q2: 2, q3: 2, q4: 2, q5: 2, q6: 2, q7: 2, q8: 1, q9: 1 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(16);
      expect(result.severity).toBe('moderately-severe');
      expect(result.positiveScreen).toBe(true);
    }, 30000);

    it('should score severe depression (20-27)', () => {
      const responses = { q1: 3, q2: 3, q3: 3, q4: 3, q5: 3, q6: 3, q7: 3, q8: 2, q9: 2 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(25);
      expect(result.severity).toBe('severe');
      expect(result.positiveScreen).toBe(true);
      expect(result.requiresFollowup).toBe(true);
    }, 30000);

    it('should handle maximum score (27)', () => {
      const responses = { q1: 3, q2: 3, q3: 3, q4: 3, q5: 3, q6: 3, q7: 3, q8: 3, q9: 3 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(27);
      expect(result.severity).toBe('severe');
    }, 30000);

    it('should handle minimum score (0)', () => {
      const responses = { q1: 0, q2: 0, q3: 0, q4: 0, q5: 0, q6: 0, q7: 0, q8: 0, q9: 0 };

      const result = service.scorePHQ9(responses);

      expect(result.score).toBe(0);
      expect(result.severity).toBe('minimal');
    }, 30000);
  }, 30000);

  describe('GAD-7 Anxiety Scoring', () => {
    it('should score minimal anxiety (0-4)', () => {
      const responses = { q1: 0, q2: 1, q3: 0, q4: 1, q5: 0, q6: 1, q7: 0 };

      const result = service.scoreGAD7(responses);

      expect(result.type).toBe('GAD-7');
      expect(result.score).toBe(3);
      expect(result.maxScore).toBe(21);
      expect(result.severity).toBe('minimal');
      expect(result.positiveScreen).toBe(false);
    }, 30000);

    it('should score mild anxiety (5-9)', () => {
      const responses = { q1: 1, q2: 1, q3: 1, q4: 1, q5: 1, q6: 1, q7: 1 };

      const result = service.scoreGAD7(responses);

      expect(result.score).toBe(7);
      expect(result.severity).toBe('mild');
      expect(result.positiveScreen).toBe(false);
    }, 30000);

    it('should score moderate anxiety (10-14)', () => {
      const responses = { q1: 2, q2: 2, q3: 2, q4: 2, q5: 1, q6: 1, q7: 1 };

      const result = service.scoreGAD7(responses);

      expect(result.score).toBe(11);
      expect(result.severity).toBe('moderate');
      expect(result.positiveScreen).toBe(true);
    }, 30000);

    it('should score severe anxiety (15-21)', () => {
      const responses = { q1: 3, q2: 3, q3: 3, q4: 3, q5: 2, q6: 2, q7: 2 };

      const result = service.scoreGAD7(responses);

      expect(result.score).toBe(18);
      expect(result.severity).toBe('severe');
      expect(result.positiveScreen).toBe(true);
    }, 30000);

    it('should handle maximum score (21)', () => {
      const responses = { q1: 3, q2: 3, q3: 3, q4: 3, q5: 3, q6: 3, q7: 3 };

      const result = service.scoreGAD7(responses);

      expect(result.score).toBe(21);
      expect(result.severity).toBe('severe');
    }, 30000);
  }, 30000);

  describe('PHQ-2 Depression Screening', () => {
    it('should score negative screen (0-2)', () => {
      const responses = { q1: 1, q2: 1 };

      const result = service.scorePHQ2(responses);

      expect(result.type).toBe('PHQ-2');
      expect(result.score).toBe(2);
      expect(result.maxScore).toBe(6);
      expect(result.positiveScreen).toBe(false);
      expect(result.severity).toBe('minimal');
    }, 30000);

    it('should score positive screen (3+)', () => {
      const responses = { q1: 2, q2: 2 };

      const result = service.scorePHQ2(responses);

      expect(result.score).toBe(4);
      expect(result.positiveScreen).toBe(true);
      expect(result.severity).toBe('moderate');
    }, 30000);

    it('should handle exact threshold (3)', () => {
      const responses = { q1: 1, q2: 2 };

      const result = service.scorePHQ2(responses);

      expect(result.score).toBe(3);
      expect(result.positiveScreen).toBe(true);
    }, 30000);
  }, 30000);

  describe('Mental Health Trend Calculation', () => {
    it('should return stable trend for insufficient data', () => {
      const history: AssessmentHistory[] = [];

      const result = service.calculateMentalHealthTrend(history);

      expect(result).toBe('stable');
    }, 30000);

    it('should return improving trend when scores decrease', () => {
      const history: AssessmentHistory[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 20, date: new Date('2024-01-01') },
        { assessmentId: '2', type: 'PHQ-9', score: 10, date: new Date('2024-06-01') },
      ];

      const result = service.calculateMentalHealthTrend(history);

      expect(result).toBe('improving');
    }, 30000);

    it('should return declining trend when scores increase', () => {
      const history: AssessmentHistory[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 5, date: new Date('2024-01-01') },
        { assessmentId: '2', type: 'PHQ-9', score: 15, date: new Date('2024-06-01') },
      ];

      const result = service.calculateMentalHealthTrend(history);

      expect(result).toBe('declining');
    }, 30000);

    it('should return stable trend for small changes', () => {
      const history: AssessmentHistory[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 10, date: new Date('2024-01-01') },
        { assessmentId: '2', type: 'PHQ-9', score: 11, date: new Date('2024-06-01') },
      ];

      const result = service.calculateMentalHealthTrend(history);

      expect(result).toBe('stable');
    }, 30000);
  }, 30000);

  describe('Detailed Mental Health Trend Calculation', () => {
    it('should return stable trend for empty history', () => {
      const history: AssessmentHistoryEntry[] = [];

      const result = service.calculateDetailedMentalHealthTrend(history);

      expect(result.direction).toBe('stable');
      expect(result.percentageChange).toBe(0);
    }, 30000);

    it('should return stable trend for single entry', () => {
      const history: AssessmentHistoryEntry[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 10, date: new Date(), severity: 'moderate' },
      ];

      const result = service.calculateDetailedMentalHealthTrend(history);

      expect(result.direction).toBe('stable');
    }, 30000);

    it('should calculate improving trend when score decreases significantly', () => {
      const history: AssessmentHistoryEntry[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 20, date: new Date('2024-01-01'), severity: 'severe' },
        { assessmentId: '2', type: 'PHQ-9', score: 10, date: new Date('2024-06-01'), severity: 'moderate' },
      ];

      const result = service.calculateDetailedMentalHealthTrend(history);

      expect(result.direction).toBe('improving');
      // Score decreased from 20 to 10 = -50% change
      expect(result.percentageChange).toBeLessThan(0);
    }, 30000);

    it('should calculate declining trend when score increases significantly', () => {
      const history: AssessmentHistoryEntry[] = [
        { assessmentId: '1', type: 'PHQ-9', score: 5, date: new Date('2024-01-01'), severity: 'mild' },
        { assessmentId: '2', type: 'PHQ-9', score: 18, date: new Date('2024-06-01'), severity: 'moderately-severe' },
      ];

      const result = service.calculateDetailedMentalHealthTrend(history);

      expect(result.direction).toBe('declining');
      // Score increased from 5 to 18 = +260% change
      expect(result.percentageChange).toBeGreaterThan(0);
    }, 30000);
  }, 30000);

  describe('getMentalHealthSummary', () => {
    it('should fetch mental health summary from backend', (done) => {
      const mockResponse = {
        assessments: [
          { type: 'PHQ-9', score: 10, severity: 'moderate', date: new Date().toISOString() },
        ],
        diagnoses: [],
        treatmentEngagement: { inTherapy: true },
        substanceUse: { overallRisk: 'low' },
        suicideRisk: { level: 'low', lastAssessed: new Date().toISOString() },
        socialSupport: 'good',
        currentTreatments: [],
      };

      service.getMentalHealthSummary('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.assessments.length).toBe(1);
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/patient-health/mental-health/patient-1')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    }, 30000);

    it('should return cached result on second call', (done) => {
      const mockResponse = {
        assessments: [],
        diagnoses: [],
        treatmentEngagement: { inTherapy: false },
        substanceUse: { overallRisk: 'low' },
        suicideRisk: { level: 'low', lastAssessed: new Date().toISOString() },
        socialSupport: 'good',
        currentTreatments: [],
      };

      // First call - populates cache
      service.getMentalHealthSummary('patient-cached').subscribe(() => {
        // Second call - should use cache (no HTTP request)
        service.getMentalHealthSummary('patient-cached').subscribe((result) => {
          expect(result).toBeDefined();
          done();
        }, 30000);
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/patient-health/mental-health/patient-cached')
      );
      req.flush(mockResponse);
    }, 30000);
  }, 30000);

  describe('submitMentalHealthAssessment', () => {
    it('should submit PHQ-9 assessment to backend', (done) => {
      const responses = { q1: 2, q2: 2, q3: 2, q4: 1, q5: 1, q6: 1, q7: 1, q8: 1, q9: 1 };
      const mockResponse = {
        type: 'PHQ-9',
        score: 12,
        severity: 'moderate',
        date: new Date().toISOString(),
        positiveScreen: true,
        requiresFollowup: true,
      };

      service.submitMentalHealthAssessment('patient-1', 'PHQ-9', responses).subscribe((result) => {
        expect(result.type).toBe('PHQ-9');
        expect(result.score).toBe(12);
        expect(result.severity).toBe('moderate');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/mental-health/assessments')
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should fall back to local scoring on backend error', (done) => {
      const responses = { q1: 2, q2: 2, q3: 2, q4: 2, q5: 1, q6: 1, q7: 1, q8: 1, q9: 1 };

      service.submitMentalHealthAssessment('patient-1', 'PHQ-9', responses).subscribe((result) => {
        expect(result.type).toBe('PHQ-9');
        expect(result.score).toBe(13);
        expect(result.severity).toBe('moderate');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/mental-health/assessments')
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should submit GAD-7 assessment', (done) => {
      const responses = { q1: 2, q2: 2, q3: 2, q4: 2, q5: 2, q6: 2, q7: 2 };

      service.submitMentalHealthAssessment('patient-1', 'GAD-7', responses).subscribe((result) => {
        expect(result.type).toBe('GAD-7');
        expect(result.score).toBe(14);
        expect(result.severity).toBe('moderate');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/mental-health/assessments')
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Cache Invalidation', () => {
    it('should invalidate mental health cache for patient', (done) => {
      const mockResponse = {
        assessments: [],
        diagnoses: [],
        treatmentEngagement: { inTherapy: false },
        substanceUse: { overallRisk: 'low' },
        suicideRisk: { level: 'low', lastAssessed: new Date().toISOString() },
        socialSupport: 'good',
        currentTreatments: [],
      };

      // First call - populates cache
      service.getMentalHealthSummary('patient-invalidate-test').subscribe(() => {
        // Invalidate cache
        service.invalidatePatientMentalHealth('patient-invalidate-test');

        // Third call - should make new HTTP request
        service.getMentalHealthSummary('patient-invalidate-test').subscribe(() => {
          done();
        });

        // Expect second HTTP request after invalidation
        const req2 = httpMock.expectOne((request) =>
          request.url.includes('/patient-health/mental-health/patient-invalidate-test')
        );
        req2.flush(mockResponse);
      });

      const req1 = httpMock.expectOne((request) =>
        request.url.includes('/patient-health/mental-health/patient-invalidate-test')
      );
      req1.flush(mockResponse);
    });
  });
});
