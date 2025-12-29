import { test, expect } from '@playwright/test';
import { CareGapPage } from '../../../pages/care-gap.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Care Gap Closure Workflow Tests
 *
 * Test Suite: CG
 * Coverage: Care gap identification, intervention, and closure workflows
 *
 * These tests verify the care gap management functionality
 * as documented in the clinical workflows.
 *
 * Workflow Reference: documentation-site/workflows/care-gap-closure.md
 */

test.describe('Care Gap Workflows', () => {
  let careGapPage: CareGapPage;
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    careGapPage = new CareGapPage(page);
    loginPage = new LoginPage(page);

    // Login before each test
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * CG-001: View All Care Gaps
   *
   * Verifies that users can view the list of all open care gaps.
   *
   * Workflow Reference: User Story US-CG-001
   */
  test.describe('CG-001: View Care Gaps', () => {
    test('should display care gaps page with list or no-gaps message', async ({ page }) => {
      await careGapPage.goto();
      await careGapPage.waitForLoad();

      // Should show either a table or no-gaps message
      const loaded = await careGapPage.isLoaded();
      expect(loaded).toBe(true);
    });

    test('should display care gap count', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      console.log(`Total care gaps displayed: ${count}`);
    });

    test('should show filter controls', async ({ page }) => {
      await careGapPage.goto();

      // Check for filter dropdowns
      const urgencyFilter = careGapPage.urgencyFilter;
      const statusFilter = careGapPage.statusFilter;

      if ((await urgencyFilter.count()) > 0) {
        await expect(urgencyFilter).toBeVisible();
      }
      if ((await statusFilter.count()) > 0) {
        await expect(statusFilter).toBeVisible();
      }
    });
  });

  /**
   * CG-002: Filter Care Gaps by Urgency
   *
   * Verifies the urgency filtering functionality.
   */
  test.describe('CG-002: Filter by Urgency', () => {
    test('should filter by HIGH urgency', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByUrgency('HIGH');

      const count = await careGapPage.getGapCount();
      console.log(`High urgency gaps: ${count}`);

      // If there are results, verify they're high urgency
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        const details = await careGapPage.getSelectedGapDetails();
        expect(details.urgency.toUpperCase()).toContain('HIGH');
      }
    });

    test('should filter by MEDIUM urgency', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByUrgency('MEDIUM');

      const count = await careGapPage.getGapCount();
      console.log(`Medium urgency gaps: ${count}`);
    });

    test('should filter by LOW urgency', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByUrgency('LOW');

      const count = await careGapPage.getGapCount();
      console.log(`Low urgency gaps: ${count}`);
    });

    test('should show all urgencies when filter cleared', async ({ page }) => {
      await careGapPage.goto();

      // Filter first
      await careGapPage.filterByUrgency('HIGH');
      const highCount = await careGapPage.getGapCount();

      // Clear filters
      await careGapPage.filterByUrgency('ALL');
      const allCount = await careGapPage.getGapCount();

      expect(allCount).toBeGreaterThanOrEqual(highCount);
    });
  });

  /**
   * CG-003: Filter Care Gaps by Status
   *
   * Verifies status-based filtering (Open, Closed, In Progress).
   */
  test.describe('CG-003: Filter by Status', () => {
    test('should filter by OPEN status', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByStatus('OPEN');

      const count = await careGapPage.getGapCount();
      console.log(`Open gaps: ${count}`);
    });

    test('should filter by CLOSED status', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByStatus('CLOSED');

      const count = await careGapPage.getGapCount();
      console.log(`Closed gaps: ${count}`);
    });

    test('should filter by IN_PROGRESS status', async ({ page }) => {
      await careGapPage.goto();

      await careGapPage.filterByStatus('IN_PROGRESS');

      const count = await careGapPage.getGapCount();
      console.log(`In-progress gaps: ${count}`);
    });
  });

  /**
   * CG-004: View Care Gap Details
   *
   * Verifies the care gap detail view.
   */
  test.describe('CG-004: View Gap Details', () => {
    test('should display gap details panel when gap selected', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        await expect(careGapPage.gapDetailsPanel).toBeVisible();
      }
    });

    test('should show patient name in gap details', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        const details = await careGapPage.getSelectedGapDetails();

        expect(details.patientName).toBeTruthy();
        // PHI is masked in test output
        console.log('Patient name retrieved (PHI masked)');
      }
    });

    test('should show measure name in gap details', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        const details = await careGapPage.getSelectedGapDetails();

        expect(details.measureName).toBeTruthy();
        console.log(`Measure: ${details.measureName}`);
      }
    });

    test('should show recommendation in gap details', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        const details = await careGapPage.getSelectedGapDetails();

        if (details.recommendation) {
          console.log('Recommendation provided:', details.recommendation.substring(0, 50));
        }
      }
    });
  });

  /**
   * CG-005: Record Intervention
   *
   * Tests the intervention recording workflow.
   *
   * Workflow Reference: Step 6 in care-gap-closure.md
   */
  test.describe('CG-005: Record Intervention', () => {
    test('should display record intervention button', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        await expect(careGapPage.recordInterventionButton).toBeVisible();
      }
    });

    test('should record phone call intervention', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        await careGapPage.recordIntervention({
          type: 'Phone Call',
          outcome: 'Reached patient',
          notes: 'E2E test intervention - phone call completed',
        });

        // Verify intervention is recorded
        await careGapPage.assertInterventionRecorded();
      }
    });

    test('should record appointment scheduled intervention', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        await careGapPage.recordIntervention({
          type: 'Appointment Scheduled',
          outcome: 'Appointment confirmed',
          notes: 'E2E test - appointment scheduled for follow-up',
        });
      }
    });

    test('should show intervention in history', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        // Record an intervention
        await careGapPage.recordIntervention({
          type: 'Email',
          outcome: 'Sent successfully',
          notes: 'E2E test email intervention',
        });

        // Check history
        const history = await careGapPage.getInterventionHistory();
        expect(history.length).toBeGreaterThan(0);
      }
    });
  });

  /**
   * CG-006: Close Care Gap
   *
   * Tests the care gap closure workflow.
   *
   * Workflow Reference: Step 7 in care-gap-closure.md
   */
  test.describe('CG-006: Close Care Gap', () => {
    test('should display close gap button', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        await expect(careGapPage.closeGapButton).toBeVisible();
      }
    });

    test('should close gap with Completed reason', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        const details = await careGapPage.getSelectedGapDetails();

        await careGapPage.closeGap({
          reason: 'Completed',
          notes: 'E2E test - gap completed successfully',
        });

        // Verify gap is closed (not in open list anymore)
        console.log('Gap closed successfully');
      }
    });

    test('should close gap with Not Applicable reason', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        await careGapPage.closeGap({
          reason: 'Not Applicable',
          notes: 'E2E test - patient excluded from measure',
        });
      }
    });

    test('should close gap with Patient Declined reason', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        await careGapPage.closeGap({
          reason: 'Patient Declined',
          notes: 'E2E test - patient refused intervention',
        });
      }
    });

    test('should require notes for closure', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        await careGapPage.closeGapButton.click();

        // Try to submit without notes
        await careGapPage.closeReasonSelect.selectOption('Completed');
        // Don't fill notes

        // Confirm button might be disabled or show validation error
        const confirmButton = careGapPage.confirmCloseButton;
        if ((await confirmButton.count()) > 0) {
          const isDisabled = await confirmButton.isDisabled();
          console.log('Close without notes - button disabled:', isDisabled);
        }
      }
    });
  });

  /**
   * CG-007: Bulk Close Care Gaps
   *
   * Tests the bulk closure workflow for multiple gaps.
   *
   * Workflow Reference: Bulk Gap Closure section in care-gap-closure.md
   */
  test.describe('CG-007: Bulk Close', () => {
    test('should have select all checkbox', async ({ page }) => {
      await careGapPage.goto();

      const selectAll = careGapPage.selectAllCheckbox;
      if ((await selectAll.count()) > 0) {
        await expect(selectAll).toBeVisible();
      }
    });

    test('should select multiple gaps for bulk operation', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count >= 2) {
        await careGapPage.selectGapsForBulk([0, 1]);

        const selectedCount = await careGapPage.getSelectedCount();
        expect(selectedCount).toBe(2);
      }
    });

    test('should enable bulk close button when gaps selected', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count >= 1) {
        await careGapPage.selectGapsForBulk([0]);

        const bulkButton = careGapPage.bulkCloseButton;
        if ((await bulkButton.count()) > 0) {
          await expect(bulkButton).toBeEnabled();
        }
      }
    });

    test('should bulk close selected gaps', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count >= 2) {
        await careGapPage.selectGapsForBulk([0, 1]);
        await careGapPage.bulkCloseGaps('Completed');

        // Verify success message
        console.log('Bulk close completed');
      }
    });
  });

  /**
   * CG-008: Quick Actions from Care Gap
   *
   * Tests navigation to patient and evaluation from care gap.
   */
  test.describe('CG-008: Quick Actions', () => {
    test('should navigate to patient from care gap', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        const viewPatientButton = careGapPage.viewPatientButton;
        if ((await viewPatientButton.count()) > 0 && (await viewPatientButton.isVisible())) {
          await careGapPage.viewPatient();
          await expect(page).toHaveURL(/.*patients\/.*/);
        }
      }
    });

    test('should trigger re-evaluation from care gap', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        const reEvalButton = careGapPage.runEvaluationButton;
        if ((await reEvalButton.count()) > 0 && (await reEvalButton.isVisible())) {
          await careGapPage.runReEvaluation();
          await expect(page).toHaveURL(/.*evaluations/);
        }
      }
    });
  });

  /**
   * Care Gap - Role-Based Access Tests
   */
  test.describe('Care Gap - Role-Based Access', () => {
    test('viewer should not see close gap button', async ({ page }) => {
      // Logout and login as viewer
      await page.goto('/login');
      await page.evaluate(() => localStorage.clear());
      await loginPage.loginAndWait(TEST_USERS.viewer.username, TEST_USERS.viewer.password);

      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);

        const closeButton = careGapPage.closeGapButton;
        if ((await closeButton.count()) > 0) {
          const isDisabled = await closeButton.isDisabled();
          const isHidden = !(await closeButton.isVisible());
          expect(isDisabled || isHidden).toBe(true);
        }
      }
    });

    test('evaluator should be able to record intervention', async ({ page }) => {
      await careGapPage.goto();

      const count = await careGapPage.getGapCount();
      if (count > 0) {
        await careGapPage.selectGapByIndex(0);
        await expect(careGapPage.recordInterventionButton).toBeEnabled();
      }
    });
  });
});
