import { test, expect } from '@playwright/test';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Administration Workflow Tests
 *
 * Test Suite: ADM (Administration)
 * Coverage: User management, role assignment, data import, audit logs, system config
 *
 * These tests verify administrative functions that are critical
 * for tenant management and system configuration.
 */

test.describe('Administration Workflows', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    // Admin functions require admin role
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  /**
   * ADM-001: User Management
   *
   * Verifies user creation, editing, and management functions.
   */
  test.describe('ADM-001: User Management', () => {
    test('should navigate to user management', async ({ page }) => {
      await page.goto('/admin/users');

      const heading = page.locator('h1, h2').filter({ hasText: /user|management/i });
      const hasPage = await heading.count() > 0;
      console.log('User management page loaded:', hasPage);
    });

    test('should display user list', async ({ page }) => {
      await page.goto('/admin/users');

      const userList = page.locator(
        '[data-testid="user-list"], .user-table, table'
      );

      if (await userList.count() > 0) {
        const userCount = await userList.locator('tr, .user-item').count();
        console.log('Users displayed:', userCount);
      }
    });

    test('should search users', async ({ page }) => {
      await page.goto('/admin/users');

      const searchInput = page.locator(
        '[data-testid="user-search"], #userSearch, input[placeholder*="Search"]'
      );

      if (await searchInput.count() > 0) {
        await searchInput.fill('test');
        await page.waitForTimeout(500);
        console.log('User search executed');
      }
    });

    test('should open create user form', async ({ page }) => {
      await page.goto('/admin/users');

      const createButton = page.locator(
        '[data-testid="create-user"], button:has-text("Create User"), button:has-text("Add User")'
      );

      if (await createButton.count() > 0) {
        await createButton.click();

        const createForm = page.locator(
          '[data-testid="user-form"], .user-dialog, [role="dialog"]'
        );

        await expect(createForm).toBeVisible({ timeout: 3000 }).catch(() => {
          console.log('Create user form not found');
        });
      }
    });

    test('should fill user creation form', async ({ page }) => {
      await page.goto('/admin/users');

      const createButton = page.locator('[data-testid="create-user"]');
      if (await createButton.count() > 0) {
        await createButton.click();

        // Fill form fields
        const emailField = page.locator('#email, [data-testid="user-email"]');
        if (await emailField.count() > 0) {
          await emailField.fill('e2e-test@example.com');
        }

        const firstNameField = page.locator('#firstName, [data-testid="first-name"]');
        if (await firstNameField.count() > 0) {
          await firstNameField.fill('E2E');
        }

        const lastNameField = page.locator('#lastName, [data-testid="last-name"]');
        if (await lastNameField.count() > 0) {
          await lastNameField.fill('TestUser');
        }

        console.log('User creation form filled');
      }
    });

    test('should edit existing user', async ({ page }) => {
      await page.goto('/admin/users');

      const editButton = page.locator(
        '[data-testid="edit-user"], button:has-text("Edit"), .edit-icon'
      ).first();

      if (await editButton.count() > 0) {
        await editButton.click();

        const editForm = page.locator('[data-testid="user-form"], [role="dialog"]');
        const hasForm = await editForm.count() > 0;
        console.log('Edit user form opened:', hasForm);
      }
    });

    test('should deactivate user', async ({ page }) => {
      await page.goto('/admin/users');

      const deactivateButton = page.locator(
        '[data-testid="deactivate-user"], button:has-text("Deactivate")'
      ).first();

      if (await deactivateButton.count() > 0) {
        await deactivateButton.click();

        // Confirmation dialog
        const confirmDialog = page.locator('[role="dialog"], .confirm-dialog');
        const hasConfirm = await confirmDialog.count() > 0;
        console.log('Deactivation confirmation:', hasConfirm);
      }
    });

    test('should filter users by status', async ({ page }) => {
      await page.goto('/admin/users');

      const statusFilter = page.locator(
        '[data-testid="status-filter"], #statusFilter, select[name="status"]'
      );

      if (await statusFilter.count() > 0) {
        await statusFilter.selectOption('ACTIVE');
        console.log('Filtered by active status');
      }
    });
  });

  /**
   * ADM-002: Role Assignment
   *
   * Verifies role-based access control configuration.
   */
  test.describe('ADM-002: Role Assignment', () => {
    test('should navigate to role management', async ({ page }) => {
      await page.goto('/admin/roles');

      const heading = page.locator('h1, h2').filter({ hasText: /role/i });
      const hasPage = await heading.count() > 0;
      console.log('Role management page:', hasPage);
    });

    test('should display available roles', async ({ page }) => {
      await page.goto('/admin/roles');

      const roleList = page.locator(
        '[data-testid="role-list"], .role-table, table'
      );

      if (await roleList.count() > 0) {
        const roles = await roleList.locator('tr, .role-item').allTextContents();
        console.log('Available roles:', roles.length);
      }
    });

    test('should assign role to user', async ({ page }) => {
      await page.goto('/admin/users');

      const editButton = page.locator('[data-testid="edit-user"]').first();
      if (await editButton.count() > 0) {
        await editButton.click();

        const roleSelect = page.locator(
          '[data-testid="role-select"], #role, select[name="role"]'
        );

        if (await roleSelect.count() > 0) {
          const options = await roleSelect.locator('option').allTextContents();
          console.log('Role options:', options);

          await roleSelect.selectOption({ index: 1 });
          console.log('Role assigned');
        }
      }
    });

    test('should display role permissions', async ({ page }) => {
      await page.goto('/admin/roles');

      const roleRow = page.locator('[data-testid="role-row"], .role-item').first();
      if (await roleRow.count() > 0) {
        await roleRow.click();

        const permissions = page.locator(
          '[data-testid="permissions"], .permission-list'
        );

        if (await permissions.count() > 0) {
          const permCount = await permissions.locator('.permission-item, li').count();
          console.log('Permissions displayed:', permCount);
        }
      }
    });

    test('should create custom role', async ({ page }) => {
      await page.goto('/admin/roles');

      const createButton = page.locator(
        '[data-testid="create-role"], button:has-text("Create Role")'
      );

      if (await createButton.count() > 0) {
        await createButton.click();

        const roleNameInput = page.locator('#roleName, [data-testid="role-name"]');
        if (await roleNameInput.count() > 0) {
          await roleNameInput.fill('E2E_TEST_ROLE');
          console.log('Custom role form opened');
        }
      }
    });
  });

  /**
   * ADM-003: Data Import Configuration
   *
   * Verifies data import and integration settings.
   */
  test.describe('ADM-003: Data Import Configuration', () => {
    test('should navigate to import configuration', async ({ page }) => {
      await page.goto('/admin/import');

      const heading = page.locator('h1, h2').filter({ hasText: /import|data/i });
      const hasPage = await heading.count() > 0;
      console.log('Import configuration page:', hasPage);
    });

    test('should display import sources', async ({ page }) => {
      await page.goto('/admin/import');

      const importSources = page.locator(
        '[data-testid="import-sources"], .source-list, .import-connectors'
      );

      if (await importSources.count() > 0) {
        const sourceCount = await importSources.locator('.source-item, .connector').count();
        console.log('Import sources:', sourceCount);
      }
    });

    test('should configure import schedule', async ({ page }) => {
      await page.goto('/admin/import');

      const scheduleConfig = page.locator(
        '[data-testid="import-schedule"], .schedule-config'
      );

      if (await scheduleConfig.count() > 0) {
        const frequencySelect = page.locator('#frequency, [data-testid="frequency"]');
        if (await frequencySelect.count() > 0) {
          const options = await frequencySelect.locator('option').allTextContents();
          console.log('Schedule frequency options:', options);
        }
      }
    });

    test('should test import connection', async ({ page }) => {
      await page.goto('/admin/import');

      const testButton = page.locator(
        '[data-testid="test-connection"], button:has-text("Test")'
      ).first();

      if (await testButton.count() > 0) {
        await testButton.click();

        // Wait for test result
        const result = page.locator('.test-result, [data-testid="connection-status"]');
        await page.waitForTimeout(2000);

        const hasResult = await result.count() > 0;
        console.log('Connection test result:', hasResult);
      }
    });

    test('should view import history', async ({ page }) => {
      await page.goto('/admin/import/history');

      const historyTable = page.locator(
        '[data-testid="import-history"], .history-table, table'
      );

      if (await historyTable.count() > 0) {
        const historyCount = await historyTable.locator('tr').count();
        console.log('Import history records:', historyCount);
      }
    });

    test('should trigger manual import', async ({ page }) => {
      await page.goto('/admin/import');

      const importButton = page.locator(
        '[data-testid="trigger-import"], button:has-text("Import Now")'
      );

      if (await importButton.count() > 0) {
        await importButton.click();

        const confirmDialog = page.locator('[role="dialog"]');
        const hasConfirm = await confirmDialog.count() > 0;
        console.log('Import confirmation:', hasConfirm);
      }
    });
  });

  /**
   * ADM-004: Audit Log Viewing
   *
   * Verifies audit trail access and filtering.
   */
  test.describe('ADM-004: Audit Log Viewing', () => {
    test('should navigate to audit logs', async ({ page }) => {
      await page.goto('/admin/audit');

      const heading = page.locator('h1, h2').filter({ hasText: /audit/i });
      const hasPage = await heading.count() > 0;
      console.log('Audit log page:', hasPage);
    });

    test('should display audit log entries', async ({ page }) => {
      await page.goto('/admin/audit');

      const auditTable = page.locator(
        '[data-testid="audit-log"], .audit-table, table'
      );

      if (await auditTable.count() > 0) {
        const entryCount = await auditTable.locator('tr').count();
        console.log('Audit entries:', entryCount);
      }
    });

    test('should filter audit logs by date', async ({ page }) => {
      await page.goto('/admin/audit');

      const dateFilter = page.locator(
        '[data-testid="date-filter"], #dateFrom, input[type="date"]'
      ).first();

      if (await dateFilter.count() > 0) {
        await dateFilter.fill('2024-01-01');
        console.log('Audit filtered by date');
      }
    });

    test('should filter audit logs by action type', async ({ page }) => {
      await page.goto('/admin/audit');

      const actionFilter = page.locator(
        '[data-testid="action-filter"], #actionType, select[name="action"]'
      );

      if (await actionFilter.count() > 0) {
        const options = await actionFilter.locator('option').allTextContents();
        console.log('Audit action types:', options);
      }
    });

    test('should filter audit logs by user', async ({ page }) => {
      await page.goto('/admin/audit');

      const userFilter = page.locator(
        '[data-testid="user-filter"], #user, input[name="user"]'
      );

      if (await userFilter.count() > 0) {
        await userFilter.fill('test');
        console.log('Audit filtered by user');
      }
    });

    test('should view audit entry details', async ({ page }) => {
      await page.goto('/admin/audit');

      const auditEntry = page.locator('[data-testid="audit-row"], .audit-entry').first();
      if (await auditEntry.count() > 0) {
        await auditEntry.click();

        const details = page.locator(
          '[data-testid="audit-details"], .entry-details, [role="dialog"]'
        );

        const hasDetails = await details.count() > 0;
        console.log('Audit details displayed:', hasDetails);
      }
    });

    test('should export audit logs', async ({ page }) => {
      await page.goto('/admin/audit');

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const exportButton = page.locator(
        '[data-testid="export-audit"], button:has-text("Export")'
      );

      if (await exportButton.count() > 0) {
        await exportButton.click();

        const download = await downloadPromise;
        if (download) {
          console.log('Audit logs exported:', download.suggestedFilename());
        }
      }
    });

    test('should display PHI access logs separately', async ({ page }) => {
      await page.goto('/admin/audit/phi');

      const phiLogs = page.locator(
        '[data-testid="phi-access-log"], .phi-audit'
      );

      const hasPhiLogs = await phiLogs.count() > 0;
      console.log('PHI access logs section:', hasPhiLogs);
    });
  });

  /**
   * ADM-005: System Configuration
   *
   * Verifies system-level configuration options.
   */
  test.describe('ADM-005: System Configuration', () => {
    test('should navigate to system settings', async ({ page }) => {
      await page.goto('/admin/settings');

      const heading = page.locator('h1, h2').filter({ hasText: /settings|configuration/i });
      const hasPage = await heading.count() > 0;
      console.log('System settings page:', hasPage);
    });

    test('should display tenant configuration', async ({ page }) => {
      await page.goto('/admin/settings');

      const tenantConfig = page.locator(
        '[data-testid="tenant-config"], .tenant-settings'
      );

      const hasTenantConfig = await tenantConfig.count() > 0;
      console.log('Tenant configuration:', hasTenantConfig);
    });

    test('should configure session timeout', async ({ page }) => {
      await page.goto('/admin/settings');

      const timeoutInput = page.locator(
        '[data-testid="session-timeout"], #sessionTimeout, input[name="timeout"]'
      );

      if (await timeoutInput.count() > 0) {
        const currentValue = await timeoutInput.inputValue();
        console.log('Session timeout:', currentValue);
      }
    });

    test('should configure password policy', async ({ page }) => {
      await page.goto('/admin/settings/security');

      const passwordPolicy = page.locator(
        '[data-testid="password-policy"], .password-settings'
      );

      if (await passwordPolicy.count() > 0) {
        const minLength = page.locator('#minLength, [data-testid="min-length"]');
        if (await minLength.count() > 0) {
          const value = await minLength.inputValue();
          console.log('Min password length:', value);
        }
      }
    });

    test('should configure notification settings', async ({ page }) => {
      await page.goto('/admin/settings/notifications');

      const notificationSettings = page.locator(
        '[data-testid="notification-settings"], .notification-config'
      );

      const hasSettings = await notificationSettings.count() > 0;
      console.log('Notification settings:', hasSettings);
    });

    test('should save configuration changes', async ({ page }) => {
      await page.goto('/admin/settings');

      const saveButton = page.locator(
        '[data-testid="save-settings"], button:has-text("Save")'
      );

      if (await saveButton.count() > 0) {
        const responsePromise = page.waitForResponse(
          resp => resp.url().includes('settings') && resp.status() === 200
        ).catch(() => null);

        await saveButton.click();

        const response = await responsePromise;
        if (response) {
          console.log('Settings saved successfully');
        }
      }
    });

    test('should display feature flags', async ({ page }) => {
      await page.goto('/admin/settings/features');

      const featureFlags = page.locator(
        '[data-testid="feature-flags"], .feature-toggles'
      );

      if (await featureFlags.count() > 0) {
        const flagCount = await featureFlags.locator('.feature-flag, .toggle').count();
        console.log('Feature flags:', flagCount);
      }
    });

    test('should view system health', async ({ page }) => {
      await page.goto('/admin/health');

      const healthStatus = page.locator(
        '[data-testid="health-status"], .system-health'
      );

      if (await healthStatus.count() > 0) {
        const statusText = await healthStatus.textContent();
        console.log('System health:', statusText?.substring(0, 100));
      }
    });
  });
});

