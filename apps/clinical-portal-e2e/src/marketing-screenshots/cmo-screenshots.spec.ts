import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'cmo';

/**
 * Marketing Screenshot Capture — CMO / VP Quality Role
 *
 * Story: "How Are Our Star Ratings?"
 * Dashboard → HEDIS measures → CDC detail → CQL results → comparison →
 * care gap summary → risk strat → QRDA reports → constellation → AI insights
 */
test.describe('CMO Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Executive dashboard overview', async ({ page }) => {
    await waitForAppReady(page);
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'dashboard');
  });

  test('02 — Quality measures list', async ({ page }) => {
    await navigateAuthenticated(page, '/quality-measures');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '02', 'quality-measures');
  });

  test('03 — CDC measure detail', async ({ page }) => {
    await navigateAuthenticated(page, '/quality-measures');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    const firstRow = page.locator('table tbody tr, mat-row, .measure-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '03', 'cdc-measure-detail');
  });

  test('04 — CQL evaluation results', async ({ page }) => {
    await navigateAuthenticated(page, '/results');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '04', 'evaluation-results');
  });

  test('05 — Measure comparison', async ({ page }) => {
    await navigateAuthenticated(page, '/evaluations');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'measure-comparison');
  });

  test('06 — Care gap summary', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '06', 'care-gap-summary');
  });

  test('07 — Risk stratification', async ({ page }) => {
    await navigateAuthenticated(page, '/risk-stratification');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '07', 'risk-stratification');
  });

  test('08 — Reports / QRDA export', async ({ page }) => {
    await navigateAuthenticated(page, '/reports');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'reports');
  });

  test('09 — Quality constellation', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/quality-constellation');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'quality-constellation');
  });

  test('10 — AI insights', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'ai-insights');
  });
});
