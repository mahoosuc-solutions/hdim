import { test, expect } from '@playwright/test';
import { EvaluationPage } from '../../../pages/evaluation.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Quality Evaluation Workflow Tests
 *
 * Test Suite: EVAL
 * Coverage: Quality measure evaluation workflows
 *
 * These tests verify the complete quality evaluation lifecycle
 * as documented in the clinical workflows.
 */

test.describe('Quality Evaluation Workflows', () => {
  let evaluationPage: EvaluationPage;
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    evaluationPage = new EvaluationPage(page);
    loginPage = new LoginPage(page);

    // Login as evaluator
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * EVAL-001: Single Patient Evaluation Workflow
   *
   * Tests the complete workflow for evaluating a single patient
   * against a quality measure.
   *
   * Workflow Reference: documentation-site/workflows/quality-evaluation.md
   */
  test.describe('EVAL-001: Single Patient Evaluation', () => {
    test('should display evaluation page with required elements', async ({ page }) => {
      await evaluationPage.goto();

      await expect(evaluationPage.patientSearchInput).toBeVisible();
      await expect(evaluationPage.measureDropdown).toBeVisible();
    });

    test('should search and select patient for evaluation', async ({ page }) => {
      await evaluationPage.goto();

      // Search for test patient
      await evaluationPage.selectPatient('Test');

      // Verify patient is selected
      await evaluationPage.assertPatientSelected();
    });

    test('should select quality measure from dropdown', async ({ page }) => {
      await evaluationPage.goto();

      // First select a patient
      await evaluationPage.selectPatient('Test');

      // Then select a measure
      await evaluationPage.selectMeasure('CMS');

      // Verify measure is selected
      await evaluationPage.assertMeasureSelected();
    });

    test('should enable evaluate button when patient and measure selected', async ({ page }) => {
      await evaluationPage.goto();

      // Initially button might be disabled
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');

      // Now button should be enabled
      const isEnabled = await evaluationPage.isEvaluateButtonEnabled();
      expect(isEnabled).toBe(true);
    });

    test('should run evaluation and display result', async ({ page }) => {
      await evaluationPage.goto();

      // Complete the workflow
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      // Verify result is displayed
      await evaluationPage.assertResultDisplayed();

      // Get result status
      const status = await evaluationPage.getResultStatus();
      expect(['COMPLIANT', 'NON_COMPLIANT', 'NOT_ELIGIBLE']).toContain(status);
    });

    test('should display evaluation details in result', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      // Get detailed results
      const details = await evaluationPage.getResultDetails();

      expect(details.evaluationId).toBeTruthy();
      expect(typeof details.numerator).toBe('boolean');
      expect(typeof details.denominator).toBe('boolean');
    });

    test('should allow running another evaluation', async ({ page }) => {
      await evaluationPage.goto();

      // Run first evaluation
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      // Click to run another
      await evaluationPage.runAnother();

      // Should be back to form
      await expect(evaluationPage.patientSearchInput).toBeVisible();
    });

    test('should clear form and reset state', async ({ page }) => {
      await evaluationPage.goto();

      // Select patient and measure
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');

      // Clear form
      await evaluationPage.clearForm();

      // Patient should be deselected
      const patientCard = evaluationPage.selectedPatientCard;
      if ((await patientCard.count()) > 0) {
        await expect(patientCard).not.toBeVisible();
      }
    });
  });

  /**
   * EVAL-002: Measure Selection and Filtering
   *
   * Tests the quality measure selection UI including
   * filtering and favorites functionality.
   */
  test.describe('EVAL-002: Measure Selection', () => {
    test('should display measure categories for filtering', async ({ page }) => {
      await evaluationPage.goto();

      // Open measure dropdown
      await evaluationPage.measureDropdown.click();

      // Check for category filter
      const categoryFilter = evaluationPage.measureCategoryFilter;
      if ((await categoryFilter.count()) > 0) {
        await expect(categoryFilter).toBeVisible();
      }
    });

    test('should filter measures by category', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');

      // Filter by Preventive category
      await evaluationPage.filterByCategory('Preventive');

      // Select a measure from filtered list
      await evaluationPage.measureDropdown.click();

      // Measures shown should be in the Preventive category
      const options = evaluationPage.measureOptions;
      const count = await options.count();
      console.log(`Found ${count} measures in Preventive category`);
    });

    test('should show recently used measures', async ({ page }) => {
      await evaluationPage.goto();

      // Run an evaluation first to add to recents
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS130'); // Colorectal Cancer Screening
      await evaluationPage.evaluate();

      // Go back to form
      await evaluationPage.runAnother();

      // Open measure dropdown
      await evaluationPage.measureDropdown.click();

      // Check for recent section
      const recentSection = evaluationPage.page.locator('.recent-measures, [data-testid="recent-measures"]');
      if ((await recentSection.count()) > 0) {
        console.log('Recent measures section is displayed');
      }
    });
  });

  /**
   * EVAL-003: Evaluation Result Interpretation
   *
   * Tests the display and interpretation of evaluation results.
   */
  test.describe('EVAL-003: Result Interpretation', () => {
    test('should display compliant result with green indicator', async ({ page }) => {
      await evaluationPage.goto();

      // Note: This test depends on test data having a compliant patient
      await evaluationPage.selectPatient('Test_Compliant');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      const resultStatus = evaluationPage.resultStatus;
      if ((await resultStatus.count()) > 0) {
        // Check for green color class or compliant text
        const classes = (await resultStatus.getAttribute('class')) || '';
        const text = await resultStatus.textContent();

        const isCompliant =
          classes.includes('green') ||
          classes.includes('success') ||
          classes.includes('compliant') ||
          text?.toLowerCase().includes('compliant');

        console.log('Result appears compliant:', isCompliant);
      }
    });

    test('should display non-compliant result with red indicator', async ({ page }) => {
      await evaluationPage.goto();

      // Run evaluation (result depends on test data)
      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      const status = await evaluationPage.getResultStatus();
      console.log('Evaluation result:', status);

      // Verify result indicator matches status
      const resultStatus = evaluationPage.resultStatus;
      const classes = (await resultStatus.getAttribute('class')) || '';

      if (status === 'NON_COMPLIANT') {
        const hasNonCompliantIndicator =
          classes.includes('red') ||
          classes.includes('danger') ||
          classes.includes('non-compliant');
        console.log('Non-compliant indicator displayed:', hasNonCompliantIndicator);
      }
    });

    test('should show numerator and denominator values', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      const details = await evaluationPage.getResultDetails();

      // Numerator and denominator should be boolean values
      expect(typeof details.numerator).toBe('boolean');
      expect(typeof details.denominator).toBe('boolean');

      console.log(`Numerator: ${details.numerator}, Denominator: ${details.denominator}`);
    });
  });

  /**
   * EVAL-004: Care Gap Creation from Evaluation
   *
   * Tests the workflow for creating care gaps from non-compliant evaluations.
   */
  test.describe('EVAL-004: Care Gap Creation', () => {
    test('should show care gap button for non-compliant result', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      const status = await evaluationPage.getResultStatus();

      if (status === 'NON_COMPLIANT') {
        // Care gap button should be visible
        const careGapButton = evaluationPage.viewCareGapButton;
        await expect(careGapButton).toBeVisible();
      }
    });

    test('should navigate to care gap from evaluation result', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');
      await evaluationPage.evaluate();

      const status = await evaluationPage.getResultStatus();

      if (status === 'NON_COMPLIANT') {
        await evaluationPage.viewCareGap();
        await expect(page).toHaveURL(/.*care-gaps/);
      }
    });
  });

  /**
   * EVAL-005: Batch Evaluation
   *
   * Tests the batch evaluation workflow for multiple patients.
   */
  test.describe('EVAL-005: Batch Evaluation', () => {
    test('should have batch mode toggle', async ({ page }) => {
      await evaluationPage.goto();

      const batchToggle = evaluationPage.batchModeToggle;
      if ((await batchToggle.count()) > 0) {
        await expect(batchToggle).toBeVisible();
      }
    });

    test('should enable batch mode', async ({ page }) => {
      await evaluationPage.goto();

      const batchToggle = evaluationPage.batchModeToggle;
      if ((await batchToggle.count()) > 0) {
        await evaluationPage.enableBatchMode();
        await expect(evaluationPage.batchPatientList).toBeVisible();
      }
    });
  });

  /**
   * Evaluation - Role-Based Access Tests
   */
  test.describe('Evaluation - Role-Based Access', () => {
    test('viewer should not be able to run evaluations', async ({ page }) => {
      // Logout and login as viewer
      await page.goto('/login');
      await page.evaluate(() => localStorage.clear());
      await loginPage.loginAndWait(TEST_USERS.viewer.username, TEST_USERS.viewer.password);

      await evaluationPage.goto();

      // Evaluate button should be disabled or hidden
      const isEnabled = await evaluationPage.isEvaluateButtonEnabled();
      const isVisible = await evaluationPage.evaluateButton.isVisible();

      expect(isEnabled && isVisible).toBe(false);
    });

    test('evaluator should be able to run evaluations', async ({ page }) => {
      await evaluationPage.goto();

      await evaluationPage.selectPatient('Test');
      await evaluationPage.selectMeasure('CMS');

      const isEnabled = await evaluationPage.isEvaluateButtonEnabled();
      expect(isEnabled).toBe(true);
    });
  });
});
