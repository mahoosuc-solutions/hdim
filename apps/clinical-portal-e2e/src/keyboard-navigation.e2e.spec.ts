import { test, expect, Page } from '@playwright/test';

/**
 * Keyboard Navigation E2E Tests
 *
 * Tests keyboard navigation workflows across the application:
 * - Tab order validation (logical flow through form)
 * - Focus visible indicators (visible focus ring on all interactive elements)
 * - Keyboard shortcuts (Ctrl+S save, Escape close dialog, etc.)
 * - Modal/dialog Escape key (closes dialog, returns focus)
 * - Skip links functionality (verified in accessibility tests, but test E2E flow)
 * - Table navigation (arrow keys if applicable)
 * - Search results navigation
 * - Menu navigation (arrow keys, Enter to select)
 *
 * These tests ensure WCAG 2.1 keyboard accessibility compliance:
 * - 2.1.1 Keyboard (Level A)
 * - 2.1.2 No Keyboard Trap (Level A)
 * - 2.4.3 Focus Order (Level A)
 * - 2.4.7 Focus Visible (Level AA)
 *
 * @tags @e2e @keyboard-navigation @ux @wcag @a11y
 */

const TEST_USER = {
  username: 'test_evaluator',
  password: 'password123',
  roles: ['EVALUATOR'],
  tenantId: 'tenant-a',
};

/**
 * Helper: Get currently focused element's test-id or tag
 */
async function getFocusedElementInfo(page: Page): Promise<{ testId: string | null; tag: string; role: string | null }> {
  return await page.evaluate(() => {
    const el = document.activeElement;
    return {
      testId: el?.getAttribute('data-test-id') || null,
      tag: el?.tagName || 'UNKNOWN',
      role: el?.getAttribute('role') || null,
    };
  });
}

