/**
 * Patients Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests patient list view for keyboard accessibility, ARIA attributes,
 * and screen reader support per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Patient search and display is primary user workflow
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PatientsComponent } from './patients.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('PatientsComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: PatientsComponent;
  let fixture: ComponentFixture<PatientsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientsComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientsComponent);
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

    it('should have aria-label on action buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button.hasAttribute('aria-label') || button.hasAttribute('aria-labelledby');
        const hasTextContent = button.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('a, button, input, select, textarea, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Patient List-Specific Accessibility', () => {
    it('should have accessible search input', () => {
      const searchInput = fixture.nativeElement.querySelector('input[type="search"], input[placeholder*="search"]');
      if (searchInput) {
        expect(searchInput.hasAttribute('aria-label') || searchInput.hasAttribute('id')).toBe(true);
      }
    });

    it('should have accessible table headers', () => {
      const tableHeaders = fixture.nativeElement.querySelectorAll('th');
      tableHeaders.forEach((header: HTMLElement) => {
        expect(header.hasAttribute('scope') || header.hasAttribute('role')).toBe(true);
      });
    });

    it('should have row selection indicators', () => {
      const checkboxes = fixture.nativeElement.querySelectorAll('input[type="checkbox"]');
      checkboxes.forEach((checkbox: HTMLElement) => {
        expect(checkbox.hasAttribute('aria-label') || checkbox.hasAttribute('aria-labelledby')).toBe(true);
      });
    });

    it('should announce table sort changes', () => {
      const sortableHeaders = fixture.nativeElement.querySelectorAll('[aria-sort]');
      sortableHeaders.forEach((header: HTMLElement) => {
        const ariaSort = header.getAttribute('aria-sort');
        expect(['none', 'ascending', 'descending']).toContain(ariaSort);
      });
    });

    it('should have accessible pagination controls', () => {
      const pagination = fixture.nativeElement.querySelector('[role="navigation"][aria-label*="pagination"]');
      if (pagination) {
        const prevButton = pagination.querySelector('button[aria-label*="previous"]');
        const nextButton = pagination.querySelector('button[aria-label*="next"]');
        expect(prevButton || nextButton).toBeTruthy();
      }
    });
  });
});
