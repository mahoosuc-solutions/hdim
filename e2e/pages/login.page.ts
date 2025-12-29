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

    // Form elements
    this.usernameInput = page.locator('[data-testid="username-input"], #username, input[name="username"]');
    this.passwordInput = page.locator('[data-testid="password-input"], #password, input[name="password"]');
    this.loginButton = page.locator('[data-testid="login-button"], button[type="submit"]');
    this.forgotPasswordLink = page.locator('[data-testid="forgot-password-link"], a:has-text("Forgot")');
    this.ssoButton = page.locator('[data-testid="sso-button"], button:has-text("SSO")');
    this.rememberMeCheckbox = page.locator('[data-testid="remember-me"], #rememberMe, input[name="rememberMe"]');

    // MFA elements
    this.mfaCodeInput = page.locator('[data-testid="mfa-code-input"], #mfaCode, input[name="mfaCode"]');
    this.mfaSubmitButton = page.locator('[data-testid="mfa-submit"], button:has-text("Verify")');

    // Error display
    this.loginError = page.locator('[data-testid="login-error"], .login-error, .error-message');

    // Tenant selector (for multi-tenant)
    this.tenantSelector = page.locator('[data-testid="tenant-selector"], #tenant');

    // Logo
    this.logoImage = page.locator('[data-testid="logo"], .logo img');
  }

  /**
   * Navigate to login page
   */
  async goto(): Promise<void> {
    await this.page.goto('/login');
    await this.waitForLoad();
  }

  /**
   * Check if login page is loaded
   */
  async isLoaded(): Promise<boolean> {
    return this.usernameInput.isVisible();
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
}
