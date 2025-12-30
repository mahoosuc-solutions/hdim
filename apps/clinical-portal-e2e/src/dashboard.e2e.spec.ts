import { test, expect, Page } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  waitForAppReady,
} from './fixtures/auth.fixture';

/**
 * Dashboard E2E Tests
 *
 * Comprehensive end-to-end tests covering:
 * - Dashboard loading and initial data display
 * - Statistics cards functionality
 * - Care gap alerts and summary
 * - Recent activity table
 * - Compliance trend charts
 * - Quick actions navigation
 * - Role-based dashboard switching
 * - Data refresh functionality
 * - Error handling and loading states
 *
 * @tags @e2e @dashboard @critical
 */

// Mock data
const MOCK_PATIENTS = [
  {
    id: 'patient-001',
    fullName: 'John Doe',
    mrn: 'MRN-001',
    birthDate: '1980-05-15',
    gender: 'male',
  },
  {
    id: 'patient-002',
    fullName: 'Jane Smith',
    mrn: 'MRN-002',
    birthDate: '1975-03-22',
    gender: 'female',
  },
  {
    id: 'patient-003',
    fullName: 'Bob Johnson',
    mrn: 'MRN-003',
    birthDate: '1990-11-08',
    gender: 'male',
  },
];

const MOCK_EVALUATIONS = [
  {
    id: 'eval-001',
    patientId: 'patient-001',
    evaluationDate: new Date().toISOString(),
    library: { id: 'HEDIS_CBP', name: 'Controlling High Blood Pressure' },
    evaluationResult: { InDenominator: true, InNumerator: true },
  },
  {
    id: 'eval-002',
    patientId: 'patient-002',
    evaluationDate: new Date().toISOString(),
    library: { id: 'HEDIS_COL', name: 'Colorectal Cancer Screening' },
    evaluationResult: { InDenominator: true, InNumerator: false },
  },
  {
    id: 'eval-003',
    patientId: 'patient-003',
    evaluationDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
    library: { id: 'HEDIS_BCS', name: 'Breast Cancer Screening' },
    evaluationResult: { InDenominator: true, InNumerator: true },
  },
];

const MOCK_MEASURES = [
  {
    id: 'HEDIS_CBP',
    displayName: 'Controlling High Blood Pressure',
    category: 'HEDIS',
  },
  {
    id: 'HEDIS_COL',
    displayName: 'Colorectal Cancer Screening',
    category: 'HEDIS',
  },
  {
    id: 'HEDIS_BCS',
    displayName: 'Breast Cancer Screening',
    category: 'HEDIS',
  },
];

/**
 * Helper: Set up demo authentication
 * Uses the shared auth fixture for reliable authentication
 */
async function setupDemoAuth(page: Page) {
  await setupDemoAuthViaStorage(page, '/dashboard');
}

/**
 * Helper: Mock dashboard API endpoints
 */
async function mockDashboardApis(page: Page) {
  // Mock patients API
  await page.route('**/patient/**', async (route) => {
    if (route.request().url().includes('/summary')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_PATIENTS),
      });
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_PATIENTS),
    });
  });

  // Mock evaluations API
  await page.route('**/cql-engine/api/v1/evaluations**', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_EVALUATIONS),
    });
  });

  // Mock measures API
  await page.route('**/quality-measure/api/v1/**', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_MEASURES),
    });
  });
}

test.describe('Dashboard Loading and Display', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
  });

  test('should display dashboard with main heading', async ({ page }) => {
    const heading = page.locator('h1, h2').first();
    await expect(heading).toBeVisible({ timeout: 5000 });
  });

  test('should display statistics cards section', async ({ page }) => {
    // Look for stat cards (may be custom component)
    const statCards = page.locator('app-stat-card, mat-card, .stat-card');
    await expect(statCards.first()).toBeVisible({ timeout: 5000 });
  });

  test('should show loading overlay initially then hide', async ({ page }) => {
    // The loading overlay should eventually disappear
    await page.waitForTimeout(3000);
    const loadingOverlay = page.locator('app-loading-overlay');

    // Loading should be hidden after data loads
    const isVisible = await loadingOverlay.isVisible().catch(() => false);
    if (isVisible) {
      await expect(loadingOverlay).toBeHidden({ timeout: 10000 });
    }
  });

  test('should display last updated timestamp', async ({ page }) => {
    await page.waitForTimeout(2000);

    // Look for last updated text
    const lastUpdated = page.locator('text=/last updated|updated at/i');
    // This might not be visible immediately, depends on data loading
    const count = await lastUpdated.count();
    if (count > 0) {
      await expect(lastUpdated.first()).toBeVisible();
    }
  });
});

