import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnimatedCounterComponent } from './animated-counter.component';

const createChange = (current: number, previous: number, firstChange: boolean) => ({
  currentValue: current,
  previousValue: previous,
  firstChange,
  isFirstChange: () => firstChange,
});

describe('AnimatedCounterComponent', () => {
  let fixture: ComponentFixture<AnimatedCounterComponent>;
  let component: AnimatedCounterComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnimatedCounterComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AnimatedCounterComponent);
    component = fixture.componentInstance;
  });

  it('formats numbers with grouping and decimals', () => {
    component.decimals = 2;
    component.useGrouping = true;

    expect(component.formatNumber(1234.567)).toBe('1,234.57');

    component.useGrouping = false;
    expect(component.formatNumber(12.3)).toBe('12.30');
  });

  it('sets display value immediately on first change', () => {
    component.ngOnChanges({
      value: createChange(100, 0, true),
    });

    expect(component.displayValue).toBe('100');
    expect(component.isAnimating).toBe(false);
  });

  it('animates value changes to completion', () => {
    const rafSpy = jest
      .spyOn(window, 'requestAnimationFrame')
      .mockImplementation((cb: FrameRequestCallback) => {
        cb(component.duration);
        return 1;
      });
    const perfSpy = jest.spyOn(performance, 'now').mockReturnValue(0);

    component.ngOnChanges({
      value: createChange(200, 100, false),
    });

    expect(component.displayValue).toBe('200');
    expect(component.isAnimating).toBe(false);

    rafSpy.mockRestore();
    perfSpy.mockRestore();
  });

  it('cancels animation on destroy', () => {
    const cancelSpy = jest.spyOn(window, 'cancelAnimationFrame').mockImplementation();
    (component as any).animationFrameId = 1;

    component.ngOnDestroy();
    expect(cancelSpy).toHaveBeenCalledWith(1);

    cancelSpy.mockRestore();
  });
});
