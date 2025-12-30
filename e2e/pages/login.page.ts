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
    this.usernameInput = page.locator('input[formcontrolname="username"], input[ng-reflect-name="username"], input[autocomplete="username"]');
    this.passwordInput = page.locator('input[formcontrolname="password"], input[ng-reflect-name="password"], input[autocomplete="current-password"]');
    this.loginButton = page.locator('button[type="submit"]:has-text("Sign In"), button[type="submit"]:has(mat-icon)');
    this.forgotPasswordLink = page.locator('a[routerlink="/forgot-password"], a:has-text("Forgot password")');
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
   */
  async goto(): Promise<void> {
    await this.page.goto('/login');
    await this.waitForLoad();
  }

  /**
   * Check if login page is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      await this.usernameInput.waitFor({ state: 'visible', timeout: 5000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Use demo login (for development/testing)
   */
  async demoLogin(): Promise<void> {
    const demoButton = this.page.locator('button:has-text("Demo Login")');
    if (await demoButton.isVisible()) {
      await demoButton.click();
      await this.page.waitForURL('**/dashboard', { timeout: 30000 });
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