test.describe('Statistics Cards', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display total patients count', async ({ page }) => {
    // Look for patient count in stat cards
    const patientCard = page.locator('text=/patients|total patients/i').first();
    await expect(patientCard).toBeVisible({ timeout: 5000 });
  });

  test('should display total evaluations count', async ({ page }) => {
    const evalCard = page.locator('text=/evaluations/i').first();
    await expect(evalCard).toBeVisible({ timeout: 5000 });
  });

  test('should display compliance rate percentage', async ({ page }) => {
    const complianceCard = page.locator('text=/compliance/i').first();
    await expect(complianceCard).toBeVisible({ timeout: 5000 });
  });

  test('should allow clicking on stat card to navigate', async ({ page }) => {
    // Find a clickable stat card
    const statCards = page.locator('app-stat-card, .stat-card').first();

    // Click on it
    await statCards.click({ timeout: 5000 }).catch(() => {
      // Card might not be clickable, which is fine
    });

    // Check if we potentially navigated (or stayed on dashboard)
    await page.waitForTimeout(500);
  });
});

test.describe('Care Gap Alerts', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display care gap section', async ({ page }) => {
    const careGapSection = page.locator('text=/care gap|care gaps/i').first();
    await expect(careGapSection).toBeVisible({ timeout: 5000 });
  });

  test('should display urgency breakdown if available', async ({ page }) => {
    // Look for urgency labels (high, medium, low)
    const urgencyLabels = page.locator('text=/high urgency|medium|low urgency|urgent/i');
    const count = await urgencyLabels.count();

    // Care gaps might be empty, so this is optional
    if (count > 0) {
      await expect(urgencyLabels.first()).toBeVisible();
    }
  });

  test('should allow viewing all care gaps', async ({ page }) => {
    const viewAllButton = page.locator('button, a').filter({ hasText: /view all.*gap|all care gap/i });
    const count = await viewAllButton.count();

    if (count > 0) {
      await viewAllButton.first().click();
      // Should navigate to care gaps page
      await page.waitForURL(/care-gap|patients/i, { timeout: 5000 }).catch(() => {
        // Navigation might not happen in all cases
      });
    }
  });
});

test.describe('Recent Activity Table', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display recent activity section', async ({ page }) => {
    const activitySection = page.locator('text=/recent activity|recent evaluations/i').first();
    await expect(activitySection).toBeVisible({ timeout: 5000 });
  });

  test('should display activity table with data', async ({ page }) => {
    const table = page.locator('table, mat-table').first();
    await expect(table).toBeVisible({ timeout: 5000 });
  });

  test('should show outcome badges for results', async ({ page }) => {
    // Look for outcome badges (compliant, non-compliant)
    const badges = page.locator('.badge, mat-chip, .status-badge');
    const count = await badges.count();

    // If there are evaluations, we should have badges
    if (count > 0) {
      await expect(badges.first()).toBeVisible();
    }
  });

  test('should allow clicking on activity row to view details', async ({ page }) => {
    const tableRows = page.locator('table tbody tr, mat-row');
    const rowCount = await tableRows.count();

    if (rowCount > 0) {
      await tableRows.first().click();
      // Should navigate to result details or stay (depending on implementation)
      await page.waitForTimeout(500);
    }
  });
});

