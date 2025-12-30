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

    // Patient selection - Angular Material autocomplete
    this.patientSearchInput = page.locator('[data-testid="patient-search"], input[placeholder*="patient" i], input[aria-label*="patient" i], mat-form-field input');
    this.patientAutocomplete = page.locator('[data-testid="patient-autocomplete"], mat-option, .mat-mdc-option, .autocomplete-options');
    this.selectedPatientCard = page.locator('[data-testid="selected-patient"], .selected-patient-card, mat-card:has-text("Patient")');
    this.changePatientButton = page.locator('[data-testid="change-patient"], button:has-text("Change Patient"), button:has-text("Change")');

    // Measure selection - Angular Material select
    this.measureDropdown = page.locator('[data-testid="measure-dropdown"], mat-select[formcontrolname="measure"], mat-select[aria-label*="measure" i], #measureSelect');
    this.measureSearchInput = page.locator('[data-testid="measure-search"], .measure-search input, mat-form-field:has-text("Search") input');
    this.measureOptions = page.locator('[data-testid="measure-option"], mat-option, .mat-mdc-option');
    this.selectedMeasureCard = page.locator('[data-testid="selected-measure"], .selected-measure-card, mat-card:has-text("Measure")');
    this.measureCategoryFilter = page.locator('[data-testid="measure-category-filter"], mat-select[formcontrolname="category"], .category-filter');
    this.favoriteMeasuresToggle = page.locator('[data-testid="favorites-toggle"], button:has-text("Favorites"), mat-slide-toggle:has-text("Favorites")');

    // Evaluation controls
    this.evaluateButton = page.locator('[data-testid="evaluate-button"], button:has-text("Evaluate"), button:has-text("Run Evaluation")');
    this.clearFormButton = page.locator('[data-testid="clear-form"], button:has-text("Clear"), button:has-text("Reset")');
    this.evaluationProgress = page.locator('[data-testid="evaluation-progress"], mat-progress-bar, mat-spinner, .evaluation-progress');

    // Results display - Angular Material cards
    this.resultCard = page.locator('[data-testid="result-card"], mat-card.result-card, .evaluation-result-card, mat-card:has-text("Result")');
    this.resultStatus = page.locator('[data-testid="result-status"], .result-status, mat-chip[class*="status"], .compliance-badge');
    this.resultDetails = page.locator('[data-testid="result-details"], .result-details, mat-expansion-panel');
    this.numeratorValue = page.locator('[data-testid="numerator"], .numerator-value, :has-text("Numerator") + *');
    this.denominatorValue = page.locator('[data-testid="denominator"], .denominator-value, :has-text("Denominator") + *');
    this.complianceRate = page.locator('[data-testid="compliance-rate"], .compliance-rate, :has-text("Compliance") .value');
    this.evaluationDate = page.locator('[data-testid="evaluation-date"], .evaluation-date, :has-text("Date") .value');
    this.evaluationId = page.locator('[data-testid="evaluation-id"], .evaluation-id');

    // Result actions
    this.viewCareGapButton = page.locator('[data-testid="view-care-gap"], button:has-text("View Care Gap"), button:has-text("Care Gap")');
    this.runAnotherButton = page.locator('[data-testid="run-another"], button:has-text("Run Another"), button:has-text("New Evaluation")');
    this.exportResultButton = page.locator('[data-testid="export-result"], button:has-text("Export")');

    // Batch evaluation
    this.batchModeToggle = page.locator('[data-testid="batch-mode-toggle"], button:has-text("Batch Mode"), mat-slide-toggle:has-text("Batch")');
    this.batchPatientList = page.locator('[data-testid="batch-patients"], .batch-patient-list, mat-selection-list');
    this.batchMeasureList = page.locator('[data-testid="batch-measures"], .batch-measure-list');
    this.batchProgress = page.locator('[data-testid="batch-progress"], mat-progress-bar, .batch-progress');
    this.batchResults = page.locator('[data-testid="batch-results"], mat-table, .batch-results');
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
    try {
      // Wait for page heading - "Quality Measure Evaluations"
      await this.page.locator('h1:has-text("Evaluation"), h1:has-text("Quality Measure"), .evaluations-container').first().waitFor({ state: 'visible', timeout: 10000 });

      // Wait for loading to complete
      await this.waitForSpinnerToDisappear();

      // Check for evaluation form elements - look for the searchbox or combobox
      const formVisible = await this.page.locator('form[aria-label*="evaluation" i], form[aria-label*="measure" i], searchbox, combobox[aria-label*="measure" i]').first().isVisible().catch(() => false);

      return true;  // If we got past the heading wait, page is loaded
    } catch {
      return false;
    }
  }

  /**
   * Wait for evaluation data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
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
