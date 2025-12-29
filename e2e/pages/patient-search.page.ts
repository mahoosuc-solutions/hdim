import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Patient Search Page Object Model
 *
 * Handles patient search and selection workflows:
 * - Search by name, MRN, DOB
 * - Filter results
 * - Select patient for evaluation
 * - View patient details
 */
export class PatientSearchPage extends BasePage {
  // Search elements
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly clearSearchButton: Locator;
  readonly advancedSearchToggle: Locator;

  // Advanced search fields
  readonly mrnInput: Locator;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly dobInput: Locator;
  readonly genderSelect: Locator;

  // Results elements
  readonly resultsTable: Locator;
  readonly resultsRows: Locator;
  readonly noResultsMessage: Locator;
  readonly resultCount: Locator;

  // Pagination
  readonly paginationControls: Locator;
  readonly nextPageButton: Locator;
  readonly previousPageButton: Locator;
  readonly pageSizeSelector: Locator;

  // Action buttons
  readonly selectPatientButton: Locator;
  readonly viewDetailsButton: Locator;
  readonly batchSelectCheckbox: Locator;
  readonly batchEvaluateButton: Locator;

  constructor(page: Page) {
    super(page);

    // Search
    this.searchInput = page.locator('[data-testid="patient-search-input"], #patientSearch, input[placeholder*="Search"]');
    this.searchButton = page.locator('[data-testid="search-button"], button:has-text("Search")');
    this.clearSearchButton = page.locator('[data-testid="clear-search"], button[aria-label="Clear"]');
    this.advancedSearchToggle = page.locator('[data-testid="advanced-search-toggle"], button:has-text("Advanced")');

    // Advanced search
    this.mrnInput = page.locator('[data-testid="mrn-input"], #mrn, input[name="mrn"]');
    this.firstNameInput = page.locator('[data-testid="first-name-input"], #firstName, input[name="firstName"]');
    this.lastNameInput = page.locator('[data-testid="last-name-input"], #lastName, input[name="lastName"]');
    this.dobInput = page.locator('[data-testid="dob-input"], #dob, input[name="dateOfBirth"]');
    this.genderSelect = page.locator('[data-testid="gender-select"], #gender, select[name="gender"]');

    // Results
    this.resultsTable = page.locator('[data-testid="patients-table"], .patients-table, table');
    this.resultsRows = page.locator('[data-testid="patient-row"], .patient-row, tbody tr');
    this.noResultsMessage = page.locator('[data-testid="no-results"], .no-results, :has-text("No patients found")');
    this.resultCount = page.locator('[data-testid="result-count"], .result-count');

    // Pagination
    this.paginationControls = page.locator('[data-testid="pagination"], .pagination, mat-paginator');
    this.nextPageButton = page.locator('[data-testid="next-page"], button[aria-label="Next page"]');
    this.previousPageButton = page.locator('[data-testid="prev-page"], button[aria-label="Previous page"]');
    this.pageSizeSelector = page.locator('[data-testid="page-size"], .page-size-selector');

    // Actions
    this.selectPatientButton = page.locator('[data-testid="select-patient"], button:has-text("Select")');
    this.viewDetailsButton = page.locator('[data-testid="view-details"], button:has-text("View Details")');
    this.batchSelectCheckbox = page.locator('[data-testid="batch-select-all"], th input[type="checkbox"]');
    this.batchEvaluateButton = page.locator('[data-testid="batch-evaluate"], button:has-text("Batch Evaluate")');
  }

  /**
   * Navigate to patient search page
   */
  async goto(): Promise<void> {
    await this.page.goto('/patients');
    await this.waitForLoad();
  }

  /**
   * Check if page is loaded
   */
  async isLoaded(): Promise<boolean> {
    return this.searchInput.isVisible();
  }

