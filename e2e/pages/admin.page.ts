import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Admin Page Object
 *
 * Handles administration functionality including user management,
 * role assignment, system configuration, and audit logs.
 * Used for ADM-001 to ADM-005 test workflows.
 */
export class AdminPage extends BasePage {
  readonly url = '/admin';

  // Navigation
  readonly pageHeading: Locator;
  readonly usersTab: Locator;
  readonly rolesTab: Locator;
  readonly settingsTab: Locator;
  readonly auditTab: Locator;
  readonly importTab: Locator;

  // User Management
  readonly userList: Locator;
  readonly userRows: Locator;
  readonly createUserButton: Locator;
  readonly userSearchInput: Locator;
  readonly userStatusFilter: Locator;
  readonly userRoleFilter: Locator;

  // User Form
  readonly userModal: Locator;
  readonly usernameInput: Locator;
  readonly emailInput: Locator;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly roleSelect: Locator;
  readonly statusSelect: Locator;
  readonly saveUserButton: Locator;
  readonly cancelButton: Locator;

  // Role Management
  readonly roleList: Locator;
  readonly roleRows: Locator;
  readonly createRoleButton: Locator;
  readonly permissionsList: Locator;

  // System Settings
  readonly settingsForm: Locator;
  readonly tenantSettings: Locator;
  readonly securitySettings: Locator;
  readonly notificationSettings: Locator;
  readonly saveSettingsButton: Locator;

  // Audit Log
  readonly auditTable: Locator;
  readonly auditRows: Locator;
  readonly auditDateFilter: Locator;
  readonly auditActionFilter: Locator;
  readonly auditUserFilter: Locator;
  readonly exportAuditButton: Locator;

  // Data Import
  readonly importDropzone: Locator;
  readonly importFileInput: Locator;
  readonly importTypeSelect: Locator;
  readonly importButton: Locator;
  readonly importProgress: Locator;
  readonly importResults: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation
    this.pageHeading = page.locator('h1, h2').filter({ hasText: /admin|settings/i });
    this.usersTab = page.locator('[data-testid="users-tab"], [role="tab"]:has-text("Users")');
    this.rolesTab = page.locator('[data-testid="roles-tab"], [role="tab"]:has-text("Roles")');
    this.settingsTab = page.locator('[data-testid="settings-tab"], [role="tab"]:has-text("Settings")');
    this.auditTab = page.locator('[data-testid="audit-tab"], [role="tab"]:has-text("Audit")');
    this.importTab = page.locator('[data-testid="import-tab"], [role="tab"]:has-text("Import")');

    // User Management
    this.userList = page.locator('[data-testid="user-list"], .user-list, table');
    this.userRows = page.locator('[data-testid="user-row"], .user-row, tbody tr');
    this.createUserButton = page.locator('[data-testid="create-user"], button:has-text("Add User"), button:has-text("Create")');
    this.userSearchInput = page.locator('[data-testid="user-search"], input[placeholder*="Search"]');
    this.userStatusFilter = page.locator('[data-testid="user-status-filter"], #userStatus');
    this.userRoleFilter = page.locator('[data-testid="user-role-filter"], #userRole');

    // User Form
    this.userModal = page.locator('[data-testid="user-modal"], [role="dialog"]');
    this.usernameInput = page.locator('[data-testid="username"], #username');
    this.emailInput = page.locator('[data-testid="email"], #email');
    this.firstNameInput = page.locator('[data-testid="first-name"], #firstName');
    this.lastNameInput = page.locator('[data-testid="last-name"], #lastName');
    this.roleSelect = page.locator('[data-testid="role-select"], #role');
    this.statusSelect = page.locator('[data-testid="status-select"], #status');
    this.saveUserButton = page.locator('[data-testid="save-user"], button:has-text("Save")');
    this.cancelButton = page.locator('[data-testid="cancel"], button:has-text("Cancel")');

    // Role Management
    this.roleList = page.locator('[data-testid="role-list"], .role-list');
    this.roleRows = page.locator('[data-testid="role-row"], .role-row');
    this.createRoleButton = page.locator('[data-testid="create-role"], button:has-text("Add Role")');
    this.permissionsList = page.locator('[data-testid="permissions"], .permissions-list');

    // System Settings
    this.settingsForm = page.locator('[data-testid="settings-form"], form.settings');
    this.tenantSettings = page.locator('[data-testid="tenant-settings"], .tenant-settings');
    this.securitySettings = page.locator('[data-testid="security-settings"], .security-settings');
    this.notificationSettings = page.locator('[data-testid="notification-settings"], .notification-settings');
    this.saveSettingsButton = page.locator('[data-testid="save-settings"], button:has-text("Save Settings")');

    // Audit Log
    this.auditTable = page.locator('[data-testid="audit-table"], .audit-table, table');
    this.auditRows = page.locator('[data-testid="audit-row"], .audit-row, tbody tr');
    this.auditDateFilter = page.locator('[data-testid="audit-date"], #auditDate');
    this.auditActionFilter = page.locator('[data-testid="audit-action"], #auditAction');
    this.auditUserFilter = page.locator('[data-testid="audit-user"], #auditUser');
    this.exportAuditButton = page.locator('[data-testid="export-audit"], button:has-text("Export")');

