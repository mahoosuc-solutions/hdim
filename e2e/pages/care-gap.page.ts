import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Care Gap Management Page Object Model
 *
 * Handles care gap workflows:
 * - View care gaps list
 * - Filter and prioritize gaps
 * - Record interventions
 * - Close care gaps
 * - Bulk operations
 */
export class CareGapPage extends BasePage {
  // Filter controls
  readonly urgencyFilter: Locator;
  readonly typeFilter: Locator;
  readonly statusFilter: Locator;
  readonly patientFilter: Locator;
  readonly measureFilter: Locator;
  readonly clearFiltersButton: Locator;

  // Care gap list
  readonly careGapTable: Locator;
  readonly careGapRows: Locator;
  readonly noGapsMessage: Locator;
  readonly gapCount: Locator;

  // Pagination
  readonly paginationControls: Locator;
  readonly nextPageButton: Locator;
  readonly previousPageButton: Locator;

  // Gap details panel
  readonly gapDetailsPanel: Locator;
  readonly gapPatientName: Locator;
  readonly gapMeasureName: Locator;
  readonly gapUrgency: Locator;
  readonly gapType: Locator;
  readonly gapDueDate: Locator;
  readonly gapRecommendation: Locator;
  readonly gapHistory: Locator;

  // Intervention controls
  readonly recordInterventionButton: Locator;
  readonly interventionTypeSelect: Locator;
  readonly interventionOutcomeSelect: Locator;
  readonly interventionNotesInput: Locator;
  readonly saveInterventionButton: Locator;

  // Close gap controls
  readonly closeGapButton: Locator;
  readonly closeReasonSelect: Locator;
  readonly closeDateInput: Locator;
  readonly closeNotesInput: Locator;
  readonly confirmCloseButton: Locator;

  // Bulk operations
  readonly selectAllCheckbox: Locator;
  readonly bulkCloseButton: Locator;
  readonly bulkExportButton: Locator;
  readonly selectedCount: Locator;

  // Quick actions
  readonly viewPatientButton: Locator;
  readonly runEvaluationButton: Locator;

