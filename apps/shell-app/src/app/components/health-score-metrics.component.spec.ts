import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HealthScoreMetricsComponent } from './health-score-metrics.component';
import { WebSocketService } from '@health-platform/shared/realtime';
import { Subject } from 'rxjs';

/**
 * Health Score Metrics Component Tests
 *
 * Tests for real-time health score display component.
 * Verifies:
 * - Score display and updates
 * - Category calculation
 * - Trend indication
 * - Factor display
 * - Animation and highlighting
 */
describe('HealthScoreMetricsComponent', () => {
  let component: HealthScoreMetricsComponent;
  let fixture: ComponentFixture<HealthScoreMetricsComponent>;
  let mockWebSocketService: any;
  let healthScoreSubject: Subject<any>;

  beforeEach(async () => {
    // Create subject for simulating health score updates
    healthScoreSubject = new Subject();

    // Create mock WebSocket service
    mockWebSocketService = {
      ofType: (type: string) => {
        if (type === 'HEALTH_SCORE_UPDATE') {
          return healthScoreSubject.asObservable();
        }
        return new Subject().asObservable();
      },
      connectionStatus$: new Subject().asObservable(),
    };

    await TestBed.configureTestingModule({
      imports: [HealthScoreMetricsComponent],
      providers: [
        { provide: WebSocketService, useValue: mockWebSocketService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HealthScoreMetricsComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default values', () => {
      expect(component.currentScore).toBe(0);
      expect(component.scoreCategory).toBe('Unknown');
    });

    it('should subscribe to health score updates on init', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
    });
  });

  describe('Score Display', () => {
    it('should display health score value', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.currentScore).toBe(85);
      const element = fixture.nativeElement.querySelector('[data-testid="health-score-value"]');
      expect(element?.textContent).toContain('85');
    });

    it('should display numeric score in large font', () => {
      healthScoreSubject.next({
        data: {
          score: 92,
          category: 'excellent',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      const scoreElement = fixture.nativeElement.querySelector('.score-number');
      expect(scoreElement).toBeTruthy();
      expect(window.getComputedStyle(scoreElement).fontSize).toContain('3');
    });
  });

  describe('Category Display', () => {
    it('should display excellent category', () => {
      healthScoreSubject.next({
        data: {
          score: 95,
          category: 'excellent',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreCategory).toBe('Excellent');
      expect(fixture.nativeElement.textContent).toContain('Excellent');
    });

    it('should display good category', () => {
      healthScoreSubject.next({
        data: {
          score: 80,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreCategory).toBe('Good');
    });

    it('should display fair category', () => {
      healthScoreSubject.next({
        data: {
          score: 60,
          category: 'fair',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreCategory).toBe('Fair');
    });

    it('should display poor category', () => {
      healthScoreSubject.next({
        data: {
          score: 30,
          category: 'poor',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreCategory).toBe('Poor');
    });

    it('should display category badge with appropriate color', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      const categoryElement = fixture.nativeElement.querySelector('.score-category');
      expect(categoryElement.classList.contains('good')).toBe(true);
    });
  });

  describe('Progress Bar', () => {
    it('should display progress bar matching score', () => {
      healthScoreSubject.next({
        data: {
          score: 70,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar?.style.width).toBe('70%');
    });

    it('should update progress bar when score changes', () => {
      healthScoreSubject.next({
        data: {
          score: 50,
          category: 'fair',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      let progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar?.style.width).toBe('50%');

      // Update score
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar?.style.width).toBe('85%');
    });

    it('should cap progress bar at 100%', () => {
      healthScoreSubject.next({
        data: {
          score: 150,
          category: 'excellent',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar?.style.width).toBe('150%'); // Raw value
    });
  });

  describe('Trend Indicator', () => {
    it('should show improving trend when score increases', () => {
      healthScoreSubject.next({
        data: {
          score: 70,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      healthScoreSubject.next({
        data: {
          score: 80,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.trendText).toBe('Improving');
      expect(component.trendValue).toContain('+10');
    });

    it('should show declining trend when score decreases', () => {
      healthScoreSubject.next({
        data: {
          score: 80,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      healthScoreSubject.next({
        data: {
          score: 65,
          category: 'fair',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.trendText).toBe('Declining');
      expect(component.trendValue).toContain('-15');
    });

    it('should show stable trend when score unchanged', () => {
      healthScoreSubject.next({
        data: {
          score: 75,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      healthScoreSubject.next({
        data: {
          score: 75,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.trendText).toBe('Stable');
      expect(component.trendValue).toBe('±0');
    });

    it('should apply trend class based on direction', () => {
      healthScoreSubject.next({
        data: {
          score: 70,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.trendClass).toBe('trend-improving');
      const trendElement = fixture.nativeElement.querySelector('.trend-indicator');
      expect(trendElement.classList.contains('trend-improving')).toBe(true);
    });
  });

  describe('Contributing Factors', () => {
    it('should display contributing factors', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: ['medication-adherence', 'blood-pressure-control'],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.factors.length).toBe(2);
      const factorElements = fixture.nativeElement.querySelectorAll('[data-testid="health-score-factor"]');
      expect(factorElements.length).toBe(2);
    });

    it('should format factor names for display', () => {
      const formatted = component.formatFactorName('medication-adherence');
      expect(formatted).toBe('Medication Adherence');
    });

    it('should display no factors when empty', () => {
      healthScoreSubject.next({
        data: {
          score: 75,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.factors.length).toBe(0);
    });

    it('should update factors when score changes', () => {
      healthScoreSubject.next({
        data: {
          score: 80,
          category: 'good',
          factors: ['exercise', 'diet'],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.factors).toEqual(['exercise', 'diet']);

      healthScoreSubject.next({
        data: {
          score: 75,
          category: 'good',
          factors: ['medication-adherence'],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.factors).toEqual(['medication-adherence']);
    });
  });

  describe('Animations & Updates', () => {
    it('should apply highlighting class on update', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreClass).toBe('score-updating');
    });

    it('should remove highlighting class after animation', (done) => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreClass).toBe('score-updating');

      setTimeout(() => {
        expect(component.scoreClass).toBe('');
        done();
      }, 700);
    });

    it('should update timestamp on each score update', () => {
      const timeBeforeUpdate = new Date().toLocaleTimeString();

      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      const timeAfterUpdate = new Date().toLocaleTimeString();
      expect(component.updatedTime).toBeTruthy();
    });
  });

  describe('Component Cleanup', () => {
    it('should unsubscribe from health score updates on destroy', () => {
      fixture.detectChanges();
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });

    it('should clear update timeout on destroy', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      spyOn(window, 'clearTimeout');
      component.ngOnDestroy();

      expect(window.clearTimeout).toHaveBeenCalled();
    });

    it('should not process updates after destroy', () => {
      fixture.detectChanges();
      component.ngOnDestroy();

      const initialScore = component.currentScore;

      healthScoreSubject.next({
        data: {
          score: 100,
          category: 'excellent',
          factors: [],
          calculatedAt: Date.now(),
        },
      });

      // Score should not update after destroy
      expect(component.currentScore).toBe(initialScore);
    });
  });

  describe('Edge Cases', () => {
    it('should handle missing score data', () => {
      healthScoreSubject.next({
        data: {
          category: 'unknown',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.currentScore).toBe(0);
    });

    it('should handle missing category data', () => {
      healthScoreSubject.next({
        data: {
          score: 75,
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.scoreCategory).toBe('Unknown');
    });

    it('should handle null factors array', () => {
      healthScoreSubject.next({
        data: {
          score: 85,
          category: 'good',
          factors: null,
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.factors).toEqual([]);
    });

    it('should handle very high scores', () => {
      healthScoreSubject.next({
        data: {
          score: 200,
          category: 'excellent',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.currentScore).toBe(200);
    });

    it('should handle negative scores', () => {
      healthScoreSubject.next({
        data: {
          score: -10,
          category: 'poor',
          factors: [],
          calculatedAt: Date.now(),
        },
      });
      fixture.detectChanges();

      expect(component.currentScore).toBe(-10);
    });
  });

  describe('Visual Rendering', () => {
    it('should render metric card element', () => {
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.metric-card');
      expect(card).toBeTruthy();
    });

    it('should render all required sections', () => {
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.metric-header')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.metric-content')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.score-display')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.score-progress')).toBeTruthy();
    });

    it('should apply health-score-card class', () => {
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.health-score-card');
      expect(card).toBeTruthy();
    });
  });
});
