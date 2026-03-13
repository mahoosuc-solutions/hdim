import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StarRatingsService } from './star-ratings.service';
import { LoggerService } from '../../services/logger.service';
import { createMockLoggerService } from '../../../testing/mocks';
import { StarRatingResponse, StarRatingTrendResponse } from './star-ratings.model';

describe('StarRatingsService', () => {
  let service: StarRatingsService;
  let httpMock: HttpTestingController;

  const mockRating: StarRatingResponse = {
    tenantId: 'test-tenant',
    overallRating: 3.85,
    roundedRating: 4.0,
    measureCount: 12,
    openGapCount: 45,
    closedGapCount: 120,
    qualityBonusEligible: false,
    lastTriggerEvent: 'CARE_GAP_CLOSED',
    calculatedAt: '2026-03-13T10:00:00Z',
    domains: [
      { domain: 'Chronic Conditions', domainStars: 3.5, measureCount: 4, averagePerformanceRate: 0.72 },
    ],
    measures: [
      { measureCode: 'CDC', measureName: 'Comprehensive Diabetes Care', domain: 'Chronic Conditions', numerator: 80, denominator: 100, performanceRate: 0.80, stars: 4 },
    ],
  };

  const mockTrend: StarRatingTrendResponse = {
    tenantId: 'test-tenant',
    points: [
      { snapshotDate: '2026-03-01', granularity: 'WEEKLY', overallRating: 3.75, roundedRating: 4.0, openGapCount: 50, closedGapCount: 115, qualityBonusEligible: false },
      { snapshotDate: '2026-03-08', granularity: 'WEEKLY', overallRating: 3.85, roundedRating: 4.0, openGapCount: 45, closedGapCount: 120, qualityBonusEligible: false },
    ],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        StarRatingsService,
        { provide: LoggerService, useValue: createMockLoggerService() },
      ],
    });
    service = TestBed.inject(StarRatingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getCurrentRating', () => {
    it('should fetch current star rating via GET', () => {
      service.getCurrentRating().subscribe((result) => {
        expect(result).toEqual(mockRating);
      });

      const req = httpMock.expectOne((r) => r.url.includes('/api/v1/star-ratings/current'));
      expect(req.request.method).toBe('GET');
      req.flush(mockRating);
    });

    it('should propagate HTTP errors', () => {
      service.getCurrentRating().subscribe({
        error: (err) => {
          expect(err.status).toBe(500);
        },
      });

      const req = httpMock.expectOne((r) => r.url.includes('/api/v1/star-ratings/current'));
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getTrend', () => {
    it('should use default params (12 weeks, WEEKLY)', () => {
      service.getTrend().subscribe((result) => {
        expect(result).toEqual(mockTrend);
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('/api/v1/star-ratings/trend') &&
        r.url.includes('weeks=12') &&
        r.url.includes('granularity=WEEKLY')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockTrend);
    });

    it('should pass custom params', () => {
      service.getTrend(26, 'MONTHLY').subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.includes('weeks=26') &&
        r.url.includes('granularity=MONTHLY')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockTrend);
    });
  });

  describe('simulate', () => {
    it('should POST simulation request', () => {
      const request = {
        closures: [
          { gapCode: 'CDC', closures: 10 },
          { gapCode: 'BCS', closures: 5 },
        ],
      };

      const simulatedResult = { ...mockRating, overallRating: 4.15, qualityBonusEligible: true };

      service.simulate(request).subscribe((result) => {
        expect(result.overallRating).toBe(4.15);
        expect(result.qualityBonusEligible).toBe(true);
      });

      const req = httpMock.expectOne((r) => r.url.includes('/api/v1/star-ratings/simulate'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(simulatedResult);
    });
  });
});
