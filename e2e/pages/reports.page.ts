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

    // Navigation
    this.pageHeading = page.locator('h1, h2').filter({ hasText: /report/i });
    this.patientReportsTab = page.locator('[data-testid="patient-reports-tab"], [role="tab"]:has-text("Patient")');
    this.populationReportsTab = page.locator('[data-testid="population-reports-tab"], [role="tab"]:has-text("Population")');
    this.qualityReportsTab = page.locator('[data-testid="quality-reports-tab"], [role="tab"]:has-text("Quality")');
    this.customReportsTab = page.locator('[data-testid="custom-reports-tab"], [role="tab"]:has-text("Custom")');

    // Report List
    this.reportList = page.locator('[data-testid="report-list"], .report-list');
    this.reportItems = page.locator('[data-testid="report-item"], .report-item');
    this.createReportButton = page.locator('[data-testid="create-report"], button:has-text("Create Report"), button:has-text("Generate")');
    this.refreshButton = page.locator('[data-testid="refresh"], button:has-text("Refresh")');

    // Report Generation
    this.reportTypeSelect = page.locator('[data-testid="report-type"], #reportType');
    this.dateRangeStart = page.locator('[data-testid="date-start"], #startDate');
    this.dateRangeEnd = page.locator('[data-testid="date-end"], #endDate');
    this.measureSelect = page.locator('[data-testid="measure-select"], #measures');
    this.patientPopulationSelect = page.locator('[data-testid="population-select"], #population');
    this.generateButton = page.locator('[data-testid="generate-report"], button:has-text("Generate")');

    // Export Options
    this.exportButton = page.locator('[data-testid="export-button"], button:has-text("Export")');
    this.exportCSV = page.locator('[data-testid="export-csv"], [role="menuitem"]:has-text("CSV")');
    this.exportExcel = page.locator('[data-testid="export-excel"], [role="menuitem"]:has-text("Excel")');
    this.exportPDF = page.locator('[data-testid="export-pdf"], [role="menuitem"]:has-text("PDF")');
    this.exportQRDA = page.locator('[data-testid="export-qrda"], [role="menuitem"]:has-text("QRDA")');

    // QRDA Specific
    this.qrdaCategorySelect = page.locator('[data-testid="qrda-category"], #qrdaCategory');
    this.qrdaProviderSelect = page.locator('[data-testid="qrda-provider"], #provider');
    this.qrdaValidateButton = page.locator('[data-testid="validate-qrda"], button:has-text("Validate")');

    // Report Viewer
    this.reportViewer = page.locator('[data-testid="report-viewer"], .report-viewer');
    this.reportTitle = page.locator('[data-testid="report-title"], .report-title');
    this.reportSummary = page.locator('[data-testid="report-summary"], .report-summary');
    this.reportData = page.locator('[data-testid="report-data"], .report-data, table');
    this.reportCharts = page.locator('[data-testid="report-charts"], .charts-container');

    // KPI Dashboard
    this.kpiCards = page.locator('[data-testid="kpi-card"], .kpi-card');
    this.complianceRate = page.locator('[data-testid="compliance-rate"], .compliance-rate');
    this.careGapsClosed = page.locator('[data-testid="gaps-closed"], .gaps-closed');
    this.patientsEvaluated = page.locator('[data-testid="patients-evaluated"], .patients-evaluated');
  }

  async goto(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitForLoad();
  }

  async isLoaded(): Promise<boolean> {
    return (await this.pageHeading.count()) > 0;
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
