import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { HealthScoreMetricsComponent } from './health-score-metrics.component';
import { WebSocketService } from '@health-platform/shared/realtime';

type HealthScoreUpdate = {
  data: {
    score?: number;
    category?: string;
    factors?: string[] | null;
    calculatedAt?: number;
  };
};

describe('HealthScoreMetricsComponent', () => {
  let fixture: ComponentFixture<HealthScoreMetricsComponent>;
  let component: HealthScoreMetricsComponent;
  let updates$: Subject<HealthScoreUpdate>;

  beforeEach(async () => {
    jest.useFakeTimers();
    updates$ = new Subject<HealthScoreUpdate>();

    await TestBed.configureTestingModule({
      imports: [HealthScoreMetricsComponent],
      providers: [
        {
          provide: WebSocketService,
          useValue: {
            ofType: jest.fn(() => updates$.asObservable()),
            connectionStatus$: new Subject().asObservable(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HealthScoreMetricsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(false);
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  function emitUpdate(score: number, category = 'good', factors: string[] = []): void {
    updates$.next({
      data: {
        score,
        category,
        factors,
        calculatedAt: Date.now(),
      },
    });
    fixture.detectChanges(false);
  }

  it('initializes defaults', () => {
    expect(component.currentScore).toBe(0);
    expect(component.scoreCategory).toBe('Unknown');
    expect(component.trendText).toBe('Stable');
    expect(component.trendClass).toBe('trend-stable');
  });

  it('applies score, category, and category class from updates', () => {
    emitUpdate(85, 'good', ['medication-adherence']);

    expect(component.currentScore).toBe(85);
    expect(component.scoreCategory).toBe('Good');
    expect(component.scoreCategoryClass).toBe('good');
    expect(component.factors).toEqual(['medication-adherence']);
  });

  it('computes improving, declining, and stable trends', () => {
    emitUpdate(70, 'good');
    expect(component.trendText).toBe('Improving');
    expect(component.trendValue).toBe('+70');
    expect(component.trendClass).toBe('trend-improving');

    emitUpdate(60, 'fair');
    expect(component.trendText).toBe('Declining');
    expect(component.trendValue).toBe('-10');
    expect(component.trendClass).toBe('trend-declining');

    emitUpdate(60, 'fair');
    expect(component.trendText).toBe('Stable');
    expect(component.trendValue).toBe('±0');
    expect(component.trendClass).toBe('trend-stable');
  });

  it('formats factor labels and updates factor list', () => {
    expect(component.formatFactorName('medication-adherence')).toBe('Medication Adherence');

    emitUpdate(75, 'good', ['exercise', 'diet']);
    expect(component.factors).toEqual(['exercise', 'diet']);
  });

  it('applies and clears score update highlight', () => {
    emitUpdate(82, 'good');
    expect(component.scoreClass).toBe('score-updating');

    jest.advanceTimersByTime(650);
    expect(component.scoreClass).toBe('');
  });

  it('handles missing/null fields safely', () => {
    updates$.next({ data: { score: undefined, category: undefined, factors: null } });
    fixture.detectChanges(false);

    expect(component.currentScore).toBe(0);
    expect(component.scoreCategory).toBe('Unknown');
    expect(component.scoreCategoryClass).toBe('unknown');
    expect(component.factors).toEqual([]);
  });

  it('stops reacting after destroy', () => {
    component.ngOnDestroy();
    const before = component.currentScore;

    updates$.next({ data: { score: 99, category: 'excellent', factors: [] } });
    expect(component.currentScore).toBe(before);
  });
});
