import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { LiveMetricsPanelComponent } from './live-metrics-panel.component';
import { SystemEventsService } from '../../../services/system-events.service';
import { LiveMetrics } from '../../../models/system-event.model';

const createMetrics = (overrides: Partial<LiveMetrics> = {}): LiveMetrics => ({
  patientsProcessed: 10,
  patientsProcessedChange: 2,
  throughputPerSecond: 4,
  maxThroughput: 10,
  complianceRate: 90,
  complianceRateChange: 0.6,
  openCareGaps: 5,
  careGapsChange: -1,
  successRate: 85,
  avgProcessingTimeMs: 120,
  lastUpdated: new Date('2024-01-01T12:00:00Z').toISOString(),
  ...overrides,
});

describe('LiveMetricsPanelComponent', () => {
  let fixture: ComponentFixture<LiveMetricsPanelComponent>;
  let component: LiveMetricsPanelComponent;
  let metricsSubject: BehaviorSubject<LiveMetrics>;
  let statusSubject: BehaviorSubject<'connected' | 'disconnected' | 'simulating'>;

  beforeEach(async () => {
    metricsSubject = new BehaviorSubject(createMetrics());
    statusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'simulating'>('connected');

    await TestBed.configureTestingModule({
      imports: [LiveMetricsPanelComponent],
      providers: [
        {
          provide: SystemEventsService,
          useValue: {
            metrics$: metricsSubject.asObservable(),
            connectionStatus$: statusSubject.asObservable(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LiveMetricsPanelComponent);
    component = fixture.componentInstance;
  });

  it('subscribes to metrics and connection status', () => {
    component.ngOnInit();

    expect(component.metrics.throughputPerSecond).toBe(4);
    expect(component.connectionStatus).toBe('connected');
  });

  it('computes throughput percent and trends', () => {
    component.metrics = createMetrics({ throughputPerSecond: 5, maxThroughput: 10 });

    expect(component.getThroughputPercent()).toBe(50);
    expect(component.getTrendClass(1)).toBe('trend-up');
    expect(component.getTrendIcon(-1)).toBe('trending_down');
    expect(component.getTrendClass(-0.2)).toBe('trend-stable');
    expect(component.getTrendIcon(0)).toBe('trending_flat');
  });

  it('formats trends and care gap changes', () => {
    expect(component.formatTrend(0)).toBe('Stable');
    expect(component.formatTrend(-1)).toBe('-1.0%');
    component.metrics = createMetrics({ careGapsChange: 2 });
    expect(component.getGapChangeClass()).toBe('negative');
    expect(component.getGapChangeIcon()).toBe('arrow_upward');
    expect(component.formatGapChange()).toBe('+2 today');
  });

  it('returns colors and labels for success rate and processing time', () => {
    component.metrics = createMetrics({ successRate: 99, avgProcessingTimeMs: 40 });
    expect(component.getSuccessRateColor()).toBe('primary');
    expect(component.getProcessingTimeLabel()).toBe('Excellent');

    component.metrics = createMetrics({ successRate: 85, avgProcessingTimeMs: 80 });
    expect(component.getSuccessRateColor()).toBe('accent');
    expect(component.getProcessingTimeLabel()).toBe('Good');

    component.metrics = createMetrics({ successRate: 70, avgProcessingTimeMs: 150 });
    expect(component.getSuccessRateColor()).toBe('warn');
    expect(component.getProcessingTimeLabel()).toBe('Normal');

    component.metrics = createMetrics({ avgProcessingTimeMs: 300 });
    expect(component.getProcessingTimeLabel()).toBe('Slow');

    component.metrics = createMetrics({ avgProcessingTimeMs: 600 });
    expect(component.getProcessingTimeLabel()).toBe('Very slow');
  });

  it('formats last updated time', () => {
    const formatted = component.formatLastUpdated();
    expect(formatted).toContain(':');
  });

  it('handles neutral care gap change states', () => {
    component.metrics = createMetrics({ careGapsChange: 0 });

    expect(component.getGapChangeClass()).toBe('neutral');
    expect(component.getGapChangeIcon()).toBe('remove');
    expect(component.formatGapChange()).toBe('No change');
  });

  it('stops reacting to updates after destroy', () => {
    component.ngOnInit();
    component.ngOnDestroy();

    metricsSubject.next(createMetrics({ patientsProcessed: 99 }));
    statusSubject.next('disconnected');

    expect(component.metrics.patientsProcessed).toBe(10);
    expect(component.connectionStatus).toBe('connected');
  });
});
