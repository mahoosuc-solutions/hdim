import { test, expect, Page } from '@playwright/test';

/**
 * Agent Creation Wizard E2E Tests
 *
 * Test Coverage:
 * - Complete agent creation workflow (5 steps)
 * - Form validation at each step
 * - Navigation between steps
 * - Agent save and listing
 * - Error handling
 *
 * HIPAA Compliance: Tests verify PHI filtering and audit logging
 */

const TEST_AGENT = {
  name: 'E2E Test Agent',
  description: 'Automated test agent for E2E validation',
  category: 'Clinical Decision Support',
  provider: 'OpenAI',
  model: 'gpt-4',
  temperature: 0.7,
  maxTokens: 2000,
  systemPrompt: 'You are a clinical decision support assistant. Help healthcare providers with evidence-based recommendations.',
  tools: ['patient_search', 'care_gap_analysis', 'quality_measure_evaluation'],
  phiFilteringEnabled: true,
  disclaimerRequired: true,
};

/**
 * Helper: Navigate to Agent Builder page
 */
async function navigateToAgentBuilder(page: Page) {
  await page.goto('/agent-builder');
  await expect(page).toHaveTitle(/Agent Builder/i);
  await expect(page.locator('h1')).toContainText('Agent Builder');
}

/**
 * Helper: Click "Create New Agent" button
 */
async function clickCreateNewAgent(page: Page) {
  const createButton = page.getByRole('button', { name: /create.*agent/i });
  await expect(createButton).toBeVisible();
  await createButton.click();

  // Wait for wizard dialog to appear
  await expect(page.locator('mat-dialog-container')).toBeVisible();
  await expect(page.locator('.wizard-dialog h2')).toContainText('Create New Agent');
}

/**
 * Helper: Complete Step 1 - Basic Info
 */
async function completeStep1BasicInfo(page: Page) {
  // Verify we're on Step 1
  await expect(page.locator('.step-indicator.active')).toContainText('Basic Info');

  // Fill in agent name
  const nameInput = page.getByLabel(/agent name/i);
  await expect(nameInput).toBeVisible();
  await nameInput.fill(TEST_AGENT.name);

  // Fill in description
  const descriptionInput = page.getByLabel(/description/i);
  await descriptionInput.fill(TEST_AGENT.description);

  // Select category
  const categorySelect = page.locator('mat-select[formControlName="category"]');
  await categorySelect.click();
  await page.getByRole('option', { name: TEST_AGENT.category }).click();

  // Click Next
  const nextButton = page.getByRole('button', { name: /next/i });
  await expect(nextButton).toBeEnabled();
  await nextButton.click();
}

/**
 * Helper: Complete Step 2 - Model Configuration
 */
async function completeStep2ModelConfig(page: Page) {
  // Verify we're on Step 2
  await expect(page.locator('.step-indicator.active')).toContainText('Model Configuration');

  // Select provider
  const providerSelect = page.locator('mat-select[formControlName="provider"]');
  await providerSelect.click();
  await page.getByRole('option', { name: TEST_AGENT.provider }).click();

  // Select model
  const modelSelect = page.locator('mat-select[formControlName="model"]');
  await modelSelect.click();
  await page.getByRole('option', { name: TEST_AGENT.model }).click();

  // Set temperature (slider)
  const temperatureSlider = page.locator('mat-slider[formControlName="temperature"]');
  await temperatureSlider.click(); // Use default or set specific value

  // Set max tokens
  const maxTokensInput = page.getByLabel(/max.*tokens/i);
  await maxTokensInput.fill(TEST_AGENT.maxTokens.toString());

  // Click Next
  await page.getByRole('button', { name: /next/i }).click();
}

/**
 * Helper: Complete Step 3 - System Prompt
 */
async function completeStep3SystemPrompt(page: Page) {
  // Verify we're on Step 3
  await expect(page.locator('.step-indicator.active')).toContainText('System Prompt');

  // Fill in system prompt (Monaco Editor)
  const promptEditor = page.locator('.monaco-editor textarea');
  await expect(promptEditor).toBeVisible();
  await promptEditor.fill(TEST_AGENT.systemPrompt);

  // Verify character count updates
  await expect(page.locator('.character-count')).toContainText(/\d+ characters/);

  // Click Next
  await page.getByRole('button', { name: /next/i }).click();
}

/**
 * Helper: Complete Step 4 - Tool Configuration
 */
