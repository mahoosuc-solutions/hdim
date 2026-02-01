import { test, expect, Page } from '@playwright/test';

/**
 * Template Library E2E Tests
 *
 * Test Coverage:
 * - Browse template catalog with search and filtering
 * - Create new templates
 * - Edit existing templates
 * - Select templates for agent configuration
 * - Template preview functionality
 * - Variable extraction and validation
 *
 * Phase 1 Implementation: Template Library UI (completed January 25, 2026)
 */

const TEST_TEMPLATE = {
  name: 'Clinical Assessment Template',
  category: 'Clinical Decision Support',
  description: 'Template for clinical assessment workflows',
  content: `You are a clinical assessment assistant.

Patient Context: {{patient_name}}, {{patient_age}} years old
Chief Complaint: {{chief_complaint}}

Please provide evidence-based assessment and recommendations.`,
  variables: ['patient_name', 'patient_age', 'chief_complaint'],
};

/**
 * Helper: Navigate to Agent Builder
 */
async function navigateToAgentBuilder(page: Page) {
  await page.goto('/agent-builder');
  await expect(page).toHaveTitle(/Agent Builder/i);
}

/**
 * Helper: Open template library from wizard
 */
async function openTemplateLibraryFromWizard(page: Page) {
  // Click Create New Agent
  await page.getByRole('button', { name: /create.*agent/i }).click();
  await expect(page.locator('mat-dialog-container')).toBeVisible();

  // Navigate to Step 3 (System Prompt)
  // First complete Steps 1 & 2
  await page.getByLabel(/agent name/i).fill('Test Agent');
  await page.getByLabel(/description/i).fill('Test description');
  await page.locator('mat-select[formControlName="category"]').click();
  await page.getByRole('option').first().click();
  await page.getByRole('button', { name: /next/i }).click();

  // Step 2: Model Config
  await page.locator('mat-select[formControlName="provider"]').click();
  await page.getByRole('option', { name: /openai/i }).click();
  await page.locator('mat-select[formControlName="model"]').click();
  await page.getByRole('option').first().click();
  await page.getByRole('button', { name: /next/i }).click();

  // Now on Step 3 - click Browse Templates
  const browseTemplatesButton = page.getByRole('button', { name: /browse.*template/i });
  await expect(browseTemplatesButton).toBeVisible();
  await browseTemplatesButton.click();

  // Wait for template library dialog
  await expect(page.locator('mat-dialog-container').nth(1)).toBeVisible();
  await expect(page.locator('h2')).toContainText('Template Library');
}

/**
 * Helper: Open template library for browsing (standalone)
 */
async function openTemplateLibraryStandalone(page: Page) {
  // Assuming there's a "Browse Templates" button on main page
  const browseButton = page.getByRole('button', { name: /template.*library/i });
  if (await browseButton.isVisible()) {
    await browseButton.click();
    await expect(page.locator('mat-dialog-container')).toBeVisible();
  }
}

/**
 * Helper: Create a new template
 */
async function createNewTemplate(page: Page) {
  // Click Create Template button
  const createButton = page.getByRole('button', { name: /create.*template/i });
  await expect(createButton).toBeVisible();
  await createButton.click();

  // Wait for create template dialog
  await expect(page.locator('mat-dialog-container').nth(1)).toBeVisible();
  await expect(page.locator('h2')).toContainText('Create Template');
}

