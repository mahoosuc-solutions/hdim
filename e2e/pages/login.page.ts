import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Login Page Object Model
 *
 * Handles authentication workflows including:
 * - Standard username/password login
 * - SSO login
 * - MFA flows
 * - Password reset
 * - Session management
 */
export class LoginPage extends BasePage {
  // Page elements
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly forgotPasswordLink: Locator;
  readonly ssoButton: Locator;
  readonly rememberMeCheckbox: Locator;
  readonly mfaCodeInput: Locator;
  readonly mfaSubmitButton: Locator;
  readonly loginError: Locator;
  readonly tenantSelector: Locator;
  readonly logoImage: Locator;

  constructor(page: Page) {
    super(page);

    // Form elements - Angular Material with reactive forms
    this.usernameInput = page.locator('input[formcontrolname="username"], input[ng-reflect-name="username"], input[autocomplete="username"], input[placeholder*="username" i], input[aria-label*="Username" i]');
    this.passwordInput = page.locator('input[formcontrolname="password"], input[ng-reflect-name="password"], input[autocomplete="current-password"], input[placeholder*="password" i], input[aria-label*="Password" i]');
    this.loginButton = page.locator('button[type="submit"]:has-text("Sign In"), button[type="submit"]:has(mat-icon), button:has-text("Sign In")');
    this.forgotPasswordLink = page.locator('a[routerlink="/forgot-password"], a:has-text("Forgot password"), a[href*="forgot"]');
    this.ssoButton = page.locator('button:has-text("SSO")');
    this.rememberMeCheckbox = page.locator('mat-checkbox[formcontrolname="rememberMe"], mat-checkbox:has-text("Remember me")');

    // MFA elements
    this.mfaCodeInput = page.locator('[data-testid="mfa-code-input"], #mfaCode, input[name="mfaCode"]');
    this.mfaSubmitButton = page.locator('[data-testid="mfa-submit"], button:has-text("Verify")');

    // Error display - Angular Material snackbar or mat-error
    this.loginError = page.locator('mat-error, .mat-mdc-snack-bar-container, simple-snack-bar, .error-snackbar');

    // Tenant selector (for multi-tenant)
    this.tenantSelector = page.locator('[data-testid="tenant-selector"], #tenant, mat-select[formcontrolname="tenant"]');

    // Logo - Angular Material icon
    this.logoImage = page.locator('.logo-icon, mat-icon:has-text("health_and_safety"), .logo-container mat-icon');
  }

  /**
   * Navigate to login page
   * Clears any existing auth state first to ensure fresh login
   */
  async goto(): Promise<void> {
    // First navigate to a page to establish context for evaluate
    await this.page.goto('/login', { waitUntil: 'commit' });

    // Clear auth state
    await this.page.evaluate(() => {
      localStorage.removeItem('healthdata_auth_token');
      localStorage.removeItem('healthdata_user');
      sessionStorage.clear();
    });

    // Reload to apply cleared state
    await this.page.reload();
    await this.waitForLoad();
  }

  /**
   * Navigate to login page without clearing auth
   * Use when testing redirect behavior
   */
  async gotoPreserveAuth(): Promise<void> {
    await this.page.goto('/login');
    await this.waitForLoad();
  }

  /**
   * Check if login page is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      // Try multiple selectors to find login form
      const loginFormSelectors = [
        this.usernameInput,
        this.page.getByRole('textbox', { name: /username/i }),
        this.page.locator('form').filter({ hasText: 'Username' }),
        this.page.locator('button:has-text("Demo Login")')
      ];

      for (const locator of loginFormSelectors) {
        try {
          await locator.waitFor({ state: 'visible', timeout: 3000 });
          return true;
        } catch {
          continue;
        }
      }
      return false;
    } catch {
      return false;
    }
  }

  /**
   * Use demo login (for development/testing)
   * Handles already being logged in (redirected to dashboard)
   */
  async demoLogin(): Promise<void> {
    // Check if already on dashboard (already logged in)
    const currentUrl = this.page.url();
    if (currentUrl.includes('/dashboard')) {
      return; // Already logged in
    }

    const demoButton = this.page.locator('button:has-text("Demo Login")');
    if (await demoButton.isVisible({ timeout: 5000 }).catch(() => false)) {
      await demoButton.click();
      await this.page.waitForURL('**/dashboard', { timeout: 30000 });
    } else {
      // If demo button not visible, check if we're already logged in
      await this.page.waitForURL('**/dashboard', { timeout: 5000 }).catch(() => {
        // If not on dashboard and no demo button, navigate directly
        this.page.goto('/dashboard');
      });
    }
  }

