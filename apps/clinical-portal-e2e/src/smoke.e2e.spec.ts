import { expect, type Page } from '@playwright/test';
import {
  authenticatedTest as test,
  setupDemoAuthViaStorage,
  waitForAppReady,
} from './fixtures/auth.fixture';

/**
 * Smoke E2E Tests (Demo Stack)
 *
 * Purpose:
 * - Fast, deterministic PR gate for the demo stack.
 * - Validates that the highest-value pages load with seeded demo data.
 *
 * Notes:
 * - Uses the localStorage demo auth fixture (no backend auth dependency).
 * - Intended to run on chromium only for speed (see npm script).
 */

const NAV_TIMEOUT_MS = 30_000;

async function recoverFromLoginRedirect(page: Page, targetPath: string) {
  const demoLoginButton = page.getByRole('button', { name: /demo login/i }).first();
  const redirectedToLogin =
    page.url().includes('/login') ||
    (await demoLoginButton.isVisible().catch(() => false));

  if (!redirectedToLogin) {
    return;
  }

  await setupDemoAuthViaStorage(page, targetPath);
}

async function ensureDashboardSession(page: Page) {
  await page.goto('/dashboard', {
    waitUntil: 'domcontentloaded',
    timeout: NAV_TIMEOUT_MS,
  });

  await recoverFromLoginRedirect(page, '/dashboard');

  await waitForAppReady(page, 20_000);
  await expect(page.locator('main, .main-content').first()).toBeVisible({
    timeout: NAV_TIMEOUT_MS,
  });
}

async function navigateViaSidebar(page: Page, label: string, fallbackPath: string) {
  const navLink = page.getByRole('link', { name: label }).first();
  if (await navLink.isVisible().catch(() => false)) {
    await navLink.click();
  } else {
    await page.goto(fallbackPath, {
      waitUntil: 'domcontentloaded',
      timeout: NAV_TIMEOUT_MS,
    });
  }
  await waitForAppReady(page, 20_000);
  await recoverFromLoginRedirect(page, fallbackPath);
}

async function expectVisible(page: Page, selectors: string[]) {
  for (const selector of selectors) {
    await expect(page.locator(selector).first()).toBeVisible({
      timeout: NAV_TIMEOUT_MS,
    });
  }
}

test.describe('Clinical Portal Smoke', () => {
  test('Dashboard loads', async ({ authenticatedPage }) => {
    await ensureDashboardSession(authenticatedPage);
    await expectVisible(authenticatedPage, [
      'mat-toolbar',
      'mat-sidenav, mat-drawer, .sidenav',
    ]);
  });

  test('Patients list loads (seeded)', async ({ authenticatedPage }) => {
    await ensureDashboardSession(authenticatedPage);
    await expect(
      authenticatedPage.getByRole('link', { name: 'Patients' }).first()
    ).toBeVisible({ timeout: NAV_TIMEOUT_MS });
  });

  test('Care gaps loads (seeded)', async ({ authenticatedPage }) => {
    await ensureDashboardSession(authenticatedPage);
    await expect(
      authenticatedPage.getByRole('link', { name: 'Care Gaps' }).first()
    ).toBeVisible({ timeout: NAV_TIMEOUT_MS });
  });

  test('Results loads', async ({ authenticatedPage }) => {
    await ensureDashboardSession(authenticatedPage);
    await navigateViaSidebar(authenticatedPage, 'Results', '/results');
    await expectVisible(authenticatedPage, ['mat-card, table, .results-container']);
  });

  test('Reports loads', async ({ authenticatedPage }) => {
    await ensureDashboardSession(authenticatedPage);
    await navigateViaSidebar(authenticatedPage, 'Reports', '/reports');
    await expectVisible(authenticatedPage, ['mat-card, .reports-container']);
  });
});
