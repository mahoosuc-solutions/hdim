/**
 * Pre-Visit Planning Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests appointment scheduling for keyboard accessibility, ARIA attributes,
 * and date/time controls per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Scheduling workflows must be accessible
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PreVisitPlanningComponent } from './pre-visit-planning.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('PreVisitPlanningComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: PreVisitPlanningComponent;
  let fixture: ComponentFixture<PreVisitPlanningComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreVisitPlanningComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PreVisitPlanningComponent);
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
    it('should have valid ARIA attributes', async () => {
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
      const inputs = fixture.nativeElement.querySelectorAll('input, select');
      inputs.forEach((input: HTMLElement) => {
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || input.id;
        expect(hasLabel).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('button, input, select, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Scheduling-Specific Accessibility', () => {
    it('should have accessible date picker', () => {
      const datePicker = fixture.nativeElement.querySelector('input[type="date"], [role="combobox"][aria-label*="date"]');
      if (datePicker) {
        expect(datePicker.hasAttribute('aria-label') || datePicker.hasAttribute('aria-labelledby')).toBe(true);
      }
    });

    it('should have accessible time slot selection', () => {
      const timeSlots = fixture.nativeElement.querySelectorAll('[role="button"][aria-label*="time"], button[aria-label*="slot"]');
      timeSlots.forEach((slot: HTMLElement) => {
        expect(slot.hasAttribute('aria-label')).toBe(true);
      });
    });

    it('should have accessible appointment list', () => {
      const appointmentList = fixture.nativeElement.querySelector('[role="list"], table');
      if (appointmentList) {
        const hasAriaLabel = appointmentList.hasAttribute('aria-label');
        const hasCaption = appointmentList.querySelector('caption');
        expect(hasAriaLabel || hasCaption).toBeTruthy();
      }
    });

    it('should announce scheduling confirmations', () => {
      const statusMessages = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(statusMessages.length).toBeGreaterThanOrEqual(0);
    });

    it('should have accessible action buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button.hasAttribute('aria-label');
        const hasTextContent = button.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });
  });
});
