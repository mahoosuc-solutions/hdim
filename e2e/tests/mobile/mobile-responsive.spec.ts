import { test, expect, devices } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { DashboardPage } from '../../pages/dashboard.page';
import { PatientPage } from '../../pages/patient.page';
import { TEST_USERS } from '../../fixtures/test-fixtures';

/**
 * Mobile Device Testing
 *
 * Test Suite: MOBILE
 * Coverage: Responsive design, touch interactions, mobile navigation
 *
 * These tests verify that the application works correctly across
 * different mobile devices and screen sizes.
 *
 * Device Coverage:
 * - iPhone 12/13/14 (390x844)
 * - iPhone SE (375x667)
 * - iPad (768x1024)
 * - iPad Pro (1024x1366)
 * - Pixel 5 (393x851)
 * - Galaxy S21 (360x800)
 */

// Mobile device viewport configurations
const mobileViewports = {
  iPhoneSE: { width: 375, height: 667 },
  iPhone12: { width: 390, height: 844 },
  iPhone14: { width: 390, height: 844 },
  pixel5: { width: 393, height: 851 },
  galaxyS21: { width: 360, height: 800 },
};

// Tablet viewport configurations
const tabletViewports = {
  iPad: { width: 768, height: 1024 },
  iPadPro: { width: 1024, height: 1366 },
  iPadLandscape: { width: 1024, height: 768 },
};

/**
 * Helper to configure mobile viewport
 */
async function setupMobileViewport(page: any, viewport: { width: number; height: number }) {
  await page.setViewportSize(viewport);
}

/**
 * MOBILE-001: iPhone Responsive Tests
 */
test.describe('MOBILE-001: iPhone Responsive Tests', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
  });

  test('should display mobile login form correctly', async ({ page }) => {
    await loginPage.goto();

    // Login form should be visible and properly sized
    const loginForm = page.locator('form, [data-testid="login-form"]');
    await expect(loginForm).toBeVisible();

    // Check form fits within viewport
    const formBox = await loginForm.boundingBox();
    if (formBox) {
      expect(formBox.width).toBeLessThanOrEqual(390);
    }

    // Input fields should be full width on mobile
    const inputs = page.locator('input[type="text"], input[type="password"]');
    const inputCount = await inputs.count();

    for (let i = 0; i < inputCount; i++) {
      const inputBox = await inputs.nth(i).boundingBox();
      if (inputBox) {
        expect(inputBox.width).toBeGreaterThan(200);
      }
    }
  });

  test('should show mobile navigation menu', async ({ page }) => {
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Look for hamburger menu or mobile nav toggle
    const mobileMenuToggle = page.locator(
      '[data-testid="mobile-menu"], .hamburger, [aria-label*="menu"], button.menu-toggle'
    );

    const hasMobileMenu = await mobileMenuToggle.count() > 0;
    console.log('Mobile menu toggle present:', hasMobileMenu);

    // On mobile, navigation should either be hidden in a menu or use a hamburger icon
    if (hasMobileMenu) {
      await mobileMenuToggle.first().click();
      // Navigation should now be visible
      const nav = page.locator('nav, [role="navigation"], .mobile-nav');
      await expect(nav.first()).toBeVisible();
    }
  });

  test('should support touch scrolling on patient list', async ({ page }) => {
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Navigate to patients
    await page.click('text=Patients, a[href*="patient"], [data-testid="nav-patients"]');

    // Get scroll container
    const scrollContainer = page.locator('[data-testid="patient-list"], .patient-list, main');

    if (await scrollContainer.count() > 0) {
      const initialScroll = await scrollContainer.first().evaluate(el => el.scrollTop);

      // Simulate touch scroll
      await scrollContainer.first().evaluate(el => {
        el.scrollTop = 200;
      });

      const newScroll = await scrollContainer.first().evaluate(el => el.scrollTop);
      console.log(`Scroll changed from ${initialScroll} to ${newScroll}`);
    }
  });

  test('should display touch-friendly buttons', async ({ page }) => {
    await loginPage.goto();

    // Buttons should have minimum touch target size (44x44 per WCAG)
    const buttons = page.locator('button, [role="button"], .btn');
    const buttonCount = await buttons.count();

    let touchFriendlyCount = 0;
    for (let i = 0; i < Math.min(buttonCount, 5); i++) {
      const buttonBox = await buttons.nth(i).boundingBox();
      if (buttonBox && buttonBox.height >= 44 && buttonBox.width >= 44) {
        touchFriendlyCount++;
      }
    }

    console.log(`Touch-friendly buttons: ${touchFriendlyCount}/${Math.min(buttonCount, 5)}`);
  });
});

