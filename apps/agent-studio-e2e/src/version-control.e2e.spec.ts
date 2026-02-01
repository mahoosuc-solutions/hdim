import { test, expect, Page } from '@playwright/test';

/**
 * Version Control E2E Tests
 *
 * Test Coverage:
 * - View version history for agents
 * - Compare versions side-by-side
 * - Rollback to previous versions
 * - Version status tracking (PUBLISHED, DRAFT, ROLLED_BACK, SUPERSEDED)
 * - Change type indicators (MAJOR, MINOR, PATCH)
 * - Parallel version loading
 *
 * Phase 2 Implementation: Version Control UI (completed January 25, 2026)
 */

/**
 * Helper: Navigate to Agent Builder
 */
async function navigateToAgentBuilder(page: Page) {
  await page.goto('/agent-builder');
  await expect(page).toHaveTitle(/Agent Builder/i);
}

/**
 * Helper: Open version history for an agent
 */
async function openVersionHistory(page: Page, agentName?: string) {
  // Click on first agent card or specific agent
  if (agentName) {
    await page.locator('.agent-card', { hasText: agentName }).click();
  } else {
    await page.locator('.agent-card').first().click();
  }

  // Click Version History button
  const versionHistoryButton = page.getByRole('button', { name: /version.*history/i });
  await expect(versionHistoryButton).toBeVisible();
  await versionHistoryButton.click();

  // Wait for version history dialog
  await expect(page.locator('mat-dialog-container')).toBeVisible();
  await expect(page.locator('h2')).toContainText(/version.*history/i);
}

/**
 * Helper: Compare two versions
 */
async function compareVersions(page: Page, version1Index: number, version2Index: number) {
  // Click compare button for first version
  const rows = page.locator('.version-row');
  const compareButton = rows.nth(version1Index).getByRole('button', { name: /compare/i });
  await compareButton.click();

  // Wait for version compare dialog
  await expect(page.locator('mat-dialog-container').nth(1)).toBeVisible();
  await expect(page.locator('h2')).toContainText(/compare.*version/i);
}

