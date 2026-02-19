import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'quality-analyst';

/**
 * Marketing Screenshot Capture — Quality Analyst Role
 *
 * Story: "Evaluate Measures, Generate Reports"
 * Measures list → select CDC → measure detail → run evaluation → batch processing →
 * results → non-compliant drilldown → QRDA export → report builder → trends
 */
test.describe('Quality Analyst Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Quality measures list', async ({ page }) => {
    await navigateAuthenticated(page, '/quality-measures');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'measures-list');
  });

  test('02 — Select CDC measure', async ({ page }) => {
    await navigateAuthenticated(page, '/quality-measures');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    const cdcRow = page
      .locator('table tbody tr, mat-row')
      .filter({ hasText: /CDC|diabetes|comprehensive/i })
      .first();

    const firstRow = page.locator('table tbody tr, mat-row').first();
    const target = (await cdcRow.isVisible({ timeout: 3000 }).catch(() => false))
      ? cdcRow
      : firstRow;

    if (await target.isVisible({ timeout: 5000 }).catch(() => false)) {
      await target.hover();
      await page.waitForTimeout(300);
    }

    await captureScreenshot(page, ROLE, '02', 'select-cdc');
  });

  test('03 — Measure detail with CQL logic', async ({ page }) => {
    await navigateAuthenticated(page, '/quality-measures');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    const firstRow = page.locator('table tbody tr, mat-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '03', 'measure-detail');
  });

  test('04 — Run evaluation button', async ({ page }) => {
    await navigateAuthenticated(page, '/evaluations');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    try {
      const runButton = page
        .locator('button, [role="button"]')
        .filter({ hasText: /run|evaluate|execute|start/i })
        .first();

      if (await runButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await runButton.hover({ timeout: 2000 });
        await page.waitForTimeout(300);
      }
    } catch {
      // Button not interactable in demo mode — capture page as-is
    }

    await captureScreenshot(page, ROLE, '04', 'run-evaluation');
  });

  test('05 — Batch processing / live monitor', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/live-monitor');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'batch-processing');
  });

  test('06 — Results with pass/fail breakdown', async ({ page }) => {
    await navigateAuthenticated(page, '/results');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '06', 'results-pass-fail');
  });

  test('07 — Non-compliant patient drilldown', async ({ page }) => {
    await navigateAuthenticated(page, '/results');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click on a result row to drill in
    const firstRow = page.locator('table tbody tr, mat-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '07', 'non-compliant-drilldown');
  });

  test('08 — QRDA export', async ({ page }) => {
    await navigateAuthenticated(page, '/reports');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Hover export button if visible
    const exportButton = page
      .locator('button, [role="button"]')
      .filter({ hasText: /export|QRDA|download/i })
      .first();

    if (await exportButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await exportButton.hover();
      await page.waitForTimeout(300);
    }

    await captureScreenshot(page, ROLE, '08', 'qrda-export');
  });

  test('09 — Report builder', async ({ page }) => {
    await navigateAuthenticated(page, '/reports');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'report-builder');
  });

  test('10 — Measure comparison trends', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/measure-matrix');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'measure-trends');
  });
});
