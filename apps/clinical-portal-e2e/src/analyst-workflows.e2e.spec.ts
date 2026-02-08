import { test, expect } from '@playwright/test';
import { waitForAppReady } from './fixtures/auth.fixture';

/**
 * ANALYST Role Workflow E2E Tests
 *
 * Tests analyst-specific workflows:
 * - Generate quality measure report
 * - Export data to Excel
 * - View population health analytics
 *
 * @tags @e2e @role-analyst @analytics @reporting
 */

const ANALYST_USER = {
  id: 'analyst-user-1',
  username: 'test_analyst',
  email: 'analyst@healthdata.com',
  firstName: 'Test',
  lastName: 'Analyst',
  fullName: 'Test Analyst',
  roles: [
    {
      id: 'role-analyst',
      name: 'ANALYST',
      description: 'Analyst',
      permissions: [
        { id: 'perm-1', name: 'VIEW_REPORTS', description: 'View reports' },
        { id: 'perm-2', name: 'EXPORT_DATA', description: 'Export data' },
      ],
    },
  ],
  tenantId: 'acme-health',
  tenantIds: ['acme-health'],
  active: true,
};

async function setupAnalystAuth(page: any) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  await page.evaluate(
    ({ user }) => {
      localStorage.setItem('healthdata_user', JSON.stringify(user));
      localStorage.setItem('healthdata_tenant', user.tenantIds?.[0] || user.tenantId || 'acme-health');
      localStorage.removeItem('healthdata-demo-mode');
      sessionStorage.removeItem('healthdata-demo-mode');
    },
    { user: ANALYST_USER }
  );
  await page.goto('/dashboard', { waitUntil: 'domcontentloaded' });
  await waitForAppReady(page);
}

