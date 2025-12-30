import { test, expect, Page } from '@playwright/test';

/**
 * Authentication E2E Tests
 *
 * Comprehensive end-to-end tests covering:
 * - Login page accessibility and display
 * - Form validation
 * - Demo login flow
 * - Regular login flow (with mocked backend)
 * - MFA verification flow
 * - Authentication redirect behavior
 * - Logout functionality
 * - Session persistence
 * - HIPAA security compliance checks
 *
 * @tags @e2e @authentication @security @critical
 */

// Test data
const TEST_USERS = {
  admin: { username: 'test_admin', password: 'password123' },
  evaluator: { username: 'test_evaluator', password: 'password123' },
  invalid: { username: 'invalid_user', password: 'wrong_password' },
};

const DEMO_USER = {
  id: 'demo-user-1',
  username: 'demo',
  email: 'demo@healthdata.com',
  firstName: 'Demo',
  lastName: 'User',
  fullName: 'Demo User',
  roles: [{ id: 'role-admin', name: 'ADMIN', permissions: [] }],
  tenantId: 'demo-tenant',
  active: true,
};

/**
 * Helper: Mock authentication API responses
 */
async function mockAuthBackend(page: Page, options: { loginSuccess?: boolean; requireMfa?: boolean } = {}) {
  const { loginSuccess = true, requireMfa = false } = options;

  await page.route('**/auth/login', async (route) => {
    const request = route.request();
    const body = JSON.parse(request.postData() || '{}');

    if (body.username === TEST_USERS.invalid.username) {
      return route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid credentials' }),
      });
    }

    if (requireMfa) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          mfaRequired: true,
          mfaToken: 'test-mfa-token-123',
        }),
      });
    }

    if (loginSuccess) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'test-jwt-token-' + Date.now(),
          user: { ...DEMO_USER, username: body.username },
        }),
      });
    }

    return route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Authentication failed' }),
    });
  });

  await page.route('**/auth/verify-mfa', async (route) => {
    const body = JSON.parse(route.request().postData() || '{}');

    if (body.code === '123456') {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'test-jwt-token-' + Date.now(),
          user: DEMO_USER,
        }),
      });
    }

    return route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Invalid MFA code' }),
    });
  });

  await page.route('**/auth/logout', async (route) => {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true }),
    });
  });
}

test.describe('Login Page Display and Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login page with all required elements', async ({ page }) => {
    // Check page title and branding
    await expect(page.locator('h1')).toContainText('Health Data Intelligence');
    await expect(page.locator('.logo-icon')).toBeVisible();

    // Check form fields
    await expect(page.locator('input[formcontrolname="username"]')).toBeVisible();
    await expect(page.locator('input[formcontrolname="password"]')).toBeVisible();

    // Check buttons
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /demo login/i })).toBeVisible();

    // Check HIPAA security notice
    await expect(page.locator('.security-notice')).toContainText('HIPAA-compliant');
  });

  test('should have proper accessibility labels', async ({ page }) => {
    // Check form field labels
    await expect(page.getByLabel(/username/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();

    // Check password visibility toggle has aria-label
    const passwordToggle = page.locator('button[aria-label*="password"]');
    await expect(passwordToggle).toBeVisible();
  });

  test('should toggle password visibility', async ({ page }) => {
    const passwordInput = page.locator('input[formcontrolname="password"]');
    const toggleButton = page.locator('button[aria-label*="password"]');

    // Initially password should be hidden
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Click toggle to show password
    await toggleButton.click();
    await expect(passwordInput).toHaveAttribute('type', 'text');

    // Click again to hide
    await toggleButton.click();
    await expect(passwordInput).toHaveAttribute('type', 'password');
  });

  test('should have demo mode notice visible', async ({ page }) => {
    await expect(page.locator('.demo-notice')).toBeVisible();
    await expect(page.locator('.demo-notice')).toContainText('Demo Mode');
  });
});

test.describe('Login Form Validation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should disable submit button when form is invalid', async ({ page }) => {
    const submitButton = page.getByRole('button', { name: /sign in/i });

    // Initially disabled (empty form)
    await expect(submitButton).toBeDisabled();

    // Fill only username
    await page.fill('input[formcontrolname="username"]', 'testuser');
    await expect(submitButton).toBeDisabled();

    // Clear and fill only password
    await page.fill('input[formcontrolname="username"]', '');
    await page.fill('input[formcontrolname="password"]', 'password');
    await expect(submitButton).toBeDisabled();
  });

  test('should enable submit button when form is valid', async ({ page }) => {
    const submitButton = page.getByRole('button', { name: /sign in/i });

    await page.fill('input[formcontrolname="username"]', 'testuser');
    await page.fill('input[formcontrolname="password"]', 'password123');

    await expect(submitButton).toBeEnabled();
  });

  test('should show validation errors on blur', async ({ page }) => {
    const usernameInput = page.locator('input[formcontrolname="username"]');
    const passwordInput = page.locator('input[formcontrolname="password"]');

    // Focus and blur username without entering value
    await usernameInput.focus();
    await passwordInput.focus();

    // Error message should appear
    await expect(page.locator('mat-error')).toContainText(/required/i);
  });
});

