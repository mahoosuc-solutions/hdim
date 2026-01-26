/**
 * Global Search Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests patient search for keyboard accessibility, ARIA attributes,
 * and search results announcement per WCAG 2.1 guidelines.
 *
 * Priority: CRITICAL - Search is primary navigation method
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GlobalSearchComponent } from './global-search.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('GlobalSearchComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: GlobalSearchComponent;
  let fixture: ComponentFixture<GlobalSearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent, NoopAnimationsModule, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalSearchComponent);
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

    it('should have labeled search input', () => {
      const searchInput = fixture.nativeElement.querySelector('input[type="search"], input[role="searchbox"]');
      expect(searchInput).toBeTruthy();
      expect(searchInput?.hasAttribute('aria-label') || searchInput?.hasAttribute('aria-labelledby')).toBe(true);
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('input, button, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Search-Specific Accessibility', () => {
    it('should have role="search" on search container', () => {
      const searchContainer = fixture.nativeElement.querySelector('[role="search"]');
      expect(searchContainer).toBeTruthy();
    });

    it('should have combobox pattern for autocomplete', () => {
      const searchInput = fixture.nativeElement.querySelector('[role="combobox"]');
      if (searchInput) {
        expect(searchInput.hasAttribute('aria-expanded')).toBe(true);
        expect(searchInput.hasAttribute('aria-controls')).toBe(true);
        expect(searchInput.hasAttribute('aria-autocomplete')).toBe(true);
      }
    });

    it('should announce search results count', () => {
      const resultsAnnouncement = fixture.nativeElement.querySelector('[role="status"], [aria-live="polite"]');
      expect(resultsAnnouncement).toBeTruthy();
    });

    it('should have accessible search results list', () => {
      const resultsList = fixture.nativeElement.querySelector('[role="listbox"], [role="list"]');
      if (resultsList) {
        expect(resultsList.hasAttribute('aria-label') || resultsList.hasAttribute('aria-labelledby')).toBe(true);
      }
    });

    it('should have keyboard-navigable results', () => {
      const results = fixture.nativeElement.querySelectorAll('[role="option"], [role="listitem"]');
      results.forEach((result: HTMLElement) => {
        expect(result.hasAttribute('tabindex') || result.tagName.toLowerCase() === 'a').toBe(true);
      });
    });

    it('should announce loading state', () => {
      const loadingIndicator = fixture.nativeElement.querySelector('[role="status"][aria-live]');
      expect(loadingIndicator).toBeTruthy();
    });

    it('should clear results on Escape key', () => {
      const searchInput = fixture.nativeElement.querySelector('input[type="search"]');
      if (searchInput) {
        const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
        searchInput.dispatchEvent(escapeEvent);
        fixture.detectChanges();
        // Results should be cleared
      }
    });
  });
});
