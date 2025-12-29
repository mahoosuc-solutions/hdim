import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Dashboard Page Object Model
 *
 * The main dashboard view after login showing:
 * - Summary metrics
 * - Care gap overview
 * - Recent evaluations
 * - Quick actions
 */
export class DashboardPage extends BasePage {
  // Navigation elements
  readonly navigation: Locator;
  readonly userMenu: Locator;
  readonly logoutButton: Locator;
  readonly settingsLink: Locator;

  // Dashboard cards
  readonly patientCountCard: Locator;
  readonly careGapsCard: Locator;
  readonly evaluationsCard: Locator;
  readonly complianceRateCard: Locator;

  // Quick actions
  readonly newEvaluationButton: Locator;
  readonly viewCareGapsButton: Locator;
  readonly searchPatientButton: Locator;
  readonly generateReportButton: Locator;

  // Recent activity
  readonly recentEvaluationsList: Locator;
  readonly recentCareGapsList: Locator;

  // Charts/Visualizations
  readonly complianceTrendChart: Locator;
  readonly careGapDistributionChart: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation
    this.navigation = page.locator('[data-testid="navigation"], nav, .sidebar');
    this.userMenu = page.locator('[data-testid="user-menu"], .user-menu, [aria-label="User menu"]');
    this.logoutButton = page.locator('[data-testid="logout-button"], button:has-text("Logout"), button:has-text("Sign out")');
    this.settingsLink = page.locator('[data-testid="settings-link"], a:has-text("Settings")');

    // Dashboard metric cards
    this.patientCountCard = page.locator('[data-testid="patient-count-card"], .dashboard-card:has-text("Patients")');
    this.careGapsCard = page.locator('[data-testid="care-gaps-card"], .dashboard-card:has-text("Care Gaps")');
    this.evaluationsCard = page.locator('[data-testid="evaluations-card"], .dashboard-card:has-text("Evaluations")');
    this.complianceRateCard = page.locator('[data-testid="compliance-rate-card"], .dashboard-card:has-text("Compliance")');

    // Quick action buttons
    this.newEvaluationButton = page.locator('[data-testid="new-evaluation-button"], button:has-text("New Evaluation")');
    this.viewCareGapsButton = page.locator('[data-testid="view-care-gaps-button"], button:has-text("View Care Gaps")');
    this.searchPatientButton = page.locator('[data-testid="search-patient-button"], button:has-text("Search Patient")');
    this.generateReportButton = page.locator('[data-testid="generate-report-button"], button:has-text("Generate Report")');

    // Recent activity lists
    this.recentEvaluationsList = page.locator('[data-testid="recent-evaluations-list"], .recent-evaluations');
    this.recentCareGapsList = page.locator('[data-testid="recent-care-gaps-list"], .recent-care-gaps');

