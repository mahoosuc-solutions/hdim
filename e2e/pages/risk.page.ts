import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Risk Stratification Page Object
 *
 * Handles risk scoring, HCC risk adjustment, and population stratification.
 * Used for RISK-001 to RISK-003 test workflows.
 */
export class RiskPage extends BasePage {
  readonly url = '/risk';

  // Navigation
  readonly pageHeading: Locator;
  readonly stratificationTab: Locator;
  readonly hccTab: Locator;
  readonly trendsTab: Locator;

  // Risk Dashboard
  readonly riskSummaryCards: Locator;
  readonly highRiskCount: Locator;
  readonly mediumRiskCount: Locator;
  readonly lowRiskCount: Locator;
  readonly averageRafScore: Locator;

  // Patient Risk List
  readonly riskTable: Locator;
  readonly riskRows: Locator;
  readonly riskLevelFilter: Locator;
  readonly sortByRiskButton: Locator;
  readonly searchInput: Locator;

  // Risk Details
  readonly patientRiskCard: Locator;
  readonly riskScore: Locator;
  readonly riskLevel: Locator;
  readonly riskFactors: Locator;
  readonly hccCodes: Locator;
  readonly rafScore: Locator;

  // HCC Risk Adjustment
  readonly hccDashboard: Locator;
  readonly hccCategories: Locator;
  readonly hccTrends: Locator;
  readonly recalculateButton: Locator;
  readonly modelVersionSelect: Locator;

  // Population Stratification
  readonly stratificationChart: Locator;
  readonly riskDistribution: Locator;
  readonly cohortSelector: Locator;
  readonly exportButton: Locator;

  // Risk Recommendations
  readonly recommendationsPanel: Locator;
  readonly recommendationItems: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation
    this.pageHeading = page.locator('h1, h2').filter({ hasText: /risk/i });
    this.stratificationTab = page.locator('[data-testid="stratification-tab"], [role="tab"]:has-text("Stratification")');
    this.hccTab = page.locator('[data-testid="hcc-tab"], [role="tab"]:has-text("HCC")');
    this.trendsTab = page.locator('[data-testid="trends-tab"], [role="tab"]:has-text("Trends")');

    // Risk Dashboard
    this.riskSummaryCards = page.locator('[data-testid="risk-summary"], .risk-summary');
    this.highRiskCount = page.locator('[data-testid="high-risk-count"], .high-risk .count');
    this.mediumRiskCount = page.locator('[data-testid="medium-risk-count"], .medium-risk .count');
    this.lowRiskCount = page.locator('[data-testid="low-risk-count"], .low-risk .count');
    this.averageRafScore = page.locator('[data-testid="avg-raf"], .average-raf');

    // Patient Risk List
    this.riskTable = page.locator('[data-testid="risk-table"], .risk-table, table');
    this.riskRows = page.locator('[data-testid="risk-row"], .risk-row, tbody tr');
    this.riskLevelFilter = page.locator('[data-testid="risk-level-filter"], #riskLevel');
    this.sortByRiskButton = page.locator('[data-testid="sort-risk"], th:has-text("Risk")');
    this.searchInput = page.locator('[data-testid="risk-search"], input[placeholder*="Search"]');

    // Risk Details
    this.patientRiskCard = page.locator('[data-testid="patient-risk-card"], .patient-risk');
    this.riskScore = page.locator('[data-testid="risk-score"], .risk-score');
    this.riskLevel = page.locator('[data-testid="risk-level"], .risk-level');
    this.riskFactors = page.locator('[data-testid="risk-factors"], .risk-factors');
    this.hccCodes = page.locator('[data-testid="hcc-codes"], .hcc-codes');
    this.rafScore = page.locator('[data-testid="raf-score"], .raf-score');

    // HCC Risk Adjustment
    this.hccDashboard = page.locator('[data-testid="hcc-dashboard"], .hcc-dashboard');
    this.hccCategories = page.locator('[data-testid="hcc-category"], .hcc-category');
    this.hccTrends = page.locator('[data-testid="hcc-trends"], .hcc-trends');
    this.recalculateButton = page.locator('[data-testid="recalculate"], button:has-text("Recalculate")');
    this.modelVersionSelect = page.locator('[data-testid="model-version"], #modelVersion');

    // Population Stratification
    this.stratificationChart = page.locator('[data-testid="stratification-chart"], .stratification-chart');
    this.riskDistribution = page.locator('[data-testid="risk-distribution"], .risk-distribution');
    this.cohortSelector = page.locator('[data-testid="cohort-select"], #cohort');
    this.exportButton = page.locator('[data-testid="export"], button:has-text("Export")');

