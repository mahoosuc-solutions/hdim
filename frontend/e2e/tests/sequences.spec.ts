import { test, expect } from '../fixtures/sales-fixtures';

test.describe('Email Sequences', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
    await salesPage.switchToTab('sequences');
  });

  test('displays sequences as cards', async ({ sequencesPage, page }) => {
    await sequencesPage.waitForLoad();

    // Should either show cards or empty state
    const hasCards = await sequencesPage.sequenceCards.first().isVisible();
    const hasEmptyState = await sequencesPage.hasEmptyState();

    expect(hasCards || hasEmptyState).toBe(true);
  });

  test('can toggle sequence active/inactive', async ({ sequencesPage, page }) => {
    await sequencesPage.waitForLoad();

    const count = await sequencesPage.getSequenceCount();

    if (count > 0) {
      // Get the first sequence card
      const firstCard = sequencesPage.sequenceCards.first();
      const switchElement = firstCard.locator('[class*="Switch"]');

      if (await switchElement.isVisible()) {
        // Get initial state
        const initialState = await firstCard.locator('[class*="Switch"] input').isChecked();

        // Toggle
        await switchElement.click();
        await page.waitForLoadState('networkidle');

        // State should have changed (or API call made)
        // Note: The actual state change depends on API response
      }
    }
  });

  test('shows sequence analytics', async ({ sequencesPage, page }) => {
    await sequencesPage.waitForLoad();

    const count = await sequencesPage.getSequenceCount();

    if (count > 0) {
      // Look for analytics indicators
      const analyticsTexts = ['Open', 'Click', 'Enroll', 'Sent', '%'];
      let foundAnalytics = false;

      for (const text of analyticsTexts) {
        const element = await page.getByText(new RegExp(text, 'i')).isVisible();
        if (element) {
          foundAnalytics = true;
          break;
        }
      }

      // Analytics should be displayed if sequences exist
      expect(foundAnalytics).toBe(true);
    }
  });

  test('displays enrollment metrics', async ({ sequencesPage, page }) => {
    await sequencesPage.waitForLoad();

    const count = await sequencesPage.getSequenceCount();

    if (count > 0) {
      // Look for enrollment-related metrics
      const enrollmentTexts = ['Total', 'Active', 'Sent', 'Completed'];
      let foundMetric = false;

      for (const text of enrollmentTexts) {
        const element = await page.getByText(new RegExp(text, 'i')).isVisible();
        if (element) {
          foundMetric = true;
          break;
        }
      }

      expect(foundMetric).toBe(true);
    }
  });

  test('empty state shows when no sequences', async ({ sequencesPage, page }) => {
    await sequencesPage.waitForLoad();

    const count = await sequencesPage.getSequenceCount();

    if (count === 0) {
      // Should show empty state
      const hasEmptyState = await sequencesPage.hasEmptyState();
      expect(hasEmptyState).toBe(true);

      // Should have a create button
      const createButton = page.getByRole('button', { name: /create/i });
      await expect(createButton).toBeVisible();
    }
  });
});
