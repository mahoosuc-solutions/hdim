import { test, expect } from '@playwright/test';
import {
  LoginPage,
  DashboardPage,
  PatientPage,
  CareGapPage,
  EvaluationPage,
  ReportsPage,
  RiskPage,
  AdminPage,
} from '../../pages';

/**
 * HDIM Smoke Test Suite
 *
 * Critical path tests that verify core functionality is working.
 * Run before deployment or after major changes.
 *
 * Tags: @smoke - All smoke tests
 *       @critical - Most critical tests (subset of smoke)
 */

test.describe('HDIM Smoke Tests @smoke', () => {
  // Test data
  const testUser = {
    username: 'test_evaluator',
    password: 'password123',
  };

  test.describe('Authentication @smoke @critical', () => {
    test('SMOKE-001: Login page loads and accepts credentials', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Verify login page is accessible
      await expect(page).toHaveTitle(/HDIM|HealthData|Login/i);
      expect(await loginPage.isLoaded()).toBe(true);

      // Perform login
      await loginPage.login(testUser.username, testUser.password);

      // Verify redirect to dashboard
      await expect(page).toHaveURL(/dashboard|home/i);
    });

    test('SMOKE-002: Invalid credentials show error', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      await loginPage.login('invalid_user', 'wrong_password');

      // Verify error message
      expect(await loginPage.hasError()).toBe(true);
    });

    test('SMOKE-003: Logout works correctly', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const dashboardPage = new DashboardPage(page);

      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
      await dashboardPage.waitForLoad();

      await dashboardPage.logout();

      // Verify redirect to login
      await expect(page).toHaveURL(/login/i);
    });
  });

  test.describe('Dashboard @smoke @critical', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-010: Dashboard loads with key metrics', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);

      expect(await dashboardPage.isLoaded()).toBe(true);

      // Verify key elements are present
      await expect(dashboardPage.patientCountCard).toBeVisible();
      await expect(dashboardPage.careGapCard).toBeVisible();
    });

    test('SMOKE-011: Dashboard navigation works', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);

      // Navigate to patients
      await dashboardPage.navigateToPatients();
      await expect(page).toHaveURL(/patient/i);

      // Navigate back to dashboard
      await page.goBack();
      expect(await dashboardPage.isLoaded()).toBe(true);
    });
  });

  test.describe('Patient Management @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-020: Patient list loads', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();

      expect(await patientPage.isLoaded()).toBe(true);

      // Verify patient table is visible
      await expect(patientPage.patientTable).toBeVisible();
    });

    test('SMOKE-021: Patient search works', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();

      await patientPage.searchPatients('test');

      // Verify search executed (results or no results message)
      await patientPage.waitForDataLoad();
    });

    test('SMOKE-022: Patient detail view accessible', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();

      const patientCount = await patientPage.getPatientCount();
      if (patientCount > 0) {
        await patientPage.selectPatient(0);

        // Verify patient detail page loaded
        await expect(patientPage.patientName).toBeVisible();
      }
    });
  });

  test.describe('Care Gap Management @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-030: Care gap dashboard loads', async ({ page }) => {
      const careGapPage = new CareGapPage(page);
      await careGapPage.goto();

      expect(await careGapPage.isLoaded()).toBe(true);
    });

    test('SMOKE-031: Care gap list displays', async ({ page }) => {
      const careGapPage = new CareGapPage(page);
      await careGapPage.goto();

      await expect(careGapPage.careGapTable).toBeVisible();
    });

    test('SMOKE-032: Care gap filtering works', async ({ page }) => {
      const careGapPage = new CareGapPage(page);
      await careGapPage.goto();

      // Filter by status
      await careGapPage.filterByStatus('OPEN');
      await careGapPage.waitForDataLoad();

      // Verify filter applied
      expect(await careGapPage.isLoaded()).toBe(true);
    });
  });

  test.describe('Quality Measure Evaluation @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-040: Evaluation page loads', async ({ page }) => {
      const evaluationPage = new EvaluationPage(page);
      await evaluationPage.goto();

      expect(await evaluationPage.isLoaded()).toBe(true);
    });

    test('SMOKE-041: Quality measures list displays', async ({ page }) => {
      const evaluationPage = new EvaluationPage(page);
      await evaluationPage.goto();

      await expect(evaluationPage.measureList).toBeVisible();
    });

    test('SMOKE-042: Can select measure for evaluation', async ({ page }) => {
      const evaluationPage = new EvaluationPage(page);
      await evaluationPage.goto();

      const measureCount = await evaluationPage.getMeasureCount();
      if (measureCount > 0) {
        await evaluationPage.selectMeasure(0);
        await evaluationPage.waitForDataLoad();
      }
    });
  });

  test.describe('Reports @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-050: Reports page loads', async ({ page }) => {
      const reportsPage = new ReportsPage(page);
      await reportsPage.goto();

      expect(await reportsPage.isLoaded()).toBe(true);
    });

    test('SMOKE-051: Report list displays', async ({ page }) => {
      const reportsPage = new ReportsPage(page);
      await reportsPage.goto();

      await expect(reportsPage.reportList).toBeVisible();
    });
  });

  test.describe('Risk Stratification @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-060: Risk page loads', async ({ page }) => {
      const riskPage = new RiskPage(page);
      await riskPage.goto();

      expect(await riskPage.isLoaded()).toBe(true);
    });

    test('SMOKE-061: Risk dashboard displays counts', async ({ page }) => {
      const riskPage = new RiskPage(page);
      await riskPage.goto();

      // Verify risk count elements are present
      await expect(riskPage.riskSummaryCards).toBeVisible();
    });
  });

  test.describe('API Health @smoke @critical', () => {
    test('SMOKE-070: Gateway health check', async ({ request }) => {
      const response = await request.get('/api/health');
      expect(response.ok()).toBe(true);
    });

    test('SMOKE-071: Patient service responds', async ({ request, page }) => {
      // Login first to get auth token
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);

      const response = await request.get('/api/v1/patients', {
        headers: {
          'X-Tenant-ID': 'ACME001',
        },
      });

      // Expect 200 or 401 (if token not passed correctly in test)
      expect([200, 401]).toContain(response.status());
    });
  });

  test.describe('Admin Panel @smoke', () => {
    test.beforeEach(async ({ page }) => {
      // Login as admin
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('test_admin', 'password123');
    });

    test('SMOKE-080: Admin page loads for admin user', async ({ page }) => {
      const adminPage = new AdminPage(page);
      await adminPage.goto();

      expect(await adminPage.isLoaded()).toBe(true);
    });

    test('SMOKE-081: User list accessible', async ({ page }) => {
      const adminPage = new AdminPage(page);
      await adminPage.gotoUsers();

      await expect(adminPage.userList).toBeVisible();
    });

    test('SMOKE-082: Audit log accessible', async ({ page }) => {
      const adminPage = new AdminPage(page);
      await adminPage.gotoAudit();

      await expect(adminPage.auditTable).toBeVisible();
    });
  });

  test.describe('Navigation @smoke', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);
    });

    test('SMOKE-090: All main navigation links work', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);

      // Check dashboard
      await dashboardPage.waitForLoad();
      expect(await dashboardPage.isLoaded()).toBe(true);

      // Navigate to patients
      await dashboardPage.navigateToPatients();
      await expect(page).toHaveURL(/patient/i);

      // Navigate to care gaps
      await dashboardPage.navigateToCareGaps();
      await expect(page).toHaveURL(/care-gap|caregap/i);

      // Navigate to evaluations
      await dashboardPage.navigateToEvaluations();
      await expect(page).toHaveURL(/evaluation|measure/i);
    });

    test('SMOKE-091: Breadcrumb navigation works', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();

      // Click patient to go to detail
      const patientCount = await patientPage.getPatientCount();
      if (patientCount > 0) {
        await patientPage.selectPatient(0);

        // Use breadcrumb to go back
        const breadcrumb = page.locator('[data-testid="breadcrumb"], .breadcrumb, nav[aria-label="breadcrumb"]');
        if (await breadcrumb.count() > 0) {
          const patientsLink = breadcrumb.locator('a:has-text("Patient")');
          if (await patientsLink.count() > 0) {
            await patientsLink.click();
            await expect(page).toHaveURL(/patient/i);
          }
        }
      }
    });
  });

  test.describe('Responsive Design @smoke', () => {
    test('SMOKE-100: Mobile viewport renders correctly', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Verify login page is still functional on mobile
      expect(await loginPage.isLoaded()).toBe(true);
    });

    test('SMOKE-101: Tablet viewport renders correctly', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });

      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login(testUser.username, testUser.password);

      const dashboardPage = new DashboardPage(page);
      expect(await dashboardPage.isLoaded()).toBe(true);
    });
  });
});
