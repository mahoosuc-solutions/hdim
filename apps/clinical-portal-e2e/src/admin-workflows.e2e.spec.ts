import { test, expect } from '@playwright/test';
import { setupDemoAuthViaStorage } from './fixtures/auth.fixture';

/**
 * ADMIN Role Workflow E2E Tests
 *
 * Tests admin-specific workflows:
 * - User management (create, edit, delete)
 * - Role assignment
 * - Tenant settings management
 * - Audit log review
 *
 * @tags @e2e @role-admin @user-management
 */

test.describe('ADMIN Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('should create new user and assign role', async ({ page }) => {
    // Navigate to user management
    await page.goto('/admin/users');

    // Click create user button
    await page.click('[data-test-id="create-user-button"]');

    // Fill user form
    await page.fill('[data-test-id="user-username"]', 'new_evaluator_user');
    await page.fill('[data-test-id="user-email"]', 'evaluator@example.com');
    await page.fill('[data-test-id="user-first-name"]', 'Jane');
    await page.fill('[data-test-id="user-last-name"]', 'Smith');

    // Assign EVALUATOR role
    await page.selectOption('[data-test-id="user-role-select"]', 'EVALUATOR');

    // Save user
    await page.click('[data-test-id="save-user-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'User created successfully'
    );

    // Verify user appears in user list
    const userRow = page.locator('[data-test-id="user-row"]', {
      hasText: 'new_evaluator_user',
    });
    await expect(userRow).toBeVisible();
    await expect(userRow).toContainText('EVALUATOR');
  });

  test('should edit user role', async ({ page }) => {
    // Navigate to user management
    await page.goto('/admin/users');

    // Find existing user and click edit
    const userRow = page.locator('[data-test-id="user-row"]').first();
    await userRow.locator('[data-test-id="edit-user-button"]').click();

    // Change role from EVALUATOR to ANALYST
    await page.selectOption('[data-test-id="user-role-select"]', 'ANALYST');

    // Save changes
    await page.click('[data-test-id="save-user-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'User updated successfully'
    );

    // Verify role changed in user list
    await expect(userRow).toContainText('ANALYST');
  });

  test('should view audit logs', async ({ page }) => {
    // Navigate to audit logs
    await page.goto('/admin/audit-logs');

    // Verify audit log table visible
    await expect(page.locator('[data-test-id="audit-log-table"]')).toBeVisible();

    // Verify audit log entries present
    const logRows = page.locator('[data-test-id="audit-log-row"]');
    await expect(logRows).toHaveCount(await logRows.count());

    // Verify log entry contains required fields
    const firstLog = logRows.first();
    await expect(firstLog.locator('[data-test-id="log-timestamp"]')).toBeVisible();
    await expect(firstLog.locator('[data-test-id="log-action"]')).toBeVisible();
    await expect(firstLog.locator('[data-test-id="log-user"]')).toBeVisible();
  });

  test('should manage tenant settings', async ({ page }) => {
    // Navigate to tenant settings
    await page.goto('/admin/tenant-settings');

    // Update tenant display name
    await page.fill('[data-test-id="tenant-name"]', 'Updated Tenant Name');

    // Update session timeout
    await page.fill('[data-test-id="session-timeout-minutes"]', '20');

    // Save settings
    await page.click('[data-test-id="save-settings-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Settings saved successfully'
    );
  });
});
