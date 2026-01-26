import { test, expect } from '@playwright/test';

/**
 * PROVIDER Role Workflow E2E Tests
 *
 * Tests provider-specific workflows:
 * - Review lab results
 * - Prescribe medication
 * - Order diagnostic tests
 * - Review care recommendations
 *
 * @tags @e2e @role-provider @clinical-decision-support
 */

const PROVIDER_USER = {
  username: 'test_provider',
  password: 'password123',
  roles: ['PROVIDER'],
  tenantId: 'tenant-a',
};

test.describe('PROVIDER Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    // Login as provider
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', PROVIDER_USER.username);
    await page.fill('[data-test-id="password"]', PROVIDER_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should review lab results and document findings', async ({ page }) => {
    // Navigate to patients
    await page.click('[data-test-id="nav-patients"]');
    await page.waitForURL('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to lab results tab
    await page.click('[data-test-id="tab-lab-results"]');

    // Filter to show new/unreviewed results
    await page.click('[data-test-id="result-status-filter"]');
    await page.click('[data-test-id="status-option-NEW"]');

    // Select first lab result
    const labRow = page.locator('[data-test-id="lab-result-row"]').first();
    await labRow.click();

    // Verify lab details displayed
    await expect(page.locator('[data-test-id="lab-test-name"]')).toBeVisible();
    await expect(page.locator('[data-test-id="lab-result-value"]')).toBeVisible();
    await expect(page.locator('[data-test-id="lab-reference-range"]')).toBeVisible();

    // Check for critical values
    const criticalFlag = page.locator('[data-test-id="critical-value-flag"]');
    if (await criticalFlag.isVisible()) {
      await expect(criticalFlag).toBeVisible();
    }

    // Add provider interpretation
    await page.fill('[data-test-id="provider-interpretation"]', 'HbA1c elevated at 8.2%. Will adjust diabetes medication regimen. Schedule follow-up in 3 months.');

    // Mark as reviewed
    await page.click('[data-test-id="mark-reviewed-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Lab result reviewed successfully'
    );

    // Verify result status changed
    await expect(labRow.locator('[data-test-id="lab-status"]')).toContainText('Reviewed');
  });

  test('should prescribe medication with e-prescribe workflow', async ({ page }) => {
    // Navigate to patients
    await page.goto('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to medications tab
    await page.click('[data-test-id="tab-medications"]');

    // Click prescribe medication button
    await page.click('[data-test-id="prescribe-medication-button"]');

    // Search for medication
    await page.fill('[data-test-id="medication-search"]', 'Metformin');
    await page.click('[data-test-id="medication-option"]', { hasText: 'Metformin 500mg tablet' });

    // Select dosage
    await page.fill('[data-test-id="medication-dose"]', '500');
    await page.click('[data-test-id="dose-unit-select"]');
    await page.click('[data-test-id="unit-option-MG"]');

    // Select frequency
    await page.click('[data-test-id="medication-frequency-select"]');
    await page.click('[data-test-id="frequency-option-BID"]'); // Twice daily

    // Set duration
    await page.fill('[data-test-id="medication-duration"]', '90');
    await page.click('[data-test-id="duration-unit-select"]');
    await page.click('[data-test-id="duration-unit-DAYS"]');

    // Add instructions
    await page.fill('[data-test-id="medication-instructions"]', 'Take with food. May cause GI upset initially.');

    // Check for drug interactions
    await page.click('[data-test-id="check-interactions-button"]');
    await expect(page.locator('[data-test-id="interaction-check-result"]')).toContainText('No significant interactions found');

    // Set refills
    await page.fill('[data-test-id="medication-refills"]', '3');

    // Send prescription
    await page.click('[data-test-id="send-prescription-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Prescription sent successfully'
    );

    // Verify medication appears in active medications list
    await expect(page.locator('[data-test-id="medication-row"]', {
      hasText: 'Metformin 500mg',
    })).toBeVisible();
  });

  test('should order diagnostic tests with clinical indication', async ({ page }) => {
    // Navigate to patients
    await page.goto('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to orders tab
    await page.click('[data-test-id="tab-orders"]');

    // Click order diagnostic test button
    await page.click('[data-test-id="order-test-button"]');

    // Select test category
    await page.click('[data-test-id="test-category-select"]');
    await page.click('[data-test-id="category-option-LABORATORY"]');

    // Search for specific test
    await page.fill('[data-test-id="test-search"]', 'Hemoglobin A1c');
    await page.click('[data-test-id="test-option"]', { hasText: 'Hemoglobin A1c' });

    // Add clinical indication
    await page.fill('[data-test-id="clinical-indication"]', 'Diabetes monitoring - baseline elevated at 8.2%, adjusting treatment plan');

    // Select priority
    await page.click('[data-test-id="order-priority-select"]');
    await page.click('[data-test-id="priority-option-ROUTINE"]');

    // Set collection date
    await page.fill('[data-test-id="collection-date"]', '2024-01-20');

    // Add special instructions
    await page.fill('[data-test-id="special-instructions"]', 'Fasting required - schedule morning appointment');

    // Submit order
    await page.click('[data-test-id="submit-order-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Diagnostic order placed successfully'
    );

    // Verify order appears in orders list
    await expect(page.locator('[data-test-id="order-row"]', {
      hasText: 'Hemoglobin A1c',
    })).toBeVisible();
  });

  test('should review and act on AI care recommendations', async ({ page }) => {
    // Navigate to patients
    await page.goto('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to care recommendations tab
    await page.click('[data-test-id="tab-care-recommendations"]');

    // Verify care recommendations displayed
    await expect(page.locator('[data-test-id="care-recommendation-list"]')).toBeVisible();

    // Select first high-priority recommendation
    const recommendationRow = page.locator('[data-test-id="recommendation-row"]', {
      hasText: 'High Priority',
    }).first();
    await recommendationRow.click();

    // Verify recommendation details
    await expect(page.locator('[data-test-id="recommendation-title"]')).toBeVisible();
    await expect(page.locator('[data-test-id="recommendation-rationale"]')).toBeVisible();
    await expect(page.locator('[data-test-id="recommendation-evidence"]')).toBeVisible();

    // Review evidence-based guidelines
    await page.click('[data-test-id="view-guidelines-button"]');
    await expect(page.locator('[data-test-id="clinical-guidelines"]')).toBeVisible();

    // Accept recommendation and create order
    await page.click('[data-test-id="accept-recommendation-button"]');

    // Confirm order creation
    await page.click('[data-test-id="confirm-order-button"]');

    // Add provider notes
    await page.fill('[data-test-id="provider-notes"]', 'Recommendation accepted. Colorectal screening overdue. Order placed for colonoscopy.');

    // Save action
    await page.click('[data-test-id="save-action-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Care recommendation processed successfully'
    );

    // Verify recommendation status updated
    await expect(recommendationRow.locator('[data-test-id="recommendation-status"]')).toContainText('Accepted');
  });
});