  /**
   * Search for patient by name or MRN
   */
  async search(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchButton.click();
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Search using keyboard enter
   */
  async searchWithEnter(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Clear search and results
   */
  async clearSearch(): Promise<void> {
    if (await this.clearSearchButton.isVisible()) {
      await this.clearSearchButton.click();
    } else {
      await this.searchInput.clear();
    }
  }

  /**
   * Open advanced search panel
   */
  async openAdvancedSearch(): Promise<void> {
    if (await this.advancedSearchToggle.isVisible()) {
      await this.advancedSearchToggle.click();
      await this.mrnInput.waitFor({ state: 'visible' });
    }
  }

  /**
   * Perform advanced search
   */
  async advancedSearch(criteria: {
    mrn?: string;
    firstName?: string;
    lastName?: string;
    dob?: string;
    gender?: string;
  }): Promise<void> {
    await this.openAdvancedSearch();

    if (criteria.mrn) await this.mrnInput.fill(criteria.mrn);
    if (criteria.firstName) await this.firstNameInput.fill(criteria.firstName);
    if (criteria.lastName) await this.lastNameInput.fill(criteria.lastName);
    if (criteria.dob) await this.dobInput.fill(criteria.dob);
    if (criteria.gender) await this.genderSelect.selectOption(criteria.gender);

    await this.searchButton.click();
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Get number of search results
   */
  async getResultCount(): Promise<number> {
    if (await this.noResultsMessage.isVisible()) {
      return 0;
    }

    const countText = await this.resultCount.textContent();
    if (countText) {
      const match = countText.match(/(\d+)/);
      return match ? parseInt(match[1], 10) : 0;
    }

    return this.resultsRows.count();
  }

  /**
   * Select patient by row index
   */
  async selectPatientByIndex(index: number): Promise<void> {
    const row = this.resultsRows.nth(index);
    await row.click();
    await this.selectPatientButton.click();
  }

  /**
   * Select patient by MRN
   */
  async selectPatientByMrn(mrn: string): Promise<void> {
    const row = this.resultsRows.filter({ hasText: mrn });
    await row.click();
    await this.selectPatientButton.click();
  }

  /**
   * View patient details by index
   */
  async viewPatientDetailsByIndex(index: number): Promise<void> {
    const row = this.resultsRows.nth(index);
    await row.locator('[data-testid="view-details"], button:has-text("View")').click();
    await this.page.waitForURL('**/patients/*');
  }

  /**
   * Get patient data from results row
   */
  async getPatientDataByIndex(index: number): Promise<{
    name: string;
    mrn: string;
    dob: string;
    gender: string;
  }> {
    const row = this.resultsRows.nth(index);
    const cells = row.locator('td');

    return {
      name: await this.safeText(cells.nth(0)),
      mrn: await this.safeText(cells.nth(1)),
      dob: await this.safeText(cells.nth(2)),
      gender: await this.safeText(cells.nth(3)),
    };
  }

  /**
   * Select multiple patients for batch operation
   */
  async selectPatientsForBatch(indices: number[]): Promise<void> {
    for (const index of indices) {
      const row = this.resultsRows.nth(index);
      await row.locator('input[type="checkbox"]').check();
    }
  }

  /**
   * Select all patients on current page
   */
  async selectAllPatients(): Promise<void> {
    await this.batchSelectCheckbox.check();
  }

  /**
   * Start batch evaluation for selected patients
   */
  async startBatchEvaluation(): Promise<void> {
    await this.batchEvaluateButton.click();
  }

  /**
   * Go to next page of results
   */
  async nextPage(): Promise<void> {
    await this.nextPageButton.click();
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Go to previous page of results
   */
  async previousPage(): Promise<void> {
    await this.previousPageButton.click();
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Change page size
   */
  async setPageSize(size: number): Promise<void> {
    await this.pageSizeSelector.selectOption(size.toString());
    await this.waitForSpinnerToDisappear();
  }

  /**
   * Assert search results are displayed
   */
  async assertResultsDisplayed(): Promise<void> {
    await expect(this.resultsTable).toBeVisible();
    const count = await this.resultsRows.count();
    expect(count).toBeGreaterThan(0);
  }

  /**
   * Assert no results found
   */
  async assertNoResults(): Promise<void> {
    await expect(this.noResultsMessage).toBeVisible();
  }

  /**
   * Assert specific patient in results
   */
  async assertPatientInResults(mrn: string): Promise<void> {
    const row = this.resultsRows.filter({ hasText: mrn });
    await expect(row).toBeVisible();
  }

  /**
   * Wait for search results to load
   */
  async waitForResults(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForSelector('[data-testid="patients-table"] tbody tr, [data-testid="no-results"]');
  }
}