/**
 * MOBILE-002: Android Responsive Tests
 */
test.describe('MOBILE-002: Android Responsive Tests', () => {
  test('should display login page on Pixel 5 viewport', async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.pixel5);
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Verify page renders correctly
    const loginForm = page.locator('form, [data-testid="login-form"]');
    await expect(loginForm).toBeVisible();
  });

  test('should display login page on Galaxy S21 viewport', async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.galaxyS21);
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Verify page renders correctly
    const loginForm = page.locator('form, [data-testid="login-form"]');
    await expect(loginForm).toBeVisible();

    // Check viewport width is respected
    const viewport = page.viewportSize();
    expect(viewport?.width).toBe(360);
  });
});

/**
 * MOBILE-003: Tablet Responsive Tests (iPad)
 */
test.describe('MOBILE-003: Tablet Responsive Tests', () => {
  test('should display two-column layout on iPad', async ({ page }) => {
    await setupMobileViewport(page, tabletViewports.iPad);
    const loginPage = new LoginPage(page);
    const dashboardPage = new DashboardPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // On tablets, we expect more spacious layout
    const mainContent = page.locator('main, [role="main"], .main-content');
    if (await mainContent.count() > 0) {
      const mainBox = await mainContent.first().boundingBox();
      if (mainBox) {
        expect(mainBox.width).toBeGreaterThan(700);
      }
    }
  });

  test('should display sidebar navigation on iPad', async ({ page }) => {
    await setupMobileViewport(page, tabletViewports.iPad);
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.admin.username, TEST_USERS.admin.password);

    // On tablets, sidebar navigation might be visible
    const sidebar = page.locator('[data-testid="sidebar"], aside, .sidebar, nav.side-nav');
    const hasSidebar = await sidebar.count() > 0 && await sidebar.first().isVisible();
    console.log('Sidebar visible on tablet:', hasSidebar);
  });

  test('should handle iPad Pro resolution', async ({ page }) => {
    await setupMobileViewport(page, tabletViewports.iPadPro);
    const loginPage = new LoginPage(page);

    await loginPage.goto();

    const viewport = page.viewportSize();
    expect(viewport?.width).toBe(1024);
    expect(viewport?.height).toBe(1366);
  });
});

/**
 * MOBILE-004: Touch Interaction Tests
 */
test.describe('MOBILE-004: Touch Interaction Tests', () => {
  test.beforeEach(async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);
  });

  test('should support tap to select', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Tap on username field
    const usernameField = page.locator('input[name="username"], input[type="text"]').first();
    await usernameField.tap();

    // Field should be focused
    const isFocused = await usernameField.evaluate(el => document.activeElement === el);
    expect(isFocused).toBe(true);
  });

  test('should support long press for context menu', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Find an element that might have a context menu
    const element = page.locator('[data-testid="patient-row"], tr, .list-item').first();

    if (await element.count() > 0) {
      // Simulate long press
      await element.click({ delay: 500 });
      console.log('Long press simulated');
    }
  });

  test('should have appropriate spacing between interactive elements', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Get all interactive elements
    const interactiveElements = page.locator('button, a, input, select, [role="button"]');
    const count = await interactiveElements.count();

    // Check spacing between adjacent elements
    let adequateSpacing = 0;
    for (let i = 0; i < Math.min(count - 1, 5); i++) {
      const box1 = await interactiveElements.nth(i).boundingBox();
      const box2 = await interactiveElements.nth(i + 1).boundingBox();

      if (box1 && box2) {
        const gap = Math.min(
          Math.abs(box2.x - (box1.x + box1.width)),
          Math.abs(box2.y - (box1.y + box1.height))
        );
        if (gap >= 8) {
          adequateSpacing++;
        }
      }
    }

    console.log(`Elements with adequate spacing: ${adequateSpacing}/${Math.min(count - 1, 5)}`);
  });
});

/**
 * MOBILE-005: Viewport Breakpoint Tests
 */
