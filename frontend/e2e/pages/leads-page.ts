import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the Leads management page
 */
export class LeadsPage {
  readonly page: Page;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly sourceFilter: Locator;
  readonly addLeadButton: Locator;
  readonly leadsTable: Locator;
  readonly tableRows: Locator;
  readonly pagination: Locator;
  readonly rowsPerPageSelect: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;
  readonly editDialog: Locator;

  constructor(page: Page) {
    this.page = page;
    this.searchInput = page.getByPlaceholder(/search/i);
    this.statusFilter = page.locator('select').filter({ hasText: /status/i }).first();
    this.sourceFilter = page.locator('select').filter({ hasText: /source/i }).first();
    this.addLeadButton = page.getByRole('button', { name: /add lead/i });
    this.leadsTable = page.locator('table');
    this.tableRows = page.locator('tbody tr');
    this.pagination = page.locator('[class*="TablePagination"]');
    this.rowsPerPageSelect = page.locator('[class*="TablePagination"] select').first();
    this.loadingSpinner = page.locator('[class*="CircularProgress"]');
    this.emptyState = page.getByText(/no leads/i);
    this.editDialog = page.getByRole('dialog');
  }

  async searchLeads(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500); // Debounce
    await this.page.waitForLoadState('networkidle');
  }

  async clearSearch() {
    await this.searchInput.clear();
    await this.page.waitForLoadState('networkidle');
  }

  async filterByStatus(status: string) {
    // Find the status select dropdown
    const statusSelect = this.page.locator('div').filter({ hasText: /all statuses/i }).first();
    await statusSelect.click();
    await this.page.getByRole('option', { name: new RegExp(status, 'i') }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async filterBySource(source: string) {
    // Find the source select dropdown
    const sourceSelect = this.page.locator('div').filter({ hasText: /all sources/i }).first();
    await sourceSelect.click();
    await this.page.getByRole('option', { name: new RegExp(source, 'i') }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async getLeadCount(): Promise<number> {
    await this.page.waitForLoadState('networkidle');
    return this.tableRows.count();
  }

  async getTotalLeadCount(): Promise<number> {
    // Get from pagination info
    const paginationText = await this.pagination.textContent();
    const match = paginationText?.match(/of\s+(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  async getLeadByEmail(email: string): Promise<Locator> {
    return this.tableRows.filter({ hasText: email });
  }

  async clickLeadActions(email: string) {
    const row = await this.getLeadByEmail(email);
    await row.locator('[data-testid="MoreVertIcon"]').click();
  }

  async editLead(email: string) {
    await this.clickLeadActions(email);
    await this.page.getByRole('menuitem', { name: /edit/i }).click();
  }

  async deleteLead(email: string) {
    await this.clickLeadActions(email);
    await this.page.getByRole('menuitem', { name: /delete/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async convertLead(email: string) {
    await this.clickLeadActions(email);
    await this.page.getByRole('menuitem', { name: /convert/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async openAddLeadDialog() {
    await this.addLeadButton.click();
    await this.editDialog.waitFor({ state: 'visible' });
  }

  async fillLeadForm(data: {
    firstName?: string;
    lastName?: string;
    email?: string;
    company?: string;
  }) {
    if (data.firstName) {
      await this.page.getByLabel(/first name/i).fill(data.firstName);
    }
    if (data.lastName) {
      await this.page.getByLabel(/last name/i).fill(data.lastName);
    }
    if (data.email) {
      await this.page.getByLabel(/email/i).fill(data.email);
    }
    if (data.company) {
      await this.page.getByLabel(/company/i).fill(data.company);
    }
  }

  async saveLeadForm() {
    await this.page.getByRole('button', { name: /save/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async cancelLeadForm() {
    await this.page.getByRole('button', { name: /cancel/i }).click();
  }

  async changeRowsPerPage(count: number) {
    await this.rowsPerPageSelect.click();
    await this.page.getByRole('option', { name: count.toString() }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async goToNextPage() {
    await this.page.getByRole('button', { name: /next page/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async goToPreviousPage() {
    await this.page.getByRole('button', { name: /previous page/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async isLoading(): Promise<boolean> {
    return this.loadingSpinner.isVisible();
  }

  async waitForTableLoad() {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    await this.page.waitForLoadState('networkidle');
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

  async getScoreBadgeColor(email: string): Promise<string> {
    const row = await this.getLeadByEmail(email);
    const scoreBadge = row.locator('[class*="MuiChip"]').last();
    const className = await scoreBadge.getAttribute('class') || '';
    if (className.includes('success')) return 'green';
    if (className.includes('warning')) return 'yellow';
    if (className.includes('error')) return 'red';
    return 'default';
  }

  async getStatusChipColor(email: string): Promise<string> {
    const row = await this.getLeadByEmail(email);
    const chips = row.locator('[class*="MuiChip"]');
    // Status chip is typically the 4th column
    const statusChip = chips.nth(1);
    const className = await statusChip.getAttribute('class') || '';
    if (className.includes('success')) return 'success';
    if (className.includes('info')) return 'info';
    if (className.includes('warning')) return 'warning';
    if (className.includes('error')) return 'error';
    if (className.includes('primary')) return 'primary';
    return 'default';
  }
}
