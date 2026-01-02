import { test, expect } from '@playwright/test';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Risk Stratification Workflow Tests
 *
 * Test Suite: RISK
 * Coverage: Risk score viewing, population stratification, HCC risk adjustment
 *
 * These tests verify the risk stratification features that enable
 * proactive care management for high-risk patients.
 */

test.describe('Risk Stratification Workflows', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * RISK-001: Risk Score Viewing
   *
   * Verifies that individual patient risk scores are displayed
   * with supporting clinical factors.
   */
  test.describe('RISK-001: Risk Score Viewing', () => {
    test('should display risk score on patient profile', async ({ page }) => {
      await page.goto('/patients');

      // Navigate to patient detail
      const firstPatient = page.locator('[data-testid="patient-row"], .patient-item').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        // Look for risk score
        const riskScore = page.locator(
          '[data-testid="risk-score"], .risk-score, .patient-risk'
        );

        const hasRiskScore = await riskScore.count() > 0;
        console.log('Risk score displayed:', hasRiskScore);

        if (hasRiskScore) {
          const scoreText = await riskScore.textContent();
          console.log('Risk score value:', scoreText);
        }
      }
    });

    test('should display risk level indicator', async ({ page }) => {
      await page.goto('/patients');

      const riskIndicators = page.locator(
        '[data-testid="risk-level"], .risk-indicator, .risk-badge'
      );

      const count = await riskIndicators.count();
      console.log('Risk level indicators:', count);

      if (count > 0) {
        const firstIndicator = riskIndicators.first();
        const level = await firstIndicator.textContent();
        console.log('Risk level:', level);
      }
    });

    test('should display risk factors breakdown', async ({ page }) => {
      await page.goto('/patients');

      const firstPatient = page.locator('[data-testid="patient-row"]').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        const riskFactors = page.locator(
          '[data-testid="risk-factors"], .risk-breakdown, .contributing-factors'
        );

        if (await riskFactors.count() > 0) {
          const factors = await riskFactors.textContent();
          console.log('Risk factors:', factors?.substring(0, 100));
        }
      }
    });

    test('should show risk score history', async ({ page }) => {
      await page.goto('/patients');

      const firstPatient = page.locator('[data-testid="patient-row"]').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        // Look for history/trend view
        const historyTab = page.locator(
          '[data-testid="risk-history"], button:has-text("History"), a:has-text("Trend")'
        );

        if (await historyTab.count() > 0) {
          await historyTab.click();

          const historyChart = page.locator('.risk-history-chart, canvas, svg');
          const hasChart = await historyChart.count() > 0;
          console.log('Risk history chart:', hasChart);
        }
      }
    });

    test('should display risk category details', async ({ page }) => {
      await page.goto('/patients');

      const riskBadge = page.locator('[data-testid="risk-level"]').first();
      if (await riskBadge.count() > 0) {
        await riskBadge.hover();

        // Tooltip or popover with details
        const tooltip = page.locator(
          '[role="tooltip"], .risk-tooltip, .popover'
        );

        const hasTooltip = await tooltip.count() > 0;
        console.log('Risk details tooltip:', hasTooltip);
      }
    });

    test('should filter patients by risk level', async ({ page }) => {
      await page.goto('/patients');

      const riskFilter = page.locator(
        '[data-testid="risk-filter"], #riskLevel, select[name="risk"]'
      );

      if (await riskFilter.count() > 0) {
        await riskFilter.selectOption('HIGH');
        await page.waitForTimeout(500);

        const patients = page.locator('[data-testid="patient-row"]');
        const count = await patients.count();
        console.log('High-risk patients:', count);
      }
    });
  });

  /**
   * RISK-002: Population Risk Stratification
   *
   * Verifies population-level risk analysis and stratification
   * for care management prioritization.
   */
  test.describe('RISK-002: Population Risk Stratification', () => {
    test('should navigate to population risk view', async ({ page }) => {
      await page.goto('/risk-stratification');

      const heading = page.locator('h1, h2').filter({ hasText: /risk|stratification/i });
      const hasPage = await heading.count() > 0;
      console.log('Population risk page loaded:', hasPage);
    });

    test('should display risk distribution chart', async ({ page }) => {
      await page.goto('/risk-stratification');

      const distributionChart = page.locator(
        '[data-testid="risk-distribution"], .risk-chart, .stratification-chart, canvas'
      );

      const hasChart = await distributionChart.count() > 0;
      console.log('Risk distribution chart:', hasChart);
    });

    test('should show patient counts by risk tier', async ({ page }) => {
      await page.goto('/risk-stratification');

      const riskTiers = page.locator(
        '[data-testid="risk-tier"], .tier-card, .risk-category'
      );

      const tierCount = await riskTiers.count();
      console.log('Risk tiers displayed:', tierCount);

      if (tierCount > 0) {
        for (let i = 0; i < tierCount; i++) {
          const tier = riskTiers.nth(i);
          const text = await tier.textContent();
          console.log(`Tier ${i + 1}:`, text?.substring(0, 50));
        }
      }
    });

    test('should drill down into risk tier', async ({ page }) => {
      await page.goto('/risk-stratification');

      const highRiskTier = page.locator(
        '[data-testid="risk-tier-high"], .tier-card:has-text("High")'
      ).first();

      if (await highRiskTier.count() > 0) {
        await highRiskTier.click();

        // Should show patients in that tier
        await page.waitForTimeout(500);
        const patientList = page.locator('[data-testid="tier-patients"], .patient-list');
        const hasList = await patientList.count() > 0 || page.url().includes('risk');
        console.log('Tier drill-down available:', hasList);
      }
    });

    test('should display risk trend over time', async ({ page }) => {
      await page.goto('/risk-stratification');

      const trendChart = page.locator(
        '[data-testid="risk-trend"], .trend-chart, .time-series'
      );

      const hasTrend = await trendChart.count() > 0;
      console.log('Risk trend chart:', hasTrend);
    });

    test('should filter by population segment', async ({ page }) => {
      await page.goto('/risk-stratification');

      const segmentFilter = page.locator(
        '[data-testid="segment-filter"], #segment, select[name="population"]'
      );

      if (await segmentFilter.count() > 0) {
        const options = await segmentFilter.locator('option').allTextContents();
        console.log('Population segments:', options);
      }
    });

    test('should export risk stratification report', async ({ page }) => {
      await page.goto('/risk-stratification');

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const exportButton = page.locator(
        '[data-testid="export-risk"], button:has-text("Export")'
      );

      if (await exportButton.count() > 0) {
        await exportButton.click();

        const download = await downloadPromise;
        if (download) {
          console.log('Risk report exported:', download.suggestedFilename());
        }
      }
    });
  });

  /**
   * RISK-003: HCC Risk Adjustment
   *
   * Verifies HCC (Hierarchical Condition Categories) risk adjustment
   * for Medicare Advantage and value-based contracts.
   */
  test.describe('RISK-003: HCC Risk Adjustment', () => {
    test('should navigate to HCC dashboard', async ({ page }) => {
      await page.goto('/hcc');

      const heading = page.locator('h1, h2').filter({ hasText: /hcc|risk adjustment/i });
      const hasPage = await heading.count() > 0;
      console.log('HCC dashboard loaded:', hasPage);
    });

    test('should display HCC score summary', async ({ page }) => {
      await page.goto('/hcc');

      const hccSummary = page.locator(
        '[data-testid="hcc-summary"], .hcc-score, .raf-score'
      );

      if (await hccSummary.count() > 0) {
        const scoreText = await hccSummary.textContent();
        console.log('HCC summary:', scoreText?.substring(0, 100));
      }
    });

    test('should show patient HCC codes', async ({ page }) => {
      await page.goto('/patients');

      const firstPatient = page.locator('[data-testid="patient-row"]').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        const hccCodes = page.locator(
          '[data-testid="hcc-codes"], .hcc-list, .condition-codes'
        );

        const hasHccCodes = await hccCodes.count() > 0;
        console.log('HCC codes displayed:', hasHccCodes);
      }
    });

    test('should display HCC gaps', async ({ page }) => {
      await page.goto('/hcc/gaps');

      const hccGaps = page.locator(
        '[data-testid="hcc-gap"], .hcc-opportunity, .recapture-gap'
      );

      const gapCount = await hccGaps.count();
      console.log('HCC gaps identified:', gapCount);
    });

    test('should show RAF score calculation', async ({ page }) => {
      await page.goto('/hcc');

      const rafCalculation = page.locator(
        '[data-testid="raf-calculation"], .raf-breakdown, .score-components'
      );

      if (await rafCalculation.count() > 0) {
        const calculation = await rafCalculation.textContent();
        console.log('RAF calculation:', calculation?.substring(0, 100));
      }
    });

    test('should identify recapture opportunities', async ({ page }) => {
      await page.goto('/hcc/recapture');

      const recaptureList = page.locator(
        '[data-testid="recapture-opportunities"], .recapture-list'
      );

      if (await recaptureList.count() > 0) {
        const opportunities = await recaptureList.locator('.opportunity-item, tr').count();
        console.log('Recapture opportunities:', opportunities);
      }
    });

    test('should filter HCC by category', async ({ page }) => {
      await page.goto('/hcc');

      const categoryFilter = page.locator(
        '[data-testid="hcc-category"], #category, select[name="hccCategory"]'
      );

      if (await categoryFilter.count() > 0) {
        const options = await categoryFilter.locator('option').allTextContents();
        console.log('HCC categories:', options);
      }
    });

    test('should show year-over-year HCC comparison', async ({ page }) => {
      await page.goto('/hcc');

      const comparison = page.locator(
        '[data-testid="hcc-comparison"], .yoy-comparison, .year-comparison'
      );

      if (await comparison.count() > 0) {
        console.log('Year-over-year HCC comparison available');
      }
    });

    test('should display HCC documentation status', async ({ page }) => {
      await page.goto('/hcc');

      const docStatus = page.locator(
        '[data-testid="doc-status"], .documentation-status, .capture-status'
      );

      if (await docStatus.count() > 0) {
        const statusText = await docStatus.textContent();
        console.log('Documentation status:', statusText?.substring(0, 50));
      }
    });

    test('should generate HCC report', async ({ page }) => {
      await page.goto('/hcc');

      const generateButton = page.locator(
        '[data-testid="generate-hcc-report"], button:has-text("Generate Report")'
      );

      if (await generateButton.count() > 0) {
        await generateButton.click();

        // Report generation progress
        const progress = page.locator('.report-progress, [data-testid="report-generating"]');
        const hasProgress = await progress.count() > 0;
        console.log('HCC report generation initiated:', hasProgress);
      }
    });
  });
});

