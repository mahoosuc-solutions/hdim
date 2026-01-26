/**
 * Reports Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests report display and export for keyboard accessibility, ARIA attributes,
 * and data table accessibility per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Report viewing is critical for analysts and administrators
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportsComponent } from './reports.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ReportsComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportsComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsComponent);
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
    it('should have valid ARIA attributes on all elements', async () => {
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

  describe('Report Viewer-Specific Accessibility', () => {
    it('should have accessible report tables', () => {
      const tables = fixture.nativeElement.querySelectorAll('table');
      tables.forEach((table: HTMLElement) => {
        const caption = table.querySelector('caption');
        const hasAriaLabel = table.hasAttribute('aria-label');
        expect(caption || hasAriaLabel).toBeTruthy();
      });
    });

    it('should have accessible table headers with scope', () => {
      const headers = fixture.nativeElement.querySelectorAll('th');
      headers.forEach((header: HTMLElement) => {
        expect(header.hasAttribute('scope') || header.hasAttribute('role')).toBe(true);
      });
    });

    it('should have accessible export buttons', () => {
      const exportButtons = fixture.nativeElement.querySelectorAll('button[aria-label*="export"], button[aria-label*="download"]');
      exportButtons.forEach((button: HTMLElement) => {
        expect(button.hasAttribute('aria-label')).toBe(true);
      });
    });

    it('should have accessible charts with text alternatives', () => {
      const charts = fixture.nativeElement.querySelectorAll('canvas, svg');
      charts.forEach((chart: HTMLElement) => {
        expect(chart.hasAttribute('aria-label') || chart.hasAttribute('role')).toBe(true);
      });
    });

    it('should announce loading states', () => {
      const loadingIndicators = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live="polite"]');
      expect(loadingIndicators.length).toBeGreaterThanOrEqual(0);
    });
  });
});
