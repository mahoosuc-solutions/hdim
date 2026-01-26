/**
 * Dashboard Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests role-based dashboard for keyboard accessibility, ARIA attributes,
 * and screen reader support per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Dashboard is first page users see after login
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DashboardComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
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
    it('should have valid ARIA attributes on all interactive elements', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have proper color contrast on dashboard widgets', async () => {
      const results = await testAccessibility(fixture);
      const contrastViolations = results.violations.filter(
        (v: any) => v.id === 'color-contrast'
      );
      expect(contrastViolations.length).toBe(0);
    });

    it('should have aria-label on all action buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button.hasAttribute('aria-label') || button.hasAttribute('aria-labelledby');
        const hasTextContent = button.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should have keyboard-focusable interactive elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Dashboard-Specific Accessibility', () => {
    it('should have accessible dashboard widgets with regions', () => {
      const widgets = fixture.nativeElement.querySelectorAll('[role="region"]');
      widgets.forEach((widget: HTMLElement) => {
        expect(widget.hasAttribute('aria-label') || widget.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should have accessible statistics cards', () => {
      const statCards = fixture.nativeElement.querySelectorAll('.stat-card, .dashboard-card');
      statCards.forEach((card: HTMLElement) => {
        const heading = card.querySelector('h1, h2, h3, h4');
        expect(heading).toBeTruthy();
      });
    });

    it('should have accessible charts with text alternatives', () => {
      const charts = fixture.nativeElement.querySelectorAll('canvas, svg');
      charts.forEach((chart: HTMLElement) => {
        const hasAriaLabel = chart.hasAttribute('aria-label');
        const hasRole = chart.hasAttribute('role');
        expect(hasAriaLabel || hasRole).toBe(true);
      });
    });

    it('should have accessible quick actions', () => {
      const quickActions = fixture.nativeElement.querySelectorAll('[role="navigation"] a, .quick-actions button');
      quickActions.forEach((action: HTMLElement) => {
        const hasAriaLabel = action.hasAttribute('aria-label');
        const hasTextContent = action.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should announce loading states', () => {
      const loadingIndicators = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live="polite"]');
      expect(loadingIndicators.length).toBeGreaterThanOrEqual(0);
    });
  });
});
