import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Integration Page Object
 *
 * Handles EHR connector management and integration workflows.
 * Used for INT-001 to INT-005 test workflows.
 */
export class IntegrationPage extends BasePage {
  readonly url = '/integrations';

  // Navigation
  readonly pageHeading: Locator;
  readonly connectorsTab: Locator;
  readonly mappingsTab: Locator;
  readonly historyTab: Locator;
  readonly settingsTab: Locator;

  // Connector List
  readonly connectorList: Locator;
  readonly connectorCards: Locator;
  readonly addConnectorButton: Locator;
  readonly refreshButton: Locator;

  // Connector Card Elements
  readonly connectorName: Locator;
  readonly connectorType: Locator;
  readonly connectorStatus: Locator;
  readonly lastSyncTime: Locator;

  // Connector Form
  readonly connectorModal: Locator;
  readonly connectorNameInput: Locator;
  readonly connectorTypeSelect: Locator;
  readonly endpointUrlInput: Locator;
  readonly clientIdInput: Locator;
  readonly clientSecretInput: Locator;
  readonly scopeInput: Locator;
  readonly saveConnectorButton: Locator;
  readonly testConnectionButton: Locator;
  readonly cancelButton: Locator;

  // Sync Controls
  readonly syncButton: Locator;
  readonly pauseButton: Locator;
  readonly deleteButton: Locator;
  readonly syncProgress: Locator;
  readonly syncStatus: Locator;

  // Data Mappings
  readonly mappingsList: Locator;
  readonly mappingRows: Locator;
  readonly addMappingButton: Locator;
  readonly sourceFieldSelect: Locator;
  readonly targetFieldSelect: Locator;
  readonly transformSelect: Locator;

  // Sync History
  readonly historyTable: Locator;
  readonly historyRows: Locator;
  readonly historyDateFilter: Locator;
  readonly historyStatusFilter: Locator;

  // Connection Status
  readonly connectionIndicator: Locator;
  readonly connectionMessage: Locator;
  readonly retryButton: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation
    this.pageHeading = page.locator('h1, h2').filter({ hasText: /integration|connector/i });
    this.connectorsTab = page.locator('[data-testid="connectors-tab"], [role="tab"]:has-text("Connectors")');
    this.mappingsTab = page.locator('[data-testid="mappings-tab"], [role="tab"]:has-text("Mappings")');
    this.historyTab = page.locator('[data-testid="history-tab"], [role="tab"]:has-text("History")');
    this.settingsTab = page.locator('[data-testid="settings-tab"], [role="tab"]:has-text("Settings")');

    // Connector List
    this.connectorList = page.locator('[data-testid="connector-list"], .connector-list');
    this.connectorCards = page.locator('[data-testid="connector-card"], .connector-card');
    this.addConnectorButton = page.locator('[data-testid="add-connector"], button:has-text("Add Connector"), button:has-text("New")');
    this.refreshButton = page.locator('[data-testid="refresh"], button:has-text("Refresh")');

    // Connector Card Elements
    this.connectorName = page.locator('[data-testid="connector-name"], .connector-name');
    this.connectorType = page.locator('[data-testid="connector-type"], .connector-type');
    this.connectorStatus = page.locator('[data-testid="connector-status"], .connector-status');
    this.lastSyncTime = page.locator('[data-testid="last-sync"], .last-sync');

    // Connector Form
    this.connectorModal = page.locator('[data-testid="connector-modal"], [role="dialog"]');
    this.connectorNameInput = page.locator('[data-testid="connector-name-input"], #connectorName');
    this.connectorTypeSelect = page.locator('[data-testid="connector-type-select"], #connectorType');
    this.endpointUrlInput = page.locator('[data-testid="endpoint-url"], #endpointUrl');
    this.clientIdInput = page.locator('[data-testid="client-id"], #clientId');
    this.clientSecretInput = page.locator('[data-testid="client-secret"], #clientSecret');
    this.scopeInput = page.locator('[data-testid="scope"], #scope');
    this.saveConnectorButton = page.locator('[data-testid="save-connector"], button:has-text("Save")');
    this.testConnectionButton = page.locator('[data-testid="test-connection"], button:has-text("Test Connection")');
    this.cancelButton = page.locator('[data-testid="cancel"], button:has-text("Cancel")');

    // Sync Controls
    this.syncButton = page.locator('[data-testid="sync"], button:has-text("Sync"), button:has-text("Run Sync")');
    this.pauseButton = page.locator('[data-testid="pause"], button:has-text("Pause")');
    this.deleteButton = page.locator('[data-testid="delete"], button:has-text("Delete")');
    this.syncProgress = page.locator('[data-testid="sync-progress"], .sync-progress, .progress-bar');
    this.syncStatus = page.locator('[data-testid="sync-status"], .sync-status');

    // Data Mappings
    this.mappingsList = page.locator('[data-testid="mappings-list"], .mappings-list, table');
    this.mappingRows = page.locator('[data-testid="mapping-row"], .mapping-row, tbody tr');
    this.addMappingButton = page.locator('[data-testid="add-mapping"], button:has-text("Add Mapping")');
    this.sourceFieldSelect = page.locator('[data-testid="source-field"], #sourceField');
    this.targetFieldSelect = page.locator('[data-testid="target-field"], #targetField');
    this.transformSelect = page.locator('[data-testid="transform"], #transform');

    // Sync History
    this.historyTable = page.locator('[data-testid="history-table"], .history-table, table');
    this.historyRows = page.locator('[data-testid="history-row"], .history-row, tbody tr');
    this.historyDateFilter = page.locator('[data-testid="history-date"], #historyDate');
    this.historyStatusFilter = page.locator('[data-testid="history-status"], #historyStatus');

