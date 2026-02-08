import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RangeThresholdSliderComponent } from './range-threshold-slider.component';
import { SliderConfig, RangeSliderConfig, ThresholdSliderConfig } from '../../models/measure-builder.model';

describe('RangeThresholdSliderComponent - Team 3 Test Suite', () => {
  let component: RangeThresholdSliderComponent;
  let fixture: ComponentFixture<RangeThresholdSliderComponent>;

  const mockRangeConfig: RangeSliderConfig = {
    id: 'age-range',
    name: 'Age Range',
    description: 'Patient age range (years)',
    type: 'range',
    minValue: 18,
    maxValue: 120,
    step: 1,
    unit: 'years',
    currentMin: 40,
    currentMax: 75,
    labels: ['40', '75']
  };

  const mockThresholdConfig: ThresholdSliderConfig = {
    id: 'hba1c-threshold',
    name: 'HbA1c Threshold',
    description: 'Hemoglobin A1c target',
    type: 'threshold',
    minValue: 4.0,
    maxValue: 13.0,
    step: 0.1,
    unit: '%',
    currentValue: 9.0,
    warningThreshold: 10.0,
    criticalThreshold: 11.0,
    preset: 'hba1c_control'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RangeThresholdSliderComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RangeThresholdSliderComponent);
    component = fixture.componentInstance;
  });

  describe('Range Slider - Basic Rendering', () => {
    beforeEach(() => {
      component.config = mockRangeConfig;
      fixture.detectChanges();
    });

    it('should create range slider component', () => {
      expect(component).toBeTruthy();
    });

    it('should render slider label', () => {
      const label = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-label'));
      expect(label?.nativeElement.textContent).toContain('Age Range');
    });

    it('should render slider description', () => {
      const description = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-description'));
      expect(description?.nativeElement.textContent).toContain('Patient age range');
    });

    it('should render two input sliders for range', () => {
      const sliders = fixture.debugElement.queryAll(el => el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range');
      expect(sliders.length).toBe(2);
    });

    it('should render min value input', () => {
      const minInput = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );
      expect(minInput).toBeTruthy();
      expect(minInput?.nativeElement.value).toBe('40');
    });

    it('should render max value input', () => {
      const maxInput = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-max')
      );
      expect(maxInput).toBeTruthy();
      expect(maxInput?.nativeElement.value).toBe('75');
    });

    it('should set slider min/max attributes correctly', () => {
      const sliders = fixture.debugElement.queryAll(el => el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range');
      sliders.forEach(slider => {
        expect(slider?.nativeElement.getAttribute('min')).toBe('18');
        expect(slider?.nativeElement.getAttribute('max')).toBe('120');
        expect(slider?.nativeElement.getAttribute('step')).toBe('1');
      });
    });

    it('should display current values below sliders', () => {
      const values = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-values'));
      expect(values?.nativeElement.textContent).toContain('40');
      expect(values?.nativeElement.textContent).toContain('75');
    });

    it('should display unit next to values', () => {
      const unit = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-unit'));
      expect(unit?.nativeElement.textContent).toContain('years');
    });
  });

  describe('Range Slider - Dual Value Interaction', () => {
    beforeEach(() => {
      component.config = mockRangeConfig;
      fixture.detectChanges();
    });

    it('should update min value when min slider changes', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      const event = new Event('input');
      minSlider?.nativeElement.value = '50';
      minSlider?.nativeElement.dispatchEvent(event);
      fixture.detectChanges();

      expect(component.config.currentMin).toBe(50);
    });

    it('should update max value when max slider changes', () => {
      const maxSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-max')
      );

      const event = new Event('input');
      maxSlider?.nativeElement.value = '80';
      maxSlider?.nativeElement.dispatchEvent(event);
      fixture.detectChanges();

      expect(component.config.currentMax).toBe(80);
    });

    it('should prevent min value from exceeding max value', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      const event = new Event('input');
      minSlider?.nativeElement.value = '85'; // > current max of 75
      minSlider?.nativeElement.dispatchEvent(event);
      fixture.detectChanges();

      expect(component.config.currentMin).toBeLessThanOrEqual(component.config.currentMax);
    });

    it('should prevent max value from going below min value', () => {
      const maxSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-max')
      );

      const event = new Event('input');
      maxSlider?.nativeElement.value = '30'; // < current min of 40
      maxSlider?.nativeElement.dispatchEvent(event);
      fixture.detectChanges();

      expect(component.config.currentMax).toBeGreaterThanOrEqual(component.config.currentMin);
    });

    it('should emit change event when range values change', () => {
      jest.spyOn(component.valueChanged, 'emit');

      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      const event = new Event('input');
      minSlider?.nativeElement.value = '45';
      minSlider?.nativeElement.dispatchEvent(event);
      fixture.detectChanges();

      expect(component.valueChanged.emit).toHaveBeenCalled();
    });

    it('should update display values in real-time during drag', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      for (let i = 40; i <= 60; i += 5) {
        minSlider?.nativeElement.value = i.toString();
        minSlider?.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();

        const display = fixture.debugElement.query(el =>
          el.nativeElement.classList.contains('slider-values')
        );
        expect(display?.nativeElement.textContent).toContain(i.toString());
      }
    });
  });

  describe('Threshold Slider - Basic Rendering', () => {
    beforeEach(() => {
      component.config = mockThresholdConfig;
      fixture.detectChanges();
    });

    it('should create threshold slider component', () => {
      expect(component).toBeTruthy();
    });

    it('should render threshold slider label', () => {
      const label = fixture.debugElement.query(el => el.nativeElement.classList.contains('slider-label'));
      expect(label?.nativeElement.textContent).toContain('HbA1c Threshold');
    });

    it('should render single input slider for threshold', () => {
      const sliders = fixture.debugElement.queryAll(el => el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range');
      expect(sliders.length).toBe(1);
    });

    it('should render current value input', () => {
      const valueInput = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-value')
      );
      expect(valueInput).toBeTruthy();
      expect(valueInput?.nativeElement.value).toBe('9');
    });

    it('should set slider min/max correctly', () => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range'
      );
      expect(slider?.nativeElement.getAttribute('min')).toBe('4');
      expect(slider?.nativeElement.getAttribute('max')).toBe('13');
      expect(slider?.nativeElement.getAttribute('step')).toBe('0.1');
    });

    it('should display current value with unit', () => {
      const valueDisplay = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-current-value')
      );
      expect(valueDisplay?.nativeElement.textContent).toContain('9');
      expect(valueDisplay?.nativeElement.textContent).toContain('%');
    });
  });

  describe('Threshold Slider - Warning/Critical Indicators', () => {
    beforeEach(() => {
      component.config = mockThresholdConfig;
      fixture.detectChanges();
    });

    it('should render warning indicator at warning threshold', () => {
      const warningIndicator = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('threshold-warning')
      );
      expect(warningIndicator).toBeTruthy();
    });

    it('should render critical indicator at critical threshold', () => {
      const criticalIndicator = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('threshold-critical')
      );
      expect(criticalIndicator).toBeTruthy();
    });

    it('should position warning indicator at correct percentage', () => {
      const range = mockThresholdConfig.maxValue - mockThresholdConfig.minValue; // 9
      const warningPosition = ((mockThresholdConfig.warningThreshold - mockThresholdConfig.minValue) / range) * 100;

      const warningIndicator = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('threshold-warning')
      );
      const style = warningIndicator?.nativeElement.style;
      const left = parseInt(style?.left || '0');

      expect(left).toBeCloseTo(warningPosition, 5);
    });

    it('should change slider appearance when value exceeds warning threshold', () => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range'
      );

      slider?.nativeElement.value = '10.5'; // > warning of 10.0
      slider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(slider?.nativeElement.classList.contains('warning-level')).toBeTruthy();
    });

    it('should change slider appearance when value exceeds critical threshold', () => {
      const slider = fixture.debugElement.query(el =>
        el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range'
      );

      slider?.nativeElement.value = '11.5'; // > critical of 11.0
      slider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(slider?.nativeElement.classList.contains('critical-level')).toBeTruthy();
    });

    it('should display warning tooltip when value in warning zone', () => {
      component.config.currentValue = 10.5;
      fixture.detectChanges();

      const tooltip = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('warning-tooltip')
      );
      expect(tooltip).toBeTruthy();
      expect(tooltip?.nativeElement.textContent).toContain('Warning');
    });

    it('should display critical tooltip when value in critical zone', () => {
      component.config.currentValue = 11.5;
      fixture.detectChanges();

      const tooltip = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('critical-tooltip')
      );
      expect(tooltip).toBeTruthy();
      expect(tooltip?.nativeElement.textContent).toContain('Critical');
    });
  });

  describe('Threshold Slider - Presets', () => {
    beforeEach(() => {
      component.config = mockThresholdConfig;
      fixture.detectChanges();
    });

    it('should render preset buttons', () => {
      const presetButtons = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('preset-button')
      );
      expect(presetButtons.length).toBeGreaterThan(0);
    });

    it('should apply HbA1c control preset (≤7%)', () => {
      const presetButton = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('HbA1c Control')
      );

      presetButton?.nativeElement.click();
      fixture.detectChanges();

      expect(component.config.currentValue).toBeLessThanOrEqual(7);
    });

    it('should apply BMI normal preset (18.5-24.9)', () => {
      component.config = {
        ...mockThresholdConfig,
        id: 'bmi-threshold',
        name: 'BMI',
        preset: 'bmi_normal'
      };
      fixture.detectChanges();

      const presetButton = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Normal')
      );

      presetButton?.nativeElement.click();
      fixture.detectChanges();

      expect(component.config.currentValue).toBeGreaterThanOrEqual(18.5);
      expect(component.config.currentValue).toBeLessThanOrEqual(24.9);
    });

    it('should highlight active preset', () => {
      const presetButtons = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('preset-button')
      );

      const activePreset = presetButtons.find(btn =>
        btn?.nativeElement.classList.contains('active')
      );

      expect(activePreset).toBeTruthy();
    });
  });

  describe('Slider - CQL Integration', () => {
    it('should generate CQL for age range', () => {
      component.config = mockRangeConfig;
      const cql = component.generateCQL();

      expect(cql).toContain('40');
      expect(cql).toContain('75');
      expect(cql).toContain('years');
    });

    it('should generate CQL for threshold with unit', () => {
      component.config = mockThresholdConfig;
      const cql = component.generateCQL();

      expect(cql).toContain('9');
      expect(cql).toContain('%');
    });

    it('should format CQL with proper operators', () => {
      component.config = mockRangeConfig;
      const cql = component.generateCQL();

      expect(cql).toContain('>=');
      expect(cql).toContain('<=');
    });
  });

  describe('Slider - Accessibility', () => {
    beforeEach(() => {
      component.config = mockRangeConfig;
      fixture.detectChanges();
    });

    it('should have aria-label on range sliders', () => {
      const sliders = fixture.debugElement.queryAll(el =>
        el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range'
      );

      sliders.forEach(slider => {
        expect(slider?.nativeElement.getAttribute('aria-label')).toBeTruthy();
      });
    });

    it('should have aria-valuemin and aria-valuemax', () => {
      const sliders = fixture.debugElement.queryAll(el =>
        el.nativeElement.tagName === 'INPUT' && el.nativeElement.type === 'range'
      );

      sliders.forEach(slider => {
        expect(slider?.nativeElement.getAttribute('aria-valuemin')).toBeTruthy();
        expect(slider?.nativeElement.getAttribute('aria-valuemax')).toBeTruthy();
      });
    });

    it('should update aria-valuenow on value change', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      minSlider?.nativeElement.value = '50';
      minSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(minSlider?.nativeElement.getAttribute('aria-valuenow')).toBe('50');
    });
  });

  describe('Slider - Range Validation', () => {
    beforeEach(() => {
      component.config = mockRangeConfig;
      fixture.detectChanges();
    });

    it('should enforce minimum boundary', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      minSlider?.nativeElement.value = '5'; // < min of 18
      minSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.config.currentMin).toBeGreaterThanOrEqual(18);
    });

    it('should enforce maximum boundary', () => {
      const maxSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-max')
      );

      maxSlider?.nativeElement.value = '150'; // > max of 120
      maxSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.config.currentMax).toBeLessThanOrEqual(120);
    });

    it('should respect step increments', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      minSlider?.nativeElement.value = '41'; // Not multiple of 1 but close
      minSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.config.currentMin % 1).toBe(0);
    });
  });

  describe('Slider - Visual Feedback', () => {
    beforeEach(() => {
      component.config = mockRangeConfig;
      fixture.detectChanges();
    });

    it('should show track fill between min and max values', () => {
      const track = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-track-fill')
      );
      expect(track).toBeTruthy();
    });

    it('should update track fill position on value change', () => {
      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      minSlider?.nativeElement.value = '60';
      minSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      const track = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-track-fill')
      );
      const style = track?.nativeElement.style;
      expect(style?.left).toBeTruthy();
    });
  });

  describe('Slider - Performance', () => {
    it('should handle rapid value changes efficiently', () => {
      component.config = mockRangeConfig;
      fixture.detectChanges();

      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      const startTime = performance.now();

      for (let i = 40; i <= 80; i++) {
        minSlider?.nativeElement.value = i.toString();
        minSlider?.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
      }

      const endTime = performance.now();

      expect(endTime - startTime).toBeLessThan(100);
    });
  });

  describe('Slider - Integration', () => {
    it('should accept RangeSliderConfig', () => {
      component.config = mockRangeConfig;
      expect(component.config.type).toBe('range');
    });

    it('should accept ThresholdSliderConfig', () => {
      component.config = mockThresholdConfig;
      expect(component.config.type).toBe('threshold');
    });

    it('should emit value changes to parent component', () => {
      component.config = mockRangeConfig;
      fixture.detectChanges();

      jest.spyOn(component.valueChanged, 'emit');

      const minSlider = fixture.debugElement.query(el =>
        el.nativeElement.classList.contains('slider-min')
      );

      minSlider?.nativeElement.value = '50';
      minSlider?.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.valueChanged.emit).toHaveBeenCalledWith({
        id: 'age-range',
        currentMin: 50,
        currentMax: 75
      });
    });
  });
});
