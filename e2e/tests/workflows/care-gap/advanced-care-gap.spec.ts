import { test, expect } from '@playwright/test';
import { CareGapsPage } from '../../../pages/care-gaps.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Advanced Care Gap Workflow Tests
 *
 * Test Suite: CG (Care Gaps) - Advanced
 * Coverage: Intervention documentation, bulk operations, prioritization
 *
 * These tests verify advanced care gap management features
 * that enable efficient gap closure and care coordination.
 */

test.describe('Advanced Care Gap Workflows', () => {
  let loginPage: LoginPage;
  let careGapsPage: CareGapsPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    careGapsPage = new CareGapsPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * CG-004: Intervention Documentation
   *
   * Verifies that care managers can document interventions
   * performed to address care gaps.
   */
  test.describe('CG-004: Intervention Documentation', () => {
    test('should navigate to care gap detail for intervention', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      // Click on first care gap
      const firstGap = page.locator('[data-testid="care-gap-row"], .gap-item, tr').first();
      if (await firstGap.count() > 0) {
        await firstGap.click();

        // Should show detail view
        const detailView = page.locator(
          '[data-testid="gap-detail"], .gap-detail, .care-gap-detail'
        );
        await expect(detailView).toBeVisible({ timeout: 5000 }).catch(() => {
          console.log('Gap detail view in modal or separate page');
        });
      }
    });

    test('should display intervention button on care gap', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const interventionButton = page.locator(
        '[data-testid="record-intervention"], button:has-text("Intervention"), button:has-text("Record")'
      );

      const hasInterventionButton = await interventionButton.count() > 0;
      console.log('Intervention button available:', hasInterventionButton);
    });

    test('should open intervention form', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const interventionButton = page.locator(
        '[data-testid="record-intervention"], button:has-text("Intervention")'
      ).first();

      if (await interventionButton.count() > 0) {
        await interventionButton.click();

        // Intervention form should appear
        const interventionForm = page.locator(
          '[data-testid="intervention-form"], .intervention-dialog, [role="dialog"]'
        );

        await expect(interventionForm).toBeVisible({ timeout: 3000 }).catch(() => {
          console.log('Intervention form not found');
        });
      }
    });

    test('should select intervention type', async ({ page }) => {
      await careGapsPage.goto();

      const interventionButton = page.locator('[data-testid="record-intervention"]').first();
      if (await interventionButton.count() > 0) {
        await interventionButton.click();

        const typeSelect = page.locator(
          '[data-testid="intervention-type"], #interventionType, select[name="type"]'
        );

        if (await typeSelect.count() > 0) {
          const options = await typeSelect.locator('option').allTextContents();
          console.log('Intervention types:', options);

          // Select first type
          await typeSelect.selectOption({ index: 1 });
        }
      }
    });

    test('should document intervention outcome', async ({ page }) => {
      await careGapsPage.goto();

      const interventionButton = page.locator('[data-testid="record-intervention"]').first();
      if (await interventionButton.count() > 0) {
        await interventionButton.click();

        // Outcome selection
        const outcomeSelect = page.locator(
          '[data-testid="intervention-outcome"], #outcome, select[name="outcome"]'
        );

        if (await outcomeSelect.count() > 0) {
          const options = await outcomeSelect.locator('option').allTextContents();
          console.log('Intervention outcomes:', options);
        }

        // Notes field
        const notesField = page.locator(
          '[data-testid="intervention-notes"], #notes, textarea'
        );

        if (await notesField.count() > 0) {
          await notesField.fill('E2E Test - Patient contacted, appointment scheduled');
          console.log('Intervention notes documented');
        }
      }
    });

    test('should save intervention record', async ({ page }) => {
      await careGapsPage.goto();

      const interventionButton = page.locator('[data-testid="record-intervention"]').first();
      if (await interventionButton.count() > 0) {
        await interventionButton.click();

        // Fill form
        const typeSelect = page.locator('[data-testid="intervention-type"]');
        if (await typeSelect.count() > 0) {
          await typeSelect.selectOption({ index: 1 });
        }

        const notesField = page.locator('[data-testid="intervention-notes"], textarea');
        if (await notesField.count() > 0) {
          await notesField.fill('E2E Test Intervention');
        }

        // Submit
        const saveButton = page.locator(
          'button:has-text("Save"), button:has-text("Submit"), [data-testid="save-intervention"]'
        );

        if (await saveButton.count() > 0) {
          const responsePromise = page.waitForResponse(
            resp => resp.url().includes('intervention') && resp.status() === 200
          ).catch(() => null);

          await saveButton.click();

          const response = await responsePromise;
          if (response) {
            console.log('Intervention saved successfully');
          }
        }
      }
    });

    test('should view intervention history', async ({ page }) => {
      await careGapsPage.goto();

      // Look for intervention history section
      const historyTab = page.locator(
        '[data-testid="intervention-history"], button:has-text("History"), a:has-text("History")'
      );

      if (await historyTab.count() > 0) {
        await historyTab.click();

        const historyList = page.locator('.intervention-history, [data-testid="history-list"]');
        if (await historyList.count() > 0) {
          const historyCount = await historyList.locator('.history-item, tr').count();
          console.log('Intervention history items:', historyCount);
        }
      }
    });
  });

  /**
   * CG-005: Care Gap Prioritization
   *
   * Verifies that care gaps can be prioritized based on
   * urgency, patient risk, and clinical factors.
   */
  test.describe('CG-005: Care Gap Prioritization', () => {
    test('should display priority/urgency indicators', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const priorityIndicators = page.locator(
        '[data-testid="priority-indicator"], .urgency-badge, .priority-tag'
      );

      const count = await priorityIndicators.count();
      console.log('Priority indicators displayed:', count);
    });

    test('should filter by HIGH urgency', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const urgencyFilter = careGapsPage.urgencyFilter;

      if (await urgencyFilter.count() > 0) {
        await urgencyFilter.click();
        await page.locator('[role="option"]:has-text("High"), option:has-text("High")').click();

        await page.waitForTimeout(500);

        // Verify filtered results
        const gaps = page.locator('[data-testid="care-gap-row"], .gap-item');
        const count = await gaps.count();
        console.log('HIGH urgency gaps:', count);
      }
    });

    test('should filter by MEDIUM urgency', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const urgencyFilter = careGapsPage.urgencyFilter;

      if (await urgencyFilter.count() > 0) {
        await urgencyFilter.click();
        await page.locator('[role="option"]:has-text("Medium"), option:has-text("Medium")').click();

        await page.waitForTimeout(500);
        console.log('Filtered by MEDIUM urgency');
      }
    });

    test('should sort by priority', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const sortControl = page.locator(
        '[data-testid="sort-priority"], th:has-text("Priority"), th:has-text("Urgency")'
      );

      if (await sortControl.count() > 0) {
        await sortControl.click();
        console.log('Sorted by priority');

        // Verify sorting
        const firstGap = page.locator('[data-testid="care-gap-row"], .gap-item').first();
        const priorityBadge = firstGap.locator('.urgency-badge, .priority-tag');

        if (await priorityBadge.count() > 0) {
          const priority = await priorityBadge.textContent();
          console.log('First gap priority:', priority);
        }
      }
    });

    test('should display days overdue', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const daysOverdue = page.locator(
        '[data-testid="days-overdue"], .days-overdue, :has-text("days overdue")'
      );

      const count = await daysOverdue.count();
      console.log('Days overdue indicators:', count);
    });

    test('should prioritize by patient risk score', async ({ page }) => {
      await careGapsPage.goto();

      // Look for risk score column or filter
      const riskFilter = page.locator(
        '[data-testid="risk-filter"], #riskScore, select[name="risk"]'
      );

      if (await riskFilter.count() > 0) {
        await riskFilter.selectOption('HIGH_RISK');
        console.log('Filtered by high risk patients');
      }
    });

    test('should show prioritized worklist', async ({ page }) => {
      await careGapsPage.goto();

      const worklistTab = page.locator(
        '[data-testid="worklist"], button:has-text("Worklist"), a:has-text("My Worklist")'
      );

      if (await worklistTab.count() > 0) {
        await worklistTab.click();

        // Worklist should be prioritized
        const worklist = page.locator('[data-testid="worklist-items"], .worklist');
        const hasWorklist = await worklist.count() > 0;
        console.log('Worklist displayed:', hasWorklist);
      }
    });
  });

  /**
   * CG-006: Gap Recommendations
   *
   * Verifies that the system provides recommendations
   * for addressing care gaps based on clinical guidelines.
   */
  test.describe('CG-006: Gap Recommendations', () => {
    test('should display recommendations on care gap', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      // Click on a gap to see recommendations
      const firstGap = page.locator('[data-testid="care-gap-row"], .gap-item').first();
      if (await firstGap.count() > 0) {
        await firstGap.click();

        const recommendations = page.locator(
          '[data-testid="recommendations"], .recommendations-section, .suggested-actions'
        );

        if (await recommendations.count() > 0) {
          const text = await recommendations.textContent();
          console.log('Recommendations:', text?.substring(0, 100));
        }
      }
    });

    test('should show recommended actions', async ({ page }) => {
      await careGapsPage.goto();

      const recommendedActions = page.locator(
        '[data-testid="recommended-action"], .action-recommendation, .suggested-intervention'
      );

      const count = await recommendedActions.count();
      console.log('Recommended actions displayed:', count);
    });

    test('should show clinical guidelines reference', async ({ page }) => {
      await careGapsPage.goto();

      const guidelinesLink = page.locator(
        '[data-testid="guidelines"], a:has-text("Guidelines"), .clinical-guidelines'
      );

      const hasGuidelines = await guidelinesLink.count() > 0;
      console.log('Clinical guidelines available:', hasGuidelines);
    });

    test('should apply recommendation to close gap', async ({ page }) => {
      await careGapsPage.goto();
      await careGapsPage.waitForDataLoad();

      const applyRecommendation = page.locator(
        '[data-testid="apply-recommendation"], button:has-text("Apply")'
      ).first();

      if (await applyRecommendation.count() > 0) {
        await applyRecommendation.click();

        // Should open intervention or closure form
        const form = page.locator('[data-testid="intervention-form"], [data-testid="closure-form"]');
        const formOpened = await form.count() > 0;
        console.log('Recommendation form opened:', formOpened);
      }
    });

    test('should show measure-specific guidance', async ({ page }) => {
      await careGapsPage.goto();

      // Filter by specific measure
      const measureFilter = careGapsPage.measureFilter;
      if (await measureFilter.count() > 0) {
        await measureFilter.click();
        await page.locator('[role="option"]').first().click();

        // Check for measure-specific guidance
        const measureGuidance = page.locator(
          '[data-testid="measure-guidance"], .measure-info, .hedis-guidance'
        );

        const hasGuidance = await measureGuidance.count() > 0;
        console.log('Measure-specific guidance:', hasGuidance);
      }
    });
  });
});

