import { test, expect } from '@playwright/test';

/**
 * VIEWER Role Workflow E2E Tests
 *
 * Tests viewer-specific workflows:
 * - View reports (read-only validation)
 * - Attempt write operation (should fail with 403)
 * - View care gaps (read-only validation)
 *
 * Purpose: Validate role-based access control for read-only users
 *
 * @tags @e2e @role-viewer @rbac @security
 */

const VIEWER_USER = {
  username: 'test_viewer',
  password: 'password123',
  roles: ['VIEWER'],
  tenantId: 'tenant-a',
};

test.describe('VIEWER Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    // Login as viewer
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', VIEWER_USER.username);
    await page.fill('[data-test-id="password"]', VIEWER_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should view reports in read-only mode', async ({ page }) => {
    // Navigate to reports
    await page.click('[data-test-id="nav-reports"]');
    await page.waitForURL('/reports');

    // Verify reports page accessible
    await expect(page.locator('[data-test-id="reports-page"]')).toBeVisible();

    // Verify report list displayed
    await expect(page.locator('[data-test-id="report-row"]').first()).toBeVisible();

    // Click on first report to view details
    await page.locator('[data-test-id="report-row"]').first().click();

    // Verify report details displayed
    await expect(page.locator('[data-test-id="report-title"]')).toBeVisible();
    await expect(page.locator('[data-test-id="report-data"]')).toBeVisible();

    // Verify edit/delete buttons NOT visible (read-only)
    await expect(page.locator('[data-test-id="edit-report-button"]')).not.toBeVisible();
    await expect(page.locator('[data-test-id="delete-report-button"]')).not.toBeVisible();

    // Verify download button IS visible (viewers can export)
    await expect(page.locator('[data-test-id="download-report-button"]')).toBeVisible();

    // Verify can download report
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-test-id="download-report-button"]');
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('.pdf');
  });

  test('should be denied when attempting write operations', async ({ page }) => {
    // Navigate to patients
    await page.click('[data-test-id="nav-patients"]');
    await page.waitForURL('/patients');

    // Verify patient list is visible (read access)
    await expect(page.locator('[data-test-id="patient-row"]').first()).toBeVisible();

    // Verify create patient button NOT visible
    await expect(page.locator('[data-test-id="create-patient-button"]')).not.toBeVisible();

    // Click on patient to view details
    await page.locator('[data-test-id="patient-row"]').first().click();

    // Verify patient details visible (read access)
    await expect(page.locator('[data-test-id="patient-name"]')).toBeVisible();

    // Verify edit button NOT visible
    await expect(page.locator('[data-test-id="edit-patient-button"]')).not.toBeVisible();

    // Attempt to directly navigate to edit page (URL manipulation)
    const patientId = await page.locator('[data-test-id="patient-id"]').textContent();
    await page.goto(`/patients/${patientId}/edit`);

    // Verify access denied (403 or redirect)
    await expect(page.locator('[data-test-id="access-denied-message"]')).toBeVisible();
    await expect(page.locator('[data-test-id="access-denied-message"]')).toContainText(
      'You do not have permission'
    );

    // Verify redirected back to safe page
    await page.waitForURL(/\/(patients|dashboard|unauthorized)/);
  });

  test('should view care gaps in read-only mode', async ({ page }) => {
    // Navigate to care gaps
    await page.click('[data-test-id="nav-care-gaps"]');
    await page.waitForURL('/care-gaps');

    // Verify care gaps page accessible
    await expect(page.locator('[data-test-id="care-gaps-page"]')).toBeVisible();

    // Verify care gap list displayed
    const careGapRows = page.locator('[data-test-id="care-gap-row"]');
    await expect(careGapRows.first()).toBeVisible();

    // Click on first care gap to view details
    await careGapRows.first().click();

    // Verify care gap details displayed
    await expect(page.locator('[data-test-id="care-gap-title"]')).toBeVisible();
    await expect(page.locator('[data-test-id="care-gap-status"]')).toBeVisible();
    await expect(page.locator('[data-test-id="care-gap-patient"]')).toBeVisible();

    // Verify action buttons NOT visible (read-only)
    await expect(page.locator('[data-test-id="assign-care-gap-button"]')).not.toBeVisible();
    await expect(page.locator('[data-test-id="close-care-gap-button"]')).not.toBeVisible();
    await expect(page.locator('[data-test-id="update-status-button"]')).not.toBeVisible();

    // Verify can view care gap history
    await page.click('[data-test-id="tab-history"]');
    await expect(page.locator('[data-test-id="care-gap-history"]')).toBeVisible();

    // Verify can export care gaps list
    await page.goto('/care-gaps');
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-test-id="export-care-gaps-button"]');
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('care-gaps');
  });
});
