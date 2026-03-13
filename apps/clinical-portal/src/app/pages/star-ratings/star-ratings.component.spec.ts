import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { StarRatingsComponent } from './star-ratings.component';
import { StarRatingsService } from './star-ratings.service';
import { LoggerService } from '../../services/logger.service';
import { createMockLoggerService } from '../../../testing/mocks';
import { StarRatingResponse, StarRatingTrendResponse } from './star-ratings.model';

// Mock canvas getContext for Chart.js in jsdom
beforeAll(() => {
  HTMLCanvasElement.prototype.getContext = jest.fn().mockReturnValue({
    canvas: { width: 300, height: 150 },
    clearRect: jest.fn(),
    fillRect: jest.fn(),
    getImageData: jest.fn().mockReturnValue({ data: [] }),
    putImageData: jest.fn(),
    createImageData: jest.fn().mockReturnValue([]),
    setTransform: jest.fn(),
    drawImage: jest.fn(),
    save: jest.fn(),
    fillText: jest.fn(),
    restore: jest.fn(),
    beginPath: jest.fn(),
    moveTo: jest.fn(),
    lineTo: jest.fn(),
    closePath: jest.fn(),
    stroke: jest.fn(),
    translate: jest.fn(),
    scale: jest.fn(),
    rotate: jest.fn(),
    arc: jest.fn(),
    fill: jest.fn(),
    measureText: jest.fn().mockReturnValue({ width: 0 }),
    transform: jest.fn(),
    rect: jest.fn(),
    clip: jest.fn(),
    createLinearGradient: jest.fn().mockReturnValue({ addColorStop: jest.fn() }),
    createRadialGradient: jest.fn().mockReturnValue({ addColorStop: jest.fn() }),
    createPattern: jest.fn(),
  }) as any;
});

