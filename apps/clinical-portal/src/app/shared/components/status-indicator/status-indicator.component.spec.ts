import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusIndicatorComponent } from './status-indicator.component';

describe('StatusIndicatorComponent', () => {
  let fixture: ComponentFixture<StatusIndicatorComponent>;
  let component: StatusIndicatorComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusIndicatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusIndicatorComponent);
    component = fixture.componentInstance;
  });

  it('returns a status class based on status', () => {
    component.status = 'processing';
    expect(component.getStatusClass()).toBe('status-processing');
  });

  it('pulses for active-like statuses', () => {
    component.status = 'active';
    expect(component.shouldPulse()).toBe(true);

    component.status = 'processing';
    expect(component.shouldPulse()).toBe(true);

    component.status = 'connected';
    expect(component.shouldPulse()).toBe(true);

    component.status = 'simulating';
    expect(component.shouldPulse()).toBe(true);

    component.status = 'idle';
    expect(component.shouldPulse()).toBe(false);
  });

  it('returns default labels and tooltips', () => {
    component.status = 'warning';
    expect(component.getDefaultLabel()).toBe('Warning');
    expect(component.getDefaultTooltip()).toBe('Attention may be required');

    component.status = 'simulating';
    expect(component.getDefaultLabel()).toBe('Demo Mode');
    expect(component.getDefaultTooltip()).toBe('Running in demo simulation mode');
  });

  it('falls back when status is unknown', () => {
    component.status = 'unknown' as any;

    expect(component.getDefaultLabel()).toBe('unknown');
    expect(component.getDefaultTooltip()).toBe('');
  });
});
