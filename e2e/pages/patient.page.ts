import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Patient Page Object
 *
 * Handles patient list and detail page interactions.
 * Used for PAT-001 to PAT-005 test workflows.
 */
export class PatientPage extends BasePage {
  // Page URL
  readonly url = '/patients';

  // Patient List Locators
  readonly pageHeading: Locator;
  readonly patientTable: Locator;
  readonly patientRows: Locator;
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly filterButton: Locator;
  readonly createPatientButton: Locator;
  readonly exportButton: Locator;
  readonly refreshButton: Locator;

  // Pagination
  readonly paginationContainer: Locator;
  readonly nextPageButton: Locator;
  readonly prevPageButton: Locator;
  readonly pageSizeSelect: Locator;
  readonly pageInfo: Locator;

  // Filters
  readonly statusFilter: Locator;
  readonly genderFilter: Locator;
  readonly ageFilter: Locator;
  readonly riskFilter: Locator;
  readonly clearFiltersButton: Locator;

  // Patient Detail
  readonly patientName: Locator;
  readonly patientMRN: Locator;
  readonly patientDOB: Locator;
  readonly patientGender: Locator;
  readonly patientStatus: Locator;
  readonly careGapsTab: Locator;
  readonly conditionsTab: Locator;
  readonly medicationsTab: Locator;
  readonly evaluationsTab: Locator;
  readonly riskScoreCard: Locator;

  // Modals
  readonly patientModal: Locator;
  readonly confirmDialog: Locator;