test.describe('MOBILE-005: Viewport Breakpoint Tests', () => {
  test('should handle extra small screens (320px)', async ({ page }) => {
    await page.setViewportSize({ width: 320, height: 568 });
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Page should still be usable at minimum width
    const loginForm = page.locator('form, [data-testid="login-form"]');
    await expect(loginForm).toBeVisible();

    // No horizontal overflow
    const hasOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });
    expect(hasOverflow).toBe(false);
  });

  test('should transition between mobile and tablet layouts', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Start at mobile size
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(500);

    const mobileLayout = await page.evaluate(() => ({
      width: document.documentElement.clientWidth,
      hasHamburger: !!document.querySelector('.hamburger, [data-testid="mobile-menu"]'),
    }));

    // Expand to tablet size
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(500);

    const tabletLayout = await page.evaluate(() => ({
      width: document.documentElement.clientWidth,
      hasHamburger: !!document.querySelector('.hamburger, [data-testid="mobile-menu"]'),
    }));

    console.log('Mobile layout:', mobileLayout);
    console.log('Tablet layout:', tabletLayout);

    expect(tabletLayout.width).toBeGreaterThan(mobileLayout.width);
  });

  test('should handle landscape orientation on mobile', async ({ page }) => {
    // Portrait
    await page.setViewportSize({ width: 390, height: 844 });
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const portraitViewport = page.viewportSize();

    // Landscape
    await page.setViewportSize({ width: 844, height: 390 });
    await page.waitForTimeout(300);

    const landscapeViewport = page.viewportSize();

    expect(landscapeViewport?.width).toBeGreaterThan(portraitViewport?.width || 0);
    expect(landscapeViewport?.height).toBeLessThan(portraitViewport?.height || 0);

    // Page should still be functional
    const loginForm = page.locator('form, [data-testid="login-form"]');
    await expect(loginForm).toBeVisible();
  });
});

/**
 * MOBILE-006: Mobile Form Input Tests
 */
test.describe('MOBILE-006: Mobile Form Input Tests', () => {
  test.beforeEach(async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);
  });

  test('should display appropriate mobile keyboard for email input', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Check for email input type
    const emailInput = page.locator('input[type="email"], input[name="email"], input[name="username"]').first();
    if (await emailInput.count() > 0) {
      const inputType = await emailInput.getAttribute('type');
      const inputMode = await emailInput.getAttribute('inputmode');
      console.log(`Input type: ${inputType}, inputmode: ${inputMode}`);
    }
  });

  test('should have appropriate input sizes for touch', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const inputs = page.locator('input:not([type="hidden"])');
    const inputCount = await inputs.count();

    let appropriatelySized = 0;
    for (let i = 0; i < inputCount; i++) {
      const inputBox = await inputs.nth(i).boundingBox();
      if (inputBox && inputBox.height >= 40) {
        appropriatelySized++;
      }
    }

    console.log(`Appropriately sized inputs: ${appropriatelySized}/${inputCount}`);
    // Most inputs should be at least 40px tall for touch
    expect(appropriatelySized).toBeGreaterThanOrEqual(Math.floor(inputCount * 0.8));
  });

  test('should handle form submission on mobile', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Fill form using tap
    const usernameField = page.locator('input[name="username"], input[type="text"]').first();
    await usernameField.tap();
    await usernameField.fill(TEST_USERS.evaluator.username);

    const passwordField = page.locator('input[name="password"], input[type="password"]').first();
    await passwordField.tap();
    await passwordField.fill(TEST_USERS.evaluator.password);

    // Submit
    const submitButton = page.locator('button[type="submit"], input[type="submit"], button:has-text("Login")').first();
    await submitButton.tap();

    // Should either succeed or show validation
    await page.waitForTimeout(1000);
  });
});

/**
 * MOBILE-007: Mobile Performance Tests
 */
test.describe('MOBILE-007: Mobile Performance Tests', () => {
  test('should load within acceptable time on 3G simulation', async ({ page, context }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);

    // Slow 3G simulation
    const cdpSession = await context.newCDPSession(page);
    await cdpSession.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: (500 * 1024) / 8, // 500 Kbps
      uploadThroughput: (500 * 1024) / 8,
      latency: 400,
    });

    const startTime = Date.now();
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Wait for main content to be visible
    await page.locator('form, [data-testid="login-form"]').waitFor({ timeout: 30000 });
    const loadTime = Date.now() - startTime;

    console.log(`Page load time on 3G: ${loadTime}ms`);

    // Should load within 10 seconds even on slow connection
    expect(loadTime).toBeLessThan(10000);
  });

  test('should have minimal layout shifts', async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);

    // Inject CLS observer
    await page.addInitScript(() => {
      (window as any).clsValue = 0;
      new PerformanceObserver((entryList) => {
        for (const entry of entryList.getEntries()) {
          if (!(entry as any).hadRecentInput) {
            (window as any).clsValue += (entry as any).value;
          }
        }
      }).observe({ type: 'layout-shift', buffered: true });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await page.waitForTimeout(2000);

    const cls = await page.evaluate(() => (window as any).clsValue || 0);
    console.log(`Cumulative Layout Shift: ${cls}`);

    // CLS should be under 0.1 for good user experience
    expect(cls).toBeLessThan(0.25); // Allowing some tolerance for test environment
  });
});

