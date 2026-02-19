import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'provider';

/**
 * Marketing Screenshot Capture — Provider / Physician Role
 *
 * Story: "Pre-Visit Patient Preparation"
 * Each test navigates to a distinct page and captures — no inter-test dependencies.
 */
test.describe('Provider Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });
  test.setTimeout(60000);

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Pre-visit planning dashboard', async ({ page }) => {
    await navigateAuthenticated(page, '/pre-visit');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'pre-visit');
  });

  test('02 — Patient search', async ({ page }) => {
    await navigateAuthenticated(page, '/patients');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    try {
      const searchInput = page.locator('input[type="search"], input[type="text"], input[placeholder*="search" i]').first();
      if (await searchInput.isVisible({ timeout: 3000 }).catch(() => false)) {
        await searchInput.fill('Michael Chen');
        await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
      }
    } catch {
      // Search field not interactable
    }

    await captureScreenshot(page, ROLE, '02', 'patient-search');
  });

  test('03 — Patient demographics', async ({ page }) => {
    await navigateAuthenticated(page, '/patients');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '03', 'patient-demographics');
  });

  test('04 — Patient conditions (clinical view)', async ({ page }) => {
    // Use dashboard clinical view as fallback for conditions
    await navigateAuthenticated(page, '/dashboard');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '04', 'conditions');
  });

  test('05 — Patient medications', async ({ page }) => {
    // Navigate to care recommendations which shows medication context
    await navigateAuthenticated(page, '/care-recommendations');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'medications');
  });

  test('06 — Patient-level care gaps', async ({ page }) => {
    await navigateAuthenticated(page, '/care-gaps');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '06', 'care-gaps');
  });

  test('07 — Care recommendations', async ({ page }) => {
    await navigateAuthenticated(page, '/care-recommendations');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '07', 'care-recommendations');
  });

  test('08 — Risk profile', async ({ page }) => {
    await navigateAuthenticated(page, '/risk-stratification');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'risk-profile');
  });

  test('09 — AI assistant', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'ai-assistant');
  });

  test('10 — Quality constellation (longitudinal)', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/quality-constellation');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'longitudinal-view');
  });
});
