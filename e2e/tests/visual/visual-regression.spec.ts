import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { DashboardPage } from '../../pages/dashboard.page';
import { PatientPage } from '../../pages/patient.page';
import { CareGapsPage } from '../../pages/care-gap.page';
import { TEST_USERS } from '../../fixtures/test-fixtures';
import { maskPHIElements } from '../../utils/phi-masking';

/**
 * Visual Regression Tests
 *
 * Test Suite: VIS (Visual)
 * Coverage: Screenshot comparison for UI consistency
 *
 * These tests capture screenshots of key pages and compare them
 * against baseline images to detect unintended visual changes.
 *
 * Configuration:
 * - Uses Playwright's built-in screenshot comparison
 * - Can be integrated with Percy or Chromatic for cloud-based comparison
 *
 * Usage:
 * - First run: npx playwright test visual --update-snapshots
 * - Subsequent runs: npx playwright test visual
 */

test.describe('Visual Regression Tests', () => {
  /**
   * VIS-001: Login Page Visual
   */
  test.describe('VIS-001: Login Page', () => {
    test('should match login page baseline', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Wait for page to stabilize
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(500);

      // Take screenshot
      await expect(page).toHaveScreenshot('login-page.png', {
        fullPage: true,
        animations: 'disabled',
        mask: [
          page.locator('.dynamic-content'),
          page.locator('[data-testid="timestamp"]'),
        ],
      });
    });

    test('should match login page with error state', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();

      // Trigger error state
      await loginPage.login('invalid_user', 'wrong_password');
      await page.waitForTimeout(500);

      await expect(page).toHaveScreenshot('login-page-error.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match login page on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      const loginPage = new LoginPage(page);
      await loginPage.goto();

      await page.waitForLoadState('networkidle');

      await expect(page).toHaveScreenshot('login-page-mobile.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });
  });

  /**
   * VIS-002: Dashboard Visual
   */
  test.describe('VIS-002: Dashboard', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match dashboard baseline', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Mask dynamic content
      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('dashboard.png', {
        fullPage: true,
        animations: 'disabled',
        mask: [
          page.locator('[data-testid="timestamp"]'),
          page.locator('[data-testid="date"]'),
          page.locator('.chart-tooltip'),
        ],
      });
    });

    test('should match dashboard KPI widgets', async ({ page }) => {
      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Screenshot just the KPI section
      const kpiSection = page.locator('[data-testid="kpi-section"], .kpi-widgets, .dashboard-metrics').first();

      if (await kpiSection.count() > 0) {
        await expect(kpiSection).toHaveScreenshot('dashboard-kpis.png', {
          animations: 'disabled',
        });
      }
    });

    test('should match dashboard on tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });

      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('dashboard-tablet.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match dashboard on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      const dashboardPage = new DashboardPage(page);
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('dashboard-mobile.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });
  });

  /**
   * VIS-003: Patient List Visual
   */
  test.describe('VIS-003: Patient List', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match patient list baseline', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      // Mask PHI in patient list
      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('patient-list.png', {
        fullPage: true,
        animations: 'disabled',
        mask: [
          page.locator('[data-phi]'),
          page.locator('.patient-name'),
          page.locator('.patient-dob'),
          page.locator('.patient-mrn'),
        ],
      });
    });

    test('should match patient search results', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      // Perform search
      if (await patientPage.searchInput.count() > 0) {
        await patientPage.searchInput.fill('Test');
        await page.waitForTimeout(500);
      }

      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('patient-search-results.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match patient detail view', async ({ page }) => {
      const patientPage = new PatientPage(page);
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      // Click first patient
      const firstPatient = page.locator('[data-testid="patient-row"]').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();
        await page.waitForTimeout(500);

        await maskPHIElements(page);

        await expect(page).toHaveScreenshot('patient-detail.png', {
          fullPage: true,
          animations: 'disabled',
        });
      }
    });
  });

  /**
   * VIS-004: Care Gaps Visual
   */
  test.describe('VIS-004: Care Gaps', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match care gaps list baseline', async ({ page }) => {
      const careGapsPage = new CareGapsPage(page);
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('care-gaps-list.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match care gaps filtered by urgency', async ({ page }) => {
      const careGapsPage = new CareGapsPage(page);
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      // Filter by HIGH urgency
      if (await careGapsPage.urgencyFilter.count() > 0) {
        await careGapsPage.urgencyFilter.click();
        await page.locator('[role="option"]:has-text("High")').click();
        await page.waitForTimeout(500);
      }

      await maskPHIElements(page);

      await expect(page).toHaveScreenshot('care-gaps-high-urgency.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match care gap detail modal', async ({ page }) => {
      const careGapsPage = new CareGapsPage(page);
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      // Click first gap
      const firstGap = page.locator('[data-testid="care-gap-row"]').first();
      if (await firstGap.count() > 0) {
        await firstGap.click();
        await page.waitForTimeout(500);

        await maskPHIElements(page);

        // Screenshot the modal/detail view
        const modal = page.locator('[role="dialog"], .gap-detail');
        if (await modal.count() > 0) {
          await expect(modal).toHaveScreenshot('care-gap-detail-modal.png', {
            animations: 'disabled',
          });
        }
      }
    });
  });

  /**
   * VIS-005: Reports Visual
   */
  test.describe('VIS-005: Reports', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);
    });

    test('should match reports page baseline', async ({ page }) => {
      await page.goto('/reports');
      await page.waitForLoadState('networkidle');

      await expect(page).toHaveScreenshot('reports-page.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should match population report view', async ({ page }) => {
      await page.goto('/reports/population');
      await page.waitForLoadState('networkidle');

      await expect(page).toHaveScreenshot('population-report.png', {
        fullPage: true,
        animations: 'disabled',
        mask: [
          page.locator('[data-testid="date"]'),
          page.locator('.timestamp'),
        ],
      });
    });
  });

  /**
   * VIS-006: Navigation Visual
   */
  test.describe('VIS-006: Navigation', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match header navigation', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const header = page.locator('header, [role="banner"], .app-header').first();
      if (await header.count() > 0) {
        await expect(header).toHaveScreenshot('header-navigation.png', {
          animations: 'disabled',
        });
      }
    });

    test('should match sidebar navigation', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const sidebar = page.locator('nav, [role="navigation"], .sidebar').first();
      if (await sidebar.count() > 0) {
        await expect(sidebar).toHaveScreenshot('sidebar-navigation.png', {
          animations: 'disabled',
        });
      }
    });

    test('should match user menu dropdown', async ({ page }) => {
      await page.goto('/dashboard');

      const userMenu = page.locator('[data-testid="user-menu"], .user-avatar, .profile-icon').first();
      if (await userMenu.count() > 0) {
        await userMenu.click();
        await page.waitForTimeout(300);

        const dropdown = page.locator('.user-dropdown, [role="menu"]');
        if (await dropdown.count() > 0) {
          await expect(dropdown).toHaveScreenshot('user-menu-dropdown.png', {
            animations: 'disabled',
          });
        }
      }
    });
  });

  /**
   * VIS-007: Forms Visual
   */
  test.describe('VIS-007: Forms', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match evaluation form', async ({ page }) => {
      await page.goto('/evaluations');
      await page.waitForLoadState('networkidle');

      const form = page.locator('form, [data-testid="evaluation-form"]').first();
      if (await form.count() > 0) {
        await expect(form).toHaveScreenshot('evaluation-form.png', {
          animations: 'disabled',
        });
      }
    });

    test('should match form validation errors', async ({ page }) => {
      await page.goto('/evaluations');

      const submitButton = page.locator('button[type="submit"]').first();
      if (await submitButton.count() > 0) {
        await submitButton.click();
        await page.waitForTimeout(300);

        await expect(page).toHaveScreenshot('form-validation-errors.png', {
          fullPage: true,
          animations: 'disabled',
        });
      }
    });
  });

  /**
   * VIS-008: Alerts and Notifications Visual
   */
  test.describe('VIS-008: Alerts and Notifications', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
    });

    test('should match alert panel', async ({ page }) => {
      await page.goto('/dashboard');

      const alertBell = page.locator('[data-testid="alert-bell"], .notification-icon').first();
      if (await alertBell.count() > 0) {
        await alertBell.click();
        await page.waitForTimeout(300);

        const alertPanel = page.locator('.alert-dropdown, .notification-panel');
        if (await alertPanel.count() > 0) {
          await expect(alertPanel).toHaveScreenshot('alert-panel.png', {
            animations: 'disabled',
          });
        }
      }
    });

    test('should match toast notifications', async ({ page }) => {
      await page.goto('/care-gaps');

      // Trigger an action that shows toast
      const closeButton = page.locator('[data-testid="close-gap"]').first();
      if (await closeButton.count() > 0) {
        await closeButton.click();

        // Look for toast
        const toast = page.locator('.toast, .snackbar, [role="alert"]').first();
        if (await toast.count() > 0) {
          await expect(toast).toHaveScreenshot('toast-notification.png', {
            animations: 'disabled',
          });
        }
      }
    });
  });
});

