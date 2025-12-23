import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RiskTrendChartComponent } from './risk-trend-chart.component';
import { PatientHealthService } from '../../../services/patient-health.service';
import { RiskAssessmentService } from '../../../services/risk-assessment.service';

describe('RiskTrendChartComponent', () => {
  let fixture: ComponentFixture<RiskTrendChartComponent>;
  let component: RiskTrendChartComponent;
  let patientHealthService: jest.Mocked<PatientHealthService>;
  let riskAssessmentService: jest.Mocked<RiskAssessmentService>;

  beforeEach(async () => {
    patientHealthService = {
      getHealthScoreHistory: jest.fn(),
    } as unknown as jest.Mocked<PatientHealthService>;

    riskAssessmentService = {
      getRiskHistory: jest.fn(),
    } as unknown as jest.Mocked<RiskAssessmentService>;

    await TestBed.configureTestingModule({
      imports: [RiskTrendChartComponent],
      providers: [
        { provide: PatientHealthService, useValue: patientHealthService },
        { provide: RiskAssessmentService, useValue: riskAssessmentService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskTrendChartComponent);
    component = fixture.componentInstance;
    component.patientId = 'p1';
  });

  it('loads risk assessment data and computes trend', () => {
    riskAssessmentService.getRiskHistory.mockReturnValue(of([
      { assessmentDate: '2024-01-01T00:00:00Z', riskScore: 10, riskLevel: 'low' },
      { assessmentDate: '2024-02-01T00:00:00Z', riskScore: 30, riskLevel: 'moderate' },
    ] as any));

    fixture.detectChanges();

    expect(component.dataSeries).toHaveLength(1);
    expect(component.trendIndicator?.direction).toBe('declining');
    expect(component.getTrendIcon()).toBe('trending_up');
  });

  it('falls back to health score history when risk data empty', () => {
    riskAssessmentService.getRiskHistory.mockReturnValue(of([]));
    patientHealthService.getHealthScoreHistory.mockReturnValue(of([
      { calculatedAt: '2024-01-01T00:00:00Z', score: 90 },
      { calculatedAt: '2024-02-01T00:00:00Z', score: 80 },
    ] as any));

    fixture.detectChanges();

    expect(component.dataSeries).toHaveLength(1);
    expect(component.trendIndicator).not.toBeNull();
  });

  it('falls back to health score history on error', () => {
    riskAssessmentService.getRiskHistory.mockReturnValue(
      throwError(() => new Error('risk failure'))
    );
    patientHealthService.getHealthScoreHistory.mockReturnValue(of([
      { calculatedAt: '2024-01-01T00:00:00Z', score: 75 },
      { calculatedAt: '2024-02-01T00:00:00Z', score: 70 },
    ] as any));

    fixture.detectChanges();

    expect(component.dataSeries).toHaveLength(1);
  });

  it('toggles series visibility', () => {
    component.dataSeries = [
      { id: 'overall', label: 'Overall', color: '#000', data: [], visible: true },
    ];

    component.toggleSeries('overall');

    expect(component.dataSeries[0].visible).toBe(false);
  });

  it('returns correct trend icon for stable and improving', () => {
    component.trendIndicator = {
      direction: 'stable',
      percentChange: 0,
      periodDescription: 'over last 30 days',
    };
    expect(component.getTrendIcon()).toBe('trending_flat');

    component.trendIndicator = {
      direction: 'improving',
      percentChange: 10,
      periodDescription: 'over last 30 days',
    };
    expect(component.getTrendIcon()).toBe('trending_down');
  });

  it('loads data when range changes', () => {
    const loadSpy = jest.spyOn(component as any, 'loadData');
    riskAssessmentService.getRiskHistory.mockReturnValue(of([]));
    patientHealthService.getHealthScoreHistory.mockReturnValue(of([]));
    component.selectedRange = '30d';

    component.onRangeChange();

    expect(loadSpy).toHaveBeenCalled();
  });

  it('loads data when custom dates are set', () => {
    const loadSpy = jest.spyOn(component as any, 'loadData');
    riskAssessmentService.getRiskHistory.mockReturnValue(of([]));
    patientHealthService.getHealthScoreHistory.mockReturnValue(of([]));
    component.selectedRange = 'custom';
    component.customStartDate = new Date('2024-01-01');
    component.customEndDate = new Date('2024-02-01');

    component.onCustomDateChange();

    expect(loadSpy).toHaveBeenCalled();
  });

  it('returns early when no patientId', () => {
    const loadSpy = jest.spyOn(component as any, 'loadData');
    component.patientId = '';
    component.ngOnInit();
    expect(loadSpy).toHaveBeenCalled();
    expect(component.loading).toBe(false);
  });

  it('handles empty risk assessment history', () => {
    (component as any).processRiskAssessmentData([]);
    expect(component.dataSeries).toEqual([]);
    expect(component.trendIndicator).toBeNull();
  });

  it('uses custom date range when provided', () => {
    component.selectedRange = 'custom';
    component.customStartDate = new Date('2024-01-01');
    component.customEndDate = new Date('2024-02-01');

    const range = (component as any).getDateRange();
    expect(range.startDate).toEqual(component.customStartDate);
    expect(range.endDate).toEqual(component.customEndDate);
  });

  it('calculates stable trend for small change', () => {
    component.selectedRange = '30d';
    const trend = (component as any).calculateTrend([
      { date: new Date('2024-01-01'), value: 50 },
      { date: new Date('2024-01-10'), value: 52 },
    ]);
    expect(trend.direction).toBe('stable');
  });

  it('initializes chart and draws canvas', () => {
    const ctx = {
      canvas: { width: 400, height: 200 },
      clearRect: jest.fn(),
      fillRect: jest.fn(),
      beginPath: jest.fn(),
      moveTo: jest.fn(),
      lineTo: jest.fn(),
      stroke: jest.fn(),
      fillText: jest.fn(),
      setLineDash: jest.fn(),
      arc: jest.fn(),
      fill: jest.fn(),
      save: jest.fn(),
      translate: jest.fn(),
      rotate: jest.fn(),
      restore: jest.fn(),
    } as unknown as CanvasRenderingContext2D;

    const canvas = { getContext: () => ctx };
    component.chartCanvas = { nativeElement: canvas } as any;
    component.dataSeries = [
      {
        id: 'overall',
        label: 'Overall',
        color: '#000',
        visible: true,
        data: [
          { date: new Date('2024-01-01'), value: 20 },
          { date: new Date('2024-01-02'), value: 40 },
        ],
      },
    ];

    (component as any).initializeChart();

    expect(ctx.clearRect).toHaveBeenCalled();
    expect(ctx.stroke).toHaveBeenCalled();
  });

  it('draws chart with thresholds enabled', () => {
    const ctx = {
      canvas: { width: 300, height: 150 },
      clearRect: jest.fn(),
      fillRect: jest.fn(),
      beginPath: jest.fn(),
      moveTo: jest.fn(),
      lineTo: jest.fn(),
      stroke: jest.fn(),
      fillText: jest.fn(),
      setLineDash: jest.fn(),
      arc: jest.fn(),
      fill: jest.fn(),
      save: jest.fn(),
      translate: jest.fn(),
      rotate: jest.fn(),
      restore: jest.fn(),
    } as unknown as CanvasRenderingContext2D;

    component.showThresholds = true;
    (component as any).drawSimpleChart(ctx, [
      {
        label: 'Overall',
        borderColor: '#000',
        data: [
          { x: new Date('2024-01-01'), y: 10 },
          { x: new Date('2024-01-05'), y: 20 },
        ],
      },
    ]);

    expect(ctx.setLineDash).toHaveBeenCalled();
    expect(ctx.arc).toHaveBeenCalled();
  });
});
