import { test, expect } from '@playwright/test';

/**
 * REGISTERED_NURSE Role Workflow E2E Tests
 *
 * Tests registered nurse-specific workflows:
 * - Create care plan
 * - Document patient education
 * - Review provider orders
 *
 * @tags @e2e @role-registered-nurse @care-planning
 */

const REGISTERED_NURSE_USER = {
  username: 'test_registered_nurse',
  password: 'password123',
  roles: ['REGISTERED_NURSE'],
  tenantId: 'tenant-a',
};

test.describe('REGISTERED_NURSE Role Workflows', () => {
  test.beforeEach(async ({ page }) => {
    // Login as registered nurse
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', REGISTERED_NURSE_USER.username);
    await page.fill('[data-test-id="password"]', REGISTERED_NURSE_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should create care plan for patient', async ({ page }) => {
    // Navigate to patients
    await page.click('[data-test-id="nav-patients"]');
    await page.waitForURL('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to care plans tab
    await page.click('[data-test-id="tab-care-plans"]');

    // Click create care plan button
    await page.click('[data-test-id="create-care-plan-button"]');

    // Fill care plan details
    await page.fill('[data-test-id="care-plan-title"]', 'Diabetes Management Care Plan');

    // Select care plan category
    await page.click('[data-test-id="care-plan-category-select"]');
    await page.click('[data-test-id="category-option-CHRONIC-DISEASE"]');

    // Add goals
    await page.click('[data-test-id="add-goal-button"]');
    await page.fill('[data-test-id="goal-description-0"]', 'Achieve HbA1c below 7.0%');
    await page.fill('[data-test-id="goal-target-date-0"]', '2024-06-30');

    // Add interventions
    await page.click('[data-test-id="add-intervention-button"]');
    await page.fill('[data-test-id="intervention-description-0"]', 'Monthly blood glucose monitoring and medication adherence counseling');

    // Assign care team members
    await page.click('[data-test-id="assign-care-team-button"]');
    await page.check('[data-test-id="team-member-dietitian"]');
    await page.check('[data-test-id="team-member-diabetes-educator"]');

    // Save care plan
    await page.click('[data-test-id="save-care-plan-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Care plan created successfully'
    );

    // Verify care plan appears in list
    await expect(page.locator('[data-test-id="care-plan-row"]', {
      hasText: 'Diabetes Management Care Plan',
    })).toBeVisible();
  });

  test('should document patient education session', async ({ page }) => {
    // Navigate to patients
    await page.goto('/patients');

    // Select patient
    const patientRow = page.locator('[data-test-id="patient-row"]').first();
    await patientRow.click();

    // Navigate to education tab
    await page.click('[data-test-id="tab-patient-education"]');

    // Click document education button
    await page.click('[data-test-id="document-education-button"]');

    // Select education topic
    await page.click('[data-test-id="education-topic-select"]');
    await page.click('[data-test-id="topic-option-MEDICATION-MANAGEMENT"]');

    // Select education method
    await page.click('[data-test-id="education-method-select"]');
    await page.click('[data-test-id="method-option-ONE-ON-ONE"]');

    // Document session duration
    await page.fill('[data-test-id="session-duration-minutes"]', '30');

    // Document education content
    await page.fill('[data-test-id="education-content"]', 'Reviewed proper insulin administration technique, storage requirements, and importance of consistent timing. Patient demonstrated proper injection technique. Provided written materials.');

    // Document patient understanding
    await page.click('[data-test-id="patient-understanding-select"]');
    await page.click('[data-test-id="understanding-option-GOOD"]');

    // Document follow-up needs
    await page.check('[data-test-id="followup-needed-checkbox"]');
    await page.fill('[data-test-id="followup-notes"]', 'Schedule follow-up in 2 weeks to reassess technique and address any questions');

    // Save education documentation
    await page.click('[data-test-id="save-education-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Patient education documented successfully'
    );

    // Verify education appears in timeline
    await expect(page.locator('[data-test-id="education-entry"]', {
      hasText: 'Medication Management',
    })).toBeVisible();
  });

  test('should review and acknowledge provider orders', async ({ page }) => {
    // Navigate to orders review queue
    await page.click('[data-test-id="nav-orders"]');
    await page.waitForURL('/orders');

    // Filter to show pending RN review orders
    await page.click('[data-test-id="order-status-filter"]');
    await page.click('[data-test-id="status-option-PENDING-RN-REVIEW"]');

    // Select first order
    const orderRow = page.locator('[data-test-id="order-row"]').first();
    await orderRow.click();

    // Verify order details displayed
    await expect(page.locator('[data-test-id="order-type"]')).toBeVisible();
    await expect(page.locator('[data-test-id="ordering-provider"]')).toBeVisible();
    await expect(page.locator('[data-test-id="order-details"]')).toBeVisible();

    // Review medication order details
    await expect(page.locator('[data-test-id="medication-name"]')).toBeVisible();
    await expect(page.locator('[data-test-id="medication-dose"]')).toBeVisible();
    await expect(page.locator('[data-test-id="medication-frequency"]')).toBeVisible();

    // Check for allergies or contraindications
    await page.click('[data-test-id="check-allergies-button"]');
    await expect(page.locator('[data-test-id="allergy-check-result"]')).toContainText('No contraindications found');

    // Add nursing notes
    await page.fill('[data-test-id="nursing-notes"]', 'Order reviewed. No allergies or contraindications identified. Patient education on medication provided.');

    // Acknowledge order
    await page.click('[data-test-id="acknowledge-order-button"]');

    // Verify success message
    await expect(page.locator('[data-test-id="success-message"]')).toContainText(
      'Order acknowledged successfully'
    );

    // Verify order status changed
    await page.goto('/orders');
    await expect(orderRow.locator('[data-test-id="order-status"]')).toContainText('Acknowledged');
  });
});
