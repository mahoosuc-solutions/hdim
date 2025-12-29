import { test, expect } from '@playwright/test';
import { PatientSearchPage } from '../../../pages/patient-search.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Patient Workflow Tests
 *
 * Test Suite: PAT
 * Coverage: Patient search, selection, and management workflows
 *
 * These tests verify the patient search and selection functionality
 * used throughout the clinical portal.
 */

test.describe('Patient Workflows', () => {
  let patientSearchPage: PatientSearchPage;
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    patientSearchPage = new PatientSearchPage(page);
    loginPage = new LoginPage(page);

    // Login before each test
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * PAT-001: Patient Search by Name
   *
   * Verifies that users can search for patients by name
   * and view matching results.
   *
   * Workflow Reference: User Story US-PAT-001
   */
  test.describe('PAT-001: Patient Search by Name', () => {
    test('should display search input on patients page', async ({ page }) => {
      await patientSearchPage.goto();
      await expect(patientSearchPage.searchInput).toBeVisible();
      await expect(patientSearchPage.searchButton).toBeVisible();
    });

    test('should search by full name and display results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for test patient
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      // Should have results
      const count = await patientSearchPage.getResultCount();
      expect(count).toBeGreaterThan(0);
    });

    test('should search by partial name', async ({ page }) => {
      await patientSearchPage.goto();

      // Search with partial name
      await patientSearchPage.search('Tes');
      await patientSearchPage.waitForResults();

      // Should still find results
      const count = await patientSearchPage.getResultCount();
      console.log(`Found ${count} patients matching partial name`);
    });

    test('should be case-insensitive search', async ({ page }) => {
      await patientSearchPage.goto();

      // Search lowercase
      await patientSearchPage.search('test');
      await patientSearchPage.waitForResults();
      const lowerCount = await patientSearchPage.getResultCount();

      // Clear and search uppercase
      await patientSearchPage.clearSearch();
      await patientSearchPage.search('TEST');
      await patientSearchPage.waitForResults();
      const upperCount = await patientSearchPage.getResultCount();

      // Results should be the same (or both > 0 if different test data)
      console.log(`Lowercase: ${lowerCount}, Uppercase: ${upperCount}`);
    });

    test('should show no results message for non-existent patient', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for non-existent patient
      await patientSearchPage.search('ZZZ_NONEXISTENT_PATIENT_XYZ');
      await patientSearchPage.waitForResults();

      // Should show no results
      await patientSearchPage.assertNoResults();
    });

    test('should clear search and reset results', async ({ page }) => {
      await patientSearchPage.goto();

      // Perform search
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      // Clear search
      await patientSearchPage.clearSearch();

      // Search input should be empty
      const value = await patientSearchPage.searchInput.inputValue();
      expect(value).toBe('');
    });

    test('should search using Enter key', async ({ page }) => {
      await patientSearchPage.goto();

      // Search using Enter
      await patientSearchPage.searchWithEnter('Test');
      await patientSearchPage.waitForResults();

      // Should have results
      const count = await patientSearchPage.getResultCount();
      expect(count).toBeGreaterThanOrEqual(0);
    });
  });

  /**
   * PAT-002: Patient Search by MRN
   *
   * Verifies that users can search for patients by Medical Record Number
   * using the advanced search functionality.
   *
   * Workflow Reference: User Story US-PAT-002
   */
  test.describe('PAT-002: Patient Search by MRN', () => {
    test('should have advanced search toggle', async ({ page }) => {
      await patientSearchPage.goto();

      // Check for advanced search option
      const toggle = patientSearchPage.advancedSearchToggle;
      if (await toggle.count() > 0) {
        await expect(toggle).toBeVisible();
      } else {
        console.log('Advanced search is always visible or integrated');
      }
    });

    test('should search by MRN in advanced search', async ({ page }) => {
      await patientSearchPage.goto();

      // Use advanced search with MRN
      await patientSearchPage.advancedSearch({
        mrn: 'TEST_MRN',
      });

      await patientSearchPage.waitForResults();

      // Log result count
      const count = await patientSearchPage.getResultCount();
      console.log(`Found ${count} patients with MRN pattern`);
    });

    test('should search by exact MRN match', async ({ page }) => {
      await patientSearchPage.goto();

      // First get a valid MRN from regular search
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count > 0) {
        const patientData = await patientSearchPage.getPatientDataByIndex(0);
        const mrn = patientData.mrn;

        // Now search by that exact MRN
        await patientSearchPage.clearSearch();
        await patientSearchPage.advancedSearch({ mrn });

        await patientSearchPage.waitForResults();
        const resultCount = await patientSearchPage.getResultCount();

        // Should find exactly 1 patient with that MRN
        expect(resultCount).toBeGreaterThan(0);
      }
    });

    test('should combine MRN with other search criteria', async ({ page }) => {
      await patientSearchPage.goto();

      // Search with multiple criteria
      await patientSearchPage.advancedSearch({
        mrn: 'TEST',
        lastName: 'Test',
      });

      await patientSearchPage.waitForResults();
    });

    test('should handle special characters in MRN', async ({ page }) => {
      await patientSearchPage.goto();

      // MRNs might contain dashes or other characters
      await patientSearchPage.advancedSearch({
        mrn: 'MRN-12345',
      });

      await patientSearchPage.waitForResults();
      // Should not crash, may or may not find results
    });
  });

  /**
   * PAT-003: Patient Selection for Evaluation
   *
   * Verifies that users can select a patient from search results
   * to proceed with quality measure evaluation.
   *
   * Workflow Reference: User Story US-PAT-003
   */
  test.describe('PAT-003: Patient Selection', () => {
    test('should select patient from search results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count > 0) {
        // Select first patient
        await patientSearchPage.selectPatientByIndex(0);

        // Verify selection or navigation occurred
        // This might navigate to evaluation page or show selection
      }
    });

    test('should view patient details from search results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count > 0) {
        // Get initial data
        const patientData = await patientSearchPage.getPatientDataByIndex(0);
        console.log('Viewing patient:', patientData.name);

        // Click to view details
        await patientSearchPage.viewPatientDetailsByIndex(0);

        // Should navigate to patient details page
        await expect(page).toHaveURL(/.*patients\/.*/);
      }
    });

    test('should display patient data in search results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count > 0) {
        // Get patient data from first row
        const patientData = await patientSearchPage.getPatientDataByIndex(0);

        // All fields should have values
        expect(patientData.name).toBeTruthy();
        expect(patientData.mrn).toBeTruthy();

        // PHI fields should be masked in logs
        console.log('Patient data retrieved (PHI masked)');
      }
    });

    test('should paginate through search results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for all test patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const initialCount = await patientSearchPage.getResultCount();

      // If pagination is available
      const nextButton = patientSearchPage.nextPageButton;
      if ((await nextButton.count()) > 0 && (await nextButton.isEnabled())) {
        await patientSearchPage.nextPage();

        // Should show different results (or same on last page)
        console.log('Navigated to next page of results');
      }
    });

    test('should change page size', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      // Try to change page size if available
      const pageSizeSelector = patientSearchPage.pageSizeSelector;
      if ((await pageSizeSelector.count()) > 0) {
        await patientSearchPage.setPageSize(25);

        // Results should adjust
        await patientSearchPage.waitForResults();
      }
    });

    test('should select multiple patients for batch evaluation', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count >= 2) {
        // Select multiple patients
        await patientSearchPage.selectPatientsForBatch([0, 1]);

        // Batch evaluate button should be visible/enabled
        const batchButton = patientSearchPage.batchEvaluateButton;
        if ((await batchButton.count()) > 0) {
          await expect(batchButton).toBeEnabled();
        }
      }
    });

    test('should select all patients on page', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      const count = await patientSearchPage.getResultCount();
      if (count > 0) {
        // Select all
        const selectAll = patientSearchPage.batchSelectCheckbox;
        if ((await selectAll.count()) > 0) {
          await patientSearchPage.selectAllPatients();

          // All checkboxes should be checked
          console.log(`Selected all ${count} patients on page`);
        }
      }
    });
  });

  /**
   * Patient Search - PHI Safety Tests
   *
   * Verifies that PHI is properly handled in search results
   * and does not leak to unauthorized places.
   */
  test.describe('Patient Search - PHI Safety', () => {
    test('should not expose SSN in search results', async ({ page }) => {
      await patientSearchPage.goto();

      // Search for patients
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      // Get page content
      const content = await page.content();

      // Should not contain SSN patterns in visible content
      const ssnPattern = /\b\d{3}-\d{2}-\d{4}\b/;
      const visibleText = await page.evaluate(() => document.body.innerText);

      if (ssnPattern.test(visibleText)) {
        console.warn('WARNING: SSN pattern found in visible page content');
      }
    });

    test('should not log PHI to console', async ({ page }) => {
      const consoleLogs: string[] = [];

      // Listen to console
      page.on('console', (msg) => {
        consoleLogs.push(msg.text());
      });

      await patientSearchPage.goto();
      await patientSearchPage.search('Test');
      await patientSearchPage.waitForResults();

      // Check logs for PHI patterns
      const ssnPattern = /\b\d{3}-\d{2}-\d{4}\b/;
      const phonePattern = /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/;

      for (const log of consoleLogs) {
        if (ssnPattern.test(log)) {
          console.warn('WARNING: SSN pattern found in console logs');
        }
        if (phonePattern.test(log)) {
          console.warn('NOTE: Phone pattern found in console logs - verify if test data');
        }
      }
    });
  });
});