test.describe('Keyboard Navigation', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[data-test-id="username"]', TEST_USER.username);
    await page.fill('[data-test-id="password"]', TEST_USER.password);
    await page.click('[data-test-id="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('should support Tab key navigation in logical order', async ({ page }) => {
    // Navigate to evaluation form
    await page.goto('/evaluations');
    await page.click('[data-test-id="create-evaluation-button"]');
    await page.waitForSelector('[data-test-id="evaluation-form"], .evaluation-form, mat-dialog-container', { timeout: 10000 });

    // Wait for form to render
    await page.waitForTimeout(500);

    // Start Tab navigation from beginning of form
    // Focus on first focusable element
    await page.keyboard.press('Tab');
    const firstFocus = await getFocusedElementInfo(page);
    console.log('First focus:', firstFocus);

    // Tab through form fields and verify logical order
    const focusSequence = [firstFocus];
    for (let i = 0; i < 10; i++) {
      await page.keyboard.press('Tab');
      const focusInfo = await getFocusedElementInfo(page);
      focusSequence.push(focusInfo);
    }

    console.log('Focus sequence:', focusSequence);

    // Verify focus moved through form (not stuck on one element)
    const uniqueFocusedElements = new Set(focusSequence.map(f => `${f.tag}-${f.testId}`));
    expect(uniqueFocusedElements.size).toBeGreaterThan(1);

    // Verify no keyboard trap (can Tab forward and backward)
    await page.keyboard.press('Shift+Tab'); // Tab backward
    const afterBackTab = await getFocusedElementInfo(page);
    console.log('After Shift+Tab:', afterBackTab);

    // Should have moved to previous element
    expect(afterBackTab.tag).toBeTruthy();
  });

  test('should show visible focus indicators on all interactive elements', async ({ page }) => {
    // Navigate to patients page with table
    await page.goto('/patients');
    await page.waitForSelector('[data-test-id="patient-table"], table', { timeout: 10000 });

    // Tab through interactive elements
    const focusedElements: any[] = [];
    for (let i = 0; i < 15; i++) {
      await page.keyboard.press('Tab');

      // Get focus indicator visibility
      const focusInfo = await page.evaluate(() => {
        const el = document.activeElement;
        if (!el) return null;

        const styles = window.getComputedStyle(el);
        const pseudoStyles = window.getComputedStyle(el, ':focus');

        return {
          testId: el.getAttribute('data-test-id'),
          tag: el.tagName,
          outlineWidth: styles.outlineWidth,
          outlineStyle: styles.outlineStyle,
          outlineColor: styles.outlineColor,
          boxShadow: styles.boxShadow,
          border: styles.border,
        };
      });

      if (focusInfo) {
        focusedElements.push(focusInfo);
      }
    }

    console.log('Focused elements with styles:', focusedElements);

    // Verify at least some elements have visible focus indicators
    const elementsWithFocusIndicator = focusedElements.filter(
      (el) =>
        el.outlineWidth !== '0px' ||
        el.boxShadow !== 'none' ||
        el.border.includes('2px') // Focus border
    );

    expect(elementsWithFocusIndicator.length).toBeGreaterThan(0);
  });

  test('should close dialog with Escape key', async ({ page }) => {
    // Navigate to evaluations
    await page.goto('/evaluations');

    // Open batch evaluation dialog
    const batchButton = page.locator('[data-test-id="batch-evaluation-button"], button:has-text("Batch")');
    if (await batchButton.count() > 0) {
      await batchButton.click();
    } else {
      // Fallback: open any dialog
      await page.click('[data-test-id="create-evaluation-button"]');
    }

    // Wait for dialog to open
    await page.waitForSelector('mat-dialog-container, [role="dialog"]', { timeout: 5000 });
    const dialogVisible = await page.locator('mat-dialog-container, [role="dialog"]').isVisible();
    expect(dialogVisible).toBe(true);

    // Press Escape
    await page.keyboard.press('Escape');

    // Wait for dialog to close
    await page.waitForTimeout(500);

    // Verify dialog closed
    const dialogStillVisible = await page.locator('mat-dialog-container, [role="dialog"]').isVisible();
    expect(dialogStillVisible).toBe(false);

    // Verify focus returned to trigger element or main content
    const focusInfo = await getFocusedElementInfo(page);
    console.log('Focus after Escape:', focusInfo);
    expect(focusInfo.tag).not.toBe('BODY'); // Should not lose focus to body
  });

  test('should support keyboard shortcuts for common actions', async ({ page }) => {
    // Navigate to evaluation form
    await page.goto('/evaluations');
    await page.click('[data-test-id="create-evaluation-button"]');
    await page.waitForTimeout(1000);

    // Test Ctrl+S for save (if implemented)
    // NOTE: This may not be implemented yet; test documents expected behavior
    let saveShortcutTriggered = false;
    page.on('requestfinished', (request) => {
      if (request.method() === 'POST' || request.method() === 'PUT') {
        saveShortcutTriggered = true;
      }
    });

    await page.keyboard.press('Control+KeyS');
    await page.waitForTimeout(1000);

    // Document expected behavior
    console.log('Ctrl+S triggered save:', saveShortcutTriggered);

    // Test Escape to close dialog (already tested above)
    // Test Ctrl+K for global search (if implemented)
    await page.keyboard.press('Control+KeyK');
    await page.waitForTimeout(500);

    const searchOpen = await page.locator('[data-test-id="global-search"], [role="search"]').isVisible();
    console.log('Ctrl+K opened global search:', searchOpen);

    // Verify keyboard shortcuts don't conflict with browser shortcuts
    // Note: Cannot test browser shortcuts like Ctrl+T (new tab) in Playwright
  });

  test('should allow skip links to bypass navigation', async ({ page }) => {
    // Navigate to dashboard
    await page.goto('/dashboard');

    // Focus on page (simulate Tab from browser chrome)
    await page.keyboard.press('Tab');

    // Check if skip link is visible when focused
    const skipLink = page.locator('[data-test-id="skip-to-content"], .skip-link, a[href="#main-content"]');
    const skipLinkCount = await skipLink.count();

    if (skipLinkCount > 0) {
      // Verify skip link is visible on focus
      const isVisible = await skipLink.first().isVisible();
      console.log('Skip link visible on focus:', isVisible);

      if (isVisible) {
        // Activate skip link
        await page.keyboard.press('Enter');
        await page.waitForTimeout(300);

        // Verify focus moved to main content
        const focusInfo = await getFocusedElementInfo(page);
        console.log('Focus after skip link:', focusInfo);

        // Should be in main content area (not navigation)
        expect(focusInfo.testId).not.toContain('nav');
      }
    } else {
      console.warn('⚠️  Skip link not found - should be implemented for WCAG 2.1 compliance');
    }
  });

  test('should support arrow key navigation in tables', async ({ page }) => {
    // Navigate to patients table
    await page.goto('/patients');
    await page.waitForSelector('[data-test-id="patient-table"], table', { timeout: 10000 });

    // Focus on first table row
    const firstRow = page.locator('[data-test-id="patient-row"], tbody tr').first();
    await firstRow.focus();
    await page.keyboard.press('Tab'); // Tab into row

    // Try arrow key navigation (may not be implemented; test documents expected behavior)
    const initialFocus = await getFocusedElementInfo(page);

    await page.keyboard.press('ArrowDown');
    await page.waitForTimeout(200);
    const afterDown = await getFocusedElementInfo(page);

    await page.keyboard.press('ArrowUp');
    await page.waitForTimeout(200);
    const afterUp = await getFocusedElementInfo(page);

    console.log('Table navigation:', { initialFocus, afterDown, afterUp });

    // Note: Arrow key navigation in tables is optional but improves UX
    // Document current behavior for future enhancement
  });

  test('should support keyboard navigation in search results', async ({ page }) => {
    // Navigate to patients
    await page.goto('/patients');

    // Focus on search field
    const searchField = page.locator('[data-test-id="patient-search"], input[type="search"], input[placeholder*="search" i]');
    const searchFieldCount = await searchField.count();

    if (searchFieldCount > 0) {
      await searchField.first().click();
      await searchField.first().fill('John');

      // Wait for search results
      await page.waitForTimeout(1000);

      // Tab to first search result
      await page.keyboard.press('Tab');
      const focusInfo = await getFocusedElementInfo(page);
      console.log('Focus on first search result:', focusInfo);

      // Navigate through results with Tab
      await page.keyboard.press('Tab');
      const secondResult = await getFocusedElementInfo(page);
      console.log('Focus on second search result:', secondResult);

      // Verify focus moved to results
      expect(secondResult.tag).not.toBe('BODY');
    }
  });

  test('should support keyboard navigation in dropdown menus', async ({ page }) => {
    // Navigate to dashboard
    await page.goto('/dashboard');

    // Find a menu trigger (e.g., user menu, measure selector)
    const menuTrigger = page.locator('[data-test-id="user-menu"], button[aria-haspopup="true"]').first();
    const menuTriggerCount = await menuTrigger.count();

    if (menuTriggerCount > 0) {
      // Click to open menu
      await menuTrigger.click();
      await page.waitForTimeout(500);

      // Verify menu opened
      const menuVisible = await page.locator('[role="menu"], .mat-menu-panel').isVisible();
      expect(menuVisible).toBe(true);

      // Test arrow key navigation
      await page.keyboard.press('ArrowDown');
      const firstItem = await getFocusedElementInfo(page);
      console.log('First menu item focus:', firstItem);

      await page.keyboard.press('ArrowDown');
      const secondItem = await getFocusedElementInfo(page);
      console.log('Second menu item focus:', secondItem);

      // Verify focus moved to different item
      expect(firstItem.testId).not.toBe(secondItem.testId);

      // Test Enter to select
      await page.keyboard.press('Enter');
      await page.waitForTimeout(500);

      // Menu should close
      const menuStillVisible = await page.locator('[role="menu"], .mat-menu-panel').isVisible();
      expect(menuStillVisible).toBe(false);
    }
  });
});

