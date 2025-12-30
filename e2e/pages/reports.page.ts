import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Reports Page Object
 *
 * Handles reporting and export functionality.
 * Used for RPT-001 to RPT-005 test workflows.
 */
export class ReportsPage extends BasePage {
  readonly url = '/reports';

  // Navigation
  readonly pageHeading: Locator;
  readonly patientReportsTab: Locator;
  readonly populationReportsTab: Locator;
  readonly qualityReportsTab: Locator;
  readonly customReportsTab: Locator;

  // Report List
  readonly reportList: Locator;
  readonly reportItems: Locator;
  readonly createReportButton: Locator;
  readonly refreshButton: Locator;

  // Report Generation
  readonly reportTypeSelect: Locator;
  readonly dateRangeStart: Locator;
  readonly dateRangeEnd: Locator;
  readonly measureSelect: Locator;
  readonly patientPopulationSelect: Locator;
  readonly generateButton: Locator;

  // Export Options
  readonly exportButton: Locator;
  readonly exportCSV: Locator;
  readonly exportExcel: Locator;
  readonly exportPDF: Locator;
  readonly exportQRDA: Locator;

  // QRDA Specific
  readonly qrdaCategorySelect: Locator;
  readonly qrdaProviderSelect: Locator;
  readonly qrdaValidateButton: Locator;

  // Report Viewer
  readonly reportViewer: Locator;
  readonly reportTitle: Locator;
  readonly reportSummary: Locator;
  readonly reportData: Locator;
  readonly reportCharts: Locator;

  // KPI Dashboard
  readonly kpiCards: Locator;
  readonly complianceRate: Locator;
  readonly careGapsClosed: Locator;
  readonly patientsEvaluated: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation - Angular Material tabs
    this.pageHeading = page.locator('h1:has-text("Report"), h2:has-text("Report"), h1.page-title');
    this.patientReportsTab = page.locator('[data-testid="patient-reports-tab"], [role="tab"]:has-text("Patient"), mat-tab:has-text("Patient")');
    this.populationReportsTab = page.locator('[data-testid="population-reports-tab"], [role="tab"]:has-text("Population"), mat-tab:has-text("Population")');
    this.qualityReportsTab = page.locator('[data-testid="quality-reports-tab"], [role="tab"]:has-text("Quality"), mat-tab:has-text("Quality")');
    this.customReportsTab = page.locator('[data-testid="custom-reports-tab"], [role="tab"]:has-text("Custom"), mat-tab:has-text("Custom")');

    // Report List - Angular Material table/list
    this.reportList = page.locator('[data-testid="report-list"], mat-table, mat-list, .report-list');
    this.reportItems = page.locator('[data-testid="report-item"], mat-row, mat-list-item, .report-item');
    this.createReportButton = page.locator('[data-testid="create-report"], button:has-text("Create Report"), button:has-text("Generate"), button:has-text("New Report")');
    this.refreshButton = page.locator('[data-testid="refresh"], button:has-text("Refresh"), button[aria-label*="refresh" i]');

    // Report Generation - Angular Material form fields
    this.reportTypeSelect = page.locator('[data-testid="report-type"], mat-select[formcontrolname="reportType"], mat-select[aria-label*="type" i], #reportType');
    this.dateRangeStart = page.locator('[data-testid="date-start"], input[formcontrolname="startDate"], mat-datepicker input, #startDate');
    this.dateRangeEnd = page.locator('[data-testid="date-end"], input[formcontrolname="endDate"], #endDate');
    this.measureSelect = page.locator('[data-testid="measure-select"], mat-select[formcontrolname="measures"], #measures');
    this.patientPopulationSelect = page.locator('[data-testid="population-select"], mat-select[formcontrolname="population"], #population');
    this.generateButton = page.locator('[data-testid="generate-report"], button:has-text("Generate"), button:has-text("Run Report")');

    // Export Options - Angular Material menu
    this.exportButton = page.locator('[data-testid="export-button"], button:has-text("Export"), button[aria-label*="export" i]');
    this.exportCSV = page.locator('[data-testid="export-csv"], [role="menuitem"]:has-text("CSV"), button:has-text("CSV")');
    this.exportExcel = page.locator('[data-testid="export-excel"], [role="menuitem"]:has-text("Excel"), button:has-text("Excel")');
    this.exportPDF = page.locator('[data-testid="export-pdf"], [role="menuitem"]:has-text("PDF"), button:has-text("PDF")');
    this.exportQRDA = page.locator('[data-testid="export-qrda"], [role="menuitem"]:has-text("QRDA"), button:has-text("QRDA")');

    // QRDA Specific
    this.qrdaCategorySelect = page.locator('[data-testid="qrda-category"], mat-select[formcontrolname="qrdaCategory"], #qrdaCategory');
    this.qrdaProviderSelect = page.locator('[data-testid="qrda-provider"], mat-select[formcontrolname="provider"], #provider');
    this.qrdaValidateButton = page.locator('[data-testid="validate-qrda"], button:has-text("Validate")');

