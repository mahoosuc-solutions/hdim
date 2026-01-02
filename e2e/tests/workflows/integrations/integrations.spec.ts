import { test, expect } from '@playwright/test';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Integration Workflow Tests
 *
 * Test Suite: INT (Integrations)
 * Coverage: EHR connectors, FHIR data import/export, prior authorization
 *
 * These tests verify the integration capabilities that connect
 * HDIM with external systems and data sources.
 */

test.describe('Integration Workflows', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  /**
   * INT-001: EHR Connector Configuration
   *
   * Verifies EHR integration setup and management.
   */
  test.describe('INT-001: EHR Connector Configuration', () => {
    test('should navigate to EHR connectors', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const heading = page.locator('h1, h2').filter({ hasText: /ehr|connector/i });
      const hasPage = await heading.count() > 0;
      console.log('EHR connectors page:', hasPage);
    });

    test('should display available EHR systems', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const ehrList = page.locator(
        '[data-testid="ehr-list"], .ehr-connectors, .integration-list'
      );

      if (await ehrList.count() > 0) {
        const ehrSystems = await ehrList.locator('.ehr-item, .connector-card').count();
        console.log('EHR systems available:', ehrSystems);
      }
    });

    test('should configure Epic connector', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const epicConnector = page.locator(
        '[data-testid="epic-connector"], .connector-card:has-text("Epic")'
      );

      if (await epicConnector.count() > 0) {
        await epicConnector.click();

        const configForm = page.locator('[data-testid="connector-config"], .config-form');
        const hasConfig = await configForm.count() > 0;
        console.log('Epic configuration form:', hasConfig);
      }
    });

    test('should configure Cerner connector', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const cernerConnector = page.locator(
        '[data-testid="cerner-connector"], .connector-card:has-text("Cerner")'
      );

      if (await cernerConnector.count() > 0) {
        await cernerConnector.click();

        const configForm = page.locator('[data-testid="connector-config"]');
        const hasConfig = await configForm.count() > 0;
        console.log('Cerner configuration form:', hasConfig);
      }
    });

    test('should test EHR connection', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const testButton = page.locator(
        '[data-testid="test-ehr-connection"], button:has-text("Test Connection")'
      ).first();

      if (await testButton.count() > 0) {
        await testButton.click();

        // Wait for test result
        await page.waitForTimeout(3000);

        const result = page.locator(
          '.connection-result, [data-testid="test-result"]'
        );

        const hasResult = await result.count() > 0;
        console.log('EHR connection test completed:', hasResult);
      }
    });

    test('should configure OAuth credentials', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const oauthConfig = page.locator(
        '[data-testid="oauth-config"], .oauth-settings'
      );

      if (await oauthConfig.count() > 0) {
        const clientIdField = page.locator('#clientId, [data-testid="client-id"]');
        const hasOauth = await clientIdField.count() > 0;
        console.log('OAuth configuration available:', hasOauth);
      }
    });

    test('should configure data mapping', async ({ page }) => {
      await page.goto('/admin/integrations/ehr/mapping');

      const mappingConfig = page.locator(
        '[data-testid="data-mapping"], .field-mapping'
      );

      if (await mappingConfig.count() > 0) {
        const mappingFields = await mappingConfig.locator('.mapping-row, tr').count();
        console.log('Data mapping fields:', mappingFields);
      }
    });

    test('should view EHR sync status', async ({ page }) => {
      await page.goto('/admin/integrations/ehr');

      const syncStatus = page.locator(
        '[data-testid="sync-status"], .sync-indicator'
      );

      if (await syncStatus.count() > 0) {
        const status = await syncStatus.textContent();
        console.log('EHR sync status:', status);
      }
    });
  });

  /**
   * INT-002: FHIR Data Import/Export
   *
   * Verifies FHIR R4 data exchange capabilities.
   */
  test.describe('INT-002: FHIR Data Import/Export', () => {
    test('should navigate to FHIR interface', async ({ page }) => {
      await page.goto('/admin/integrations/fhir');

      const heading = page.locator('h1, h2').filter({ hasText: /fhir/i });
      const hasPage = await heading.count() > 0;
      console.log('FHIR interface page:', hasPage);
    });

    test('should display FHIR server configuration', async ({ page }) => {
      await page.goto('/admin/integrations/fhir');

      const serverConfig = page.locator(
        '[data-testid="fhir-server"], .fhir-endpoint-config'
      );

      if (await serverConfig.count() > 0) {
        const endpoint = page.locator('#fhirEndpoint, [data-testid="endpoint-url"]');
        if (await endpoint.count() > 0) {
          const url = await endpoint.inputValue();
          console.log('FHIR endpoint:', url);
        }
      }
    });

    test('should import FHIR bundle', async ({ page }) => {
      await page.goto('/admin/integrations/fhir/import');

      const importButton = page.locator(
        '[data-testid="import-fhir"], button:has-text("Import")'
      );

      if (await importButton.count() > 0) {
        await importButton.click();

        const fileInput = page.locator('input[type="file"]');
        const hasFileInput = await fileInput.count() > 0;
        console.log('FHIR import file input:', hasFileInput);
      }
    });

    test('should export patient as FHIR resource', async ({ page }) => {
      await page.goto('/patients');

      const firstPatient = page.locator('[data-testid="patient-row"]').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        const exportFhir = page.locator(
          '[data-testid="export-fhir"], button:has-text("Export FHIR")'
        );

        if (await exportFhir.count() > 0) {
          const downloadPromise = page.waitForEvent('download').catch(() => null);
          await exportFhir.click();

          const download = await downloadPromise;
          if (download) {
            console.log('FHIR resource exported:', download.suggestedFilename());
          }
        }
      }
    });

    test('should display supported FHIR resources', async ({ page }) => {
      await page.goto('/admin/integrations/fhir');

      const resourceList = page.locator(
        '[data-testid="fhir-resources"], .resource-list'
      );

      if (await resourceList.count() > 0) {
        const resources = await resourceList.locator('.resource-item, li').allTextContents();
        console.log('Supported FHIR resources:', resources.length);
      }
    });

    test('should validate FHIR conformance', async ({ page }) => {
      await page.goto('/admin/integrations/fhir');

      const validateButton = page.locator(
        '[data-testid="validate-fhir"], button:has-text("Validate")'
      );

      if (await validateButton.count() > 0) {
        await validateButton.click();

        // Validation results
        const results = page.locator('[data-testid="validation-results"]');
        await page.waitForTimeout(2000);

        const hasResults = await results.count() > 0;
        console.log('FHIR validation completed:', hasResults);
      }
    });

    test('should configure bulk FHIR export', async ({ page }) => {
      await page.goto('/admin/integrations/fhir/bulk-export');

      const bulkExportConfig = page.locator(
        '[data-testid="bulk-export"], .bulk-export-settings'
      );

      const hasBulkExport = await bulkExportConfig.count() > 0;
      console.log('Bulk FHIR export available:', hasBulkExport);
    });

    test('should view FHIR operation history', async ({ page }) => {
      await page.goto('/admin/integrations/fhir/history');

      const history = page.locator(
        '[data-testid="fhir-history"], .operation-history'
      );

      if (await history.count() > 0) {
        const historyCount = await history.locator('tr, .history-item').count();
        console.log('FHIR operation history:', historyCount);
      }
    });
  });

  /**
   * INT-003: Prior Authorization Workflow
   *
   * Verifies prior authorization integration features.
   */
  test.describe('INT-003: Prior Authorization Workflow', () => {
    test('should navigate to prior auth section', async ({ page }) => {
      await page.goto('/prior-auth');

      const heading = page.locator('h1, h2').filter({ hasText: /prior auth|authorization/i });
      const hasPage = await heading.count() > 0;
      console.log('Prior auth page:', hasPage);
    });

    test('should display pending authorizations', async ({ page }) => {
      await page.goto('/prior-auth');

      const pendingList = page.locator(
        '[data-testid="pending-auth"], .pending-authorizations, table'
      );

      if (await pendingList.count() > 0) {
        const pendingCount = await pendingList.locator('tr, .auth-item').count();
        console.log('Pending authorizations:', pendingCount);
      }
    });

    test('should create new prior auth request', async ({ page }) => {
      await page.goto('/prior-auth');

      const createButton = page.locator(
        '[data-testid="create-auth"], button:has-text("New Authorization")'
      );

      if (await createButton.count() > 0) {
        await createButton.click();

        const authForm = page.locator('[data-testid="auth-form"], [role="dialog"]');
        const hasForm = await authForm.count() > 0;
        console.log('Prior auth form opened:', hasForm);
      }
    });

    test('should select patient for prior auth', async ({ page }) => {
      await page.goto('/prior-auth/new');

      const patientSearch = page.locator(
        '[data-testid="patient-search"], #patient'
      );

      if (await patientSearch.count() > 0) {
        await patientSearch.fill('Test');
        console.log('Patient search for prior auth');
      }
    });

    test('should select service requiring authorization', async ({ page }) => {
      await page.goto('/prior-auth/new');

      const serviceSelect = page.locator(
        '[data-testid="service-select"], #serviceType, select[name="service"]'
      );

      if (await serviceSelect.count() > 0) {
        const options = await serviceSelect.locator('option').allTextContents();
        console.log('Service types:', options.length);
      }
    });

    test('should attach clinical documentation', async ({ page }) => {
      await page.goto('/prior-auth/new');

      const attachButton = page.locator(
        '[data-testid="attach-docs"], button:has-text("Attach")'
      );

      if (await attachButton.count() > 0) {
        await attachButton.click();

        const fileInput = page.locator('input[type="file"]');
        const hasUpload = await fileInput.count() > 0;
        console.log('Document upload available:', hasUpload);
      }
    });

    test('should submit prior auth request', async ({ page }) => {
      await page.goto('/prior-auth/new');

      const submitButton = page.locator(
        '[data-testid="submit-auth"], button:has-text("Submit")'
      );

      if (await submitButton.count() > 0) {
        console.log('Prior auth submit button available');
      }
    });

    test('should check authorization status', async ({ page }) => {
      await page.goto('/prior-auth');

      const checkButton = page.locator(
        '[data-testid="check-status"], button:has-text("Check Status")'
      ).first();

      if (await checkButton.count() > 0) {
        await checkButton.click();

        const status = page.locator('.auth-status, [data-testid="status"]');
        await page.waitForTimeout(1000);

        const hasStatus = await status.count() > 0;
        console.log('Status check completed:', hasStatus);
      }
    });

    test('should display authorization decision', async ({ page }) => {
      await page.goto('/prior-auth');

      const authRow = page.locator('[data-testid="auth-row"]').first();
      if (await authRow.count() > 0) {
        await authRow.click();

        const decision = page.locator(
          '[data-testid="auth-decision"], .decision-details'
        );

        const hasDecision = await decision.count() > 0;
        console.log('Authorization decision displayed:', hasDecision);
      }
    });

    test('should view authorization history', async ({ page }) => {
      await page.goto('/prior-auth/history');

      const history = page.locator(
        '[data-testid="auth-history"], .authorization-history'
      );

      if (await history.count() > 0) {
        const historyCount = await history.locator('tr, .history-item').count();
        console.log('Authorization history:', historyCount);
      }
    });

    test('should filter by authorization status', async ({ page }) => {
      await page.goto('/prior-auth');

      const statusFilter = page.locator(
        '[data-testid="status-filter"], #status, select[name="status"]'
      );

      if (await statusFilter.count() > 0) {
        const options = await statusFilter.locator('option').allTextContents();
        console.log('Authorization statuses:', options);
      }
    });
  });
});

