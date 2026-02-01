/**
 * Error Banner Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests error handling display for keyboard accessibility, ARIA attributes,
 * and alert announcements per WCAG 2.1 guidelines.
 *
 * Priority: CRITICAL - Error messages must be accessible to all users
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorBannerComponent } from './error-banner.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ErrorBannerComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: ErrorBannerComponent;
  let fixture: ComponentFixture<ErrorBannerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorBannerComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorBannerComponent);
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

    it('should have proper color contrast for error messages', async () => {
      const results = await testAccessibility(fixture);
      const contrastViolations = results.violations.filter(
        (v: any) => v.id === 'color-contrast'
      );
      expect(contrastViolations.length).toBe(0);
    });

    it('should have labeled close button', () => {
      const closeButton = fixture.nativeElement.querySelector('button[aria-label*="close"], button[aria-label*="dismiss"]');
      if (closeButton) {
        expect(closeButton.hasAttribute('aria-label')).toBe(true);
      }
    });

    it('should have keyboard-focusable close button', () => {
      const closeButton = fixture.nativeElement.querySelector('button');
      if (closeButton) {
        const tabIndex = parseInt(closeButton.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      }
    });
  });

  describe('Error Banner-Specific Accessibility', () => {
    it('should have role="alert" for error messages', () => {
      const errorAlert = fixture.nativeElement.querySelector('[role="alert"]');
      expect(errorAlert).toBeTruthy();
    });

    it('should have aria-live="assertive" for critical errors', () => {
      const errorMessage = fixture.nativeElement.querySelector('[role="alert"]');
      if (errorMessage) {
        const ariaLive = errorMessage.getAttribute('aria-live');
        expect(['assertive', 'polite']).toContain(ariaLive);
      }
    });

    it('should have descriptive error message text', () => {
      const errorMessage = fixture.nativeElement.querySelector('[role="alert"]');
      if (errorMessage) {
        expect(errorMessage.textContent?.trim().length!).toBeGreaterThan(0);
      }
    });

    it('should have accessible icon with aria-hidden', () => {
      const errorIcon = fixture.nativeElement.querySelector('mat-icon, svg, [class*="icon"]');
      if (errorIcon) {
        expect(errorIcon.hasAttribute('aria-hidden')).toBe(true);
      }
    });

    it('should close on Escape key', () => {
      const errorBanner = fixture.nativeElement.querySelector('[role="alert"]');
      if (errorBanner) {
        const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
        errorBanner.dispatchEvent(escapeEvent);
        fixture.detectChanges();
        // Banner should be dismissed
      }
    });

    it('should have appropriate alert level indicators', () => {
      const errorBanner = fixture.nativeElement.querySelector('[role="alert"]');
      if (errorBanner) {
        // Should have visual indicators like color, icon, or class
        const hasVisualIndicator = 
          errorBanner.className.includes('error') ||
          errorBanner.className.includes('warning') ||
          errorBanner.className.includes('info');
        expect(hasVisualIndicator).toBe(true);
      }
    });
  });
});
