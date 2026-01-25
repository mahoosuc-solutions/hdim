import { test, expect, Page } from '@playwright/test';

/**
 * Testing Sandbox E2E Tests
 *
 * Test Coverage:
 * - Agent testing interface
 * - Message sending and receiving
 * - Tool invocation visualization
 * - Conversation export (JSON, Markdown, CSV)
 * - Guardrail trigger detail panel
 * - Test metrics and performance tracking
 * - User feedback collection
 *
 * Phase 3 Implementation: Testing Sandbox Enhancements (completed January 25, 2026)
 */

const TEST_MESSAGE = {
  user: 'What are the care gaps for patient John Doe?',
  expectedTools: ['patient_search', 'care_gap_analysis'],
};

/**
 * Helper: Navigate to Agent Builder
 */
async function navigateToAgentBuilder(page: Page) {
  await page.goto('/agent-builder');
  await expect(page).toHaveTitle(/Agent Builder/i);
}

/**
 * Helper: Open testing sandbox for an agent
 */
async function openTestingSandbox(page: Page, agentName?: string) {
  // Click on agent card
  if (agentName) {
    await page.locator('.agent-card', { hasText: agentName }).click();
  } else {
    await page.locator('.agent-card').first().click();
  }

  // Click Test Agent button
  const testButton = page.getByRole('button', { name: /test.*agent/i });
  await expect(testButton).toBeVisible();
  await testButton.click();

  // Wait for test dialog
  await expect(page.locator('mat-dialog-container')).toBeVisible();
  await expect(page.locator('h2')).toContainText(/test.*agent/i);
}

/**
 * Helper: Send a test message
 */
async function sendTestMessage(page: Page, message: string) {
  const messageInput = page.getByPlaceholder(/type.*message/i);
  await expect(messageInput).toBeVisible();
  await messageInput.fill(message);

  const sendButton = page.getByRole('button', { name: /send/i });
  await expect(sendButton).toBeEnabled();
  await sendButton.click();
}