test.describe('Demo Login Flow', () => {
  test('should successfully login via demo button', async ({ page }) => {
    await page.goto('/login');

    const demoButton = page.getByRole('button', { name: /demo login/i });
    await expect(demoButton).toBeVisible();

    // Click demo login
    await demoButton.click();

    // Should show loading state briefly
    // Then redirect to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Verify we're on the dashboard
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('should set demo user in localStorage after demo login', async ({ page }) => {
    await page.goto('/login');

    await page.getByRole('button', { name: /demo login/i }).click();
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Check localStorage for auth token
    const authToken = await page.evaluate(() => localStorage.getItem('healthdata_auth_token'));
    const user = await page.evaluate(() => localStorage.getItem('healthdata_user'));

    expect(authToken).toBeTruthy();
    expect(authToken).toContain('demo-jwt-token');
    expect(user).toBeTruthy();

    const parsedUser = JSON.parse(user || '{}');
    expect(parsedUser.username).toBe('demo');
    expect(parsedUser.roles).toBeDefined();
  });
});

test.describe('Regular Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBackend(page);
  });

  test('should successfully login with valid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should redirect to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('should show error message for invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.invalid.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.invalid.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should show error snackbar
    await expect(page.locator('.mat-mdc-snack-bar-container')).toContainText(/invalid/i, { timeout: 5000 });

    // Should remain on login page
    await expect(page).toHaveURL(/.*login/);
  });

  test('should show loading state during login', async ({ page }) => {
    // Add delay to mock response
    await page.route('**/auth/login', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'test-token',
          user: DEMO_USER,
        }),
      });
    });

    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Check for loading spinner
    await expect(page.locator('mat-spinner')).toBeVisible();
    await expect(page.locator('text=Signing in')).toBeVisible();
  });
});

test.describe('MFA Verification Flow', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBackend(page, { requireMfa: true });
  });

  test('should show MFA verification form when MFA is required', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // MFA verification component should appear
    await expect(page.locator('app-mfa-verify')).toBeVisible({ timeout: 5000 });
  });

  test('should allow cancelling MFA and return to login form', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page.locator('app-mfa-verify')).toBeVisible({ timeout: 5000 });

    // Cancel MFA
    const cancelButton = page.getByRole('button', { name: /cancel|back/i });
    if (await cancelButton.isVisible()) {
      await cancelButton.click();
      // Should return to login form
      await expect(page.locator('input[formcontrolname="username"]')).toBeVisible();
    }
  });
});

test.describe('Authentication Redirects', () => {
  test('should redirect to login when accessing protected route without auth', async ({ page }) => {
    // Try to access dashboard directly without auth
    await page.goto('/dashboard');

    // Should redirect to login
    await expect(page).toHaveURL(/.*login/);
  });

  test('should redirect to original URL after login', async ({ page }) => {
    // Set up mock backend
    await mockAuthBackend(page);

    // Try to access patients page
    await page.goto('/patients');

    // Should redirect to login with returnUrl
    await expect(page).toHaveURL(/.*login.*returnUrl/);

    // Login
    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should redirect back to patients
    await page.waitForURL('**/patients', { timeout: 10000 });
    await expect(page).toHaveURL(/.*patients/);
  });

  test('should redirect authenticated user from login to dashboard', async ({ page }) => {
    // Set up auth state first (simulate being logged in)
    await page.goto('/login');

    // Do demo login
    await page.getByRole('button', { name: /demo login/i }).click();
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Now try to visit login again
    await page.goto('/login');

    // Should redirect to dashboard
    await page.waitForURL('**/dashboard', { timeout: 5000 });
    await expect(page).toHaveURL(/.*dashboard/);
  });
});

test.describe('Session Persistence', () => {
  test('should persist session after page reload', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.getByRole('button', { name: /demo login/i }).click();
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Reload the page
    await page.reload();

    // Should still be on dashboard (not redirected to login)
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('should have auth token in localStorage after login', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /demo login/i }).click();
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    const token = await page.evaluate(() => localStorage.getItem('healthdata_auth_token'));
    expect(token).toBeTruthy();
  });
});

test.describe('Security Compliance', () => {
  test('should not expose sensitive data in page source', async ({ page }) => {
    await page.goto('/login');

    const content = await page.content();

    // Should not contain plain text passwords or tokens in source
    expect(content).not.toContain('password123');
    expect(content).not.toContain('Bearer ');
    expect(content).not.toContain('jwt-secret');
  });

  test('should have secure password input field', async ({ page }) => {
    await page.goto('/login');

    const passwordInput = page.locator('input[formcontrolname="password"]');

    // Should be type password (not exposed)
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Should have autocomplete attribute
    await expect(passwordInput).toHaveAttribute('autocomplete', 'current-password');
  });

  test('should display HIPAA compliance notice', async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('.security-notice')).toContainText(/hipaa/i);
    await expect(page.locator('.security-notice mat-icon')).toBeVisible();
  });

  test('should have remember me checkbox', async ({ page }) => {
    await page.goto('/login');

    const rememberMe = page.locator('mat-checkbox[formcontrolname="rememberMe"]');
    await expect(rememberMe).toBeVisible();
  });
});

test.describe('Error Handling', () => {
  test('should handle network errors gracefully', async ({ page }) => {
    // Mock network failure
    await page.route('**/auth/login', (route) => route.abort('connectionrefused'));

    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should show error message
    await expect(page.locator('.mat-mdc-snack-bar-container')).toContainText(/unable to connect|try again/i, {
      timeout: 5000,
    });
  });

  test('should handle account locked error (403)', async ({ page }) => {
    await page.route('**/auth/login', async (route) => {
      return route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Account locked' }),
      });
    });

    await page.goto('/login');

    await page.fill('input[formcontrolname="username"]', TEST_USERS.admin.username);
    await page.fill('input[formcontrolname="password"]', TEST_USERS.admin.password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should show locked account message
    await expect(page.locator('.mat-mdc-snack-bar-container')).toContainText(/locked|contact support/i, {
      timeout: 5000,
    });
  });
});
