import { test, expect } from '../fixtures/sales-fixtures';

test.describe('Sales Portal Navigation', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
  });

  test('can navigate to sales page', async ({ salesPage }) => {
    await expect(salesPage.pageTitle).toBeVisible();
    await expect(salesPage.pageSubtitle).toBeVisible();
  });

  test('can switch between all 5 tabs', async ({ salesPage }) => {
    // Check Dashboard tab (default)
    await expect(salesPage.dashboardTab).toHaveAttribute('aria-selected', 'true');

    // Switch to Leads tab
    await salesPage.switchToTab('leads');
    await expect(salesPage.leadsTab).toHaveAttribute('aria-selected', 'true');
    await expect(salesPage.dashboardTab).toHaveAttribute('aria-selected', 'false');

    // Switch to Pipeline tab
    await salesPage.switchToTab('pipeline');
    await expect(salesPage.pipelineTab).toHaveAttribute('aria-selected', 'true');

    // Switch to Accounts tab
    await salesPage.switchToTab('accounts');
    await expect(salesPage.accountsTab).toHaveAttribute('aria-selected', 'true');

    // Switch to Sequences tab
    await salesPage.switchToTab('sequences');
    await expect(salesPage.sequencesTab).toHaveAttribute('aria-selected', 'true');

    // Switch back to Dashboard
    await salesPage.switchToTab('dashboard');
    await expect(salesPage.dashboardTab).toHaveAttribute('aria-selected', 'true');
  });

  test('tabs have correct ARIA labels', async ({ salesPage, page }) => {
    // All tabs should have role="tab"
    const tabs = page.locator('[role="tab"]');
    await expect(tabs).toHaveCount(5);

    // Each tab should have an id for accessibility
    for (let i = 0; i < 5; i++) {
      const tab = tabs.nth(i);
      await expect(tab).toHaveAttribute('id', `sales-tab-${i}`);
    }
  });

  test('correct tab is highlighted when active', async ({ salesPage }) => {
    // Dashboard should be selected by default
    const isDashboardSelected = await salesPage.isTabSelected('dashboard');
    expect(isDashboardSelected).toBe(true);

    // Switch to leads and verify
    await salesPage.switchToTab('leads');
    const isLeadsSelected = await salesPage.isTabSelected('leads');
    expect(isLeadsSelected).toBe(true);

    const isDashboardStillSelected = await salesPage.isTabSelected('dashboard');
    expect(isDashboardStillSelected).toBe(false);
  });

  test('tab content loads correctly', async ({ salesPage, page }) => {
    // Dashboard content should be visible by default
    await salesPage.waitForTabContent();
    // Check the visible (non-hidden) tabpanel
    await expect(page.locator('[role="tabpanel"]:not([hidden])')).toBeVisible();

    // Switch to Leads and verify content loads
    await salesPage.switchToTab('leads');
    // Either table or loading/error message should be present
    const hasTable = await page.locator('table').isVisible().catch(() => false);
    const hasContent = await page.locator('[role="tabpanel"]:not([hidden])').isVisible();
    expect(hasTable || hasContent).toBe(true);

    // Switch to Pipeline and verify content loads
    await salesPage.switchToTab('pipeline');
    await expect(page.locator('[role="tabpanel"]:not([hidden])')).toBeVisible();

    // Switch to Accounts and verify content loads
    await salesPage.switchToTab('accounts');
    await expect(page.locator('[role="tabpanel"]:not([hidden])')).toBeVisible();

    // Switch to Sequences and verify content loads
    await salesPage.switchToTab('sequences');
    await expect(page.locator('[role="tabpanel"]:not([hidden])')).toBeVisible();
  });
});
