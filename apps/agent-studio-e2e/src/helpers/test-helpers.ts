import { Page, expect } from '@playwright/test';

/**
 * Common Test Helpers for Agent Studio E2E Tests
 *
 * Provides reusable functions for authentication, navigation,
 * and common assertions across all test files.
 */

/**
 * Login to application (for tests that require authentication)
 */
export async function login(
  page: Page,
  email: string = 'test@example.com',
  password: string = 'password123'
) {
  await page.goto('/login');

  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/password/i).fill(password);
  await page.getByRole('button', { name: /sign in|login/i }).click();

  // Wait for redirect to dashboard
  await expect(page).toHaveURL(/\/dashboard/);
}

/**
 * Navigate to Agent Builder page
 */
export async function navigateToAgentBuilder(page: Page) {
  await page.goto('/agent-builder');
  await expect(page).toHaveTitle(/Agent Builder/i);
  await expect(page.locator('h1')).toContainText('Agent Builder');
}

/**
 * Create a test agent with minimal required fields
 */
export async function createMinimalAgent(page: Page, agentName: string) {
  // Click Create New Agent
  await page.getByRole('button', { name: /create.*agent/i }).click();
  await expect(page.locator('mat-dialog-container')).toBeVisible();

  // Step 1: Basic Info
  await page.getByLabel(/agent name/i).fill(agentName);
  await page.getByLabel(/description/i).fill(`Test agent: ${agentName}`);
  await page.locator('mat-select[formControlName="category"]').click();
  await page.getByRole('option').first().click();
  await page.getByRole('button', { name: /next/i }).click();

  // Step 2: Model Config
  await page.locator('mat-select[formControlName="provider"]').click();
  await page.getByRole('option', { name: /openai/i }).click();
  await page.locator('mat-select[formControlName="model"]').click();
  await page.getByRole('option').first().click();
  await page.getByRole('button', { name: /next/i }).click();

  // Step 3: System Prompt
  await page.locator('.monaco-editor textarea').fill('You are a helpful assistant.');
  await page.getByRole('button', { name: /next/i }).click();

  // Step 4: Tools (skip)
  await page.getByRole('button', { name: /next/i }).click();

  // Step 5: Guardrails (skip)
  // Save
  await page.getByRole('button', { name: /save.*agent/i }).click();

  // Wait for success
  await expect(page.locator('.mat-snack-bar-container')).toContainText(/success/i);
  await expect(page.locator('mat-dialog-container')).not.toBeVisible({ timeout: 5000 });
}

/**
 * Delete agent by name (cleanup helper)
 */
export async function deleteAgent(page: Page, agentName: string) {
  // Find agent card
  const agentCard = page.locator('.agent-card', { hasText: agentName });
  if (await agentCard.isVisible()) {
    await agentCard.click();

    // Click delete button
    const deleteButton = page.getByRole('button', { name: /delete/i });
    if (await deleteButton.isVisible()) {
      await deleteButton.click();

      // Confirm deletion
      await page.getByRole('button', { name: /confirm|yes/i }).click();

      // Wait for success
      await expect(page.locator('.mat-snack-bar-container')).toContainText(/deleted/i);
    }
  }
}

/**
 * Wait for dialog to be fully visible
 */
export async function waitForDialog(page: Page, titleText?: string) {
  await expect(page.locator('mat-dialog-container')).toBeVisible();
  if (titleText) {
    await expect(page.locator('mat-dialog-container h2')).toContainText(titleText);
  }
}

/**
 * Wait for snackbar notification and verify message
 */
export async function expectSuccessMessage(page: Page, messagePattern?: string | RegExp) {
  const snackbar = page.locator('.mat-snack-bar-container');
  await expect(snackbar).toBeVisible({ timeout: 5000 });

  if (messagePattern) {
    if (typeof messagePattern === 'string') {
      await expect(snackbar).toContainText(messagePattern);
    } else {
      const text = await snackbar.textContent();
      expect(text).toMatch(messagePattern);
    }
  }
}

/**
 * Close all open dialogs
 */
export async function closeAllDialogs(page: Page) {
  const dialogs = page.locator('mat-dialog-container');
  const count = await dialogs.count();

  for (let i = count - 1; i >= 0; i--) {
    const closeButton = dialogs.nth(i).getByRole('button', { name: /close|cancel/i });
    if (await closeButton.isVisible()) {
      await closeButton.click();
      await page.waitForTimeout(300);
    }
  }
}

/**
 * Check if element has Material Design error state
 */
export async function expectFormError(page: Page, fieldLabel: string) {
  const field = page.getByLabel(fieldLabel);
  const formField = field.locator('..').locator('.mat-mdc-form-field');
  await expect(formField).toHaveClass(/mat-form-field-invalid/);
  await expect(page.locator('mat-error')).toBeVisible();
}

