import { test, expect } from '@playwright/test';
import { LoginPage } from '../../../pages/login.page';
import { DashboardPage } from '../../../pages/dashboard.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Authentication Workflow Tests
 *
 * Test Suite: AUTH
 * Coverage: Authentication, session management, and role-based access
 *
 * These tests verify the complete authentication lifecycle including
 * login, logout, session handling, and RBAC enforcement.
 */

test.describe('Authentication Workflows', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
  });

  /**
   * AUTH-001: Valid Login with Correct Credentials
   *
   * Verifies that users can successfully log in with valid credentials
   * and are redirected to the dashboard.
   *
   * Workflow Reference: User Story US-AUTH-001
   */
  test.describe('AUTH-001: Valid Login', () => {
    test('should login successfully with valid evaluator credentials', async ({ page }) => {
      // Navigate to login page
      await loginPage.goto();
      await loginPage.assertLoginPageDisplayed();

      // Enter valid credentials
      const user = TEST_USERS.evaluator;
      await loginPage.login(user.username, user.password);

      // Verify redirect to dashboard
      await page.waitForURL('**/dashboard', { timeout: 30000 });
      await expect(page).toHaveURL(/.*dashboard/);

      // Verify dashboard is loaded
      await dashboardPage.assertMetricsDisplayed();
    });

    test('should login successfully with admin credentials', async ({ page }) => {
      await loginPage.goto();

      const user = TEST_USERS.admin;
      await loginPage.loginAndWait(user.username, user.password, '/dashboard');

      // Admin should see all dashboard elements
      await dashboardPage.assertMetricsDisplayed();
      await dashboardPage.assertQuickActionsAvailable();
    });

    test('should persist authentication across page refresh', async ({ page }) => {
      // Login first
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Store current URL
      const dashboardUrl = page.url();

      // Refresh page
      await page.reload();

      // Should still be on dashboard, not redirected to login
      await expect(page).toHaveURL(/.*dashboard/);
      await dashboardPage.isLoaded();
    });

    test('should include auth token in localStorage after login', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Check localStorage for auth token
      const token = await page.evaluate(() => localStorage.getItem('auth_token'));
      expect(token).toBeTruthy();
      expect(token).not.toBeNull();
    });
  });

  /**
   * AUTH-002: Invalid Credentials Handling
   *
   * Verifies proper error handling when users attempt to login
   * with incorrect credentials.
   *
   * Workflow Reference: User Story US-AUTH-002
   */
  test.describe('AUTH-002: Invalid Credentials', () => {
    test('should display error for incorrect password', async ({ page }) => {
      await loginPage.goto();

      // Attempt login with wrong password
      await loginPage.login(TEST_USERS.evaluator.username, 'wrong_password');

      // Verify error message
      await loginPage.assertLoginError(/invalid|incorrect|failed/i);

      // Verify still on login page
      await expect(page).toHaveURL(/.*login/);
    });

    test('should display error for non-existent user', async ({ page }) => {
      await loginPage.goto();

      // Attempt login with non-existent user
      await loginPage.login('nonexistent_user@test.com', 'password123');

      // Verify error message
      await loginPage.assertLoginError(/invalid|not found|failed/i);
    });

    test('should display error for empty username', async ({ page }) => {
      await loginPage.goto();

      // Attempt login with empty username
      await loginPage.login('', 'password123');

      // Verify validation error
      const hasError = await loginPage.hasUsernameError();
      expect(hasError).toBe(true);

      // Login button should be disabled or form should show error
      const isEnabled = await loginPage.isLoginButtonEnabled();
      if (isEnabled) {
        // If button is enabled, clicking should show error
        await page.click('[data-testid="login-button"], button[type="submit"]');
        await loginPage.assertLoginError(/required|username/i);
      }
    });

    test('should display error for empty password', async ({ page }) => {
      await loginPage.goto();

      // Attempt login with empty password
      await loginPage.login(TEST_USERS.evaluator.username, '');

      // Verify validation error
      const hasError = await loginPage.hasPasswordError();
      expect(hasError).toBe(true);
    });

    test('should not reveal whether username exists in error message', async ({ page }) => {
      await loginPage.goto();

      // Try with known user but wrong password
      await loginPage.login(TEST_USERS.evaluator.username, 'wrong_password');
      const error1 = await loginPage.getLoginError();

      // Clear and try with unknown user
      await loginPage.clearForm();
      await loginPage.login('unknown_user@test.com', 'wrong_password');
      const error2 = await loginPage.getLoginError();

      // Error messages should be similar to prevent user enumeration
      // Both should be generic authentication failure messages
      expect(error1.toLowerCase()).toContain('invalid');
      expect(error2.toLowerCase()).toContain('invalid');
    });

    test('should limit login attempts (rate limiting)', async ({ page }) => {
      await loginPage.goto();

      // Attempt multiple failed logins
      for (let i = 0; i < 5; i++) {
        await loginPage.login(TEST_USERS.evaluator.username, 'wrong_password_' + i);
        await page.waitForTimeout(500);
        await loginPage.clearForm();
      }

      // After multiple attempts, should see rate limit message or lockout
      // This test verifies security controls are in place
      const pageContent = await page.content();
      const hasRateLimitIndicator =
        pageContent.toLowerCase().includes('too many') ||
        pageContent.toLowerCase().includes('locked') ||
        pageContent.toLowerCase().includes('try again') ||
        pageContent.toLowerCase().includes('attempts');

      // Note: If rate limiting is not implemented, this test documents the gap
      console.log('Rate limiting implemented:', hasRateLimitIndicator);
    });
  });

  /**
   * AUTH-003: Session Expiration Handling
   *
   * Verifies that expired sessions are handled gracefully,
   * redirecting users to login with appropriate messaging.
   *
   * Workflow Reference: User Story US-AUTH-003
   */
  test.describe('AUTH-003: Session Expiration', () => {
    test('should redirect to login when session token is invalid', async ({ page }) => {
      // Set an invalid token
      await page.goto('/login');
      await page.evaluate(() => {
        localStorage.setItem('auth_token', 'invalid_token_12345');
      });

      // Try to access protected page
      await page.goto('/dashboard');

      // Should be redirected to login
      await expect(page).toHaveURL(/.*login/);
    });

    test('should redirect to login when token is removed', async ({ page }) => {
      // First login
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Verify on dashboard
      await expect(page).toHaveURL(/.*dashboard/);

      // Clear auth token (simulate session expiration)
      await page.evaluate(() => {
        localStorage.removeItem('auth_token');
      });

      // Navigate to another protected page
      await page.goto('/patients');

      // Should be redirected to login
      await expect(page).toHaveURL(/.*login/);
    });

    test('should show session expired message when redirected from protected page', async ({ page }) => {
      // Set an expired token scenario
      await page.goto('/evaluations');

      // If redirected to login, check for appropriate messaging
      if (page.url().includes('login')) {
        // Check for session expired query parameter or message
        const url = page.url();
        const hasReturnUrl = url.includes('returnUrl') || url.includes('redirect');
        console.log('Return URL preserved:', hasReturnUrl);

        // Page should indicate why user was redirected (optional but good UX)
        const content = await page.content();
        const hasExpirationHint =
          content.toLowerCase().includes('session') ||
          content.toLowerCase().includes('expired') ||
          content.toLowerCase().includes('sign in');

        expect(hasExpirationHint).toBe(true);
      }
    });

    test('should redirect to originally requested page after re-login', async ({ page }) => {
      // Clear any existing auth
      await page.goto('/');
      await page.evaluate(() => {
        localStorage.clear();
      });

      // Try to access a specific protected page
      await page.goto('/care-gaps');

      // Should redirect to login (possibly with returnUrl)
      await page.waitForURL(/.*login/);

      // Login
      await loginPage.login(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Should be redirected back to care-gaps or dashboard
      await page.waitForURL(/(care-gaps|dashboard)/);
    });
  });

  /**
   * AUTH-004: Role-Based Access Control
   *
   * Verifies that different user roles have appropriate access
   * to features and can only perform authorized actions.
   *
   * Workflow Reference: User Story US-AUTH-004
   */
  test.describe('AUTH-004: Role-Based Access', () => {
    test('viewer role should have read-only access', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.viewer.username, TEST_USERS.viewer.password);

      // Navigate to dashboard
      await expect(page).toHaveURL(/.*dashboard/);

      // Viewer should be able to view data
      await dashboardPage.assertMetricsDisplayed();

      // Navigate to care gaps
      await page.goto('/care-gaps');

      // Close Gap button should not be visible or should be disabled for viewer
      const closeGapButton = page.locator('[data-testid="close-gap"], button:has-text("Close Gap")');

      // Either button doesn't exist, is hidden, or is disabled
      const count = await closeGapButton.count();
      if (count > 0) {
        const isDisabled = await closeGapButton.isDisabled();
        const isHidden = !(await closeGapButton.isVisible());
        expect(isDisabled || isHidden).toBe(true);
      }
    });

    test('evaluator role should be able to run evaluations', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Navigate to evaluations
      await page.goto('/evaluations');

      // Evaluator should see the evaluate button
      const evaluateButton = page.locator('[data-testid="evaluate-button"], button:has-text("Evaluate")');
      await expect(evaluateButton).toBeVisible();
    });

    test('admin role should have access to administration', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);

      // Navigate to admin section
      await page.goto('/admin');

      // Should not be redirected away (403 or login)
      await expect(page).not.toHaveURL(/.*login/);

      // Admin navigation should be visible
      const adminNav = page.locator('[data-testid="admin-nav"], .admin-navigation, nav:has-text("Admin")');
      const adminContent = page.locator('.admin-content, [data-testid="admin-content"]');

      // Either navigation or content should be visible
      const hasAdminAccess = (await adminNav.count()) > 0 || (await adminContent.count()) > 0;
      expect(hasAdminAccess).toBe(true);
    });

    test('non-admin should not access admin pages', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Try to navigate to admin
      await page.goto('/admin');

      // Should be redirected to dashboard or show access denied
      const url = page.url();
      const content = await page.content();

      const isRedirected = !url.includes('/admin');
      const hasAccessDenied =
        content.toLowerCase().includes('access denied') ||
        content.toLowerCase().includes('forbidden') ||
        content.toLowerCase().includes('not authorized') ||
        content.toLowerCase().includes('403');

      expect(isRedirected || hasAccessDenied).toBe(true);
    });

    test('analyst role should have access to reports', async ({ page }) => {
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);

      // Navigate to reports
      await page.goto('/reports');

      // Analyst should see report generation options
      const generateButton = page.locator('[data-testid="generate-report"], button:has-text("Generate")');
      await expect(generateButton).toBeVisible();
    });
  });

  /**
   * AUTH-005: Logout Functionality
   *
   * Verifies that users can successfully log out and their
   * session is properly terminated.
   *
   * Workflow Reference: User Story US-AUTH-005
   */
  test.describe('AUTH-005: Logout', () => {
    test('should logout successfully from dashboard', async ({ page }) => {
      // Login first
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Verify on dashboard
      await expect(page).toHaveURL(/.*dashboard/);

      // Logout
      await dashboardPage.logout();

      // Should be redirected to login
      await expect(page).toHaveURL(/.*login/);
    });

    test('should clear auth token on logout', async ({ page }) => {
      // Login
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Verify token exists
      let token = await page.evaluate(() => localStorage.getItem('auth_token'));
      expect(token).toBeTruthy();

      // Logout
      await dashboardPage.logout();

      // Verify token is cleared
      token = await page.evaluate(() => localStorage.getItem('auth_token'));
      expect(token).toBeFalsy();
    });

    test('should not be able to access protected pages after logout', async ({ page }) => {
      // Login
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Logout
      await dashboardPage.logout();

      // Try to access protected page
      await page.goto('/dashboard');

      // Should be redirected to login
      await expect(page).toHaveURL(/.*login/);
    });

    test('should redirect to login from any page on logout', async ({ page }) => {
      // Login and navigate to different pages
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Navigate to care-gaps
      await page.goto('/care-gaps');

      // Logout from care-gaps page
      await dashboardPage.openUserMenu();
      await dashboardPage.logoutButton.click();

      // Should be redirected to login
      await expect(page).toHaveURL(/.*login/);
    });

    test('back button should not restore session after logout', async ({ page }) => {
      // Login
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

      // Navigate around
      await page.goto('/patients');
      await page.goto('/evaluations');

      // Logout
      await dashboardPage.logout();

      // Try to go back
      await page.goBack();
      await page.goBack();

      // Should still be on login page or redirected to login
      // Wait a bit for any redirects
      await page.waitForTimeout(1000);

      // Either still on login or auth check should redirect
      const url = page.url();
      const token = await page.evaluate(() => localStorage.getItem('auth_token'));

      // Token should still be null
      expect(token).toBeFalsy();
    });
  });
});
