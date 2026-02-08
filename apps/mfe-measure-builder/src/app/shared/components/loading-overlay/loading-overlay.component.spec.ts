import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoadingOverlayComponent } from './loading-overlay.component';
import { By } from '@angular/platform-browser';

/**
 * TDD Test Suite for LoadingOverlay Component
 *
 * This component provides loading state UI with a spinner and optional message.
 * Tests cover visibility, accessibility, styling modes, and message display.
 */
describe('LoadingOverlayComponent (TDD)', () => {
  let component: LoadingOverlayComponent;
  let fixture: ComponentFixture<LoadingOverlayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingOverlayComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingOverlayComponent);
    component = fixture.componentInstance;
  });

  // ============================================================================
  // 1. Component Initialization (3 tests)
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default values', () => {
      expect(component.isLoading).toBeFalsy();
      expect(component.fullscreen).toBe(false);
      expect(component.spinnerSize).toBe(48);
      expect(component.message).toBeUndefined();
    });

    it('should not display overlay when not loading', () => {
      component.isLoading = false;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay).toBeNull();
    });
  });

  // ============================================================================
  // 2. Visibility Control (4 tests)
  // ============================================================================
  describe('Visibility Control', () => {
    it('should display overlay when isLoading is true', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay).toBeTruthy();
    });

    it('should hide overlay when isLoading changes to false', () => {
      component.isLoading = true;
      fixture.detectChanges();
      let overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay).toBeTruthy();

      component.isLoading = false;
      fixture.detectChanges();
      overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay).toBeNull();
    });

    it('should show spinner when loading', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner).toBeTruthy();
    });

    it('should toggle overlay visibility dynamically', () => {
      // Start hidden
      component.isLoading = false;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeNull();

      // Show
      component.isLoading = true;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeTruthy();

      // Hide again
      component.isLoading = false;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeNull();
    });
  });

  // ============================================================================
  // 3. Fullscreen Mode (3 tests)
  // ============================================================================
  describe('Fullscreen Mode', () => {
    it('should not have fullscreen class by default', () => {
      component.isLoading = true;
      component.fullscreen = false;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(false);
    });

    it('should apply fullscreen class when fullscreen is true', () => {
      component.isLoading = true;
      component.fullscreen = true;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(true);
    });

    it('should remove fullscreen class when changed to false', () => {
      component.isLoading = true;
      component.fullscreen = true;
      fixture.detectChanges();
      let overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(true);

      component.fullscreen = false;
      fixture.detectChanges();
      overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(false);
    });
  });

  // ============================================================================
  // 4. Message Display (4 tests)
  // ============================================================================
  describe('Message Display', () => {
    it('should not display message when message is undefined', () => {
      component.isLoading = true;
      component.message = undefined;
      fixture.detectChanges();

      const messageEl = fixture.debugElement.query(By.css('.loading-message'));
      expect(messageEl).toBeNull();
    });

    it('should display message when provided', () => {
      component.isLoading = true;
      component.message = 'Loading patient data...';
      fixture.detectChanges();

      const messageEl = fixture.debugElement.query(By.css('.loading-message'));
      expect(messageEl).toBeTruthy();
      expect(messageEl.nativeElement.textContent).toContain('Loading patient data...');
    });

    it('should update message text dynamically', () => {
      component.isLoading = true;
      component.message = 'Loading...';
      fixture.detectChanges();
      let messageEl = fixture.debugElement.query(By.css('.loading-message'));
      expect(messageEl.nativeElement.textContent).toContain('Loading...');

      component.message = 'Processing...';
      fixture.detectChanges();
      messageEl = fixture.debugElement.query(By.css('.loading-message'));
      expect(messageEl.nativeElement.textContent).toContain('Processing...');
    });

    it('should handle empty string message', () => {
      component.isLoading = true;
      component.message = '';
      fixture.detectChanges();

      const messageEl = fixture.debugElement.query(By.css('.loading-message'));
      expect(messageEl).toBeNull();
    });
  });

  // ============================================================================
  // 5. Spinner Configuration (3 tests)
  // ============================================================================
  describe('Spinner Configuration', () => {
    it('should use default spinner size of 48', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner.componentInstance.diameter).toBe(48);
    });

    it('should apply custom spinner size', () => {
      component.isLoading = true;
      component.spinnerSize = 100;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner.componentInstance.diameter).toBe(100);
    });

    it('should update spinner size dynamically', () => {
      component.isLoading = true;
      component.spinnerSize = 40;
      fixture.detectChanges();
      let spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner.componentInstance.diameter).toBe(40);

      component.spinnerSize = 80;
      fixture.detectChanges();
      spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner.componentInstance.diameter).toBe(80);
    });
  });

  // ============================================================================
  // 6. Accessibility (4 tests)
  // ============================================================================
  describe('Accessibility', () => {
    it('should have role="status" attribute', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.getAttribute('role')).toBe('status');
    });

    it('should have aria-live="polite" attribute', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-busy="true" when loading', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay.nativeElement.getAttribute('aria-busy')).toBe('true');
    });

    it('should have aria-label on spinner', () => {
      component.isLoading = true;
      component.message = 'Loading data...';
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner.nativeElement.getAttribute('aria-label')).toBe('Loading data...');
    });
  });

  // ============================================================================
  // 7. Integration Scenarios (3 tests)
  // ============================================================================
  describe('Integration Scenarios', () => {
    it('should work as section overlay with message', () => {
      component.isLoading = true;
      component.fullscreen = false;
      component.message = 'Loading section...';
      component.spinnerSize = 40;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      const message = fixture.debugElement.query(By.css('.loading-message'));

      expect(overlay).toBeTruthy();
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(false);
      expect(spinner.componentInstance.diameter).toBe(40);
      expect(message.nativeElement.textContent).toContain('Loading section...');
    });

    it('should work as fullscreen overlay without message', () => {
      component.isLoading = true;
      component.fullscreen = true;
      component.message = undefined;
      fixture.detectChanges();

      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      const message = fixture.debugElement.query(By.css('.loading-message'));

      expect(overlay).toBeTruthy();
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(true);
      expect(spinner).toBeTruthy();
      expect(message).toBeNull();
    });

    it('should handle rapid state changes', () => {
      // Show
      component.isLoading = true;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeTruthy();

      // Hide
      component.isLoading = false;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeNull();

      // Show again with different config
      component.isLoading = true;
      component.fullscreen = true;
      component.message = 'Processing...';
      fixture.detectChanges();
      const overlay = fixture.debugElement.query(By.css('.loading-overlay'));
      expect(overlay).toBeTruthy();
      expect(overlay.nativeElement.classList.contains('fullscreen')).toBe(true);
    });
  });
});