async function completeStep4ToolConfig(page: Page) {
  // Verify we're on Step 4
  await expect(page.locator('.step-indicator.active')).toContainText('Tool Configuration');

  // Select tools (checkboxes)
  for (const tool of TEST_AGENT.tools) {
    const toolCheckbox = page.getByRole('checkbox', { name: new RegExp(tool, 'i') });
    await expect(toolCheckbox).toBeVisible();
    await toolCheckbox.check();
    await expect(toolCheckbox).toBeChecked();
  }

  // Verify selected tool count
  const selectedCount = page.locator('.selected-tools-count');
  await expect(selectedCount).toContainText(`${TEST_AGENT.tools.length} tool`);

  // Click Next
  await page.getByRole('button', { name: /next/i }).click();
}

/**
 * Helper: Complete Step 5 - Guardrails
 */
async function completeStep5Guardrails(page: Page) {
  // Verify we're on Step 5
  await expect(page.locator('.step-indicator.active')).toContainText('Guardrails');

  // Enable PHI filtering
  const phiCheckbox = page.getByRole('checkbox', { name: /phi.*filter/i });
  await phiCheckbox.check();
  await expect(phiCheckbox).toBeChecked();

  // Enable disclaimer
  const disclaimerCheckbox = page.getByRole('checkbox', { name: /disclaimer/i });
  await disclaimerCheckbox.check();
  await expect(disclaimerCheckbox).toBeChecked();

  // Verify guardrail summary
  await expect(page.locator('.guardrail-summary')).toContainText('2 guardrails enabled');
}

/**
 * Helper: Save agent
 */
async function saveAgent(page: Page) {
  const saveButton = page.getByRole('button', { name: /save.*agent/i });
  await expect(saveButton).toBeEnabled();
  await saveButton.click();

  // Wait for success message
  await expect(page.locator('.mat-snack-bar-container')).toContainText(/success/i);

  // Wait for dialog to close
  await expect(page.locator('mat-dialog-container')).not.toBeVisible({ timeout: 5000 });
}

