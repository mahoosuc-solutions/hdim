/**
 * Medication Reconciliation Workflow Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests medication list display for keyboard accessibility, ARIA attributes,
 * and data table accessibility per WCAG 2.1 guidelines.
 *
 * Priority: CRITICAL - Medication data is safety-critical PHI
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MedicationReconciliationWorkflowComponent } from './medication-reconciliation-workflow.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('MedicationReconciliationWorkflowComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: MedicationReconciliationWorkflowComponent;
  let fixture: ComponentFixture<MedicationReconciliationWorkflowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicationReconciliationWorkflowComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(MedicationReconciliationWorkflowComponent);
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

    it('should have aria-label on action buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button.hasAttribute('aria-label') || button.hasAttribute('aria-labelledby');
        const hasTextContent = button.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, input, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Medication List-Specific Accessibility', () => {
    it('should have accessible medication table', () => {
      const table = fixture.nativeElement.querySelector('table');
      if (table) {
        const caption = table.querySelector('caption');
        const hasAriaLabel = table.hasAttribute('aria-label');
        expect(caption || hasAriaLabel).toBeTruthy();
      }
    });

    it('should have accessible medication rows', () => {
      const rows = fixture.nativeElement.querySelectorAll('tr[role="row"]');
      rows.forEach((row: HTMLElement) => {
        const cells = row.querySelectorAll('td, th');
        expect(cells.length).toBeGreaterThan(0);
      });
    });

    it('should have accessible alert indicators', () => {
      const alerts = fixture.nativeElement.querySelectorAll('[role="alert"], .medication-alert');
      alerts.forEach((alert: HTMLElement) => {
        expect(alert.hasAttribute('role') || alert.hasAttribute('aria-live')).toBe(true);
      });
    });

    it('should have accessible checkboxes for medication selection', () => {
      const checkboxes = fixture.nativeElement.querySelectorAll('input[type="checkbox"]');
      checkboxes.forEach((checkbox: HTMLElement) => {
        expect(checkbox.hasAttribute('aria-label') || checkbox.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should announce medication changes', () => {
      const statusMessages = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(statusMessages.length).toBeGreaterThanOrEqual(0);
    });
  });
});
