import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the main Sales page with tab navigation
 */
export class SalesPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly pageSubtitle: Locator;
  readonly dashboardTab: Locator;
  readonly leadsTab: Locator;
  readonly pipelineTab: Locator;
  readonly accountsTab: Locator;
  readonly sequencesTab: Locator;
  readonly tabPanels: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.getByText('Sales Automation').first();
    this.pageSubtitle = page.getByText('Manage leads, pipeline, and email sequences');
    this.dashboardTab = page.getByRole('tab', { name: /dashboard/i });
    this.leadsTab = page.getByRole('tab', { name: /leads/i });
    this.pipelineTab = page.getByRole('tab', { name: /pipeline/i });
    this.accountsTab = page.getByRole('tab', { name: /accounts/i });
    this.sequencesTab = page.getByRole('tab', { name: /sequences/i });
    this.tabPanels = page.locator('[role="tabpanel"]');
  }

  async navigateTo() {
    await this.page.goto('/sales');
    await this.page.waitForLoadState('domcontentloaded');
    // Wait for React to render the Sales Automation heading (MUI Typography h4 variant)
    await this.page.waitForSelector('text=Sales Automation', { timeout: 30000 });
    // Wait for tabs to be visible
    await this.page.waitForSelector('[role="tab"]', { timeout: 10000 });
  }

  async switchToTab(tab: 'dashboard' | 'leads' | 'pipeline' | 'accounts' | 'sequences') {
    const tabMap = {
      dashboard: this.dashboardTab,
      leads: this.leadsTab,
      pipeline: this.pipelineTab,
      accounts: this.accountsTab,
      sequences: this.sequencesTab,
    };
    await tabMap[tab].click();
    await this.page.waitForLoadState('networkidle');
  }

  async getCurrentTab(): Promise<string> {
    const selectedTab = this.page.locator('[role="tab"][aria-selected="true"]');
    return (await selectedTab.textContent()) || '';
  }

  async isTabSelected(tab: 'dashboard' | 'leads' | 'pipeline' | 'accounts' | 'sequences'): Promise<boolean> {
    const tabMap = {
      dashboard: this.dashboardTab,
      leads: this.leadsTab,
      pipeline: this.pipelineTab,
      accounts: this.accountsTab,
      sequences: this.sequencesTab,
    };
    const ariaSelected = await tabMap[tab].getAttribute('aria-selected');
    return ariaSelected === 'true';
  }

  async waitForTabContent() {
    await this.page.waitForSelector('[role="tabpanel"]');
  }

  async getTabAriaLabel(tabIndex: number): Promise<string | null> {
    const tab = this.page.locator(`#sales-tab-${tabIndex}`);
    return tab.getAttribute('aria-label');
  }

  async getAllTabs(): Promise<string[]> {
    const tabs = this.page.locator('[role="tab"]');
    const count = await tabs.count();
    const tabNames: string[] = [];
    for (let i = 0; i < count; i++) {
      const text = await tabs.nth(i).textContent();
      if (text) tabNames.push(text.trim());
    }
    return tabNames;
  }
}
