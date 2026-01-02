import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the Accounts management page
 */
export class AccountsPage {
  readonly page: Page;
  readonly searchInput: Locator;
  readonly stageFilter: Locator;
  readonly addAccountButton: Locator;
  readonly accountsTable: Locator;
  readonly tableRows: Locator;
  readonly pagination: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;

  constructor(page: Page) {
    this.page = page;
    this.searchInput = page.getByPlaceholder(/search/i);
    this.stageFilter = page.locator('div').filter({ hasText: /all stages/i }).first();
    this.addAccountButton = page.getByRole('button', { name: /add account/i });
    this.accountsTable = page.locator('table');
    this.tableRows = page.locator('tbody tr');
    this.pagination = page.locator('[class*="TablePagination"]');
    this.loadingSpinner = page.locator('[class*="CircularProgress"]');
    this.emptyState = page.getByText(/no accounts/i);
  }

  async searchAccounts(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500); // Debounce
    await this.page.waitForLoadState('networkidle');
  }

  async clearSearch() {
    await this.searchInput.clear();
    await this.page.waitForLoadState('networkidle');
  }

  async filterByStage(stage: string) {
    await this.stageFilter.click();
    await this.page.getByRole('option', { name: new RegExp(stage.replace('_', ' '), 'i') }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async getAccountCount(): Promise<number> {
    await this.page.waitForLoadState('networkidle');
    return this.tableRows.count();
  }

  async getAccountByName(name: string): Promise<Locator> {
    return this.tableRows.filter({ hasText: name });
  }

  async getAccountDetails(name: string): Promise<{
    type: string;
    stage: string;
    location: string;
    patients: string;
    ehrs: string;
  }> {
    const row = await this.getAccountByName(name);
    const cells = row.locator('td');

    return {
      type: await cells.nth(1).textContent() || '',
      stage: await cells.nth(2).textContent() || '',
      location: await cells.nth(3).textContent() || '',
      patients: await cells.nth(4).textContent() || '',
      ehrs: await cells.nth(5).textContent() || '',
    };
  }

  async clickViewDetails(name: string) {
    const row = await this.getAccountByName(name);
    await row.getByRole('button').click();
  }

  async openAddAccountDialog() {
    await this.addAccountButton.click();
    await this.page.getByRole('dialog').waitFor({ state: 'visible' });
  }

  async waitForTableLoad() {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    await this.page.waitForLoadState('networkidle');
  }

  async isLoading(): Promise<boolean> {
    return this.loadingSpinner.isVisible();
  }

  async getColumnHeaders(): Promise<string[]> {
    const headers = this.page.locator('thead th');
    const count = await headers.count();
    const headerTexts: string[] = [];
    for (let i = 0; i < count; i++) {
      const text = await headers.nth(i).textContent();
      if (text) headerTexts.push(text.trim());
    }
    return headerTexts;
  }

  async getStageChipColor(name: string): Promise<string> {
    const row = await this.getAccountByName(name);
    const stageChip = row.locator('[class*="Chip"]').first();
    const className = await stageChip.getAttribute('class') || '';
    if (className.includes('success')) return 'success';
    if (className.includes('info')) return 'info';
    if (className.includes('warning')) return 'warning';
    if (className.includes('error')) return 'error';
    if (className.includes('primary')) return 'primary';
    return 'default';
  }

  async goToNextPage() {
    await this.page.getByRole('button', { name: /next page/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async goToPreviousPage() {
    await this.page.getByRole('button', { name: /previous page/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async changeRowsPerPage(count: number) {
    const rowsPerPageSelect = this.page.locator('[class*="TablePagination"] select').first();
    await rowsPerPageSelect.click();
    await this.page.getByRole('option', { name: count.toString() }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async getPatientCount(name: string): Promise<number> {
    const row = await this.getAccountByName(name);
    const patientCell = row.locator('td').nth(4);
    const text = await patientCell.textContent() || '0';
    return parseInt(text.replace(/,/g, ''));
  }

  async getAllAccountNames(): Promise<string[]> {
    const rows = this.tableRows;
    const count = await rows.count();
    const names: string[] = [];
    for (let i = 0; i < count; i++) {
      const nameCell = rows.nth(i).locator('td').first();
      const name = await nameCell.textContent();
      if (name) names.push(name.trim());
    }
    return names;
  }
}