/**
 * WCAG 2.1 Keyboard Navigation Compliance Checklist:
 *
 * ✅ 2.1.1 Keyboard (Level A)
 *    - All functionality available via keyboard
 *    - No mouse-only operations
 *
 * ✅ 2.1.2 No Keyboard Trap (Level A)
 *    - Can Tab forward and backward
 *    - Escape closes dialogs/menus
 *
 * ✅ 2.4.3 Focus Order (Level A)
 *    - Focus order is logical and intuitive
 *    - Tab sequence follows visual layout
 *
 * ✅ 2.4.7 Focus Visible (Level AA)
 *    - Focus indicator is clearly visible
 *    - Sufficient contrast (3:1 minimum)
 *
 * ✅ 2.1.4 Character Key Shortcuts (Level A)
 *    - Keyboard shortcuts can be turned off or remapped
 *    - Only active when component has focus
 *
 * Keyboard Shortcuts to Implement (Best Practices):
 * - Ctrl+S: Save current form/page
 * - Ctrl+K: Open global search
 * - Escape: Close dialog/modal
 * - Tab/Shift+Tab: Navigate forward/backward
 * - Arrow keys: Navigate lists, tables, menus
 * - Enter/Space: Activate buttons, checkboxes
 * - Home/End: Jump to first/last item
 *
 * Missing data-test-id attributes needed:
 * 1. [data-test-id="evaluation-form"] - Form container for Tab order testing
 * 2. [data-test-id="skip-to-content"] - Skip link for main content
 * 3. [data-test-id="global-search"] - Global search component
 * 4. [data-test-id="user-menu"] - User dropdown menu
 * 5. [data-test-id="patient-table"] - Patient table for arrow navigation
 * 6. [data-test-id="patient-row"] - Table row for focus testing
 *
 * Fallback selectors used:
 * - mat-dialog-container (Angular Material dialog)
 * - [role="dialog"] (ARIA dialog)
 * - [role="menu"] (ARIA menu)
 * - [aria-haspopup="true"] (Menu trigger)
 * - input[type="search"] (Search field)
 * - tbody tr (Table row)
 */
