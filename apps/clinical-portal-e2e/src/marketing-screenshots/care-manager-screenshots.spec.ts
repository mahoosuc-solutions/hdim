import { test, expect } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'care-manager';

/**
 * Marketing Screenshot Capture — Care Manager Role
 *
 * Captures 10 screenshots telling the story:
 *   "A care manager closes Eleanor Anderson's mammography gap in 8 seconds"
 *
 * These screenshots feed into the Remotion RoleCareManager video composition.
 * Run: npx playwright test care-manager-screenshots.spec.ts
 */
test.describe('Care Manager Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });
  test.setTimeout(60000); // 60s per test — interactions with care gap tables can be slow

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Dashboard overview with care gap widget', async ({ page }) => {
    await waitForAppReady(page);
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'dashboard-overview');
  });

  test('02 — Care gaps table with summary stats', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '02', 'care-gaps-table');
  });

  test('03 — HIGH urgency filter applied', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Try to apply urgency/priority filter
    const urgencyFilter = page
      .locator('mat-select, select, [role="combobox"]')
      .filter({ hasText: /urgency|priority|severity/i })
      .first();

    if (await urgencyFilter.isVisible({ timeout: 3000 }).catch(() => false)) {
      await urgencyFilter.click();
      const highOption = page
        .locator('mat-option, option')
        .filter({ hasText: /high/i })
        .first();
      if (await highOption.isVisible({ timeout: 2000 }).catch(() => false)) {
        await highOption.click();
      } else {
        await page.keyboard.press('Escape');
      }
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '03', 'high-urgency-filter');
  });

  test('04 — Eleanor Anderson row highlighted', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Look for a patient row — find a specific row or the first one
    const eleanorRow = page
      .locator('table tbody tr, mat-row, .care-gap-row')
      .filter({ hasText: /eleanor|anderson/i })
      .first();

    const firstRow = page.locator('table tbody tr, mat-row, .care-gap-row').first();
    const targetRow = (await eleanorRow.isVisible({ timeout: 3000 }).catch(() => false))
      ? eleanorRow
      : firstRow;

    if (await targetRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await targetRow.hover();
      await page.waitForTimeout(300);
    }

    await captureScreenshot(page, ROLE, '04', 'eleanor-row');
  });

  test('05 — Gap detail view', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click first gap row to open detail
    const firstRow = page.locator('table tbody tr, mat-row, .care-gap-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '05', 'gap-detail');
  });

  test('06 — Intervention dialog', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click first gap row
    const firstRow = page.locator('table tbody tr, mat-row, .care-gap-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    // Click close/resolve/action button
    const actionButton = page
      .locator('button, [role="button"]')
      .filter({ hasText: /close|resolve|schedule|intervene|action/i })
      .first();

    if (await actionButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await actionButton.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '06', 'intervention-dialog');
  });

  test('07 — Gap closed with success notification', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click first gap row
    const firstRow = page.locator('table tbody tr, mat-row, .care-gap-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    // Attempt to close the gap
    const closeButton = page
      .locator('button, [role="button"]')
      .filter({ hasText: /close gap|close|resolve|complete/i })
      .first();

    if (await closeButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await closeButton.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);

      // Fill closure reason if dialog appears
      const reasonField = page.locator('textarea, input[type="text"]').last();
      if (await reasonField.isVisible({ timeout: 2000 }).catch(() => false)) {
        await reasonField.fill('Mammography screening scheduled and completed');
      }

      // Confirm closure
      const confirmButton = page
        .locator('button, [role="button"]')
        .filter({ hasText: /confirm|submit|save|yes/i })
        .first();
      if (await confirmButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmButton.click();
        await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
      }
    }

    await captureScreenshot(page, ROLE, '07', 'gap-closed');
  });

  test('08 — Patient list (health records)', async ({ page }) => {
    await navigateAuthenticated(page, '/patients');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'patient-detail');
  });

  test('09 — Outreach campaigns page', async ({ page }) => {
    await navigateAuthenticated(page, '/outreach-campaigns');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'outreach-campaigns');
  });

  test('10 — Dashboard with updated compliance percentage', async ({ page }) => {
    await navigateAuthenticated(page, '/dashboard');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'dashboard-updated');
  });
});
