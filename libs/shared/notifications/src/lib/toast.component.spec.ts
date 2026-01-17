import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { NotificationType, Toast } from './notification.service';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

/**
 * Toast Component Tests
 *
 * Tests for toast notification display, interactions, and lifecycle.
 */
describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let debugElement: DebugElement;

  const createToast = (overrides?: Partial<Toast>): Toast => ({
    id: 'test-toast-1',
    type: NotificationType.Success,
    message: 'Test message',
    duration: 3000,
    timestamp: Date.now(),
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
  });

  describe('Component Rendering', () => {
    it('should create the component', () => {
      component.toast = createToast();
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });

    it('should display toast message', () => {
      component.toast = createToast({ message: 'Hello World' });
      fixture.detectChanges();

      const message = debugElement.query(By.css('.toast-message'));
      expect(message.nativeElement.textContent).toBe('Hello World');
    });

    it('should apply success class for success type', () => {
      component.toast = createToast({ type: NotificationType.Success });
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.classList.contains('toast-success')).toBe(true);
    });

    it('should apply error class for error type', () => {
      component.toast = createToast({ type: NotificationType.Error });
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.classList.contains('toast-error')).toBe(true);
    });

    it('should apply warning class for warning type', () => {
      component.toast = createToast({ type: NotificationType.Warning });
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.classList.contains('toast-warning')).toBe(true);
    });

    it('should apply info class for info type', () => {
      component.toast = createToast({ type: NotificationType.Info });
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.classList.contains('toast-info')).toBe(true);
    });

    it('should render close button', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const closeButton = debugElement.query(By.css('.toast-close'));
      expect(closeButton).toBeTruthy();
    });

    it('should display correct icon for success', () => {
      component.toast = createToast({ type: NotificationType.Success });

      const icon = component.getIcon();
      expect(icon).toBe('✓');
    });

    it('should display correct icon for error', () => {
      component.toast = createToast({ type: NotificationType.Error });

      const icon = component.getIcon();
      expect(icon).toBe('✕');
    });

    it('should display correct icon for warning', () => {
      component.toast = createToast({ type: NotificationType.Warning });

      const icon = component.getIcon();
      expect(icon).toBe('⚠');
    });

    it('should display correct icon for info', () => {
      component.toast = createToast({ type: NotificationType.Info });

      const icon = component.getIcon();
      expect(icon).toBe('ⓘ');
    });
  });

  describe('Progress Bar', () => {
    it('should render progress bar for toast with duration', () => {
      component.toast = createToast({ duration: 3000 });
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      expect(progress).toBeTruthy();
    });

    it('should not render progress bar for toast without duration', () => {
      component.toast = createToast({ duration: 0 });
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      expect(progress).toBeFalsy();
    });

    it('should set animation duration based on toast duration', () => {
      component.toast = createToast({ duration: 5000 });
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      const style = progress.nativeElement.getAttribute('style');
      expect(style).toContain('5s');
    });
  });

  describe('Action Button', () => {
    it('should render action button if actionLabel provided', () => {
      component.toast = createToast({ actionLabel: 'Undo' });
      fixture.detectChanges();

      const actionButton = debugElement.query(By.css('.toast-action'));
      expect(actionButton).toBeTruthy();
      expect(actionButton.nativeElement.textContent).toBe('Undo');
    });

    it('should not render action button if actionLabel not provided', () => {
      component.toast = createToast({ actionLabel: undefined });
      fixture.detectChanges();

      const actionButton = debugElement.query(By.css('.toast-action'));
      expect(actionButton).toBeFalsy();
    });

    it('should call onAction callback when action button clicked', () => {
      const onAction = jasmine.createSpy('onAction');
      component.toast = createToast({ actionLabel: 'Undo', onAction });
      fixture.detectChanges();

      const actionButton = debugElement.query(By.css('.toast-action'));
      actionButton.nativeElement.click();

      expect(onAction).toHaveBeenCalled();
    });

    it('should dismiss toast after action button click', () => {
      spyOn(component, 'dismiss');
      component.toast = createToast({ actionLabel: 'Undo' });
      fixture.detectChanges();

      const actionButton = debugElement.query(By.css('.toast-action'));
      actionButton.nativeElement.click();

      expect(component.dismiss).toHaveBeenCalled();
    });
  });

  describe('Close Button', () => {
    it('should call dismiss when close button clicked', () => {
      spyOn(component, 'dismiss');
      component.toast = createToast();
      fixture.detectChanges();

      const closeButton = debugElement.query(By.css('.toast-close'));
      closeButton.nativeElement.click();

      expect(component.dismiss).toHaveBeenCalled();
    });

    it('should have accessible close button', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const closeButton = debugElement.query(By.css('.toast-close'));
      expect(closeButton.nativeElement.getAttribute('aria-label')).toBe(
        'Close notification'
      );
    });
  });

  describe('Accessibility', () => {
    it('should have role status', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.getAttribute('role')).toBe('status');
    });

    it('should have aria-live polite', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-label with message', () => {
      component.toast = createToast({ message: 'Test message' });

      const label = component.getAriaLabel();
      expect(label).toContain('Test message');
      expect(label).toContain('Success');
    });

    it('should have accessible close button with tabindex', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const closeButton = debugElement.query(By.css('.toast-close'));
      expect(closeButton.nativeElement.getAttribute('tabindex')).toBe('0');
    });
  });

  describe('Hover Behavior', () => {
    it('should pause progress on mouseenter', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      progress.nativeElement.dispatchEvent(new MouseEvent('mouseenter'));

      expect(component['progressPaused']).toBe(true);
    });

    it('should resume progress on mouseleave', () => {
      component.toast = createToast();
      component['progressPaused'] = true;
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      progress.nativeElement.dispatchEvent(new MouseEvent('mouseleave'));

      expect(component['progressPaused']).toBe(false);
    });
  });

  describe('Icon Class', () => {
    it('should return correct icon class', () => {
      component.toast = createToast({ type: NotificationType.Success });

      const iconClass = component.getIconClass();
      expect(iconClass).toBe('icon-success');
    });

    it('should apply icon class to icon element', () => {
      component.toast = createToast({ type: NotificationType.Error });
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.toast-icon'));
      expect(icon.nativeElement.classList.contains('icon-error')).toBe(true);
    });
  });

  describe('Lifecycle', () => {
    it('should initialize component', () => {
      component.toast = createToast();

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

  describe('Edge Cases', () => {
    it('should handle very long message', () => {
      const longMessage = 'x'.repeat(1000);
      component.toast = createToast({ message: longMessage });
      fixture.detectChanges();

      const message = debugElement.query(By.css('.toast-message'));
      expect(message.nativeElement.textContent).toBe(longMessage);
    });

    it('should handle empty message', () => {
      component.toast = createToast({ message: '' });
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });

    it('should handle zero duration', () => {
      component.toast = createToast({ duration: 0 });
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      expect(progress).toBeFalsy();
    });

    it('should handle very large duration', () => {
      component.toast = createToast({ duration: 60000 });
      fixture.detectChanges();

      const progress = debugElement.query(By.css('.toast-progress'));
      const style = progress.nativeElement.getAttribute('style');
      expect(style).toContain('60s');
    });

    it('should handle multiple toasts with same type', () => {
      const toast1 = createToast({ id: 'toast-1' });
      const toast2 = createToast({ id: 'toast-2' });

      component.toast = toast1;
      fixture.detectChanges();

      expect(component.toast.id).toBe('toast-1');

      component.toast = toast2;
      fixture.detectChanges();

      expect(component.toast.id).toBe('toast-2');
    });
  });

  describe('Visual States', () => {
    it('should render with success styling', () => {
      component.toast = createToast({
        type: NotificationType.Success,
      });
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.toast-icon'));
      expect(window.getComputedStyle(icon.nativeElement).color).toBeTruthy();
    });

    it('should render with error styling', () => {
      component.toast = createToast({
        type: NotificationType.Error,
      });
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.toast-icon'));
      expect(window.getComputedStyle(icon.nativeElement).color).toBeTruthy();
    });

    it('should have proper z-index for visibility', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      const zIndex = window.getComputedStyle(toast.nativeElement).zIndex;
      expect(parseInt(zIndex)).toBeGreaterThan(1000);
    });
  });

  describe('DOM Cleanup', () => {
    it('should properly initialize element', () => {
      component.toast = createToast();
      fixture.detectChanges();

      const toast = debugElement.query(By.css('[data-testid="toast-container"]'));
      expect(toast).toBeTruthy();
    });

    it('should have data-testid attribute', () => {
      component.toast = createToast({ id: 'test-123' });
      fixture.detectChanges();

      const toast = debugElement.query(By.css('.toast'));
      expect(toast.nativeElement.getAttribute('data-testid')).toBe('toast-container');
    });
  });
});