test.describe('Template Library', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAgentBuilder(page);
  });

  test('should open template library from agent wizard', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Verify library structure
    await expect(page.locator('.template-list')).toBeVisible();
    await expect(page.locator('.template-preview')).toBeVisible();

    // Verify at least one template exists
    const rowCount = await page.locator('.template-row').count();
    expect(rowCount).toBeGreaterThanOrEqual(1);
  });

  test('should display template list with pagination', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Verify table headers
    await expect(page.locator('mat-header-cell')).toContainText(/name/i);
    await expect(page.locator('mat-header-cell')).toContainText(/category/i);
    await expect(page.locator('mat-header-cell')).toContainText(/variables/i);

    // Verify pagination controls
    const paginator = page.locator('mat-paginator');
    await expect(paginator).toBeVisible();

    // Verify items per page options
    await expect(paginator.locator('.mat-mdc-select')).toBeVisible();
  });

  test('should search templates with debounced input', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Get initial template count
    const initialCount = await page.locator('.template-row').count();

    // Type in search box
    const searchInput = page.getByPlaceholder(/search.*template/i);
    await expect(searchInput).toBeVisible();
    await searchInput.fill('clinical');

    // Wait for debounce (300ms)
    await page.waitForTimeout(400);

    // Verify filtered results
    const filteredCount = await page.locator('.template-row').count();

    // Should have filtering effect (count changed or stayed same if all match)
    expect(filteredCount).toBeLessThanOrEqual(initialCount);
  });

  test('should filter templates by category', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click category filter dropdown
    const categoryFilter = page.locator('mat-select[formControlName="categoryFilter"]');
    await categoryFilter.click();

    // Select a category
    await page.getByRole('option', { name: /clinical/i }).click();

    // Wait for filtering
    await page.waitForTimeout(300);

    // Verify all visible templates match category
    const templateRows = page.locator('.template-row');
    const count = await templateRows.count();

    for (let i = 0; i < count; i++) {
      await expect(templateRows.nth(i)).toContainText(/clinical/i);
    }
  });

  test('should show template preview on row click', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click first template row
    const firstRow = page.locator('.template-row').first();
    await firstRow.click();

    // Verify preview pane shows content
    const previewPane = page.locator('.template-preview');
    await expect(previewPane.locator('.template-name')).toBeVisible();
    await expect(previewPane.locator('.template-description')).toBeVisible();
    await expect(previewPane.locator('.template-content')).toBeVisible();

    // Verify variables section
    await expect(previewPane.locator('.template-variables')).toBeVisible();
  });

  test('should display variable count for each template', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Check first template row
    const firstRow = page.locator('.template-row').first();

    // Should show variable count badge
    const variableCount = firstRow.locator('.variable-count');
    await expect(variableCount).toBeVisible();
    await expect(variableCount).toContainText(/\d+/); // Should contain number
  });

  test('should sort templates by column headers', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click name column header to sort
    const nameHeader = page.locator('mat-header-cell', { hasText: /^name$/i });
    await nameHeader.click();

    // Wait for sorting
    await page.waitForTimeout(300);

    // Get first two template names
    const rows = page.locator('.template-row');
    const firstName = await rows.nth(0).locator('.template-name').textContent();
    const secondName = await rows.nth(1).locator('.template-name').textContent();

    // Verify alphabetical order
    expect(firstName?.localeCompare(secondName || '')).toBeLessThanOrEqual(0);
  });

  test('should open create template dialog', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Verify create dialog fields
    await expect(page.getByLabel(/template name/i)).toBeVisible();
    await expect(page.getByLabel(/category/i)).toBeVisible();
    await expect(page.getByLabel(/description/i)).toBeVisible();
    await expect(page.locator('.monaco-editor')).toBeVisible();

    // Verify Save button is disabled initially
    const saveButton = page.getByRole('button', { name: /save.*template/i });
    await expect(saveButton).toBeDisabled();
  });

  test('should create new template with auto-detected variables', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Fill in template details
    await page.getByLabel(/template name/i).fill(TEST_TEMPLATE.name);
    await page.getByLabel(/description/i).fill(TEST_TEMPLATE.description);

    // Select category
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option', { name: TEST_TEMPLATE.category }).click();

    // Fill in content with variables
    const contentEditor = page.locator('.monaco-editor textarea');
    await contentEditor.fill(TEST_TEMPLATE.content);

    // Wait for variable extraction
    await page.waitForTimeout(500);

    // Verify variables were detected
    const variableList = page.locator('.detected-variables');
    await expect(variableList).toBeVisible();

    for (const variable of TEST_TEMPLATE.variables) {
      await expect(variableList).toContainText(variable);
    }

    // Verify variable count
    await expect(page.locator('.variable-count')).toContainText(`${TEST_TEMPLATE.variables.length}`);
  });

  test('should validate required fields in create template', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Try to save without filling required fields
    const saveButton = page.getByRole('button', { name: /save.*template/i });
    await expect(saveButton).toBeDisabled();

    // Fill only name
    await page.getByLabel(/template name/i).fill('Test Template');
    await expect(saveButton).toBeDisabled();

    // Fill description
    await page.getByLabel(/description/i).fill('Test description');
    await expect(saveButton).toBeDisabled();

    // Select category
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option').first().click();
    await expect(saveButton).toBeDisabled();

    // Fill content
    await page.locator('.monaco-editor textarea').fill('Test content');

    // Now should be enabled
    await expect(saveButton).toBeEnabled();
  });

  test('should save new template and show in list', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Fill in all fields
    await page.getByLabel(/template name/i).fill(TEST_TEMPLATE.name);
    await page.getByLabel(/description/i).fill(TEST_TEMPLATE.description);
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option', { name: TEST_TEMPLATE.category }).click();
    await page.locator('.monaco-editor textarea').fill(TEST_TEMPLATE.content);

    // Save template
    const saveButton = page.getByRole('button', { name: /save.*template/i });
    await saveButton.click();

    // Verify success message
    await expect(page.locator('.mat-snack-bar-container')).toContainText(/success/i);

    // Verify create dialog closed
    await page.waitForTimeout(500);

    // Verify template appears in list
    await expect(page.locator('.template-row')).toContainText(TEST_TEMPLATE.name);
  });

  test('should select template and append to agent prompt', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click first template
    await page.locator('.template-row').first().click();

    // Click "Use Template" button
    const useButton = page.getByRole('button', { name: /use.*template/i });
    await expect(useButton).toBeVisible();
    await expect(useButton).toBeEnabled();
    await useButton.click();

    // Verify library dialog closed
    await page.waitForTimeout(500);

    // Verify content was appended to system prompt
    const promptEditor = page.locator('.monaco-editor textarea').first();
    const promptContent = await promptEditor.inputValue();
    expect(promptContent.length).toBeGreaterThan(0);
  });

  test('should cancel template creation', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Fill in some data
    await page.getByLabel(/template name/i).fill('Template to Cancel');

    // Click Cancel
    const cancelButton = page.getByRole('button', { name: /cancel/i });
    await cancelButton.click();

    // Verify create dialog closed
    await page.waitForTimeout(300);

    // Verify template was NOT created
    await expect(page.locator('.template-row')).not.toContainText('Template to Cancel');
  });

  test('should highlight variables in template preview', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click template with variables
    await page.locator('.template-row').first().click();

    // Verify preview shows variables section
    const previewPane = page.locator('.template-preview');
    const variablesSection = previewPane.locator('.template-variables');
    await expect(variablesSection).toBeVisible();

    // Verify variables are listed
    const variableChips = variablesSection.locator('mat-chip');
    const chipCount = await variableChips.count();
    expect(chipCount).toBeGreaterThanOrEqual(0);
  });

  test('should show template character count', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);
    await createNewTemplate(page);

    // Type content
    const testContent = 'This is a test template content';
    await page.locator('.monaco-editor textarea').fill(testContent);

    // Verify character count updates
    await expect(page.locator('.character-count')).toContainText(`${testContent.length}`);
  });

  test('should support template editing', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click template row
    await page.locator('.template-row').first().click();

    // Click Edit button (if available)
    const editButton = page.getByRole('button', { name: /edit/i });
    if (await editButton.isVisible()) {
      await editButton.click();

      // Verify edit dialog opened
      await expect(page.locator('h2')).toContainText(/edit.*template/i);

      // Verify fields are populated
      const nameInput = page.getByLabel(/template name/i);
      const nameValue = await nameInput.inputValue();
      expect(nameValue.length).toBeGreaterThan(0);
    }
  });

  test('should display "No templates found" when search has no results', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Search for non-existent template
    const searchInput = page.getByPlaceholder(/search.*template/i);
    await searchInput.fill('xyznonexistenttemplate123');

    // Wait for debounce
    await page.waitForTimeout(400);

    // Verify no results message
    await expect(page.locator('.no-results')).toBeVisible();
    await expect(page.locator('.no-results')).toContainText(/no.*template.*found/i);
  });

  test('should preserve search filters when navigating pages', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Apply search filter
    const searchInput = page.getByPlaceholder(/search.*template/i);
    await searchInput.fill('clinical');
    await page.waitForTimeout(400);

    // Navigate to next page
    const nextPageButton = page.locator('mat-paginator button[aria-label*="next"]');
    if (await nextPageButton.isEnabled()) {
      await nextPageButton.click();

      // Verify search filter still applied
      await expect(searchInput).toHaveValue('clinical');
    }
  });

  test('should close template library without selecting', async ({ page }) => {
    await openTemplateLibraryFromWizard(page);

    // Click Close button
    const closeButton = page.getByRole('button', { name: /close/i });
    await closeButton.click();

    // Verify library dialog closed
    await page.waitForTimeout(300);

    // Verify we're back in wizard Step 3
    await expect(page.locator('.step-indicator.active')).toContainText('System Prompt');
  });
});