describe('StarRatingsComponent', () => {
  let component: StarRatingsComponent;
  let fixture: ComponentFixture<StarRatingsComponent>;
  let starRatingsServiceSpy: any;
  let routerSpy: any;

  const mockRating: StarRatingResponse = {
    tenantId: 'test-tenant',
    overallRating: 3.85,
    roundedRating: 4.0,
    measureCount: 3,
    openGapCount: 45,
    closedGapCount: 120,
    qualityBonusEligible: false,
    lastTriggerEvent: 'CARE_GAP_CLOSED',
    calculatedAt: '2026-03-13T10:00:00Z',
    domains: [
      { domain: 'Chronic Conditions', domainStars: 3.5, measureCount: 2, averagePerformanceRate: 0.72 },
      { domain: 'Prevention', domainStars: 4.2, measureCount: 1, averagePerformanceRate: 0.88 },
    ],
    measures: [
      { measureCode: 'CDC', measureName: 'Comprehensive Diabetes Care', domain: 'Chronic Conditions', numerator: 80, denominator: 100, performanceRate: 0.80, stars: 4 },
      { measureCode: 'CBP', measureName: 'Controlling Blood Pressure', domain: 'Chronic Conditions', numerator: 60, denominator: 100, performanceRate: 0.60, stars: 3 },
      { measureCode: 'BCS', measureName: 'Breast Cancer Screening', domain: 'Prevention', numerator: 90, denominator: 100, performanceRate: 0.90, stars: 5 },
    ],
  };

  const mockTrend: StarRatingTrendResponse = {
    tenantId: 'test-tenant',
    points: [
      { snapshotDate: '2026-03-01', granularity: 'WEEKLY', overallRating: 3.75, roundedRating: 4.0, openGapCount: 50, closedGapCount: 115, qualityBonusEligible: false },
      { snapshotDate: '2026-03-08', granularity: 'WEEKLY', overallRating: 3.85, roundedRating: 4.0, openGapCount: 45, closedGapCount: 120, qualityBonusEligible: false },
    ],
  };

  beforeEach(async () => {
    starRatingsServiceSpy = {
      getCurrentRating: jest.fn().mockReturnValue(of(mockRating)),
      getTrend: jest.fn().mockReturnValue(of(mockTrend)),
      simulate: jest.fn(),
    };
    routerSpy = { navigate: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [StarRatingsComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: StarRatingsService, useValue: starRatingsServiceSpy },
        { provide: LoggerService, useValue: createMockLoggerService() },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StarRatingsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should load current rating and trend on init', () => {
      fixture.detectChanges();

      expect(starRatingsServiceSpy.getCurrentRating).toHaveBeenCalled();
      expect(starRatingsServiceSpy.getTrend).toHaveBeenCalledWith(12, 'WEEKLY');
      expect(component.currentRating).toEqual(mockRating);
      expect(component.trendData).toEqual(mockTrend);
      expect(component.loading).toBe(false);
      expect(component.error).toBeNull();
    });

    it('should set sorted measures from current rating', () => {
      fixture.detectChanges();

      expect(component.sortedMeasures).toHaveLength(3);
      expect(component.sortedMeasures[0].measureCode).toBe('CDC');
    });

    it('should build trend chart data', () => {
      fixture.detectChanges();

      expect(component.trendChartData.labels).toHaveLength(2);
      expect(component.trendChartData.datasets).toHaveLength(2);
      expect(component.trendChartData.datasets[0].data).toEqual([3.75, 3.85]);
    });
  });

  describe('error handling', () => {
    it('should show error when current rating fails', () => {
      starRatingsServiceSpy.getCurrentRating.mockReturnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();

      expect(component.error).toContain('Unable to load star rating data');
      expect(component.loading).toBe(false);
    });

    it('should still show data when only trend fails', () => {
      starRatingsServiceSpy.getTrend.mockReturnValue(throwError(() => new Error('Trend error')));

      fixture.detectChanges();

      expect(component.currentRating).toEqual(mockRating);
      expect(component.trendData).toBeNull();
      expect(component.error).toBeNull();
    });
  });

  describe('measure table', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should return all measures when domain filter is ALL', () => {
      component.domainFilter = 'ALL';
      expect(component.filteredMeasures).toHaveLength(3);
    });

    it('should filter measures by domain', () => {
      component.domainFilter = 'Prevention';
      expect(component.filteredMeasures).toHaveLength(1);
      expect(component.filteredMeasures[0].measureCode).toBe('BCS');
    });

    it('should return unique sorted domains', () => {
      expect(component.uniqueDomains).toEqual(['Chronic Conditions', 'Prevention']);
    });

    it('should sort measures by stars ascending', () => {
      component.sortMeasures({ active: 'stars', direction: 'asc' });
      expect(component.sortedMeasures[0].stars).toBe(3);
      expect(component.sortedMeasures[2].stars).toBe(5);
    });

    it('should sort measures by performanceRate descending', () => {
      component.sortMeasures({ active: 'performanceRate', direction: 'desc' });
      expect(component.sortedMeasures[0].performanceRate).toBe(0.90);
    });

    it('should reset sort when direction is empty', () => {
      component.sortMeasures({ active: 'stars', direction: 'asc' });
      component.sortMeasures({ active: '', direction: '' });
      expect(component.sortedMeasures[0].measureCode).toBe('CDC');
    });

    it('should calculate gap count correctly', () => {
      const measure = mockRating.measures[0]; // CDC: 100 - 80 = 20
      expect(component.getMeasureGapCount(measure)).toBe(20);
    });
  });

  describe('star display helpers', () => {
    it('should return correct star array for rating 3', () => {
      const result = component.getStarArray(3);
      expect(result).toEqual([true, true, true, false, false]);
    });

    it('should return all false for rating 0', () => {
      const result = component.getStarArray(0);
      expect(result).toEqual([false, false, false, false, false]);
    });

    it('should return all true for rating 5', () => {
      const result = component.getStarArray(5);
      expect(result).toEqual([true, true, true, true, true]);
    });
  });

  describe('simulation', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should start with one empty closure row', () => {
      expect(component.simulationClosures).toHaveLength(1);
      expect(component.simulationClosures[0]).toEqual({ gapCode: '', closures: 1 });
    });

    it('should add a closure row', () => {
      component.addSimulationRow();
      expect(component.simulationClosures).toHaveLength(2);
    });

    it('should remove a closure row', () => {
      component.addSimulationRow();
      component.removeSimulationRow(0);
      expect(component.simulationClosures).toHaveLength(1);
    });

    it('should not remove the last closure row', () => {
      component.removeSimulationRow(0);
      expect(component.simulationClosures).toHaveLength(1);
    });

    it('should return available measure codes from current rating', () => {
      expect(component.availableMeasureCodes).toEqual(['CDC', 'CBP', 'BCS']);
    });

    it('should not call simulate when all closures are empty', () => {
      component.simulationClosures = [{ gapCode: '', closures: 1 }];
      component.runSimulation();
      expect(starRatingsServiceSpy.simulate).not.toHaveBeenCalled();
    });

    it('should call simulate with valid closures', () => {
      const simulatedResult = { ...mockRating, overallRating: 4.15, qualityBonusEligible: true };
      starRatingsServiceSpy.simulate.mockReturnValue(of(simulatedResult));

      component.simulationClosures = [
        { gapCode: 'CDC', closures: 10 },
        { gapCode: '', closures: 0 }, // should be filtered out
      ];
      component.runSimulation();

      expect(starRatingsServiceSpy.simulate).toHaveBeenCalledWith({
        closures: [{ gapCode: 'CDC', closures: 10 }],
      });
      expect(component.simulatedRating?.overallRating).toBe(4.15);
      expect(component.simulating).toBe(false);
    });

    it('should calculate simulation delta', () => {
      const simulatedResult = { ...mockRating, overallRating: 4.15 };
      starRatingsServiceSpy.simulate.mockReturnValue(of(simulatedResult));

      component.simulationClosures = [{ gapCode: 'CDC', closures: 10 }];
      component.runSimulation();

      expect(component.simulationDelta).toBeCloseTo(0.30, 2);
    });

    it('should return null delta when no simulation has run', () => {
      expect(component.simulationDelta).toBeNull();
    });
  });

  describe('trend controls', () => {
    it('should refresh trend with new granularity', () => {
      fixture.detectChanges();
      starRatingsServiceSpy.getTrend.mockClear();

      component.trendGranularity = 'MONTHLY';
      component.trendWeeks = 26;
      component.refreshTrend();

      expect(starRatingsServiceSpy.getTrend).toHaveBeenCalledWith(26, 'MONTHLY');
    });
  });

  describe('navigation', () => {
    it('should navigate to care gaps with measure filter', () => {
      component.navigateToCareGaps('CDC');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/care-gaps'], {
        queryParams: { filter: 'CDC' },
      });
    });

    it('should navigate to care gaps without filter', () => {
      component.navigateToCareGaps();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/care-gaps'], {
        queryParams: undefined,
      });
    });
  });

  describe('CSV export', () => {
    let originalCreateObjectURL: typeof URL.createObjectURL;
    let originalRevokeObjectURL: typeof URL.revokeObjectURL;

    beforeEach(() => {
      originalCreateObjectURL = URL.createObjectURL;
      originalRevokeObjectURL = URL.revokeObjectURL;
      URL.createObjectURL = jest.fn().mockReturnValue('blob:mock');
      URL.revokeObjectURL = jest.fn();
    });

    afterEach(() => {
      URL.createObjectURL = originalCreateObjectURL;
      URL.revokeObjectURL = originalRevokeObjectURL;
    });

    it('should generate CSV with correct headers and data', () => {
      fixture.detectChanges();

      const mockAnchor = { href: '', download: '', click: jest.fn() } as any;
      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockAnchor);

      component.exportMeasureCsv();

      expect(URL.createObjectURL).toHaveBeenCalled();
      const blob: Blob = (URL.createObjectURL as jest.Mock).mock.calls[0][0] as Blob;
      expect(blob.type).toBe('text/csv');
      expect(mockAnchor.download).toMatch(/^star-ratings-measures-\d{4}-\d{2}-\d{2}\.csv$/);
      expect(mockAnchor.click).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock');

      createElementSpy.mockRestore();
    });

    it('should not export when no current rating', () => {
      component.currentRating = null;
      component.exportMeasureCsv();
      expect(URL.createObjectURL).not.toHaveBeenCalled();
    });
  });

  describe('cleanup', () => {
    it('should complete destroy$ on ngOnDestroy', () => {
      const destroySpy = jest.spyOn(component['destroy$'], 'complete');
      component.ngOnDestroy();
      expect(destroySpy).toHaveBeenCalled();
    });
  });
});
