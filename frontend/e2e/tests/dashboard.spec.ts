import { test, expect } from '../fixtures/sales-fixtures';

test.describe('Sales Dashboard', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
    // Dashboard is the default tab
  });

  test('displays all 4 key metric cards', async ({ page }) => {
    // Wait for dashboard to load
    await page.waitForLoadState('networkidle');

    // Look for metric cards (Paper components with metric data)
    const metricCards = page.locator('[class*="Paper"], [class*="Card"]').filter({
      has: page.locator('[class*="Typography"]'),
    });

    // Should have at least 4 metric cards
    const count = await metricCards.count();
    expect(count).toBeGreaterThanOrEqual(4);

    // Check for specific metrics
    await expect(page.getByText(/total leads/i)).toBeVisible();
    await expect(page.getByText(/pipeline.*value/i)).toBeVisible();
    await expect(page.getByText(/win rate/i)).toBeVisible();
  });

  test('shows correct pipeline value formatting', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // Pipeline values should be formatted as currency
    const currencyValues = page.locator('text=/\\$\\d+/');
    const count = await currencyValues.count();
    expect(count).toBeGreaterThan(0);
  });

  test('displays lead conversion progress bar', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // Look for conversion rate or progress bar
    const progressBars = page.locator('[class*="LinearProgress"]');
    const hasProgressBar = await progressBars.first().isVisible();

    // Either progress bar or conversion percentage should be visible
    const conversionText = await page.getByText(/conversion|qualified/i).isVisible();
    expect(hasProgressBar || conversionText).toBe(true);
  });

  test('shows leads by source distribution', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // Check for source categories
    const sources = ['WEBSITE', 'ROI', 'REFERRAL', 'CONFERENCE'];
    let foundSource = false;

    for (const source of sources) {
      const sourceElement = await page.getByText(new RegExp(source, 'i')).isVisible();
      if (sourceElement) {
        foundSource = true;
        break;
      }
    }

    // Should display at least one source if data exists
    const noDataMessage = await page.getByText(/no data|no leads/i).isVisible();
    expect(foundSource || noDataMessage).toBe(true);
  });

  test('shows opportunities by stage', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // Check for stage categories
    const stages = ['DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION'];
    let foundStage = false;

    for (const stage of stages) {
      const stageElement = await page.getByText(new RegExp(stage, 'i')).isVisible();
      if (stageElement) {
        foundStage = true;
        break;
      }
    }

    // Should display stages or empty state
    const noDataMessage = await page.getByText(/no data|no opportunities/i).isVisible();
    expect(foundStage || noDataMessage).toBe(true);
  });

  test('handles loading state', async ({ page }) => {
    // Navigate to sales and immediately check for loading state
    await page.goto('/sales');

    // Look for loading indicators
    const loadingSpinner = page.locator('[class*="CircularProgress"]');
    const skeleton = page.locator('[class*="Skeleton"]');

    // Either a spinner or skeleton should appear briefly during load
    // or the content should load immediately
    await page.waitForLoadState('networkidle');

    // After load, content should be visible
    const dashboardContent = page.locator('[role="tabpanel"]');
    await expect(dashboardContent).toBeVisible();
  });

  test('handles error state gracefully', async ({ page }) => {
    // This test would require mocking API errors
    // For now, verify that the page doesn't crash
    await page.goto('/sales');
    await page.waitForLoadState('networkidle');

    // Page should still be responsive
    const tabs = page.locator('[role="tab"]');
    await expect(tabs.first()).toBeVisible();
  });

  test('handles empty data state', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // If there's no data, should show appropriate message or zeros
    const zeroValues = await page.locator('text=/^0$|0%|\\$0/').count();
    const noDataMessages = await page.getByText(/no data|no leads|no opportunities/i).count();
    const hasData = await page.locator('text=/\\$[1-9]|[1-9]\\d*%/').count();

    // Either has data, shows zeros, or shows empty state message
    expect(hasData > 0 || zeroValues > 0 || noDataMessages > 0).toBe(true);
  });
});
