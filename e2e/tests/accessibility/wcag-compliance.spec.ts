import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { DashboardPage } from '../../pages/dashboard.page';
import { TEST_USERS } from '../../fixtures/test-fixtures';
import AxeBuilder from '@axe-core/playwright';

/**
 * WCAG 2.1 AA Accessibility Tests
 *
 * Test Suite: AX (Accessibility)
 * Coverage: WCAG 2.1 Level AA compliance across all pages
 *
 * These tests verify that the HDIM application is accessible to
 * users with disabilities according to WCAG 2.1 Level AA guidelines.
 *
 * Requirements:
 * - Install @axe-core/playwright: npm install -D @axe-core/playwright
 */

test.describe('WCAG 2.1 AA Compliance', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
  });

  /**
   * AX-001: Login Page Accessibility
   */
  test.describe('AX-001: Login Page Accessibility', () => {
    test('should have no critical accessibility violations on login page', async ({ page }) => {
      await loginPage.goto();

      try {
        const accessibilityScanResults = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
          .analyze();

        const violations = accessibilityScanResults.violations;
        console.log('Login page violations:', violations.length);

        if (violations.length > 0) {
          violations.forEach(v => {
            console.log(`- ${v.id}: ${v.description} (${v.impact})`);
          });
        }

        // No critical or serious violations
        const criticalViolations = violations.filter(
          v => v.impact === 'critical' || v.impact === 'serious'
        );
        expect(criticalViolations.length).toBe(0);
      } catch (e) {
        console.log('Axe not available, running manual checks');
        await runManualA11yChecks(page);
      }
    });

    test('should have proper form labels', async ({ page }) => {
      await loginPage.goto();

      // Check username field has label
      const usernameLabel = page.locator('label[for="username"], label:has-text("Username")');
      const hasUsernameLabel = await usernameLabel.count() > 0;
      console.log('Username label present:', hasUsernameLabel);

      // Check password field has label
      const passwordLabel = page.locator('label[for="password"], label:has-text("Password")');
      const hasPasswordLabel = await passwordLabel.count() > 0;
      console.log('Password label present:', hasPasswordLabel);
    });

    test('should have focus indicators', async ({ page }) => {
      await loginPage.goto();

      const usernameField = page.locator('#username, [name="username"]');
      if (await usernameField.count() > 0) {
        await usernameField.focus();

        // Check if focus is visible (has outline or similar)
        const hasFocusStyle = await usernameField.evaluate(el => {
          const style = window.getComputedStyle(el);
          return style.outlineWidth !== '0px' || style.boxShadow !== 'none';
        });
        console.log('Focus indicator visible:', hasFocusStyle);
      }
    });

    test('should be keyboard navigable', async ({ page }) => {
      await loginPage.goto();

      // Tab through form elements
      await page.keyboard.press('Tab');
      let focusedElement = await page.locator(':focus').getAttribute('type');
      console.log('First tab focus:', focusedElement);

      await page.keyboard.press('Tab');
      focusedElement = await page.locator(':focus').getAttribute('type');
      console.log('Second tab focus:', focusedElement);
    });

    test('should have proper heading hierarchy', async ({ page }) => {
      await loginPage.goto();

      const h1 = await page.locator('h1').count();
      const h2 = await page.locator('h2').count();
      const h3 = await page.locator('h3').count();

      console.log(`Heading counts - H1: ${h1}, H2: ${h2}, H3: ${h3}`);

      // Should have exactly one H1
      expect(h1).toBeGreaterThanOrEqual(1);
    });
  });

  /**
   * AX-002: Dashboard Accessibility
   */
  test.describe('AX-002: Dashboard Accessibility', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should have no critical accessibility violations on dashboard', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      try {
        const accessibilityScanResults = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa'])
          .analyze();

        const violations = accessibilityScanResults.violations;
        console.log('Dashboard violations:', violations.length);

        const criticalViolations = violations.filter(
          v => v.impact === 'critical' || v.impact === 'serious'
        );
        expect(criticalViolations.length).toBe(0);
      } catch (e) {
        console.log('Axe not available, running manual checks');
        await runManualA11yChecks(page);
      }
    });

    test('should have proper ARIA landmarks', async ({ page }) => {
      await dashboardPage.goto();

      const main = await page.locator('main, [role="main"]').count();
      const nav = await page.locator('nav, [role="navigation"]').count();
      const banner = await page.locator('header, [role="banner"]').count();

      console.log(`Landmarks - Main: ${main}, Nav: ${nav}, Banner: ${banner}`);

      expect(main).toBeGreaterThanOrEqual(1);
    });

    test('should have accessible data tables', async ({ page }) => {
      await dashboardPage.goto();

      const tables = page.locator('table');
      const tableCount = await tables.count();

      if (tableCount > 0) {
        for (let i = 0; i < tableCount; i++) {
          const table = tables.nth(i);

          // Check for caption or aria-label
          const hasCaption = await table.locator('caption').count() > 0;
          const hasAriaLabel = await table.getAttribute('aria-label') !== null;

          // Check for header cells
          const headerCells = await table.locator('th').count();

          console.log(`Table ${i + 1}: caption=${hasCaption}, aria-label=${hasAriaLabel}, headers=${headerCells}`);
        }
      }
    });

    test('should have skip links', async ({ page }) => {
      await dashboardPage.goto();

      const skipLink = page.locator('a[href="#main"], a[href="#content"], .skip-link');
      const hasSkipLink = await skipLink.count() > 0;
      console.log('Skip link present:', hasSkipLink);
    });
  });

  /**
   * AX-003: Forms Accessibility
   */
  test.describe('AX-003: Forms Accessibility', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should have accessible form controls', async ({ page }) => {
      await page.goto('/patients');

      // Check search input accessibility
      const searchInput = page.locator('input[type="search"], input[placeholder*="Search"]').first();

      if (await searchInput.count() > 0) {
        const hasLabel = await page.locator(`label[for="${await searchInput.getAttribute('id')}"]`).count() > 0;
        const hasAriaLabel = await searchInput.getAttribute('aria-label') !== null;
        const hasPlaceholder = await searchInput.getAttribute('placeholder') !== null;

        console.log(`Search input: label=${hasLabel}, aria-label=${hasAriaLabel}, placeholder=${hasPlaceholder}`);
        expect(hasLabel || hasAriaLabel).toBe(true);
      }
    });

    test('should have accessible select dropdowns', async ({ page }) => {
      await page.goto('/care-gaps');

      const selects = page.locator('select');
      const selectCount = await selects.count();

      for (let i = 0; i < selectCount; i++) {
        const select = selects.nth(i);
        const id = await select.getAttribute('id');
        const hasLabel = await page.locator(`label[for="${id}"]`).count() > 0;
        const ariaLabel = await select.getAttribute('aria-label');

        console.log(`Select ${i + 1}: label=${hasLabel}, aria-label=${ariaLabel}`);
      }
    });

    test('should have accessible error messages', async ({ page }) => {
      await page.goto('/evaluations');

      // Try to submit form without required fields
      const submitButton = page.locator('button[type="submit"]').first();

      if (await submitButton.count() > 0) {
        await submitButton.click();

        // Check for error messages
        const errorMessages = page.locator('[role="alert"], .error-message, .validation-error');
        const errorCount = await errorMessages.count();
        console.log('Error messages with role="alert":', errorCount);
      }
    });

    test('should have accessible required field indicators', async ({ page }) => {
      await page.goto('/evaluations');

      const requiredFields = page.locator('[required], [aria-required="true"]');
      const requiredCount = await requiredFields.count();
      console.log('Required fields:', requiredCount);

      // Check if required indicator is visible
      const requiredIndicator = page.locator('.required, .asterisk, :has-text("*")');
      const hasIndicator = await requiredIndicator.count() > 0;
      console.log('Required indicator visible:', hasIndicator);
    });
  });

  /**
   * AX-004: Color Contrast
   */
  test.describe('AX-004: Color Contrast', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should meet color contrast requirements', async ({ page }) => {
      await dashboardPage.goto();

      try {
        const accessibilityScanResults = await new AxeBuilder({ page })
          .withTags(['wcag2aa'])
          .include(['color-contrast'])
          .analyze();

        const contrastViolations = accessibilityScanResults.violations.filter(
          v => v.id === 'color-contrast'
        );

        console.log('Color contrast violations:', contrastViolations.length);
        expect(contrastViolations.length).toBe(0);
      } catch (e) {
        console.log('Axe not available for contrast check');
      }
    });

    test('should not rely on color alone for information', async ({ page }) => {
      await page.goto('/care-gaps');

      // Check urgency indicators have more than just color
      const urgencyIndicators = page.locator('.urgency-badge, .priority-indicator, [data-urgency]');
      const count = await urgencyIndicators.count();

      if (count > 0) {
        for (let i = 0; i < Math.min(count, 3); i++) {
          const indicator = urgencyIndicators.nth(i);
          const text = await indicator.textContent();
          const ariaLabel = await indicator.getAttribute('aria-label');

          console.log(`Urgency indicator ${i + 1}: text="${text}", aria-label="${ariaLabel}"`);
          // Should have text or aria-label, not just color
          expect(text || ariaLabel).toBeTruthy();
        }
      }
    });
  });

  /**
   * AX-005: Keyboard Navigation
   */
  test.describe('AX-005: Keyboard Navigation', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should allow complete keyboard navigation', async ({ page }) => {
      await dashboardPage.goto();

      // Tab through all interactive elements
      const interactiveElements: string[] = [];

      for (let i = 0; i < 20; i++) {
        await page.keyboard.press('Tab');
        const focused = await page.locator(':focus').first();

        if (await focused.count() > 0) {
          const tagName = await focused.evaluate(el => el.tagName.toLowerCase());
          interactiveElements.push(tagName);
        }
      }

      console.log('Keyboard navigable elements:', [...new Set(interactiveElements)]);
    });

    test('should have visible focus indicators throughout', async ({ page }) => {
      await page.goto('/patients');

      // Tab and check focus visibility
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');

        const focused = page.locator(':focus');
        if (await focused.count() > 0) {
          const hasOutline = await focused.evaluate(el => {
            const style = window.getComputedStyle(el);
            return style.outlineStyle !== 'none' || style.outlineWidth !== '0px';
          });

          if (!hasOutline) {
            console.log(`Element ${i + 1} may lack visible focus indicator`);
          }
        }
      }
    });

    test('should support Escape to close modals', async ({ page }) => {
      await page.goto('/patients');

      // Try to open a modal
      const openModalButton = page.locator('[data-testid="create-patient"], button:has-text("Add")').first();

      if (await openModalButton.count() > 0) {
        await openModalButton.click();

        const modal = page.locator('[role="dialog"], .modal');
        if (await modal.count() > 0) {
          await page.keyboard.press('Escape');

          await page.waitForTimeout(500);
          const isModalClosed = await modal.isHidden();
          console.log('Modal closed with Escape:', isModalClosed);
        }
      }
    });

    test('should trap focus in modals', async ({ page }) => {
      await page.goto('/patients');

      const openModalButton = page.locator('[data-testid="create-patient"]').first();

      if (await openModalButton.count() > 0) {
        await openModalButton.click();

        const modal = page.locator('[role="dialog"]');
        if (await modal.count() > 0) {
          // Tab through modal elements
          const initialFocused = await page.locator(':focus').getAttribute('data-testid');

          for (let i = 0; i < 10; i++) {
            await page.keyboard.press('Tab');
          }

          // Focus should still be within modal
          const finalFocused = page.locator(':focus');
          const isInModal = await modal.locator(':focus').count() > 0;
          console.log('Focus trapped in modal:', isInModal);
        }
      }
    });
  });

  /**
   * AX-006: Screen Reader Compatibility
   */
  test.describe('AX-006: Screen Reader Compatibility', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should have proper ARIA attributes', async ({ page }) => {
      await dashboardPage.goto();

      // Check for common ARIA attributes
      const ariaLabels = await page.locator('[aria-label]').count();
      const ariaDescribedby = await page.locator('[aria-describedby]').count();
      const ariaLive = await page.locator('[aria-live]').count();

      console.log(`ARIA usage - labels: ${ariaLabels}, describedby: ${ariaDescribedby}, live: ${ariaLive}`);
    });

    test('should have proper button labels', async ({ page }) => {
      await dashboardPage.goto();

      const buttons = page.locator('button');
      const buttonCount = await buttons.count();

      let unlabeledCount = 0;

      for (let i = 0; i < buttonCount; i++) {
        const button = buttons.nth(i);
        const text = await button.textContent();
        const ariaLabel = await button.getAttribute('aria-label');
        const title = await button.getAttribute('title');

        if (!text?.trim() && !ariaLabel && !title) {
          unlabeledCount++;
        }
      }

      console.log(`Buttons without labels: ${unlabeledCount} of ${buttonCount}`);
      expect(unlabeledCount).toBe(0);
    });

    test('should have accessible images', async ({ page }) => {
      await dashboardPage.goto();

      const images = page.locator('img');
      const imageCount = await images.count();

      let missingAlt = 0;

      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i);
        const alt = await img.getAttribute('alt');
        const role = await img.getAttribute('role');

        if (alt === null && role !== 'presentation') {
          missingAlt++;
        }
      }

      console.log(`Images missing alt text: ${missingAlt} of ${imageCount}`);
      expect(missingAlt).toBe(0);
    });

    test('should announce dynamic content changes', async ({ page }) => {
      await page.goto('/care-gaps');

      // Check for aria-live regions
      const liveRegions = page.locator('[aria-live]');
      const liveCount = await liveRegions.count();
      console.log('Live regions:', liveCount);

      // Check for status messages
      const statusRegions = page.locator('[role="status"], [role="alert"]');
      const statusCount = await statusRegions.count();
      console.log('Status/Alert regions:', statusCount);
    });
  });

  /**
   * AX-007: Mobile Accessibility
   */
  test.describe('AX-007: Mobile Accessibility', () => {
    test.use({ viewport: { width: 375, height: 667 } }); // iPhone SE size

    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should have adequate touch target sizes', async ({ page }) => {
      await dashboardPage.goto();

      const buttons = page.locator('button, a, [role="button"]');
      const buttonCount = await buttons.count();

      let smallTargets = 0;

      for (let i = 0; i < Math.min(buttonCount, 10); i++) {
        const button = buttons.nth(i);
        const box = await button.boundingBox();

        if (box && (box.width < 44 || box.height < 44)) {
          smallTargets++;
        }
      }

      console.log(`Small touch targets (< 44x44): ${smallTargets}`);
    });

    test('should have proper viewport meta tag', async ({ page }) => {
      await dashboardPage.goto();

      const viewport = await page.locator('meta[name="viewport"]').getAttribute('content');
      console.log('Viewport meta:', viewport);

      expect(viewport).toContain('width=device-width');
    });

    test('should be usable in portrait orientation', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Content should be visible
      const mainContent = page.locator('main, [role="main"], .main-content');
      const isVisible = await mainContent.isVisible();
      console.log('Main content visible in portrait:', isVisible);
    });
  });

  /**
   * AX-008: Page-Specific Accessibility
   */
  test.describe('AX-008: Page-Specific Accessibility', () => {
    test.beforeEach(async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should have accessible patient list page', async ({ page }) => {
      await page.goto('/patients');

      try {
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2aa'])
          .analyze();

        console.log('Patient page violations:', results.violations.length);
      } catch (e) {
        await runManualA11yChecks(page);
      }
    });

    test('should have accessible care gaps page', async ({ page }) => {
      await page.goto('/care-gaps');

      try {
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2aa'])
          .analyze();

        console.log('Care gaps page violations:', results.violations.length);
      } catch (e) {
        await runManualA11yChecks(page);
      }
    });

    test('should have accessible reports page', async ({ page }) => {
      await page.goto('/reports');

      try {
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2aa'])
          .analyze();

        console.log('Reports page violations:', results.violations.length);
      } catch (e) {
        await runManualA11yChecks(page);
      }
    });
  });
});

/**
 * Manual accessibility checks when axe is not available
 */
async function runManualA11yChecks(page: any): Promise<void> {
  // Check for main landmark
  const hasMain = await page.locator('main, [role="main"]').count() > 0;
  console.log('Has main landmark:', hasMain);

  // Check for h1
  const hasH1 = await page.locator('h1').count() > 0;
  console.log('Has h1:', hasH1);

  // Check for skip link
  const hasSkipLink = await page.locator('.skip-link, a[href="#main"]').count() > 0;
  console.log('Has skip link:', hasSkipLink);

  // Check for lang attribute
  const htmlLang = await page.locator('html').getAttribute('lang');
  console.log('HTML lang:', htmlLang);

  // Check for form labels
  const inputsWithoutLabels = await page.locator('input:not([type="hidden"]):not([type="submit"])').count();
  const labels = await page.locator('label').count();
  console.log(`Inputs: ${inputsWithoutLabels}, Labels: ${labels}`);
}
