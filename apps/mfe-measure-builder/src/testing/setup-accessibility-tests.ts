/**
 * Accessibility Testing Setup
 *
 * Configures Jest environment for axe-core accessibility testing.
 * This file should be imported in jest.config.ts setupFilesAfterEnv.
 */

import { toHaveNoViolations } from 'jest-axe';

// Extend Jest matchers with axe-core accessibility matchers
expect.extend(toHaveNoViolations);

/**
 * Configure axe-core for HDIM Clinical Portal
 *
 * This sets global configuration for all accessibility tests.
 */
beforeAll(() => {
  // Suppress axe-core console output during tests
  if (typeof window !== 'undefined') {
    window.axe = window.axe || {};
  }
});

/**
 * Global test timeout for accessibility tests
 *
 * Accessibility tests may take longer due to DOM analysis.
 * Set to 30000ms to allow axe-core to complete full page analysis.
 */
jest.setTimeout(30000);

/**
 * Mock window.matchMedia for responsive design tests
 *
 * Required for Material Design components that use media queries.
 */
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // Deprecated
    removeListener: jest.fn(), // Deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

/**
 * Mock IntersectionObserver for lazy loading components
 */
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  takeRecords() {
    return [];
  }
  unobserve() {}
} as any;

/**
 * Mock ResizeObserver for responsive components
 */
global.ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
} as any;

/**
 * Suppress console warnings during tests
 *
 * Material Design may emit warnings that are not relevant to accessibility testing.
 */
const originalWarn = console.warn;
const originalError = console.error;

beforeAll(() => {
  console.warn = (...args: any[]) => {
    // Suppress specific warnings
    const message = args[0]?.toString() || '';
    if (
      message.includes('Material') ||
      message.includes('animation') ||
      message.includes('zone.js')
    ) {
      return;
    }
    originalWarn.apply(console, args);
  };

  console.error = (...args: any[]) => {
    // Suppress specific errors
    const message = args[0]?.toString() || '';
    if (
      message.includes('Not implemented: HTMLFormElement.prototype.submit') ||
      message.includes('Not implemented: navigation')
    ) {
      return;
    }
    originalError.apply(console, args);
  };
});

afterAll(() => {
  console.warn = originalWarn;
  console.error = originalError;
});

/**
 * TypeScript declarations for axe global
 */
declare global {
  interface Window {
    axe: any;
  }
}
