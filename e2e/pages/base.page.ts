import { Page, Locator, expect } from '@playwright/test';
import { PHIMasking, defaultPHIMasking } from '../utils/phi-masking';

/**
 * Base Page Object
 *
 * All page objects extend this base class which provides common
 * functionality for navigation, waiting, and HIPAA-compliant operations.
 */

export abstract class BasePage {
  protected readonly page: Page;
  protected readonly phiMasking: PHIMasking;

  // Common locators
  protected readonly loadingSpinner: Locator;
  protected readonly errorMessage: Locator;
  protected readonly successMessage: Locator;
  protected readonly toastNotification: Locator;

  constructor(page: Page, phiMasking?: PHIMasking) {
    this.page = page;
    this.phiMasking = phiMasking || defaultPHIMasking;

    // Common elements
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"], .loading-spinner, mat-spinner');
    this.errorMessage = page.locator('[data-testid="error-message"], .error-message, .alert-danger');
    this.successMessage = page.locator('[data-testid="success-message"], .success-message, .alert-success');
    this.toastNotification = page.locator('[data-testid="toast"], .toast, .snackbar, mat-snack-bar-container');
  }

  /**
   * Navigate to page
   */
  abstract goto(): Promise<void>;

  /**
   * Check if page is loaded
   */
  abstract isLoaded(): Promise<boolean>;

  /**
   * Get page title
   */
  async getTitle(): Promise<string> {
    return this.page.title();
  }

  /**
   * Wait for page to be fully loaded
   */
  async waitForLoad(timeout?: number): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout: timeout || 30000 });
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Wait for loading spinner to disappear
   */
  async waitForSpinnerToDisappear(timeout?: number): Promise<void> {
    try {
      await this.loadingSpinner.waitFor({
        state: 'hidden',
        timeout: timeout || 30000,
      });
    } catch {
      // Spinner may not be present
    }
  }

  /**
   * Wait for success message
   */
  async waitForSuccessMessage(timeout?: number): Promise<string> {
    await this.successMessage.waitFor({ state: 'visible', timeout: timeout || 10000 });
    const text = await this.successMessage.textContent() || '';
    return this.phiMasking.mask(text).masked;
  }

  /**
   * Wait for error message
   */
  async waitForErrorMessage(timeout?: number): Promise<string> {
    await this.errorMessage.waitFor({ state: 'visible', timeout: timeout || 10000 });
    const text = await this.errorMessage.textContent() || '';
    return this.phiMasking.mask(text).masked;
  }

  /**
   * Get toast notification text
   */
  async getToastMessage(timeout?: number): Promise<string> {
    await this.toastNotification.waitFor({ state: 'visible', timeout: timeout || 10000 });
    const text = await this.toastNotification.textContent() || '';
    return this.phiMasking.mask(text).masked;
  }

  /**
   * Dismiss toast notification
   */
  async dismissToast(): Promise<void> {
    const closeButton = this.toastNotification.locator('button, [aria-label="Close"]');
    if (await closeButton.isVisible()) {
      await closeButton.click();
    }
  }

  /**
   * Check if error is displayed
   */
  async hasError(): Promise<boolean> {
    return this.errorMessage.isVisible();
  }

  /**
   * Check if success message is displayed
   */
  async hasSuccess(): Promise<boolean> {
    return this.successMessage.isVisible();
  }

  /**
   * Wait for navigation to complete
   */
  async waitForNavigation(url?: string | RegExp): Promise<void> {
    if (url) {
      await this.page.waitForURL(url);
    } else {
      await this.page.waitForLoadState('networkidle');
    }
  }

  /**
   * Take a screenshot (with PHI masking consideration)
   */
  async takeScreenshot(name: string): Promise<Buffer> {
    return this.page.screenshot({
      path: `test-results/screenshots/${name}.png`,
      fullPage: false,
    });
  }

  /**
   * Safely get text content (with PHI masking)
   */
  async safeText(locator: Locator): Promise<string> {
    const text = await locator.textContent() || '';
    return this.phiMasking.mask(text).masked;
  }

  /**
   * Fill form field with safe logging
   */
  async safeFill(locator: Locator, value: string): Promise<void> {
    const maskedValue = this.phiMasking.mask(value).masked;
    console.log(`Filling field with: ${maskedValue}`);
    await locator.fill(value);
  }

  /**
   * Click and wait for network idle
   */
  async clickAndWait(locator: Locator): Promise<void> {
    await locator.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Scroll element into view
   */
  async scrollIntoView(locator: Locator): Promise<void> {
    await locator.scrollIntoViewIfNeeded();
  }

  /**
   * Wait for element to be visible
   */
  async waitForVisible(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'visible', timeout: timeout || 10000 });
  }

  /**
   * Wait for element to be hidden
   */
  async waitForHidden(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'hidden', timeout: timeout || 10000 });
  }

  /**
   * Check if element exists in DOM
   */
  async exists(locator: Locator): Promise<boolean> {
    return (await locator.count()) > 0;
  }

  /**
   * Get current URL
   */
  getCurrentUrl(): string {
    return this.page.url();
  }

  /**
   * Press keyboard key
   */
  async pressKey(key: string): Promise<void> {
    await this.page.keyboard.press(key);
  }

  /**
   * Wait for specific text to appear
   */
  async waitForText(text: string, timeout?: number): Promise<void> {
    await this.page.waitForSelector(`text=${text}`, { timeout: timeout || 10000 });
  }

  /**
   * Assert page URL matches pattern
   */
  async assertUrl(pattern: string | RegExp): Promise<void> {
    await expect(this.page).toHaveURL(pattern);
  }

  /**
   * Assert element is visible
   */
  async assertVisible(locator: Locator): Promise<void> {
    await expect(locator).toBeVisible();
  }

  /**
   * Assert element contains text
   */
  async assertText(locator: Locator, text: string | RegExp): Promise<void> {
    await expect(locator).toContainText(text);
  }
}
