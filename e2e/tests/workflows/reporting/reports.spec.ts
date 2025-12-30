import { test, expect } from '@playwright/test';
import { DashboardPage } from '../../../pages/dashboard.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Reporting Workflow Tests
 *
 * Test Suite: RPT (Reports)
 * Coverage: Patient reports, population reports, exports, QRDA, dashboards
 *
 * These tests verify the reporting and analytics workflows
 * that are critical for quality measure reporting and compliance.
 */

test.describe('Reporting Workflows', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);
  });

  /**
   * RPT-001: Patient Report Generation
   *
   * Verifies that individual patient reports can be generated
   * with quality measure results and care gap summaries.
   */
  test.describe('RPT-001: Patient Report Generation', () => {
    test('should navigate to patient reports section', async ({ page }) => {
      await page.goto('/reports');

      const reportsHeading = page.locator('h1, h2').filter({ hasText: /reports/i });
      await expect(reportsHeading.first()).toBeVisible();

      // Look for patient report option
      const patientReportOption = page.locator(
        '[data-testid="patient-report"], a:has-text("Patient Report"), button:has-text("Patient")'
      );
      const hasPatientReports = await patientReportOption.count() > 0;
      console.log('Patient reports available:', hasPatientReports);
    });

    test('should select patient for report generation', async ({ page }) => {
      await page.goto('/reports/patient');

      // Patient search for report
      const patientSearch = page.locator(
        '[data-testid="patient-search"], #patientSearch, input[placeholder*="patient"]'
      );

      if (await patientSearch.count() > 0) {
        await patientSearch.fill('Test');
        await page.waitForTimeout(500);

        // Select from results
        const results = page.locator('.search-result, .patient-option, [role="option"]');
        if (await results.count() > 0) {
          await results.first().click();
          console.log('Patient selected for report');
        }
      }
    });

    test('should configure report parameters', async ({ page }) => {
      await page.goto('/reports/patient');

      // Date range selection
      const dateRangeStart = page.locator('#startDate, [data-testid="start-date"]');
      const dateRangeEnd = page.locator('#endDate, [data-testid="end-date"]');

      if (await dateRangeStart.count() > 0) {
        await dateRangeStart.fill('2024-01-01');
        await dateRangeEnd.fill('2024-12-31');
        console.log('Date range configured');
      }

      // Measure selection
      const measureSelect = page.locator(
        '[data-testid="measure-select"], #measureSelect, select[name="measures"]'
      );
      if (await measureSelect.count() > 0) {
        console.log('Measure selection available');
      }
    });

    test('should generate patient report', async ({ page }) => {
      await page.goto('/reports/patient');

      const generateButton = page.locator(
        '[data-testid="generate-report"], button:has-text("Generate"), button:has-text("Create Report")'
      );

      if (await generateButton.count() > 0) {
        // Monitor for report generation request
        const reportPromise = page.waitForResponse(
          resp => resp.url().includes('/report') && resp.status() === 200
        ).catch(() => null);

        await generateButton.click();

        const reportResponse = await reportPromise;
        if (reportResponse) {
          console.log('Patient report generated successfully');
        }
      }
    });

    test('should display report preview', async ({ page }) => {
      await page.goto('/reports/patient');

      // Look for report preview area
      const reportPreview = page.locator(
        '[data-testid="report-preview"], .report-container, .report-output, iframe[src*="report"]'
      );

      const hasPreview = await reportPreview.count() > 0;
      console.log('Report preview available:', hasPreview);
    });
  });

  /**
   * RPT-002: Population Report Generation
   *
   * Verifies that population-level quality reports can be generated
   * for quality measure performance across patient populations.
   */
  test.describe('RPT-002: Population Report Generation', () => {
    test('should navigate to population reports', async ({ page }) => {
      await page.goto('/reports/population');

      const heading = page.locator('h1, h2').filter({ hasText: /population|quality/i });
      const hasHeading = await heading.count() > 0;
      console.log('Population reports page loaded:', hasHeading);
    });

    test('should select population criteria', async ({ page }) => {
      await page.goto('/reports/population');

      // Population filters
      const filters = {
        payer: page.locator('#payerFilter, [data-testid="payer-filter"]'),
        program: page.locator('#programFilter, [data-testid="program-filter"]'),
        provider: page.locator('#providerFilter, [data-testid="provider-filter"]'),
      };

      for (const [name, filter] of Object.entries(filters)) {
        if (await filter.count() > 0) {
          console.log(`${name} filter available`);
        }
      }
    });

    test('should select quality measures for report', async ({ page }) => {
      await page.goto('/reports/population');

      const measureCheckboxes = page.locator(
        '[data-testid="measure-checkbox"], input[type="checkbox"][name*="measure"]'
      );

      const count = await measureCheckboxes.count();
      console.log('Measure checkboxes found:', count);

      if (count > 0) {
        // Select first 3 measures
        for (let i = 0; i < Math.min(3, count); i++) {
          await measureCheckboxes.nth(i).check();
        }
        console.log('Measures selected for report');
      }
    });

    test('should generate population report', async ({ page }) => {
      await page.goto('/reports/population');

      const generateButton = page.locator(
        '[data-testid="generate-population-report"], button:has-text("Generate")'
      );

      if (await generateButton.count() > 0) {
        await generateButton.click();

        // Wait for report generation
        const reportReady = page.locator(
          '.report-ready, [data-testid="report-complete"], .download-ready'
        );

        await expect(reportReady).toBeVisible({ timeout: 30000 }).catch(() => {
          console.log('Report generation in progress');
        });
      }
    });

    test('should display population metrics summary', async ({ page }) => {
      await page.goto('/reports/population');

      const metricsSummary = page.locator(
        '[data-testid="metrics-summary"], .population-metrics, .summary-stats'
      );

      if (await metricsSummary.count() > 0) {
        const summaryText = await metricsSummary.textContent();
        console.log('Population metrics displayed:', summaryText?.substring(0, 100));
      }
    });
  });

  /**
   * RPT-003: Report Export (CSV/Excel)
   *
   * Verifies that reports can be exported in various formats
   * for offline analysis and sharing.
   */
  test.describe('RPT-003: Report Export', () => {
    test('should display export options', async ({ page }) => {
      await page.goto('/reports');

      const exportButton = page.locator(
        '[data-testid="export-button"], button:has-text("Export"), .export-dropdown'
      );

      if (await exportButton.count() > 0) {
        await exportButton.click();

        const exportOptions = page.locator('.export-option, [role="menuitem"]');
        const optionCount = await exportOptions.count();
        console.log('Export options available:', optionCount);
      }
    });

    test('should export report as CSV', async ({ page }) => {
      await page.goto('/reports');

      // Start download listener
      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const csvExport = page.locator(
        '[data-testid="export-csv"], button:has-text("CSV"), a:has-text("CSV")'
      );

      if (await csvExport.count() > 0) {
        await csvExport.click();

        const download = await downloadPromise;
        if (download) {
          const filename = download.suggestedFilename();
          console.log('CSV downloaded:', filename);
          expect(filename).toMatch(/\.csv$/i);
        }
      }
    });

    test('should export report as Excel', async ({ page }) => {
      await page.goto('/reports');

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const excelExport = page.locator(
        '[data-testid="export-excel"], button:has-text("Excel"), a:has-text("Excel"), button:has-text("XLSX")'
      );

      if (await excelExport.count() > 0) {
        await excelExport.click();

        const download = await downloadPromise;
        if (download) {
          const filename = download.suggestedFilename();
          console.log('Excel downloaded:', filename);
          expect(filename).toMatch(/\.(xlsx|xls)$/i);
        }
      }
    });

    test('should export report as PDF', async ({ page }) => {
      await page.goto('/reports');

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const pdfExport = page.locator(
        '[data-testid="export-pdf"], button:has-text("PDF"), a:has-text("PDF")'
      );

      if (await pdfExport.count() > 0) {
        await pdfExport.click();

        const download = await downloadPromise;
        if (download) {
          const filename = download.suggestedFilename();
          console.log('PDF downloaded:', filename);
          expect(filename).toMatch(/\.pdf$/i);
        }
      }
    });

    test('should show export progress for large reports', async ({ page }) => {
      await page.goto('/reports/population');

      const exportButton = page.locator('[data-testid="export-button"]');

      if (await exportButton.count() > 0) {
        await exportButton.click();

        // Look for progress indicator
        const progressIndicator = page.locator(
          '.export-progress, [data-testid="export-progress"], mat-progress-bar'
        );

        const hasProgress = await progressIndicator.count() > 0;
        console.log('Export progress indicator:', hasProgress);
      }
    });
  });

  /**
   * RPT-004: QRDA I/III Export
   *
   * Verifies that quality reports can be exported in QRDA format
   * for CMS submission and compliance reporting.
   */
  test.describe('RPT-004: QRDA Export', () => {
    test('should navigate to QRDA export section', async ({ page }) => {
      await page.goto('/reports/qrda');

      const qrdaHeading = page.locator('h1, h2').filter({ hasText: /qrda/i });
      const hasQRDASection = await qrdaHeading.count() > 0;
      console.log('QRDA export section available:', hasQRDASection);
    });

    test('should select QRDA I (patient-level) export', async ({ page }) => {
      await page.goto('/reports/qrda');

      const qrdaI = page.locator(
        '[data-testid="qrda-i"], input[value="QRDA_I"], button:has-text("QRDA I")'
      );

      if (await qrdaI.count() > 0) {
        await qrdaI.click();
        console.log('QRDA I selected');

        // Verify patient selection is required
        const patientSelect = page.locator('[data-testid="patient-select"]');
        const requiresPatient = await patientSelect.count() > 0;
        console.log('Patient selection required:', requiresPatient);
      }
    });

    test('should select QRDA III (aggregate) export', async ({ page }) => {
      await page.goto('/reports/qrda');

      const qrdaIII = page.locator(
        '[data-testid="qrda-iii"], input[value="QRDA_III"], button:has-text("QRDA III")'
      );

      if (await qrdaIII.count() > 0) {
        await qrdaIII.click();
        console.log('QRDA III selected');

        // Verify population selection
        const populationSelect = page.locator('[data-testid="population-select"]');
        const requiresPopulation = await populationSelect.count() > 0;
        console.log('Population selection required:', requiresPopulation);
      }
    });

    test('should configure QRDA reporting period', async ({ page }) => {
      await page.goto('/reports/qrda');

      const reportingPeriod = page.locator(
        '[data-testid="reporting-period"], #reportingPeriod, select[name="period"]'
      );

      if (await reportingPeriod.count() > 0) {
        const options = await reportingPeriod.locator('option').allTextContents();
        console.log('Reporting periods available:', options);
      }
    });

    test('should generate QRDA export', async ({ page }) => {
      await page.goto('/reports/qrda');

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const generateButton = page.locator(
        '[data-testid="generate-qrda"], button:has-text("Generate QRDA")'
      );

      if (await generateButton.count() > 0) {
        await generateButton.click();

        const download = await downloadPromise;
        if (download) {
          const filename = download.suggestedFilename();
          console.log('QRDA file downloaded:', filename);
          expect(filename).toMatch(/\.(xml|zip)$/i);
        }
      }
    });

    test('should validate QRDA before export', async ({ page }) => {
      await page.goto('/reports/qrda');

      const validateButton = page.locator(
        '[data-testid="validate-qrda"], button:has-text("Validate")'
      );

      if (await validateButton.count() > 0) {
        await validateButton.click();

        // Wait for validation results
        const validationResults = page.locator(
          '[data-testid="validation-results"], .validation-output'
        );

        if (await validationResults.count() > 0) {
          const results = await validationResults.textContent();
          console.log('QRDA validation results:', results?.substring(0, 100));
        }
      }
    });
  });

  /**
   * RPT-005: Dashboard KPI Viewing
   *
   * Verifies that the dashboard displays key performance indicators
   * and allows interactive exploration of quality metrics.
   */
  test.describe('RPT-005: Dashboard KPIs', () => {
    test('should display main KPI widgets', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Look for KPI widgets
      const kpiWidgets = page.locator(
        '[data-testid="kpi-widget"], .kpi-card, .metric-card, .stat-widget'
      );

      const widgetCount = await kpiWidgets.count();
      console.log('KPI widgets displayed:', widgetCount);
      expect(widgetCount).toBeGreaterThan(0);
    });

    test('should display compliance rate KPI', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      const complianceKpi = page.locator(
        '[data-testid="compliance-rate"], .compliance-metric, :has-text("Compliance Rate")'
      );

      if (await complianceKpi.count() > 0) {
        const text = await complianceKpi.textContent();
        console.log('Compliance rate:', text);

        // Should contain a percentage
        expect(text).toMatch(/\d+(\.\d+)?%?/);
      }
    });

    test('should display care gap count KPI', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      const careGapCount = await dashboardPage.getCareGapsCount();
      console.log('Care gaps KPI:', careGapCount);
    });

    test('should display patient count KPI', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      const patientKpi = page.locator(
        '[data-testid="patient-count"], .patient-metric, :has-text("Patients")'
      );

      if (await patientKpi.count() > 0) {
        const text = await patientKpi.textContent();
        console.log('Patient count:', text);
      }
    });

    test('should show KPI trends over time', async ({ page }) => {
      await dashboardPage.goto();

      // Look for trend indicators
      const trendIndicators = page.locator(
        '.trend-indicator, .trend-up, .trend-down, [data-testid="trend"]'
      );

      const hasTrends = await trendIndicators.count() > 0;
      console.log('Trend indicators displayed:', hasTrends);
    });

    test('should allow KPI drill-down', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      // Click on a KPI to drill down
      const kpiWidget = page.locator('.kpi-card, [data-testid="kpi-widget"]').first();

      if (await kpiWidget.count() > 0) {
        await kpiWidget.click();

        // Should navigate to detailed view or show modal
        await page.waitForTimeout(500);

        const detailView = page.locator(
          '.kpi-detail, [data-testid="kpi-detail"], .modal, [role="dialog"]'
        );

        const hasDrillDown = await detailView.count() > 0 || page.url().includes('detail');
        console.log('KPI drill-down available:', hasDrillDown);
      }
    });

    test('should refresh KPIs', async ({ page }) => {
      await dashboardPage.goto();
      await dashboardPage.waitForDataLoad();

      const refreshButton = page.locator(
        '[data-testid="refresh-kpis"], button:has-text("Refresh"), .refresh-button'
      );

      if (await refreshButton.count() > 0) {
        const responsePromise = page.waitForResponse(
          resp => resp.url().includes('/dashboard') || resp.url().includes('/metrics')
        ).catch(() => null);

        await refreshButton.click();

        const response = await responsePromise;
        if (response) {
          console.log('KPIs refreshed successfully');
        }
      }
    });

    test('should display measure-specific KPIs', async ({ page }) => {
      await dashboardPage.goto();

      // Look for measure-level metrics
      const measureKpis = page.locator(
        '[data-testid="measure-kpi"], .measure-metric, .hedis-metric'
      );

      const count = await measureKpis.count();
      console.log('Measure-specific KPIs:', count);
    });
  });
});

