import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlertComponent } from './alert.component';
import { AlertSeverity, Alert } from './notification.service';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

/**
 * Alert Component Tests
 *
 * Tests for alert notification display, user interactions, and accessibility.
 */
describe('AlertComponent', () => {
  let component: AlertComponent;
  let fixture: ComponentFixture<AlertComponent>;
  let debugElement: DebugElement;

  const createAlert = (overrides?: Partial<Alert>): Alert => ({
    id: 'test-alert-1',
    severity: AlertSeverity.Info,
    title: 'Test Alert',
    message: 'Test message',
    confirmLabel: 'OK',
    timestamp: Date.now(),
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AlertComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
  });

  describe('Component Rendering', () => {
    it('should create the component', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });

    it('should display alert title', () => {
      component.alert = createAlert({ title: 'Test Title' });
      fixture.detectChanges();

      const title = debugElement.query(By.css('.alert-title'));
      expect(title.nativeElement.textContent).toBe('Test Title');
    });

    it('should display alert message', () => {
      component.alert = createAlert({ message: 'Test message' });
      fixture.detectChanges();

      const message = debugElement.query(By.css('.alert-message'));
      expect(message.nativeElement.textContent).toContain('Test message');
    });

    it('should render overlay', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const overlay = debugElement.query(By.css('.alert-overlay'));
      expect(overlay).toBeTruthy();
    });

    it('should apply info class for info severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.classList.contains('alert-info')).toBe(true);
    });

    it('should apply warning class for warning severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Warning });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.classList.contains('alert-warning')).toBe(true);
    });

    it('should apply error class for error severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Error });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.classList.contains('alert-error')).toBe(true);
    });

    it('should apply critical class for critical severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Critical });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.classList.contains('alert-critical')).toBe(true);
    });
  });

  describe('Buttons', () => {
    it('should render confirm button', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      expect(confirmBtn).toBeTruthy();
      expect(confirmBtn.nativeElement.textContent).toBe('OK');
    });

    it('should render cancel button if cancelLabel provided', () => {
      component.alert = createAlert({ cancelLabel: 'Cancel' });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      expect(cancelBtn).toBeTruthy();
      expect(cancelBtn.nativeElement.textContent).toBe('Cancel');
    });

    it('should not render cancel button if cancelLabel not provided', () => {
      component.alert = createAlert({ cancelLabel: undefined });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      expect(cancelBtn).toBeFalsy();
    });

    it('should call onConfirm when confirm button clicked', () => {
      const onConfirm = jasmine.createSpy('onConfirm');
      component.alert = createAlert({ onConfirm });
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      confirmBtn.nativeElement.click();

      expect(onConfirm).toHaveBeenCalled();
    });

    it('should call onCancel when cancel button clicked', () => {
      const onCancel = jasmine.createSpy('onCancel');
      component.alert = createAlert({ cancelLabel: 'Cancel', onCancel });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      cancelBtn.nativeElement.click();

      expect(onCancel).toHaveBeenCalled();
    });

    it('should dismiss after confirm', () => {
      spyOn(component, 'dismiss');
      component.alert = createAlert();
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      confirmBtn.nativeElement.click();

      expect(component.dismiss).toHaveBeenCalled();
    });

    it('should dismiss after cancel', () => {
      spyOn(component, 'dismiss');
      component.alert = createAlert({ cancelLabel: 'Cancel' });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      cancelBtn.nativeElement.click();

      expect(component.dismiss).toHaveBeenCalled();
    });
  });

  describe('Close Button', () => {
    it('should render close button for non-critical alerts', () => {
      component.alert = createAlert({ severity: AlertSeverity.Warning });
      fixture.detectChanges();

      const closeBtn = debugElement.query(By.css('.alert-close'));
      expect(closeBtn).toBeTruthy();
    });

    it('should not render close button for critical alerts', () => {
      component.alert = createAlert({ severity: AlertSeverity.Critical });
      fixture.detectChanges();

      const closeBtn = debugElement.query(By.css('.alert-close'));
      expect(closeBtn).toBeFalsy();
    });

    it('should call dismiss when close button clicked', () => {
      spyOn(component, 'dismiss');
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const closeBtn = debugElement.query(By.css('.alert-close'));
      closeBtn.nativeElement.click();

      expect(component.dismiss).toHaveBeenCalled();
    });
  });

  describe('Icons', () => {
    it('should display correct icon for info', () => {
      component.alert = createAlert({ severity: AlertSeverity.Info });

      const icon = component.getIcon();
      expect(icon).toBe('ⓘ');
    });

    it('should display correct icon for warning', () => {
      component.alert = createAlert({ severity: AlertSeverity.Warning });

      const icon = component.getIcon();
      expect(icon).toBe('⚠');
    });

    it('should display correct icon for error', () => {
      component.alert = createAlert({ severity: AlertSeverity.Error });

      const icon = component.getIcon();
      expect(icon).toBe('✕');
    });

    it('should display correct icon for critical', () => {
      component.alert = createAlert({ severity: AlertSeverity.Critical });

      const icon = component.getIcon();
      expect(icon).toBe('⚠');
    });

    it('should render icon element', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.alert-icon'));
      expect(icon).toBeTruthy();
    });
  });

  describe('Accessibility', () => {
    it('should have role alert for critical severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Critical });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('role')).toBe('alert');
    });

    it('should have role dialog for non-critical severity', () => {
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('role')).toBe('dialog');
    });

    it('should have aria-live assertive for critical', () => {
      component.alert = createAlert({ severity: AlertSeverity.Critical });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('aria-live')).toBe('assertive');
    });

    it('should have aria-live polite for non-critical', () => {
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-modal true', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('aria-modal')).toBe('true');
    });

    it('should have aria-labelledby', () => {
      component.alert = createAlert({ id: 'test-alert-123' });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('aria-labelledby')).toContain(
        'alert-title-test-alert-123'
      );
    });

    it('should have aria-describedby', () => {
      component.alert = createAlert({ id: 'test-alert-456' });
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('aria-describedby')).toContain(
        'alert-message-test-alert-456'
      );
    });

    it('should have accessible confirm button', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      expect(confirmBtn.nativeElement.getAttribute('tabindex')).toBe('0');
    });

    it('should have accessible cancel button', () => {
      component.alert = createAlert({ cancelLabel: 'Cancel' });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      expect(cancelBtn.nativeElement.getAttribute('tabindex')).toBe('0');
    });

    it('should have accessible close button', () => {
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const closeBtn = debugElement.query(By.css('.alert-close'));
      expect(closeBtn.nativeElement.getAttribute('aria-label')).toBe('Close alert');
    });
  });

  describe('Overlay Behavior', () => {
    it('should dismiss on overlay click for non-critical alerts', () => {
      spyOn(component, 'onBackdropClick');
      component.alert = createAlert({ severity: AlertSeverity.Info });
      fixture.detectChanges();

      const overlay = debugElement.query(By.css('.alert-overlay'));
      overlay.nativeElement.click();

      expect(component.onBackdropClick).toHaveBeenCalled();
    });

    it('should not dismiss on overlay click for critical alerts', () => {
      spyOn(component, 'onBackdropClick').and.callThrough();
      spyOn(component, 'cancel');
      component.alert = createAlert({ severity: AlertSeverity.Critical });
      fixture.detectChanges();

      const overlay = debugElement.query(By.css('.alert-overlay'));
      overlay.nativeElement.click();

      // onBackdropClick should be called but cancel should not
      expect(component.cancel).not.toHaveBeenCalled();
    });
  });

  describe('Lifecycle', () => {
    it('should initialize component', () => {
      component.alert = createAlert();

      expect(() => component.ngOnInit()).not.toThrow();
    });

    it('should cleanup on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });

  describe('Custom Labels', () => {
    it('should use custom confirm label', () => {
      component.alert = createAlert({ confirmLabel: 'Proceed' });
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      expect(confirmBtn.nativeElement.textContent).toBe('Proceed');
    });

    it('should use custom cancel label', () => {
      component.alert = createAlert({ cancelLabel: 'Skip' });
      fixture.detectChanges();

      const cancelBtn = debugElement.query(By.css('.alert-cancel'));
      expect(cancelBtn.nativeElement.textContent).toBe('Skip');
    });

    it('should default to OK when confirmLabel not provided', () => {
      component.alert = createAlert({ confirmLabel: undefined });
      fixture.detectChanges();

      const confirmBtn = debugElement.query(By.css('.alert-confirm'));
      expect(confirmBtn.nativeElement.textContent).toBe('OK');
    });
  });

  describe('Edge Cases', () => {
    it('should handle very long title', () => {
      const longTitle = 'x'.repeat(1000);
      component.alert = createAlert({ title: longTitle });
      fixture.detectChanges();

      const title = debugElement.query(By.css('.alert-title'));
      expect(title.nativeElement.textContent).toBe(longTitle);
    });

    it('should handle very long message', () => {
      const longMessage = 'x'.repeat(2000);
      component.alert = createAlert({ message: longMessage });
      fixture.detectChanges();

      const message = debugElement.query(By.css('.alert-message'));
      expect(message.nativeElement.textContent).toContain(longMessage);
    });

    it('should handle empty title', () => {
      component.alert = createAlert({ title: '' });
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });

    it('should handle empty message', () => {
      component.alert = createAlert({ message: '' });
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });

    it('should handle null callbacks', () => {
      component.alert = createAlert({
        onConfirm: null as any,
        onCancel: null as any,
      });
      fixture.detectChanges();

      expect(() => {
        const confirmBtn = debugElement.query(By.css('.alert-confirm'));
        confirmBtn.nativeElement.click();
      }).not.toThrow();
    });
  });

  describe('Visual States', () => {
    it('should have different colors for different severities', () => {
      const severities = [
        AlertSeverity.Info,
        AlertSeverity.Warning,
        AlertSeverity.Error,
        AlertSeverity.Critical,
      ];

      severities.forEach((severity) => {
        component.alert = createAlert({ severity });
        fixture.detectChanges();

        const alert = debugElement.query(By.css('.alert'));
        expect(alert.nativeElement.classList.contains(`alert-${severity}`)).toBe(true);
      });
    });

    it('should have proper z-index for visibility', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      const zIndex = window.getComputedStyle(alert.nativeElement).zIndex;
      expect(parseInt(zIndex)).toBeGreaterThan(9000);
    });
  });

  describe('DOM Cleanup', () => {
    it('should have data-testid attribute', () => {
      component.alert = createAlert();
      fixture.detectChanges();

      const alert = debugElement.query(By.css('.alert'));
      expect(alert.nativeElement.getAttribute('data-testid')).toBe('alert-container');
    });

    it('should have proper id attributes for ARIA', () => {
      component.alert = createAlert({ id: 'alert-xyz' });
      fixture.detectChanges();

      const title = debugElement.query(By.css('.alert-title'));
      expect(title.nativeElement.getAttribute('id')).toContain('alert-xyz');
    });
  });
});