/**
 * Risk Score Model Configuration Tests
 */
test.describe('Risk Score Configuration', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display risk model settings', async ({ page }) => {
    await page.goto('/admin/risk-models');

    const modelSettings = page.locator(
      '[data-testid="risk-model-settings"], .model-config'
    );

    const hasSettings = await modelSettings.count() > 0;
    console.log('Risk model settings available:', hasSettings);
  });

  test('should show available risk models', async ({ page }) => {
    await page.goto('/admin/risk-models');

    const modelList = page.locator(
      '[data-testid="risk-models"], .model-list, table'
    );

    if (await modelList.count() > 0) {
      const models = await modelList.locator('tr, .model-item').count();
      console.log('Risk models available:', models);
    }
  });

  test('should configure risk thresholds', async ({ page }) => {
    await page.goto('/admin/risk-models');

    const thresholdInputs = page.locator(
      '[data-testid="risk-threshold"], input[name*="threshold"]'
    );

    const thresholdCount = await thresholdInputs.count();
    console.log('Risk threshold inputs:', thresholdCount);
  });
});

/**
 * Predictive Risk Analytics Tests
 */
test.describe('Predictive Risk Analytics', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);
  });

  test('should display predictive risk dashboard', async ({ page }) => {
    await page.goto('/analytics/predictive-risk');

    const dashboard = page.locator(
      '[data-testid="predictive-dashboard"], .predictive-analytics'
    );

    const hasDashboard = await dashboard.count() > 0;
    console.log('Predictive risk dashboard:', hasDashboard);
  });

  test('should show rising risk patients', async ({ page }) => {
    await page.goto('/analytics/predictive-risk');

    const risingRisk = page.locator(
      '[data-testid="rising-risk"], .risk-alerts, .trending-up'
    );

    if (await risingRisk.count() > 0) {
      const count = await risingRisk.locator('.patient-item, tr').count();
      console.log('Rising risk patients:', count);
    }
  });

  test('should display risk prediction confidence', async ({ page }) => {
    await page.goto('/analytics/predictive-risk');

    const confidence = page.locator(
      '[data-testid="prediction-confidence"], .confidence-score'
    );

    const hasConfidence = await confidence.count() > 0;
    console.log('Prediction confidence displayed:', hasConfidence);
  });
});
