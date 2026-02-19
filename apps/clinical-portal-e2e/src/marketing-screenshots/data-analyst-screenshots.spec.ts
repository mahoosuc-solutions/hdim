import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'data-analyst';

/**
 * Marketing Screenshot Capture — Data Analyst Role
 *
 * Story: "Population Health Trends"
 * Dashboard analytics → risk strat → high-risk cohort → constellation →
 * measure matrix → flow network → eval results → scheduled reports →
 * report builder → AI insights
 */
test.describe('Data Analyst Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Dashboard analytics', async ({ page }) => {
    await waitForAppReady(page);
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'dashboard-analytics');
  });

  test('02 — Risk stratification distribution', async ({ page }) => {
    await navigateAuthenticated(page, '/risk-stratification');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '02', 'risk-stratification');
  });

  test('03 — High-risk cohort detail', async ({ page }) => {
    await navigateAuthenticated(page, '/risk-stratification');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click into high-risk segment if possible
    const highRisk = page.locator('table tbody tr, mat-row, [class*="risk"]').filter({ hasText: /high/i }).first();
    if (await highRisk.isVisible({ timeout: 3000 }).catch(() => false)) {
      await highRisk.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '03', 'high-risk-cohort');
  });

  test('04 — Quality constellation', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/quality-constellation');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '04', 'quality-constellation');
  });

  test('05 — Measure matrix heatmap', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/measure-matrix');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'measure-matrix');
  });

  test('06 — Flow network visualization', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/flow-network');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '06', 'flow-network');
  });

  test('07 — Evaluation results table', async ({ page }) => {
    await navigateAuthenticated(page, '/results');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '07', 'evaluation-results');
  });

  test('08 — Scheduled reports', async ({ page }) => {
    await navigateAuthenticated(page, '/reports');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'scheduled-reports');
  });

  test('09 — Report builder', async ({ page }) => {
    await navigateAuthenticated(page, '/reports');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click "New Report" or similar if available
    const newReport = page.locator('button, [role="button"]').filter({ hasText: /new|create|build/i }).first();
    if (await newReport.isVisible({ timeout: 3000 }).catch(() => false)) {
      await newReport.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '09', 'report-builder');
  });

  test('10 — AI insights', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'ai-insights');
  });
});