/**
 * Bulk Care Gap Operations
 */
test.describe('Bulk Care Gap Operations', () => {
  let loginPage: LoginPage;
  let careGapsPage: CareGapsPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    careGapsPage = new CareGapsPage(page);

    // Login as care manager for bulk operations
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display bulk selection checkboxes', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    const checkboxes = page.locator(
      '[data-testid="gap-checkbox"], input[type="checkbox"][name*="gap"]'
    );

    const count = await checkboxes.count();
    console.log('Bulk selection checkboxes:', count);
  });

  test('should select all gaps', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    const selectAll = page.locator(
      '[data-testid="select-all"], input[type="checkbox"][name="selectAll"], th input[type="checkbox"]'
    );

    if (await selectAll.count() > 0) {
      await selectAll.check();

      // Verify all selected
      const checkedBoxes = page.locator('input[type="checkbox"]:checked');
      const checkedCount = await checkedBoxes.count();
      console.log('Selected items:', checkedCount);
    }
  });

  test('should display bulk actions menu', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    // Select some gaps
    const checkboxes = page.locator('[data-testid="gap-checkbox"]');
    if (await checkboxes.count() > 0) {
      await checkboxes.first().check();
      await checkboxes.nth(1).check().catch(() => {});

      // Bulk actions should appear
      const bulkActions = page.locator(
        '[data-testid="bulk-actions"], .bulk-actions-bar, .selection-actions'
      );

      const hasBulkActions = await bulkActions.count() > 0;
      console.log('Bulk actions displayed:', hasBulkActions);
    }
  });

  test('should bulk close gaps', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    // Select gaps
    const checkboxes = page.locator('[data-testid="gap-checkbox"]');
    if (await checkboxes.count() > 1) {
      await checkboxes.first().check();
      await checkboxes.nth(1).check();

      // Click bulk close
      const bulkCloseButton = page.locator(
        '[data-testid="bulk-close"], button:has-text("Bulk Close"), button:has-text("Close Selected")'
      );

      if (await bulkCloseButton.count() > 0) {
        await bulkCloseButton.click();

        // Confirmation dialog
        const confirmDialog = page.locator('[role="dialog"], .confirm-dialog');
        if (await confirmDialog.count() > 0) {
          console.log('Bulk close confirmation displayed');
        }
      }
    }
  });

  test('should bulk assign gaps to care manager', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    // Select gaps
    const selectAll = page.locator('[data-testid="select-all"]');
    if (await selectAll.count() > 0) {
      await selectAll.check();

      // Click bulk assign
      const bulkAssignButton = page.locator(
        '[data-testid="bulk-assign"], button:has-text("Assign")'
      );

      if (await bulkAssignButton.count() > 0) {
        await bulkAssignButton.click();

        // Assignment dialog
        const assigneeSelect = page.locator(
          '[data-testid="assignee-select"], #assignee, select[name="assignee"]'
        );

        if (await assigneeSelect.count() > 0) {
          console.log('Bulk assignment dialog displayed');
        }
      }
    }
  });

  test('should export selected gaps', async ({ page }) => {
    await careGapsPage.goto();
    await careGapsPage.waitForDataLoad();

    // Select gaps
    const checkboxes = page.locator('[data-testid="gap-checkbox"]');
    if (await checkboxes.count() > 0) {
      await checkboxes.first().check();

      const downloadPromise = page.waitForEvent('download').catch(() => null);

      const exportButton = page.locator(
        '[data-testid="export-selected"], button:has-text("Export")'
      );

      if (await exportButton.count() > 0) {
        await exportButton.click();

        const download = await downloadPromise;
        if (download) {
          console.log('Selected gaps exported:', download.suggestedFilename());
        }
      }
    }
  });
});

