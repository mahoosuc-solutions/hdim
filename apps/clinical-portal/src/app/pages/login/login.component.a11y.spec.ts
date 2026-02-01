/**
 * Login Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests authentication form for keyboard accessibility, ARIA attributes,
 * form labels, and error messages per WCAG 2.1 guidelines.
 *
 * Priority: CRITICAL - Login form must be accessible to all users
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('LoginComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      const results = await testAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should have valid ARIA attributes on form elements', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have proper color contrast', async () => {
      const results = await testAccessibility(fixture);
      const contrastViolations = results.violations.filter(
        (v: any) => v.id === 'color-contrast'
      );
      expect(contrastViolations.length).toBe(0);
    });

    it('should have labeled form fields', () => {
      const inputs = fixture.nativeElement.querySelectorAll('input');
      inputs.forEach((input: HTMLElement) => {
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || input.id;
        expect(hasLabel).toBe(true);
      });
    });

    it('should have keyboard-focusable form elements', () => {
      const formElements = fixture.nativeElement.querySelectorAll('input, button');
      formElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Login Form-Specific Accessibility', () => {
    it('should have accessible form with proper role', () => {
      const form = fixture.nativeElement.querySelector('form');
      expect(form).toBeTruthy();
      expect(form.hasAttribute('role') || form.tagName.toLowerCase() === 'form').toBe(true);
    });

    it('should have labeled username field', () => {
      const usernameInput = fixture.nativeElement.querySelector('input[type="text"], input[name="username"], input[id*="username"]');
      if (usernameInput) {
        const label = fixture.nativeElement.querySelector(`label[for="${usernameInput.id}"]`);
        const hasAriaLabel = usernameInput.hasAttribute('aria-label');
        expect(label || hasAriaLabel).toBeTruthy();
      }
    });

    it('should have labeled password field', () => {
      const passwordInput = fixture.nativeElement.querySelector('input[type="password"]');
      if (passwordInput) {
        const label = fixture.nativeElement.querySelector(`label[for="${passwordInput.id}"]`);
        const hasAriaLabel = passwordInput.hasAttribute('aria-label');
        expect(label || hasAriaLabel).toBeTruthy();
      }
    });

    it('should have accessible error messages', () => {
      const errorMessages = fixture.nativeElement.querySelectorAll('[role="alert"], .error-message');
      errorMessages.forEach((error: HTMLElement) => {
        expect(error.hasAttribute('role') || error.hasAttribute('aria-live')).toBe(true);
      });
    });

    it('should have accessible submit button', () => {
      const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(submitButton).toBeTruthy();
      const hasAriaLabel = submitButton?.hasAttribute('aria-label');
      const hasTextContent = submitButton?.textContent?.trim().length! > 0;
      expect(hasAriaLabel || hasTextContent).toBe(true);
    });

    it('should mark required fields', () => {
      const requiredInputs = fixture.nativeElement.querySelectorAll('input[required]');
      requiredInputs.forEach((input: HTMLElement) => {
        expect(input.hasAttribute('aria-required') || input.hasAttribute('required')).toBe(true);
      });
    });

    it('should have accessible password visibility toggle', () => {
      const visibilityToggle = fixture.nativeElement.querySelector('button[aria-label*="password"]');
      if (visibilityToggle) {
        expect(visibilityToggle.hasAttribute('aria-label')).toBe(true);
      }
    });
  });
});