  /**
   * Login with username and password
   */
  async login(username: string, password: string): Promise<void> {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  /**
   * Login and wait for successful redirect
   */
  async loginAndWait(username: string, password: string, expectedUrl: string = '/dashboard'): Promise<void> {
    await this.login(username, password);
    await this.page.waitForURL(`**${expectedUrl}`, { timeout: 30000 });
  }

  /**
   * Login with Remember Me option
   */
  async loginWithRememberMe(username: string, password: string): Promise<void> {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.rememberMeCheckbox.check();
    await this.loginButton.click();
  }

  /**
   * Attempt login and expect failure
   */
  async loginExpectFailure(username: string, password: string): Promise<string> {
    await this.login(username, password);
    await this.loginError.waitFor({ state: 'visible', timeout: 10000 });
    return this.loginError.textContent() || '';
  }

  /**
   * Select tenant before login
   */
  async selectTenant(tenantId: string): Promise<void> {
    if (await this.tenantSelector.isVisible()) {
      await this.tenantSelector.selectOption(tenantId);
    }
  }

  /**
   * Click SSO login button
   */
  async clickSsoLogin(): Promise<void> {
    await this.ssoButton.click();
  }

  /**
   * Navigate to forgot password
   */
  async goToForgotPassword(): Promise<void> {
    await this.forgotPasswordLink.click();
    await this.page.waitForURL('**/forgot-password');
  }

  /**
   * Enter MFA code
   */
  async enterMfaCode(code: string): Promise<void> {
    await this.mfaCodeInput.fill(code);
    await this.mfaSubmitButton.click();
  }

  /**
   * Check if MFA is required
   */
  async isMfaRequired(): Promise<boolean> {
    return this.mfaCodeInput.isVisible();
  }

  /**
   * Get login error message
   */
  async getLoginError(): Promise<string> {
    if (await this.loginError.isVisible()) {
      return this.loginError.textContent() || '';
    }
    return '';
  }

  /**
   * Check if login button is enabled
   */
  async isLoginButtonEnabled(): Promise<boolean> {
    return this.loginButton.isEnabled();
  }

  /**
   * Assert login error message
   */
  async assertLoginError(expectedText: string | RegExp): Promise<void> {
    await expect(this.loginError).toBeVisible();
    await expect(this.loginError).toContainText(expectedText);
  }

  /**
   * Assert login page is displayed
   */
  async assertLoginPageDisplayed(): Promise<void> {
    await expect(this.usernameInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.loginButton).toBeVisible();
  }

  /**
   * Clear login form
   */
  async clearForm(): Promise<void> {
    await this.usernameInput.clear();
    await this.passwordInput.clear();
  }

  /**
   * Check if username field has validation error
   */
  async hasUsernameError(): Promise<boolean> {
    const input = this.usernameInput;
    const classes = await input.getAttribute('class') || '';
    return classes.includes('invalid') || classes.includes('error') || classes.includes('ng-invalid');
  }

  /**
   * Check if password field has validation error
   */
  async hasPasswordError(): Promise<boolean> {
    const input = this.passwordInput;
    const classes = await input.getAttribute('class') || '';
    return classes.includes('invalid') || classes.includes('error') || classes.includes('ng-invalid');
  }

  /**
   * Check if there's a login error displayed
   */
  async hasError(): Promise<boolean> {
    try {
      await this.loginError.waitFor({ state: 'visible', timeout: 3000 });
      return true;
    } catch {
      return false;
    }
  }
}
