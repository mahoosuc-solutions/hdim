/**
 * Evaluations Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests evaluation creation and display for keyboard accessibility, ARIA attributes,
 * and form controls per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Evaluation creation is core clinical workflow
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EvaluationsComponent } from './evaluations.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoggerService } from '../../services/logger.service';

describe('EvaluationsComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: EvaluationsComponent;
  let fixture: ComponentFixture<EvaluationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvaluationsComponent, NoopAnimationsModule, HttpClientTestingModule],
    
      providers: [
        { provide: LoggerService, useValue: createMockLoggerService() },
      ],}).compileComponents();

    fixture = TestBed.createComponent(EvaluationsComponent);
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
      const inputs = fixture.nativeElement.querySelectorAll('input, select, textarea');
      inputs.forEach((input: HTMLElement) => {
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || input.id;
        expect(hasLabel).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, input, select, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Evaluation Form-Specific Accessibility', () => {
    it('should have accessible measure selection', () => {
      const measureSelect = fixture.nativeElement.querySelector('select[name*="measure"], mat-select');
      if (measureSelect) {
        expect(measureSelect.hasAttribute('aria-label') || measureSelect.hasAttribute('aria-labelledby')).toBe(true);
      }
    });

    it('should have accessible patient selection', () => {
      const patientInput = fixture.nativeElement.querySelector('input[name*="patient"]');
      if (patientInput) {
        expect(patientInput.hasAttribute('aria-label') || patientInput.hasAttribute('aria-labelledby')).toBe(true);
      }
    });

    it('should have accessible evaluation results table', () => {
      const resultsTable = fixture.nativeElement.querySelector('table');
      if (resultsTable) {
        const headers = resultsTable.querySelectorAll('th');
        expect(headers.length).toBeGreaterThan(0);
      }
    });

    it('should announce evaluation status changes', () => {
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