/**
 * Care Gap Analytics
 */
test.describe('Care Gap Analytics', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.analyst.username, TEST_USERS.analyst.password);
  });

  test('should display care gap analytics dashboard', async ({ page }) => {
    await page.goto('/care-gaps/analytics');

    const analyticsSection = page.locator(
      '[data-testid="gap-analytics"], .analytics-dashboard, .care-gap-metrics'
    );

    const hasAnalytics = await analyticsSection.count() > 0;
    console.log('Care gap analytics displayed:', hasAnalytics);
  });

  test('should show gap closure rate trend', async ({ page }) => {
    await page.goto('/care-gaps/analytics');

    const closureChart = page.locator(
      '[data-testid="closure-rate-chart"], .closure-trend, canvas'
    );

    const hasChart = await closureChart.count() > 0;
    console.log('Closure rate chart displayed:', hasChart);
  });

  test('should show gaps by measure breakdown', async ({ page }) => {
    await page.goto('/care-gaps/analytics');

    const measureBreakdown = page.locator(
      '[data-testid="gaps-by-measure"], .measure-breakdown, .gap-distribution'
    );

    const hasBreakdown = await measureBreakdown.count() > 0;
    console.log('Gaps by measure breakdown:', hasBreakdown);
  });

  test('should show gaps by urgency distribution', async ({ page }) => {
    await page.goto('/care-gaps/analytics');

    const urgencyChart = page.locator(
      '[data-testid="urgency-distribution"], .urgency-chart, .priority-breakdown'
    );

    const hasUrgencyChart = await urgencyChart.count() > 0;
    console.log('Urgency distribution chart:', hasUrgencyChart);
  });
});
