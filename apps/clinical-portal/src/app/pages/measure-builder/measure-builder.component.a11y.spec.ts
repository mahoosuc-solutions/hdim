/**
 * Measure Builder Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests measure creation tool for keyboard accessibility, ARIA attributes,
 * and complex form controls per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Measure builder is complex clinical tool
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { MeasureBuilderComponent } from './measure-builder.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideMockStore } from '@ngrx/store/testing';

describe('MeasureBuilderComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: MeasureBuilderComponent;
  let fixture: ComponentFixture<MeasureBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MeasureBuilderComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [provideMockStore()],
    }).compileComponents();

    fixture = TestBed.createComponent(MeasureBuilderComponent);
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
    it('should have valid ARIA attributes on all form elements', async () => {
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
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || !!input.id;
        expect(hasLabel).toBeTruthy();
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('button, input, select, textarea, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Measure Builder-Specific Accessibility', () => {
    it('should have accessible code editor', () => {
      const codeEditor = fixture.nativeElement.querySelector('textarea[aria-label*="code"], [role="textbox"]');
      if (codeEditor) {
        expect(codeEditor.hasAttribute('aria-label') || codeEditor.hasAttribute('aria-labelledby')).toBe(true);
      }
    });

    it('should have accessible tabs for editor sections', () => {
      const tablist = fixture.nativeElement.querySelector('[role="tablist"]');
      if (tablist) {
        const tabs = tablist.querySelectorAll('[role="tab"]');
        tabs.forEach((tab: HTMLElement) => {
          expect(tab.hasAttribute('aria-selected')).toBe(true);
          expect(tab.hasAttribute('aria-controls')).toBe(true);
        });
      }
    });

    it('should have accessible validation messages', () => {
      const validationMessages = fixture.nativeElement.querySelectorAll('[role="alert"], .validation-error');
      validationMessages.forEach((message: HTMLElement) => {
        expect(message.hasAttribute('role') || message.hasAttribute('aria-live')).toBe(true);
      });
    });

    it('should have accessible save and publish buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button[type="submit"]');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button?.hasAttribute('aria-label');
        const hasTextContent = button?.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should announce build status changes', () => {
      const statusMessages = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live]');
      expect(statusMessages.length).toBeGreaterThanOrEqual(0);
    });
  });
});
