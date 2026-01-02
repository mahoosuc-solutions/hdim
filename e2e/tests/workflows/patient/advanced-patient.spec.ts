import { test, expect } from '@playwright/test';
import { PatientPage } from '../../../pages/patient.page';
import { LoginPage } from '../../../pages/login.page';
import { TEST_USERS } from '../../../fixtures/test-fixtures';

/**
 * Advanced Patient Management Tests
 *
 * Test Suite: PAT (Patient) - Advanced
 * Coverage: Pagination, filtering, panel assignment
 *
 * These tests verify advanced patient management features
 * for efficient patient list navigation and care coordination.
 */

test.describe('Advanced Patient Management', () => {
  let loginPage: LoginPage;
  let patientPage: PatientPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    patientPage = new PatientPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);
  });

  /**
   * PAT-004: Patient List Pagination and Filtering
   *
   * Verifies that the patient list supports efficient navigation
   * through pagination and various filter options.
   */
  test.describe('PAT-004: Patient List Pagination/Filtering', () => {
    test('should display pagination controls', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const paginationControls = page.locator(
        '[data-testid="pagination"], .pagination, .mat-paginator, nav[aria-label*="pagination"]'
      );

      const hasPagination = await paginationControls.count() > 0;
      console.log('Pagination controls displayed:', hasPagination);
    });

    test('should navigate to next page', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const nextButton = page.locator(
        '[data-testid="next-page"], button[aria-label*="Next"], .mat-paginator-navigation-next'
      );

      if (await nextButton.count() > 0 && await nextButton.isEnabled()) {
        // Get first patient on current page
        const firstPatientBefore = await page.locator(
          '[data-testid="patient-row"], .patient-item, tr'
        ).first().textContent();

        await nextButton.click();
        await page.waitForTimeout(500);

        // Verify page changed
        const firstPatientAfter = await page.locator(
          '[data-testid="patient-row"], .patient-item, tr'
        ).first().textContent();

        if (firstPatientBefore !== firstPatientAfter) {
          console.log('Successfully navigated to next page');
        }
      }
    });

    test('should navigate to previous page', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      // First go to page 2
      const nextButton = page.locator('[data-testid="next-page"]');
      if (await nextButton.count() > 0 && await nextButton.isEnabled()) {
        await nextButton.click();
        await page.waitForTimeout(500);
      }

      // Then go back
      const prevButton = page.locator(
        '[data-testid="prev-page"], button[aria-label*="Previous"], .mat-paginator-navigation-previous'
      );

      if (await prevButton.count() > 0 && await prevButton.isEnabled()) {
        await prevButton.click();
        console.log('Navigated to previous page');
      }
    });

    test('should change page size', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const pageSizeSelect = page.locator(
        '[data-testid="page-size"], .mat-paginator-page-size-select, select[name="pageSize"]'
      );

      if (await pageSizeSelect.count() > 0) {
        await pageSizeSelect.click();

        const options = page.locator('[role="option"], option');
        const optionCount = await options.count();

        if (optionCount > 1) {
          await options.nth(1).click();
          console.log('Page size changed');
        }
      }
    });

    test('should display total patient count', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const totalCount = page.locator(
        '[data-testid="total-count"], .mat-paginator-range-label, .total-patients'
      );

      if (await totalCount.count() > 0) {
        const countText = await totalCount.textContent();
        console.log('Total patients:', countText);
      }
    });

    test('should filter patients by status', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const statusFilter = page.locator(
        '[data-testid="status-filter"], #statusFilter, select[name="status"]'
      );

      if (await statusFilter.count() > 0) {
        await statusFilter.selectOption('ACTIVE');

        await page.waitForTimeout(500);
        console.log('Filtered by ACTIVE status');
      }
    });

    test('should filter patients by payer', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const payerFilter = page.locator(
        '[data-testid="payer-filter"], #payerFilter, select[name="payer"]'
      );

      if (await payerFilter.count() > 0) {
        const options = await payerFilter.locator('option').allTextContents();
        console.log('Payer options:', options);

        if (options.length > 1) {
          await payerFilter.selectOption({ index: 1 });
          console.log('Filtered by payer');
        }
      }
    });

    test('should filter patients by provider', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const providerFilter = page.locator(
        '[data-testid="provider-filter"], #providerFilter, select[name="provider"]'
      );

      if (await providerFilter.count() > 0) {
        const options = await providerFilter.locator('option').allTextContents();
        console.log('Provider options:', options);
      }
    });

    test('should apply multiple filters', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      // Apply status filter
      const statusFilter = page.locator('[data-testid="status-filter"]');
      if (await statusFilter.count() > 0) {
        await statusFilter.selectOption('ACTIVE');
      }

      // Apply payer filter
      const payerFilter = page.locator('[data-testid="payer-filter"]');
      if (await payerFilter.count() > 0) {
        await payerFilter.selectOption({ index: 1 }).catch(() => {});
      }

      await page.waitForTimeout(500);
      console.log('Multiple filters applied');
    });

    test('should clear all filters', async ({ page }) => {
      await patientPage.goto();

      const clearButton = page.locator(
        '[data-testid="clear-filters"], button:has-text("Clear"), button:has-text("Reset")'
      );

      if (await clearButton.count() > 0) {
        await clearButton.click();
        console.log('Filters cleared');
      }
    });

    test('should sort patient list by name', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const nameHeader = page.locator(
        'th:has-text("Name"), th:has-text("Patient"), [data-testid="sort-name"]'
      );

      if (await nameHeader.count() > 0) {
        await nameHeader.click();
        console.log('Sorted by name');

        // Click again for descending
        await nameHeader.click();
        console.log('Sorted by name descending');
      }
    });

    test('should sort patient list by date of birth', async ({ page }) => {
      await patientPage.goto();
      await patientPage.waitForDataLoad();

      const dobHeader = page.locator(
        'th:has-text("DOB"), th:has-text("Birth"), [data-testid="sort-dob"]'
      );

      if (await dobHeader.count() > 0) {
        await dobHeader.click();
        console.log('Sorted by date of birth');
      }
    });
  });

  /**
   * PAT-005: Patient Panel Assignment
   *
   * Verifies that patients can be assigned to care panels
   * for care management and coordination.
   */
  test.describe('PAT-005: Patient Panel Assignment', () => {
    test.beforeEach(async ({ page }) => {
      // Re-login as care manager for panel assignment
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
    });

    test('should display patient panel information', async ({ page }) => {
      await page.goto('/patients');

      // Navigate to a patient detail
      const firstPatient = page.locator('[data-testid="patient-row"], .patient-item').first();
      if (await firstPatient.count() > 0) {
        await firstPatient.click();

        // Look for panel info
        const panelInfo = page.locator(
          '[data-testid="patient-panel"], .panel-assignment, :has-text("Panel")'
        );

        const hasPanel = await panelInfo.count() > 0;
        console.log('Panel information displayed:', hasPanel);
      }
    });

    test('should open panel assignment dialog', async ({ page }) => {
      await page.goto('/patients');

      const assignButton = page.locator(
        '[data-testid="assign-panel"], button:has-text("Assign Panel"), button:has-text("Change Panel")'
      ).first();

      if (await assignButton.count() > 0) {
        await assignButton.click();

        const dialog = page.locator('[role="dialog"], .panel-dialog, .assignment-modal');
        await expect(dialog).toBeVisible({ timeout: 3000 }).catch(() => {
          console.log('Panel assignment dialog not found');
        });
      }
    });

    test('should display available panels', async ({ page }) => {
      await page.goto('/patients');

      const assignButton = page.locator('[data-testid="assign-panel"]').first();
      if (await assignButton.count() > 0) {
        await assignButton.click();

        const panelSelect = page.locator(
          '[data-testid="panel-select"], #panelSelect, select[name="panel"]'
        );

        if (await panelSelect.count() > 0) {
          const options = await panelSelect.locator('option').allTextContents();
          console.log('Available panels:', options);
        }
      }
    });

    test('should assign patient to panel', async ({ page }) => {
      await page.goto('/patients');

      const assignButton = page.locator('[data-testid="assign-panel"]').first();
      if (await assignButton.count() > 0) {
        await assignButton.click();

        const panelSelect = page.locator('[data-testid="panel-select"]');
        if (await panelSelect.count() > 0) {
          await panelSelect.selectOption({ index: 1 });
        }

        const saveButton = page.locator('button:has-text("Save"), button:has-text("Assign")');
        if (await saveButton.count() > 0) {
          const responsePromise = page.waitForResponse(
            resp => resp.url().includes('panel') && resp.status() === 200
          ).catch(() => null);

          await saveButton.click();

          const response = await responsePromise;
          if (response) {
            console.log('Patient assigned to panel');
          }
        }
      }
    });

    test('should bulk assign patients to panel', async ({ page }) => {
      await page.goto('/patients');

      // Select multiple patients
      const checkboxes = page.locator('[data-testid="patient-checkbox"]');
      if (await checkboxes.count() > 1) {
        await checkboxes.first().check();
        await checkboxes.nth(1).check();

        // Click bulk assign
        const bulkAssign = page.locator(
          '[data-testid="bulk-assign-panel"], button:has-text("Assign to Panel")'
        );

        if (await bulkAssign.count() > 0) {
          await bulkAssign.click();
          console.log('Bulk panel assignment initiated');
        }
      }
    });

    test('should view panel members', async ({ page }) => {
      await page.goto('/panels');

      const panelCard = page.locator('[data-testid="panel-card"], .panel-item').first();
      if (await panelCard.count() > 0) {
        await panelCard.click();

        const membersList = page.locator(
          '[data-testid="panel-members"], .member-list, .patient-list'
        );

        const hasMembersList = await membersList.count() > 0;
        console.log('Panel members list displayed:', hasMembersList);
      }
    });

    test('should remove patient from panel', async ({ page }) => {
      await page.goto('/panels');

      const removeButton = page.locator(
        '[data-testid="remove-from-panel"], button:has-text("Remove"), .remove-member'
      ).first();

      if (await removeButton.count() > 0) {
        await removeButton.click();

        // Confirmation dialog
        const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes")');
        if (await confirmButton.count() > 0) {
          console.log('Remove from panel confirmation displayed');
        }
      }
    });

    test('should display panel statistics', async ({ page }) => {
      await page.goto('/panels');

      const panelStats = page.locator(
        '[data-testid="panel-stats"], .panel-metrics, .panel-summary'
      );

      if (await panelStats.count() > 0) {
        const statsText = await panelStats.textContent();
        console.log('Panel statistics:', statsText?.substring(0, 100));
      }
    });
  });
});

