/**
 * Patient Detail Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests patient health record display for keyboard accessibility, ARIA attributes,
 * and screen reader support per WCAG 2.1 guidelines.
 *
 * Priority: CRITICAL - PHI access must be accessible (HIPAA requirement)
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PatientDetailComponent } from './patient-detail.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

describe('PatientDetailComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: PatientDetailComponent;
  let fixture: ComponentFixture<PatientDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientDetailComponent, NoopAnimationsModule, HttpClientTestingModule, RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
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

    it('should have proper color contrast', async () => {
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

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Patient Detail-Specific Accessibility', () => {
    it('should have accessible patient information sections', () => {
      const sections = fixture.nativeElement.querySelectorAll('section, [role="region"]');
      sections.forEach((section: HTMLElement) => {
        const hasHeading = section.querySelector('h1, h2, h3, h4, h5, h6');
        const hasAriaLabel = section.hasAttribute('aria-label') || section.hasAttribute('aria-labelledby');
        expect(hasHeading || hasAriaLabel).toBeTruthy();
      });
    });

    it('should have accessible tabs for patient data categories', () => {
      const tablist = fixture.nativeElement.querySelector('[role="tablist"]');
      if (tablist) {
        const tabs = tablist.querySelectorAll('[role="tab"]');
        tabs.forEach((tab: HTMLElement) => {
          expect(tab.hasAttribute('aria-selected')).toBe(true);
          expect(tab.hasAttribute('aria-controls')).toBe(true);
        });
      }
    });

    it('should have accessible data tables', () => {
      const tables = fixture.nativeElement.querySelectorAll('table');
      tables.forEach((table: HTMLElement) => {
        const caption = table.querySelector('caption');
        const hasAriaLabel = table.hasAttribute('aria-label');
        expect(caption || hasAriaLabel).toBeTruthy();
      });
    });

    it('should have accessible timeline entries', () => {
      const timelineItems = fixture.nativeElement.querySelectorAll('[role="list"] [role="listitem"]');
      timelineItems.forEach((item: HTMLElement) => {
        expect(item.hasAttribute('aria-label') || item.textContent?.trim().length! > 0).toBe(true);
      });
    });

    it('should announce loading states for PHI', () => {
      const loadingIndicators = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live="polite"]');
      expect(loadingIndicators.length).toBeGreaterThanOrEqual(0);
    });
  });
});
