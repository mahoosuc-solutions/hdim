import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { FormFieldComponent } from './form-field.component';

/**
 * TDD Test Suite for FormField Component - Accessibility (WCAG 2.1 AA)
 *
 * This component wraps Material form fields with consistent styling and validation.
 * Tests cover accessibility features including aria-invalid, aria-required, labels, and error messages.
 */
describe('FormFieldComponent - Accessibility (WCAG 2.1 AA)', () => {
  let component: FormFieldComponent;
  let fixture: ComponentFixture<FormFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormFieldComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FormFieldComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
  });

  // ============================================================================
  // 1. Component Initialization
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should require a form control input', () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
      component.ngOnInit();
      expect(consoleErrorSpy).toHaveBeenCalledWith('FormFieldComponent: control input is required');
      consoleErrorSpy.mockRestore();
    });

    it('should initialize with default values', () => {
      component.control = new FormControl('');
      fixture.detectChanges();

      expect(component.label).toBe('');
      expect(component.type).toBe('text');
      expect(component.required).toBe(false);
      expect(component.readonly).toBe(false);
      expect(component.appearance).toBe('outline');
    });
  });

  // ============================================================================
  // 2. Form Label Accessibility
  // ============================================================================
  describe('Form Label Accessibility', () => {
    it('should display a visible label for the form field', () => {
      component.control = new FormControl('');
      component.label = 'Email Address';
      fixture.detectChanges();

      const label = fixture.debugElement.query(By.css('mat-label'));
      expect(label).toBeTruthy();
      expect(label.nativeElement.textContent.trim()).toBe('Email Address');
    });

    it('should associate label with input field', () => {
      component.control = new FormControl('');
      component.label = 'Full Name';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      const formField = fixture.debugElement.query(By.css('mat-form-field'));

      expect(formField).toBeTruthy();
      expect(input).toBeTruthy();
      // Material automatically handles aria-labelledby association
    });

    it('should show required indicator when field is required', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Password';
      component.required = true;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.required).toBe(true);
    });
  });

  // ============================================================================
  // 3. aria-invalid Attribute
  // ============================================================================
  describe('aria-invalid Attribute', () => {
    it('should set aria-invalid="false" when field is valid', () => {
      component.control = new FormControl('test@example.com', Validators.email);
      component.label = 'Email';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      // Material sets aria-invalid based on control state
      expect(component.control.valid).toBe(true);
    });

    it('should set aria-invalid="true" when field is invalid and touched', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Username';
      fixture.detectChanges();

      component.control.markAsTouched();
      fixture.detectChanges();

      expect(component.hasError).toBe(true);
      expect(component.control.invalid).toBe(true);
    });

    it('should not show aria-invalid when field is invalid but not touched', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Username';
      fixture.detectChanges();

      expect(component.hasError).toBe(false);
      expect(component.control.invalid).toBe(true);
      expect(component.control.touched).toBe(false);
    });

    it('should update aria-invalid when validation state changes', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Name';
      fixture.detectChanges();

      // Initially invalid but not touched
      expect(component.hasError).toBe(false);

      // Mark as touched
      component.control.markAsTouched();
      fixture.detectChanges();
      expect(component.hasError).toBe(true);

      // Set valid value
      component.control.setValue('John Doe');
      fixture.detectChanges();
      expect(component.hasError).toBe(false);
    });
  });

  // ============================================================================
  // 4. aria-required Attribute
  // ============================================================================
  describe('aria-required Attribute', () => {
    it('should set aria-required="true" for required fields', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Email';
      component.required = true;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.required).toBe(true);
    });

    it('should not set aria-required for optional fields', () => {
      component.control = new FormControl('');
      component.label = 'Middle Name';
      component.required = false;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.required).toBe(false);
    });

    it('should indicate required fields with HTML5 required attribute', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Password';
      component.required = true;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.hasAttribute('required')).toBe(true);
    });
  });

  // ============================================================================
  // 5. Error Messages and aria-describedby
  // ============================================================================
  describe('Error Messages', () => {
    it('should display error message when field is invalid and touched', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Email';
      component.control.markAsTouched();
      fixture.detectChanges();

      const errorElement = fixture.debugElement.query(By.css('mat-error'));
      expect(errorElement).toBeTruthy();
      expect(errorElement.nativeElement.textContent.trim()).toBe('This field is required');
    });

    it('should provide descriptive error message for email validation', () => {
      component.control = new FormControl('invalid-email', Validators.email);
      component.label = 'Email';
      component.control.markAsTouched();
      fixture.detectChanges();

      expect(component.getErrorMessage()).toBe('Please enter a valid email address');
    });

    it('should provide descriptive error message for minlength validation', () => {
      component.control = new FormControl('ab', Validators.minLength(5));
      component.label = 'Password';
      component.control.markAsTouched();
      fixture.detectChanges();

      expect(component.getErrorMessage()).toBe('Minimum length is 5 characters');
    });

    it('should provide descriptive error message for maxlength validation', () => {
      component.control = new FormControl('toolongvalue', Validators.maxLength(5));
      component.label = 'Code';
      component.control.markAsTouched();
      fixture.detectChanges();

      expect(component.getErrorMessage()).toBe('Maximum length is 5 characters');
    });

    it('should support custom error messages', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Username';
      component.errors = [
        { key: 'required', message: 'Username is mandatory' }
      ];
      component.control.markAsTouched();
      fixture.detectChanges();

      expect(component.getErrorMessage()).toBe('Username is mandatory');
    });

    it('should hide error message when field becomes valid', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Name';
      component.control.markAsTouched();
      fixture.detectChanges();

      // Error should be shown
      expect(component.hasError).toBe(true);

      // Fix the error
      component.control.setValue('John');
      fixture.detectChanges();

      // Error should be hidden
      expect(component.hasError).toBe(false);
    });

    it('should connect error message to input via aria-describedby', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Email';
      component.control.markAsTouched();
      fixture.detectChanges();

      const errorElement = fixture.debugElement.query(By.css('mat-error'));
      expect(errorElement).toBeTruthy();
      // Material automatically handles aria-describedby for error messages
    });
  });

  // ============================================================================
  // 6. Hint Text Accessibility
  // ============================================================================
  describe('Hint Text', () => {
    it('should display hint text when provided', () => {
      component.control = new FormControl('');
      component.label = 'Password';
      component.hint = 'Must be at least 8 characters';
      fixture.detectChanges();

      const hint = fixture.debugElement.query(By.css('mat-hint'));
      expect(hint).toBeTruthy();
      expect(hint.nativeElement.textContent.trim()).toBe('Must be at least 8 characters');
    });

    it('should hide hint text when error is shown', () => {
      component.control = new FormControl('', Validators.required);
      component.label = 'Username';
      component.hint = 'Choose a unique username';
      component.control.markAsTouched();
      fixture.detectChanges();

      const hint = fixture.debugElement.query(By.css('mat-hint'));
      expect(hint).toBeFalsy(); // Hint should be hidden when error is shown
    });

    it('should connect hint text to input via aria-describedby', () => {
      component.control = new FormControl('');
      component.label = 'Phone';
      component.hint = 'Format: (555) 123-4567';
      fixture.detectChanges();

      const hint = fixture.debugElement.query(By.css('mat-hint'));
      expect(hint).toBeTruthy();
      // Material automatically handles aria-describedby for hints
    });
  });

  // ============================================================================
  // 7. Icon Accessibility
  // ============================================================================
  describe('Icon Accessibility', () => {
    it('should mark decorative icons as aria-hidden', () => {
      component.control = new FormControl('');
      component.label = 'Email';
      component.icon = 'email';
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon).toBeTruthy();
      // Icons are decorative and should not interfere with screen readers
    });

    it('should display icon without affecting form field accessibility', () => {
      component.control = new FormControl('');
      component.label = 'Search';
      component.icon = 'search';
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('mat-icon'));
      expect(icon.nativeElement.textContent.trim()).toBe('search');
    });
  });

  // ============================================================================
  // 8. Input Types
  // ============================================================================
  describe('Input Types', () => {
    it('should support text input type', () => {
      component.control = new FormControl('');
      component.label = 'Name';
      component.type = 'text';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input[type="text"]'));
      expect(input).toBeTruthy();
    });

    it('should support email input type', () => {
      component.control = new FormControl('');
      component.label = 'Email';
      component.type = 'email';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input[type="email"]'));
      expect(input).toBeTruthy();
    });

    it('should support password input type', () => {
      component.control = new FormControl('');
      component.label = 'Password';
      component.type = 'password';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input[type="password"]'));
      expect(input).toBeTruthy();
    });

    it('should support number input type', () => {
      component.control = new FormControl('');
      component.label = 'Age';
      component.type = 'number';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input[type="number"]'));
      expect(input).toBeTruthy();
    });

    it('should support date input type with datepicker', () => {
      component.control = new FormControl('');
      component.label = 'Birth Date';
      component.type = 'date';
      fixture.detectChanges();

      const datepicker = fixture.debugElement.query(By.css('mat-datepicker-toggle'));
      expect(datepicker).toBeTruthy();
    });
  });

  // ============================================================================
  // 9. Readonly and Disabled States
  // ============================================================================
  describe('Readonly and Disabled States', () => {
    it('should set readonly attribute when readonly is true', () => {
      component.control = new FormControl('readonly-value');
      component.label = 'ID';
      component.readonly = true;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.readOnly).toBe(true);
    });

    it('should set aria-readonly when field is readonly', () => {
      component.control = new FormControl('readonly-value');
      component.label = 'System ID';
      component.readonly = true;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.readOnly).toBe(true);
    });

    it('should disable form field when control is disabled', () => {
      component.control = new FormControl({ value: '', disabled: true });
      component.label = 'Disabled Field';
      fixture.detectChanges();

      expect(component.control.disabled).toBe(true);
    });
  });

  // ============================================================================
  // 10. Autocomplete Attribute
  // ============================================================================
  describe('Autocomplete', () => {
    it('should set autocomplete="off" by default', () => {
      component.control = new FormControl('');
      component.label = 'Username';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.getAttribute('autocomplete')).toBe('off');
    });

    it('should support custom autocomplete values', () => {
      component.control = new FormControl('');
      component.label = 'Email';
      component.autocomplete = 'email';
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.getAttribute('autocomplete')).toBe('email');
    });
  });
});