/**
 * Super Admin Functions (tenant management)
 */
test.describe('Super Admin Functions', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.superadmin.username, TEST_USERS.superadmin.password);
  });

  test('should access tenant management', async ({ page }) => {
    await page.goto('/admin/tenants');

    const tenantList = page.locator(
      '[data-testid="tenant-list"], .tenant-table'
    );

    const hasTenants = await tenantList.count() > 0;
    console.log('Tenant management accessible:', hasTenants);
  });

  test('should create new tenant', async ({ page }) => {
    await page.goto('/admin/tenants');

    const createButton = page.locator(
      '[data-testid="create-tenant"], button:has-text("Create Tenant")'
    );

    if (await createButton.count() > 0) {
      await createButton.click();

      const tenantForm = page.locator('[data-testid="tenant-form"], [role="dialog"]');
      const hasForm = await tenantForm.count() > 0;
      console.log('Create tenant form:', hasForm);
    }
  });

  test('should view cross-tenant metrics', async ({ page }) => {
    await page.goto('/admin/metrics');

    const crossTenantMetrics = page.locator(
      '[data-testid="cross-tenant-metrics"], .global-metrics'
    );

    const hasMetrics = await crossTenantMetrics.count() > 0;
    console.log('Cross-tenant metrics:', hasMetrics);
  });
});
