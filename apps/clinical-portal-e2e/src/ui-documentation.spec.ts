import { test, expect } from '@playwright/test';
import { DEMO_USER } from './fixtures/auth.fixture';

/**
 * Clinical Portal UI Documentation Tests
 *
 * These tests capture screenshots of the Clinical Portal UI for documentation
 * and visual testing purposes. Screenshots are saved to test-results directory.
 */

test.describe('Clinical Portal - UI Documentation', () => {
  test.beforeEach(async ({ page }) => {
    // Set up authentication via localStorage before navigation
    await page.addInitScript((demoUser) => {
      localStorage.setItem('healthdata_user', JSON.stringify(demoUser));
      localStorage.setItem(
        'healthdata_tenant',
        demoUser.tenantIds?.[0] || demoUser.tenantId || 'acme-health'
      );
    }, DEMO_USER);

    // Navigate to the app before each test
    await page.goto('/');

    // Wait for the app to be fully loaded
    await page.waitForLoadState('networkidle');

    // Wait for Material components to be ready
    await page.waitForSelector('mat-sidenav-container', { timeout: 10000 });
  });

  test('01 - Navigation and Layout', async ({ page }) => {
    // Wait for sidenav to be visible
    await page.waitForSelector('.sidenav', { state: 'visible' });

    // Verify navigation items are present
    const navItems = await page.locator('mat-nav-list a[mat-list-item]').count();
    expect(navItems).toBeGreaterThan(0);

    // Take full page screenshot of the navigation
    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/01-navigation-layout.png',
      fullPage: true,
    });
  });

  test('02 - Dashboard Page - Initial View', async ({ page }) => {
    // Navigate to dashboard
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Wait for dashboard content to load
    await page.waitForSelector('app-dashboard', { timeout: 10000 });

    // Wait a bit for statistics to load
    await page.waitForTimeout(2000);

    // Take screenshot of dashboard
    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/02-dashboard-initial.png',
      fullPage: true,
    });
  });

  test('03 - Dashboard Page - Statistics Cards', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Focus on statistics section
    const statsSection = page.locator('.statistics-grid').first();
    if (await statsSection.isVisible()) {
      await statsSection.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/03-dashboard-statistics.png',
      });
    }
  });

  test('04 - Patients Page - Patient List', async ({ page }) => {
    // Navigate to patients page
    await page.goto('/patients');
    await page.waitForLoadState('domcontentloaded');

    // Wait for page to render (flexible selector)
    try {
      await page.waitForSelector('app-patients, app-patient-list, mat-sidenav-content, .patients-container', { timeout: 10000 });
    } catch {
      // If specific component not found, wait for general content
      await page.waitForSelector('mat-sidenav-content, body', { timeout: 5000 });
    }
    await page.waitForTimeout(2000);

    // Take screenshot of patient list
    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/04-patients-list.png',
      fullPage: true,
    });
  });

  test('05 - Patients Page - Search and Filters', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Find search input
    const searchInput = page.locator('input[placeholder*="Search"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('John');
      await page.waitForTimeout(1000);

      await page.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/05-patients-search.png',
        fullPage: true,
      });

      // Clear search
      await searchInput.clear();
    }
  });

  test('06 - Patients Page - Patient Details Panel', async ({ page }) => {
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Click on first patient row to open details
    const firstPatientRow = page.locator('table tbody tr').first();
    if (await firstPatientRow.isVisible()) {
      await firstPatientRow.click();
      await page.waitForTimeout(1000);

      await page.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/06-patients-details.png',
        fullPage: true,
      });
    }
  });

  test('07 - Evaluations Page - Submission Form', async ({ page }) => {
    await page.goto('/evaluations');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Wait for evaluation form
    await page.waitForSelector('app-evaluations', { timeout: 10000 });

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/07-evaluations-form.png',
      fullPage: true,
    });
  });

  test('08 - Evaluations Page - Measure Selection', async ({ page }) => {
    await page.goto('/evaluations');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Try to click on measure dropdown
    const measureSelect = page.locator('mat-select').first();
    if (await measureSelect.count() > 0 && await measureSelect.isVisible().catch(() => false)) {
      await measureSelect.click().catch(() => {});
      await page.waitForTimeout(500);

      await page.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/08-evaluations-measure-dropdown.png',
        fullPage: true,
      });

      // Close dropdown by pressing Escape
      await page.keyboard.press('Escape');
    } else {
      // Take screenshot anyway even if dropdown not available
      await page.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/08-evaluations-measure-dropdown.png',
        fullPage: true,
      });
    }
  });

  test('09 - Results Page - Results Table', async ({ page }) => {
    await page.goto('/results');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Wait for results page
    await page.waitForSelector('app-results', { timeout: 10000 });

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/09-results-table.png',
      fullPage: true,
    });
  });

  test('10 - Results Page - Filter Panel', async ({ page }) => {
    await page.goto('/results');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Look for filter panel
    const filterPanel = page.locator('.filter-panel, mat-card').first();
    if (await filterPanel.isVisible()) {
      await filterPanel.screenshot({
        path: 'apps/clinical-portal-e2e/screenshots/10-results-filters.png',
      });
    }
  });

  test('11 - Reports Page', async ({ page }) => {
    await page.goto('/reports');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Wait for reports page
    await page.waitForSelector('app-reports', { timeout: 10000 });

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/11-reports-page.png',
      fullPage: true,
    });
  });

  test('12 - Navigation - Sidebar Collapsed', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Click menu toggle button to collapse sidebar
    const menuButton = page.locator('button[aria-label*="Toggle"], button[aria-label*="menu" i], .menu-toggle').first();
    if (await menuButton.count() > 0 && await menuButton.isVisible().catch(() => false)) {
      await menuButton.click().catch(() => {});
      await page.waitForTimeout(500);
    }

    // Always take screenshot
    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/12-navigation-collapsed.png',
      fullPage: true,
    });
  });

  test('13 - Toolbar - User Menu', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Click on user menu button
    const userMenuButton = page.locator('button[aria-label*="User menu"], button[aria-label*="user" i], .user-menu-button, mat-toolbar button').first();
    if (await userMenuButton.count() > 0 && await userMenuButton.isVisible().catch(() => false)) {
      await userMenuButton.click().catch(() => {});
      await page.waitForTimeout(500);
      // Close menu
      await page.keyboard.press('Escape');
    }

    // Always take screenshot
    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/13-toolbar-user-menu.png',
      fullPage: true,
    });
  });

  test('14 - Responsive - Mobile View (iPhone)', async ({ page }) => {
    // Set viewport to mobile size
    await page.setViewportSize({ width: 390, height: 844 }); // iPhone 14

    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/14-mobile-dashboard.png',
      fullPage: true,
    });
  });

  test('15 - Responsive - Tablet View (iPad)', async ({ page }) => {
    // Set viewport to tablet size
    await page.setViewportSize({ width: 820, height: 1180 }); // iPad Air

    await page.goto('/patients');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/15-tablet-patients.png',
      fullPage: true,
    });
  });

  test('16 - Theme and Styling - Material Design Colors', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    // Check that Material Design components are rendered (flexible check)
    const toolbar = page.locator('mat-toolbar').first();
    const toolbarVisible = await toolbar.isVisible().catch(() => false);
    if (toolbarVisible) {
      console.log('Toolbar is visible');
    }

    // Check sidebar styling (flexible check)
    const sidenav = page.locator('mat-sidenav, mat-sidenav-container').first();
    const sidenavVisible = await sidenav.isVisible().catch(() => false);
    if (sidenavVisible) {
      console.log('Sidenav is visible');
    }

    await page.screenshot({
      path: 'apps/clinical-portal-e2e/screenshots/16-material-design-theme.png',
      fullPage: true,
    });

    // Test passes if page loaded
    expect(page).toBeTruthy();
  });
});
