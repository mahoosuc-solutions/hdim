import { test } from '@playwright/test';
import {
  setupDemoAuthViaStorage,
  navigateAuthenticated,
  waitForAppReady,
} from '../fixtures/auth.fixture';
import { captureScreenshot, SCREENSHOT_CONFIG } from './screenshot-capture.config';

const ROLE = 'ai-user';

/**
 * Marketing Screenshot Capture — AI User Role
 *
 * Story: "AI-Accelerated Clinical Workflows"
 * AI assistant → NL query → AI response → care recommendations → measure builder →
 * agent builder → knowledge base → insights → constellation AI → pre-visit summary
 */
test.describe('AI User Marketing Screenshots', () => {
  test.use({ viewport: SCREENSHOT_CONFIG.viewport });

  test.beforeEach(async ({ page }) => {
    await setupDemoAuthViaStorage(page, '/dashboard');
  });

  test('01 — AI assistant dashboard', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '01', 'ai-assistant');
  });

  test('02 — Natural language query', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

    // Type a query in the AI input
    const chatInput = page.locator('textarea, input[type="text"], [contenteditable="true"]').last();
    if (await chatInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await chatInput.fill('Which diabetic patients are overdue for HbA1c testing?');
      await page.waitForTimeout(300);
    }

    await captureScreenshot(page, ROLE, '02', 'natural-language-query');
  });

  test('03 — AI response with patient list', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    // Capture the AI response area
    await captureScreenshot(page, ROLE, '03', 'ai-response');
  });

  test('04 — Care recommendations', async ({ page }) => {
    await navigateAuthenticated(page, '/care-recommendations');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '04', 'care-recommendations');
  });

  test('05 — CQL measure builder', async ({ page }) => {
    await navigateAuthenticated(page, '/measure-builder');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '05', 'measure-builder');
  });

  test('06 — Agent builder', async ({ page }) => {
    await navigateAuthenticated(page, '/ai-assistant');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    // Navigate to agent configuration if available
    await captureScreenshot(page, ROLE, '06', 'agent-builder');
  });

  test('07 — Knowledge base', async ({ page }) => {
    await navigateAuthenticated(page, '/knowledge-base');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '07', 'knowledge-base');
  });

  test('08 — Insights dashboard', async ({ page }) => {
    await navigateAuthenticated(page, '/dashboard');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '08', 'insights-dashboard');
  });

  test('09 — Quality constellation with AI highlights', async ({ page }) => {
    await navigateAuthenticated(page, '/visualization/quality-constellation');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '09', 'constellation-ai');
  });

  test('10 — AI pre-visit summary', async ({ page }) => {
    await navigateAuthenticated(page, '/pre-visit');
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);
    await captureScreenshot(page, ROLE, '10', 'pre-visit-summary');
  });
});