  constructor(page: Page) {
    super(page);

    // List page elements - Angular Material structure matching patients.component.html
    this.pageHeading = page.locator('h1.page-title, h1:has-text("Patient Management")');
    this.patientTable = page.locator('table.patients-table, table[mat-table], .mat-mdc-table');
    this.patientRows = page.locator('tr.patient-row, tr[mat-row], .mat-mdc-row');
    this.searchInput = page.locator('.search-field input, input[placeholder*="Search patients"]');
    this.searchButton = page.locator('.search-field mat-icon, button:has-text("Search")');
    this.filterButton = page.locator('button:has-text("Reset"), app-loading-button:has-text("Reset")');
    this.createPatientButton = page.locator('button:has-text("Add Patient"), button:has-text("New Patient")');
    this.exportButton = page.locator('button:has-text("Export"), app-loading-button:has-text("Export")');
    this.refreshButton = page.locator('button:has-text("Retry"), app-loading-button:has-text("Retry")');

    // Pagination - Angular Material paginator
    this.paginationContainer = page.locator('mat-paginator, .mat-mdc-paginator');
    this.nextPageButton = page.locator('button[aria-label*="Next" i], .mat-mdc-paginator-navigation-next');
    this.prevPageButton = page.locator('button[aria-label*="Previous" i], .mat-mdc-paginator-navigation-previous');
    this.pageSizeSelect = page.locator('.mat-mdc-paginator-page-size-select, mat-select');
    this.pageInfo = page.locator('.mat-mdc-paginator-range-label');

    // Filters - Angular Material form fields
    this.statusFilter = page.locator('mat-select[formcontrolname="status"], mat-select[aria-label*="status" i]');
    this.genderFilter = page.locator('mat-select[formcontrolname="gender"], mat-select[aria-label*="gender" i]');
    this.ageFilter = page.locator('mat-select[formcontrolname="ageRange"], mat-select[aria-label*="age" i]');
    this.riskFilter = page.locator('mat-select[formcontrolname="riskLevel"], mat-select[aria-label*="risk" i]');
    this.clearFiltersButton = page.locator('button:has-text("Reset"), button:has-text("Clear")');

    // Patient Detail
    this.patientName = page.locator('[data-testid="patient-name"], .patient-name, h1');
    this.patientMRN = page.locator('[data-testid="patient-mrn"], .patient-mrn');
    this.patientDOB = page.locator('[data-testid="patient-dob"], .patient-dob');
    this.patientGender = page.locator('[data-testid="patient-gender"], .patient-gender');
    this.patientStatus = page.locator('[data-testid="patient-status"], .patient-status');
    this.careGapsTab = page.locator('[data-testid="care-gaps-tab"], [role="tab"]:has-text("Care Gaps")');
    this.conditionsTab = page.locator('[data-testid="conditions-tab"], [role="tab"]:has-text("Conditions")');
    this.medicationsTab = page.locator('[data-testid="medications-tab"], [role="tab"]:has-text("Medications")');
    this.evaluationsTab = page.locator('[data-testid="evaluations-tab"], [role="tab"]:has-text("Evaluations")');
    this.riskScoreCard = page.locator('[data-testid="risk-score"], .risk-score-card');

    // Modals
    this.patientModal = page.locator('[data-testid="patient-modal"], [role="dialog"]');
    this.confirmDialog = page.locator('[data-testid="confirm-dialog"], .confirm-dialog');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    try {
      // Wait for page container
      await this.page.locator('.patients-container').first().waitFor({ state: 'visible', timeout: 10000 });

      // Wait for loading overlay to disappear
      const loadingOverlay = this.page.locator('app-loading-overlay[ng-reflect-is-loading="true"], .loading-overlay:visible');
      await loadingOverlay.waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {});

      // Wait for either table or empty state to appear
      const tableOrEmpty = this.page.locator('table.patients-table, .empty-state-card, .stat-card');
      await tableOrEmpty.first().waitFor({ state: 'visible', timeout: 10000 });

      return true;
    } catch {
      return false;
    }
  }

  /**
   * Wait for patient data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Search for patients
   */
  async searchPatients(query: string): Promise<void> {
    await this.searchInput.fill(query);
    if (await this.searchButton.count() > 0) {
      await this.searchButton.click();
    } else {
      await this.page.keyboard.press('Enter');
    }
    await this.waitForDataLoad();
  }

  /**
   * Get patient count from table
   */
  async getPatientCount(): Promise<number> {
    await this.waitForDataLoad();
    return this.patientRows.count();
  }

  /**
   * Click on a specific patient row
   */
  async selectPatient(index: number = 0): Promise<void> {
    await this.patientRows.nth(index).click();
    await this.waitForLoad();
  }

  /**
   * Select patient by name
   */
  async selectPatientByName(name: string): Promise<void> {
    const row = this.patientRows.filter({ hasText: name });
    await row.first().click();
    await this.waitForLoad();
  }

  /**
   * Get patient name from row
   */
  async getPatientNameFromRow(index: number = 0): Promise<string> {
    const nameCell = this.patientRows.nth(index).locator('td').first();
    return this.safeText(nameCell);
  }

  /**
   * Navigate to next page
   */
  async nextPage(): Promise<void> {
    if (await this.nextPageButton.isEnabled()) {
      await this.nextPageButton.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Navigate to previous page
   */
  async previousPage(): Promise<void> {
    if (await this.prevPageButton.isEnabled()) {
      await this.prevPageButton.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Change page size
   */
  async setPageSize(size: number): Promise<void> {
    await this.pageSizeSelect.click();
    await this.page.locator(`[role="option"]:has-text("${size}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Apply status filter
   */
  async filterByStatus(status: string): Promise<void> {
    await this.statusFilter.click();
    await this.page.locator(`[role="option"]:has-text("${status}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Apply risk level filter
   */
  async filterByRisk(riskLevel: string): Promise<void> {
    await this.riskFilter.click();
    await this.page.locator(`[role="option"]:has-text("${riskLevel}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Clear all filters
   */
  async clearFilters(): Promise<void> {
    if (await this.clearFiltersButton.isVisible()) {
      await this.clearFiltersButton.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Open create patient modal
   */
  async openCreatePatientModal(): Promise<void> {
    await this.createPatientButton.click();
    await this.patientModal.waitFor({ state: 'visible' });
  }

  /**
   * Export patients
   */
  async exportPatients(format: 'csv' | 'excel' = 'csv'): Promise<void> {
    await this.exportButton.click();
    const formatOption = this.page.locator(`[role="menuitem"]:has-text("${format}"), button:has-text("${format}")`);
    if (await formatOption.count() > 0) {
      await formatOption.click();
    }
  }

  /**
   * Go to patient detail page
   */
  async gotoPatientDetail(patientId: string): Promise<void> {
    await this.page.goto(`/patients/${patientId}`);
    await this.waitForLoad();
  }

  /**
   * Get risk score from patient detail
   */
  async getRiskScore(): Promise<string> {
    const score = this.riskScoreCard.locator('.score-value, [data-testid="score-value"]');
    if (await score.count() > 0) {
      return score.textContent() || '';
    }
    return '';
  }

  /**
   * Navigate to care gaps tab
   */
  async gotoCareGapsTab(): Promise<void> {
    await this.careGapsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to conditions tab
   */
  async gotoConditionsTab(): Promise<void> {
    await this.conditionsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to medications tab
   */
  async gotoMedicationsTab(): Promise<void> {
    await this.medicationsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Sort patients by column
   */
  async sortByColumn(columnName: string): Promise<void> {
    const header = this.patientTable.locator(`th:has-text("${columnName}")`);
    await header.click();
    await this.waitForDataLoad();
  }

  /**
   * Check if patient exists in list
   */
  async patientExists(name: string): Promise<boolean> {
    const row = this.patientRows.filter({ hasText: name });
    return (await row.count()) > 0;
  }
}