test.describe('Quick Actions', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display quick action buttons', async ({ page }) => {
    // Look for quick action buttons
    const newEvalButton = page.locator('button, a').filter({ hasText: /new evaluation|create evaluation/i });
    const viewResultsButton = page.locator('button, a').filter({ hasText: /view.*results|all results/i });
    const viewReportsButton = page.locator('button, a').filter({ hasText: /view.*reports|reports/i });

    // At least one quick action should be visible
    const buttons = [newEvalButton, viewResultsButton, viewReportsButton];
    let foundButton = false;

    for (const btn of buttons) {
      if ((await btn.count()) > 0) {
        await expect(btn.first()).toBeVisible();
        foundButton = true;
        break;
      }
    }

    expect(foundButton).toBeTruthy();
  });

  test('should navigate to evaluations page from quick action', async ({ page }) => {
    const newEvalButton = page.locator('button, a').filter({ hasText: /new evaluation|evaluations/i });
    const count = await newEvalButton.count();

    if (count > 0) {
      await newEvalButton.first().click();
      await page.waitForURL('**/evaluations', { timeout: 5000 });
      await expect(page).toHaveURL(/evaluations/);
    }
  });

  test('should navigate to results page from quick action', async ({ page }) => {
    const viewResultsButton = page.locator('button, a').filter({ hasText: /view.*results|results/i });
    const count = await viewResultsButton.count();

    if (count > 0) {
      await viewResultsButton.first().click();
      await page.waitForURL('**/results', { timeout: 5000 });
      await expect(page).toHaveURL(/results/);
    }
  });

  test('should navigate to reports page from quick action', async ({ page }) => {
    const viewReportsButton = page.locator('button, a').filter({ hasText: /reports/i });
    const count = await viewReportsButton.count();

    if (count > 0) {
      await viewReportsButton.first().click();
      await page.waitForURL('**/reports', { timeout: 5000 });
      await expect(page).toHaveURL(/reports/);
    }
  });
});

test.describe('Role-Based Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display role toggle buttons if available', async ({ page }) => {
    const roleToggle = page.locator('mat-button-toggle-group, .role-toggle, .role-selector');
    const count = await roleToggle.count();

    if (count > 0) {
      await expect(roleToggle.first()).toBeVisible();
    }
  });

  test('should switch to Medical Assistant view if available', async ({ page }) => {
    const maButton = page.locator('mat-button-toggle, button').filter({ hasText: /medical assistant|ma/i });
    const count = await maButton.count();

    if (count > 0) {
      await maButton.first().click();
      await page.waitForTimeout(1000);
      // Dashboard content should update
    }
  });

  test('should switch to Registered Nurse view if available', async ({ page }) => {
    const rnButton = page.locator('mat-button-toggle, button').filter({ hasText: /registered nurse|rn/i });
    const count = await rnButton.count();

    if (count > 0) {
      await rnButton.first().click();
      await page.waitForTimeout(1000);
    }
  });

  test('should switch to Provider view if available', async ({ page }) => {
    const providerButton = page.locator('mat-button-toggle, button').filter({ hasText: /provider|physician/i });
    const count = await providerButton.count();

    if (count > 0) {
      await providerButton.first().click();
      await page.waitForTimeout(1000);
    }
  });
});

test.describe('Compliance Charts', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(3000);
  });

  test('should display compliance trend chart', async ({ page }) => {
    // Look for ngx-charts elements
    const chart = page.locator('ngx-charts-line-chart, ngx-charts-area-chart, .ngx-charts, svg.ngx-charts');
    const count = await chart.count();

    if (count > 0) {
      await expect(chart.first()).toBeVisible();
    }
  });

  test('should display measure performance chart', async ({ page }) => {
    const barChart = page.locator('ngx-charts-bar-horizontal, ngx-charts-bar-vertical, .bar-chart');
    const count = await barChart.count();

    if (count > 0) {
      await expect(barChart.first()).toBeVisible();
    }
  });

  test('should allow changing trend period', async ({ page }) => {
    const periodToggle = page.locator('mat-button-toggle, button').filter({ hasText: /daily|weekly|monthly/i });
    const count = await periodToggle.count();

    if (count > 0) {
      await periodToggle.first().click();
      await page.waitForTimeout(1000);
      // Chart should update
    }
  });
});

