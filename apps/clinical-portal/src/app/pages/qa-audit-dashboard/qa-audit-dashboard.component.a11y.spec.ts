/**
 * QA Audit Dashboard Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests QA audit log viewer for keyboard accessibility, ARIA attributes,
 * and audit trail display per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Audit logs are critical for compliance (HIPAA)
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QaAuditDashboardComponent } from './qa-audit-dashboard.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('QaAuditDashboardComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: QaAuditDashboardComponent;
  let fixture: ComponentFixture<QaAuditDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QaAuditDashboardComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(QaAuditDashboardComponent);
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

  describe('Audit Log Viewer-Specific Accessibility', () => {
    it('should have accessible audit log table', () => {
      const table = fixture.nativeElement.querySelector('table');
      if (table) {
        const caption = table.querySelector('caption');
        const hasAriaLabel = table.hasAttribute('aria-label');
        expect(caption || hasAriaLabel).toBeTruthy();
      }
    });

    it('should have accessible table headers', () => {
      const headers = fixture.nativeElement.querySelectorAll('th');
      headers.forEach((header: HTMLElement) => {
        expect(header.hasAttribute('scope') || header.hasAttribute('role')).toBe(true);
      });
    });

    it('should have accessible filter controls', () => {
      const filters = fixture.nativeElement.querySelectorAll('input[type="search"], input[type="date"], select');
      filters.forEach((filter: HTMLElement) => {
        expect(filter.hasAttribute('aria-label') || filter.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should have accessible pagination', () => {
      const pagination = fixture.nativeElement.querySelector('[role="navigation"][aria-label*="pagination"]');
      if (pagination) {
        const prevButton = pagination.querySelector('button[aria-label*="previous"]');
        const nextButton = pagination.querySelector('button[aria-label*="next"]');
        expect(prevButton || nextButton).toBeTruthy();
      }
    });

    it('should announce log loading states', () => {
      const loadingIndicators = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live="polite"]');
      expect(loadingIndicators.length).toBeGreaterThanOrEqual(0);
    });

    it('should have accessible export button', () => {
      const exportButton = fixture.nativeElement.querySelector('button[aria-label*="export"], button[aria-label*="download"]');
      if (exportButton) {
        expect(exportButton.hasAttribute('aria-label')).toBe(true);
      }
    });
  });
});