    // Charts
    this.complianceTrendChart = page.locator('[data-testid="compliance-trend-chart"], .compliance-chart');
    this.careGapDistributionChart = page.locator('[data-testid="care-gap-distribution-chart"], .care-gap-chart');
  }

  /**
   * Navigate to dashboard
   */
  async goto(): Promise<void> {
    await this.page.goto('/dashboard');
    await this.waitForLoad();
  }

  /**
   * Check if dashboard is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      await this.patientCountCard.waitFor({ state: 'visible', timeout: 5000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get patient count from dashboard card
   */
  async getPatientCount(): Promise<number> {
    const text = await this.patientCountCard.locator('.metric-value, .card-value').textContent() || '0';
    return parseInt(text.replace(/[^0-9]/g, ''), 10);
  }

  /**
   * Get open care gaps count
   */
  async getCareGapsCount(): Promise<number> {
    const text = await this.careGapsCard.locator('.metric-value, .card-value').textContent() || '0';
    return parseInt(text.replace(/[^0-9]/g, ''), 10);
  }

  /**
   * Get total evaluations count
   */
  async getEvaluationsCount(): Promise<number> {
    const text = await this.evaluationsCard.locator('.metric-value, .card-value').textContent() || '0';
    return parseInt(text.replace(/[^0-9]/g, ''), 10);
  }

  /**
   * Get compliance rate percentage
   */
  async getComplianceRate(): Promise<number> {
    const text = await this.complianceRateCard.locator('.metric-value, .card-value').textContent() || '0%';
    return parseFloat(text.replace(/[^0-9.]/g, ''));
  }

  /**
   * Click to start new evaluation
   */
  async startNewEvaluation(): Promise<void> {
    await this.newEvaluationButton.click();
    await this.page.waitForURL('**/evaluations');
  }

  /**
   * Click to view care gaps
   */
  async goToCareGaps(): Promise<void> {
    await this.viewCareGapsButton.click();
    await this.page.waitForURL('**/care-gaps');
  }

  /**
   * Click to search patients
   */
  async goToPatientSearch(): Promise<void> {
    await this.searchPatientButton.click();
    await this.page.waitForURL('**/patients');
  }

  /**
   * Click to generate report
   */
  async goToReportGeneration(): Promise<void> {
    await this.generateReportButton.click();
    await this.page.waitForURL('**/reports');
  }

  /**
   * Open user menu
   */
  async openUserMenu(): Promise<void> {
    await this.userMenu.click();
  }

  /**
   * Logout from dashboard
   */
  async logout(): Promise<void> {
    await this.openUserMenu();
    await this.logoutButton.click();
    await this.page.waitForURL('**/login');
  }

  /**
   * Navigate to settings
   */
  async goToSettings(): Promise<void> {
    await this.openUserMenu();
    await this.settingsLink.click();
    await this.page.waitForURL('**/settings');
  }

  /**
   * Get recent evaluations list
   */
  async getRecentEvaluations(): Promise<string[]> {
    const items = this.recentEvaluationsList.locator('.evaluation-item, tr, li');
    const count = await items.count();
    const evaluations: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await this.safeText(items.nth(i));
      evaluations.push(text);
    }

    return evaluations;
  }

  /**
   * Get recent care gaps list
   */
  async getRecentCareGaps(): Promise<string[]> {
    const items = this.recentCareGapsList.locator('.care-gap-item, tr, li');
    const count = await items.count();
    const gaps: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await this.safeText(items.nth(i));
      gaps.push(text);
    }

    return gaps;
  }

  /**
   * Navigate using sidebar
   */
  async navigateTo(menuItem: 'patients' | 'evaluations' | 'care-gaps' | 'reports' | 'measures' | 'admin'): Promise<void> {
    const menuMap: Record<string, string> = {
      'patients': 'Patients',
      'evaluations': 'Evaluations',
      'care-gaps': 'Care Gaps',
      'reports': 'Reports',
      'measures': 'Quality Measures',
      'admin': 'Administration',
    };

    const linkText = menuMap[menuItem];
    await this.navigation.locator(`a:has-text("${linkText}"), [routerlink*="${menuItem}"]`).click();
    await this.page.waitForURL(`**/${menuItem}`);
  }

  /**
   * Assert dashboard metrics are displayed
   */
  async assertMetricsDisplayed(): Promise<void> {
    await expect(this.patientCountCard).toBeVisible();
    await expect(this.careGapsCard).toBeVisible();
    await expect(this.evaluationsCard).toBeVisible();
    await expect(this.complianceRateCard).toBeVisible();
  }

  /**
   * Assert quick actions are available
   */
  async assertQuickActionsAvailable(): Promise<void> {
    await expect(this.newEvaluationButton).toBeVisible();
    await expect(this.viewCareGapsButton).toBeVisible();
    await expect(this.searchPatientButton).toBeVisible();
  }

  /**
   * Wait for dashboard data to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    // Wait for at least one metric to have a value
    await this.page.waitForSelector('.metric-value:not(:empty), .card-value:not(:empty)');
  }
}