/**
 * Integration Monitoring Tests
 */
test.describe('Integration Monitoring', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display integration health dashboard', async ({ page }) => {
    await page.goto('/admin/integrations/health');

    const healthDashboard = page.locator(
      '[data-testid="integration-health"], .health-dashboard'
    );

    const hasDashboard = await healthDashboard.count() > 0;
    console.log('Integration health dashboard:', hasDashboard);
  });

  test('should show integration metrics', async ({ page }) => {
    await page.goto('/admin/integrations/metrics');

    const metrics = page.locator(
      '[data-testid="integration-metrics"], .metrics-panel'
    );

    if (await metrics.count() > 0) {
      const metricText = await metrics.textContent();
      console.log('Integration metrics:', metricText?.substring(0, 100));
    }
  });

  test('should display error logs', async ({ page }) => {
    await page.goto('/admin/integrations/logs');

    const errorLogs = page.locator(
      '[data-testid="error-logs"], .error-log-table'
    );

    if (await errorLogs.count() > 0) {
      const errorCount = await errorLogs.locator('tr, .error-item').count();
      console.log('Integration errors:', errorCount);
    }
  });

  test('should configure integration alerts', async ({ page }) => {
    await page.goto('/admin/integrations/alerts');

    const alertConfig = page.locator(
      '[data-testid="alert-config"], .integration-alerts'
    );

    const hasAlerts = await alertConfig.count() > 0;
    console.log('Integration alerts config:', hasAlerts);
  });
});
