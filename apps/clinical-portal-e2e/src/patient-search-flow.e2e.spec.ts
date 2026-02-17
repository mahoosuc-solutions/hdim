import { test, expect } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from './fixtures/auth.fixture';

/**
 * E2E Test - Patient Search Flow (#267)
 */
test.describe('Patient Search Flow', () => {
  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
    await navigateAuthenticated(page, '/patients');
    await waitForAppReady(page);
  });

  test('completes patient search and patient detail verification workflow', async ({ page }) => {
    await expect(page).toHaveURL(/patients/);

    // Step 2: Enter patient name in search
    const searchInput = page.locator(
      'input[placeholder*="search" i], input[type="search"], [data-test-id="patient-search"]'
    ).first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    await searchInput.fill('John');
    await page.waitForTimeout(600);

    // Step 3: Verify autocomplete suggestions or search results appear
    const suggestions = page.locator(
      'mat-option, [role="option"], .autocomplete-option, .search-suggestion'
    );
    const rows = page.locator('table tbody tr, mat-row, .patient-row');
    const suggestionsCount = await suggestions.count();
    const rowCount = await rows.count();
    expect(suggestionsCount > 0 || rowCount > 0).toBeTruthy();

    // Step 4: Select patient from suggestions/results
    if (suggestionsCount > 0) {
      await suggestions.first().click();
    } else {
      await rows.first().click();
    }
    await page.waitForTimeout(800);

    // Step 5: Verify patient 360/patient detail loads
    const detailContainers = page.locator(
      'app-patient-detail, .patient-detail, .patient-360, .patient-details-panel'
    );
    const onDetailRoute = /\/patients\/[^/]+/.test(page.url());
    const detailVisible = (await detailContainers.count()) > 0;
    expect(onDetailRoute || detailVisible).toBeTruthy();

    // Step 6: Verify demographics content is present
    const demographicsSignals = page.locator(
      'text=/demographics|date of birth|dob|mrn|medical record number/i'
    );
    await expect(demographicsSignals.first()).toBeVisible({ timeout: 10000 });
  });
});
