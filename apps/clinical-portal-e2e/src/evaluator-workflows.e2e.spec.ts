import { test, expect } from '@playwright/test';

/**
 * EVALUATOR Role Workflow E2E Tests
 *
 * Tests evaluator-specific workflows:
 * - Create evaluation
 * - Run batch evaluation
 * - View evaluation results
 * - Export evaluation data
 * - Trigger CQL measure execution
 *
 * @tags @e2e @role-evaluator @quality-measures
 */

const EVALUATOR_USER = {
  username: 'test_evaluator',
  password: 'password123',
  roles: ['EVALUATOR'],
  tenantId: 'tenant-a',
};

test.describe('EVALUATOR Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    // Login as evaluator
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', EVALUATOR_USER.username);
    await page.fill('[data-test-id="password"]', EVALUATOR_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should create single patient evaluation', async ({ page }) => {
    // Navigate to evaluations
    await page.click('[data-test-id="nav-evaluations"]');
    await page.waitForURL('/evaluations');

    // Click create evaluation button
    await page.click('[data-test-id="create-evaluation-button"]');

    // Select patient
    await page.click('[data-test-id="patient-select"]');
    await page.fill('[data-test-id="patient-search"]', 'John Doe');
    await page.click('[data-test-id="patient-option"]').first();

    // Select measure
    await page.click('[data-test-id="measure-select"]');
    await page.click('[data-test-id="measure-option-COL"]'); // Colorectal Cancer Screening

    // Select measurement period
    await page.fill('[data-test-id="period-start"]', '2023-01-01');
    await page.fill('[data-test-id="period-end"]', '2023-12-31');

    // Run evaluation
    await page.click('[data-test-id="run-evaluation-button"]');

    // Wait for evaluation to complete
    await page.waitForSelector('[data-test-id="evaluation-status-complete"]', {
      timeout: 30000,
    });

    // Verify results displayed
    await expect(page.locator('[data-test-id="evaluation-result"]')).toContainText(
      'Evaluation Complete'
    );
    await expect(page.locator('[data-test-id="numerator-result"]')).toBeVisible();
    await expect(page.locator('[data-test-id="denominator-result"]')).toBeVisible();
  });

  test('should run batch evaluation for population', async ({ page }) => {
    // Navigate to batch evaluations
    await page.click('[data-test-id="nav-evaluations"]');
    await page.click('[data-test-id="nav-batch-evaluations"]');
    await page.waitForURL('/evaluations/batch');

    // Click create batch evaluation
    await page.click('[data-test-id="create-batch-evaluation-button"]');

    // Select measure bundle
    await page.click('[data-test-id="measure-bundle-select"]');
    await page.click('[data-test-id="bundle-option-HEDIS-2023"]');

    // Select patient population
    await page.click('[data-test-id="population-select"]');
    await page.click('[data-test-id="population-option-all-active"]');

    // Select measurement period
    await page.fill('[data-test-id="period-start"]', '2023-01-01');
    await page.fill('[data-test-id="period-end"]', '2023-12-31');

    // Start batch evaluation
    await page.click('[data-test-id="start-batch-evaluation-button"]');

    // Verify batch started
    await expect(page.locator('[data-test-id="batch-status"]')).toContainText('Running');

    // Monitor progress
    await page.waitForSelector('[data-test-id="batch-progress"]', { timeout: 60000 });
    const progress = await page.locator('[data-test-id="batch-progress"]').textContent();
    expect(parseInt(progress || '0')).toBeGreaterThan(0);
  });

  test('should view evaluation results and details', async ({ page }) => {
    // Navigate to evaluations
    await page.goto('/evaluations');

    // Click on first evaluation
    await page.locator('[data-test-id="evaluation-row"]').first().click();

    // Verify evaluation details page
    await expect(page.locator('[data-test-id="evaluation-detail"]')).toBeVisible();

    // Verify measure information displayed
    await expect(page.locator('[data-test-id="measure-name"]')).toBeVisible();
    await expect(page.locator('[data-test-id="patient-name"]')).toBeVisible();

    // Verify CQL execution results
    await expect(page.locator('[data-test-id="cql-results"]')).toBeVisible();

    // Verify care gap recommendations (if any)
    const careGaps = page.locator('[data-test-id="care-gap-recommendation"]');
    if ((await careGaps.count()) > 0) {
      await expect(careGaps.first()).toBeVisible();
    }
  });

  test('should export evaluation data to Excel', async ({ page }) => {
    // Navigate to evaluations
    await page.goto('/evaluations');

    // Select multiple evaluations
    await page.locator('[data-test-id="evaluation-checkbox"]').first().check();
    await page.locator('[data-test-id="evaluation-checkbox"]').nth(1).check();

    // Click export button
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-test-id="export-evaluations-button"]');

    // Wait for download
    const download = await downloadPromise;

    // Verify file downloaded
    expect(download.suggestedFilename()).toContain('evaluations');
    expect(download.suggestedFilename()).toContain('.xlsx');
  });

  test('should trigger CQL measure execution manually', async ({ page }) => {
    // Navigate to quality measures
    await page.click('[data-test-id="nav-quality-measures"]');
    await page.waitForURL('/quality-measures');

    // Find CQL measure
    const measureRow = page.locator('[data-test-id="measure-row"]', {
      hasText: 'COL - Colorectal Cancer Screening',
    });

    // Click "Run Measure" button
    await measureRow.locator('[data-test-id="run-measure-button"]').click();

    // Select patient population
    await page.click('[data-test-id="population-select"]');
    await page.click('[data-test-id="population-option-eligible-patients"]');

    // Start measure execution
    await page.click('[data-test-id="start-execution-button"]');

    // Verify execution started
    await expect(page.locator('[data-test-id="execution-status"]')).toContainText('Running');

    // Wait for completion
    await page.waitForSelector('[data-test-id="execution-status-complete"]', {
      timeout: 60000,
    });

    // Verify results summary
    await expect(page.locator('[data-test-id="execution-summary"]')).toBeVisible();
    await expect(page.locator('[data-test-id="total-evaluated"]')).toContainText(/\d+/);
  });
});
