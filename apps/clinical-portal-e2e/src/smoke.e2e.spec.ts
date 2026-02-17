import { expect, type Page } from '@playwright/test';
import { authenticatedTest as test, waitForAppReady } from './fixtures/auth.fixture';

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

async function gotoAndAssert(
  page: Page,
  path: string,
  selectors: string[]
) {
  await page.goto(path, { waitUntil: 'domcontentloaded', timeout: NAV_TIMEOUT_MS });
  await waitForAppReady(page, 20_000);
  for (const selector of selectors) {
    await expect(page.locator(selector).first()).toBeVisible({ timeout: NAV_TIMEOUT_MS });
  }
}

test.describe('Clinical Portal Smoke', () => {
  test('Dashboard loads', async ({ authenticatedPage }) => {
    await gotoAndAssert(authenticatedPage, '/dashboard', [
      'mat-toolbar',
      'mat-sidenav, mat-drawer, .sidenav',
    ]);
  });

  test('Patients list loads (seeded)', async ({ authenticatedPage }) => {
    await gotoAndAssert(authenticatedPage, '/patients', ['table, mat-table, .patients-table']);
  });

  test('Care gaps loads (seeded)', async ({ authenticatedPage }) => {
    await gotoAndAssert(authenticatedPage, '/care-gaps', ['mat-card, table, mat-table, .care-gaps']);
  });

  test('Results loads', async ({ authenticatedPage }) => {
    await gotoAndAssert(authenticatedPage, '/results', ['mat-card, table, .results-container']);
  });

  test('Reports loads', async ({ authenticatedPage }) => {
    await gotoAndAssert(authenticatedPage, '/reports', ['mat-card, .reports-container']);
  });
});
