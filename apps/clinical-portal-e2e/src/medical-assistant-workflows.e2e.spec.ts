import { test, expect } from '@playwright/test';

/**
 * MEDICAL_ASSISTANT Role Workflow E2E Tests
 *
 * Tests medical assistant-specific workflows:
 * - Assign care gap to patient
 * - Complete patient call workflow
 * - Update patient contact information
 *
 * @tags @e2e @role-medical-assistant @care-coordination
 */

const MEDICAL_ASSISTANT_USER = {
  username: 'test_medical_assistant',
  password: 'password123',
  roles: ['MEDICAL_ASSISTANT'],
  tenantId: 'tenant-a',
};

test.describe('MEDICAL_ASSISTANT Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    // Login as medical assistant
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', MEDICAL_ASSISTANT_USER.username);
    await page.fill('[data-test-id="password"]', MEDICAL_ASSISTANT_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should assign care gap to patient', async ({ page }) => {
    // Navigate to care gaps
    await page.click('[data-test-id="nav-care-gaps"]');
    await page.waitForURL('/care-gaps');

    // Find unassigned care gap
    const careGapRow = page.locator('[data-test-id="care-gap-row"]', {
      hasText: 'Unassigned',
    }).first();

    // Click assign button
    await careGapRow.locator('[data-test-id="assign-care-gap-button"]').click();

    // Search for patient
    await page.fill('[data-test-id="patient-search"]', 'Jane Smith');
    await page.click('[data-test-id="patient-option"]').first();

    // Select priority level
    await page.click('[data-test-id="priority-select"]');
    await page.click('[data-test-id="priority-option-HIGH"]');

    // Add notes
    await page.fill('[data-test-id="assignment-notes"]', 'Patient requires follow-up for diabetes screening');

    // Assign care gap
    await page.click('[data-test-id="confirm-assignment-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Care gap assigned successfully'
    );

    // Verify care gap status changed
    await expect(careGapRow.locator('[data-test-id="care-gap-status"]')).toContainText('Assigned');
  });

  test('should complete patient call workflow', async ({ page }) => {
    // Navigate to patient call queue
    await page.click('[data-test-id="nav-patient-calls"]');
    await page.waitForURL('/patient-calls');

    // Select first patient in queue
    const callRow = page.locator('[data-test-id="patient-call-row"]').first();
    await callRow.click();

    // Verify patient details displayed
    await expect(page.locator('[data-test-id="patient-name"]')).toBeVisible();
    await expect(page.locator('[data-test-id="patient-phone"]')).toBeVisible();

    // View care gaps for call
    await expect(page.locator('[data-test-id="call-care-gaps"]')).toBeVisible();

    // Document call outcome
    await page.click('[data-test-id="call-outcome-select"]');
    await page.click('[data-test-id="outcome-option-CONTACTED"]');

    // Add call notes
    await page.fill('[data-test-id="call-notes"]', 'Patient agreed to schedule appointment for colorectal screening. Will call back to confirm date.');

    // Mark appointment scheduled
    await page.check('[data-test-id="appointment-scheduled-checkbox"]');

    // Submit call documentation
    await page.click('[data-test-id="submit-call-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Call documented successfully'
    );

    // Verify patient moved out of active queue
    await page.goto('/patient-calls');
    await expect(callRow).not.toBeVisible();
  });

  test('should update patient contact information', async ({ page }) => {
    // Navigate to patients
    await page.click('[data-test-id="nav-patients"]');
    await page.waitForURL('/patients');

    // Search for patient
    await page.fill('[data-test-id="patient-search"]', 'John Doe');
    await page.click('[data-test-id="search-button"]');

    // Click on patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to contact information tab
    await page.click('[data-test-id="tab-contact-info"]');

    // Click edit button
    await page.click('[data-test-id="edit-contact-button"]');

    // Update phone number
    await page.fill('[data-test-id="patient-phone"]', '555-123-4567');

    // Update email
    await page.fill('[data-test-id="patient-email"]', 'john.doe.updated@example.com');

    // Update preferred contact method
    await page.click('[data-test-id="preferred-contact-select"]');
    await page.click('[data-test-id="contact-method-PHONE"]');

    // Save changes
    await page.click('[data-test-id="save-contact-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Contact information updated successfully'
    );

    // Verify changes reflected
    await expect(page.locator('[data-test-id="patient-phone"]')).toHaveValue('555-123-4567');
    await expect(page.locator('[data-test-id="patient-email"]')).toHaveValue('john.doe.updated@example.com');
  });
});