    // Data Import
    this.importDropzone = page.locator('[data-testid="import-dropzone"], .dropzone');
    this.importFileInput = page.locator('input[type="file"]');
    this.importTypeSelect = page.locator('[data-testid="import-type"], #importType');
    this.importButton = page.locator('[data-testid="import-button"], button:has-text("Import")');
    this.importProgress = page.locator('[data-testid="import-progress"], .progress-bar');
    this.importResults = page.locator('[data-testid="import-results"], .import-results');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    return (await this.pageHeading.count()) > 0;
  }

  /**
   * Wait for data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Navigate to users section
   */
  async gotoUsers(): Promise<void> {
    await this.page.goto('/admin/users');
    await this.waitForDataLoad();
  }

  /**
   * Navigate to roles section
   */
  async gotoRoles(): Promise<void> {
    await this.page.goto('/admin/roles');
    await this.waitForDataLoad();
  }

  /**
   * Navigate to settings section
   */
  async gotoSettings(): Promise<void> {
    await this.page.goto('/admin/settings');
    await this.waitForDataLoad();
  }

  /**
   * Navigate to audit log
   */
  async gotoAudit(): Promise<void> {
    await this.page.goto('/admin/audit');
    await this.waitForDataLoad();
  }

  /**
   * Navigate to import section
   */
  async gotoImport(): Promise<void> {
    await this.page.goto('/admin/import');
    await this.waitForDataLoad();
  }

  /**
   * Get user count
   */
  async getUserCount(): Promise<number> {
    return this.userRows.count();
  }

  /**
   * Search for users
   */
  async searchUsers(query: string): Promise<void> {
    await this.userSearchInput.fill(query);
    await this.page.keyboard.press('Enter');
    await this.waitForDataLoad();
  }

  /**
   * Create a new user
   */
  async createUser(user: {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
  }): Promise<void> {
    await this.createUserButton.click();
    await this.userModal.waitFor({ state: 'visible' });

    await this.usernameInput.fill(user.username);
    await this.emailInput.fill(user.email);
    await this.firstNameInput.fill(user.firstName);
    await this.lastNameInput.fill(user.lastName);

    await this.roleSelect.click();
    await this.page.locator(`[role="option"]:has-text("${user.role}")`).click();

    await this.saveUserButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Edit user by username
   */
  async editUser(username: string): Promise<void> {
    const row = this.userRows.filter({ hasText: username });
    const editButton = row.locator('[data-testid="edit"], button:has-text("Edit")');
    await editButton.click();
    await this.userModal.waitFor({ state: 'visible' });
  }

  /**
   * Delete user by username
   */
  async deleteUser(username: string): Promise<void> {
    const row = this.userRows.filter({ hasText: username });
    const deleteButton = row.locator('[data-testid="delete"], button:has-text("Delete")');
    await deleteButton.click();

    // Confirm deletion
    const confirmButton = this.page.locator('[data-testid="confirm-delete"], button:has-text("Confirm")');
    await confirmButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Assign role to user
   */
  async assignRole(username: string, role: string): Promise<void> {
    await this.editUser(username);
    await this.roleSelect.click();
    await this.page.locator(`[role="option"]:has-text("${role}")`).click();
    await this.saveUserButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Get audit log count
   */
  async getAuditLogCount(): Promise<number> {
    return this.auditRows.count();
  }

  /**
   * Filter audit logs by action
   */
  async filterAuditByAction(action: string): Promise<void> {
    await this.auditActionFilter.click();
    await this.page.locator(`[role="option"]:has-text("${action}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Export audit logs
   */
  async exportAuditLog(): Promise<void> {
    await this.exportAuditButton.click();
  }

  /**
   * Import data from file
   */
  async importData(filePath: string, type: string): Promise<void> {
    await this.importTypeSelect.click();
    await this.page.locator(`[role="option"]:has-text("${type}")`).click();

    await this.importFileInput.setInputFiles(filePath);
    await this.importButton.click();

    // Wait for import to complete
    await this.importProgress.waitFor({ state: 'hidden', timeout: 60000 }).catch(() => {});
    await this.waitForDataLoad();
  }

  /**
   * Check if import was successful
   */
  async isImportSuccessful(): Promise<boolean> {
    if (await this.importResults.count() > 0) {
      const text = await this.importResults.textContent();
      return text?.includes('success') || text?.includes('Success') || false;
    }
    return false;
  }

  /**
   * Save system settings
   */
  async saveSettings(): Promise<void> {
    await this.saveSettingsButton.click();
    await this.waitForSuccessMessage();
  }

  /**
   * Check if user exists
   */
  async userExists(username: string): Promise<boolean> {
    const row = this.userRows.filter({ hasText: username });
    return (await row.count()) > 0;
  }
}