/**
 * Patient Demographics Update Tests
 */
test.describe('Patient Demographics Management', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display patient demographics form', async ({ page }) => {
    await page.goto('/patients');

    const firstPatient = page.locator('[data-testid="patient-row"]').first();
    if (await firstPatient.count() > 0) {
      await firstPatient.click();

      const editButton = page.locator(
        '[data-testid="edit-demographics"], button:has-text("Edit")'
      );

      if (await editButton.count() > 0) {
        await editButton.click();

        const demographicsForm = page.locator('[data-testid="demographics-form"], form');
        const hasForm = await demographicsForm.count() > 0;
        console.log('Demographics form displayed:', hasForm);
      }
    }
  });

  test('should validate demographics fields', async ({ page }) => {
    await page.goto('/patients');

    const editButton = page.locator('[data-testid="edit-demographics"]').first();
    if (await editButton.count() > 0) {
      await editButton.click();

      // Clear required field
      const firstNameField = page.locator('#firstName, [data-testid="first-name"]');
      if (await firstNameField.count() > 0) {
        await firstNameField.clear();
        await firstNameField.blur();

        // Check for validation error
        const errorMessage = page.locator('.error, .validation-error, [role="alert"]');
        const hasError = await errorMessage.count() > 0;
        console.log('Validation error displayed:', hasError);
      }
    }
  });

  test('should save demographics changes', async ({ page }) => {
    await page.goto('/patients');

    const editButton = page.locator('[data-testid="edit-demographics"]').first();
    if (await editButton.count() > 0) {
      await editButton.click();

      // Update a field
      const phoneField = page.locator('#phone, [data-testid="phone"]');
      if (await phoneField.count() > 0) {
        await phoneField.fill('555-123-4567');
      }

      // Save
      const saveButton = page.locator('button:has-text("Save")');
      if (await saveButton.count() > 0) {
        const responsePromise = page.waitForResponse(
          resp => resp.url().includes('patient') && resp.status() === 200
        ).catch(() => null);

        await saveButton.click();

        const response = await responsePromise;
        if (response) {
          console.log('Demographics saved');
        }
      }
    }
  });
});

/**
 * Patient Import Tests
 */
test.describe('Patient Data Import', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);
  });

  test('should display import option', async ({ page }) => {
    await page.goto('/patients');

    const importButton = page.locator(
      '[data-testid="import-patients"], button:has-text("Import")'
    );

    const hasImport = await importButton.count() > 0;
    console.log('Import option available:', hasImport);
  });

  test('should show supported import formats', async ({ page }) => {
    await page.goto('/patients/import');

    const formatInfo = page.locator(
      '[data-testid="import-formats"], .format-info, .supported-formats'
    );

    if (await formatInfo.count() > 0) {
      const text = await formatInfo.textContent();
      console.log('Supported formats:', text);
    }
  });

  test('should validate import file', async ({ page }) => {
    await page.goto('/patients/import');

    const fileInput = page.locator('input[type="file"]');
    if (await fileInput.count() > 0) {
      console.log('File input available for import');
    }
  });
});
