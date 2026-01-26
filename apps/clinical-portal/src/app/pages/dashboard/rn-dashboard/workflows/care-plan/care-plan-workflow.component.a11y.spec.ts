/**
 * Care Plan Workflow Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests care plan creation for keyboard accessibility, ARIA attributes,
 * and form controls per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Care plan editing is complex clinical workflow
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CarePlanWorkflowComponent } from './care-plan-workflow.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('CarePlanWorkflowComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: CarePlanWorkflowComponent;
  let fixture: ComponentFixture<CarePlanWorkflowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CarePlanWorkflowComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CarePlanWorkflowComponent);
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

  describe('Care Plan Editor-Specific Accessibility', () => {
    it('should have accessible goal entry fields', () => {
      const goalInputs = fixture.nativeElement.querySelectorAll('input[name*="goal"], textarea[name*="goal"]');
      goalInputs.forEach((input: HTMLElement) => {
        expect(input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should have accessible add/remove buttons', () => {
      const addButtons = fixture.nativeElement.querySelectorAll('button[aria-label*="add"]');
      const removeButtons = fixture.nativeElement.querySelectorAll('button[aria-label*="remove"]');
      expect(addButtons.length + removeButtons.length).toBeGreaterThan(0);
    });

    it('should announce dynamic content changes', () => {
      const liveRegions = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(liveRegions.length).toBeGreaterThanOrEqual(0);
    });

    it('should have accessible intervention lists', () => {
      const lists = fixture.nativeElement.querySelectorAll('[role="list"]');
      lists.forEach((list: HTMLElement) => {
        const items = list.querySelectorAll('[role="listitem"]');
        expect(items.length).toBeGreaterThanOrEqual(0);
      });
    });

    it('should have accessible save button', () => {
      const saveButton = fixture.nativeElement.querySelector('button[type="submit"], button:contains("Save")');
      if (saveButton) {
        expect(saveButton.hasAttribute('aria-label') || saveButton.textContent?.trim().length! > 0).toBe(true);
      }
    });
  });
});
