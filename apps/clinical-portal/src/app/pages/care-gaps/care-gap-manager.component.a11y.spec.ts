/**
 * Accessibility Tests for CareGapManagerComponent
 *
 * Tests WCAG 2.1 Level AA compliance for the care gap management interface.
 * Validates table accessibility, action button labels, and keyboard navigation.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { CareGapManagerComponent } from './care-gap-manager.component';
import {
  testAccessibility,
  testKeyboardAccessibility,
  testAriaAttributes,
  testAccessibilityForElement,
} from '../../../testing/accessibility.helper';

describe('CareGapManagerComponent - Accessibility', () => {
  let component: CareGapManagerComponent;
  let fixture: ComponentFixture<CareGapManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CareGapManagerComponent,
        HttpClientTestingModule,
        BrowserAnimationsModule,
        MatTableModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatChipsModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatSelectModule,
        MatInputModule,
        FormsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CareGapManagerComponent);
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
      expect(heading.textContent).toContain('Care Gap Manager');
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

  describe('Table Accessibility (WCAG 2.1 4.1.2)', () => {
    it('should have accessible table structure', async () => {
      // Wait for data to load
      await fixture.whenStable();
      fixture.detectChanges();

      const table = fixture.nativeElement.querySelector('table');
      if (table) {
        const results = await testAccessibilityForElement(fixture, 'table');
        expect(results).toHaveNoViolations();
      }
    });

    it('should have proper table headers', () => {
      const table = fixture.nativeElement.querySelector('table');
      if (table) {
        const headers = table.querySelectorAll('th');
        expect(headers.length).toBeGreaterThan(0);

        headers.forEach((header: HTMLElement) => {
          expect(header.textContent?.trim()).not.toBe('');
        });
      }
    });
  });

  describe('Action Button Accessibility (WCAG 2.1 4.1.2)', () => {
    it('should have descriptive ARIA labels on primary action buttons', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const primaryButtons = fixture.nativeElement.querySelectorAll('.primary-action');

      primaryButtons.forEach((button: HTMLElement) => {
        const ariaLabel = button.getAttribute('aria-label');
        expect(ariaLabel).toBeTruthy();
        expect(ariaLabel).toContain('for');
        expect(ariaLabel?.length).toBeGreaterThan(10); // Should be descriptive
      });
    });

    it('should have descriptive ARIA labels on close gap buttons', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const closeButtons = fixture.nativeElement.querySelectorAll('.close-gap-btn');

      closeButtons.forEach((button: HTMLElement) => {
        const ariaLabel = button.getAttribute('aria-label');
        expect(ariaLabel).toBeTruthy();
        expect(ariaLabel).toContain('Close care gap for');
        expect(ariaLabel?.length).toBeGreaterThan(15); // Should include patient and measure context
      });
    });

    it('should mark icons as decorative with aria-hidden', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const buttonIcons = fixture.nativeElement.querySelectorAll('button mat-icon[aria-hidden="true"]');
      expect(buttonIcons.length).toBeGreaterThan(0);
    });

    it('should have tooltips for additional context', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const buttonsWithTooltips = fixture.nativeElement.querySelectorAll('button[matTooltip]');
      expect(buttonsWithTooltips.length).toBeGreaterThan(0);

      buttonsWithTooltips.forEach((button: HTMLElement) => {
        const tooltip = button.getAttribute('matTooltip');
        expect(tooltip).toBeTruthy();
      });
    });
  });

  describe('Filter Controls Accessibility', () => {
    it('should have accessible form fields', async () => {
      const filterCard = fixture.nativeElement.querySelector('.filters-card');
      if (filterCard) {
        const results = await testAccessibilityForElement(fixture, '.filters-card');
        expect(results).toHaveNoViolations();
      }
    });

    it('should have labels for all form fields', () => {
      const formFields = fixture.nativeElement.querySelectorAll('mat-form-field');

      formFields.forEach((field: HTMLElement) => {
        const label = field.querySelector('mat-label');
        expect(label).toBeTruthy();
        expect(label?.textContent?.trim()).not.toBe('');
      });
    });

    it('should have accessible search input', () => {
      const searchInput = fixture.nativeElement.querySelector('input[placeholder*="Search"]');
      if (searchInput) {
        const formField = searchInput.closest('mat-form-field');
        const label = formField?.querySelector('mat-label');
        expect(label).toBeTruthy();
      }
    });
  });

  describe('Status Chips Accessibility', () => {
    it('should have semantic color coding', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const statusChips = fixture.nativeElement.querySelectorAll('mat-chip');

      statusChips.forEach((chip: HTMLElement) => {
        const text = chip.textContent?.trim();
        expect(text).toBeTruthy();
        expect(text?.length).toBeGreaterThan(0);
      });
    });
  });

  describe('Keyboard Navigation', () => {
    it('should have focusable interactive elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), a[href]'
      );

      expect(focusableElements.length).toBeGreaterThan(0);

      focusableElements.forEach((element: HTMLElement) => {
        expect(element.tabIndex).toBeGreaterThanOrEqual(-1);
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

  describe('Focus Indicators', () => {
    it('should have visible focus on action buttons', () => {
      const buttons = fixture.nativeElement.querySelectorAll('button');

      buttons.forEach((button: HTMLElement) => {
        button.focus();
        expect(button).toBe(document.activeElement);
      });
    });
  });

  describe('Empty State Accessibility', () => {
    it('should have accessible empty state message', () => {
      // Set component to have no care gaps
      component.careGaps = [];
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.no-results, .empty-state');
      if (emptyState) {
        const message = emptyState.textContent?.trim();
        expect(message).toBeTruthy();
        expect(message?.length).toBeGreaterThan(10);
      }
    });
  });
});
