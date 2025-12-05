import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DateRangePickerComponent, DateRange } from './date-range-picker.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DateRangePickerComponent', () => {
  let component: DateRangePickerComponent;
  let fixture: ComponentFixture<DateRangePickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DateRangePickerComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(DateRangePickerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with provided dates', () => {
    const startDate = new Date('2024-01-01');
    const endDate = new Date('2024-01-31');

    component.startDate = startDate;
    component.endDate = endDate;
    component.ngOnInit();

    expect(component.internalStartDate).toEqual(startDate);
    expect(component.internalEndDate).toEqual(endDate);
  });

  it('should display custom labels', () => {
    component.startLabel = 'From Date';
    component.endLabel = 'To Date';
    fixture.detectChanges();

    const labels = fixture.nativeElement.querySelectorAll('mat-label');
    expect(labels[0].textContent).toContain('From Date');
    expect(labels[1].textContent).toContain('To Date');
  });

  describe('Date validation', () => {
    it('should validate that end date is after start date', () => {
      component.internalStartDate = new Date('2024-01-15');
      component.internalEndDate = new Date('2024-01-10');

      expect(component.isValidRange()).toBe(false);
    });

    it('should allow end date equal to start date', () => {
      const date = new Date('2024-01-15');
      component.internalStartDate = date;
      component.internalEndDate = date;

      expect(component.isValidRange()).toBe(true);
    });

    it('should allow partial date ranges', () => {
      component.internalStartDate = new Date('2024-01-01');
      component.internalEndDate = null;

      expect(component.isValidRange()).toBe(true);
    });

    it('should return error message for invalid range', () => {
      component.internalStartDate = new Date('2024-01-15');
      component.internalEndDate = new Date('2024-01-10');

      expect(component.getErrorMessage()).toBe('End date must be after start date');
    });

    it('should return empty error message for valid range', () => {
      component.internalStartDate = new Date('2024-01-01');
      component.internalEndDate = new Date('2024-01-15');

      expect(component.getErrorMessage()).toBe('');
    });
  });

  describe('Date change events', () => {
    it('should emit rangeChange when start date changes', () => {
      jest.spyOn(component.rangeChange, 'emit');
      const newDate = new Date('2024-01-01');

      component.onStartDateChange(newDate);

      expect(component.internalStartDate).toEqual(newDate);
      expect(component.rangeChange.emit).toHaveBeenCalledWith({
        start: newDate,
        end: null
      });
    });

    it('should emit rangeChange when end date changes', () => {
      jest.spyOn(component.rangeChange, 'emit');
      const newDate = new Date('2024-01-31');

      component.onEndDateChange(newDate);

      expect(component.internalEndDate).toEqual(newDate);
      expect(component.rangeChange.emit).toHaveBeenCalledWith({
        start: null,
        end: newDate
      });
    });

    it('should auto-correct invalid end date', () => {
      component.internalStartDate = new Date('2024-01-15');
      const invalidEndDate = new Date('2024-01-10');

      component.onEndDateChange(invalidEndDate);

      // Should auto-correct to start date
      expect(component.internalEndDate).toEqual(component.internalStartDate);
    });
  });

  describe('Clear range', () => {
    it('should clear both dates', () => {
      component.internalStartDate = new Date('2024-01-01');
      component.internalEndDate = new Date('2024-01-31');

      component.clearRange();

      expect(component.internalStartDate).toBeNull();
      expect(component.internalEndDate).toBeNull();
    });

    it('should emit rangeChange when cleared', () => {
      jest.spyOn(component.rangeChange, 'emit');
      component.clearRange();

      expect(component.rangeChange.emit).toHaveBeenCalledWith({
        start: null,
        end: null
      });
    });
  });

  describe('Preset ranges', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should initialize preset ranges', () => {
      expect(component.presets.length).toBe(4);
      expect(component.presets[0].label).toBe('Today');
      expect(component.presets[1].label).toBe('Last 7 Days');
      expect(component.presets[2].label).toBe('Last 30 Days');
      expect(component.presets[3].label).toBe('Last 90 Days');
    });

    it('should apply preset range', () => {
      jest.spyOn(component.rangeChange, 'emit');
      const preset = component.presets[0]; // Today

      component.applyPreset(preset);

      expect(component.internalStartDate).toEqual(preset.start);
      expect(component.internalEndDate).toEqual(preset.end);
      expect(component.rangeChange.emit).toHaveBeenCalled();
    });

    it('should show preset menu button when showPresets is true', () => {
      component.showPresets = true;
      fixture.detectChanges();

      const menuButton = fixture.nativeElement.querySelector('[matMenuTriggerFor]');
      expect(menuButton).toBeTruthy();
    });

    it('should not show preset menu button when showPresets is false', () => {
      component.showPresets = false;
      fixture.detectChanges();

      const menuButton = fixture.nativeElement.querySelector('[matMenuTriggerFor]');
      expect(menuButton).toBeFalsy();
    });
  });

  describe('Min/Max date constraints', () => {
    it('should set min date on start date picker', () => {
      const minDate = new Date('2024-01-01');
      component.minDate = minDate;
      fixture.detectChanges();

      const inputs = fixture.nativeElement.querySelectorAll('input[matInput]');
      expect(inputs[0]).toBeTruthy();
    });

    it('should set max date on both pickers', () => {
      const maxDate = new Date('2024-12-31');
      component.maxDate = maxDate;
      fixture.detectChanges();

      const inputs = fixture.nativeElement.querySelectorAll('input[matInput]');
      expect(inputs.length).toBe(2);
    });

    it('should use start date as min for end date picker', () => {
      const startDate = new Date('2024-01-15');
      component.internalStartDate = startDate;

      expect(component.getMinEndDate()).toEqual(startDate);
    });

    it('should use minDate when no start date is set', () => {
      const minDate = new Date('2024-01-01');
      component.minDate = minDate;
      component.internalStartDate = null;

      expect(component.getMinEndDate()).toEqual(minDate);
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should have aria-label on date inputs', () => {
      const inputs = fixture.nativeElement.querySelectorAll('input[matInput]');
      expect(inputs[0].getAttribute('aria-label')).toBe('Start Date');
      expect(inputs[1].getAttribute('aria-label')).toBe('End Date');
    });

    it('should have aria-label on clear button', () => {
      const clearButton = fixture.nativeElement.querySelector('button[aria-label="Clear date range"]');
      expect(clearButton).toBeTruthy();
    });

    it('should have aria-label on preset menu button', () => {
      component.showPresets = true;
      fixture.detectChanges();

      const menuButton = fixture.nativeElement.querySelector('[matMenuTriggerFor]');
      expect(menuButton.getAttribute('aria-label')).toBe('Select preset date range');
    });

    it('should have aria-hidden on separator', () => {
      const separator = fixture.nativeElement.querySelector('.date-separator');
      expect(separator.getAttribute('aria-hidden')).toBe('true');
    });
  });
});
