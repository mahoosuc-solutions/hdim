import { test, expect } from '@playwright/test';

/**
 * Agent Studio Smoke Tests
 *
 * Quick validation tests to verify core functionality is working.
 * These should run first and fast to catch major issues early.
 *
 * Test Coverage:
 * - Agent Builder page loads
 * - Can view agent list
 * - Can open create agent dialog
 * - Can access template library
 * - Can view version history
 * - Can open testing sandbox
 */

test.describe('Agent Studio Smoke Tests', () => {
  test('should load agent builder page successfully', async ({ page }) => {
    await page.goto('/agent-builder');

    // Verify page loaded
    await expect(page).toHaveTitle(/Agent Builder/i);
    await expect(page.locator('h1')).toContainText('Agent Builder');

    // Verify key elements exist
    await expect(page.getByRole('button', { name: /create.*agent/i })).toBeVisible();
  });

  test('should display agent cards if agents exist', async ({ page }) => {
    await page.goto('/agent-builder');

    // Check if any agents exist
    const agentCards = page.locator('.agent-card');
    const count = await agentCards.count();

    if (count > 0) {
      // Verify first agent has required info
      const firstCard = agentCards.first();
      await expect(firstCard.locator('.agent-name')).toBeVisible();
      await expect(firstCard.locator('.agent-description')).toBeVisible();
    } else {
      // Verify empty state
      await expect(page.locator('.empty-state')).toBeVisible();
    }
  });

  test('should open create agent wizard', async ({ page }) => {
    await page.goto('/agent-builder');

    const createButton = page.getByRole('button', { name: /create.*agent/i });
    await createButton.click();

    // Verify wizard opened
    await expect(page.locator('mat-dialog-container')).toBeVisible();
    await expect(page.locator('h2')).toContainText(/create.*agent/i);

    // Verify step indicators
    await expect(page.locator('.step-indicator')).toHaveCount(5);
  });

  test('should access template library from wizard', async ({ page }) => {
    await page.goto('/agent-builder');

    // Open wizard
    await page.getByRole('button', { name: /create.*agent/i }).click();

    // Navigate to Step 3 (minimal steps)
    await page.getByLabel(/agent name/i).fill('Smoke Test Agent');
    await page.getByLabel(/description/i).fill('Test');
    await page.locator('mat-select[formControlName="category"]').click();
    await page.getByRole('option').first().click();
    await page.getByRole('button', { name: /next/i }).click();

    // Step 2
    await page.locator('mat-select[formControlName="provider"]').click();
    await page.getByRole('option', { name: /openai/i }).click();
    await page.locator('mat-select[formControlName="model"]').click();
    await page.getByRole('option').first().click();
    await page.getByRole('button', { name: /next/i }).click();

    // Step 3 - click Browse Templates
    const browseButton = page.getByRole('button', { name: /browse.*template/i });
    await expect(browseButton).toBeVisible();
    await browseButton.click();

    // Verify template library opened
    await expect(page.locator('mat-dialog-container').nth(1)).toBeVisible();
    await expect(page.locator('h2')).toContainText(/template.*library/i);
  });

  test('should access version history for agent', async ({ page }) => {
    await page.goto('/agent-builder');

    // Check if agents exist
    const agentCards = page.locator('.agent-card');
    if ((await agentCards.count()) > 0) {
      // Click first agent
      await agentCards.first().click();

      // Click version history
      const versionButton = page.getByRole('button', { name: /version.*history/i });
      await expect(versionButton).toBeVisible();
      await versionButton.click();

      // Verify version history dialog
      await expect(page.locator('mat-dialog-container')).toBeVisible();
      await expect(page.locator('h2')).toContainText(/version.*history/i);
    }
  });

  test('should access testing sandbox for agent', async ({ page }) => {
    await page.goto('/agent-builder');

    // Check if agents exist
    const agentCards = page.locator('.agent-card');
    if ((await agentCards.count()) > 0) {
      // Click first agent
      await agentCards.first().click();

      // Click test agent
      const testButton = page.getByRole('button', { name: /test.*agent/i });
      await expect(testButton).toBeVisible();
      await testButton.click();

      // Verify sandbox dialog
      await expect(page.locator('mat-dialog-container')).toBeVisible();
      await expect(page.locator('h2')).toContainText(/test.*agent/i);

      // Verify key elements
      await expect(page.locator('.conversation-panel')).toBeVisible();
      await expect(page.getByPlaceholder(/type.*message/i)).toBeVisible();
    }
  });

  test('should have working navigation buttons', async ({ page }) => {
    await page.goto('/agent-builder');

    // Verify main nav works
    const homeLink = page.getByRole('link', { name: /home|dashboard/i });
    if (await homeLink.isVisible()) {
      await homeLink.click();
      await expect(page).toHaveURL(/\/dashboard/);
    }
  });

  test('should display user info in header', async ({ page }) => {
    await page.goto('/agent-builder');

    // Check for user menu
    const userMenu = page.locator('.user-menu, .user-avatar, [aria-label*="user"]');
    if ((await userMenu.count()) > 0) {
      await expect(userMenu.first()).toBeVisible();
    }
  });

  test('should have responsive layout on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/agent-builder');

    // Verify page loads on mobile
    await expect(page.locator('h1')).toBeVisible();

    // Mobile menu should exist
    const mobileMenu = page.locator('button[aria-label*="menu"], .mobile-menu-button');
    if ((await mobileMenu.count()) > 0) {
      await expect(mobileMenu.first()).toBeVisible();
    }
  });

  test('should load without console errors', async ({ page }) => {
    const consoleErrors: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto('/agent-builder');
    await page.waitForTimeout(2000);

    // Filter out known acceptable errors (e.g., 404 for optional resources)
    const criticalErrors = consoleErrors.filter((error) => {
      return (
        !error.includes('404') &&
        !error.includes('favicon') &&
        !error.includes('analytics')
      );
    });

    expect(criticalErrors).toHaveLength(0);
  });

  test('should not expose sensitive data in network responses', async ({ page }) => {
    const responses: any[] = [];

    page.on('response', async (response) => {
      if (response.url().includes('/api/')) {
        try {
          const data = await response.json();
          responses.push(data);
        } catch {
          // Ignore non-JSON responses
        }
      }
    });

    await page.goto('/agent-builder');
    await page.waitForTimeout(2000);

    // Verify no PHI patterns in responses
    const phiPatterns = [
      /\d{3}-\d{2}-\d{4}/, // SSN
      /\d{16}/, // Credit card
      /password/i,
      /secret/i,
      /api[_-]?key/i,
    ];

    for (const response of responses) {
      const responseText = JSON.stringify(response);
      for (const pattern of phiPatterns) {
        expect(responseText).not.toMatch(pattern);
      }
    }
  });
});