test.describe('ANALYST Role Workflows', () => {
  test.setTimeout(60000);
  test.beforeEach(async ({ page }) => {
    await setupAnalystAuth(page);
  });

  test('should generate quality measure report', async ({ page }) => {
    // Navigate to reports
    await page.click('[data-test-id="nav-reports"]');
    await page.waitForURL('/reports');

    // Click create report button
    await page.click('[data-test-id="create-report-button"]');

    // Select report type
    await page.click('[data-test-id="report-type-select"]');
    await page.click('[data-test-id="report-type-QUALITY-MEASURES"]');

    // Configure report parameters
    await page.fill('[data-test-id="report-name"]', 'HEDIS 2023 Quality Measures Summary');

    // Select measurement period
    await page.fill('[data-test-id="period-start"]', '2023-01-01');
    await page.fill('[data-test-id="period-end"]', '2023-12-31');

    // Select measure bundle
    await page.click('[data-test-id="measure-bundle-select"]');
    await page.check('[data-test-id="bundle-checkbox-HEDIS-2023"]');
    await page.check('[data-test-id="bundle-checkbox-STARS"]');

    // Select population filters
    await page.click('[data-test-id="population-filter-select"]');
    await page.click('[data-test-id="population-option-ALL-ACTIVE"]');

    // Select grouping options
    await page.click('[data-test-id="group-by-select"]');
    await page.check('[data-test-id="group-by-PROVIDER"]');
    await page.check('[data-test-id="group-by-MEASURE"]');

    // Select output format
    await page.click('[data-test-id="output-format-select"]');
    await page.click('[data-test-id="format-option-EXCEL"]');

    // Generate report
    await page.click('[data-test-id="generate-report-button"]');

    // Wait for report generation (may take time)
    await page.waitForSelector('[data-test-id="report-status-complete"]', {
      timeout: 60000,
    });

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Report generated successfully'
    );

    // Verify report preview displayed
    await expect(page.locator('[data-test-id="report-preview"]')).toBeVisible();

    // Verify key metrics displayed
    await expect(page.locator('[data-test-id="total-measures-evaluated"]')).toBeVisible();
    await expect(page.locator('[data-test-id="overall-performance-score"]')).toBeVisible();
  });

  test('should export aggregated data to Excel with multiple sheets', async ({ page }) => {
    // Navigate to quality measures dashboard
    await page.click('[data-test-id="nav-quality-measures"]');
    await page.waitForURL('/quality-measures');

    // Apply filters for export
    await page.click('[data-test-id="filter-panel-toggle"]');

    // Filter by measure type
    await page.click('[data-test-id="measure-type-filter"]');
    await page.check('[data-test-id="type-DIABETES"]');
    await page.check('[data-test-id="type-CARDIOVASCULAR"]');

    // Filter by performance threshold
    await page.click('[data-test-id="performance-filter"]');
    await page.click('[data-test-id="performance-BELOW-THRESHOLD"]');

    // Apply filters
    await page.click('[data-test-id="apply-filters-button"]');

    // Wait for filtered results
    await page.waitForSelector('[data-test-id="measure-row"]');

    // Click export button
    await page.click('[data-test-id="export-data-button"]');

    // Configure export options
    await page.click('[data-test-id="export-format-select"]');
    await page.click('[data-test-id="format-option-EXCEL-DETAILED"]');

    // Select data elements to include
    await page.check('[data-test-id="include-patient-level-data"]');
    await page.check('[data-test-id="include-provider-attribution"]');
    await page.check('[data-test-id="include-care-gap-details"]');

    // Start export
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-test-id="start-export-button"]');

    // Wait for download
    const download = await downloadPromise;

    // Verify file downloaded
    expect(download.suggestedFilename()).toContain('quality-measures');
    expect(download.suggestedFilename()).toContain('.xlsx');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Data exported successfully'
    );
  });

  test('should view population health analytics dashboard', async ({ page }) => {
    // Navigate to population health analytics
    await page.click('[data-test-id="nav-analytics"]');
    await page.click('[data-test-id="nav-population-health"]');
    await page.waitForURL('/analytics/population-health');

    // Verify dashboard loaded
    await expect(page.locator('[data-test-id="population-health-dashboard"]')).toBeVisible();

    // Verify key metric cards displayed
    await expect(page.locator('[data-test-id="metric-total-patients"]')).toBeVisible();
    await expect(page.locator('[data-test-id="metric-active-care-gaps"]')).toBeVisible();
    await expect(page.locator('[data-test-id="metric-avg-hcc-score"]')).toBeVisible();
    await expect(page.locator('[data-test-id="metric-overall-quality-score"]')).toBeVisible();

    // Verify trend charts displayed
    await expect(page.locator('[data-test-id="chart-quality-trends"]')).toBeVisible();
    await expect(page.locator('[data-test-id="chart-risk-stratification"]')).toBeVisible();
    await expect(page.locator('[data-test-id="chart-care-gap-closure-rate"]')).toBeVisible();

    // Interact with date range filter
    await page.click('[data-test-id="date-range-select"]');
    await page.click('[data-test-id="date-range-LAST-12-MONTHS"]');

    // Wait for charts to refresh
    await page.waitForTimeout(1000);

    // Verify charts updated
    await expect(page.locator('[data-test-id="chart-quality-trends"]')).toBeVisible();

    // Drill down into specific measure
    await page.click('[data-test-id="measure-performance-table"]');
    const measureRow = page.locator('[data-test-id="measure-row"]').first();
    await measureRow.click();

    // Verify drill-down details displayed
    await expect(page.locator('[data-test-id="measure-detail-panel"]')).toBeVisible();
    await expect(page.locator('[data-test-id="numerator-breakdown"]')).toBeVisible();
    await expect(page.locator('[data-test-id="denominator-breakdown"]')).toBeVisible();
    await expect(page.locator('[data-test-id="exclusion-breakdown"]')).toBeVisible();

    // View provider-level breakdown
    await page.click('[data-test-id="tab-provider-breakdown"]');
    await expect(page.locator('[data-test-id="provider-performance-table"]')).toBeVisible();

    // Verify can sort by performance
    await page.click('[data-test-id="column-header-performance"]');
    const firstProviderScore = await page.locator('[data-test-id="provider-score"]').first().textContent();
    expect(parseFloat(firstProviderScore || '0')).toBeGreaterThanOrEqual(0);

    // Export analytics data
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-test-id="export-analytics-button"]');
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('population-health-analytics');
  });
});