    // Connection Status
    this.connectionIndicator = page.locator('[data-testid="connection-indicator"], .connection-indicator');
    this.connectionMessage = page.locator('[data-testid="connection-message"], .connection-message');
    this.retryButton = page.locator('[data-testid="retry"], button:has-text("Retry")');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    return (await this.pageHeading.count()) > 0 || (await this.connectorList.count()) > 0;
  }

  /**
   * Wait for integration data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Navigate to connectors tab
   */
  async gotoConnectors(): Promise<void> {
    await this.connectorsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to mappings tab
   */
  async gotoMappings(): Promise<void> {
    await this.mappingsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to history tab
   */
  async gotoHistory(): Promise<void> {
    await this.historyTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Get connector count
   */
  async getConnectorCount(): Promise<number> {
    return this.connectorCards.count();
  }

  /**
   * Add a new connector
   */
  async addConnector(config: {
    name: string;
    type: string;
    endpointUrl: string;
    clientId?: string;
    clientSecret?: string;
    scope?: string;
  }): Promise<void> {
    await this.addConnectorButton.click();
    await this.connectorModal.waitFor({ state: 'visible' });

    await this.connectorNameInput.fill(config.name);

    await this.connectorTypeSelect.click();
    await this.page.locator(`[role="option"]:has-text("${config.type}")`).click();

    await this.endpointUrlInput.fill(config.endpointUrl);

    if (config.clientId) {
      await this.clientIdInput.fill(config.clientId);
    }

    if (config.clientSecret) {
      await this.clientSecretInput.fill(config.clientSecret);
    }

    if (config.scope) {
      await this.scopeInput.fill(config.scope);
    }

    await this.saveConnectorButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Test connector connection
   */
  async testConnection(): Promise<boolean> {
    await this.testConnectionButton.click();
    await this.waitForDataLoad();

    // Check connection status
    const indicator = await this.connectionIndicator.getAttribute('class');
    return indicator?.includes('success') || indicator?.includes('connected') || false;
  }

  /**
   * Select a connector
   */
  async selectConnector(index: number = 0): Promise<void> {
    await this.connectorCards.nth(index).click();
    await this.waitForDataLoad();
  }

  /**
   * Select connector by name
   */
  async selectConnectorByName(name: string): Promise<void> {
    const card = this.connectorCards.filter({ hasText: name });
    await card.first().click();
    await this.waitForDataLoad();
  }

  /**
   * Trigger manual sync
   */
  async triggerSync(): Promise<void> {
    await this.syncButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Wait for sync to complete
   */
  async waitForSyncComplete(timeout: number = 60000): Promise<void> {
    // Wait for progress bar to disappear or status to show complete
    await this.syncProgress.waitFor({ state: 'hidden', timeout }).catch(() => {});
    await this.waitForDataLoad();
  }

  /**
   * Get sync status
   */
  async getSyncStatus(): Promise<string> {
    if (await this.syncStatus.count() > 0) {
      return (await this.syncStatus.textContent()) || '';
    }
    return '';
  }

  /**
   * Pause connector
   */
  async pauseConnector(): Promise<void> {
    await this.pauseButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Delete connector
   */
  async deleteConnector(): Promise<void> {
    await this.deleteButton.click();

    // Confirm deletion
    const confirmButton = this.page.locator('[data-testid="confirm-delete"], button:has-text("Confirm")');
    await confirmButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Get connector status
   */
  async getConnectorStatus(index: number = 0): Promise<string> {
    const status = this.connectorCards.nth(index).locator('[data-testid="connector-status"], .connector-status');
    if (await status.count() > 0) {
      return (await status.textContent()) || '';
    }
    return '';
  }

  /**
   * Get last sync time
   */
  async getLastSyncTime(index: number = 0): Promise<string> {
    const lastSync = this.connectorCards.nth(index).locator('[data-testid="last-sync"], .last-sync');
    if (await lastSync.count() > 0) {
      return (await lastSync.textContent()) || '';
    }
    return '';
  }

  /**
   * Add field mapping
   */
  async addMapping(sourceField: string, targetField: string, transform?: string): Promise<void> {
    await this.addMappingButton.click();

    await this.sourceFieldSelect.click();
    await this.page.locator(`[role="option"]:has-text("${sourceField}")`).click();

    await this.targetFieldSelect.click();
    await this.page.locator(`[role="option"]:has-text("${targetField}")`).click();

    if (transform) {
      await this.transformSelect.click();
      await this.page.locator(`[role="option"]:has-text("${transform}")`).click();
    }

    const saveButton = this.page.locator('button:has-text("Save")');
    await saveButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Get mapping count
   */
  async getMappingCount(): Promise<number> {
    return this.mappingRows.count();
  }

  /**
   * Get history count
   */
  async getHistoryCount(): Promise<number> {
    return this.historyRows.count();
  }

  /**
   * Filter history by status
   */
  async filterHistoryByStatus(status: string): Promise<void> {
    await this.historyStatusFilter.click();
    await this.page.locator(`[role="option"]:has-text("${status}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Check if connector exists
   */
  async connectorExists(name: string): Promise<boolean> {
    const card = this.connectorCards.filter({ hasText: name });
    return (await card.count()) > 0;
  }

  /**
   * Retry failed connection
   */
  async retryConnection(): Promise<void> {
    if (await this.retryButton.count() > 0) {
      await this.retryButton.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Get connection message
   */
  async getConnectionMessage(): Promise<string> {
    if (await this.connectionMessage.count() > 0) {
      return (await this.connectionMessage.textContent()) || '';
    }
    return '';
  }
}
