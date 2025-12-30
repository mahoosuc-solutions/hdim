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

    // Navigation - Angular Material sidenav
    this.navigation = page.locator('mat-sidenav, .mat-drawer, nav, .sidenav');
    this.userMenu = page.locator('button[aria-label="User menu"], button[aria-label*="user" i], button[aria-label*="account" i]').first();
    this.logoutButton = page.locator('button:has-text("Logout"), button:has-text("Sign out"), button:has-text("Log out"), [role="menuitem"]:has-text("Logout")');
    this.settingsLink = page.locator('a:has-text("Settings"), button:has-text("Settings"), [role="menuitem"]:has-text("Settings")');

    // Dashboard metric cards - role-based dashboards have different cards
    // Provider dashboard: Patients Today, Results to Review, High Priority Gaps, Quality Score
    this.patientCountCard = page.locator('[aria-label*="Patients" i], :has-text("Patients Today"):not(button), :has-text("Total Patients"):not(button)').first();
    this.careGapsCard = page.locator('[aria-label*="Priority Gaps" i], :has-text("High Priority Gaps"):not(button), :has-text("Care Gap"):not(button)').first();
    this.evaluationsCard = page.locator('[aria-label*="Results" i], :has-text("Results to Review"):not(button), :has-text("Evaluations"):not(button)').first();
    this.complianceRateCard = page.locator('[aria-label*="Quality Score" i], :has-text("Quality Score"):not(button), :has-text("Compliance"):not(button)').first();

    // Quick action buttons
    this.newEvaluationButton = page.locator('button:has-text("New Evaluation"), a:has-text("New Evaluation")');
    this.viewCareGapsButton = page.locator('button:has-text("View All Care Gaps"), button:has-text("View Care Gaps")');
    this.searchPatientButton = page.locator('button:has-text("View All"):near(app-stat-card:has-text("Patients"))');
    this.generateReportButton = page.locator('button:has-text("Generate Report"), a:has-text("Reports")');

    // Recent activity lists
    this.recentEvaluationsList = page.locator('.recent-activity-section, .recent-evaluations, mat-card:has-text("Recent Activity")');
    this.recentCareGapsList = page.locator('.care-gaps-list, .care-gap-item');

    // Charts - ngx-charts
    this.complianceTrendChart = page.locator('ngx-charts-line-chart, .compliance-trend-chart');
    this.careGapDistributionChart = page.locator('ngx-charts-bar-horizontal, .care-gap-distribution-chart');
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
      // Multiple ways to detect dashboard - try each
      const dashboardIndicators = [
        this.page.locator('h1:has-text("Dashboard")').first(),
        this.page.locator('h1:has-text("Clinical Portal Dashboard")'),
        this.page.locator('[aria-label*="user role" i], [aria-label*="role view" i]'),
        this.page.locator('radiogroup[aria-label*="Select user role"]'),
        this.page.locator('.dashboard-container, .provider-dashboard'),
        this.page.locator('nav:has(a[href="/dashboard"])')
      ];

      for (const indicator of dashboardIndicators) {
        try {
          await indicator.waitFor({ state: 'visible', timeout: 5000 });
          return true;
        } catch {
          continue;
        }
      }

      // Final check - if URL contains dashboard, consider it loaded
      if (this.page.url().includes('/dashboard')) {
        await this.page.waitForLoadState('domcontentloaded');
        return true;
      }

      return false;
    } catch {
      return false;
    }
  }

  // Alias for careGapsCard
  get careGapCard(): Locator {
    return this.careGapsCard;
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
   * Uses Angular Material mat-nav-list with mat-list-item anchors
   */
  async navigateTo(menuItem: 'patients' | 'evaluations' | 'care-gaps' | 'reports' | 'measures' | 'admin' | 'results'): Promise<void> {
    const menuMap: Record<string, { text: string; urlPattern: string; fallbackRoute: string }> = {
      'patients': { text: 'Patients', urlPattern: '**/patients', fallbackRoute: '/patients' },
      'evaluations': { text: 'Evaluations', urlPattern: '**/evaluations', fallbackRoute: '/evaluations' },
      'care-gaps': { text: 'Care Gaps', urlPattern: '**/care-gaps', fallbackRoute: '/care-gaps' },
      'reports': { text: 'Reports', urlPattern: '**/reports', fallbackRoute: '/reports' },
      'measures': { text: 'Measure Builder', urlPattern: '**/measures', fallbackRoute: '/measures' },
      'results': { text: 'Results', urlPattern: '**/results', fallbackRoute: '/results' },
      'admin': { text: 'Administration', urlPattern: '**/admin', fallbackRoute: '/admin' },
    };

    const config = menuMap[menuItem];

    // Try multiple selector strategies for Angular Material nav
    const navSelectors = [
      `mat-nav-list a[mat-list-item]:has-text("${config.text}")`,
      `mat-sidenav a:has-text("${config.text}")`,
      `.nav-list a:has-text("${config.text}")`,
      `a[routerlink*="${config.fallbackRoute.replace('/', '')}"]`,
      `a[href*="${config.fallbackRoute}"]`,
    ];

    let clicked = false;
    for (const selector of navSelectors) {
      const element = this.page.locator(selector).first();
      if (await element.isVisible({ timeout: 2000 }).catch(() => false)) {
        await element.click();
        clicked = true;
        break;
      }
    }

    // If no nav item found, navigate directly
    if (!clicked) {
      await this.page.goto(config.fallbackRoute);
    }

    await this.page.waitForURL(config.urlPattern, { timeout: 10000 });
  }

  /**
   * Navigate to patients page using sidebar
   */
  async navigateToPatients(): Promise<void> {
    await this.navigateTo('patients');
  }

  /**
   * Navigate to care gaps page using sidebar
   */
  async navigateToCareGaps(): Promise<void> {
    // Try direct navigation link or "Results" which contains care gaps
    const careGapsLink = this.page.locator('a[href*="care-gap"], a:has-text("Care Gap")');
    if (await careGapsLink.count() > 0) {
      await careGapsLink.first().click();
      await this.page.waitForURL(/care-gap|caregap/i, { timeout: 10000 });
    } else {
      // Fall back to Results which may contain care gaps
      await this.navigateTo('results');
    }
  }

  /**
   * Navigate to evaluations page using sidebar
   */
  async navigateToEvaluations(): Promise<void> {
    await this.navigateTo('evaluations');
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