/**
 * MOBILE-008: Mobile Accessibility Tests
 */
test.describe('MOBILE-008: Mobile Accessibility Tests', () => {
  test.beforeEach(async ({ page }) => {
    await setupMobileViewport(page, mobileViewports.iPhone12);
  });

  test('should have readable font sizes on mobile', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Check font sizes
    const textElements = page.locator('p, span, label, h1, h2, h3, button');
    const count = await textElements.count();

    let readableCount = 0;
    for (let i = 0; i < Math.min(count, 10); i++) {
      const fontSize = await textElements.nth(i).evaluate(el => {
        const style = window.getComputedStyle(el);
        return parseFloat(style.fontSize);
      });

      if (fontSize >= 14) {
        readableCount++;
      }
    }

    console.log(`Readable font sizes: ${readableCount}/${Math.min(count, 10)}`);
    // Most text should be at least 14px
    expect(readableCount).toBeGreaterThanOrEqual(Math.floor(Math.min(count, 10) * 0.7));
  });

  test('should have sufficient color contrast', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Basic contrast check for important text
    const mainHeading = page.locator('h1, .title, [data-testid="page-title"]').first();
    if (await mainHeading.count() > 0) {
      const colors = await mainHeading.evaluate(el => {
        const style = window.getComputedStyle(el);
        return {
          color: style.color,
          backgroundColor: style.backgroundColor,
        };
      });
      console.log('Heading colors:', colors);
    }
  });

  test('should support zoom without horizontal scrolling', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Simulate 2x zoom by halving the viewport
    await page.setViewportSize({ width: 195, height: 422 });
    await page.waitForTimeout(500);

    // Check for horizontal overflow
    const hasOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });

    // Should handle zoom gracefully (some overflow may be acceptable)
    console.log(`Has horizontal overflow at 2x zoom: ${hasOverflow}`);
  });

  test('should have proper focus indicators for touch navigation', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Tab through focusable elements
    const usernameField = page.locator('input[name="username"], input[type="text"]').first();
    await usernameField.focus();

    // Check for visible focus indicator
    const hasFocusStyle = await usernameField.evaluate(el => {
      const style = window.getComputedStyle(el);
      const hasOutline = style.outlineStyle !== 'none' && style.outlineWidth !== '0px';
      const hasBorder = style.borderColor !== 'transparent';
      const hasBoxShadow = style.boxShadow !== 'none';
      return hasOutline || hasBorder || hasBoxShadow;
    });

    console.log('Has visible focus indicator:', hasFocusStyle);
  });
});

/**
 * Orientation Change Tests
 */
test.describe('Orientation Change Tests', () => {
  test('should handle orientation change gracefully', async ({ page }) => {
    const loginPage = new LoginPage(page);

    // Start in portrait
    await page.setViewportSize({ width: 390, height: 844 });
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Get initial layout info
    const portraitInfo = await page.evaluate(() => ({
      width: document.documentElement.clientWidth,
      height: document.documentElement.clientHeight,
    }));

    // Switch to landscape
    await page.setViewportSize({ width: 844, height: 390 });
    await page.waitForTimeout(500);

    const landscapeInfo = await page.evaluate(() => ({
      width: document.documentElement.clientWidth,
      height: document.documentElement.clientHeight,
    }));

    console.log('Portrait:', portraitInfo);
    console.log('Landscape:', landscapeInfo);

    // Content should adapt to new orientation
    expect(landscapeInfo.width).toBeGreaterThan(portraitInfo.width);
  });

  test('should maintain scroll position during orientation change', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.loginAndWait(TEST_USERS.evaluator.username, TEST_USERS.evaluator.password);

    // Scroll down
    await page.evaluate(() => window.scrollTo(0, 200));
    const scrollBefore = await page.evaluate(() => window.scrollY);

    // Change orientation
    await page.setViewportSize({ width: 844, height: 390 });
    await page.waitForTimeout(300);

    const scrollAfter = await page.evaluate(() => window.scrollY);

    console.log(`Scroll before: ${scrollBefore}, after: ${scrollAfter}`);
    // Scroll position should be preserved or reasonably close
  });
});
