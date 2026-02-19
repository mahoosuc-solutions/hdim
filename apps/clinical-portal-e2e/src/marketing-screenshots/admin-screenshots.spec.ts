import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'admin';

/**
 * Marketing Screenshot Capture — Administrator Role
 *
 * Story: "Security & HIPAA Compliance"
 * Admin dashboard → tenant settings → user management → role assignment →
 * audit logs → filtered search → demo seeding → compliance → deployment → live monitor
 */
test.describe('Admin Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — Admin dashboard', async ({ page }) => {
    await waitForAppReady(page);
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'admin-dashboard');
  });

  test('02 — Tenant settings (multi-tenant isolation)', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/tenant-settings');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '02', 'tenant-settings');
  });

  test('03 — User management', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/users');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '03', 'user-management');
  });

  test('04 — Role assignment (RBAC)', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/users');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Click first user row to open role details
    const firstRow = page.locator('table tbody tr, mat-row').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
    }

    await captureScreenshot(page, ROLE, '04', 'role-assignment');
  });

  test('05 — Audit logs (PHI access trail)', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/audit-logs');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'audit-logs');
  });

  test('06 — Filtered audit search', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/audit-logs');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Apply a date or action filter if available
    const filterInput = page.locator('input[type="search"], input[type="text"], mat-select').first();
    if (await filterInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await filterInput.click();
      await page.waitForTimeout(300);
    }

    await captureScreenshot(page, ROLE, '06', 'audit-search');
  });

  test('07 — Demo data seeding', async ({ page }) => {
    await navigateAuthenticated(page, '/admin/demo-seeding');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '07', 'demo-seeding');
  });

  test('08 — Compliance dashboard', async ({ page }) => {
    await navigateAuthenticated(page, '/compliance');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'compliance-dashboard');
  });

  test('09 — Deployment monitoring', async ({ page }) => {
    await navigateAuthenticated(page, '/testing');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'deployment-monitor');
  });

  test('10 — Live service monitor', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/live-monitor');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'live-monitor');
  });
});
