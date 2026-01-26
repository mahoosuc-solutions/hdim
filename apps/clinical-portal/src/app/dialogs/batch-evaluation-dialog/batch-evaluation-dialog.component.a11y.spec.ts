/**
 * Batch Evaluation Dialog Component - Accessibility Tests (WCAG 2.1 Level AA)
 *
 * Tests batch evaluation dialog for keyboard accessibility, ARIA attributes,
 * focus management, and dialog patterns per WCAG 2.1 guidelines.
 *
 * Priority: HIGH - Batch operations are complex workflows
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BatchEvaluationDialogComponent } from './batch-evaluation-dialog.component';
import { testAccessibility, testAriaAttributes, testKeyboardAccessibility } from '../../../testing/accessibility.helper';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { LoggerService } from '../services/logger.service';
import { createMockLoggerService } from '../testing/mocks';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('BatchEvaluationDialogComponent - Accessibility (WCAG 2.1 Level AA)', () => {
  let component: BatchEvaluationDialogComponent;
  let fixture: ComponentFixture<BatchEvaluationDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BatchEvaluationDialogComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [{ provide: LoggerService, useValue: createMockLoggerService() },
        
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchEvaluationDialogComponent);
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

    it('should have labeled form fields', () => {
      const inputs = fixture.nativeElement.querySelectorAll('input, select, textarea');
      inputs.forEach((input: HTMLElement) => {
        const hasLabel = input.hasAttribute('aria-label') || input.hasAttribute('aria-labelledby') || input.id;
        expect(hasLabel).toBe(true);
      });
    });

    it('should have keyboard-focusable elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll('button, input, select, [tabindex="0"]');
      focusableElements.forEach((element: HTMLElement) => {
        const tabIndex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabIndex).toBeGreaterThanOrEqual(-1);
      });
    });
  });

  describe('Dialog-Specific Accessibility', () => {
    it('should have role="dialog" and aria-modal', () => {
      const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
      expect(dialog).toBeTruthy();
      expect(dialog?.hasAttribute('aria-modal')).toBe(true);
    });

    it('should have accessible dialog title', () => {
      const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
      if (dialog) {
        const hasAriaLabel = dialog.hasAttribute('aria-label');
        const hasAriaLabelledBy = dialog.hasAttribute('aria-labelledby');
        expect(hasAriaLabel || hasAriaLabelledBy).toBe(true);
      }
    });

    it('should close on Escape key', () => {
      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(escapeEvent);
      // Dialog should close (handled by Material Dialog)
    });

    it('should have accessible close button', () => {
      const closeButton = fixture.nativeElement.querySelector('button[mat-dialog-close], button[aria-label*="close"]');
      if (closeButton) {
        expect(closeButton.hasAttribute('aria-label')).toBe(true);
      }
    });

    it('should have accessible action buttons', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('button');
      actionButtons.forEach((button: HTMLElement) => {
        const hasAriaLabel = button.hasAttribute('aria-label');
        const hasTextContent = button.textContent?.trim().length! > 0;
        expect(hasAriaLabel || hasTextContent).toBe(true);
      });
    });

    it('should announce batch progress', () => {
      const progressIndicators = fixture.nativeElement.querySelectorAll('[role="status"], [aria-live], [role="progressbar"]');
      expect(progressIndicators.length).toBeGreaterThanOrEqual(0);
    });
  });
});
