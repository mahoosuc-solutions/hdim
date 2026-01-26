/**
 * Unit Tests for Health Scoring Service
 */
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { HealthScoringService } from './health-scoring.service';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import { LoggerService } from '../logger.service';
import {
  HealthScore,
  HealthScoreHistoryPoint,
  HealthStatus,
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
} from '../../models/patient-health.model';

describe('HealthScoringService', () => {
  let service: HealthScoringService;
  let httpMock: HttpTestingController;
  let physicalHealthService: jest.Mocked<PhysicalHealthService>;
  let mentalHealthService: jest.Mocked<MentalHealthService>;
  let sdohService: jest.Mocked<SDOHService>;

  const mockPhysicalHealthSummary: PhysicalHealthSummary = {
    vitals: {
      bloodPressure: { value: '120/80', systolic: 120, diastolic: 80, unit: 'mmHg', date: new Date(), status: 'normal' },
      heartRate: { value: 72, unit: 'bpm', date: new Date(), status: 'normal' },
      respiratoryRate: { value: 16, unit: '/min', date: new Date(), status: 'normal' },
      temperature: { value: 98.6, unit: '°F', date: new Date(), status: 'normal' },
      bmi: { value: 24.5, height: 70, weight: 170, unit: 'kg/m²', date: new Date(), status: 'normal' },
      oxygenSaturation: { value: 98, unit: '%', date: new Date(), status: 'normal' },
    },
    recentLabs: [],
    chronicConditions: [],
    medicationAdherence: { status: 'good', pdcScore: 0.85, lastFillDate: new Date() },
    functionalStatus: {
      adlScore: 10,
      iadlScore: 8,
      mobilityLevel: 'Independent',
      painLevel: 2,
    },
  };

  const mockMentalHealthSummary: MentalHealthSummary = {
    assessments: [],
    diagnoses: [],
    treatmentEngagement: {
      inTherapy: true,
      therapyType: 'CBT',
      therapyFrequency: 'weekly',
      therapyAdherence: 90,
    },
    substanceUse: { overallRisk: 'low' },
    suicideRisk: { level: 'low', lastAssessed: new Date() },
    socialSupport: 'good',
    currentTreatments: [],
  };

  const mockSDOHSummary: SDOHSummary = {
    overallRisk: 'low',
    categories: {
      'food-insecurity': 'none',
      'housing-instability': 'none',
      transportation: 'none',
      employment: 'none',
      education: 'none',
      'social-isolation': 'none',
      'financial-strain': 'none',
      'healthcare-access': 'none',
    },
    activeNeeds: [],
    screeningDate: new Date(),
    interventions: [],
    referrals: [],
    strengths: [],
  };

  beforeEach(() => {
    const physicalHealthMock = {
      getPhysicalHealthSummary: jest.fn().mockReturnValue(of(mockPhysicalHealthSummary)),
    };

    const mentalHealthMock = {
      getMentalHealthSummary: jest.fn().mockReturnValue(of(mockMentalHealthSummary)),
    };

    const sdohMock = {
      getSDOHSummary: jest.fn().mockReturnValue(of(mockSDOHSummary)),
    };

    const loggerMock = {
      withContext: jest.fn().mockReturnValue({
        info: jest.fn(),
        debug: jest.fn(),
        error: jest.fn(),
        warn: jest.fn(),
      }),
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        HealthScoringService,
        { provide: PhysicalHealthService, useValue: physicalHealthMock },
        { provide: MentalHealthService, useValue: mentalHealthMock },
        { provide: SDOHService, useValue: sdohMock },
        { provide: LoggerService, useValue: loggerMock },
        HttpTestingController,
      ],
    });

    service = TestBed.inject(HealthScoringService);
    httpMock = TestBed.inject(HttpTestingController);
    physicalHealthService = TestBed.inject(PhysicalHealthService) as jest.Mocked<PhysicalHealthService>;
    mentalHealthService = TestBed.inject(MentalHealthService) as jest.Mocked<MentalHealthService>;
    sdohService = TestBed.inject(SDOHService) as jest.Mocked<SDOHService>;
  });

  afterEach(() => {
    // Flush any pending requests
    try {
      const pendingReqs = httpMock.match((req) => true);
      pendingReqs.forEach((req) => {
        if (!req.cancelled) {
          req.flush({});
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
    });
  });

  describe('calculateWeightedHealthScore', () => {
    it('should calculate weighted score with correct weights', () => {
      const components = {
        physical: 80,
        mental: 70,
        social: 60,
        preventive: 90,
      };

      const result = service.calculateWeightedHealthScore(components);

      // Expected: 80*0.4 + 70*0.3 + 60*0.15 + 90*0.15 = 32 + 21 + 9 + 13.5 = 75.5 -> 76
      expect(result).toBe(76);
    });

    it('should round the result', () => {
      const components = {
        physical: 75,
        mental: 75,
        social: 75,
        preventive: 75,
      };

      const result = service.calculateWeightedHealthScore(components);

      expect(result).toBe(75);
    });

    it('should handle maximum scores', () => {
      const components = {
        physical: 100,
        mental: 100,
        social: 100,
        preventive: 100,
      };

      const result = service.calculateWeightedHealthScore(components);

      expect(result).toBe(100);
    });

    it('should handle minimum scores', () => {
      const components = {
        physical: 0,
        mental: 0,
        social: 0,
        preventive: 0,
      };

      const result = service.calculateWeightedHealthScore(components);

      expect(result).toBe(0);
    });
  });

  describe('determineHealthStatus', () => {
    it('should return "excellent" for scores >= 80', () => {
      expect(service.determineHealthStatus(80)).toBe('excellent');
      expect(service.determineHealthStatus(90)).toBe('excellent');
      expect(service.determineHealthStatus(100)).toBe('excellent');
    });

    it('should return "good" for scores 60-79', () => {
      expect(service.determineHealthStatus(60)).toBe('good');
      expect(service.determineHealthStatus(70)).toBe('good');
      expect(service.determineHealthStatus(79)).toBe('good');
    });

    it('should return "fair" for scores 40-59', () => {
      expect(service.determineHealthStatus(40)).toBe('fair');
      expect(service.determineHealthStatus(50)).toBe('fair');
      expect(service.determineHealthStatus(59)).toBe('fair');
    });

    it('should return "poor" for scores < 40', () => {
      expect(service.determineHealthStatus(0)).toBe('poor');
      expect(service.determineHealthStatus(20)).toBe('poor');
      expect(service.determineHealthStatus(39)).toBe('poor');
    });
  });

  describe('calculateHealthScoreTrend', () => {
    it('should return stable trend for less than 2 history points', () => {
      const history: HealthScoreHistoryPoint[] = [
        { date: new Date(), score: 80, status: 'excellent', components: { physical: 80, mental: 80, social: 80, preventive: 80 } },
      ];

      const trend = service.calculateHealthScoreTrend(history);

      expect(trend.direction).toBe('stable');
      expect(trend.percentChange).toBe(0);
      expect(trend.pointsChange).toBe(0);
    });

    it('should return improving trend when score increases significantly', () => {
      const history: HealthScoreHistoryPoint[] = [
        { date: new Date('2024-01-01'), score: 60, status: 'good', components: { physical: 60, mental: 60, social: 60, preventive: 60 } },
        { date: new Date('2024-06-01'), score: 80, status: 'excellent', components: { physical: 80, mental: 80, social: 80, preventive: 80 } },
      ];

      const trend = service.calculateHealthScoreTrend(history);

      expect(trend.direction).toBe('improving');
      expect(trend.pointsChange).toBe(20);
    });

    it('should return declining trend when score decreases significantly', () => {
      const history: HealthScoreHistoryPoint[] = [
        { date: new Date('2024-01-01'), score: 80, status: 'excellent', components: { physical: 80, mental: 80, social: 80, preventive: 80 } },
        { date: new Date('2024-06-01'), score: 50, status: 'fair', components: { physical: 50, mental: 50, social: 50, preventive: 50 } },
      ];

      const trend = service.calculateHealthScoreTrend(history);

      expect(trend.direction).toBe('declining');
      expect(trend.pointsChange).toBe(-30);
    });

    it('should return stable trend for small changes', () => {
      const history: HealthScoreHistoryPoint[] = [
        { date: new Date('2024-01-01'), score: 75, status: 'good', components: { physical: 75, mental: 75, social: 75, preventive: 75 } },
        { date: new Date('2024-06-01'), score: 78, status: 'good', components: { physical: 78, mental: 78, social: 78, preventive: 78 } },
      ];

      const trend = service.calculateHealthScoreTrend(history);

      expect(trend.direction).toBe('stable');
    });

    it('should sort history by date correctly', () => {
      const history: HealthScoreHistoryPoint[] = [
        { date: new Date('2024-06-01'), score: 80, status: 'excellent', components: { physical: 80, mental: 80, social: 80, preventive: 80 } },
        { date: new Date('2024-01-01'), score: 60, status: 'good', components: { physical: 60, mental: 60, social: 60, preventive: 60 } },
      ];

      const trend = service.calculateHealthScoreTrend(history);

      // Despite being passed in wrong order, should correctly determine improving trend
      expect(trend.direction).toBe('improving');
      expect(trend.pointsChange).toBe(20);
    });
  });

  describe('getHealthScore', () => {
    it('should fetch health score from backend on cache miss', (done) => {
      const mockResponse = {
        patientId: 'patient-1',
        score: 85,
        overallScore: 85,
        status: 'excellent',
        trend: 'improving',
        components: {
          physical: 90,
          mental: 80,
          social: 85,
          preventive: 80,
        },
        calculatedAt: new Date().toISOString(),
      };

      service.getHealthScore('patient-1').subscribe((result) => {
        expect(result.patientId).toBe('patient-1');
        expect(result.overallScore).toBe(85);
        expect(result.status).toBe('excellent');
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/health-score/patient-1') || request.url.includes('/patient-1/health-score')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should return cached health score on cache hit', (done) => {
      const mockResponse = {
        patientId: 'patient-2',
        score: 75,
        overallScore: 75,
        status: 'good',
        trend: 'stable',
        components: { physical: 75, mental: 75, social: 75, preventive: 75 },
        calculatedAt: new Date().toISOString(),
      };

      // First call - cache miss
      service.getHealthScore('patient-2').subscribe(() => {
        // Second call - should use cache
        service.getHealthScore('patient-2').subscribe((result) => {
          expect(result.overallScore).toBe(75);
          done();
        });
        // No second HTTP request expected
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/health-score/patient-2') || request.url.includes('/patient-2/health-score')
      );
      req.flush(mockResponse);
    });

    it('should fall back to local calculation on backend error', (done) => {
      service.getHealthScore('patient-3').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.patientId).toBe('patient-3');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/health-score/patient-3') || request.url.includes('/patient-3/health-score')
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getHealthScoreHistory', () => {
    it('should fetch health score history from backend', (done) => {
      const mockHistory = [
        { score: 80, calculatedAt: new Date().toISOString(), trigger: 'manual' },
        { score: 75, calculatedAt: new Date().toISOString(), trigger: 'scheduled' },
      ];

      service.getHealthScoreHistory('patient-1').subscribe((result) => {
        expect(result.length).toBe(2);
        expect(result[0].score).toBe(80);
        expect(result[0].trigger).toBe('manual');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/health-score/patient-1/history') || request.url.includes('/patient-1/health-score/history')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockHistory);
    });

    it('should return empty array on error', (done) => {
      service.getHealthScoreHistory('patient-1').subscribe((result) => {
        expect(result).toEqual([]);
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/health-score/patient-1/history') || request.url.includes('/patient-1/health-score/history')
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('invalidateHealthScoreCache', () => {
    it('should call invalidatePatientCache with patient ID', () => {
      // Spy on the parent class method
      const invalidateSpy = jest.spyOn(service as any, 'invalidatePatientCache');

      service.invalidateHealthScoreCache('patient-cache-test');

      expect(invalidateSpy).toHaveBeenCalledWith('patient-cache-test');
    });
  });
});
