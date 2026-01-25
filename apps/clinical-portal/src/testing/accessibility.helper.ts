/**
 * Accessibility Testing Helper
 *
 * Provides utilities for automated accessibility testing using axe-core.
 * Integrates with Angular TestBed for component-level accessibility validation.
 *
 * @see https://www.deque.com/axe/
 * @see https://github.com/nickcolley/jest-axe
 */

import { ComponentFixture } from '@angular/core/testing';
import { configureAxe, toHaveNoViolations } from 'jest-axe';

// Extend Jest matchers with axe-core accessibility matchers
expect.extend(toHaveNoViolations);

/**
 * Default axe configuration for WCAG 2.1 Level AA compliance testing
 */
const defaultAxeConfig = {
  rules: {
    // Enable all WCAG 2.1 Level A and AA rules
    'color-contrast': { enabled: true },
    'valid-aria-attr': { enabled: true },
    'aria-roles': { enabled: true },
    'button-name': { enabled: true },
    'image-alt': { enabled: true },
    'label': { enabled: true },
    'link-name': { enabled: true },
    'list': { enabled: true },
    'listitem': { enabled: true },
    'region': { enabled: true },
    'bypass': { enabled: true }, // Skip navigation links
    'focus-order-semantics': { enabled: true },
    'landmark-one-main': { enabled: true },
    'page-has-heading-one': { enabled: true },
  },
};

/**
 * Configure axe-core with custom rules for HDIM Clinical Portal
 */
export const axe = configureAxe({
  ...defaultAxeConfig,
  // Custom configuration for HDIM
  tags: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
});

/**
 * Run accessibility tests on an Angular component fixture
 *
 * @param fixture - Angular ComponentFixture to test
 * @param config - Optional axe configuration overrides
 * @returns Promise resolving to axe test results
 *
 * @example
 * ```typescript
 * it('should have no accessibility violations', async () => {
 *   const fixture = TestBed.createComponent(MyComponent);
 *   fixture.detectChanges();
 *
 *   const results = await testAccessibility(fixture);
 *   expect(results).toHaveNoViolations();
 * });
 * ```
 */
export async function testAccessibility<T>(
  fixture: ComponentFixture<T>,
  config?: any
): Promise<any> {
  const element = fixture.nativeElement;
  const axeInstance = config ? configureAxe(config) : axe;

  return axeInstance(element);
}

/**
 * Run accessibility tests on a specific element within a component
 *
 * @param fixture - Angular ComponentFixture containing the element
 * @param selector - CSS selector for the element to test
 * @param config - Optional axe configuration overrides
 * @returns Promise resolving to axe test results
 *
 * @example
 * ```typescript
 * it('should have accessible table', async () => {
 *   const fixture = TestBed.createComponent(MyComponent);
 *   fixture.detectChanges();
 *
 *   const results = await testAccessibilityForElement(fixture, '.mat-table');
 *   expect(results).toHaveNoViolations();
 * });
 * ```
 */
export async function testAccessibilityForElement<T>(
  fixture: ComponentFixture<T>,
  selector: string,
  config?: any
): Promise<any> {
  const element = fixture.nativeElement.querySelector(selector);

  if (!element) {
    throw new Error(`Element not found for selector: ${selector}`);
  }

  const axeInstance = config ? configureAxe(config) : axe;
  return axeInstance(element);
}

/**
 * Test keyboard navigation accessibility
 *
 * Verifies that all interactive elements are keyboard accessible
 * and have proper focus indicators.
 *
 * @param fixture - Angular ComponentFixture to test
 * @returns Promise resolving to test results
 *
 * @example
 * ```typescript
 * it('should support keyboard navigation', async () => {
 *   const fixture = TestBed.createComponent(MyComponent);
 *   fixture.detectChanges();
 *
 *   const results = await testKeyboardAccessibility(fixture);
 *   expect(results).toHaveNoViolations();
 * });
 * ```
 */
export async function testKeyboardAccessibility<T>(
  fixture: ComponentFixture<T>
): Promise<any> {
  const keyboardConfig = {
    rules: {
      'focus-order-semantics': { enabled: true },
      'tabindex': { enabled: true },
      region: { enabled: false }, // Disable for component-level testing
    },
  };

  return testAccessibility(fixture, keyboardConfig);
}

/**
 * Test ARIA attributes and semantic HTML
 *
 * Verifies proper use of ARIA attributes and semantic HTML elements.
 *
 * @param fixture - Angular ComponentFixture to test
 * @returns Promise resolving to test results
 *
 * @example
 * ```typescript
 * it('should have valid ARIA attributes', async () => {
 *   const fixture = TestBed.createComponent(MyComponent);
 *   fixture.detectChanges();
 *
 *   const results = await testAriaAttributes(fixture);
 *   expect(results).toHaveNoViolations();
 * });
 * ```
 */
export async function testAriaAttributes<T>(
  fixture: ComponentFixture<T>
): Promise<any> {
  const ariaConfig = {
    rules: {
      'aria-allowed-attr': { enabled: true },
      'aria-required-attr': { enabled: true },
      'aria-required-children': { enabled: true },
      'aria-required-parent': { enabled: true },
      'aria-roles': { enabled: true },
      'aria-valid-attr': { enabled: true },
      'aria-valid-attr-value': { enabled: true },
    },
  };

  return testAccessibility(fixture, ariaConfig);
}

/**
 * Test color contrast ratios
 *
 * Verifies that all text meets WCAG 2.1 Level AA color contrast requirements:
 * - Normal text: 4.5:1
 * - Large text (18pt+): 3:1
 *
 * @param fixture - Angular ComponentFixture to test
 * @returns Promise resolving to test results
 *
 * @example
 * ```typescript
 * it('should meet color contrast requirements', async () => {
 *   const fixture = TestBed.createComponent(MyComponent);
 *   fixture.detectChanges();
 *
 *   const results = await testColorContrast(fixture);
 *   expect(results).toHaveNoViolations();
 * });
 * ```
 */
export async function testColorContrast<T>(
  fixture: ComponentFixture<T>
): Promise<any> {
  const contrastConfig = {
    rules: {
      'color-contrast': { enabled: true },
      'color-contrast-enhanced': { enabled: true }, // AAA level (optional)
    },
  };

  return testAccessibility(fixture, contrastConfig);
}

/**
 * Custom Jest matcher type declarations
 */
declare global {
  namespace jest {
    interface Matchers<R> {
      toHaveNoViolations(): R;
    }
  }
}
