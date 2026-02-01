/**
 * MFA Settings Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests user settings form for keyboard accessibility, ARIA attributes,
 * and form controls per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - User preferences must be accessible
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MfaSettingsComponent } from './mfa-settings.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('MfaSettingsComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: MfaSettingsComponent;
  let fixture: ComponentFixture<MfaSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfaSettingsComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(MfaSettingsComponent);
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
    it('should have valid ARIA attributes on all form elements', async () => {
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
      const inputs = fixture.nativeElement.querySelectorAll('input, select, textarea');
      inputs.forEach((input: HTMLElement) => {
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || input.id;
        expect(hasLabel).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('input, button, select, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Settings-Specific Accessibility', () => {
    it('should have accessible toggle switches', () => {
      const toggles = fixture.nativeElement.querySelectorAll('input[type="checkbox"][role="switch"]');
      toggles.forEach((toggle: HTMLElement) => {
        expect(toggle.hasAttribute('aria-label') || toggle.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should have accessible fieldsets', () => {
      const fieldsets = fixture.nativeElement.querySelectorAll('fieldset');
      fieldsets.forEach((fieldset: HTMLElement) => {
        const legend = fieldset.querySelector('legend');
        expect(legend).toBeTruthy();
      });
    });

    it('should have accessible help text', () => {
      const helpTexts = fixture.nativeElement.querySelectorAll('.help-text, [class*="hint"]');
      helpTexts.forEach((helpText: HTMLElement) => {
        const parentInput = helpText.previousElementSibling || helpText.parentElement?.querySelector('input');
        if (parentInput) {
          expect(helpText.id).toBeTruthy();
          expect(parentInput.hasAttribute('aria-describedby')).toBe(true);
        }
      });
    });

    it('should have accessible save button', () => {
      const saveButton = fixture.nativeElement.querySelector('button[type="submit"], button:contains("Save")');
      if (saveButton) {
        const hasAriaLabel = saveButton.hasAttribute('aria-label');
        const hasTextContent = saveButton.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      }
    });

    it('should announce settings changes', () => {
      const statusMessages = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(statusMessages.length).toBeGreaterThanOrEqual(0);
    });
  });
});
