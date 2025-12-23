import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CriticalAlertBannerComponent, CriticalAlert } from './critical-alert-banner.component';

describe('CriticalAlertBannerComponent', () => {
  let fixture: ComponentFixture<CriticalAlertBannerComponent>;
  let component: CriticalAlertBannerComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CriticalAlertBannerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CriticalAlertBannerComponent);
    component = fixture.componentInstance;
  });

  it('maps severity and type icons', () => {
    expect(component.getSeverityIcon('critical')).toBe('error');
    expect(component.getSeverityIcon('high')).toBe('warning');
    expect(component.getSeverityIcon('medium')).toBe('info');

    expect(component.getTypeIcon('suicide-risk')).toBe('psychology');
    expect(component.getTypeIcon('care-gap')).toBe('event_busy');
    expect(component.getTypeIcon('medication')).toBe('medication');
  });

  it('emits actions and dismissals', () => {
    const alert: CriticalAlert = {
      id: 'a1',
      type: 'care-gap',
      severity: 'high',
      title: 'Gap',
      description: 'desc',
    };
    const actionSpy = jest.spyOn(component.alertAction, 'emit');
    const dismissSpy = jest.spyOn(component.alertDismiss, 'emit');

    component.onAction(alert);
    component.onDismiss(alert);

    expect(actionSpy).toHaveBeenCalledWith(alert);
    expect(dismissSpy).toHaveBeenCalledWith(alert);
  });

  it('formats timestamps and aria live levels', () => {
    jest.useFakeTimers().setSystemTime(new Date('2024-01-02T00:00:00Z'));

    expect(component.formatTimestamp(new Date('2024-01-02T00:00:00Z'))).toBe('Just now');
    expect(component.formatTimestamp(new Date('2024-01-01T23:55:00Z'))).toBe('5m ago');
    expect(component.formatTimestamp(new Date('2024-01-01T22:00:00Z'))).toBe('2h ago');
    expect(component.formatTimestamp(new Date('2023-12-31T00:00:00Z'))).toBe('2d ago');

    expect(component.getAriaLive('critical')).toBe('assertive');
    expect(component.getAriaLive('medium')).toBe('polite');

    jest.useRealTimers();
  });

  it('returns severity classes and defaults', () => {
    expect(component.getSeverityClass('critical')).toBe('severity-critical');
    expect(component.getSeverityIcon('unknown' as any)).toBe('info');
    expect(component.getTypeIcon('unknown' as any)).toBe('warning');
  });

  it('handles missing timestamps and older dates', () => {
    jest.useFakeTimers().setSystemTime(new Date('2024-01-20T00:00:00Z'));

    expect(component.formatTimestamp(undefined)).toBe('');
    const older = new Date('2023-12-01T00:00:00Z');
    expect(component.formatTimestamp(older)).toBe(older.toLocaleDateString());

    jest.useRealTimers();
  });
});
