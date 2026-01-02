import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the Email Sequences management page
 */
export class SequencesPage {
  readonly page: Page;
  readonly createSequenceButton: Locator;
  readonly sequenceCards: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createSequenceButton = page.getByRole('button', { name: /create sequence/i });
    this.sequenceCards = page.locator('[class*="Card"]').filter({ has: page.locator('[class*="Switch"]') });
    this.loadingSpinner = page.locator('[class*="CircularProgress"]');
    this.emptyState = page.getByText(/no sequences/i);
  }

  async waitForLoad() {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    await this.page.waitForLoadState('networkidle');
  }

  async isLoading(): Promise<boolean> {
    return this.loadingSpinner.isVisible();
  }

  async hasEmptyState(): Promise<boolean> {
    return this.emptyState.isVisible();
  }

  async getSequenceCount(): Promise<number> {
    await this.waitForLoad();
    return this.sequenceCards.count();
  }

  async getSequenceCard(name: string): Promise<Locator> {
    return this.page.locator('[class*="Card"]').filter({ hasText: name });
  }

  async getSequenceDetails(name: string): Promise<{
    type: string;
    isActive: boolean;
    stepCount: string;
    description: string;
  }> {
    const card = await this.getSequenceCard(name);
    const text = await card.textContent() || '';

    // Check if switch is checked
    const switchElement = card.locator('[class*="Switch"] input');
    const isActive = await switchElement.isChecked();

    // Get type chip
    const typeChip = card.locator('[class*="Chip"]').first();
    const type = await typeChip.textContent() || '';

    // Get step count
    const stepCountMatch = text.match(/(\d+)\s*steps?/i);
    const stepCount = stepCountMatch?.[1] || '0';

    return {
      type: type.trim(),
      isActive,
      stepCount,
      description: '', // Description may or may not be present
    };
  }

  async toggleSequenceActive(name: string) {
    const card = await this.getSequenceCard(name);
    const switchElement = card.locator('[class*="Switch"]');
    await switchElement.click();
    await this.page.waitForLoadState('networkidle');
  }

  async isSequenceActive(name: string): Promise<boolean> {
    const card = await this.getSequenceCard(name);
    const switchInput = card.locator('[class*="Switch"] input');
    return switchInput.isChecked();
  }

  async clickEditSequence(name: string) {
    const card = await this.getSequenceCard(name);
    await card.getByRole('button', { name: /edit/i }).click();
  }

  async clickViewAnalytics(name: string) {
    const card = await this.getSequenceCard(name);
    await card.getByRole('button', { name: /analytics/i }).click();
  }

  async openCreateSequenceDialog() {
    await this.createSequenceButton.click();
    await this.page.getByRole('dialog').waitFor({ state: 'visible' });
  }

  async getSequenceAnalytics(name: string): Promise<{
    openRate: string;
    clickRate: string;
    totalEnrollments: string;
    activeEnrollments: string;
    totalSent: string;
  }> {
    const card = await this.getSequenceCard(name);
    const text = await card.textContent() || '';

    return {
      openRate: text.match(/open.*?(\d+(?:\.\d+)?%)/i)?.[1] || '0%',
      clickRate: text.match(/click.*?(\d+(?:\.\d+)?%)/i)?.[1] || '0%',
      totalEnrollments: text.match(/total.*?(\d+)/i)?.[1] || '0',
      activeEnrollments: text.match(/active.*?(\d+)/i)?.[1] || '0',
      totalSent: text.match(/sent.*?(\d+)/i)?.[1] || '0',
    };
  }

  async getAllSequenceNames(): Promise<string[]> {
    const cards = this.sequenceCards;
    const count = await cards.count();
    const names: string[] = [];
    for (let i = 0; i < count; i++) {
      const titleElement = cards.nth(i).locator('h6, [class*="Typography"]').first();
      const name = await titleElement.textContent();
      if (name) names.push(name.trim());
    }
    return names;
  }

  async getSequenceTypeColor(name: string): Promise<string> {
    const card = await this.getSequenceCard(name);
    const typeChip = card.locator('[class*="Chip"]').first();
    const className = await typeChip.getAttribute('class') || '';
    if (className.includes('primary')) return 'primary';
    if (className.includes('success')) return 'success';
    if (className.includes('info')) return 'info';
    if (className.includes('warning')) return 'warning';
    if (className.includes('secondary')) return 'secondary';
    return 'default';
  }

  async hasOpenRateProgressBar(name: string): Promise<boolean> {
    const card = await this.getSequenceCard(name);
    const progressBar = card.locator('[class*="LinearProgress"]');
    return progressBar.isVisible();
  }

  async getCardLayout(): Promise<'grid' | 'list'> {
    // Check if cards are in a grid layout
    const container = this.page.locator('[class*="Grid"]').first();
    const isGrid = await container.isVisible();
    return isGrid ? 'grid' : 'list';
  }
}
