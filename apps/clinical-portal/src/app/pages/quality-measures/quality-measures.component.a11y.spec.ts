/**
 * Accessibility Tests for QualityMeasuresComponent
 *
 * Tests WCAG 2.1 Level AA compliance for the quality measures interface.
 * Validates card accessibility, modal dialogs, and interactive controls.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { QualityMeasuresComponent } from './quality-measures.component';
import {
  testAccessibility,
  testKeyboardAccessibility,
  testAriaAttributes,
  testAccessibilityForElement,
} from '../../../testing/accessibility.helper';

describe('QualityMeasuresComponent - Accessibility', () => {
  let component: QualityMeasuresComponent;
  let fixture: ComponentFixture<QualityMeasuresComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        QualityMeasuresComponent,
        HttpClientTestingModule,
        BrowserAnimationsModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatChipsModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatProgressBarModule,
        MatDividerModule,
        FormsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityMeasuresComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      const results = await testAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have accessible page heading', () => {
      const heading = fixture.nativeElement.querySelector('h1');
      expect(heading).toBeTruthy();
      expect(heading.textContent).toContain('Quality Measures');
    });

    it('should have descriptive subtitle', () => {
      const subtitle = fixture.nativeElement.querySelector('.subtitle');
      expect(subtitle).toBeTruthy();
      expect(subtitle.textContent?.trim()).not.toBe('');
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have valid ARIA attributes', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Measure Cards Accessibility', () => {
    it('should have accessible measure cards', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const measureCards = fixture.nativeElement.querySelector('.measures-grid');
      if (measureCards) {
        const results = await testAccessibilityForElement(fixture, '.measures-grid');
        expect(results).toHaveNoViolations();
      }
    });

    it('should have clickable cards with keyboard support', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const measureCards = fixture.nativeElement.querySelectorAll('.measure-card');

      measureCards.forEach((card: HTMLElement) => {
        // Cards should be keyboard accessible (clickable via Enter/Space)
        const tabindex = card.getAttribute('tabindex');
        const hasClickHandler = card.onclick || card.getAttribute('(click)');

        if (hasClickHandler) {
          expect(tabindex).not.toBeNull();
          expect(parseInt(tabindex || '0')).toBeGreaterThanOrEqual(0);
        }
      });
    });

    it('should have accessible measure names', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const measureNames = fixture.nativeElement.querySelectorAll('.measure-name');

      measureNames.forEach((name: HTMLElement) => {
        expect(name.textContent?.trim()).not.toBe('');
        expect(name.textContent?.length).toBeGreaterThan(5);
      });
    });

    it('should have accessible status chips', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const statusChips = fixture.nativeElement.querySelectorAll('mat-chip');

      statusChips.forEach((chip: HTMLElement) => {
        const text = chip.textContent?.trim();
        expect(text).toBeTruthy();
        // Status should have semantic class names for color coding
        expect(chip.className).toContain('mat-mdc-chip');
      });
    });
  });

  describe('Measure Detail Panel Accessibility', () => {
    it('should have accessible close button with ARIA label', async () => {
      // Simulate selecting a measure
      if (component.measures && component.measures().length > 0) {
        component.selectMeasure(component.measures()[0]);
        fixture.detectChanges();
        await fixture.whenStable();

        const closeButton = fixture.nativeElement.querySelector('button[aria-label="Close measure details"]');
        expect(closeButton).toBeTruthy();

        const icon = closeButton?.querySelector('mat-icon[aria-hidden="true"]');
        expect(icon).toBeTruthy();
      }
    });

    it('should have accessible action buttons', async () => {
      if (component.measures && component.measures().length > 0) {
        component.selectMeasure(component.measures()[0]);
        fixture.detectChanges();
        await fixture.whenStable();

        const actionButtons = fixture.nativeElement.querySelectorAll('.action-buttons button');

        actionButtons.forEach((button: HTMLElement) => {
          const text = button.textContent?.trim();
          const icon = button.querySelector('mat-icon');

          expect(text || icon).toBeTruthy();

          // If icon-only, should have accessible name
          if (!text && icon) {
            const ariaLabel = button.getAttribute('aria-label');
            const ariaLabelledBy = button.getAttribute('aria-labelledby');
            expect(ariaLabel || ariaLabelledBy).toBeTruthy();
          }
        });
      }
    });

    it('should have accessible progress indicators', async () => {
      if (component.measures && component.measures().length > 0) {
        component.selectMeasure(component.measures()[0]);
        component['isEvaluating'].set(true);
        fixture.detectChanges();
        await fixture.whenStable();

        const progressBar = fixture.nativeElement.querySelector('mat-progress-bar');
        if (progressBar) {
          const role = progressBar.getAttribute('role');
          expect(role).toBe('progressbar');

          const ariaLabel = progressBar.getAttribute('aria-label') || progressBar.getAttribute('aria-labelledby');
          // Progress bar should have accessible name or be within a labeled context
          const context = progressBar.closest('.evaluation-progress');
          expect(ariaLabel || context).toBeTruthy();
        }
      }
    });
  });

  describe('Filter Controls Accessibility', () => {
    it('should have accessible search field', async () => {
      const searchField = fixture.nativeElement.querySelector('.search-field');
      if (searchField) {
        const results = await testAccessibilityForElement(fixture, '.search-field');
        expect(results).toHaveNoViolations();
      }
    });

    it('should have labels for all filter fields', () => {
      const formFields = fixture.nativeElement.querySelectorAll('mat-form-field');

      formFields.forEach((field: HTMLElement) => {
        const label = field.querySelector('mat-label');
        expect(label).toBeTruthy();
        expect(label?.textContent?.trim()).not.toBe('');
      });
    });

    it('should have accessible select dropdowns', () => {
      const selects = fixture.nativeElement.querySelectorAll('mat-select');

      selects.forEach((select: HTMLElement) => {
        const formField = select.closest('mat-form-field');
        const label = formField?.querySelector('mat-label');
        expect(label).toBeTruthy();
      });
    });
  });

  describe('Star Rating Accessibility', () => {
    it('should have semantic star rating representation', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const starRatings = fixture.nativeElement.querySelectorAll('.star-rating');

      starRatings.forEach((rating: HTMLElement) => {
        const stars = rating.querySelectorAll('mat-icon');
        const label = rating.querySelector('.star-label');

        expect(stars.length).toBeGreaterThan(0);
        expect(label).toBeTruthy();
        expect(label?.textContent?.trim()).not.toBe('');
      });
    });
  });

  describe('Results Cards Accessibility', () => {
    it('should have accessible results cards', async () => {
      // Simulate evaluation complete
      if (component.measures && component.measures().length > 0) {
        component.selectMeasure(component.measures()[0]);
        // Set evaluation result (this would normally come from service)
        component['evaluationResult'].set({
          measureCode: 'BCS',
          denominator: 1000,
          numerator: 800,
          rate: 80,
          benchmark: 75,
          gapToBenchmark: 5,
          careGapsCount: 200,
          patientsEvaluated: 1000,
          evaluationTime: 45,
        });
        fixture.detectChanges();
        await fixture.whenStable();

        const resultsCards = fixture.nativeElement.querySelectorAll('.results-card');

        resultsCards.forEach((card: HTMLElement) => {
          const heading = card.querySelector('h5, h4, h3');
          expect(heading).toBeTruthy();
          expect(heading?.textContent?.trim()).not.toBe('');
        });
      }
    });

    it('should have semantic color coding for performance metrics', async () => {
      if (component['evaluationResult']()) {
        const positiveMetrics = fixture.nativeElement.querySelectorAll('.positive');
        const negativeMetrics = fixture.nativeElement.querySelectorAll('.negative');

        // Visual indicators should be supplemented with text
        positiveMetrics.forEach((metric: HTMLElement) => {
          const text = metric.textContent?.trim();
          expect(text).toBeTruthy();
        });

        negativeMetrics.forEach((metric: HTMLElement) => {
          const text = metric.textContent?.trim();
          expect(text).toBeTruthy();
        });
      }
    });
  });

  describe('Keyboard Navigation', () => {
    it('should have focusable interactive elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll(
        'button:not([disabled]), input:not([disabled]), mat-select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );

      expect(focusableElements.length).toBeGreaterThan(0);

      focusableElements.forEach((element: HTMLElement) => {
        const tabindex = element.getAttribute('tabindex');
        if (tabindex) {
          expect(parseInt(tabindex)).toBeGreaterThanOrEqual(-1);
        }
      });
    });

    it('should not have positive tabindex values', () => {
      const positiveTabindexElements = fixture.nativeElement.querySelectorAll(
        '[tabindex]:not([tabindex="0"]):not([tabindex="-1"])'
      );

      positiveTabindexElements.forEach((element: HTMLElement) => {
        const tabindex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabindex).toBeLessThanOrEqual(0);
      });
    });
  });

  describe('Empty State Accessibility', () => {
    it('should have accessible empty state message', () => {
      component['filteredMeasures'] = () => [];
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.no-results');
      if (emptyState) {
        const icon = emptyState.querySelector('mat-icon');
        const message = emptyState.querySelector('p');

        expect(icon).toBeTruthy();
        expect(message).toBeTruthy();
        expect(message?.textContent?.trim()).not.toBe('');
      }
    });
  });
});
