import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Evaluation Page Object Model
 *
 * Handles quality measure evaluation workflows:
 * - Select patient
 * - Select quality measure
 * - Submit evaluation
 * - View results
 * - Batch evaluations
 */
export class EvaluationPage extends BasePage {
  // Patient selection
  readonly patientSearchInput: Locator;
  readonly patientAutocomplete: Locator;
  readonly selectedPatientCard: Locator;
  readonly changePatientButton: Locator;

  // Measure selection
  readonly measureDropdown: Locator;
  readonly measureSearchInput: Locator;
  readonly measureOptions: Locator;
  readonly selectedMeasureCard: Locator;
  readonly measureCategoryFilter: Locator;
  readonly favoriteMeasuresToggle: Locator;

  // Evaluation controls
  readonly evaluateButton: Locator;
  readonly clearFormButton: Locator;
  readonly evaluationProgress: Locator;

  // Results display
  readonly resultCard: Locator;
  readonly resultStatus: Locator;
  readonly resultDetails: Locator;
  readonly numeratorValue: Locator;
  readonly denominatorValue: Locator;
  readonly complianceRate: Locator;
  readonly evaluationDate: Locator;
  readonly evaluationId: Locator;

  // Result actions
  readonly viewCareGapButton: Locator;
  readonly runAnotherButton: Locator;
  readonly exportResultButton: Locator;

  // Batch evaluation
  readonly batchModeToggle: Locator;
  readonly batchPatientList: Locator;
  readonly batchMeasureList: Locator;
  readonly batchProgress: Locator;
  readonly batchResults: Locator;

  constructor(page: Page) {
    super(page);

    // Patient selection
    this.patientSearchInput = page.locator('[data-testid="patient-search"], #patientSearch, [placeholder*="patient"]');
    this.patientAutocomplete = page.locator('[data-testid="patient-autocomplete"], .autocomplete-options, mat-option');
    this.selectedPatientCard = page.locator('[data-testid="selected-patient"], .selected-patient-card');
    this.changePatientButton = page.locator('[data-testid="change-patient"], button:has-text("Change Patient")');

    // Measure selection
    this.measureDropdown = page.locator('[data-testid="measure-dropdown"], #measureSelect, [formcontrolname="measure"]');
    this.measureSearchInput = page.locator('[data-testid="measure-search"], .measure-search input');
    this.measureOptions = page.locator('[data-testid="measure-option"], .measure-option, mat-option');
    this.selectedMeasureCard = page.locator('[data-testid="selected-measure"], .selected-measure-card');
    this.measureCategoryFilter = page.locator('[data-testid="measure-category-filter"], .category-filter');
    this.favoriteMeasuresToggle = page.locator('[data-testid="favorites-toggle"], button:has-text("Favorites")');

    // Evaluation controls
    this.evaluateButton = page.locator('[data-testid="evaluate-button"], button:has-text("Evaluate")');
    this.clearFormButton = page.locator('[data-testid="clear-form"], button:has-text("Clear")');
    this.evaluationProgress = page.locator('[data-testid="evaluation-progress"], .evaluation-progress');

    // Results display
    this.resultCard = page.locator('[data-testid="result-card"], .evaluation-result-card');
    this.resultStatus = page.locator('[data-testid="result-status"], .result-status, .compliance-badge');
    this.resultDetails = page.locator('[data-testid="result-details"], .result-details');
    this.numeratorValue = page.locator('[data-testid="numerator"], .numerator-value');
    this.denominatorValue = page.locator('[data-testid="denominator"], .denominator-value');
    this.complianceRate = page.locator('[data-testid="compliance-rate"], .compliance-rate');
    this.evaluationDate = page.locator('[data-testid="evaluation-date"], .evaluation-date');
    this.evaluationId = page.locator('[data-testid="evaluation-id"], .evaluation-id');

    // Result actions
    this.viewCareGapButton = page.locator('[data-testid="view-care-gap"], button:has-text("View Care Gap")');
    this.runAnotherButton = page.locator('[data-testid="run-another"], button:has-text("Run Another")');
    this.exportResultButton = page.locator('[data-testid="export-result"], button:has-text("Export")');

    // Batch evaluation
    this.batchModeToggle = page.locator('[data-testid="batch-mode-toggle"], button:has-text("Batch Mode")');
    this.batchPatientList = page.locator('[data-testid="batch-patients"], .batch-patient-list');
    this.batchMeasureList = page.locator('[data-testid="batch-measures"], .batch-measure-list');
    this.batchProgress = page.locator('[data-testid="batch-progress"], .batch-progress');
    this.batchResults = page.locator('[data-testid="batch-results"], .batch-results');
  }

  /**
   * Navigate to evaluations page
   */
  async goto(): Promise<void> {
    await this.page.goto('/evaluations');
    await this.waitForLoad();
  }

  /**
   * Check if page is loaded
   */
  async isLoaded(): Promise<boolean> {
    return this.patientSearchInput.isVisible();
  }

  /**
   * Search and select patient
   */
  async selectPatient(searchQuery: string): Promise<void> {
    await this.patientSearchInput.fill(searchQuery);
    await this.patientAutocomplete.first().waitFor({ state: 'visible' });
    await this.patientAutocomplete.first().click();
    await this.selectedPatientCard.waitFor({ state: 'visible' });
  }