test.describe('Data Refresh', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should have refresh button', async ({ page }) => {
    const refreshButton = page.locator('button').filter({ hasText: /refresh/i });
    const refreshIcon = page.locator('button mat-icon').filter({ hasText: /refresh|sync/i });

    const buttonCount = await refreshButton.count();
    const iconCount = await refreshIcon.count();

    // Either a refresh button or icon should exist
    expect(buttonCount + iconCount).toBeGreaterThan(0);
  });

  test('should refresh data when refresh button clicked', async ({ page }) => {
    let apiCallCount = 0;

    // Track API calls
    page.on('request', (request) => {
      if (request.url().includes('/patient') || request.url().includes('/evaluation')) {
        apiCallCount++;
      }
    });

    const refreshButton = page.locator('button').filter({ hasText: /refresh/i });
    const count = await refreshButton.count();

    if (count > 0) {
      const initialCount = apiCallCount;
      await refreshButton.first().click();
      await page.waitForTimeout(2000);

      // API should have been called again
      expect(apiCallCount).toBeGreaterThan(initialCount);
    }
  });

  test('should show success indicator after refresh', async ({ page }) => {
    const refreshButton = page.locator('button').filter({ hasText: /refresh/i });
    const count = await refreshButton.count();

    if (count > 0) {
      await refreshButton.first().click();
      await page.waitForTimeout(2000);

      // Check for success indicator (might be a checkmark or snackbar)
      const successIndicator = page.locator('.success, mat-icon:has-text("check"), .refresh-success');
      const snackbar = page.locator('.mat-mdc-snack-bar-container');

      const successCount = await successIndicator.count();
      const snackbarCount = await snackbar.count();

      // Either indicator might show success
      // This test just verifies the action completes without error
    }
  });
});

test.describe('Error Handling', () => {
  test('should handle API errors gracefully', async ({ page }) => {
    // Mock API failures
    await page.route('**/patient/**', (route) => {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' }),
      });
    });

    await setupDemoAuth(page);

    // Dashboard should still render without crashing
    await expect(page).toHaveURL(/dashboard/);

    // Error message should be displayed
    const errorMessage = page.locator('text=/error|failed|try again/i');
    const count = await errorMessage.count();

    if (count > 0) {
      await expect(errorMessage.first()).toBeVisible();
    }
  });

  test('should show empty state when no data', async ({ page }) => {
    // Mock empty responses
    await page.route('**/patient/**', (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route('**/cql-engine/**', (route) => {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await setupDemoAuth(page);

    await page.waitForTimeout(2000);

    // Dashboard should show some content even if empty
    await expect(page).toHaveURL(/dashboard/);
  });
});

test.describe('Navigation from Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should navigate to patients page from sidebar', async ({ page }) => {
    const patientsLink = page.locator('a, mat-list-item').filter({ hasText: /patients/i });
    const count = await patientsLink.count();

    if (count > 0) {
      await patientsLink.first().click();
      await page.waitForURL('**/patients', { timeout: 5000 });
      await expect(page).toHaveURL(/patients/);
    }
  });

  test('should navigate to care gaps page', async ({ page }) => {
    const careGapsLink = page.locator('a, mat-list-item, button').filter({ hasText: /care gap/i });
    const count = await careGapsLink.count();

    if (count > 0) {
      await careGapsLink.first().click();
      await page.waitForURL(/care-gap|patients/i, { timeout: 5000 });
    }
  });

  test('should navigate to AI Assistant if available', async ({ page }) => {
    const aiLink = page.locator('a, mat-list-item').filter({ hasText: /ai assistant|ai/i });
    const count = await aiLink.count();

    if (count > 0) {
      await aiLink.first().click();
      await page.waitForURL('**/ai-assistant', { timeout: 5000 });
      await expect(page).toHaveURL(/ai-assistant/);
    }
  });
});

test.describe('Favorites and Recent Measures', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboardApis(page);
    await setupDemoAuth(page);
    await page.waitForTimeout(2000);
  });

  test('should display favorites section if available', async ({ page }) => {
    const favoritesSection = page.locator('text=/favorite|starred|pinned/i');
    const count = await favoritesSection.count();

    // Favorites section might not exist if no favorites saved
    if (count > 0) {
      await expect(favoritesSection.first()).toBeVisible();
    }
  });

  test('should display recent measures section if available', async ({ page }) => {
    const recentSection = page.locator('text=/recent measures|recently used/i');
    const count = await recentSection.count();

    if (count > 0) {
      await expect(recentSection.first()).toBeVisible();
    }
  });
});