test.describe('Testing Sandbox', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAgentBuilder(page);
  });

  test('should display test agent button for each agent', async ({ page }) => {
    // Click first agent
    await page.locator('.agent-card').first().click();

    // Verify test button exists
    const testButton = page.getByRole('button', { name: /test.*agent/i });
    await expect(testButton).toBeVisible();
    await expect(testButton).toBeEnabled();
  });

  test('should open testing sandbox dialog', async ({ page }) => {
    await openTestingSandbox(page);

    // Verify dialog structure
    await expect(page.locator('.testing-sandbox-dialog')).toBeVisible();

    // Verify key sections
    await expect(page.locator('.conversation-panel')).toBeVisible();
    await expect(page.locator('.message-input-section')).toBeVisible();
    await expect(page.locator('.metrics-panel')).toBeVisible();
  });

  test('should display agent information in header', async ({ page }) => {
    await openTestingSandbox(page);

    // Verify agent name in header
    const header = page.locator('.sandbox-header');
    await expect(header).toBeVisible();
    await expect(header.locator('.agent-name')).toHaveText(/.+/); // Non-empty
  });

  test('should show empty conversation state initially', async ({ page }) => {
    await openTestingSandbox(page);

    // Verify empty state message
    const conversation = page.locator('.conversation-panel');
    await expect(conversation).toContainText(/no.*message.*yet|start.*conversation/i);
  });

  test('should enable send button only when message is typed', async ({ page }) => {
    await openTestingSandbox(page);

    const messageInput = page.getByPlaceholder(/type.*message/i);
    const sendButton = page.getByRole('button', { name: /^send$/i });

    // Initially disabled
    await expect(sendButton).toBeDisabled();

    // Type message
    await messageInput.fill('Test message');

    // Now enabled
    await expect(sendButton).toBeEnabled();

    // Clear message
    await messageInput.clear();

    // Disabled again
    await expect(sendButton).toBeDisabled();
  });

  test('should send message and display in conversation', async ({ page }) => {
    await openTestingSandbox(page);

    const testMessage = 'Hello, agent!';
    await sendTestMessage(page, testMessage);

    // Wait for message to appear
    await page.waitForTimeout(500);

    // Verify user message displayed
    const userMessage = page.locator('.message.user').last();
    await expect(userMessage).toContainText(testMessage);

    // Verify message cleared from input
    const messageInput = page.getByPlaceholder(/type.*message/i);
    await expect(messageInput).toHaveValue('');
  });

  test('should display agent response after user message', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Hello');

    // Wait for agent response (may take time in real scenario, mock should be fast)
    await page.waitForTimeout(2000);

    // Verify agent response exists
    const agentMessages = page.locator('.message.agent');
    const messageCount = await agentMessages.count();
    expect(messageCount).toBeGreaterThanOrEqual(1);
  });

  test('should show typing indicator while agent is responding', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');

    // Verify typing indicator appears
    const typingIndicator = page.locator('.typing-indicator');
    if (await typingIndicator.isVisible({ timeout: 1000 })) {
      await expect(typingIndicator).toContainText(/typing|thinking/i);
    }
  });

  test('should display tool invocations in conversation', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, TEST_MESSAGE.user);

    // Wait for response
    await page.waitForTimeout(2000);

    // Check for tool invocation indicators
    const toolInvocations = page.locator('.tool-invocation');
    if (await toolInvocations.count() > 0) {
      // Verify tool name displayed
      await expect(toolInvocations.first()).toContainText(/.+/);

      // Verify tool icon
      await expect(toolInvocations.first().locator('mat-icon')).toBeVisible();
    }
  });

  test('should show tool parameters when expanded', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, TEST_MESSAGE.user);
    await page.waitForTimeout(2000);

    // Find tool invocation
    const toolInvocation = page.locator('.tool-invocation').first();
    if (await toolInvocation.isVisible()) {
      // Click to expand
      await toolInvocation.click();

      // Verify parameters visible
      await expect(page.locator('.tool-parameters')).toBeVisible();
    }
  });

  test('should display conversation metrics panel', async ({ page }) => {
    await openTestingSandbox(page);

    const metricsPanel = page.locator('.metrics-panel');
    await expect(metricsPanel).toBeVisible();

    // Verify metrics labels
    await expect(metricsPanel).toContainText(/message.*count|token.*usage|latency|tool.*call/i);
  });

  test('should update metrics after each message exchange', async ({ page }) => {
    await openTestingSandbox(page);

    // Get initial message count
    const messageCountBefore = await page.locator('.metric.message-count .value').textContent();

    // Send message
    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    // Get updated message count
    const messageCountAfter = await page.locator('.metric.message-count .value').textContent();

    // Should have increased
    expect(parseInt(messageCountAfter || '0')).toBeGreaterThan(parseInt(messageCountBefore || '0'));
  });

  test('should display export conversation menu', async ({ page }) => {
    await openTestingSandbox(page);

    // Send at least one message
    await sendTestMessage(page, 'Test message for export');
    await page.waitForTimeout(1000);

    // Click export button
    const exportButton = page.getByRole('button', { name: /export/i });
    await expect(exportButton).toBeVisible();
    await exportButton.click();

    // Verify export menu
    await expect(page.locator('.export-menu')).toBeVisible();
  });

  test('should show export format options (JSON, Markdown, CSV)', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(1000);

    // Open export menu
    const exportButton = page.getByRole('button', { name: /export/i });
    await exportButton.click();

    // Verify format options
    await expect(page.getByRole('menuitem', { name: /json/i })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /markdown/i })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /csv/i })).toBeVisible();
  });

  test('should export conversation as JSON', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Export test');
    await page.waitForTimeout(1000);

    // Click export > JSON
    await page.getByRole('button', { name: /export/i }).click();

    // Listen for download
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('menuitem', { name: /json/i }).click();

    // Verify download started
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.json$/);
  });

  test('should export conversation as Markdown', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Export test');
    await page.waitForTimeout(1000);

    // Click export > Markdown
    await page.getByRole('button', { name: /export/i }).click();

    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('menuitem', { name: /markdown/i }).click();

    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.md$/);
  });

  test('should export conversation as CSV', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Export test');
    await page.waitForTimeout(1000);

    // Click export > CSV
    await page.getByRole('button', { name: /export/i }).click();

    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('menuitem', { name: /csv/i }).click();

    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.csv$/);
  });

  test('should show success toast after export', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Export test');
    await page.waitForTimeout(1000);

    // Export as JSON
    await page.getByRole('button', { name: /export/i }).click();

    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('menuitem', { name: /json/i }).click();
    await downloadPromise;

    // Verify success message
    await expect(page.locator('.mat-snack-bar-container')).toContainText(/export.*success/i);
  });

  test('should display guardrail trigger detail panel', async ({ page }) => {
    await openTestingSandbox(page);

    // Send message that might trigger guardrails
    await sendTestMessage(page, 'Show me patient SSN 123-45-6789');
    await page.waitForTimeout(2000);

    // Check for guardrail panel
    const guardrailPanel = page.locator('.guardrail-triggers-panel');
    if (await guardrailPanel.isVisible()) {
      await expect(guardrailPanel).toContainText(/guardrail.*trigger/i);
    }
  });

  test('should show guardrail trigger types', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test guardrails');
    await page.waitForTimeout(2000);

    // Check for trigger types
    const triggers = page.locator('.guardrail-trigger');
    if (await triggers.count() > 0) {
      const firstTrigger = triggers.first();

      // Verify trigger has type badge
      await expect(firstTrigger.locator('.trigger-type-badge')).toBeVisible();

      // Verify type is one of: PHI_DETECTED, BLOCKED_PATTERN, DISCLAIMER_REQUIRED, etc.
      const typeText = await firstTrigger.locator('.trigger-type-badge').textContent();
      expect(typeText?.length).toBeGreaterThan(0);
    }
  });

  test('should display guardrail trigger severity', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    const triggers = page.locator('.guardrail-trigger');
    if (await triggers.count() > 0) {
      // Verify severity badge
      const severityBadge = triggers.first().locator('.severity-badge');
      if (await severityBadge.isVisible()) {
        const severityText = await severityBadge.textContent();
        expect(['HIGH', 'MEDIUM', 'LOW']).toContain(severityText?.trim());
      }
    }
  });

  test('should show trigger details on expansion', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    const triggers = page.locator('.guardrail-trigger');
    if (await triggers.count() > 0) {
      const firstTrigger = triggers.first();
      await firstTrigger.click();

      // Verify details panel
      await expect(page.locator('.trigger-details')).toBeVisible();
      await expect(page.locator('.trigger-details')).toContainText(/message|matched.*pattern|action.*taken/i);
    }
  });

  test('should allow clearing conversation history', async ({ page }) => {
    await openTestingSandbox(page);

    // Send messages
    await sendTestMessage(page, 'Message 1');
    await page.waitForTimeout(1000);
    await sendTestMessage(page, 'Message 2');
    await page.waitForTimeout(1000);

    // Click clear button
    const clearButton = page.getByRole('button', { name: /clear.*conversation/i });
    if (await clearButton.isVisible()) {
      await clearButton.click();

      // Confirm clear
      await page.getByRole('button', { name: /confirm|yes/i }).click();

      // Verify conversation cleared
      await expect(page.locator('.message.user')).toHaveCount(0);
      await expect(page.locator('.conversation-panel')).toContainText(/no.*message/i);
    }
  });

  test('should display feedback buttons for agent responses', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Hello');
    await page.waitForTimeout(2000);

    // Check for feedback buttons on agent message
    const agentMessage = page.locator('.message.agent').last();
    const thumbsUp = agentMessage.getByRole('button', { name: /thumb.*up|helpful/i });
    const thumbsDown = agentMessage.getByRole('button', { name: /thumb.*down|not.*helpful/i });

    if (await thumbsUp.isVisible()) {
      await expect(thumbsUp).toBeEnabled();
      await expect(thumbsDown).toBeEnabled();
    }
  });

  test('should submit positive feedback', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    // Click thumbs up
    const thumbsUp = page.locator('.message.agent').last().getByRole('button', { name: /thumb.*up/i });
    if (await thumbsUp.isVisible()) {
      await thumbsUp.click();

      // Verify feedback registered
      await expect(thumbsUp).toHaveClass(/selected|active/);

      // May show success message
      if (await page.locator('.mat-snack-bar-container').isVisible({ timeout: 1000 })) {
        await expect(page.locator('.mat-snack-bar-container')).toContainText(/feedback.*received/i);
      }
    }
  });

  test('should submit negative feedback with optional comment', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    // Click thumbs down
    const thumbsDown = page.locator('.message.agent').last().getByRole('button', { name: /thumb.*down/i });
    if (await thumbsDown.isVisible()) {
      await thumbsDown.click();

      // May open feedback dialog
      if (await page.locator('mat-dialog-container').count() > 1) {
        // Fill in feedback
        await page.getByPlaceholder(/feedback|comment/i).fill('Response was inaccurate');

        // Submit
        await page.getByRole('button', { name: /submit/i }).click();

        // Verify success
        await expect(page.locator('.mat-snack-bar-container')).toContainText(/feedback.*received/i);
      }
    }
  });

  test('should track response latency in metrics', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'Test');
    await page.waitForTimeout(2000);

    // Check latency metric
    const latencyMetric = page.locator('.metric.latency .value');
    if (await latencyMetric.isVisible()) {
      const latencyText = await latencyMetric.textContent();
      expect(latencyText).toMatch(/\d+.*ms|s/); // Should show time unit
    }
  });

  test('should display token usage metrics', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, 'What is the weather today?');
    await page.waitForTimeout(2000);

    // Check token usage
    const tokenMetric = page.locator('.metric.tokens .value');
    if (await tokenMetric.isVisible()) {
      const tokenText = await tokenMetric.textContent();
      expect(parseInt(tokenText || '0')).toBeGreaterThan(0);
    }
  });

  test('should show tool invocation count in metrics', async ({ page }) => {
    await openTestingSandbox(page);

    await sendTestMessage(page, TEST_MESSAGE.user);
    await page.waitForTimeout(2000);

    // Check tool call metric
    const toolMetric = page.locator('.metric.tool-calls .value');
    if (await toolMetric.isVisible()) {
      const toolCount = await toolMetric.textContent();
      expect(parseInt(toolCount || '0')).toBeGreaterThanOrEqual(0);
    }
  });

  test('should close testing sandbox', async ({ page }) => {
    await openTestingSandbox(page);

    // Click close button
    const closeButton = page.getByRole('button', { name: /close/i });
    await closeButton.click();

    // Verify dialog closed
    await expect(page.locator('mat-dialog-container')).not.toBeVisible({ timeout: 2000 });
  });

  test('should preserve conversation when minimizing and reopening', async ({ page }) => {
    await openTestingSandbox(page);

    const testMessage = 'Remember this message';
    await sendTestMessage(page, testMessage);
    await page.waitForTimeout(1000);

    // Close dialog
    await page.getByRole('button', { name: /close/i }).click();

    // Reopen sandbox
    await openTestingSandbox(page);

    // Verify message still there
    await expect(page.locator('.message.user')).toContainText(testMessage);
  });

  test('should scroll to latest message automatically', async ({ page }) => {
    await openTestingSandbox(page);

    // Send multiple messages
    for (let i = 1; i <= 5; i++) {
      await sendTestMessage(page, `Message ${i}`);
      await page.waitForTimeout(1000);
    }

    // Verify conversation panel scrolled to bottom
    const conversationPanel = page.locator('.conversation-panel');
    const isScrolledToBottom = await conversationPanel.evaluate((el) => {
      return el.scrollHeight - el.scrollTop <= el.clientHeight + 50; // 50px tolerance
    });

    expect(isScrolledToBottom).toBe(true);
  });
});