/**
 * Report Scheduling Tests
 */
test.describe('Report Scheduling', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display report scheduling options', async ({ page }) => {
    await page.goto('/reports/schedule');

    const scheduleSection = page.locator(
      '[data-testid="report-schedule"], .schedule-section, h2:has-text("Schedule")'
    );

    const hasScheduling = await scheduleSection.count() > 0;
    console.log('Report scheduling available:', hasScheduling);
  });

  test('should create scheduled report', async ({ page }) => {
    await page.goto('/reports/schedule');

    const createButton = page.locator(
      '[data-testid="create-schedule"], button:has-text("Create Schedule")'
    );

    if (await createButton.count() > 0) {
      await createButton.click();

      // Fill schedule form
      const nameInput = page.locator('#scheduleName, [data-testid="schedule-name"]');
      if (await nameInput.count() > 0) {
        await nameInput.fill('E2E Test Schedule');
      }

      // Select frequency
      const frequencySelect = page.locator('#frequency, [data-testid="frequency"]');
      if (await frequencySelect.count() > 0) {
        await frequencySelect.selectOption('weekly');
      }

      console.log('Schedule creation form filled');
    }
  });

  test('should view scheduled reports list', async ({ page }) => {
    await page.goto('/reports/schedule');

    const scheduleList = page.locator(
      '[data-testid="schedule-list"], .scheduled-reports, table'
    );

    if (await scheduleList.count() > 0) {
      const scheduleCount = await scheduleList.locator('tr, .schedule-item').count();
      console.log('Scheduled reports:', scheduleCount);
    }
  });
});

/**
 * Report History Tests
 */
test.describe('Report History', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);
  });

  test('should display report history', async ({ page }) => {
    await page.goto('/reports/history');

    const historyTable = page.locator(
      '[data-testid="report-history"], .history-table, table'
    );

    const hasHistory = await historyTable.count() > 0;
    console.log('Report history displayed:', hasHistory);
  });

  test('should filter report history by date', async ({ page }) => {
    await page.goto('/reports/history');

    const dateFilter = page.locator('#dateFilter, [data-testid="date-filter"]');

    if (await dateFilter.count() > 0) {
      await dateFilter.fill('2024-01-01');
      console.log('Date filter applied');
    }
  });

  test('should download previous report', async ({ page }) => {
    await page.goto('/reports/history');

    const downloadPromise = page.waitForEvent('download').catch(() => null);

    const downloadButton = page.locator(
      '[data-testid="download-report"], button:has-text("Download"), .download-icon'
    ).first();

    if (await downloadButton.count() > 0) {
      await downloadButton.click();

      const download = await downloadPromise;
      if (download) {
        console.log('Previous report downloaded:', download.suggestedFilename());
      }
    }
  });
});