  constructor(page: Page) {
    super(page);

    // Filters
    this.urgencyFilter = page.locator('[data-testid="urgency-filter"], #urgencyFilter, [formcontrolname="urgency"]');
    this.typeFilter = page.locator('[data-testid="type-filter"], #typeFilter, [formcontrolname="gapType"]');
    this.statusFilter = page.locator('[data-testid="status-filter"], #statusFilter, [formcontrolname="status"]');
    this.patientFilter = page.locator('[data-testid="patient-filter"], #patientFilter');
    this.measureFilter = page.locator('[data-testid="measure-filter"], #measureFilter');
    this.clearFiltersButton = page.locator('[data-testid="clear-filters"], button:has-text("Clear Filters")');

    // Care gap list
    this.careGapTable = page.locator('[data-testid="care-gaps-table"], .care-gaps-table, table');
    this.careGapRows = page.locator('[data-testid="care-gap-row"], .care-gap-row, tbody tr');
    this.noGapsMessage = page.locator('[data-testid="no-gaps"], .no-gaps-message, :has-text("No care gaps")');
    this.gapCount = page.locator('[data-testid="gap-count"], .gap-count');

    // Pagination
    this.paginationControls = page.locator('[data-testid="pagination"], .pagination, mat-paginator');
    this.nextPageButton = page.locator('[data-testid="next-page"], button[aria-label="Next page"]');
    this.previousPageButton = page.locator('[data-testid="prev-page"], button[aria-label="Previous page"]');

    // Gap details
    this.gapDetailsPanel = page.locator('[data-testid="gap-details"], .gap-details-panel, aside');
    this.gapPatientName = page.locator('[data-testid="gap-patient-name"], .gap-patient-name');
    this.gapMeasureName = page.locator('[data-testid="gap-measure-name"], .gap-measure-name');
    this.gapUrgency = page.locator('[data-testid="gap-urgency"], .gap-urgency, .urgency-badge');
    this.gapType = page.locator('[data-testid="gap-type"], .gap-type');
    this.gapDueDate = page.locator('[data-testid="gap-due-date"], .gap-due-date');
    this.gapRecommendation = page.locator('[data-testid="gap-recommendation"], .gap-recommendation');
    this.gapHistory = page.locator('[data-testid="gap-history"], .gap-history, .intervention-history');

    // Intervention
    this.recordInterventionButton = page.locator('[data-testid="record-intervention"], button:has-text("Record Intervention")');
    this.interventionTypeSelect = page.locator('[data-testid="intervention-type"], #interventionType');
    this.interventionOutcomeSelect = page.locator('[data-testid="intervention-outcome"], #interventionOutcome');
    this.interventionNotesInput = page.locator('[data-testid="intervention-notes"], #interventionNotes, textarea[name="notes"]');
    this.saveInterventionButton = page.locator('[data-testid="save-intervention"], button:has-text("Save Intervention")');

    // Close gap
    this.closeGapButton = page.locator('[data-testid="close-gap"], button:has-text("Close Gap")');
    this.closeReasonSelect = page.locator('[data-testid="close-reason"], #closeReason');
    this.closeDateInput = page.locator('[data-testid="close-date"], #closeDate');
    this.closeNotesInput = page.locator('[data-testid="close-notes"], #closeNotes');
    this.confirmCloseButton = page.locator('[data-testid="confirm-close"], button:has-text("Submit Closure"), button:has-text("Confirm")');

    // Bulk operations
    this.selectAllCheckbox = page.locator('[data-testid="select-all"], th input[type="checkbox"]');
    this.bulkCloseButton = page.locator('[data-testid="bulk-close"], button:has-text("Bulk Close")');
    this.bulkExportButton = page.locator('[data-testid="bulk-export"], button:has-text("Export")');
    this.selectedCount = page.locator('[data-testid="selected-count"], .selected-count');

    // Quick actions
    this.viewPatientButton = page.locator('[data-testid="view-patient"], button:has-text("View Patient")');
    this.runEvaluationButton = page.locator('[data-testid="run-evaluation"], button:has-text("Re-evaluate")');
  }

  /**
   * Navigate to care gaps page
   */
  async goto(): Promise<void> {
    await this.page.goto('/care-gaps');
    await this.waitForLoad();
  }

  /**
   * Check if page is loaded
   */
  async isLoaded(): Promise<boolean> {
    return this.careGapTable.isVisible() || this.noGapsMessage.isVisible();
  }