    // Report Viewer - Angular Material cards and tables
    this.reportViewer = page.locator('[data-testid="report-viewer"], mat-card.report-viewer, .report-viewer');
    this.reportTitle = page.locator('[data-testid="report-title"], mat-card-title, .report-title, h2');
    this.reportSummary = page.locator('[data-testid="report-summary"], mat-card-subtitle, .report-summary');
    this.reportData = page.locator('[data-testid="report-data"], mat-table, table, .report-data');
    this.reportCharts = page.locator('[data-testid="report-charts"], .charts-container, canvas, ngx-charts-bar-vertical');

    // KPI Dashboard - Angular Material cards
    this.kpiCards = page.locator('[data-testid="kpi-card"], mat-card.kpi-card, .summary-card, .kpi-card');
    this.complianceRate = page.locator('[data-testid="compliance-rate"], :has-text("Compliance") .summary-value, .compliance-rate');
    this.careGapsClosed = page.locator('[data-testid="gaps-closed"], :has-text("Gaps Closed") .summary-value, .gaps-closed');
    this.patientsEvaluated = page.locator('[data-testid="patients-evaluated"], :has-text("Patients Evaluated") .summary-value, .patients-evaluated');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    try {
      // Wait for page container or heading
      await this.page.locator('h1:has-text("Report"), .reports-container, h1.page-title').first().waitFor({ state: 'visible', timeout: 10000 });

      // Wait for loading to complete
      await this.waitForSpinnerToDisappear();

      return true;
    } catch {
      return false;
    }
  }

  /**
   * Wait for reports to load
   */
  async waitForDataLoad(): Promise<void> {
    await this.waitForSpinnerToDisappear();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Navigate to patient reports
   */
  async gotoPatientReports(): Promise<void> {
    await this.patientReportsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to population reports
   */
  async gotoPopulationReports(): Promise<void> {
    await this.populationReportsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Navigate to quality reports
   */
  async gotoQualityReports(): Promise<void> {
    await this.qualityReportsTab.click();
    await this.waitForDataLoad();
  }

  /**
   * Generate a report
   */
  async generateReport(options: {
    type: string;
    startDate?: string;
    endDate?: string;
  }): Promise<void> {
    await this.createReportButton.click();

    // Select report type
    await this.reportTypeSelect.click();
    await this.page.locator(`[role="option"]:has-text("${options.type}")`).click();

    // Set date range if provided
    if (options.startDate) {
      await this.dateRangeStart.fill(options.startDate);
    }
    if (options.endDate) {
      await this.dateRangeEnd.fill(options.endDate);
    }

    await this.generateButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Export report in specific format
   */
  async exportReport(format: 'csv' | 'excel' | 'pdf' | 'qrda'): Promise<void> {
    await this.exportButton.click();

    switch (format) {
      case 'csv':
        await this.exportCSV.click();
        break;
      case 'excel':
        await this.exportExcel.click();
        break;
      case 'pdf':
        await this.exportPDF.click();
        break;
      case 'qrda':
        await this.exportQRDA.click();
        break;
    }
  }

  /**
   * Get report count
   */
  async getReportCount(): Promise<number> {
    return this.reportItems.count();
  }

  /**
   * Select a report from the list
   */
  async selectReport(index: number = 0): Promise<void> {
    await this.reportItems.nth(index).click();
    await this.waitForDataLoad();
  }

  /**
   * Get compliance rate from KPI
   */
  async getComplianceRate(): Promise<string> {
    if (await this.complianceRate.count() > 0) {
      return (await this.complianceRate.textContent()) || '';
    }
    return '';
  }

  /**
   * Configure QRDA export
   */
  async configureQRDAExport(category: 'I' | 'III', provider?: string): Promise<void> {
    await this.qrdaCategorySelect.click();
    await this.page.locator(`[role="option"]:has-text("Category ${category}")`).click();

    if (provider && await this.qrdaProviderSelect.count() > 0) {
      await this.qrdaProviderSelect.click();
      await this.page.locator(`[role="option"]:has-text("${provider}")`).click();
    }
  }

  /**
   * Validate QRDA output
   */
  async validateQRDA(): Promise<boolean> {
    if (await this.qrdaValidateButton.count() > 0) {
      await this.qrdaValidateButton.click();
      await this.waitForDataLoad();
      return !(await this.hasError());
    }
    return false;
  }

  /**
   * Get KPI card values
   */
  async getKPIValues(): Promise<{ [key: string]: string }> {
    const values: { [key: string]: string } = {};
    const cards = await this.kpiCards.all();

    for (const card of cards) {
      const label = await card.locator('.kpi-label, .label').textContent();
      const value = await card.locator('.kpi-value, .value').textContent();
      if (label && value) {
        values[label.trim()] = value.trim();
      }
    }

    return values;
  }
}