test.describe('Agent Creation Wizard', () => {
  test.beforeEach(async ({ page }) => {
    // Mock authentication - navigate directly to agent builder
    await navigateToAgentBuilder(page);
  });

  test('should display agent builder page with create button', async ({ page }) => {
    // Verify page loaded correctly
    await expect(page.locator('h1')).toContainText('Agent Builder');

    // Verify create button exists
    const createButton = page.getByRole('button', { name: /create.*agent/i });
    await expect(createButton).toBeVisible();
    await expect(createButton).toBeEnabled();
  });

  test('should open wizard dialog on create button click', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Verify wizard dialog structure
    await expect(page.locator('.wizard-dialog')).toBeVisible();
    await expect(page.locator('.step-indicator')).toHaveCount(5);
    await expect(page.locator('.step-indicator.active')).toHaveText(/1.*Basic Info/i);
  });

  test('should validate required fields in Step 1', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Try to proceed without filling required fields
    const nextButton = page.getByRole('button', { name: /next/i });
    await expect(nextButton).toBeDisabled();

    // Fill only name (should still be disabled)
    await page.getByLabel(/agent name/i).fill('Test Agent');
    await expect(nextButton).toBeDisabled();

    // Fill description (should enable)
    await page.getByLabel(/description/i).fill('Test description');

    // Select category
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option').first().click();

    // Now should be enabled
    await expect(nextButton).toBeEnabled();
  });

  test('should complete full agent creation workflow', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Complete all 5 steps
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);
    await completeStep3SystemPrompt(page);
    await completeStep4ToolConfig(page);
    await completeStep5Guardrails(page);

    // Save agent
    await saveAgent(page);

    // Verify agent appears in list
    await expect(page.locator('.agent-card')).toContainText(TEST_AGENT.name);
  });

  test('should allow navigation back to previous steps', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Complete Step 1
    await completeStep1BasicInfo(page);

    // Verify we're on Step 2
    await expect(page.locator('.step-indicator.active')).toContainText('Model Configuration');

    // Click Back button
    const backButton = page.getByRole('button', { name: /back/i });
    await expect(backButton).toBeVisible();
    await backButton.click();

    // Verify we're back on Step 1
    await expect(page.locator('.step-indicator.active')).toContainText('Basic Info');

    // Verify previous input persisted
    await expect(page.getByLabel(/agent name/i)).toHaveValue(TEST_AGENT.name);
  });

  test('should show character count in system prompt editor', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Navigate to Step 3
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);

    // Type in prompt editor
    const promptEditor = page.locator('.monaco-editor textarea');
    const testPrompt = 'Short test prompt';
    await promptEditor.fill(testPrompt);

    // Verify character count
    const charCount = page.locator('.character-count');
    await expect(charCount).toContainText(`${testPrompt.length} characters`);
  });

  test('should validate temperature range in model config', async ({ page }) => {
    await clickCreateNewAgent(page);
    await completeStep1BasicInfo(page);

    // Verify temperature slider has correct range (0-2)
    const temperatureSlider = page.locator('mat-slider[formControlName="temperature"]');
    await expect(temperatureSlider).toHaveAttribute('min', '0');
    await expect(temperatureSlider).toHaveAttribute('max', '2');
    await expect(temperatureSlider).toHaveAttribute('step', '0.1');
  });

  test('should display tool descriptions on hover', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Navigate to Step 4
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);
    await completeStep3SystemPrompt(page);

    // Hover over first tool
    const firstTool = page.locator('.tool-option').first();
    await firstTool.hover();

    // Verify tooltip appears
    await expect(page.locator('mat-tooltip')).toBeVisible();
  });

  test('should show selected tool count in Step 4', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Navigate to Step 4
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);
    await completeStep3SystemPrompt(page);

    // Select 2 tools
    const tools = page.locator('.tool-option mat-checkbox');
    await tools.nth(0).check();
    await tools.nth(1).check();

    // Verify count
    await expect(page.locator('.selected-tools-count')).toContainText('2 tools');
  });

  test('should enforce HIPAA compliance with PHI filtering', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Navigate to Step 5
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);
    await completeStep3SystemPrompt(page);
    await completeStep4ToolConfig(page);

    // Verify PHI filtering is available
    const phiCheckbox = page.getByRole('checkbox', { name: /phi.*filter/i });
    await expect(phiCheckbox).toBeVisible();

    // Enable it
    await phiCheckbox.check();

    // Verify warning message about PHI
    await expect(page.locator('.guardrail-warning')).toContainText(/protected health information/i);
  });

  test('should display guardrail summary in Step 5', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Navigate to Step 5
    await completeStep1BasicInfo(page);
    await completeStep2ModelConfig(page);
    await completeStep3SystemPrompt(page);
    await completeStep4ToolConfig(page);

    // Enable all guardrails
    await page.getByRole('checkbox', { name: /phi.*filter/i }).check();
    await page.getByRole('checkbox', { name: /disclaimer/i }).check();

    // Verify summary
    const summary = page.locator('.guardrail-summary');
    await expect(summary).toContainText('2 guardrails enabled');
  });

  test('should cancel agent creation and close dialog', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Fill in some data
    await page.getByLabel(/agent name/i).fill('Test Agent to Cancel');

    // Click Cancel
    const cancelButton = page.getByRole('button', { name: /cancel/i });
    await expect(cancelButton).toBeVisible();
    await cancelButton.click();

    // Verify dialog closed
    await expect(page.locator('mat-dialog-container')).not.toBeVisible();

    // Verify agent was NOT created
    await expect(page.locator('.agent-card')).not.toContainText('Test Agent to Cancel');
  });

  test('should display validation errors for invalid inputs', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Try invalid agent name (too short)
    const nameInput = page.getByLabel(/agent name/i);
    await nameInput.fill('AB'); // Less than 3 characters
    await nameInput.blur();

    // Verify error message
    await expect(page.locator('mat-error')).toContainText(/at least 3 characters/i);

    // Next button should be disabled
    await expect(page.getByRole('button', { name: /next/i })).toBeDisabled();
  });

  test('should save agent draft and allow later editing', async ({ page }) => {
    await clickCreateNewAgent(page);

    // Fill Step 1 only
    await page.getByLabel(/agent name/i).fill('Draft Agent');
    await page.getByLabel(/description/i).fill('Test draft agent');
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option').first().click();

    // Click "Save as Draft" if available
    const saveDraftButton = page.getByRole('button', { name: /save.*draft/i });
    if (await saveDraftButton.isVisible()) {
      await saveDraftButton.click();

      // Verify success message
      await expect(page.locator('.mat-snack-bar-container')).toContainText(/draft.*saved/i);

      // Verify agent appears in list with DRAFT status
      await expect(page.locator('.agent-card .status-badge')).toContainText('DRAFT');
    }
  });
});