  /**
   * Filter by urgency
   */
  async filterByUrgency(urgency: 'HIGH' | 'MEDIUM' | 'LOW' | 'ALL'): Promise<void> {
    await this.urgencyFilter.selectOption(urgency);
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Filter by gap type
   */
  async filterByType(type: string): Promise<void> {
    await this.typeFilter.selectOption(type);
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Filter by status
   */
  async filterByStatus(status: 'OPEN' | 'CLOSED' | 'IN_PROGRESS' | 'ALL'): Promise<void> {
    await this.statusFilter.selectOption(status);
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Search by patient
   */
  async filterByPatient(patientQuery: string): Promise<void> {
    await this.patientFilter.fill(patientQuery);
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Clear all filters
   */
  async clearFilters(): Promise<void> {
    await this.clearFiltersButton.click();
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Get number of care gaps displayed
   */
  async getGapCount(): Promise<number> {
    if (await this.noGapsMessage.isVisible()) {
      return 0;
    }
    return this.careGapRows.count();
  }

  /**
   * Select care gap by index
   */
  async selectGapByIndex(index: number): Promise<void> {
    const row = this.careGapRows.nth(index);
    await row.click();
    await this.gapDetailsPanel.waitFor({ state: 'visible' });
  }

  /**
   * Get gap details from selected gap
   */
  async getSelectedGapDetails(): Promise<{
    patientName: string;
    measureName: string;
    urgency: string;
    type: string;
    dueDate: string;
    recommendation: string;
  }> {
    return {
      patientName: await this.safeText(this.gapPatientName),
      measureName: await this.safeText(this.gapMeasureName),
      urgency: await this.gapUrgency.textContent() || '',
      type: await this.gapType.textContent() || '',
      dueDate: await this.gapDueDate.textContent() || '',
      recommendation: await this.gapRecommendation.textContent() || '',
    };
  }

  /**
   * Record an intervention
   */
  async recordIntervention(intervention: {
    type: string;
    outcome: string;
    notes?: string;
  }): Promise<void> {
    await this.recordInterventionButton.click();
    await this.interventionTypeSelect.selectOption(intervention.type);
    await this.interventionOutcomeSelect.selectOption(intervention.outcome);

    if (intervention.notes) {
      await this.interventionNotesInput.fill(intervention.notes);
    }

    await this.saveInterventionButton.click();
    await this.waitForSuccessMessage();
  }

  /**
   * Close the selected care gap
   */
  async closeGap(closure: {
    reason: string;
    notes?: string;
    date?: string;
  }): Promise<void> {
    await this.closeGapButton.click();
    await this.closeReasonSelect.selectOption(closure.reason);

    if (closure.date) {
      await this.closeDateInput.fill(closure.date);
    }

    if (closure.notes) {
      await this.closeNotesInput.fill(closure.notes);
    }

    await this.confirmCloseButton.click();
    await this.waitForSuccessMessage();
  }

  /**
   * Select multiple gaps for bulk operation
   */
  async selectGapsForBulk(indices: number[]): Promise<void> {
    for (const index of indices) {
      const row = this.careGapRows.nth(index);
      await row.locator('input[type="checkbox"]').check();
    }
  }

  /**
   * Select all gaps on current page
   */
  async selectAllGaps(): Promise<void> {
    await this.selectAllCheckbox.check();
  }

  /**
   * Bulk close selected gaps
   */
  async bulkCloseGaps(reason: string): Promise<void> {
    await this.bulkCloseButton.click();
    await this.closeReasonSelect.selectOption(reason);
    await this.confirmCloseButton.click();
    await this.waitForSuccessMessage();
  }

  /**
   * Export selected gaps
   */
  async exportGaps(): Promise<void> {
    await this.bulkExportButton.click();
  }

  /**
   * Get selected count for bulk operations
   */
  async getSelectedCount(): Promise<number> {
    const text = await this.selectedCount.textContent() || '0';
    return parseInt(text.replace(/[^0-9]/g, ''), 10);
  }

  /**
   * View patient from care gap
   */
  async viewPatient(): Promise<void> {
    await this.viewPatientButton.click();
    await this.page.waitForURL('**/patients/*');
  }

  /**
   * Run re-evaluation from care gap
   */
  async runReEvaluation(): Promise<void> {
    await this.runEvaluationButton.click();
    await this.page.waitForURL('**/evaluations*');
  }

  /**
   * Get intervention history
   */
  async getInterventionHistory(): Promise<string[]> {
    const items = this.gapHistory.locator('.intervention-item, li');
    const count = await items.count();
    const history: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await this.safeText(items.nth(i));
      history.push(text);
    }

    return history;
  }

  /**
   * Assert care gaps are displayed
   */
  async assertGapsDisplayed(): Promise<void> {
    await expect(this.careGapTable).toBeVisible();
    const count = await this.getGapCount();
    expect(count).toBeGreaterThan(0);
  }

  /**
   * Assert no care gaps
   */
  async assertNoGaps(): Promise<void> {
    await expect(this.noGapsMessage).toBeVisible();
  }

  /**
   * Assert gap closed successfully
   */
  async assertGapClosed(gapId: string): Promise<void> {
    // Gap should not be in open list anymore
    const row = this.careGapRows.filter({ hasText: gapId });
    await expect(row).not.toBeVisible();
  }

  /**
   * Assert intervention recorded
   */
  async assertInterventionRecorded(): Promise<void> {
    const history = await this.getInterventionHistory();
    expect(history.length).toBeGreaterThan(0);
  }
}
