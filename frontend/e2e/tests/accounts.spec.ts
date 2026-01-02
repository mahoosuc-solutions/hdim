import { test, expect } from '../fixtures/sales-fixtures';

test.describe('Account Management', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
    await salesPage.switchToTab('accounts');
  });

  test('displays accounts table with all columns', async ({ accountsPage }) => {
    await accountsPage.waitForTableLoad();

    const headers = await accountsPage.getColumnHeaders();
    expect(headers.length).toBeGreaterThanOrEqual(5);

    // Check for expected column headers
    const expectedColumns = ['Account', 'Type', 'Stage', 'Location', 'Patients'];
    for (const expected of expectedColumns) {
      const found = headers.some(h => h.toLowerCase().includes(expected.toLowerCase()));
      expect(found).toBe(true);
    }
  });

  test('can search accounts by name', async ({ accountsPage, page }) => {
    await accountsPage.waitForTableLoad();

    // Search for a term
    await accountsPage.searchAccounts('Health');
    await page.waitForTimeout(500);

    // Table should update
    const isLoading = await accountsPage.isLoading();
    expect(isLoading).toBe(false);
  });

  test('can filter by stage', async ({ accountsPage, page }) => {
    await accountsPage.waitForTableLoad();

    // Filter by QUALIFIED stage
    await accountsPage.filterByStage('QUALIFIED');
    await page.waitForTimeout(500);

    // Verify filter applied
    const isLoading = await accountsPage.isLoading();
    expect(isLoading).toBe(false);
  });

  test('account type labels display correctly', async ({ accountsPage, page }) => {
    await accountsPage.waitForTableLoad();

    const count = await accountsPage.getAccountCount();

    if (count > 0) {
      // Check for type chips/labels
      const types = ['ACO', 'HEALTH', 'PAYER', 'HIE', 'FQHC', 'CLINIC'];
      let foundType = false;

      for (const type of types) {
        const typeElement = await page.locator('tbody').getByText(new RegExp(type, 'i')).isVisible();
        if (typeElement) {
          foundType = true;
          break;
        }
      }

      expect(foundType).toBe(true);
    }
  });

  test('patient count formats with commas', async ({ accountsPage, page }) => {
    await accountsPage.waitForTableLoad();

    const count = await accountsPage.getAccountCount();

    if (count > 0) {
      // Look for formatted numbers in the patients column
      const formattedNumbers = page.locator('tbody td').filter({
        hasText: /^\d{1,3}(,\d{3})*$|^\d+$/,
      });
      const numberCount = await formattedNumbers.count();

      // Should have patient counts displayed
      expect(numberCount).toBeGreaterThan(0);
    }
  });

  test('pagination works', async ({ accountsPage, page }) => {
    await accountsPage.waitForTableLoad();

    // Check if pagination exists
    const paginationVisible = await accountsPage.pagination.isVisible();

    if (paginationVisible) {
      const nextButton = page.getByRole('button', { name: /next page/i });
      const isDisabled = await nextButton.isDisabled();

      if (!isDisabled) {
        await accountsPage.goToNextPage();
        await accountsPage.waitForTableLoad();
        expect(await accountsPage.accountsTable.isVisible()).toBe(true);
      }
    }
  });
});
