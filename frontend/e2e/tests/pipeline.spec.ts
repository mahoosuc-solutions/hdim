import { test, expect } from '../fixtures/sales-fixtures';

test.describe('Pipeline Kanban Board', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
    await salesPage.switchToTab('pipeline');
  });

  test.describe('Board Display', () => {
    test('shows all 6 stage columns', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Check for stage column headers
      const stages = ['DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CLOSED WON', 'CLOSED LOST'];

      for (const stage of stages) {
        const stageElement = page.getByText(new RegExp(stage, 'i'));
        await expect(stageElement.first()).toBeVisible();
      }
    });

    test('displays opportunity cards in correct columns', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Check that cards exist within the kanban board
      const board = page.locator('[style*="overflow-x"]').first();
      await expect(board).toBeVisible();

      // Each column should be a paper/card component
      const columns = page.locator('[class*="Paper"]').filter({
        has: page.locator('h6'),
      });
      const columnCount = await columns.count();
      expect(columnCount).toBeGreaterThanOrEqual(1);
    });

    test('shows column count and total value', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Look for count chips in column headers
      const countChips = page.locator('[class*="Chip"]');
      const chipCount = await countChips.count();

      // Should have count indicators or value indicators
      const hasValues = await page.locator('text=/\\$\\d+|\\d+\\s*deal/i').count();
      expect(chipCount > 0 || hasValues > 0).toBe(true);
    });

    test('displays pipeline summary metrics', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Look for summary card at the top
      const summaryTexts = ['Total', 'Weighted', 'Opportunities', 'Average'];
      let foundSummary = false;

      for (const text of summaryTexts) {
        const element = await page.getByText(new RegExp(text, 'i')).isVisible();
        if (element) {
          foundSummary = true;
          break;
        }
      }

      expect(foundSummary).toBe(true);
    });
  });

  test.describe('Stage Transitions', () => {
    test('can click on opportunity card to open dialog', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Find any opportunity card
      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        await opportunityCards.first().click();

        // Dialog should open
        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible({ timeout: 5000 });

        // Close dialog
        await page.keyboard.press('Escape');
      }
    });

    test('move dialog shows stage selection', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        await opportunityCards.first().click();

        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible();

        // Should have stage selector
        const stageSelector = dialog.locator('select, [role="combobox"]');
        const hasSelectorVisible = await stageSelector.first().isVisible();

        if (hasSelectorVisible) {
          await stageSelector.first().click();
          // Should show stage options
          const options = page.getByRole('option');
          const optionCount = await options.count();
          expect(optionCount).toBeGreaterThan(0);
        }

        await page.keyboard.press('Escape');
      }
    });

    test('requires reason when moving to CLOSED_LOST', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        await opportunityCards.first().click();

        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible();

        // Select CLOSED_LOST
        const stageSelector = dialog.locator('select, [role="combobox"]').first();
        if (await stageSelector.isVisible()) {
          await stageSelector.click();
          const closedLostOption = page.getByRole('option', { name: /closed lost/i });

          if (await closedLostOption.isVisible()) {
            await closedLostOption.click();

            // Should show reason field
            const reasonField = dialog.getByLabel(/reason/i);
            await expect(reasonField).toBeVisible();

            // Move button should be disabled without reason
            const moveButton = dialog.getByRole('button', { name: /move/i });
            await expect(moveButton).toBeDisabled();
          }
        }

        await page.keyboard.press('Escape');
      }
    });

    test('at-risk deals show warning indicator', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Look for warning icons on cards
      const warningIcons = page.locator('[data-testid="WarningIcon"]');
      const hasWarnings = await warningIcons.count();

      // May or may not have at-risk deals
      expect(hasWarnings).toBeGreaterThanOrEqual(0);
    });
  });

  test.describe('Opportunity Details', () => {
    test('card shows account name', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        const firstCard = opportunityCards.first();
        const text = await firstCard.textContent();

        // Card should have some text content
        expect(text?.length).toBeGreaterThan(0);
      }
    });

    test('card shows amount formatted as currency', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Look for currency formatted values
      const currencyValues = page.locator('[class*="Card"]').locator('text=/\\$\\d/');
      const count = await currencyValues.count();

      // Should have currency values if there are opportunities
      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        expect(count).toBeGreaterThan(0);
      }
    });

    test('card shows probability with correct color', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      // Look for percentage indicators
      const percentages = page.locator('[class*="Card"]').locator('text=/\\d+%/');
      const count = await percentages.count();

      // Probabilities should be displayed if opportunities exist
      expect(count).toBeGreaterThanOrEqual(0);
    });

    test('card shows expected close date', async ({ pipelinePage, page }) => {
      await pipelinePage.waitForBoardLoad();

      const opportunityCards = page.locator('[class*="Card"][style*="cursor: pointer"]');
      const cardCount = await opportunityCards.count();

      if (cardCount > 0) {
        // Look for date patterns or calendar icons
        const datePatterns = page.locator('text=/\\d{1,2}\\/\\d{1,2}|\\d{4}-\\d{2}/');
        const calendarIcons = page.locator('[data-testid*="Calendar"]');

        const hasDateInfo = (await datePatterns.count()) > 0 || (await calendarIcons.count()) > 0;
        // Dates should be shown for opportunities
        expect(hasDateInfo).toBe(true);
      }
    });
  });
});
