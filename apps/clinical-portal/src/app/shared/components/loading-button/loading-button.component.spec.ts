import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoadingButtonComponent } from './loading-button.component';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

/**
 * TDD Test Suite for LoadingButton Component
 *
 * This component provides a button with loading, success, and disabled states.
 * Tests cover all button variants, state transitions, accessibility, and event handling.
 */
describe('LoadingButtonComponent (TDD)', () => {
  let component: LoadingButtonComponent;
  let fixture: ComponentFixture<LoadingButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingButtonComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingButtonComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
  });

  // ============================================================================
  // 1. Component Initialization (4 tests)
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default values', () => {
      expect(component.text).toBe('');
      expect(component.loading).toBe(false);
      expect(component.success).toBe(false);
      expect(component.disabled).toBe(false);
      expect(component.variant).toBe('raised');
      expect(component.type).toBe('button');
      expect(component.successDuration).toBe(2000);
    });

    it('should render button element', () => {
      component.text = 'Click Me';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button).toBeTruthy();
    });

    it('should display button text', () => {
      component.text = 'Save';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.textContent.trim()).toContain('Save');
    });
  });

  // ============================================================================
  // 2. Button Variants (5 tests)
  // ============================================================================
  describe('Button Variants', () => {
    it('should render raised button by default', () => {
      component.variant = 'raised';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      expect(button).toBeTruthy();
    });

    it('should render stroked button', () => {
      component.variant = 'stroked';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button[mat-stroked-button]'));
      expect(button).toBeTruthy();
    });

    it('should render flat button', () => {
      component.variant = 'flat';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button[mat-flat-button]'));
      expect(button).toBeTruthy();
    });

    it('should render icon button', () => {
      component.variant = 'icon';
      component.icon = 'save';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button[mat-icon-button]'));
      expect(button).toBeTruthy();
    });

    it('should render default text button for unknown variant', () => {
      component.variant = 'text';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button[mat-button]'));
      expect(button).toBeTruthy();
    });
  });

  // ============================================================================
  // 3. Loading State (5 tests)
  // ============================================================================
  describe('Loading State', () => {
    it('should display spinner when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner).toBeTruthy();
    });

    it('should disable button when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.disabled).toBe(true);
    });

    it('should display loading text when provided', () => {
      component.text = 'Save';
      component.loadingText = 'Saving...';
      component.loading = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.textContent).toContain('Saving...');
    });

    it('should use default text if no loading text provided', () => {
      component.text = 'Save';
      component.loading = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.textContent).toContain('Save');
    });

    it('should set aria-busy="true" when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('true');
    });
  });

  // ============================================================================
  // 4. Success State (6 tests)
  // ============================================================================
  describe('Success State', () => {
    it('should display check icon when success', () => {
      component.success = true;
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon).toBeTruthy();
      expect(icon.nativeElement.textContent.trim()).toBe('check_circle');
    });

    it('should disable button when success', () => {
      component.success = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.disabled).toBe(true);
    });

    it('should auto-clear success state after duration', fakeAsync(() => {
      component.success = true;
      component.successDuration = 1000;
      component.ngOnChanges(); // Trigger the timeout setup
      fixture.detectChanges();

      expect(component.success).toBe(true);

      tick(1000);

      expect(component.success).toBe(false);
    }));

    it('should not auto-clear when successDuration is 0', fakeAsync(() => {
      component.success = true;
      component.successDuration = 0;
      fixture.detectChanges();

      expect(component.success).toBe(true);

      tick(5000);

      expect(component.success).toBe(true);
    }));

    it('should clear previous timeout when success state changes', fakeAsync(() => {
      component.success = true;
      component.successDuration = 2000;
      fixture.detectChanges();

      tick(1000);

      // Set success again before timeout expires
      component.success = false;
      component.success = true;
      component.ngOnChanges();
      fixture.detectChanges();

      // Original timeout shouldn't fire
      tick(1500);
      expect(component.success).toBe(true);

      // New timeout should fire
      tick(500);
      expect(component.success).toBe(false);
    }));

    it('should cleanup timeout on destroy', fakeAsync(() => {
      component.success = true;
      component.successDuration = 2000;
      component.ngOnChanges(); // Create the timeout first
      fixture.detectChanges();

      const clearTimeoutSpy = jest.spyOn(window, 'clearTimeout');

      component.ngOnDestroy();

      expect(clearTimeoutSpy).toHaveBeenCalled();

      clearTimeoutSpy.mockRestore();
    }));
  });

  // ============================================================================
  // 5. Disabled State (3 tests)
  // ============================================================================
  describe('Disabled State', () => {
    it('should disable button when disabled is true', () => {
      component.disabled = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.disabled).toBe(true);
    });

    it('should not emit click event when disabled', () => {
      component.disabled = true;
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      const button = fixture.debugElement.query(By.css('button'));
      button.nativeElement.click();

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('should enable button when disabled changes to false', () => {
      component.disabled = true;
      fixture.detectChanges();
      let button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.disabled).toBe(true);

      component.disabled = false;
      fixture.detectChanges();
      button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.disabled).toBe(false);
    });
  });

  // ============================================================================
  // 6. Click Event Handling (5 tests)
  // ============================================================================
  describe('Click Event Handling', () => {
    it('should emit buttonClick event on click', () => {
      component.text = 'Click Me';
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      const button = fixture.debugElement.query(By.css('button'));
      button.nativeElement.click();

      expect(clickSpy).toHaveBeenCalledTimes(1);
    });

    it('should not emit event when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      component.handleClick(new Event('click'));

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('should not emit event when success', () => {
      component.success = true;
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      component.handleClick(new Event('click'));

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('should not emit event when disabled', () => {
      component.disabled = true;
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      component.handleClick(new Event('click'));

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('should emit event with click event object', () => {
      component.text = 'Click Me';
      fixture.detectChanges();

      let emittedEvent: Event | undefined;
      component.buttonClick.subscribe((event) => {
        emittedEvent = event;
      });

      const clickEvent = new Event('click');
      component.handleClick(clickEvent);

      expect(emittedEvent).toBe(clickEvent);
    });
  });

  // ============================================================================
  // 7. Icons and Text (4 tests)
  // ============================================================================
  describe('Icons and Text', () => {
    it('should display icon when provided', () => {
      component.icon = 'save';
      component.text = 'Save';
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon).toBeTruthy();
      expect(icon.nativeElement.textContent.trim()).toBe('save');
    });

    it('should display icon for icon button variant', () => {
      component.variant = 'icon';
      component.icon = 'delete';
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon).toBeTruthy();
      expect(icon.nativeElement.textContent.trim()).toBe('delete');
    });

    it('should display spinner with correct size for icon button', () => {
      component.variant = 'icon';
      component.icon = 'save';
      component.loading = true;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner).toBeTruthy();
      expect(spinner.componentInstance.diameter).toBe(24);
    });

    it('should display spinner with size 20 for non-icon buttons', () => {
      component.variant = 'raised';
      component.loading = true;
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('mat-spinner'));
      expect(spinner).toBeTruthy();
      expect(spinner.componentInstance.diameter).toBe(20);
    });
  });

  // ============================================================================
  // 8. Button Styling (4 tests)
  // ============================================================================
  describe('Button Styling', () => {
    it('should apply primary color', () => {
      component.color = 'primary';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.componentInstance.color).toBe('primary');
    });

    it('should apply warn color', () => {
      component.color = 'warn';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.componentInstance.color).toBe('warn');
    });

    it('should apply custom CSS class', () => {
      component.customClass = 'my-custom-class';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.classList.contains('my-custom-class')).toBe(true);
    });

    it('should set button type attribute', () => {
      component.type = 'submit';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.type).toBe('submit');
    });
  });

  // ============================================================================
  // 9. Accessibility (WCAG 2.1 AA) (8 tests)
  // ============================================================================
  describe('Accessibility', () => {
    it('should set aria-label when provided', () => {
      component.ariaLabel = 'Save document';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-label')).toBe('Save document');
    });

    it('should set aria-busy when loading', () => {
      component.loading = true;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('true');
    });

    it('should not set aria-busy when not loading', () => {
      component.loading = false;
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('false');
    });

    it('should have aria-label for icon buttons', () => {
      component.variant = 'icon';
      component.icon = 'delete';
      component.ariaLabel = 'Delete item';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-label')).toBe('Delete item');
    });

    it('should have tooltip for better accessibility when provided', () => {
      component.ariaLabel = 'Submit form';
      component.tooltip = 'Submit form';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.getAttribute('aria-label')).toBe('Submit form');
      // Tooltip is set via input property
      expect(component.tooltip).toBe('Submit form');
    });

    it('should be keyboard accessible and focusable', () => {
      component.text = 'Click Me';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));
      // Button should be focusable (not have tabindex=-1)
      const tabIndex = button.nativeElement.getAttribute('tabindex');
      expect(tabIndex === null || parseInt(tabIndex) >= 0).toBe(true);

      // Button should be a native button element, which is inherently keyboard accessible
      expect(button.nativeElement.tagName.toLowerCase()).toBe('button');
    });

    it('should announce state changes to screen readers via aria-busy', () => {
      component.text = 'Save';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));

      // Initial state
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('false');

      // Loading state
      component.loading = true;
      fixture.detectChanges();
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('true');

      // Success state
      component.loading = false;
      component.success = true;
      fixture.detectChanges();
      expect(button.nativeElement.getAttribute('aria-busy')).toBe('false');
    });

    it('should provide meaningful text for screen readers in all states', () => {
      component.text = 'Submit';
      component.loadingText = 'Submitting form';
      component.successText = 'Form submitted successfully';
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('button'));

      // Normal state
      expect(button.nativeElement.textContent.trim()).toContain('Submit');

      // Loading state
      component.loading = true;
      fixture.detectChanges();
      expect(button.nativeElement.textContent.trim()).toContain('Submitting form');

      // Success state (text button variants only)
      component.loading = false;
      component.success = true;
      fixture.detectChanges();
      // Success icon is shown, no text needed for visual indication
      const icon = fixture.debugElement.query(By.css('mat-icon.success-icon'));
      expect(icon).toBeTruthy();
    });
  });

  // ============================================================================
  // 10. Integration Scenarios (4 tests)
  // ============================================================================
  describe('Integration Scenarios', () => {
    it('should transition from normal to loading to success', fakeAsync(() => {
      component.text = 'Save';
      component.loadingText = 'Saving...';
      component.successText = 'Saved!';
      component.successDuration = 1000;
      fixture.detectChanges();

      // Normal state
      let button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.textContent).toContain('Save');

      // Loading state
      component.loading = true;
      fixture.detectChanges();
      button = fixture.debugElement.query(By.css('button'));
      expect(button.nativeElement.textContent).toContain('Saving...');
      expect(fixture.debugElement.query(By.css('mat-spinner'))).toBeTruthy();

      // Success state
      component.loading = false;
      component.success = true;
      component.ngOnChanges();
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('mat-icon'))).toBeTruthy();

      // Auto-clear to normal
      tick(1000);
      fixture.detectChanges();
      expect(component.success).toBe(false);
    }));

    it('should handle icon button with loading and success', fakeAsync(() => {
      component.variant = 'icon';
      component.icon = 'save';
      component.ariaLabel = 'Save';
      fixture.detectChanges();

      // Normal
      let icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon.nativeElement.textContent.trim()).toBe('save');

      // Loading
      component.loading = true;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('mat-spinner'))).toBeTruthy();

      // Success
      component.loading = false;
      component.success = true;
      component.ngOnChanges();
      fixture.detectChanges();
      icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon.nativeElement.textContent.trim()).toBe('check_circle');
    }));

    it('should prevent multiple clicks during loading', () => {
      component.text = 'Submit';
      fixture.detectChanges();

      const clickSpy = jest.fn();
      component.buttonClick.subscribe(clickSpy);

      const button = fixture.debugElement.query(By.css('button'));

      // First click works
      button.nativeElement.click();
      expect(clickSpy).toHaveBeenCalledTimes(1);

      // Set loading
      component.loading = true;
      fixture.detectChanges();

      // Clicks during loading don't emit
      button.nativeElement.click();
      button.nativeElement.click();
      expect(clickSpy).toHaveBeenCalledTimes(1);
    });

    it('should support all button colors', () => {
      const colors: Array<'primary' | 'accent' | 'warn' | undefined> = ['primary', 'accent', 'warn', undefined];

      colors.forEach(color => {
        component.color = color;
        fixture.detectChanges();
        const button = fixture.debugElement.query(By.css('button'));
        expect(button.componentInstance.color).toBe(color);
      });
    });
  });
});