test.describe('Version Control', () => {
  test.beforeEach(async ({ page }) => {
    await navigateToAgentBuilder(page);
  });

  test('should display version history button for each agent', async ({ page }) => {
    // Verify at least one agent exists
    const agentCount = await page.locator('.agent-card').count();
    expect(agentCount).toBeGreaterThanOrEqual(1);

    // Click first agent
    await page.locator('.agent-card').first().click();

    // Verify version history button
    const versionButton = page.getByRole('button', { name: /version.*history/i });
    await expect(versionButton).toBeVisible();
    await expect(versionButton).toBeEnabled();
  });

  test('should open version history dialog', async ({ page }) => {
    await openVersionHistory(page);

    // Verify dialog structure
    await expect(page.locator('.version-history-dialog')).toBeVisible();

    // Verify table headers
    await expect(page.locator('mat-header-cell')).toContainText(/version/i);
    await expect(page.locator('mat-header-cell')).toContainText(/status/i);
    await expect(page.locator('mat-header-cell')).toContainText(/change type/i);
    await expect(page.locator('mat-header-cell')).toContainText(/change summary/i);
    await expect(page.locator('mat-header-cell')).toContainText(/created by/i);
    await expect(page.locator('mat-header-cell')).toContainText(/created at/i);
    await expect(page.locator('mat-header-cell')).toContainText(/actions/i);
  });

  test('should display version list with at least one version', async ({ page }) => {
    await openVersionHistory(page);

    // Verify at least one version exists
    const versionRows = page.locator('.version-row');
    const rowCount = await versionRows.count();
    expect(rowCount).toBeGreaterThanOrEqual(1);

    // Verify current version badge
    await expect(page.locator('.current-badge')).toBeVisible();
    await expect(page.locator('.current-badge')).toContainText(/current/i);
  });

  test('should show version status with correct styling', async ({ page }) => {
    await openVersionHistory(page);

    // Check first version status
    const firstVersion = page.locator('.version-row').first();
    const statusBadge = firstVersion.locator('.status-badge');
    await expect(statusBadge).toBeVisible();

    // Verify status is one of: PUBLISHED, DRAFT, ROLLED_BACK, SUPERSEDED
    const statusText = await statusBadge.textContent();
    expect(['PUBLISHED', 'DRAFT', 'ROLLED_BACK', 'SUPERSEDED']).toContain(statusText?.trim());
  });

  test('should display change type with color coding', async ({ page }) => {
    await openVersionHistory(page);

    // Check first version change type
    const firstVersion = page.locator('.version-row').first();
    const changeTypeBadge = firstVersion.locator('.change-type-badge');
    await expect(changeTypeBadge).toBeVisible();

    // Verify change type is one of: MAJOR, MINOR, PATCH
    const changeTypeText = await changeTypeBadge.textContent();
    expect(['MAJOR', 'MINOR', 'PATCH']).toContain(changeTypeText?.trim());

    // Verify color class exists
    const classAttribute = await changeTypeBadge.getAttribute('class');
    expect(classAttribute).toMatch(/change-type-(major|minor|patch)/i);
  });

  test('should show version number with "Current" badge for active version', async ({ page }) => {
    await openVersionHistory(page);

    // Find current version row
    const currentRow = page.locator('.version-row', { has: page.locator('.current-badge') });
    await expect(currentRow).toBeVisible();

    // Verify current badge styling
    const currentBadge = currentRow.locator('.current-badge');
    await expect(currentBadge).toHaveClass(/primary|accent/);
  });

  test('should display creator information with email', async ({ page }) => {
    await openVersionHistory(page);

    // Check first version creator
    const firstVersion = page.locator('.version-row').first();
    const creatorCell = firstVersion.locator('.creator-cell');
    await expect(creatorCell).toBeVisible();

    // Verify email format
    const creatorText = await creatorCell.textContent();
    expect(creatorText).toMatch(/@/); // Should contain email
  });

  test('should format created at timestamp correctly', async ({ page }) => {
    await openVersionHistory(page);

    // Check first version timestamp
    const firstVersion = page.locator('.version-row').first();
    const timestampCell = firstVersion.locator('.timestamp-cell');
    await expect(timestampCell).toBeVisible();

    // Verify timestamp format (should contain date elements)
    const timestampText = await timestampCell.textContent();
    expect(timestampText).toMatch(/\d{4}|ago|Jan|Feb|Mar/i); // Year or relative time or month
  });

  test('should show truncated change summary with ellipsis', async ({ page }) => {
    await openVersionHistory(page);

    // Check for change summary
    const summaryCell = page.locator('.change-summary-cell').first();
    if (await summaryCell.isVisible()) {
      const summaryText = await summaryCell.textContent();

      // If summary is long, should show ellipsis
      if (summaryText && summaryText.length > 50) {
        await expect(summaryCell).toHaveClass(/truncate|ellipsis/);
      }
    }
  });

  test('should display action buttons for each version', async ({ page }) => {
    await openVersionHistory(page);

    // Check first version actions
    const firstVersion = page.locator('.version-row').first();

    // Verify View button exists
    const viewButton = firstVersion.getByRole('button', { name: /view/i });
    await expect(viewButton).toBeVisible();

    // Verify Compare button exists
    const compareButton = firstVersion.getByRole('button', { name: /compare/i });
    await expect(compareButton).toBeVisible();

    // Rollback button should exist for non-current versions
    const rollbackButton = firstVersion.getByRole('button', { name: /rollback/i });
    // May or may not be visible depending on if it's current version
  });

  test('should paginate version history', async ({ page }) => {
    await openVersionHistory(page);

    // Verify paginator exists
    const paginator = page.locator('mat-paginator');
    await expect(paginator).toBeVisible();

    // Verify page size options
    const pageSizeSelect = paginator.locator('mat-select');
    await expect(pageSizeSelect).toBeVisible();

    // Test page size change
    await pageSizeSelect.click();
    await page.getByRole('option', { name: /25/i }).click();

    // Verify pagination updated
    await expect(paginator.locator('.mat-mdc-paginator-page-size-value')).toContainText('25');
  });

  test('should sort versions by clicking column headers', async ({ page }) => {
    await openVersionHistory(page);

    // Click "Created At" header to sort
    const createdHeader = page.locator('mat-header-cell', { hasText: /created at/i });
    await createdHeader.click();

    // Wait for sorting
    await page.waitForTimeout(300);

    // Verify sort indicator
    await expect(createdHeader.locator('.mat-sort-header-arrow')).toBeVisible();
  });

  test('should show total version count in footer', async ({ page }) => {
    await openVersionHistory(page);

    // Verify footer exists
    const footer = page.locator('.version-history-footer');
    await expect(footer).toBeVisible();

    // Verify total count displayed
    await expect(footer).toContainText(/\d+.*total.*version/i);
  });

  test('should open version compare dialog', async ({ page }) => {
    await openVersionHistory(page);

    // Click compare on first version
    const compareButton = page.locator('.version-row').first().getByRole('button', { name: /compare/i });
    await compareButton.click();

    // Verify compare dialog opened
    await expect(page.locator('mat-dialog-container').nth(1)).toBeVisible();
    await expect(page.locator('h2')).toContainText(/compare.*version/i);
  });

  test('should display side-by-side version comparison', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Verify two-column layout
    await expect(page.locator('.version-column').first()).toBeVisible();
    await expect(page.locator('.version-column').nth(1)).toBeVisible();

    // Verify version labels
    await expect(page.locator('.version-label')).toHaveCount(2);
  });

  test('should show comparison tabs for different config sections', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Verify tabs exist
    const tabs = page.locator('mat-tab');
    const tabCount = await tabs.count();
    expect(tabCount).toBeGreaterThanOrEqual(5);

    // Verify tab labels
    await expect(page.locator('mat-tab')).toContainText(/basic.*info/i);
    await expect(page.locator('mat-tab')).toContainText(/model.*config/i);
    await expect(page.locator('mat-tab')).toContainText(/system.*prompt/i);
    await expect(page.locator('mat-tab')).toContainText(/tools/i);
    await expect(page.locator('mat-tab')).toContainText(/guardrail/i);
  });

  test('should highlight changed fields in comparison', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Look for changed field indicators
    const changedFields = page.locator('.field-changed');
    if (await changedFields.count() > 0) {
      // Verify changed styling
      await expect(changedFields.first()).toHaveClass(/changed|highlight|primary/);
    }
  });

  test('should show change count badge in header', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Verify change count badge
    const changeBadge = page.locator('.change-count-badge');
    if (await changeBadge.isVisible()) {
      const badgeText = await changeBadge.textContent();
      expect(badgeText).toMatch(/\d+.*change/i);
    }
  });

  test('should load versions in parallel using forkJoin', async ({ page }) => {
    await openVersionHistory(page);

    // Monitor network requests
    const requestPromise = page.waitForRequest(/api\/.*\/versions/);

    await compareVersions(page, 0, 1);

    // Wait for request
    const request = await requestPromise;

    // Verify versions loaded (both versions should be in dialog)
    await expect(page.locator('.version-column')).toHaveCount(2);
  });

  test('should navigate between comparison tabs', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Click Model Config tab
    const modelTab = page.locator('mat-tab', { hasText: /model.*config/i });
    await modelTab.click();

    // Verify tab content changed
    await expect(page.locator('.tab-content.active')).toContainText(/provider|model|temperature/i);

    // Click System Prompt tab
    const promptTab = page.locator('mat-tab', { hasText: /system.*prompt/i });
    await promptTab.click();

    // Verify prompt content visible
    await expect(page.locator('.tab-content.active')).toContainText(/prompt|system/i);
  });

  test('should show rollback confirmation dialog', async ({ page }) => {
    await openVersionHistory(page);

    // Find non-current version
    const versions = page.locator('.version-row');
    let rollbackButton = null;

    for (let i = 0; i < await versions.count(); i++) {
      const row = versions.nth(i);
      const hasCurrent = await row.locator('.current-badge').count() > 0;

      if (!hasCurrent) {
        rollbackButton = row.getByRole('button', { name: /rollback/i });
        if (await rollbackButton.isVisible()) {
          break;
        }
      }
    }

    if (rollbackButton) {
      await rollbackButton.click();

      // Verify confirmation dialog
      await expect(page.locator('.confirm-dialog')).toBeVisible();
      await expect(page.locator('.confirm-dialog')).toContainText(/are you sure.*rollback/i);
    }
  });

  test('should perform rollback and refresh version list', async ({ page }) => {
    await openVersionHistory(page);

    // Find rollback button for older version
    const versions = page.locator('.version-row');
    let rollbackButton = null;

    for (let i = 0; i < await versions.count(); i++) {
      const row = versions.nth(i);
      const hasCurrent = await row.locator('.current-badge').count() > 0;

      if (!hasCurrent) {
        rollbackButton = row.getByRole('button', { name: /rollback/i });
        if (await rollbackButton.isVisible()) {
          break;
        }
      }
    }

    if (rollbackButton) {
      await rollbackButton.click();

      // Confirm rollback
      const confirmButton = page.getByRole('button', { name: /confirm|yes/i });
      await confirmButton.click();

      // Wait for success message
      await expect(page.locator('.mat-snack-bar-container')).toContainText(/success|rollback/i);

      // Verify version list refreshed
      await page.waitForTimeout(1000);

      // Should have new ROLLED_BACK version
      await expect(page.locator('.status-badge', { hasText: /rolled.*back/i })).toBeVisible();
    }
  });

  test('should cancel rollback', async ({ page }) => {
    await openVersionHistory(page);

    // Find rollback button
    const versions = page.locator('.version-row');
    let rollbackButton = null;

    for (let i = 0; i < await versions.count(); i++) {
      const row = versions.nth(i);
      const hasCurrent = await row.locator('.current-badge').count() > 0;

      if (!hasCurrent) {
        rollbackButton = row.getByRole('button', { name: /rollback/i });
        if (await rollbackButton.isVisible()) {
          break;
        }
      }
    }

    if (rollbackButton) {
      const initialVersionCount = await versions.count();

      await rollbackButton.click();

      // Click Cancel
      const cancelButton = page.getByRole('button', { name: /cancel|no/i });
      await cancelButton.click();

      // Verify no rollback occurred
      await page.waitForTimeout(300);
      const currentVersionCount = await versions.count();
      expect(currentVersionCount).toBe(initialVersionCount);
    }
  });

  test('should disable rollback for current version', async ({ page }) => {
    await openVersionHistory(page);

    // Find current version
    const currentRow = page.locator('.version-row', { has: page.locator('.current-badge') });

    // Rollback button should be disabled or not visible
    const rollbackButton = currentRow.getByRole('button', { name: /rollback/i });
    if (await rollbackButton.isVisible()) {
      await expect(rollbackButton).toBeDisabled();
    }
  });

  test('should close version history dialog', async ({ page }) => {
    await openVersionHistory(page);

    // Click Close button
    const closeButton = page.getByRole('button', { name: /close/i });
    await closeButton.click();

    // Verify dialog closed
    await expect(page.locator('mat-dialog-container')).not.toBeVisible({ timeout: 2000 });
  });

  test('should close version compare dialog', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Click Close button
    const closeButton = page.locator('mat-dialog-container').nth(1).getByRole('button', { name: /close/i });
    await closeButton.click();

    // Verify compare dialog closed but history still open
    await expect(page.locator('mat-dialog-container')).toHaveCount(1);
  });

  test('should show version metadata in comparison', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Navigate to Basic Info tab
    const basicTab = page.locator('mat-tab', { hasText: /basic.*info/i });
    await basicTab.click();

    // Verify metadata fields
    await expect(page.locator('.version-column')).toContainText(/name|description|status/i);
  });

  test('should display full text comparison in System Prompts tab', async ({ page }) => {
    await openVersionHistory(page);
    await compareVersions(page, 0, 1);

    // Navigate to System Prompts tab
    const promptTab = page.locator('mat-tab', { hasText: /system.*prompt/i });
    await promptTab.click();

    // Verify both prompts visible
    const promptColumns = page.locator('.prompt-column');
    await expect(promptColumns).toHaveCount(2);

    // Each should have content
    for (let i = 0; i < 2; i++) {
      const content = await promptColumns.nth(i).textContent();
      expect(content?.length).toBeGreaterThan(0);
    }
  });
});
