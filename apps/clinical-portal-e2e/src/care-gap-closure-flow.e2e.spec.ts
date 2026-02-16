import { test, expect } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from './fixtures/auth.fixture';

/**
 * E2E Test - Care Gap Closure Flow (#268)
 */
test.describe('Care Gap Closure Flow', () => {
  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
    await navigateAuthenticated(page, '/care-gaps');
    await waitForAppReady(page);
  });

  test('closes a care gap and verifies CLOSED outcome', async ({ page }) => {
    await expect(page).toHaveURL(/care-gap/);

    // Step 3: Filter for open gaps
    const statusFilter = page.locator(
      'mat-select[formcontrolname*="status" i], select[formcontrolname*="status" i], mat-select, select'
    ).first();
    if (await statusFilter.isVisible().catch(() => false)) {
      await statusFilter.click();
      const openOption = page.locator('mat-option, option').filter({ hasText: /open/i }).first();
      if (await openOption.isVisible().catch(() => false)) {
        await openOption.click();
      } else {
        await page.keyboard.press('Escape');
      }
      await page.waitForTimeout(500);
    }

    // Step 4: Select a gap
    const rows = page.locator('table tbody tr, mat-row, .care-gap-row');
    await expect(rows.first()).toBeVisible({ timeout: 10000 });
    const selectedRow = rows.first();
    await selectedRow.click();

    // Step 5: Click Close Gap
    const closeAction = page.locator(
      'button, [role="button"], mat-menu-item'
    ).filter({ hasText: /close gap|close|resolve|complete/i }).first();
    await expect(closeAction).toBeVisible({ timeout: 10000 });
    await closeAction.click();
    await page.waitForTimeout(400);

    // Step 6: Enter closure reason
    const reasonSelect = page.locator('mat-select, select').filter({ hasText: /reason/i }).first();
    if (await reasonSelect.isVisible().catch(() => false)) {
      await reasonSelect.click();
      const reasonOption = page.locator('mat-option, option').first();
      await expect(reasonOption).toBeVisible({ timeout: 5000 });
      await reasonOption.click();
    } else {
      const reasonInput = page.locator(
        'textarea[placeholder*="reason" i], input[placeholder*="reason" i], textarea, input'
      ).first();
      await expect(reasonInput).toBeVisible({ timeout: 10000 });
      await reasonInput.fill('Gap closed after chart review.');
    }

    const confirmClose = page.locator('button, [role="button"]').filter({
      hasText: /confirm|submit|save|close gap|close/i,
    }).first();
    await expect(confirmClose).toBeVisible({ timeout: 10000 });
    await confirmClose.click();

    // Step 7: Verify status changes to CLOSED
    await page.waitForTimeout(1000);
    const closedIndicators = page.locator('text=/closed|resolved|complete/i');
    await expect(closedIndicators.first()).toBeVisible({ timeout: 10000 });
  });
});