/**
 * Fill Monaco Editor content
 */
export async function fillMonacoEditor(page: Page, content: string, nth: number = 0) {
  const editor = page.locator('.monaco-editor textarea').nth(nth);
  await expect(editor).toBeVisible();
  await editor.fill(content);
}

/**
 * Get Monaco Editor content
 */
export async function getMonacoEditorContent(page: Page, nth: number = 0): Promise<string> {
  const editor = page.locator('.monaco-editor textarea').nth(nth);
  return (await editor.inputValue()) || '';
}

/**
 * Select Material select option by value
 */
export async function selectMatOption(page: Page, selectLabel: string, optionText: string) {
  const select = page.getByLabel(selectLabel);
  await select.click();
  await page.getByRole('option', { name: optionText }).click();
}

/**
 * Check Material checkbox by label
 */
export async function checkMatCheckbox(page: Page, label: string) {
  const checkbox = page.getByRole('checkbox', { name: label });
  await expect(checkbox).toBeVisible();
  await checkbox.check();
  await expect(checkbox).toBeChecked();
}

/**
 * Uncheck Material checkbox by label
 */
export async function uncheckMatCheckbox(page: Page, label: string) {
  const checkbox = page.getByRole('checkbox', { name: label });
  await expect(checkbox).toBeVisible();
  await checkbox.uncheck();
  await expect(checkbox).not.toBeChecked();
}

/**
 * Wait for API call to complete
 */
export async function waitForApiCall(page: Page, urlPattern: string | RegExp) {
  await page.waitForResponse((response) => {
    const url = response.url();
    if (typeof urlPattern === 'string') {
      return url.includes(urlPattern);
    }
    return urlPattern.test(url);
  });
}

/**
 * Mock API response for testing
 */
export async function mockApiResponse(
  page: Page,
  urlPattern: string | RegExp,
  responseData: any,
  statusCode: number = 200
) {
  await page.route(urlPattern, (route) => {
    route.fulfill({
      status: statusCode,
      contentType: 'application/json',
      body: JSON.stringify(responseData),
    });
  });
}

/**
 * Take screenshot with descriptive name
 */
export async function takeScreenshot(page: Page, name: string) {
  await page.screenshot({
    path: `apps/agent-studio-e2e/screenshots/${name}.png`,
    fullPage: true,
  });
}

/**
 * Verify HIPAA compliance - no PHI in logs
 */
export async function verifyNoPHIInLogs(page: Page) {
  const logs: string[] = [];

  page.on('console', (msg) => {
    logs.push(msg.text());
  });

  // Common PHI patterns to check for
  const phiPatterns = [
    /\d{3}-\d{2}-\d{4}/, // SSN
    /\d{16}/, // Credit card
    /\d{10}/, // Phone number
    /patient.*\d{6,}/, // Patient IDs
  ];

  for (const log of logs) {
    for (const pattern of phiPatterns) {
      expect(log).not.toMatch(pattern);
    }
  }
}

/**
 * Verify audit logging occurred
 */
export async function verifyAuditLog(
  page: Page,
  action: string,
  resourceType: string
) {
  // Listen for audit log API calls
  const auditCall = await page.waitForRequest((request) => {
    return request.url().includes('/api/audit') && request.method() === 'POST';
  });

  const postData = auditCall.postDataJSON();
  expect(postData.action).toBe(action);
  expect(postData.resourceType).toBe(resourceType);
}

/**
 * Get table row count
 */
export async function getTableRowCount(page: Page, tableSelector: string = 'table'): Promise<number> {
  const rows = page.locator(`${tableSelector} tbody tr`);
  return await rows.count();
}

/**
 * Sort table by column header
 */
export async function sortTableByColumn(page: Page, columnName: string) {
  const header = page.locator('mat-header-cell', { hasText: new RegExp(columnName, 'i') });
  await header.click();
  await page.waitForTimeout(300);
}

/**
 * Change page size in paginator
 */
export async function changePageSize(page: Page, size: number) {
  const pageSizeSelect = page.locator('mat-paginator mat-select');
  await pageSizeSelect.click();
  await page.getByRole('option', { name: size.toString() }).click();
  await page.waitForTimeout(300);
}

/**
 * Navigate to next page
 */
export async function goToNextPage(page: Page) {
  const nextButton = page.locator('mat-paginator button[aria-label*="next"]');
  await expect(nextButton).toBeEnabled();
  await nextButton.click();
  await page.waitForTimeout(300);
}

/**
 * Navigate to previous page
 */
export async function goToPreviousPage(page: Page) {
  const prevButton = page.locator('mat-paginator button[aria-label*="previous"]');
  await expect(prevButton).toBeEnabled();
  await prevButton.click();
  await page.waitForTimeout(300);
}