/**
 * Component Visual Tests
 */
test.describe('Component Visual Tests', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test('should match data table component', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    const table = page.locator('table, [role="grid"]').first();
    if (await table.count() > 0) {
      await maskPHIElements(page);

      await expect(table).toHaveScreenshot('data-table-component.png', {
        animations: 'disabled',
      });
    }
  });

  test('should match pagination component', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    const pagination = page.locator('.pagination, .mat-paginator, [data-testid="pagination"]').first();
    if (await pagination.count() > 0) {
      await expect(pagination).toHaveScreenshot('pagination-component.png', {
        animations: 'disabled',
      });
    }
  });

  test('should match filter controls', async ({ page }) => {
    await page.goto('/care-gaps');
    await page.waitForLoadState('networkidle');

    const filters = page.locator('.filter-section, [data-testid="filters"]').first();
    if (await filters.count() > 0) {
      await expect(filters).toHaveScreenshot('filter-controls.png', {
        animations: 'disabled',
      });
    }
  });

  test('should match chart component', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000); // Wait for charts to render

    const chart = page.locator('canvas, .chart, [data-testid="chart"]').first();
    if (await chart.count() > 0) {
      await expect(chart).toHaveScreenshot('chart-component.png', {
        animations: 'disabled',
      });
    }
  });
});

/**
 * Dark Mode Visual Tests (if supported)
 */
test.describe('Dark Mode Visual Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Set dark mode preference
    await page.emulateMedia({ colorScheme: 'dark' });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  test('should match dashboard in dark mode', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    await maskPHIElements(page);

    await expect(page).toHaveScreenshot('dashboard-dark-mode.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should match patient list in dark mode', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    await maskPHIElements(page);

    await expect(page).toHaveScreenshot('patient-list-dark-mode.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});