  /**
   * Get selected patient info
   */
  async getSelectedPatientInfo(): Promise<string> {
    return this.safeText(this.selectedPatientCard);
  }

  /**
   * Clear selected patient
   */
  async changePatient(): Promise<void> {
    await this.changePatientButton.click();
    await this.patientSearchInput.waitFor({ state: 'visible' });
  }

  /**
   * Select quality measure by code or name
   */
  async selectMeasure(measureCodeOrName: string): Promise<void> {
    await this.measureDropdown.click();

    // Search for measure if search input is available
    if (await this.measureSearchInput.isVisible()) {
      await this.measureSearchInput.fill(measureCodeOrName);
    }

    await this.measureOptions.filter({ hasText: measureCodeOrName }).first().click();
    await this.selectedMeasureCard.waitFor({ state: 'visible' });
  }

  /**
   * Filter measures by category
   */
  async filterByCategory(category: string): Promise<void> {
    await this.measureCategoryFilter.selectOption(category);
  }

  /**
   * Toggle favorite measures view
   */
  async showFavoritesOnly(): Promise<void> {
    await this.favoriteMeasuresToggle.click();
  }

  /**
   * Submit evaluation
   */
  async evaluate(): Promise<void> {
    await this.evaluateButton.click();
    await this.evaluationProgress.waitFor({ state: 'visible' });
    await this.evaluationProgress.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Run complete evaluation workflow
   */
  async runEvaluation(patientQuery: string, measureCode: string): Promise<void> {
    await this.selectPatient(patientQuery);
    await this.selectMeasure(measureCode);
    await this.evaluate();
    await this.resultCard.waitFor({ state: 'visible' });
  }

  /**
   * Get evaluation result status
   */
  async getResultStatus(): Promise<'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE'> {
    const statusText = await this.resultStatus.textContent() || '';
    const normalized = statusText.toUpperCase().replace(/[\s-]/g, '_');

    if (normalized.includes('COMPLIANT') && !normalized.includes('NON')) {
      return 'COMPLIANT';
    } else if (normalized.includes('NON_COMPLIANT') || normalized.includes('NONCOMPLIANT')) {
      return 'NON_COMPLIANT';
    }
    return 'NOT_ELIGIBLE';
  }

  /**
   * Get evaluation result details
   */
  async getResultDetails(): Promise<{
    status: string;
    numerator: boolean;
    denominator: boolean;
    complianceRate: number;
    evaluationId: string;
  }> {
    return {
      status: await this.resultStatus.textContent() || '',
      numerator: (await this.numeratorValue.textContent() || '').toLowerCase() === 'true',
      denominator: (await this.denominatorValue.textContent() || '').toLowerCase() === 'true',
      complianceRate: parseFloat((await this.complianceRate.textContent() || '0').replace(/[^0-9.]/g, '')),
      evaluationId: await this.evaluationId.textContent() || '',
    };
  }

  /**
   * Navigate to care gap from result
   */
  async viewCareGap(): Promise<void> {
    await this.viewCareGapButton.click();
    await this.page.waitForURL('**/care-gaps/*');
  }

  /**
   * Start another evaluation
   */
  async runAnother(): Promise<void> {
    await this.runAnotherButton.click();
    await this.patientSearchInput.waitFor({ state: 'visible' });
  }

  /**
   * Export result
   */
  async exportResult(): Promise<void> {
    await this.exportResultButton.click();
  }

  /**
   * Clear the evaluation form
   */
  async clearForm(): Promise<void> {
    await this.clearFormButton.click();
  }

  /**
   * Enable batch evaluation mode
   */
  async enableBatchMode(): Promise<void> {
    await this.batchModeToggle.click();
    await this.batchPatientList.waitFor({ state: 'visible' });
  }

  /**
   * Wait for batch evaluation to complete
   */
  async waitForBatchComplete(timeout?: number): Promise<void> {
    await this.batchProgress.waitFor({ state: 'visible' });
    await this.batchResults.waitFor({ state: 'visible', timeout: timeout || 120000 });
  }

  /**
   * Assert result is displayed
   */
  async assertResultDisplayed(): Promise<void> {
    await expect(this.resultCard).toBeVisible();
    await expect(this.resultStatus).toBeVisible();
  }

  /**
   * Assert patient is selected
   */
  async assertPatientSelected(): Promise<void> {
    await expect(this.selectedPatientCard).toBeVisible();
  }

  /**
   * Assert measure is selected
   */
  async assertMeasureSelected(): Promise<void> {
    await expect(this.selectedMeasureCard).toBeVisible();
  }

  /**
   * Assert compliant result
   */
  async assertCompliantResult(): Promise<void> {
    const status = await this.getResultStatus();
    expect(status).toBe('COMPLIANT');
  }

  /**
   * Assert non-compliant result
   */
  async assertNonCompliantResult(): Promise<void> {
    const status = await this.getResultStatus();
    expect(status).toBe('NON_COMPLIANT');
  }

  /**
   * Check if evaluate button is enabled
   */
  async isEvaluateButtonEnabled(): Promise<boolean> {
    return this.evaluateButton.isEnabled();
  }
}
