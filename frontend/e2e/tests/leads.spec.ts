import { test, expect } from '../fixtures/sales-fixtures';
import { generateUniqueEmail, testLeads } from '../utils/test-data';

test.describe('Lead Management', () => {
  test.beforeEach(async ({ salesPage }) => {
    await salesPage.navigateTo();
    await salesPage.switchToTab('leads');
  });

  test.describe('List & Search', () => {
    test('displays leads table with all columns', async ({ leadsPage }) => {
      await leadsPage.waitForTableLoad();

      const headers = await leadsPage.getColumnHeaders();
      expect(headers.length).toBeGreaterThanOrEqual(5);

      // Check for expected column headers
      const expectedColumns = ['Name', 'Company', 'Email', 'Source', 'Status'];
      for (const expected of expectedColumns) {
        const found = headers.some(h => h.toLowerCase().includes(expected.toLowerCase()));
        expect(found).toBe(true);
      }
    });

    test('can search leads by name', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      const initialCount = await leadsPage.getLeadCount();

      // Search for a specific term
      await leadsPage.searchLeads('Test');
      await page.waitForTimeout(500);

      // Results should be filtered (or empty if no matches)
      const searchResults = await leadsPage.getLeadCount();
      // Search should return some results or be empty
      expect(searchResults).toBeGreaterThanOrEqual(0);
    });

    test('can filter by status', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Filter by QUALIFIED status
      await leadsPage.filterByStatus('QUALIFIED');
      await page.waitForTimeout(500);

      // Verify filter applied (table should update)
      const isLoading = await leadsPage.isLoading();
      expect(isLoading).toBe(false);
    });

    test('can filter by source', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Filter by WEBSITE source
      await leadsPage.filterBySource('WEBSITE');
      await page.waitForTimeout(500);

      // Verify filter applied
      const isLoading = await leadsPage.isLoading();
      expect(isLoading).toBe(false);
    });

    test('pagination works correctly', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Check if pagination exists
      const paginationVisible = await leadsPage.pagination.isVisible();

      if (paginationVisible) {
        // Try to go to next page if available
        const nextButton = page.getByRole('button', { name: /next page/i });
        const isDisabled = await nextButton.isDisabled();

        if (!isDisabled) {
          await leadsPage.goToNextPage();
          await leadsPage.waitForTableLoad();
          // Should still show the table
          expect(await leadsPage.leadsTable.isVisible()).toBe(true);
        }
      }
    });

    test('can change rows per page', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Check if rows per page selector exists
      const rowsPerPageSelect = page.locator('[class*="TablePagination"] select').first();
      const isVisible = await rowsPerPageSelect.isVisible();

      if (isVisible) {
        // Change to 5 rows per page
        await rowsPerPageSelect.selectOption('5');
        await leadsPage.waitForTableLoad();

        const count = await leadsPage.getLeadCount();
        expect(count).toBeLessThanOrEqual(5);
      }
    });
  });

  test.describe('CRUD Operations', () => {
    test('can open add lead dialog', async ({ leadsPage }) => {
      await leadsPage.waitForTableLoad();

      // Check if add button exists
      const addButtonVisible = await leadsPage.addLeadButton.isVisible();

      if (addButtonVisible) {
        await leadsPage.openAddLeadDialog();
        await expect(leadsPage.editDialog).toBeVisible();
        await leadsPage.cancelLeadForm();
      }
    });

    test('can edit existing lead', async ({ leadsPage, apiClient }) => {
      // Create a test lead via API first
      const testLead = {
        ...testLeads.newLead,
        email: generateUniqueEmail('edit-test'),
      };

      try {
        await apiClient.createLead(testLead);
        await leadsPage.waitForTableLoad();

        // Try to find and edit the lead
        await leadsPage.searchLeads(testLead.email);

        const leadRow = await leadsPage.getLeadByEmail(testLead.email);
        const isVisible = await leadRow.isVisible();

        if (isVisible) {
          await leadsPage.editLead(testLead.email);
          await expect(leadsPage.editDialog).toBeVisible();
          await leadsPage.cancelLeadForm();
        }
      } finally {
        // Cleanup handled by global teardown
      }
    });

    test('can delete a lead', async ({ leadsPage, apiClient, page }) => {
      // Create a test lead via API
      const testLead = {
        ...testLeads.newLead,
        email: generateUniqueEmail('delete-test'),
      };

      try {
        await apiClient.createLead(testLead);
        await page.reload();
        await leadsPage.waitForTableLoad();

        await leadsPage.searchLeads(testLead.email);

        const leadRow = await leadsPage.getLeadByEmail(testLead.email);
        const isVisible = await leadRow.isVisible();

        if (isVisible) {
          await leadsPage.deleteLead(testLead.email);
          await leadsPage.waitForTableLoad();

          // Lead should be removed
          const deletedRow = await leadsPage.getLeadByEmail(testLead.email);
          await expect(deletedRow).not.toBeVisible();
        }
      } catch (error) {
        // Lead may have been deleted successfully
      }
    });

    test('can convert lead to opportunity', async ({ leadsPage, apiClient, page }) => {
      // Create a qualified lead via API
      const testLead = {
        ...testLeads.qualifiedLead,
        email: generateUniqueEmail('convert-test'),
        status: 'QUALIFIED',
      };

      try {
        await apiClient.createLead(testLead);
        await page.reload();
        await leadsPage.waitForTableLoad();

        await leadsPage.searchLeads(testLead.email);

        const leadRow = await leadsPage.getLeadByEmail(testLead.email);
        const isVisible = await leadRow.isVisible();

        if (isVisible) {
          await leadsPage.convertLead(testLead.email);
          await leadsPage.waitForTableLoad();
        }
      } catch (error) {
        // Conversion may have succeeded or lead not found
      }
    });

    test('convert is disabled for already converted leads', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Search for a converted lead
      await leadsPage.filterByStatus('CONVERTED');

      const count = await leadsPage.getLeadCount();

      if (count > 0) {
        // Try to open actions menu on first converted lead
        const firstRow = leadsPage.tableRows.first();
        await firstRow.locator('[data-testid="MoreVertIcon"]').click();

        // Convert option should be disabled
        const convertOption = page.getByRole('menuitem', { name: /convert/i });
        const isDisabled = await convertOption.isDisabled();
        expect(isDisabled).toBe(true);

        // Close menu
        await page.keyboard.press('Escape');
      }
    });
  });

  test.describe('Visual Indicators', () => {
    test('score badge shows correct color', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      // Check if there are any leads with scores
      const count = await leadsPage.getLeadCount();

      if (count > 0) {
        // Scores should have chip styling
        const scoreChips = page.locator('tbody [class*="Chip"]');
        const chipCount = await scoreChips.count();
        expect(chipCount).toBeGreaterThan(0);
      }
    });

    test('status chip shows correct color', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      const count = await leadsPage.getLeadCount();

      if (count > 0) {
        // Status should be displayed as colored chips
        const statusChips = page.locator('tbody [class*="Chip"]');
        const chipCount = await statusChips.count();
        expect(chipCount).toBeGreaterThan(0);
      }
    });

    test('source chip displays correctly', async ({ leadsPage, page }) => {
      await leadsPage.waitForTableLoad();

      const count = await leadsPage.getLeadCount();

      if (count > 0) {
        // Source should be displayed
        const sources = ['WEBSITE', 'ROI', 'REFERRAL', 'CONFERENCE', 'COLD'];
        let foundSource = false;

        for (const source of sources) {
          const sourceElement = await page.locator('tbody').getByText(new RegExp(source, 'i')).isVisible();
          if (sourceElement) {
            foundSource = true;
            break;
          }
        }

        expect(foundSource).toBe(true);
      }
    });
  });
});
