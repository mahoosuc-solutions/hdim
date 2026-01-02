import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the Pipeline Kanban board
 */
export class PipelinePage {
  readonly page: Page;
  readonly summaryCard: Locator;
  readonly kanbanBoard: Locator;
  readonly stageColumns: Locator;
  readonly opportunityCards: Locator;
  readonly moveDialog: Locator;
  readonly loadingSpinner: Locator;

  // Stage names
  readonly stages = ['DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST'];

  constructor(page: Page) {
    this.page = page;
    this.summaryCard = page.locator('[class*="Card"]').first();
    this.kanbanBoard = page.locator('[style*="overflow-x"]').first();
    this.stageColumns = page.locator('[class*="Paper"]').filter({ has: page.locator('h6') });
    this.opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
    this.moveDialog = page.getByRole('dialog');
    this.loadingSpinner = page.locator('[class*="CircularProgress"]');
  }

  async waitForBoardLoad() {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    await this.page.waitForLoadState('networkidle');
  }

  async getStageColumn(stage: string): Promise<Locator> {
    return this.page.locator('[class*="Paper"]').filter({ hasText: new RegExp(stage.replace('_', ' '), 'i') });
  }

  async getStageCount(stage: string): Promise<number> {
    const column = await this.getStageColumn(stage);
    const countChip = column.locator('[class*="Chip"]').first();
    const text = await countChip.textContent();
    return parseInt(text || '0');
  }

  async getStageTotalValue(stage: string): Promise<string> {
    const column = await this.getStageColumn(stage);
    const valueText = column.locator('text=/\\$/').first();
    return (await valueText.textContent()) || '$0';
  }

  async getOpportunityCard(name: string): Promise<Locator> {
    return this.page.locator('[class*="Card"]').filter({ hasText: name });
  }

  async clickOpportunity(name: string) {
    const card = await this.getOpportunityCard(name);
    await card.click();
    await this.moveDialog.waitFor({ state: 'visible' });
  }

  async moveOpportunity(name: string, toStage: string) {
    await this.clickOpportunity(name);

    // Select new stage
    const stageSelect = this.moveDialog.locator('select, [role="combobox"]').first();
    await stageSelect.click();
    await this.page.getByRole('option', { name: new RegExp(toStage.replace('_', ' '), 'i') }).click();

    // Click move button
    await this.moveDialog.getByRole('button', { name: /move/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async moveToClosedLost(name: string, reason: string) {
    await this.clickOpportunity(name);

    // Select CLOSED_LOST stage
    const stageSelect = this.moveDialog.locator('select, [role="combobox"]').first();
    await stageSelect.click();
    await this.page.getByRole('option', { name: /closed lost/i }).click();

    // Fill in lost reason
    const reasonField = this.moveDialog.getByLabel(/reason/i);
    await reasonField.fill(reason);

    // Click move button
    await this.moveDialog.getByRole('button', { name: /move/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async cancelMoveDialog() {
    await this.moveDialog.getByRole('button', { name: /cancel/i }).click();
  }

  async getPipelineSummary(): Promise<{
    totalValue: string;
    weightedValue: string;
    opportunityCount: string;
    avgDealSize: string;
    avgProbability: string;
  }> {
    const summaryTexts = await this.summaryCard.allTextContents();
    const text = summaryTexts.join(' ');

    return {
      totalValue: text.match(/Total.*?(\$[\d,]+)/)?.[1] || '$0',
      weightedValue: text.match(/Weighted.*?(\$[\d,]+)/)?.[1] || '$0',
      opportunityCount: text.match(/(\d+)\s*opportunities/i)?.[1] || '0',
      avgDealSize: text.match(/Avg.*?(\$[\d,]+)/)?.[1] || '$0',
      avgProbability: text.match(/(\d+)%\s*avg/i)?.[1] || '0',
    };
  }

  async getAllOpportunityNames(): Promise<string[]> {
    const cards = this.opportunityCards;
    const count = await cards.count();
    const names: string[] = [];
    for (let i = 0; i < count; i++) {
      const titleElement = cards.nth(i).locator('h6, [class*="Typography"]').first();
      const name = await titleElement.textContent();
      if (name) names.push(name.trim());
    }
    return names;
  }

  async getOpportunityDetails(name: string): Promise<{
    accountName: string;
    amount: string;
    probability: string;
    closeDate: string;
  }> {
    const card = await this.getOpportunityCard(name);
    const text = await card.textContent() || '';

    return {
      accountName: text.match(/(.+?)\s*\$/)?.[1]?.trim() || '',
      amount: text.match(/(\$[\d,]+)/)?.[1] || '$0',
      probability: text.match(/(\d+)%/)?.[1] || '0',
      closeDate: text.match(/\d{1,2}\/\d{1,2}\/\d{4}|\d{4}-\d{2}-\d{2}/)?.[0] || '',
    };
  }

  async isOpportunityAtRisk(name: string): Promise<boolean> {
    const card = await this.getOpportunityCard(name);
    const warningIcon = card.locator('[data-testid="WarningIcon"]');
    return warningIcon.isVisible();
  }

  async getStageColumnCount(): Promise<number> {
    // Count columns with stage headers
    const columns = this.page.locator('[class*="Paper"]').filter({
      has: this.page.locator('h6')
    });
    return columns.count();
  }

  async getProbabilityColor(name: string): Promise<string> {
    const card = await this.getOpportunityCard(name);
    const probabilityChip = card.locator('[class*="Chip"]');
    const className = await probabilityChip.getAttribute('class') || '';
    if (className.includes('success')) return 'green';
    if (className.includes('warning')) return 'yellow';
    return 'default';
  }
}
