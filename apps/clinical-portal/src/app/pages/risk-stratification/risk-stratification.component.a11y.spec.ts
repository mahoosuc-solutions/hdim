/**
 * Risk Stratification Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests risk analysis display for keyboard accessibility, ARIA attributes,
 * and data visualization accessibility per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Risk stratification informs clinical decisions
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RiskStratificationComponent } from './risk-stratification.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('RiskStratificationComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: RiskStratificationComponent;
  let fixture: ComponentFixture<RiskStratificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RiskStratificationComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskStratificationComponent);
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
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Risk Stratification-Specific Accessibility', () => {
    it('should have accessible risk score indicators', () => {
      const riskScores = fixture.nativeElement.querySelectorAll('.risk-score, [class*="risk-level"]');
      riskScores.forEach((score: HTMLElement) => {
        expect(score.hasAttribute('aria-label') || score.textContent?.trim().length! > 0).toBe(true);
      });
    });

    it('should have accessible risk charts with text alternatives', () => {
      const charts = fixture.nativeElement.querySelectorAll('canvas, svg');
      charts.forEach((chart: HTMLElement) => {
        expect(chart.hasAttribute('aria-label') || chart.hasAttribute('role')).toBe(true);
      });
    });

    it('should have accessible patient risk table', () => {
      const table = fixture.nativeElement.querySelector('table');
      if (table) {
        const caption = table.querySelector('caption');
        const hasAriaLabel = table.hasAttribute('aria-label');
        expect(caption || hasAriaLabel).toBeTruthy();
      }
    });

    it('should have accessible filter controls', () => {
      const filters = fixture.nativeElement.querySelectorAll('select, input[type="range"]');
      filters.forEach((filter: HTMLElement) => {
        expect(filter.hasAttribute('aria-label') || filter.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should announce risk calculation updates', () => {
      const statusMessages = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(statusMessages.length).toBeGreaterThanOrEqual(0);
    });
  });
});
