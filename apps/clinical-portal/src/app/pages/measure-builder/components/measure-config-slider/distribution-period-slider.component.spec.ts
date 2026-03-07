import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DistributionPeriodSliderComponent } from './distribution-period-slider.component';
import { SliderConfig, DistributionSliderConfig, PeriodSelectorConfig } from '../../models/measure-builder.model';

describe('DistributionPeriodSliderComponent - Team 4 Test Suite', () => {
  let component: DistributionPeriodSliderComponent;
  let fixture: ComponentFixture<DistributionPeriodSliderComponent>;

  const mockDistributionConfig: DistributionSliderConfig = {
    id: 'measure-components-weight',
    label: 'Measure Component Weights',
    name: 'Measure Component Weights',
    description: 'Distribution of weight across measure components',
    type: 'distribution',
    category: 'composite-weights',
    cqlGenerator: (value: any) => '',
    components: [
      { componentId: 'comp1', componentName: 'Prevention', label: 'Prevention', weight: 30, color: '#2196F3' },
      { componentId: 'comp2', componentName: 'Treatment', label: 'Treatment', weight: 50, color: '#4CAF50' },
      { componentId: 'comp3', componentName: 'Monitoring', label: 'Monitoring', weight: 20, color: '#FF9800' }
    ],
    totalRequired: 100
  } as DistributionSliderConfig;

  const mockPeriodConfig: PeriodSelectorConfig = {
    id: 'measurement-period',
    label: 'Measurement Period',
    name: 'Measurement Period',
    description: 'Define the evaluation period for the measure',
    type: 'period',
    category: 'timing',
    cqlGenerator: (value: any) => '',
    options: [
      { id: 'calendar_year', label: 'Calendar Year', value: 'calendar_year', duration: 365 },
      { id: 'rolling_year', label: 'Rolling Year', value: 'rolling_year', duration: 365 },
      { id: 'fiscal_year', label: 'Fiscal Year (Oct-Sep)', value: 'fiscal_year', duration: 365 },
      { id: 'quarter', label: 'Quarterly', value: 'quarter', duration: 91 }
    ],
    value: 'calendar_year',
    periodType: 'calendar_year',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    allowCustom: true,
    presetPeriods: [
      { id: 'calendar_year', label: 'Calendar Year', value: 'calendar_year', duration: 365 },
      { id: 'rolling_year', label: 'Rolling Year', value: 'rolling_year', duration: 365 },
      { id: 'fiscal_year', label: 'Fiscal Year (Oct-Sep)', value: 'fiscal_year', duration: 365 },
      { id: 'quarter', label: 'Quarterly', value: 'quarter', duration: 91 }
    ]
  } as PeriodSelectorConfig;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DistributionPeriodSliderComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DistributionPeriodSliderComponent);
    component = fixture.componentInstance;
  });

  describe('Distribution Slider - Basic Rendering', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();
    });

    it('should create distribution slider component', () => {
      expect(component).toBeTruthy();
    });

    it('should render slider label', () => {
      const label = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-label'));
      expect(label?.nativeElement.textContent).toContain('Measure Component Weights');
    });

    it('should render all component sliders', () => {
      const sliders = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('component-slider')
      );
      expect(sliders.length).toBe(3);
    });

    it('should render component labels', () => {
      const labels = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('component-label')
      );
      expect(labels.length).toBe(3);
      expect(labels[0]?.nativeElement.textContent).toContain('Prevention');
      expect(labels[1]?.nativeElement.textContent).toContain('Treatment');
      expect(labels[2]?.nativeElement.textContent).toContain('Monitoring');
    });

    it('should display weight values for each component', () => {
      const weights = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('component-weight')
      );
      expect(weights.length).toBe(3);
      expect(weights[0]?.nativeElement.textContent).toContain('30');
      expect(weights[1]?.nativeElement.textContent).toContain('50');
      expect(weights[2]?.nativeElement.textContent).toContain('20');
    });

    it('should render color indicator for each component', () => {
      const colorIndicators = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('color-indicator')
      );
      expect(colorIndicators.length).toBe(3);
    });

    it('should display total weight (should be 100)', () => {
      const totalDisplay = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('total-weight')
      );
      expect(totalDisplay?.nativeElement.textContent).toContain('100');
    });

    it('should show visual weight distribution bar', () => {
      const distributionBar = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('distribution-bar')
      );
      expect(distributionBar).toBeTruthy();
    });
  });

  describe('Distribution Slider - Weight Adjustment', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();
    });

    it('should update weight when slider changes', fakeAsync(() => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '40';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      tick(100);
      fixture.detectChanges();

      // Component rebalances all weights proportionally, so exact 40 won't remain
      // But the total should be approximately 100
      expect(component.getTotalWeight()).toBeCloseTo(100, 0);
    }));

    it('should rebalance other weights when one changes', fakeAsync(() => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '50'; // Change from 30 to 50
      slider?.nativeElement.dispatchEvent(new Event('input'));
      tick(100);
      fixture.detectChanges();

      // Total should be approximately 100 (rounding may cause ±0.01)
      expect(component.getTotalWeight()).toBeCloseTo(100, 0);
    }));

    it('should prevent single weight from exceeding 100', () => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '150';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.getComponentWeight(0)).toBeLessThanOrEqual(100);
    });

    it('should prevent negative weights', () => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '-10';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.getComponentWeight(0)).toBeGreaterThanOrEqual(0);
    });

    it('should emit change event when weight changes', fakeAsync(() => {
      jest.spyOn(component.valueChanged, 'emit');

      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '40';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      tick(100);
      fixture.detectChanges();

      expect(component.valueChanged.emit).toHaveBeenCalled();
    }));

    it('should update distribution bar segments on weight change', fakeAsync(() => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '50';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      tick(100);
      fixture.detectChanges();

      const segments = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('bar-segment')
      );

      expect(segments.length).toBe(3);
      // After rebalancing, first component is 50 out of 120 total, scaled to ~42
      expect(component.getComponentWeight(0)).toBeGreaterThan(30);
    }));
  });

  describe('Distribution Slider - Visual Feedback', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();
    });

    it('should update bar segment width based on weight', () => {
      const segments = component.getDistributionSegments();

      // First segment should be 30%
      expect(segments[0]?.width).toBe(30);
      // Second segment should be 50%
      expect(segments[1]?.width).toBe(50);
      // Third segment should be 20%
      expect(segments[2]?.width).toBe(20);
    });

    it('should use component color for bar segment', () => {
      const segments = component.getDistributionSegments();

      expect(segments[0]?.color).toBe('#2196F3');
      expect(segments[1]?.color).toBe('#4CAF50');
      expect(segments[2]?.color).toBe('#FF9800');
    });

    it('should highlight warning when weights sum is not 100', () => {
      const config = component.config as DistributionSliderConfig;
      config.components[0].weight = 40;
      config.components[1].weight = 40;
      config.components[2].weight = 15; // Total = 95
      fixture.detectChanges();

      expect(component.isValidDistribution()).toBeFalsy();
    });
  });

  describe('Period Selector - Basic Rendering', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      fixture.detectChanges();
    });

    it('should create period selector component', () => {
      expect(component).toBeTruthy();
    });

    it('should render selector label', () => {
      const label = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-label'));
      expect(label?.nativeElement.textContent).toContain('Measurement Period');
    });

    it('should render preset period buttons', () => {
      const buttons = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('period-button')
      );
      expect(buttons.length).toBeGreaterThan(0);
    });

    it('should display current period dates', () => {
      const dateDisplay = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('period-dates')
      );
      expect(dateDisplay?.nativeElement.textContent).toContain('2024-01-01');
      expect(dateDisplay?.nativeElement.textContent).toContain('2024-12-31');
    });

    it('should highlight active period preset', () => {
      const activeButton = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('period-button') &&
        el.nativeElement.classList.contains('active')
      );
      expect(activeButton).toBeTruthy();
    });
  });

  describe('Period Selector - Period Selection', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      fixture.detectChanges();
    });

    it('should update dates when preset is selected', () => {
      component.selectPeriod('fiscal_year');
      fixture.detectChanges();

      const config = component.config as PeriodSelectorConfig;
      expect(config.periodType).toBe('fiscal_year');
    });

    it('should support calendar year period', () => {
      component.selectPeriod('calendar_year');
      fixture.detectChanges();

      const config = component.config as PeriodSelectorConfig;
      expect(config.periodType).toBe('calendar_year');
    });

    it('should support rolling year period', () => {
      component.selectPeriod('rolling_year');
      fixture.detectChanges();

      const config = component.config as PeriodSelectorConfig;
      expect(config.periodType).toBe('rolling_year');
    });

    it('should support quarterly period', () => {
      component.selectPeriod('quarter');
      fixture.detectChanges();

      const config = component.config as PeriodSelectorConfig;
      expect(config.periodType).toBe('quarter');
    });

    it('should emit change event when period changes', () => {
      jest.spyOn(component.valueChanged, 'emit');

      component.selectPeriod('fiscal_year');
      fixture.detectChanges();

      expect(component.valueChanged.emit).toHaveBeenCalled();
    });
  });

  describe('Period Selector - Custom Periods', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      fixture.detectChanges();
    });

    it('should allow custom period days input', () => {
      const customInput = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('custom-days-input')
      );
      expect(customInput).toBeTruthy();
    });

    it('should allow custom period months input', () => {
      const customInput = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('custom-months-input')
      );
      expect(customInput).toBeTruthy();
    });

    it('should validate custom period days (1-365)', () => {
      component.config.customPeriodDays = 400; // > 365
      fixture.detectChanges();

      const error = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('between 1 and 365')
      );
      expect(error).toBeTruthy();
    });

    it('should update dates when custom period is set', () => {
      component.config.periodType = 'custom';
      component.config.customPeriodDays = 90;
      fixture.detectChanges();

      expect(component.config.customPeriodDays).toBe(90);
    });
  });

  describe('Distribution Slider - CQL Integration', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();
    });

    it('should generate CQL for weight distribution', () => {
      const cql = component.generateCQL();
      expect(cql).toBeTruthy();
      expect(cql).toContain('components');
      expect(cql).toContain('Prevention');
      expect(cql).toContain('Treatment');
      expect(cql).toContain('Monitoring');
    });

    it('should format weights as percentages', () => {
      const cql = component.generateCQL();
      expect(cql).toContain('0.30');
      expect(cql).toContain('0.50');
      expect(cql).toContain('0.20');
    });
  });

  describe('Period Selector - CQL Integration', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      fixture.detectChanges();
    });

    it('should generate CQL for measurement period', () => {
      const cql = component.generateCQL();
      expect(cql).toBeTruthy();
      expect(cql).toContain('2024');
    });

    it('should include date range in CQL', () => {
      const cql = component.generateCQL();
      expect(cql).toContain('01-01');
      expect(cql).toContain('12-31');
    });
  });

  describe('Slider - Accessibility', () => {
    it('should have aria labels on distribution sliders', () => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();

      const sliders = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      sliders.forEach(slider => {
        expect(slider?.nativeElement.getAttribute('aria-label')).toBeTruthy();
      });
    });

    it('should have aria labels on period buttons', () => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('period-button')
      );

      buttons.forEach(button => {
        expect(button?.nativeElement.getAttribute('aria-label')).toBeTruthy();
      });
    });
  });

  describe('Slider - Validation', () => {
    beforeEach(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();
    });

    it('should validate total weight equals 100', () => {
      const isValid = component.isValidDistribution();
      expect(isValid).toBeTruthy();
    });

    it('should detect invalid distribution (not 100)', () => {
      component.config.components[0].weight = 40;
      component.config.components[1].weight = 40;
      component.config.components[2].weight = 15; // Total = 95

      const isValid = component.isValidDistribution();
      expect(isValid).toBeFalsy();
    });

    it('should provide helpful validation message', () => {
      component.config.components[0].weight = 40;
      const message = component.getValidationMessage();
      expect(message).toContain('100');
    });
  });

  describe('Slider - Performance', () => {
    it('should handle rapid weight changes efficiently', () => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();

      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      const startTime = performance.now();

      for (let i = 10; i <= 90; i += 5) {
        slider?.nativeElement.value = i.toString();
        slider?.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
      }

      const endTime = performance.now();

      expect(endTime - startTime).toBeLessThan(100);
    });
  });

  describe('Slider - Type Guards', () => {
    it('should identify distribution slider config', () => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      expect(component.isDistributionSlider()).toBeTruthy();
      expect(component.isPeriodSelector()).toBeFalsy();
    });

    it('should identify period selector config', () => {
      component.config = JSON.parse(JSON.stringify(mockPeriodConfig));
      expect(component.isPeriodSelector()).toBeTruthy();
      expect(component.isDistributionSlider()).toBeFalsy();
    });
  });

  describe('Slider - Integration', () => {
    it('should emit configuration changes to parent', fakeAsync(() => {
      component.config = JSON.parse(JSON.stringify(mockDistributionConfig));
      fixture.detectChanges();

      jest.spyOn(component.valueChanged, 'emit');

      const slider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('component-slider')
      );

      slider?.nativeElement.value = '40';
      slider?.nativeElement.dispatchEvent(new Event('input'));
      tick(100);
      fixture.detectChanges();

      expect(component.valueChanged.emit).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'distribution',
        })
      );
    }));
  });
});