    // Risk Recommendations
    this.recommendationsPanel = page.locator('[data-testid="recommendations"], .recommendations');
    this.recommendationItems = page.locator('[data-testid="recommendation-item"], .recommendation-item');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    return (await this.pageHeading.count()) > 0 || (await this.riskTable.count()) > 0;
  }

  /**
   * Wait for risk data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Navigate to stratification view
   */
  async gotoStratification(): Promise<void> {
    await this.stratificationTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to HCC view
   */
  async gotoHCC(): Promise<void> {
    await this.hccTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to trends view
   */
  async gotoTrends(): Promise<void> {
    await this.trendsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Get high risk patient count
   */
  async getHighRiskCount(): Promise<number> {
    if (await this.highRiskCount.count() > 0) {
      const text = await this.highRiskCount.textContent();
      return parseInt(text || '0', 10);
    }
    return 0;
  }

  /**
   * Get medium risk patient count
   */
  async getMediumRiskCount(): Promise<number> {
    if (await this.mediumRiskCount.count() > 0) {
      const text = await this.mediumRiskCount.textContent();
      return parseInt(text || '0', 10);
    }
    return 0;
  }

  /**
   * Get low risk patient count
   */
  async getLowRiskCount(): Promise<number> {
    if (await this.lowRiskCount.count() > 0) {
      const text = await this.lowRiskCount.textContent();
      return parseInt(text || '0', 10);
    }
    return 0;
  }

  /**
   * Get average RAF score
   */
  async getAverageRAF(): Promise<number> {
    if (await this.averageRafScore.count() > 0) {
      const text = await this.averageRafScore.textContent();
      return parseFloat(text || '0');
    }
    return 0;
  }

  /**
   * Filter by risk level
   */
  async filterByRiskLevel(level: 'HIGH' | 'MEDIUM' | 'LOW'): Promise<void> {
    await this.riskLevelFilter.click();
    await this.page.locator(`[role="option"]:has-text("${level}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Sort by risk score
   */
  async sortByRisk(descending: boolean = true): Promise<void> {
    await this.sortByRiskButton.click();
    if (descending) {
      // Click again for descending if needed
      const sortIcon = this.sortByRiskButton.locator('.sort-icon, mat-icon');
      const iconText = await sortIcon.textContent();
      if (iconText?.includes('asc') || iconText?.includes('up')) {
        await this.sortByRiskButton.click();
      }
    }
    await this.waitForDataLoad();
  }

  /**
   * Select a patient from risk list
   */
  async selectPatient(index: number = 0): Promise<void> {
    await this.riskRows.nth(index).click();
    await this.waitForDataLoad();
  }

  /**
   * Get patient risk score from detail view
   */
  async getPatientRiskScore(): Promise<string> {
    if (await this.riskScore.count() > 0) {
      return (await this.riskScore.textContent()) || '';
    }
    return '';
  }

  /**
   * Get patient RAF score
   */
  async getPatientRAFScore(): Promise<string> {
    if (await this.rafScore.count() > 0) {
      return (await this.rafScore.textContent()) || '';
    }
    return '';
  }

  /**
   * Get HCC codes for patient
   */
  async getHCCCodes(): Promise<string[]> {
    const codes: string[] = [];
    const codeElements = await this.hccCodes.locator('.hcc-code, li').all();
    for (const element of codeElements) {
      const text = await element.textContent();
      if (text) {
        codes.push(text.trim());
      }
    }
    return codes;
  }

  /**
   * Recalculate risk scores
   */
  async recalculateRisk(): Promise<void> {
    await this.recalculateButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Select HCC model version
   */
  async selectModelVersion(version: string): Promise<void> {
    await this.modelVersionSelect.click();
    await this.page.locator(`[role="option"]:has-text("${version}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Select cohort for stratification
   */
  async selectCohort(cohort: string): Promise<void> {
    await this.cohortSelector.click();
    await this.page.locator(`[role="option"]:has-text("${cohort}")`).click();
    await this.waitForDataLoad();
  }

  /**
   * Export risk data
   */
  async exportRiskData(): Promise<void> {
    await this.exportButton.click();
  }

  /**
   * Get risk recommendations
   */
  async getRecommendations(): Promise<string[]> {
    const recommendations: string[] = [];
    const items = await this.recommendationItems.all();
    for (const item of items) {
      const text = await item.textContent();
      if (text) {
        recommendations.push(text.trim());
      }
    }
    return recommendations;
  }

  /**
   * Get patient count in risk list
   */
  async getRiskPatientCount(): Promise<number> {
    return this.riskRows.count();
  }

  /**
   * Search for patient
   */
  async searchPatient(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.page.keyboard.press('Enter');
    await this.waitForDataLoad();
  }
}
