import { test as base, Page, expect } from '@playwright/test';

/**
 * Shared Authentication Fixtures for Clinical Portal E2E Tests
 *
 * This module provides reusable authentication helpers that:
 * 1. Set up authentication state via localStorage (most reliable)
 * 2. Handle demo login via UI click (for testing the login flow)
 * 3. Wait for the application to be fully ready after auth
 *
 * @tags @e2e @authentication @fixtures
 */

// Demo user data matching the login component
export const DEMO_USER = {
  id: 'demo-user-1',
  username: 'demo',
  email: 'demo@healthdata.com',
  firstName: 'Demo',
  lastName: 'User',
  fullName: 'Demo User',
  roles: [
    {
      id: 'role-admin',
      name: 'ADMIN',
      description: 'Administrator',
      permissions: [
        { id: 'perm-1', name: 'VIEW_PATIENTS', description: 'View patients' },
        { id: 'perm-2', name: 'EDIT_PATIENTS', description: 'Edit patients' },
        { id: 'perm-3', name: 'VIEW_EVALUATIONS', description: 'View evaluations' },
        { id: 'perm-4', name: 'RUN_EVALUATIONS', description: 'Run evaluations' },
        { id: 'perm-5', name: 'EXPORT_DATA', description: 'Export data' },
      ],
    },
  ],
  tenantId: 'demo-tenant',
  active: true,
};

/**
 * Set up demo authentication by directly setting localStorage
 * This is the most reliable method as it avoids UI timing issues
 *
 * @param page - Playwright page object
 * @param navigateTo - Optional URL to navigate to after auth (default: /dashboard)
 */
export async function setupDemoAuthViaStorage(
  page: Page,
  navigateTo: string = '/dashboard'
): Promise<void> {
  // First navigate to a page to establish the origin for localStorage
  await page.goto('/login', { waitUntil: 'domcontentloaded' });

  // Set auth tokens in localStorage
  const demoToken = 'demo-jwt-token-' + Date.now();
  await page.evaluate(
    ({ token, user }) => {
      localStorage.setItem('healthdata_auth_token', token);
      localStorage.setItem('healthdata_user', JSON.stringify(user));
    },
    { token: demoToken, user: DEMO_USER }
  );

  // Navigate to the target page
  await page.goto(navigateTo, { waitUntil: 'domcontentloaded' });

  // Wait for the app to recognize auth state
  await waitForAppReady(page);
}

/**
 * Set up demo authentication by clicking the Demo Login button
 * Use this when you specifically want to test the login UI flow
 *
 * @param page - Playwright page object
 * @param options - Configuration options
 */
export async function setupDemoAuthViaUI(
  page: Page,
  options: { timeout?: number; retries?: number } = {}
): Promise<void> {
  const { timeout = 30000, retries = 2 } = options;
  let lastError: Error | null = null;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      // Navigate to login page
      await page.goto('/login', { waitUntil: 'domcontentloaded', timeout: 15000 });

      // Wait for page to be interactive
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
        // networkidle might not fire, continue anyway
      });

      // Find and click the demo login button
      const demoButton = page.getByRole('button', { name: /demo login/i });

      // Wait for button to be visible and enabled
      await expect(demoButton).toBeVisible({ timeout: 5000 });
      await expect(demoButton).toBeEnabled({ timeout: 2000 });

      // Click the demo button
      await demoButton.click();

      // Wait for navigation - the demo login uses window.location.href
      // which causes a full page reload, so we need to wait for it
      await page.waitForURL('**/dashboard', { timeout, waitUntil: 'domcontentloaded' });

      // Wait for the app to be fully loaded
      await waitForAppReady(page);

      return; // Success!
    } catch (error) {
      lastError = error as Error;
      console.warn(`Demo auth attempt ${attempt + 1} failed:`, (error as Error).message);

      if (attempt < retries) {
        // Wait before retry
        await page.waitForTimeout(1000);
      }
    }
  }

  throw new Error(`Demo authentication failed after ${retries + 1} attempts: ${lastError?.message}`);
}

/**
 * Wait for the Angular application to be fully ready
 * This checks for common indicators that the app has loaded
 *
 * @param page - Playwright page object
 * @param timeout - Maximum time to wait in ms
 */
export async function waitForAppReady(page: Page, timeout: number = 10000): Promise<void> {
  const startTime = Date.now();

  // Wait for any loading overlays to disappear
  const loadingOverlay = page.locator('.loading-overlay, .mat-progress-spinner, mat-progress-spinner');
  try {
    await loadingOverlay.waitFor({ state: 'hidden', timeout: Math.min(5000, timeout) });
  } catch {
    // Loading overlay might not exist, that's fine
  }

  // Wait for main content to be visible
  const mainContent = page.locator('app-root, mat-sidenav-container, .main-content, main');
  await mainContent.first().waitFor({
    state: 'visible',
    timeout: Math.max(timeout - (Date.now() - startTime), 1000),
  });

  // Brief pause to let Angular finish rendering
  await page.waitForTimeout(300);
}

/**
 * Clear authentication state
 *
 * @param page - Playwright page object
 */
export async function clearAuth(page: Page): Promise<void> {
  await page.evaluate(() => {
    localStorage.removeItem('healthdata_auth_token');
    localStorage.removeItem('healthdata_user');
    sessionStorage.clear();
  });
}

/**
 * Check if currently authenticated
 *
 * @param page - Playwright page object
 * @returns Promise<boolean>
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    return !!localStorage.getItem('healthdata_auth_token');
  });
}

/**
 * Extended test fixture with pre-authenticated page
 * Use this for tests that require authentication but don't test the auth flow itself
 */
export const authenticatedTest = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
    await use(page);
  },
});

/**
 * Navigate to a page with authentication already set up
 * Convenience function for test setup
 *
 * @param page - Playwright page object
 * @param path - Path to navigate to (e.g., '/care-gaps', '/patients')
 */
export async function navigateAuthenticated(page: Page, path: string): Promise<void> {
  // Check if already authenticated
  const isAuth = await isAuthenticated(page);

  if (!isAuth) {
    await setupDemoAuthViaStorage(page, path);
  } else {
    await page.goto(path, { waitUntil: 'domcontentloaded' });
    await waitForAppReady(page);
  }
}

export default {
  DEMO_USER,
  setupDemoAuthViaStorage,
  setupDemoAuthViaUI,
  waitForAppReady,
  clearAuth,
  isAuthenticated,
  authenticatedTest,
  navigateAuthenticated,
};
